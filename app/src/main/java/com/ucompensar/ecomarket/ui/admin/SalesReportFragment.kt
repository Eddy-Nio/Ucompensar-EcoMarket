package com.ucompensar.ecomarket.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.data.Repository
import com.ucompensar.ecomarket.databinding.FragmentSalesReportBinding
import com.ucompensar.ecomarket.databinding.ItemStatBinding
import com.ucompensar.ecomarket.ui.common.SimpleAdapter
import com.ucompensar.ecomarket.util.toCop

/** Reporte de ventas: gráfico semanal, ventas por categoría y productos más vendidos. */
class SalesReportFragment : Fragment() {

    private var _binding: FragmentSalesReportBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentSalesReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity?.title = getString(R.string.report_title)
        binding.txtReportTotal.text = Repository.totalSales().toCop()
        binding.barChart.setValues(Repository.dailySales())

        val byCategory = Repository.salesByCategory()
        fillStats(binding.rvByCategory, byCategory) { it.toCop() }
        fillStats(binding.rvTopProducts, Repository.topProducts()) { "$it uds." }
    }

    private fun fillStats(list: RecyclerView, data: List<Pair<String, Int>>, format: (Int) -> String) {
        val max = (data.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
        val adapter = SimpleAdapter<Pair<String, Int>, ItemStatBinding>(ItemStatBinding::inflate) { b, (label, value) ->
            b.txtStatLabel.text = label
            b.txtStatValue.text = format(value)
            b.progressStat.max = max
            b.progressStat.progress = value
        }
        adapter.items = data
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
