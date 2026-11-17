package com.ucompensar.ecomarket.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.data.model.Approval
import com.ucompensar.ecomarket.data.model.Product
import com.ucompensar.ecomarket.data.repository.ProductRepository
import com.ucompensar.ecomarket.databinding.FragmentListBinding
import com.ucompensar.ecomarket.databinding.ItemAdminProductBinding
import com.ucompensar.ecomarket.ui.common.SimpleAdapter
import com.ucompensar.ecomarket.util.bindApproval
import com.ucompensar.ecomarket.util.collectWhileVisible
import com.ucompensar.ecomarket.util.launchSafely
import com.ucompensar.ecomarket.util.loadProductImage
import com.ucompensar.ecomarket.util.show
import com.ucompensar.ecomarket.util.toCop
import com.ucompensar.ecomarket.util.toast

/** Moderación de productos (RF-11 y RF-12): aprobar, rechazar o eliminar publicaciones. */
class AdminProductsFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!

    private val adapter = SimpleAdapter<Product, ItemAdminProductBinding>(ItemAdminProductBinding::inflate, Product::id) { b, product ->
        b.imgApThumb.loadProductImage(product.imageUrl)
        b.txtApName.text = product.name
        b.txtApMeta.text = getString(R.string.admin_product_meta, product.sellerName, product.category, product.price.toCop(), product.stock)
        b.txtApStatus.bindApproval(product.approval)
        b.btnApApprove.isEnabled = product.approval != Approval.APPROVED
        b.btnApReject.isEnabled = product.approval != Approval.REJECTED
        b.btnApApprove.setOnClickListener { launchSafely { ProductRepository.setApproval(product.id, Approval.APPROVED) } }
        b.btnApReject.setOnClickListener { launchSafely { ProductRepository.setApproval(product.id, Approval.REJECTED) } }
        b.btnApDelete.setOnClickListener { confirmDelete(product) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity?.title = getString(R.string.admin_products_title)
        binding.rvList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvList.adapter = adapter
        collectWhileVisible(ProductRepository.observeAll()) { products ->
            adapter.submitList(products)
            binding.txtListEmpty.show(products.isEmpty())
        }
    }

    private fun confirmDelete(product: Product) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirm_delete_title)
            .setMessage(getString(R.string.confirm_delete_product, product.name))
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                launchSafely {
                    ProductRepository.delete(product)
                    toast(getString(R.string.product_deleted))
                }
            }
            .show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
