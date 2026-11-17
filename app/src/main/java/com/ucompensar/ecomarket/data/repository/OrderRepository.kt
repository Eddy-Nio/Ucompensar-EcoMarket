package com.ucompensar.ecomarket.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.dataObjects
import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.data.AppException
import com.ucompensar.ecomarket.data.model.CartItem
import com.ucompensar.ecomarket.data.model.Order
import com.ucompensar.ecomarket.data.model.OrderItem
import com.ucompensar.ecomarket.data.model.OrderStatus
import com.ucompensar.ecomarket.data.model.Pricing
import com.ucompensar.ecomarket.data.model.UserProfile
import com.ucompensar.ecomarket.data.payment.PaymentResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/** Órdenes de compra en la colección `orders`: creación, confirmación de pago e historial. */
object OrderRepository {

    /**
     * Crea la orden a partir del carrito, en estado PENDING_PAYMENT, antes de abrir la pasarela.
     * Valida que cada producto siga existiendo y tenga stock suficiente.
     */
    suspend fun createPending(
        buyer: UserProfile,
        cart: List<CartItem>,
        address: String,
        latitude: Double?,
        longitude: Double?
    ): Order {
        if (cart.isEmpty()) throw AppException(R.string.cart_empty)
        cart.forEach { line ->
            val product = ProductRepository.get(line.productId)
                ?: throw AppException(R.string.error_product_unavailable, line.name)
            if (product.stock < line.quantity) throw AppException(R.string.error_out_of_stock, line.name)
        }
        val items = cart.map { OrderItem(it.productId, it.name, it.price, it.quantity, it.sellerId, it.category) }
        val subtotal = items.sumOf { it.subtotal }
        val tax = Pricing.tax(subtotal)
        val doc = FirestorePaths.orders().document()
        val order = Order(
            id = doc.id, buyerId = buyer.uid, buyerName = buyer.name, buyerEmail = buyer.email,
            items = items, sellerIds = items.map { it.sellerId }.distinct(),
            subtotal = subtotal, tax = tax, total = subtotal + tax,
            status = OrderStatus.PENDING_PAYMENT, address = address, latitude = latitude, longitude = longitude
        )
        doc.set(order).await()
        return order
    }

    /** Lee una orden una sola vez. */
    suspend fun get(id: String): Order? =
        FirestorePaths.orders().document(id).get().await().toObject(Order::class.java)

    /**
     * Registra la respuesta de ePayco. Si el pago fue aceptado, en un mismo lote marca la orden
     * como pagada, descuenta el stock de cada producto y vacía el carrito del comprador.
     */
    suspend fun applyPayment(order: Order, result: PaymentResult) {
        val orderRef = FirestorePaths.orders().document(order.id)
        val payment = mapOf(
            "paymentRef" to result.reference,
            "paymentResponse" to result.response,
            "transactionId" to result.transactionId
        )
        val batch = FirestorePaths.db.batch()
        when {
            result.approved -> {
                batch.update(orderRef, payment + ("status" to OrderStatus.PAID.name))
                order.items.forEach { item ->
                    batch.update(FirestorePaths.products().document(item.productId), "stock", FieldValue.increment(-item.quantity.toLong()))
                    batch.delete(FirestorePaths.cart(order.buyerId).document(item.productId))
                }
            }
            result.pending -> batch.update(orderRef, payment)
            else -> batch.update(orderRef, payment + ("status" to OrderStatus.PAYMENT_REJECTED.name))
        }
        batch.commit().await()
    }

    /** Historial del comprador, de la más reciente a la más antigua. */
    fun observeByBuyer(buyerId: String): Flow<List<Order>> =
        FirestorePaths.orders().whereEqualTo("buyerId", buyerId).dataObjects<Order>().map(::newestFirst)

    /** Órdenes pagadas que incluyen productos del vendedor. */
    fun observeBySeller(sellerId: String): Flow<List<Order>> =
        FirestorePaths.orders().whereArrayContains("sellerIds", sellerId).dataObjects<Order>()
            .map { orders -> newestFirst(orders.filter { it.status.isSale }) }

    /** Todas las órdenes (dashboard y reporte del administrador). */
    fun observeAll(): Flow<List<Order>> = FirestorePaths.orders().dataObjects<Order>().map(::newestFirst)

    /** Avanza el estado logístico: pagada → enviada → entregada. */
    suspend fun advance(order: Order) {
        val next = order.status.nextForSeller() ?: return
        FirestorePaths.orders().document(order.id).update("status", next.name).await()
    }

    private fun newestFirst(orders: List<Order>) = orders.sortedByDescending { it.createdAt }
}

