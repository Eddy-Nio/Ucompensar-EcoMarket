package com.ucompensar.ecomarket.ui.buyer

import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.chip.Chip
import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.data.Product
import com.ucompensar.ecomarket.data.Repository
import com.ucompensar.ecomarket.databinding.FragmentCatalogBinding
import com.ucompensar.ecomarket.databinding.ItemProductBinding
import com.ucompensar.ecomarket.ui.MainActivity
import com.ucompensar.ecomarket.ui.common.SimpleAdapter
import com.ucompensar.ecomarket.util.show
import com.ucompensar.ecomarket.util.toast

/** Catálogo con búsqueda por texto y filtro por categoría (RF-02). */
class CatalogFragment : Fragment() {

    private var _binding: FragmentCatalogBinding? = null
    private val binding get() = _binding!!
    private var category: String? = null

    private val adapter = SimpleAdapter<Product, ItemProductBinding>(ItemProductBinding::inflate) { b, product ->
        val main = requireActivity() as MainActivity
        b.bindProduct(
            product,
            onOpen = { main.open(ProductDetailFragment.newInstance(it.id)) },
            onAdd = {
                Repository.addToCart(it)
                toast(getString(R.string.added_to_cart, it.name))
            }
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentCatalogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity?.title = getString(R.string.catalog_title)
        category = arguments?.getString(ARG_CATEGORY)

        binding.rvCatalog.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvCatalog.adapter = adapter

        (listOf<String?>(null) + Repository.categories).forEach { cat ->
            val chip = Chip(ContextThemeWrapper(requireContext(), R.style.Chip_Eco)).apply {
                id = View.generateViewId()
                text = cat ?: getString(R.string.category_all)
                isCheckable = true
                isChecked = cat == category
                setOnClickListener {
                    category = cat
                    refresh()
                }
            }
            binding.chipFilters.addView(chip)
        }
        binding.etSearch.doAfterTextChanged { refresh() }
        refresh()
    }

    private fun refresh() {
        val items = Repository.approvedProducts(binding.etSearch.text.toString().trim(), category)
        adapter.items = items
        binding.txtEmpty.show(items.isEmpty())
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_CATEGORY = "category"

        fun newInstance(category: String?) = CatalogFragment().apply {
            arguments = Bundle().apply { putString(ARG_CATEGORY, category) }
        }
    }
}
