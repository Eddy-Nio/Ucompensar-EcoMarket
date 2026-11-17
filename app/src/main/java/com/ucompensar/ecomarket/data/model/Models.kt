package com.ucompensar.ecomarket.data.model

import androidx.annotation.StringRes
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import com.ucompensar.ecomarket.R
import java.util.Date

/*
 * Modelos de dominio. Se almacenan en Cloud Firestore; por eso todas las propiedades
 * son `var` con valor por defecto (Firestore necesita un constructor vacío y setters).
 * Las propiedades calculadas se marcan con @get:Exclude para que no se guarden.
 */

/** Rol del usuario; define el menú y los permisos en la app y en las reglas de seguridad. */
enum class Role(@StringRes val labelRes: Int) {
    BUYER(R.string.role_buyer),
    SELLER(R.string.role_seller),
    ADMIN(R.string.role_admin)
}

/** Estado de moderación de un producto publicado por un vendedor. */
enum class Approval(@StringRes val labelRes: Int) {
    PENDING(R.string.product_pending),
    APPROVED(R.string.product_approved),
    REJECTED(R.string.product_rejected)
}

/** Ciclo de vida de una orden: se crea pendiente de pago y avanza hasta la entrega. */
enum class OrderStatus(@StringRes val labelRes: Int) {
    PENDING_PAYMENT(R.string.order_status_pending_payment),
    PAID(R.string.order_status_paid),
    SHIPPED(R.string.order_status_shipped),
    DELIVERED(R.string.order_status_delivered),
    PAYMENT_REJECTED(R.string.order_status_rejected);

    /** Siguiente estado logístico que puede asignar el vendedor, o null si no hay más pasos. */
    fun nextForSeller(): OrderStatus? = when (this) {
        PAID -> SHIPPED
        SHIPPED -> DELIVERED
        else -> null
    }

    /** Indica si la orden ya generó una venta (pago aprobado). */
    val isSale: Boolean get() = this == PAID || this == SHIPPED || this == DELIVERED
}

/** Perfil de usuario guardado en `users/{uid}`; el id del documento es el uid de Firebase Auth. */
data class UserProfile(
    @DocumentId var uid: String = "",
    var name: String = "",
    var email: String = "",
    var phone: String = "",
    var role: Role = Role.BUYER,
    var active: Boolean = true,
    var business: String = "",
    @ServerTimestamp var createdAt: Date? = null
)

/** Producto del catálogo guardado en `products/{id}`; la imagen vive en Firebase Storage. */
data class Product(
    @DocumentId var id: String = "",
    var name: String = "",
    var description: String = "",
    var price: Int = 0,
    var stock: Int = 0,
    var category: String = "",
    var sellerId: String = "",
    var sellerName: String = "",
    var imageUrl: String = "",
    var imagePath: String = "",
    var approval: Approval = Approval.PENDING,
    @ServerTimestamp var createdAt: Date? = null
)

/** Línea del carrito en `carts/{uid}/items/{productId}`; guarda una copia del precio vigente. */
data class CartItem(
    @DocumentId var productId: String = "",
    var name: String = "",
    var price: Int = 0,
    var quantity: Int = 1,
    var stock: Int = 0,
    var imageUrl: String = "",
    var sellerId: String = "",
    var category: String = ""
) {
    @get:Exclude
    val subtotal: Int get() = price * quantity
}

/** Línea de una orden; es una foto inmutable del producto al momento de comprar. */
data class OrderItem(
    var productId: String = "",
    var name: String = "",
    var price: Int = 0,
    var quantity: Int = 0,
    var sellerId: String = "",
    var category: String = ""
) {
    @get:Exclude
    val subtotal: Int get() = price * quantity
}

/** Orden de compra en `orders/{id}`; su id también es el número de factura enviado a ePayco. */
data class Order(
    @DocumentId var id: String = "",
    var buyerId: String = "",
    var buyerName: String = "",
    var buyerEmail: String = "",
    var items: List<OrderItem> = emptyList(),
    var sellerIds: List<String> = emptyList(),
    var subtotal: Int = 0,
    var tax: Int = 0,
    var total: Int = 0,
    var status: OrderStatus = OrderStatus.PENDING_PAYMENT,
    var address: String = "",
    var latitude: Double? = null,
    var longitude: Double? = null,
    var paymentRef: String = "",
    var paymentResponse: String = "",
    var transactionId: String = "",
    @ServerTimestamp var createdAt: Date? = null
) {
    /** Resumen corto de los productos, por ejemplo "2 × Café, 1 × Miel". */
    @get:Exclude
    val summary: String get() = items.joinToString { "${it.quantity} × ${it.name}" }

    /** Número corto y legible de la orden para mostrar en pantalla. */
    @get:Exclude
    val shortId: String get() = id.take(6).uppercase()
}

/** Reglas de precio comunes al carrito y a la orden. */
object Pricing {
    /** IVA general en Colombia. */
    const val IVA = 0.19

    /** Impuesto calculado sobre el subtotal (redondeado a pesos). */
    fun tax(subtotal: Int): Int = Math.round(subtotal * IVA).toInt()
}

/** Categorías del catálogo (EcoMarket trabaja con productos sostenibles colombianos). */
val CATEGORIES = listOf("Orgánicos", "Frutas", "Verduras", "Vegano", "Sin gluten", "Artesanías")
