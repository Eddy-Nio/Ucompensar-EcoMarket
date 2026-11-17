package com.ucompensar.ecomarket.data.payment

/**
 * Resultado de una transacción de ePayco, obtenido del endpoint de validación con el `ref_payco`.
 *
 * Códigos de respuesta de ePayco (`x_cod_response`): 1 aceptada, 2 rechazada, 3 pendiente, 4 fallida.
 */
data class PaymentResult(
    val reference: String,
    val code: Int,
    val response: String,
    val invoice: String,
    val amount: Double,
    val transactionId: String,
    val franchise: String,
    val testMode: Boolean
) {
    val approved: Boolean get() = code == CODE_ACCEPTED
    val pending: Boolean get() = code == CODE_PENDING

    companion object {
        const val CODE_ACCEPTED = 1
        const val CODE_REJECTED = 2
        const val CODE_PENDING = 3
        const val CODE_FAILED = 4
    }
}
