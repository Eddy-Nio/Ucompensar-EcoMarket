package com.ucompensar.ecomarket.util

import android.widget.TextView
import androidx.core.content.ContextCompat
import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.data.Approval
import com.ucompensar.ecomarket.data.OrderStatus

private fun TextView.applyBadge(textRes: Int, backgroundRes: Int, colorRes: Int) {
    setText(textRes)
    setBackgroundResource(backgroundRes)
    setTextColor(ContextCompat.getColor(context, colorRes))
}

fun TextView.bindApproval(approval: Approval) = when (approval) {
    Approval.APPROVED -> applyBadge(approval.labelRes, R.drawable.bg_badge_active, R.color.eco_green_dark)
    Approval.PENDING -> applyBadge(approval.labelRes, R.drawable.bg_badge_warning, R.color.eco_text_primary)
    Approval.REJECTED -> applyBadge(approval.labelRes, R.drawable.bg_badge_error, R.color.eco_error)
}

fun TextView.bindOrderStatus(status: OrderStatus) = when (status) {
    OrderStatus.DELIVERED -> applyBadge(status.labelRes, R.drawable.bg_badge_active, R.color.eco_green_dark)
    OrderStatus.SHIPPED -> applyBadge(status.labelRes, R.drawable.bg_badge_warning, R.color.eco_text_primary)
    OrderStatus.PENDING -> applyBadge(status.labelRes, R.drawable.bg_badge_error, R.color.eco_error)
}

fun TextView.bindActive(active: Boolean) =
    if (active) applyBadge(R.string.user_active, R.drawable.bg_badge_active, R.color.eco_green_dark)
    else applyBadge(R.string.user_suspended, R.drawable.bg_badge_error, R.color.eco_error)
