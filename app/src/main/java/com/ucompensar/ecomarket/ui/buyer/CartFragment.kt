package com.ucompensar.ecomarket.ui.buyer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.data.Session
import com.ucompensar.ecomarket.data.model.CartItem
import com.ucompensar.ecomarket.data.model.Pricing
import com.ucompensar.ecomarket.data.repository.CartRepository
import com.ucompensar.ecomarket.databinding.FragmentCartBinding
import com.ucompensar.ecomarket.databinding.ItemCartBinding
import com.ucompensar.ecomarket.ui.MainActivity
import com.ucompensar.ecomarket.ui.common.SimpleAdapter
import com.ucompensar.ecomarket.util.collectWhileVisible
import com.ucompensar.ecomarket.util.launchSafely
import com.ucompensar.ecomarket.util.loadProductImage
import com.ucompensar.ecomarket.util.show
import com.ucompensar.ecomarket.util.toCop

/** Carrito persistido en Firestore: cantidades, eliminación y totales con IVA (RF-23 a RF-26). */
class CartFragment : Fragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!
    private val uid get() = Session.current.uid

    private val adapter = SimpleAdapter<CartItem, ItemCartBinding>(ItemCartBinding::inflate, CartItem::productId) { b, line ->
        b.imgCartThumb.loadProductImage(line.imageUrl)
        b.txtCartName.text = line.name
        b.txtCartUnit.text = line.price.toCop()
        b.txtCartSubtotal.text = line.subtotal.toCop()
        b.txtQty.text = line.quantity.toString()
        b.btnMinus.setOnClickListener { launchSafely { CartRepository.setQuantity(uid, line, line.quantity - 1) } }
        b.btnPlus.setOnClickListener { launchSafely { CartRepository.setQuantity(uid, line, line.quantity + 1) } }
        b.btnRemove.setOnClickListener { launchSafely { CartRepository.remove(uid, line.productId) } }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity?.title = getString(R.string.cart_title)
        binding.rvCart.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCart.adapter = adapter
        binding.btnClear.setOnClickListener { launchSafely { CartRepository.clear(uid) } }
        binding.btnCheckout.setOnClickListener { (requireActivity() as MainActivity).open(CheckoutFragment()) }
        // Cada cambio en Firestore (incluso desde otro dispositivo) refresca la lista y los totales
        collectWhileVisible(CartRepository.observe(uid)) { items -> render(items) }
    }

    private fun render(items: List<CartItem>) {
        adapter.submitList(items)
        val subtotal = items.sumOf { it.subtotal }
        val tax = Pricing.tax(subtotal)
        binding.txtCartEmpty.show(items.isEmpty())
        binding.panelSummary.show(items.isNotEmpty())
        binding.txtSubtotal.text = subtotal.toCop()
        binding.txtTax.text = tax.toCop()
        binding.txtTotal.text = (subtotal + tax).toCop()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
