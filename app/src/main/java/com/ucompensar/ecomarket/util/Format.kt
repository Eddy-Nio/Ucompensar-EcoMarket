package com.ucompensar.ecomarket.util

import java.text.NumberFormat
import java.util.Locale

private val copFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
    maximumFractionDigits = 0
}

fun Int.toCop(): String = copFormat.format(this)

fun String.isValidEmail(): Boolean = android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
