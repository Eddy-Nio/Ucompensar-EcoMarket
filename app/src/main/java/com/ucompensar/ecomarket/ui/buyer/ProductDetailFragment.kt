package com.ucompensar.ecomarket.ui.buyer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.data.Session
import com.ucompensar.ecomarket.data.model.Product
import com.ucompensar.ecomarket.data.repository.CartRepository
import com.ucompensar.ecomarket.data.repository.ProductRepository
import com.ucompensar.ecomarket.databinding.FragmentProductDetailBinding
import com.ucompensar.ecomarket.util.launchSafely
import com.ucompensar.ecomarket.util.loadProductImage
import com.ucompensar.ecomarket.util.toCop
import com.ucompensar.ecomarket.util.toast

/** Detalle de producto con foto, descripción, stock y selector de cantidad. */
class ProductDetailFragment : Fragment() {

    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!
    private var quantity = 1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentProductDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        launchSafely {
            ProductRepository.get(requireArguments().getString(ARG_ID).orEmpty())?.let(::bind)
        }
    }

    private fun bind(product: Product) {
        activity?.title = product.name
        binding.imgDetail.loadProductImage(product.imageUrl)
        binding.txtDetailName.text = product.name
        binding.txtDetailCategory.text = product.category
        binding.txtDetailPrice.text = product.price.toCop()
        binding.txtDetailProducer.text = getString(R.string.detail_producer, product.sellerName)
        binding.txtDetailStock.text = getString(R.string.detail_stock, product.stock)
        binding.txtDetailDescription.text = product.description
        binding.btnDetailAdd.isEnabled = product.stock > 0

        // La cantidad se limita al stock disponible
        binding.btnDetailMinus.setOnClickListener { updateQuantity(quantity - 1, product.stock) }
        binding.btnDetailPlus.setOnClickListener { updateQuantity(quantity + 1, product.stock) }
        binding.btnDetailAdd.setOnClickListener {
            launchSafely {
                CartRepository.add(Session.current.uid, product, quantity)
                toast(getString(R.string.added_to_cart, product.name))
            }
        }
    }

    private fun updateQuantity(value: Int, stock: Int) {
        quantity = value.coerceIn(1, stock.coerceAtLeast(1))
        binding.txtDetailQty.text = quantity.toString()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_ID = "product_id"

        fun newInstance(id: String) = ProductDetailFragment().apply {
            arguments = Bundle().apply { putString(ARG_ID, id) }
        }
    }
}
