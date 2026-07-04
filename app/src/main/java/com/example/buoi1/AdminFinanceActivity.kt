package com.example.buoi1

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.text.Normalizer
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AdminFinanceActivity : AppCompatActivity() {

    private data class MonthBucket(
        val year: Int,
        val month: Int,
        var income: Double = 0.0,
        var expense: Double = 0.0
    ) {
        val key: Int get() = year * 100 + month
        val profit: Double get() = income - expense
        val label: String get() = "T${month + 1}"
        val fullLabel: String get() = "T${month + 1}/$year"
    }

    private val formatter = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (UserManager.restoreSession(this)?.role != "admin") {
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_finance)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.topBarFinance)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(view.paddingLeft, bars.top, view.paddingRight, view.paddingBottom)
            insets
        }
        findViewById<ImageButton>(R.id.btnFinanceBack).setOnClickListener { finish() }
        loadFinanceData()
    }

    private fun loadFinanceData() {
        val db = FirebaseFirestore.getInstance()
        val ordersTask = db.collection("orders").get()
        val expensesTask = db.collection("expenses").get()

        Tasks.whenAllComplete(ordersTask, expensesTask).addOnCompleteListener {
            val buckets = createMonthBuckets()
            val bucketMap = buckets.associateBy { it.key }

            if (ordersTask.isSuccessful) {
                ordersTask.result?.forEach { document ->
                    val status = normalize(document.getString("status").orEmpty())
                    if (!isCompletedStatus(status)) return@forEach
                    val date = readDate(document) ?: return@forEach
                    val calendar = Calendar.getInstance().apply { time = date }
                    val key = calendar.get(Calendar.YEAR) * 100 + calendar.get(Calendar.MONTH)
                    bucketMap[key]?.income = (bucketMap[key]?.income ?: 0.0) +
                        (document.getDouble("totalAmount") ?: 0.0)
                }
            }

            if (expensesTask.isSuccessful) {
                expensesTask.result?.forEach { document ->
                    val date = readDate(document) ?: return@forEach
                    val calendar = Calendar.getInstance().apply { time = date }
                    val key = calendar.get(Calendar.YEAR) * 100 + calendar.get(Calendar.MONTH)
                    bucketMap[key]?.expense = (bucketMap[key]?.expense ?: 0.0) + readAmount(document)
                }
            }

            renderFinanceData(buckets)
        }
    }

    private fun createMonthBuckets(): MutableList<MonthBucket> {
        val cursor = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, -5)
        }
        return MutableList(6) {
            MonthBucket(
                year = cursor.get(Calendar.YEAR),
                month = cursor.get(Calendar.MONTH)
            ).also { cursor.add(Calendar.MONTH, 1) }
        }
    }

    private fun renderFinanceData(buckets: List<MonthBucket>) {
        val current = buckets.last()
        findViewById<TextView>(R.id.tvFinanceDetailIncome).text = formatMoney(current.income)
        findViewById<TextView>(R.id.tvFinanceDetailExpense).text = formatMoney(current.expense)
        findViewById<TextView>(R.id.tvFinanceDetailProfit).apply {
            text = formatMoney(current.profit)
            setTextColor(
                if (current.profit >= 0) {
                    ContextCompat.getColor(this@AdminFinanceActivity, R.color.adminGreen)
                } else {
                    Color.parseColor("#D32F2F")
                }
            )
        }

        findViewById<FinanceBarChartView>(R.id.financeBarChart).setData(
            buckets.map {
                FinanceChartPoint(
                    label = it.label,
                    income = it.income,
                    expense = it.expense
                )
            }
        )
        renderBreakdown(buckets.asReversed())
    }

    private fun renderBreakdown(buckets: List<MonthBucket>) {
        val container = findViewById<LinearLayout>(R.id.llFinanceBreakdown)
        container.removeAllViews()
        buckets.forEachIndexed { index, bucket ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 11.dp, 0, 11.dp)
            }
            row.addView(createCell(bucket.fullLabel, 0.8f, Gravity.START, R.color.colorText))
            row.addView(createCell(compactMoney(bucket.income), 1.2f, Gravity.END, R.color.adminOrange))
            row.addView(createCell(compactMoney(bucket.expense), 1.2f, Gravity.END, R.color.adminPurple))
            row.addView(
                createCell(
                    compactMoney(bucket.profit),
                    1.2f,
                    Gravity.END,
                    if (bucket.profit >= 0) R.color.adminGreen else android.R.color.holo_red_dark
                )
            )
            container.addView(row)

            if (index < buckets.lastIndex) {
                container.addView(
                    View(this).apply {
                        setBackgroundColor(ContextCompat.getColor(this@AdminFinanceActivity, R.color.chatDivider))
                    },
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        1.dp
                    )
                )
            }
        }
    }

    private fun createCell(text: String, weight: Float, gravity: Int, colorRes: Int): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 11f
            this.gravity = gravity
            setTextColor(ContextCompat.getColor(this@AdminFinanceActivity, colorRes))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight)
        }
    }

    private fun formatMoney(value: Double): String = "${formatter.format(value)}đ"

    private fun compactMoney(value: Double): String {
        val sign = if (value < 0) "-" else ""
        val absolute = kotlin.math.abs(value)
        return when {
            absolute >= 1_000_000_000 ->
                "$sign${formatter.format(absolute / 1_000_000_000)} tỷ"
            absolute >= 1_000_000 ->
                "$sign${formatter.format(absolute / 1_000_000)} tr"
            absolute >= 1_000 ->
                "$sign${formatter.format(absolute / 1_000)}k"
            else -> "$sign${formatter.format(absolute)}đ"
        }
    }

    private fun isCompletedStatus(status: String): Boolean {
        return status.contains("thanh cong") ||
            status.contains("hoan thanh") ||
            status.contains("da giao")
    }

    private fun readAmount(document: DocumentSnapshot): Double {
        return document.getDouble("amount")
            ?: document.getDouble("totalAmount")
            ?: document.getLong("amount")?.toDouble()
            ?: 0.0
    }

    private fun readDate(document: DocumentSnapshot): Date? {
        val value = document.get("date") ?: document.get("createdAt")
        return when (value) {
            is Timestamp -> value.toDate()
            is Date -> value
            is String -> parseDate(value)
            else -> null
        }
    }

    private fun parseDate(value: String): Date? {
        return listOf("dd/MM/yyyy HH:mm", "dd/MM/yyyy", "yyyy-MM-dd")
            .firstNotNullOfOrNull { pattern ->
                runCatching {
                    SimpleDateFormat(pattern, Locale.getDefault()).apply {
                        isLenient = false
                    }.parse(value)
                }.getOrNull()
            }
    }

    private fun normalize(value: String): String {
        return Normalizer.normalize(value.lowercase(Locale.getDefault()), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .replace('đ', 'd')
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}
