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
import com.ucompensar.ecomarket.data.model.CATEGORIES
import com.ucompensar.ecomarket.data.model.Product
import com.ucompensar.ecomarket.data.repository.ProductRepository
import com.ucompensar.ecomarket.databinding.FragmentCatalogBinding
import com.ucompensar.ecomarket.util.collectWhileVisible
import com.ucompensar.ecomarket.util.show

/** Catálogo con búsqueda por texto y filtro por categoría (RF-02) sobre los productos aprobados. */
class CatalogFragment : Fragment() {

    private var _binding: FragmentCatalogBinding? = null
    private val binding get() = _binding!!
    private var category: String? = null
    private var allProducts: List<Product> = emptyList()
    private val adapter by lazy { buyerProductAdapter() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentCatalogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity?.title = getString(R.string.catalog_title)
        category = arguments?.getString(ARG_CATEGORY)

        binding.rvCatalog.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvCatalog.adapter = adapter

        (listOf<String?>(null) + CATEGORIES).forEach { cat ->
            val chip = Chip(ContextThemeWrapper(requireContext(), R.style.Chip_Eco)).apply {
                id = View.generateViewId()
                text = cat ?: getString(R.string.category_all)
                isCheckable = true
                isChecked = cat == category
                setOnClickListener {
                    category = cat
                    applyFilters()
                }
            }
            binding.chipFilters.addView(chip)
        }
        binding.etSearch.doAfterTextChanged { applyFilters() }

        collectWhileVisible(ProductRepository.observeApproved()) {
            allProducts = it
            applyFilters()
        }
    }

    /** Filtra en memoria por categoría y por coincidencia en nombre, productor o categoría. */
    private fun applyFilters() {
        val query = binding.etSearch.text.toString().trim()
        val items = allProducts.filter { p ->
            (category == null || p.category == category) &&
                (query.isEmpty() || listOf(p.name, p.sellerName, p.category).any { it.contains(query, ignoreCase = true) })
        }
        adapter.submitList(items)
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
