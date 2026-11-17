package com.ucompensar.ecomarket.util

import android.widget.TextView
import androidx.core.content.ContextCompat
import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.data.model.Approval
import com.ucompensar.ecomarket.data.model.OrderStatus

/** Aplica texto, fondo y color a una insignia de estado. */
private fun TextView.applyBadge(textRes: Int, backgroundRes: Int, colorRes: Int) {
    setText(textRes)
    setBackgroundResource(backgroundRes)
    setTextColor(ContextCompat.getColor(context, colorRes))
}

private fun TextView.positive(textRes: Int) = applyBadge(textRes, R.drawable.bg_badge_active, R.color.eco_green_dark)
private fun TextView.warning(textRes: Int) = applyBadge(textRes, R.drawable.bg_badge_warning, R.color.eco_text_primary)
private fun TextView.negative(textRes: Int) = applyBadge(textRes, R.drawable.bg_badge_error, R.color.eco_error)

/** Insignia de moderación de producto. */
fun TextView.bindApproval(approval: Approval) = when (approval) {
    Approval.APPROVED -> positive(approval.labelRes)
    Approval.PENDING -> warning(approval.labelRes)
    Approval.REJECTED -> negative(approval.labelRes)
}

/** Insignia de estado de una orden. */
fun TextView.bindOrderStatus(status: OrderStatus) = when (status) {
    OrderStatus.PAID, OrderStatus.DELIVERED -> positive(status.labelRes)
    OrderStatus.SHIPPED, OrderStatus.PENDING_PAYMENT -> warning(status.labelRes)
    OrderStatus.PAYMENT_REJECTED -> negative(status.labelRes)
}

/** Insignia de cuenta activa o suspendida. */
fun TextView.bindActive(active: Boolean) =
    if (active) positive(R.string.user_active) else negative(R.string.user_suspended)
