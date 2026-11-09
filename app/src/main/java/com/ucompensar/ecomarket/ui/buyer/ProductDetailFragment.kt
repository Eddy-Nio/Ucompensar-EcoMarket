package com.ucompensar.ecomarket.ui.buyer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.data.Repository
import com.ucompensar.ecomarket.databinding.FragmentProductDetailBinding
import com.ucompensar.ecomarket.util.toCop
import com.ucompensar.ecomarket.util.toast

class ProductDetailFragment : Fragment() {

    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!
    private var quantity = 1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentProductDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val product = Repository.productById(requireArguments().getInt(ARG_ID)) ?: return
        activity?.title = product.name

        binding.txtDetailName.text = product.name
        binding.txtDetailCategory.text = product.category
        binding.txtDetailPrice.text = product.price.toCop()
        binding.txtDetailProducer.text = getString(R.string.detail_producer) + ": " + product.producer
        binding.txtDetailStock.text = getString(R.string.detail_stock, product.stock)
        binding.txtDetailDescription.text = product.description
        binding.btnDetailAdd.isEnabled = product.stock > 0

        binding.btnDetailMinus.setOnClickListener {
            quantity = (quantity - 1).coerceAtLeast(1)
            binding.txtDetailQty.text = quantity.toString()
        }
        binding.btnDetailPlus.setOnClickListener {
            quantity = (quantity + 1).coerceAtMost(product.stock.coerceAtLeast(1))
            binding.txtDetailQty.text = quantity.toString()
        }
        binding.btnDetailAdd.setOnClickListener {
            Repository.addToCart(product, quantity)
            toast(getString(R.string.added_to_cart, product.name))
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_ID = "product_id"

        fun newInstance(id: Int) = ProductDetailFragment().apply {
            arguments = Bundle().apply { putInt(ARG_ID, id) }
        }
    }
}
