package com.ucompensar.ecomarket.ui.buyer

import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.data.Session
import com.ucompensar.ecomarket.data.model.CATEGORIES
import com.ucompensar.ecomarket.data.repository.ProductRepository
import com.ucompensar.ecomarket.databinding.FragmentHomeBinding
import com.ucompensar.ecomarket.ui.MainActivity
import com.ucompensar.ecomarket.util.collectWhileVisible

/** Inicio del comprador (RF-01): banner, accesos por categoría y productos recomendados. */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity?.title = getString(R.string.home_greeting, Session.current.name.substringBefore(' '))
        val main = requireActivity() as MainActivity

        binding.searchBar.setOnClickListener { main.selectTab(R.id.nav_catalog) }
        binding.txtSeeOffers.setOnClickListener { main.selectTab(R.id.nav_catalog) }
        (listOf<String?>(null) + CATEGORIES).forEach { category ->
            val chip = Chip(ContextThemeWrapper(requireContext(), R.style.Chip_Eco)).apply {
                text = category ?: getString(R.string.category_all)
                isCheckable = false
                setOnClickListener { main.open(CatalogFragment.newInstance(category)) }
            }
            binding.chipCategories.addView(chip)
        }

        val adapter = buyerProductAdapter()
        binding.rvRecommended.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecommended.adapter = adapter
        // Recomendados: productos aprobados con stock, en tiempo real desde Firestore
        collectWhileVisible(ProductRepository.observeApproved()) { products ->
            adapter.submitList(products.filter { it.stock > 0 }.take(RECOMMENDED))
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val RECOMMENDED = 6
    }
}
