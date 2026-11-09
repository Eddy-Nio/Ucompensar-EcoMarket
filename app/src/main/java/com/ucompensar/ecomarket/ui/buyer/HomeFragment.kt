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
import com.ucompensar.ecomarket.data.Product
import com.ucompensar.ecomarket.data.Repository
import com.ucompensar.ecomarket.databinding.FragmentHomeBinding
import com.ucompensar.ecomarket.databinding.ItemProductBinding
import com.ucompensar.ecomarket.ui.MainActivity
import com.ucompensar.ecomarket.ui.common.SimpleAdapter
import com.ucompensar.ecomarket.util.toast

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity?.title = getString(R.string.app_name)
        val main = requireActivity() as MainActivity

        binding.searchBar.setOnClickListener { main.selectTab(R.id.nav_catalog) }
        binding.txtSeeOffers.setOnClickListener { main.selectTab(R.id.nav_catalog) }

        (listOf<String?>(null) + Repository.categories).forEach { category ->
            val chip = Chip(ContextThemeWrapper(requireContext(), R.style.Chip_Eco)).apply {
                text = category ?: getString(R.string.category_all)
                isCheckable = false
                setOnClickListener { main.open(CatalogFragment.newInstance(category)) }
            }
            binding.chipCategories.addView(chip)
        }

        val adapter = SimpleAdapter<Product, ItemProductBinding>(ItemProductBinding::inflate) { b, product ->
            b.bindProduct(
                product,
                onOpen = { main.open(ProductDetailFragment.newInstance(it.id)) },
                onAdd = {
                    Repository.addToCart(it)
                    toast(getString(R.string.added_to_cart, it.name))
                }
            )
        }
        adapter.items = Repository.approvedProducts().take(6)
        binding.rvRecommended.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecommended.adapter = adapter
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
