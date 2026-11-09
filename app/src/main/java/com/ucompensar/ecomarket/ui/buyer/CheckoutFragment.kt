package com.ucompensar.ecomarket.ui.buyer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.data.Repository
import com.ucompensar.ecomarket.databinding.FragmentCheckoutBinding
import com.ucompensar.ecomarket.ui.MainActivity
import com.ucompensar.ecomarket.util.show
import com.ucompensar.ecomarket.util.toCop
import com.ucompensar.ecomarket.util.toast

/** Pasarela de pagos simulada: tarjeta, PSE o efectivo (RF-27, RF-28). Nunca se almacena la tarjeta. */
class CheckoutFragment : Fragment() {

    private var _binding: FragmentCheckoutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentCheckoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity?.title = getString(R.string.checkout_title)
        binding.txtOrderSummary.text = Repository.cart.joinToString("\n") {
            "${it.quantity} × ${it.product.name} — ${it.subtotal.toCop()}"
        } + "\n\n" + getString(R.string.cart_total) + ": " + Repository.cartTotal().toCop()
        binding.btnPay.text = getString(R.string.action_pay, Repository.cartTotal().toCop())

        binding.chipPayment.setOnCheckedStateChangeListener { _, _ ->
            binding.panelCard.show(binding.chipPayment.checkedChipId == R.id.chipCard)
        }
        binding.btnPay.setOnClickListener { pay() }
    }

    private fun pay() {
        binding.tilAddress.error = null
        binding.tilCard.error = null
        val cardSelected = binding.chipPayment.checkedChipId == R.id.chipCard
        when {
            Repository.cart.isEmpty() -> return
            binding.etAddress.text.isNullOrBlank() ->
                binding.tilAddress.error = getString(R.string.pay_error_address)
            cardSelected && binding.etCard.text.toString().length != 16 ->
                binding.tilCard.error = getString(R.string.pay_error_card)
            else -> {
                val order = Repository.checkout()
                toast(getString(R.string.pay_success, order.id))
                (requireActivity() as MainActivity).selectTab(R.id.nav_home)
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
