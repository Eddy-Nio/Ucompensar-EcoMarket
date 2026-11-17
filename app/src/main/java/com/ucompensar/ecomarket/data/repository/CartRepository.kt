package com.ucompensar.ecomarket.data.repository

import com.google.firebase.firestore.dataObjects
import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.data.AppException
import com.ucompensar.ecomarket.data.model.CartItem
import com.ucompensar.ecomarket.data.model.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

/** CRUD del carrito persistido en `carts/{uid}/items` (se conserva entre sesiones y dispositivos). */
object CartRepository {

    /** Líneas del carrito en tiempo real. */
    fun observe(uid: String): Flow<List<CartItem>> = FirestorePaths.cart(uid).dataObjects<CartItem>()

    /** Lee el carrito una sola vez (para crear la orden). */
    suspend fun items(uid: String): List<CartItem> =
        FirestorePaths.cart(uid).get().await().toObjects(CartItem::class.java)

    /**
     * Agrega [quantity] unidades de un producto. Si ya estaba, suma la cantidad sin superar el stock.
     * Usa una transacción para que dos toques seguidos no pisen la cantidad.
     */
    suspend fun add(uid: String, product: Product, quantity: Int = 1) {
        if (product.stock <= 0) throw AppException(R.string.error_out_of_stock, product.name)
        val ref = FirestorePaths.cart(uid).document(product.id)
        FirestorePaths.db.runTransaction { tx ->
            val current = tx.get(ref).toObject(CartItem::class.java)?.quantity ?: 0
            val item = CartItem(
                productId = product.id, name = product.name, price = product.price,
                quantity = (current + quantity).coerceAtMost(product.stock), stock = product.stock,
                imageUrl = product.imageUrl, sellerId = product.sellerId, category = product.category
            )
            tx.set(ref, item)
        }.await()
    }

    /** Cambia la cantidad de una línea; con 0 o menos la elimina. */
    suspend fun setQuantity(uid: String, item: CartItem, quantity: Int) {
        val ref = FirestorePaths.cart(uid).document(item.productId)
        if (quantity <= 0) ref.delete().await()
        else ref.update("quantity", quantity.coerceAtMost(item.stock.coerceAtLeast(1))).await()
    }

    /** Quita un producto del carrito. */
    suspend fun remove(uid: String, productId: String) {
        FirestorePaths.cart(uid).document(productId).delete().await()
    }

    /** Vacía el carrito completo en un solo lote. */
    suspend fun clear(uid: String) {
        val docs = FirestorePaths.cart(uid).get().await().documents
        if (docs.isEmpty()) return
        val batch = FirestorePaths.db.batch()
        docs.forEach { batch.delete(it.reference) }
        batch.commit().await()
    }
}
