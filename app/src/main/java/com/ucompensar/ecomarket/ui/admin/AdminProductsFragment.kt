package com.ucompensar.ecomarket.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.data.Approval
import com.ucompensar.ecomarket.data.Product
import com.ucompensar.ecomarket.data.Repository
import com.ucompensar.ecomarket.databinding.FragmentListBinding
import com.ucompensar.ecomarket.databinding.ItemAdminProductBinding
import com.ucompensar.ecomarket.ui.common.SimpleAdapter
import com.ucompensar.ecomarket.util.bindApproval
import com.ucompensar.ecomarket.util.toCop

/** Moderación de productos publicados: aprobar o rechazar (RF-11, RF-12). */
class AdminProductsFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!

    private val adapter = SimpleAdapter<Product, ItemAdminProductBinding>(ItemAdminProductBinding::inflate) { b, product ->
        b.txtApName.text = product.name
        b.txtApMeta.text = "${product.producer} · ${product.category} · ${product.price.toCop()}"
        b.txtApStatus.bindApproval(product.approval)
        b.btnApApprove.setOnClickListener { moderate(product, Approval.APPROVED) }
        b.btnApReject.setOnClickListener { moderate(product, Approval.REJECTED) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity?.title = getString(R.string.admin_products_title)
        binding.rvList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvList.adapter = adapter
        adapter.items = Repository.products.toList()
    }

    private fun moderate(product: Product, approval: Approval) {
        product.approval = approval
        adapter.items = Repository.products.toList()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
