package com.ucompensar.ecomarket.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.data.Session
import com.ucompensar.ecomarket.data.model.Approval
import com.ucompensar.ecomarket.data.model.AuditLog
import com.ucompensar.ecomarket.data.model.Role
import com.ucompensar.ecomarket.data.model.Order
import com.ucompensar.ecomarket.data.repository.OrderRepository
import com.ucompensar.ecomarket.data.repository.ProductRepository
import com.ucompensar.ecomarket.data.repository.UserRepository
import com.ucompensar.ecomarket.databinding.FragmentAdminDashboardBinding
import com.ucompensar.ecomarket.databinding.ItemOrderBinding
import com.ucompensar.ecomarket.databinding.ViewKpiBinding
import com.ucompensar.ecomarket.ui.MainActivity
import com.ucompensar.ecomarket.ui.common.SimpleAdapter
import com.ucompensar.ecomarket.ui.common.bindOrder
import com.ucompensar.ecomarket.util.collectWhileVisible
import com.ucompensar.ecomarket.util.show
import com.ucompensar.ecomarket.util.toCop
import com.ucompensar.ecomarket.util.toShortDate

/** Dashboard del administrador: indicadores en tiempo real calculados desde Firestore y bitácora de auditoría. */
class AdminDashboardFragment : Fragment() {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!
    private val recentOrders = SimpleAdapter<Order, ItemOrderBinding>(ItemOrderBinding::inflate, Order::id) { b, order -> b.bindOrder(order) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity?.title = getString(R.string.admin_dashboard_title)
        val main = requireActivity() as MainActivity
        binding.txtGreeting.text = getString(R.string.home_greeting, Session.current.name)
        binding.btnGoUsers.setOnClickListener { main.selectTab(R.id.nav_users) }
        binding.btnGoReport.setOnClickListener { main.selectTab(R.id.nav_report) }
        binding.rvRecentOrders.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecentOrders.adapter = recentOrders

        // Cada indicador escucha su colección; se actualiza solo cuando hay cambios
        collectWhileVisible(OrderRepository.observeAll()) { orders ->
            val sales = orders.filter { it.status.isSale }
            bindKpi(binding.kpiSales, R.string.kpi_sales, sales.sumOf { it.total }.toCop())
            bindKpi(binding.kpiOrders, R.string.kpi_orders, sales.size.toString())
            recentOrders.submitList(orders.take(RECENT_ORDERS))
        }
        collectWhileVisible(UserRepository.observeAll()) { users ->
            bindKpi(binding.kpiUsers, R.string.kpi_users, users.size.toString())
        }
        collectWhileVisible(UserRepository.observeRecentAudit()) { logs -> renderAudit(logs) }
        collectWhileVisible(ProductRepository.observeAll()) { products ->
            bindKpi(binding.kpiProducts, R.string.kpi_products, products.size.toString())
            bindKpi(binding.kpiPending, R.string.kpi_pending_products, products.count { it.approval == Approval.PENDING }.toString())
        }
    }

    /** Muestra las últimas acciones administrativas registradas en la bitácora. */
    private fun renderAudit(logs: List<AuditLog>) {
        binding.txtAuditEmpty.show(logs.isEmpty())
        binding.txtAudit.show(logs.isNotEmpty())
        binding.txtAudit.text = logs.joinToString("\n\n") { log ->
            val action = getString(log.action.labelRes, log.targetName)
            val roles = if (log.fromRole.isNotBlank() && log.toRole.isNotBlank())
                " " + getString(R.string.audit_roles, roleLabel(log.fromRole), roleLabel(log.toRole)) else ""
            getString(R.string.audit_line, log.createdAt.toShortDate(), log.actorName, action) + roles
        }
    }

    private fun roleLabel(name: String) = getString(Role.valueOf(name).labelRes)

    private fun bindKpi(kpi: ViewKpiBinding, label: Int, value: String) {
        kpi.txtKpiLabel.setText(label)
        kpi.txtKpiValue.text = value
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val RECENT_ORDERS = 4
    }
}
