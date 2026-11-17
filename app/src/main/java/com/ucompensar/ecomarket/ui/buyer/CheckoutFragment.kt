package com.ucompensar.ecomarket.ui.buyer

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.ucompensar.ecomarket.BuildConfig
import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.data.Session
import com.ucompensar.ecomarket.data.model.CartItem
import com.ucompensar.ecomarket.data.model.Pricing
import com.ucompensar.ecomarket.data.payment.EpaycoGateway
import com.ucompensar.ecomarket.data.repository.CartRepository
import com.ucompensar.ecomarket.data.repository.OrderRepository
import com.ucompensar.ecomarket.databinding.FragmentCheckoutBinding
import com.ucompensar.ecomarket.location.LocationHelper
import com.ucompensar.ecomarket.ui.MainActivity
import com.ucompensar.ecomarket.util.launchSafely
import com.ucompensar.ecomarket.util.show
import com.ucompensar.ecomarket.util.toCop
import com.ucompensar.ecomarket.util.toast

/**
 * Confirmación de compra: resumen del carrito, dirección de entrega con geolocalización y
 * apertura de la pasarela ePayco. La orden se crea pendiente de pago antes de abrir la pasarela.
 */
class CheckoutFragment : Fragment() {

    private var _binding: FragmentCheckoutBinding? = null
    private val binding get() = _binding!!
    private var cart: List<CartItem> = emptyList()
    private var latitude: Double? = null
    private var longitude: Double? = null

    /** Permiso de ubicación en tiempo de ejecución; si se concede, se lee la posición. */
    private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.any { it }) readLocation() else toast(getString(R.string.location_denied))
    }

    /** Resultado de la pantalla de pago: si se aprobó, se muestra el historial de pedidos. */
    private val paymentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            (requireActivity() as MainActivity).selectTab(R.id.nav_my_orders)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentCheckoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity?.title = getString(R.string.checkout_title)
        binding.txtTestMode.show(BuildConfig.EPAYCO_TEST)
        binding.btnLocation.setOnClickListener {
            locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
        binding.btnPay.setOnClickListener { pay() }
        launchSafely(::setBusy) {
            cart = CartRepository.items(Session.current.uid)
            renderSummary()
        }
    }

    private fun renderSummary() {
        val subtotal = cart.sumOf { it.subtotal }
        val tax = Pricing.tax(subtotal)
        binding.txtOrderSummary.text = cart.joinToString("\n") { "${it.quantity} × ${it.name} — ${it.subtotal.toCop()}" }
        binding.txtOrderTotals.text = "${getString(R.string.cart_subtotal)}: ${subtotal.toCop()}\n${getString(R.string.cart_tax)}: ${tax.toCop()}"
        binding.txtOrderTotal.text = "${getString(R.string.cart_total)}: ${(subtotal + tax).toCop()}"
        binding.btnPay.text = getString(R.string.action_pay, (subtotal + tax).toCop())
        binding.btnPay.isEnabled = cart.isNotEmpty()
    }

    /** Obtiene la ubicación con FusedLocationProvider y la convierte en dirección con Geocoder. */
    private fun readLocation() {
        binding.txtCoordinates.text = getString(R.string.location_searching)
        binding.txtCoordinates.show(true)
        launchSafely(::setBusy) {
            val place = LocationHelper(requireContext()).currentPlace()
            if (place == null) {
                binding.txtCoordinates.text = getString(R.string.location_unavailable)
                return@launchSafely
            }
            latitude = place.latitude
            longitude = place.longitude
            binding.txtCoordinates.text = getString(R.string.location_found, place.latitude, place.longitude)
            place.address?.let { binding.etAddress.setText(it) }
        }
    }

    /** Crea la orden pendiente y abre el checkout de ePayco con su número como factura. */
    private fun pay() {
        val address = binding.etAddress.text.toString().trim()
        binding.tilAddress.error = if (address.isEmpty()) getString(R.string.pay_error_address) else null
        if (address.isEmpty()) return
        if (!EpaycoGateway.isConfigured) {
            toast(getString(R.string.payment_not_configured))
            return
        }
        launchSafely(::setBusy) {
            val order = OrderRepository.createPending(Session.current, cart, address, latitude, longitude)
            paymentLauncher.launch(PaymentActivity.intent(requireContext(), order.id))
        }
    }

    private fun setBusy(busy: Boolean) {
        binding.progress.visibility = if (busy) View.VISIBLE else View.INVISIBLE
        binding.btnPay.isEnabled = !busy && cart.isNotEmpty()
        binding.btnLocation.isEnabled = !busy
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
