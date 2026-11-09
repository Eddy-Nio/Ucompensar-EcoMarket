package com.ucompensar.ecomarket.ui.seller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.data.Order
import com.ucompensar.ecomarket.data.Repository
import com.ucompensar.ecomarket.databinding.FragmentListBinding
import com.ucompensar.ecomarket.databinding.ItemOrderBinding
import com.ucompensar.ecomarket.ui.common.SimpleAdapter
import com.ucompensar.ecomarket.ui.common.bindOrder
import com.ucompensar.ecomarket.util.show

/** Historial de pedidos recibidos; permite avanzar el estado de cada pedido. */
class SellerOrdersFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!

    private val adapter = SimpleAdapter<Order, ItemOrderBinding>(ItemOrderBinding::inflate) { b, order ->
        b.bindOrder(order) {
            it.status = it.status.next()
            refresh()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity?.title = getString(R.string.seller_orders_title)
        binding.rvList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvList.adapter = adapter
        refresh()
    }

    private fun refresh() {
        adapter.items = Repository.orders.toList()
        binding.txtListEmpty.show(Repository.orders.isEmpty())
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
