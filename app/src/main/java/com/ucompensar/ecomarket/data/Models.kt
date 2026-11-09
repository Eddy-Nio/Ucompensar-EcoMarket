package com.ucompensar.ecomarket.data

import androidx.annotation.StringRes
import com.ucompensar.ecomarket.R

enum class Role(@StringRes val labelRes: Int) {
    BUYER(R.string.role_buyer),
    SELLER(R.string.role_seller),
    ADMIN(R.string.role_admin)
}

enum class Approval(@StringRes val labelRes: Int) {
    PENDING(R.string.product_pending),
    APPROVED(R.string.product_approved),
    REJECTED(R.string.product_rejected)
}

enum class OrderStatus(@StringRes val labelRes: Int) {
    PENDING(R.string.order_status_pending),
    SHIPPED(R.string.order_status_shipped),
    DELIVERED(R.string.order_status_delivered);

    fun next(): OrderStatus = entries[(ordinal + 1).coerceAtMost(entries.size - 1)]
}

data class Product(
    val id: Int,
    var name: String,
    var description: String,
    var price: Int,
    var stock: Int,
    var category: String,
    var producer: String,
    var approval: Approval = Approval.APPROVED
)

data class User(
    val id: Int,
    var name: String,
    var email: String,
    var phone: String,
    var role: Role,
    var active: Boolean = true
)

data class CartItem(val product: Product, var quantity: Int) {
    val subtotal: Int get() = product.price * quantity
}

data class Order(
    val id: Int,
    val buyer: String,
    val items: List<CartItem>,
    val total: Int,
    var status: OrderStatus,
    val dayIndex: Int
) {
    val summary: String get() = items.joinToString { "${it.quantity} × ${it.product.name}" }
}
