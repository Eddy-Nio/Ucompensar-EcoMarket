package com.ucompensar.ecomarket.data.payment

import android.content.Context
import com.ucompensar.ecomarket.BuildConfig
import com.ucompensar.ecomarket.data.model.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Integración con la pasarela de pagos ePayco (Checkout web).
 *
 * Flujo:
 * 1. [checkoutPage] arma una página HTML que carga `checkout.js` de ePayco con los datos de la orden.
 * 2. La pantalla de pago la muestra en un WebView; el usuario paga con tarjeta, PSE o efectivo.
 * 3. ePayco redirige a [RESPONSE_URL]`?ref_payco=...`; el WebView intercepta esa URL.
 * 4. [validate] consulta el estado real de la transacción en los servidores de ePayco.
 */
object EpaycoGateway {

    /** URL de respuesta. No existe: el WebView la intercepta antes de cargarla. */
    const val RESPONSE_URL = "https://ecomarket.app/pago/respuesta"
    const val BASE_URL = "https://ecomarket.app/"
    private const val VALIDATION_URL = "https://secure.epayco.co/validation/v1/reference/"
    private const val TEMPLATE = "epayco_checkout.html"
    private const val TIMEOUT_MS = 15_000

    /** Indica si hay una llave pública configurada en gradle.properties. */
    val isConfigured: Boolean get() = BuildConfig.EPAYCO_PUBLIC_KEY.isNotBlank()

    /** Construye el HTML del checkout a partir de la plantilla en `assets/` y los datos de la orden. */
    fun checkoutPage(context: Context, order: Order, phone: String): String {
        val data = JSONObject().apply {
            put("name", "Compra EcoMarket #${order.shortId}")
            put("description", order.summary.take(MAX_DESCRIPTION))
            put("invoice", order.id)
            put("currency", "cop")
            put("amount", order.total.toString())
            put("tax_base", order.subtotal.toString())
            put("tax", order.tax.toString())
            put("tax_ico", "0")
            put("country", "co")
            put("lang", "es")
            put("external", "false") // checkout dentro de la misma página (onpage)
            put("response", RESPONSE_URL)
            put("method", "GET")
            put("extra1", order.buyerId)
            put("name_billing", order.buyerName)
            put("email_billing", order.buyerEmail)
            put("address_billing", order.address)
            put("mobilephone_billing", phone)
        }
        val template = context.assets.open(TEMPLATE).bufferedReader().use { it.readText() }
        return template
            .replace("__KEY__", JSONObject.quote(BuildConfig.EPAYCO_PUBLIC_KEY))
            .replace("__TEST__", BuildConfig.EPAYCO_TEST.toString())
            .replace("__DATA__", data.toString())
    }

    /** Extrae el `ref_payco` si [url] es la URL de respuesta configurada. */
    fun referenceFrom(url: String): String? {
        if (!url.startsWith(RESPONSE_URL)) return null
        return android.net.Uri.parse(url).let { it.getQueryParameter("ref_payco") ?: it.getQueryParameter("x_ref_payco") }
    }

    /**
     * Consulta la transacción en ePayco. No se confía en los parámetros de la URL de respuesta
     * (el usuario podría alterarlos): el estado, el monto y la factura se leen del servidor de ePayco.
     */
    suspend fun validate(reference: String): PaymentResult = withContext(Dispatchers.IO) {
        val connection = (URL(VALIDATION_URL + reference).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
        }
        try {
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val d = json.optJSONObject("data") ?: error(json.optString("text_response", "Respuesta inválida de ePayco"))
            val response = d.optString("x_response")
            PaymentResult(
                reference = d.optString("x_ref_payco", reference),
                code = d.optString("x_cod_response").toIntOrNull() ?: codeFromText(response),
                response = response,
                invoice = d.optString("x_id_invoice"),
                amount = d.optString("x_amount").toDoubleOrNull() ?: 0.0,
                transactionId = d.optString("x_transaction_id"),
                franchise = d.optString("x_franchise"),
                testMode = d.optString("x_test_request").equals("TRUE", ignoreCase = true)
            )
        } finally {
            connection.disconnect()
        }
    }

    /** Respaldo por si la respuesta solo trae el texto del estado. */
    private fun codeFromText(text: String): Int = when (text.lowercase()) {
        "aceptada" -> PaymentResult.CODE_ACCEPTED
        "pendiente" -> PaymentResult.CODE_PENDING
        "rechazada" -> PaymentResult.CODE_REJECTED
        else -> PaymentResult.CODE_FAILED
    }

    private const val MAX_DESCRIPTION = 200
}
