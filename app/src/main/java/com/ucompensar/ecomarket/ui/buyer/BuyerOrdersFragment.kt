package com.ucompensar.ecomarket.ui.buyer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
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
import com.ucompensar.ecomarket.util.show

/** Historial de compras del comprador (RF-33), con opción de pagar pedidos pendientes. */
class BuyerOrdersFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!

    private val paymentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    private val retryPayment = OrderAction(R.string.action_retry_payment) { order ->
        paymentLauncher.launch(PaymentActivity.intent(requireContext(), order.id))
    }

    private val adapter = SimpleAdapter<Order, ItemOrderBinding>(ItemOrderBinding::inflate, Order::id) { b, order ->
        b.bindOrder(order, action = retryPayment.takeIf { order.status == OrderStatus.PENDING_PAYMENT })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity?.title = getString(R.string.my_orders_title)
        binding.rvList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvList.adapter = adapter
        adapter.scrollToTopOnInsert(binding.rvList)
        collectWhileVisible(OrderRepository.observeByBuyer(Session.current.uid)) { orders ->
            adapter.submitList(orders)
            binding.txtListEmpty.show(orders.isEmpty())
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
