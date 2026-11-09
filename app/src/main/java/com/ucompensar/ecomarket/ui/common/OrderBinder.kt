package com.ucompensar.ecomarket.ui.common

import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.data.Order
import com.ucompensar.ecomarket.data.OrderStatus
import com.ucompensar.ecomarket.databinding.ItemOrderBinding
import com.ucompensar.ecomarket.util.bindOrderStatus
import com.ucompensar.ecomarket.util.show
import com.ucompensar.ecomarket.util.toCop

/** Enlaza una tarjeta de pedido; el botón de avance solo se muestra cuando se entrega un callback. */
fun ItemOrderBinding.bindOrder(order: Order, onAdvance: ((Order) -> Unit)? = null) {
    txtOrderTitle.text = root.context.getString(R.string.order_item_format, order.id, order.buyer)
    txtOrderItems.text = order.summary
    txtOrderTotal.text = order.total.toCop()
    txtOrderStatus.bindOrderStatus(order.status)
    btnAdvance.show(onAdvance != null && order.status != OrderStatus.DELIVERED)
    btnAdvance.setOnClickListener { onAdvance?.invoke(order) }
}
