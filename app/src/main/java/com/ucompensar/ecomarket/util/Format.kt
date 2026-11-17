package com.ucompensar.ecomarket.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val colombia = Locale("es", "CO")

private val copFormat: NumberFormat = NumberFormat.getCurrencyInstance(colombia).apply {
    maximumFractionDigits = 0
}

private val dateFormat = SimpleDateFormat("d MMM yyyy, h:mm a", colombia)

/** Formatea un valor entero como pesos colombianos, por ejemplo $ 12.500. */
fun Int.toCop(): String = copFormat.format(this)

/** Fecha corta en español, o un guion si Firestore aún no asignó la marca de tiempo. */
fun Date?.toShortDate(): String = this?.let(dateFormat::format) ?: "—"

fun String.isValidEmail(): Boolean = android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
