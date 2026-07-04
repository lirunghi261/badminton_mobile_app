package com.example.buoi1

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.max

data class FinanceChartPoint(
    val label: String,
    val income: Double,
    val expense: Double
)

class FinanceBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.chatDivider)
        strokeWidth = 1.dp
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.chatMutedText)
        textSize = 10.sp
        textAlign = Paint.Align.CENTER
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.chatMutedText)
        textSize = 9.sp
        textAlign = Paint.Align.RIGHT
    }
    private val incomePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.adminOrange)
    }
    private val expensePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.adminPurple)
    }
    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.chatMutedText)
        textSize = 13.sp
        textAlign = Paint.Align.CENTER
    }

    private var points: List<FinanceChartPoint> = emptyList()

    fun setData(newPoints: List<FinanceChartPoint>) {
        points = newPoints
        contentDescription = "Biểu đồ thu chi ${newPoints.size} tháng"
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val left = 48.dp
        val right = width - 14.dp
        val top = 20.dp
        val bottom = height - 38.dp
        val chartHeight = bottom - top
        val chartWidth = right - left

        if (points.isEmpty() || chartWidth <= 0 || chartHeight <= 0) {
            canvas.drawText("Chưa có dữ liệu để hiển thị", width / 2f, height / 2f, emptyPaint)
            return
        }

        val maxValue = points.maxOfOrNull { max(it.income, it.expense) } ?: 0.0
        val scaleMax = if (maxValue <= 0.0) 1.0 else maxValue * 1.15

        for (step in 0..4) {
            val ratio = step / 4f
            val y = bottom - chartHeight * ratio
            canvas.drawLine(left, y, right, y, axisPaint)
            valuePaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(
                compactMoney(scaleMax * ratio),
                left - 6.dp,
                y + 3.dp,
                valuePaint
            )
        }

        val groupWidth = chartWidth / points.size
        val barWidth = (groupWidth * 0.24f).coerceAtMost(20.dp)
        val gap = 3.dp

        points.forEachIndexed { index, point ->
            val centerX = left + groupWidth * index + groupWidth / 2f
            val incomeHeight = (point.income / scaleMax * chartHeight).toFloat()
            val expenseHeight = (point.expense / scaleMax * chartHeight).toFloat()

            drawBar(
                canvas,
                centerX - gap / 2f - barWidth,
                bottom - incomeHeight,
                centerX - gap / 2f,
                bottom.toFloat(),
                incomePaint
            )
            drawBar(
                canvas,
                centerX + gap / 2f,
                bottom - expenseHeight,
                centerX + gap / 2f + barWidth,
                bottom.toFloat(),
                expensePaint
            )

            labelPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(point.label, centerX, bottom + 20.dp, labelPaint)
        }
    }

    private fun drawBar(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        paint: Paint
    ) {
        val visibleTop = top.coerceAtMost(bottom - 2.dp)
        canvas.drawRoundRect(
            RectF(left, visibleTop, right, bottom),
            5.dp,
            5.dp,
            paint
        )
    }

    private fun compactMoney(value: Double): String {
        return when {
            value >= 1_000_000_000 -> "${(value / 1_000_000_000).toInt()}tỷ"
            value >= 1_000_000 -> "${(value / 1_000_000).toInt()}tr"
            value >= 1_000 -> "${(value / 1_000).toInt()}k"
            else -> value.toInt().toString()
        }
    }

    private val Int.dp: Float
        get() = this * resources.displayMetrics.density

    private val Int.sp: Float
        get() = this * resources.displayMetrics.scaledDensity
}
