package com.ucompensar.ecomarket.ui.seller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.data.Session
import com.ucompensar.ecomarket.data.model.Order
import com.ucompensar.ecomarket.data.model.OrderStatus
import com.ucompensar.ecomarket.data.repository.OrderRepository
import com.ucompensar.ecomarket.databinding.FragmentListBinding
import com.ucompensar.ecomarket.databinding.ItemOrderBinding
import com.ucompensar.ecomarket.ui.common.OrderAction
import com.ucompensar.ecomarket.ui.common.SimpleAdapter
import com.ucompensar.ecomarket.ui.common.bindOrder
import com.ucompensar.ecomarket.util.collectWhileVisible
import com.ucompensar.ecomarket.util.launchSafely
import com.ucompensar.ecomarket.util.show

/** Pedidos pagados que incluyen productos del vendedor; permite marcarlos como enviados y entregados. */
class SellerOrdersFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!
    private val uid get() = Session.current.uid

    private val adapter = SimpleAdapter<Order, ItemOrderBinding>(ItemOrderBinding::inflate, Order::id) { b, order ->
        // El vendedor solo ve las líneas de sus propios productos
        val myLines = order.items.filter { it.sellerId == uid }.joinToString { "${it.quantity} × ${it.name}" }
        b.bindOrder(order, action = actionFor(order), itemsText = getString(R.string.seller_order_lines, myLines))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity?.title = getString(R.string.seller_orders_title)
        binding.rvList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvList.adapter = adapter
        adapter.scrollToTopOnInsert(binding.rvList)
        collectWhileVisible(OrderRepository.observeBySeller(uid)) { orders ->
            adapter.submitList(orders)
            binding.txtListEmpty.show(orders.isEmpty())
        }
    }

    /** Botón según el estado: pagado → enviar; enviado → entregar; entregado → sin acción. */
    private fun actionFor(order: Order): OrderAction? {
        val label = when (order.status) {
            OrderStatus.PAID -> R.string.action_mark_shipped
            OrderStatus.SHIPPED -> R.string.action_mark_delivered
            else -> return null
        }
        return OrderAction(label) { launchSafely { OrderRepository.advance(it) } }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
