package com.ucompensar.ecomarket.ui.common

import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.data.model.Order
import com.ucompensar.ecomarket.databinding.ItemOrderBinding
import com.ucompensar.ecomarket.util.bindOrderStatus
import com.ucompensar.ecomarket.util.show
import com.ucompensar.ecomarket.util.toCop
import com.ucompensar.ecomarket.util.toShortDate

/** Acción opcional del botón de la tarjeta de pedido (pagar, marcar enviado, etc.). */
data class OrderAction(val labelRes: Int, val onClick: (Order) -> Unit)

/**
 * Enlaza una tarjeta de pedido. La usan el comprador (historial), el vendedor (pedidos recibidos)
 * y el administrador (dashboard). [itemsText] permite mostrar solo las líneas de un vendedor.
 */
fun ItemOrderBinding.bindOrder(order: Order, action: OrderAction? = null, itemsText: String = order.summary) {
    val context = root.context
    txtOrderTitle.text = context.getString(R.string.order_item_format, order.shortId, order.buyerName)
    txtOrderMeta.text = context.getString(R.string.order_meta, order.createdAt.toShortDate(), order.address)
    txtOrderItems.text = itemsText
    txtOrderTotal.text = order.total.toCop()
    txtOrderStatus.bindOrderStatus(order.status)

    val hasPayment = order.paymentRef.isNotBlank()
    txtOrderPayment.show(hasPayment)
    if (hasPayment) txtOrderPayment.text = context.getString(R.string.order_payment_ref, order.paymentRef, order.paymentResponse)

    btnOrderAction.show(action != null)
    action?.let {
        btnOrderAction.setText(it.labelRes)
        btnOrderAction.setOnClickListener { _ -> it.onClick(order) }
    }
}
