package com.example.buoi1

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.text.Normalizer
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AdminActivity : AppCompatActivity() {

    private val formatter = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"))
    private var revenueThisMonth = 0.0
    private var expenseThisMonth = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val currentUser = UserManager.restoreSession(this)
        if (currentUser?.role != "admin") {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_admin)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.topBarAdmin)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(view.paddingLeft, bars.top, view.paddingRight, view.paddingBottom)
            insets
        }

        findViewById<TextView>(R.id.tvAdminWelcome).text =
            "Xin chào, ${currentUser.fullName.ifBlank { "Admin" }}!"

        bindNavigation()
        loadStats()
    }

    override fun onResume() {
        super.onResume()
        loadStats()
    }

    private fun bindNavigation() {
        findViewById<LinearLayout>(R.id.btnFinanceDetail).setOnClickListener {
            startActivity(Intent(this, AdminFinanceActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.btnManageProducts).setOnClickListener {
            startActivity(Intent(this, AdminProductListActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.btnManageCategories).setOnClickListener {
            startActivity(Intent(this, AdminCategoryActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.btnManageOrders).setOnClickListener {
            startActivity(Intent(this, AdminOrderListActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.btnManageUsers).setOnClickListener {
            startActivity(Intent(this, AdminUserListActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.btnManageDeals).setOnClickListener {
            startActivity(Intent(this, AdminDealListActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.btnManageWarranty).setOnClickListener {
            startActivity(Intent(this, AdminWarrantyListActivity::class.java))
        }
        findViewById<Button>(R.id.btnAdminLogout).setOnClickListener {
            UserManager.logout(this)
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
            finish()
        }
    }

    private fun loadStats() {
        val db = FirebaseFirestore.getInstance()
        val totalProducts = findViewById<TextView>(R.id.tvAdminTotalProducts)
        val totalOrders = findViewById<TextView>(R.id.tvAdminTotalOrders)
        val totalUsers = findViewById<TextView>(R.id.tvAdminTotalUsers)
        val pendingOrders = findViewById<TextView>(R.id.tvAdminPendingOrders)

        db.collection("products").get()
            .addOnSuccessListener { totalProducts.text = it.size().toString() }
            .addOnFailureListener { totalProducts.text = "0" }

        db.collection("users").get()
            .addOnSuccessListener { totalUsers.text = it.size().toString() }
            .addOnFailureListener { totalUsers.text = "0" }

        db.collection("expenses").get()
            .addOnSuccessListener { documents ->
                expenseThisMonth = documents.sumOf { document ->
                    if (isCurrentMonth(readDate(document))) readAmount(document) else 0.0
                }
                updateFinanceCard()
            }
            .addOnFailureListener {
                expenseThisMonth = 0.0
                updateFinanceCard()
            }

        db.collection("orders").get()
            .addOnSuccessListener { documents ->
                totalOrders.text = documents.size().toString()

                var pending = 0
                var shipping = 0
                var completed = 0
                var cancelled = 0
                var monthlyRevenue = 0.0
                val productSales = mutableMapOf<String, Int>()

                documents.forEach { document ->
                    val status = normalize(document.getString("status").orEmpty())
                    val total = document.getDouble("totalAmount") ?: 0.0

                    when {
                        status.contains("cho xac nhan") -> pending++
                        status.contains("dang giao") -> shipping++
                        isCompletedStatus(status) -> {
                            completed++
                            if (isCurrentMonth(readDate(document))) {
                                monthlyRevenue += total
                            }
                        }
                        status.contains("huy") -> cancelled++
                    }

                    @Suppress("UNCHECKED_CAST")
                    val items = document.get("items") as? List<Map<String, Any>> ?: emptyList()
                    items.forEach { item ->
                        @Suppress("UNCHECKED_CAST")
                        val nestedProduct = item["product"] as? Map<String, Any>
                        val name = (item["productName"] as? String)
                            ?: (nestedProduct?.get("name") as? String)
                            ?: return@forEach
                        val quantity = (item["quantity"] as? Number)?.toInt() ?: 1
                        productSales[name] = (productSales[name] ?: 0) + quantity
                    }
                }

                revenueThisMonth = monthlyRevenue
                pendingOrders.text = pending.toString()
                findViewById<TextView>(R.id.tvStatPending).text = pending.toString()
                findViewById<TextView>(R.id.tvStatShipping).text = shipping.toString()
                findViewById<TextView>(R.id.tvStatCompleted).text = completed.toString()
                findViewById<TextView>(R.id.tvStatCancelled).text = cancelled.toString()
                updateFinanceCard()

                renderTopProducts(
                    productSales.entries
                        .sortedByDescending { it.value }
                        .take(5)
                )
            }
            .addOnFailureListener {
                totalOrders.text = "0"
                pendingOrders.text = "0"
                revenueThisMonth = 0.0
                updateFinanceCard()
            }
    }

    private fun updateFinanceCard() {
        val profit = revenueThisMonth - expenseThisMonth
        findViewById<TextView>(R.id.tvFinanceIncomeMonth).text = formatMoney(revenueThisMonth)
        findViewById<TextView>(R.id.tvFinanceExpenseMonth).text = formatMoney(expenseThisMonth)
        findViewById<TextView>(R.id.tvFinanceProfitMonth).text = formatMoney(profit)
    }

    private fun formatMoney(amount: Double): String = "${formatter.format(amount)}đ"

    private fun isCompletedStatus(normalizedStatus: String): Boolean {
        return normalizedStatus.contains("thanh cong") ||
            normalizedStatus.contains("hoan thanh") ||
            normalizedStatus.contains("da giao")
    }

    private fun readAmount(document: DocumentSnapshot): Double {
        return document.getDouble("amount")
            ?: document.getDouble("totalAmount")
            ?: (document.getLong("amount")?.toDouble())
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
        val formats = listOf("dd/MM/yyyy HH:mm", "dd/MM/yyyy", "yyyy-MM-dd")
        return formats.firstNotNullOfOrNull { pattern ->
            runCatching {
                SimpleDateFormat(pattern, Locale.getDefault()).apply {
                    isLenient = false
                }.parse(value)
            }.getOrNull()
        }
    }

    private fun isCurrentMonth(date: Date?): Boolean {
        if (date == null) return false
        val now = Calendar.getInstance()
        val value = Calendar.getInstance().apply { time = date }
        return now.get(Calendar.YEAR) == value.get(Calendar.YEAR) &&
            now.get(Calendar.MONTH) == value.get(Calendar.MONTH)
    }

    private fun normalize(value: String): String {
        return Normalizer.normalize(value.lowercase(Locale.getDefault()), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .replace('đ', 'd')
    }

    private fun renderTopProducts(top: List<Map.Entry<String, Int>>) {
        val container = findViewById<LinearLayout>(R.id.llTopProducts)
        container.removeAllViews()

        if (top.isEmpty()) {
            container.addView(
                TextView(this).apply {
                    text = "Chưa có dữ liệu bán hàng"
                    textSize = 13f
                    setTextColor(Color.parseColor("#8A8A92"))
                }
            )
            return
        }

        val maxQuantity = top.first().value.coerceAtLeast(1)
        top.forEachIndexed { index, entry ->
            val row = LayoutInflater.from(this)
                .inflate(R.layout.item_top_product_row, container, false)

            row.findViewById<TextView>(R.id.tvTopRank).text = "${index + 1}"
            row.findViewById<TextView>(R.id.tvTopProductName).text = entry.key
            row.findViewById<TextView>(R.id.tvTopProductQty).text = "${entry.value} sản phẩm"

            val bar = row.findViewById<android.view.View>(R.id.viewTopBar)
            bar.post {
                val parentWidth = (bar.parent as android.view.View).width
                bar.layoutParams = bar.layoutParams.apply {
                    width = ((entry.value.toFloat() / maxQuantity) * parentWidth * 0.6f)
                        .toInt()
                        .coerceAtLeast(8)
                }
            }

            val rankColor = when (index) {
                0 -> "#E64A19"
                1 -> "#EF6C00"
                2 -> "#F9A825"
                else -> "#BDBDBD"
            }
            val color = Color.parseColor(rankColor)
            row.findViewById<TextView>(R.id.tvTopRank).backgroundTintList =
                ColorStateList.valueOf(color)
            bar.backgroundTintList = ColorStateList.valueOf(color)
            container.addView(row)
        }
    }
}
