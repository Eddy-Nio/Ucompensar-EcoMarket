package com.ucompensar.ecomarket.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.data.Order
import com.ucompensar.ecomarket.data.Repository
import com.ucompensar.ecomarket.databinding.FragmentAdminDashboardBinding
import com.ucompensar.ecomarket.databinding.ItemOrderBinding
import com.ucompensar.ecomarket.databinding.ViewKpiBinding
import com.ucompensar.ecomarket.ui.MainActivity
import com.ucompensar.ecomarket.ui.common.SimpleAdapter
import com.ucompensar.ecomarket.ui.common.bindOrder
import com.ucompensar.ecomarket.util.toCop

/** Dashboard principal del administrador con indicadores y accesos rápidos. */
class AdminDashboardFragment : Fragment() {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity?.title = getString(R.string.admin_dashboard_title)
        val main = requireActivity() as MainActivity
        binding.txtGreeting.text = "Hola, " + Repository.currentUser.name

        bindKpi(binding.kpiSales, R.string.kpi_sales, Repository.totalSales().toCop())
        bindKpi(binding.kpiOrders, R.string.kpi_orders, Repository.orders.size.toString())
        bindKpi(binding.kpiUsers, R.string.kpi_users, Repository.users.size.toString())
        bindKpi(binding.kpiProducts, R.string.kpi_products, Repository.products.size.toString())

        binding.btnGoUsers.setOnClickListener { main.selectTab(R.id.nav_users) }
        binding.btnGoReport.setOnClickListener { main.selectTab(R.id.nav_report) }

        val adapter = SimpleAdapter<Order, ItemOrderBinding>(ItemOrderBinding::inflate) { b, order -> b.bindOrder(order) }
        adapter.items = Repository.orders.take(4)
        binding.rvRecentOrders.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecentOrders.adapter = adapter
    }

    private fun bindKpi(kpi: ViewKpiBinding, label: Int, value: String) {
        kpi.txtKpiLabel.setText(label)
        kpi.txtKpiValue.text = value
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
