package com.ucompensar.ecomarket.ui.seller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.data.Repository
import com.ucompensar.ecomarket.databinding.FragmentProductFormBinding
import com.ucompensar.ecomarket.util.show
import com.ucompensar.ecomarket.util.toast

/** Formulario de creación y edición de producto. Sin ID crea; con ID edita. */
class ProductFormFragment : Fragment() {

    private var _binding: FragmentProductFormBinding? = null
    private val binding get() = _binding!!
    private var productId: Int? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentProductFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        productId = arguments?.getInt(ARG_ID, NO_ID)?.takeIf { it != NO_ID }
        val product = productId?.let { Repository.productById(it) }
        activity?.title = getString(if (product == null) R.string.seller_new_product else R.string.seller_edit_product)

        binding.acPCategory.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, Repository.categories)
        )
        if (product != null) {
            binding.etPName.setText(product.name)
            binding.etPDesc.setText(product.description)
            binding.etPPrice.setText(product.price.toString())
            binding.etPStock.setText(product.stock.toString())
            binding.acPCategory.setText(product.category, false)
        }
        binding.btnPDelete.show(product != null)
        binding.btnPSave.setOnClickListener { save() }
        binding.btnPDelete.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.confirm_delete_title)
                .setMessage(getString(R.string.confirm_delete_product, product?.name.orEmpty()))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete) { _, _ ->
                    productId?.let { Repository.deleteProduct(it) }
                    toast(getString(R.string.product_deleted))
                    parentFragmentManager.popBackStack()
                }
                .show()
        }
    }

    private fun save() {
        val name = binding.etPName.text.toString().trim()
        val price = binding.etPPrice.text.toString().toIntOrNull()
        val stock = binding.etPStock.text.toString().toIntOrNull()
        val category = binding.acPCategory.text.toString().ifBlank { Repository.categories.first() }
        val required = getString(R.string.error_required)

        binding.tilPName.error = if (name.isEmpty()) required else null
        binding.tilPPrice.error = if (price == null) required else null
        binding.tilPStock.error = if (stock == null) required else null
        if (name.isEmpty() || price == null || stock == null) return

        Repository.saveProduct(productId, name, binding.etPDesc.text.toString().trim(), price, stock, category)
        toast(getString(R.string.product_saved))
        parentFragmentManager.popBackStack()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_ID = "product_id"
        private const val NO_ID = -1

        fun newInstance(id: Int?) = ProductFormFragment().apply {
            arguments = Bundle().apply { putInt(ARG_ID, id ?: NO_ID) }
        }
    }
}
