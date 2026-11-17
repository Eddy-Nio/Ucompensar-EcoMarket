package com.ucompensar.ecomarket.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.data.model.Order
import com.ucompensar.ecomarket.data.repository.OrderRepository
import com.ucompensar.ecomarket.databinding.FragmentSalesReportBinding
import com.ucompensar.ecomarket.databinding.ItemStatBinding
import com.ucompensar.ecomarket.ui.common.SimpleAdapter
import com.ucompensar.ecomarket.util.collectWhileVisible
import com.ucompensar.ecomarket.util.show
import com.ucompensar.ecomarket.util.toCop
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Reporte de ventas con datos reales: ventas de 7 días, por categoría y productos más vendidos. */
class SalesReportFragment : Fragment() {

    private var _binding: FragmentSalesReportBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentSalesReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity?.title = getString(R.string.report_title)
        collectWhileVisible(OrderRepository.observeAll()) { orders -> render(orders.filter { it.status.isSale }) }
    }

    private fun render(sales: List<Order>) {
        val total = sales.sumOf { it.total }
        binding.txtReportTotal.text = total.toCop()
        binding.txtReportCaption.text = getString(
            R.string.report_total_caption, sales.size, (if (sales.isEmpty()) 0 else total / sales.size).toCop()
        )
        binding.txtReportEmpty.show(sales.isEmpty())

        val (labels, values) = lastSevenDays(sales)
        binding.barChart.setValues(labels, values)

        val lines = sales.flatMap { it.items }
        val byCategory = lines.groupBy { it.category }.map { (cat, items) -> cat to items.sumOf { it.subtotal } }
            .sortedByDescending { it.second }
        val topProducts = lines.groupBy { it.name }.map { (name, items) -> name to items.sumOf { it.quantity } }
            .sortedByDescending { it.second }.take(TOP_PRODUCTS)
        fillStats(binding.rvByCategory, byCategory) { it.toCop() }
        fillStats(binding.rvTopProducts, topProducts) { getString(R.string.report_units, it) }
    }

    /** Suma las ventas por día de los últimos 7 días; la etiqueta es la inicial del día. */
    private fun lastSevenDays(sales: List<Order>): Pair<List<String>, List<Int>> {
        val dayKey = SimpleDateFormat("yyyyMMdd", Locale.US)
        val dayLabel = SimpleDateFormat("EEEEE", Locale("es", "CO"))
        val days = (DAYS - 1 downTo 0).map { offset ->
            Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -offset) }.time
        }
        val totals = sales.filter { it.createdAt != null }.groupBy { dayKey.format(it.createdAt!!) }
            .mapValues { (_, orders) -> orders.sumOf { it.total } }
        return days.map { dayLabel.format(it).uppercase() } to days.map { totals[dayKey.format(it)] ?: 0 }
    }

    private fun fillStats(list: RecyclerView, data: List<Pair<String, Int>>, format: (Int) -> String) {
        val max = (data.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
        val adapter = SimpleAdapter<Pair<String, Int>, ItemStatBinding>(ItemStatBinding::inflate, { it.first }) { b, (label, value) ->
            b.txtStatLabel.text = label
            b.txtStatValue.text = format(value)
            b.progressStat.max = max
            b.progressStat.progress = value
        }
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter
        adapter.submitList(data)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val DAYS = 7
        const val TOP_PRODUCTS = 5
    }
}
