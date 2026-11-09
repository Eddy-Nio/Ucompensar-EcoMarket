package com.ucompensar.ecomarket.ui.buyer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.data.CartItem
import com.ucompensar.ecomarket.data.Repository
import com.ucompensar.ecomarket.databinding.FragmentCartBinding
import com.ucompensar.ecomarket.databinding.ItemCartBinding
import com.ucompensar.ecomarket.ui.MainActivity
import com.ucompensar.ecomarket.ui.common.SimpleAdapter
import com.ucompensar.ecomarket.util.show
import com.ucompensar.ecomarket.util.toCop

/** Carrito: listar, modificar cantidades, eliminar y totales con IVA (RF-23 a RF-26). */
class CartFragment : Fragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!

    private val adapter = SimpleAdapter<CartItem, ItemCartBinding>(ItemCartBinding::inflate) { b, line ->
        b.txtCartName.text = line.product.name
        b.txtCartUnit.text = line.product.price.toCop()
        b.txtCartSubtotal.text = line.subtotal.toCop()
        b.txtQty.text = line.quantity.toString()
        b.btnMinus.setOnClickListener {
            if (line.quantity > 1) line.quantity-- else Repository.cart.remove(line)
            refresh()
        }
        b.btnPlus.setOnClickListener {
            if (line.quantity < line.product.stock) line.quantity++
            refresh()
        }
        b.btnRemove.setOnClickListener {
            Repository.cart.remove(line)
            refresh()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity?.title = getString(R.string.cart_title)
        binding.rvCart.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCart.adapter = adapter
        binding.btnClear.setOnClickListener {
            Repository.cart.clear()
            refresh()
        }
        binding.btnCheckout.setOnClickListener { (requireActivity() as MainActivity).open(CheckoutFragment()) }
        refresh()
    }

    private fun refresh() {
        val items = Repository.cart.toList()
        adapter.items = items
        val empty = items.isEmpty()
        binding.txtCartEmpty.show(empty)
        binding.panelSummary.show(!empty)
        binding.txtSubtotal.text = Repository.cartSubtotal().toCop()
        binding.txtTax.text = Repository.cartTax().toCop()
        binding.txtTotal.text = Repository.cartTotal().toCop()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
