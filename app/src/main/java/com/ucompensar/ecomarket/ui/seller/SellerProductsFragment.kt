package com.ucompensar.ecomarket.ui.seller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.data.Product
import com.ucompensar.ecomarket.data.Repository
import com.ucompensar.ecomarket.databinding.FragmentSellerProductsBinding
import com.ucompensar.ecomarket.databinding.ItemSellerProductBinding
import com.ucompensar.ecomarket.ui.MainActivity
import com.ucompensar.ecomarket.ui.common.SimpleAdapter
import com.ucompensar.ecomarket.util.bindApproval
import com.ucompensar.ecomarket.util.toCop
import com.ucompensar.ecomarket.util.toast

/** Panel de productos del vendedor con CRUD completo (RF-18 a RF-22). */
class SellerProductsFragment : Fragment() {

    private var _binding: FragmentSellerProductsBinding? = null
    private val binding get() = _binding!!

    private val adapter = SimpleAdapter<Product, ItemSellerProductBinding>(ItemSellerProductBinding::inflate) { b, product ->
        b.txtSpName.text = product.name
        b.txtSpMeta.text = "${product.price.toCop()} · ${getString(R.string.detail_stock, product.stock)}"
        b.txtSpStatus.bindApproval(product.approval)
        b.btnSpEdit.setOnClickListener { (requireActivity() as MainActivity).open(ProductFormFragment.newInstance(product.id)) }
        b.btnSpDelete.setOnClickListener { confirmDelete(product) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentSellerProductsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity?.title = getString(R.string.seller_panel_title)
        binding.rvSellerProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSellerProducts.adapter = adapter
        binding.fabNewProduct.setOnClickListener { (requireActivity() as MainActivity).open(ProductFormFragment.newInstance(null)) }
        refresh()
    }

    private fun refresh() {
        adapter.items = Repository.products.toList()
    }

    private fun confirmDelete(product: Product) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirm_delete_title)
            .setMessage(getString(R.string.confirm_delete_product, product.name))
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                Repository.deleteProduct(product.id)
                toast(getString(R.string.product_deleted))
                refresh()
            }
            .show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
