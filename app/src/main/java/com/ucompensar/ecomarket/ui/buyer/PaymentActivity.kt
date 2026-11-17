package com.ucompensar.ecomarket.ui.buyer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.data.Session
import com.ucompensar.ecomarket.data.model.Order
import com.ucompensar.ecomarket.data.payment.EpaycoGateway
import com.ucompensar.ecomarket.data.payment.PaymentResult
import com.ucompensar.ecomarket.data.repository.OrderRepository
import com.ucompensar.ecomarket.databinding.ActivityPaymentBinding
import com.ucompensar.ecomarket.util.launchSafely
import com.ucompensar.ecomarket.util.show
import com.ucompensar.ecomarket.util.toCop
import com.ucompensar.ecomarket.util.toast
import kotlin.math.abs

/**
 * Pasarela de pagos ePayco dentro de un WebView.
 *
 * Carga el checkout oficial con los datos de la orden, intercepta la redirección a la URL de
 * respuesta para obtener el `ref_payco`, valida la transacción contra ePayco y registra el
 * resultado en Firestore. Devuelve RESULT_OK solo si el pago fue aprobado.
 */
class PaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentBinding
    private lateinit var order: Order
    private var handled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { cancel() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = cancel()
        })

        launchSafely {
            order = OrderRepository.get(intent.getStringExtra(EXTRA_ORDER_ID).orEmpty()) ?: run {
                finish()
                return@launchSafely
            }
            loadCheckout()
        }
    }

    /** Configura el WebView y carga la plantilla del checkout con los datos de la orden. */
    @SuppressLint("SetJavaScriptEnabled")
    private fun loadCheckout() {
        val web = binding.webCheckout
        web.settings.javaScriptEnabled = true // checkout.js de ePayco lo requiere
        web.settings.domStorageEnabled = true
        web.addJavascriptInterface(CheckoutBridge(), BRIDGE_NAME)
        web.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                binding.progress.show(newProgress < FULL_PROGRESS)
            }
        }
        web.webViewClient = object : WebViewClient() {
            // ePayco redirige a RESPONSE_URL?ref_payco=...; esa navegación se intercepta aquí
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val reference = EpaycoGateway.referenceFrom(request.url.toString()) ?: return false
                onReference(reference)
                return true
            }
        }
        val html = EpaycoGateway.checkoutPage(this, order, Session.current.phone)
        web.loadDataWithBaseURL(EpaycoGateway.BASE_URL, html, "text/html", "UTF-8", null)
    }

    /** Valida la referencia con ePayco, comprueba factura y monto, y guarda el resultado. */
    private fun onReference(reference: String) {
        if (handled) return
        handled = true
        binding.panelValidating.show(true)
        launchSafely {
            val result = EpaycoGateway.validate(reference)
            val matchesOrder = result.invoice == order.id && abs(result.amount - order.total) < AMOUNT_TOLERANCE
            if (!matchesOrder) {
                showResult(getString(R.string.payment_rejected_title), getString(R.string.payment_mismatch), approved = false)
                return@launchSafely
            }
            OrderRepository.applyPayment(order, result)
            showResult(titleFor(result), detailFor(result), result.approved)
        }.invokeOnCompletion { error ->
            if (error != null) handled = false
        }
    }

    private fun titleFor(result: PaymentResult) = getString(
        when {
            result.approved -> R.string.payment_approved_title
            result.pending -> R.string.payment_pending_title
            else -> R.string.payment_rejected_title
        }
    )

    private fun detailFor(result: PaymentResult) = getString(
        R.string.payment_detail, order.shortId, result.reference, result.transactionId, result.response, order.total.toCop()
    )

    /** Muestra el comprobante y vuelve al checkout indicando si el pago se aprobó. */
    private fun showResult(title: String, message: String, approved: Boolean) {
        binding.panelValidating.show(false)
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setIcon(if (approved) R.drawable.ic_check else R.drawable.ic_payment)
            .setCancelable(false)
            .setPositiveButton(R.string.action_see_orders) { _, _ ->
                setResult(if (approved) RESULT_OK else RESULT_CANCELED)
                finish()
            }
            .show()
    }

    /** El usuario salió sin pagar: la orden queda pendiente y se puede pagar desde Mis pedidos. */
    private fun cancel() {
        if (handled) return
        toast(getString(R.string.payment_closed))
        setResult(RESULT_CANCELED)
        finish()
    }

    /** Puente JavaScript → Kotlin para los eventos de la página del checkout. */
    private inner class CheckoutBridge {
        @JavascriptInterface
        fun onClosed() = Unit // El usuario puede reabrir el pago desde la misma página

        @JavascriptInterface
        fun onError(message: String) = runOnUiThread {
            toast(message)
            binding.panelValidating.show(false)
        }
    }

    override fun onDestroy() {
        if (::binding.isInitialized) binding.webCheckout.destroy()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_ORDER_ID = "order_id"
        private const val BRIDGE_NAME = "Android"
        private const val FULL_PROGRESS = 100
        private const val AMOUNT_TOLERANCE = 1.0

        fun intent(context: Context, orderId: String) =
            Intent(context, PaymentActivity::class.java).putExtra(EXTRA_ORDER_ID, orderId)
    }
}
