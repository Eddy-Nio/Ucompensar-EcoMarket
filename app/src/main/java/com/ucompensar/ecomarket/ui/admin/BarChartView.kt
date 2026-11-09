package com.ucompensar.ecomarket.ui.admin

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.ucompensar.ecomarket.R

/** Gráfico de barras dibujado con Canvas; los colores provienen de res/values/colors.xml. */
class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val labels = listOf("L", "M", "X", "J", "V", "S", "D")
    private var values: List<Int> = emptyList()

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.eco_gray_border)
        strokeWidth = resources.displayMetrics.density
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.eco_text_secondary)
        textSize = resources.getDimension(R.dimen.text_caption)
        textAlign = Paint.Align.CENTER
    }
    private val colorStrong = ContextCompat.getColor(context, R.color.eco_green)
    private val colorLight = ContextCompat.getColor(context, R.color.eco_green_light)

    fun setValues(data: List<Int>) {
        values = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (values.isEmpty()) return
        val bottom = height - textPaint.textSize * 2
        val top = textPaint.textSize
        val maxValue = (values.maxOrNull() ?: 1).coerceAtLeast(1)

        for (i in 0..3) {
            val y = bottom - (bottom - top) * i / 3f
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
        }
        val slot = width / values.size.toFloat()
        val barWidth = slot * 0.55f
        values.forEachIndexed { index, value ->
            val left = index * slot + (slot - barWidth) / 2f
            val barTop = bottom - (bottom - top) * value / maxValue
            barPaint.color = if (index % 2 == 0) colorStrong else colorLight
            canvas.drawRoundRect(RectF(left, barTop, left + barWidth, bottom), 12f, 12f, barPaint)
            canvas.drawText(labels[index], left + barWidth / 2f, height - textPaint.textSize * 0.6f, textPaint)
        }
    }
}
