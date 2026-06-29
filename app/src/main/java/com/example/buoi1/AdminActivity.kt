package com.example.buoi1

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class AdminActivity : AppCompatActivity() {

    private val formatter = NumberFormat.getInstance(Locale("vi", "VN"))

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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.topBarAdmin)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, bars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        val tvAdminWelcome = findViewById<TextView>(R.id.tvAdminWelcome)
        tvAdminWelcome.text = "Xin chào, ${currentUser.fullName}!"

        loadStats()

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
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadStats()
    }

    private fun loadStats() {
        val db = FirebaseFirestore.getInstance()
        val tvTotalProducts = findViewById<TextView>(R.id.tvAdminTotalProducts)
        val tvTotalOrders = findViewById<TextView>(R.id.tvAdminTotalOrders)
        val tvTotalUsers = findViewById<TextView>(R.id.tvAdminTotalUsers)
        val tvPendingOrders = findViewById<TextView>(R.id.tvAdminPendingOrders)

        db.collection("products").get()
            .addOnSuccessListener { tvTotalProducts.text = it.size().toString() }
            .addOnFailureListener { tvTotalProducts.text = "0" }

        db.collection("users").get()
            .addOnSuccessListener { tvTotalUsers.text = it.size().toString() }
            .addOnFailureListener { tvTotalUsers.text = "0" }

        db.collection("orders").get()
            .addOnSuccessListener { documents ->
                tvTotalOrders.text = documents.size().toString()

                var revenueTotal = 0.0
                var revenueThisMonth = 0.0
                var ordersThisMonth = 0
                var countPending = 0
                var countShipping = 0
                var countCompleted = 0
                var countCancelled = 0

                val productSales = mutableMapOf<String, Int>() // productName -> qty

                val cal = Calendar.getInstance()
                val currentMonth = cal.get(Calendar.MONTH) + 1
                val currentYear = cal.get(Calendar.YEAR)

                for (doc in documents) {
                    val status = doc.getString("status") ?: ""
                    val total = doc.getDouble("totalAmount") ?: 0.0
                    val date = doc.getString("date") ?: ""

                    when (status) {
                        "Chờ xác nhận" -> countPending++
                        "Đang giao" -> countShipping++
                        "Thành công" -> {
                            countCompleted++
                            revenueTotal += total
                            // Parse date "dd/MM/yyyy HH:mm" or "dd/MM/yyyy"
                            val parts = date.split("/", " ")
                            if (parts.size >= 3) {
                                val month = parts[1].toIntOrNull() ?: 0
                                val year = parts[2].toIntOrNull() ?: 0
                                if (month == currentMonth && year == currentYear) {
                                    revenueThisMonth += total
                                    ordersThisMonth++
                                }
                            }
                        }
                        "Đã huỷ" -> countCancelled++
                    }

                    // Count product sales from items subcollection not available directly —
                    // read items as list of maps if stored inline
                    @Suppress("UNCHECKED_CAST")
                    val items = doc.get("items") as? List<Map<String, Any>> ?: emptyList()
                    for (item in items) {
                        @Suppress("UNCHECKED_CAST")
                        val product = item["product"] as? Map<String, Any>
                        val name = product?.get("name") as? String ?: continue
                        val qty = (item["quantity"] as? Long)?.toInt() ?: 1
                        productSales[name] = (productSales[name] ?: 0) + qty
                    }
                }

                // Update summary stats
                tvTotalOrders.text = documents.size().toString()
                val pending = documents.count { it.getString("status") == "Chờ xác nhận" }
                tvPendingOrders.text = pending.toString()

                // Revenue cards
                val tvRevenueThisMonth = findViewById<TextView>(R.id.tvRevenueThisMonth)
                val tvOrdersThisMonth = findViewById<TextView>(R.id.tvOrdersThisMonth)
                val tvRevenueTotal = findViewById<TextView>(R.id.tvRevenueTotal)
                val tvOrdersCompleted = findViewById<TextView>(R.id.tvOrdersCompleted)

                tvRevenueThisMonth.text = formatRevenue(revenueThisMonth)
                tvOrdersThisMonth.text = "$ordersThisMonth đơn"
                tvRevenueTotal.text = formatRevenue(revenueTotal)
                tvOrdersCompleted.text = "$countCompleted đơn hoàn thành"

                // Status breakdown
                findViewById<TextView>(R.id.tvStatPending).text = "$countPending đơn"
                findViewById<TextView>(R.id.tvStatShipping).text = "$countShipping đơn"
                findViewById<TextView>(R.id.tvStatCompleted).text = "$countCompleted đơn"
                findViewById<TextView>(R.id.tvStatCancelled).text = "$countCancelled đơn"

                // Top products
                val top5 = productSales.entries
                    .sortedByDescending { it.value }
                    .take(5)
                renderTopProducts(top5)
            }
            .addOnFailureListener {
                tvTotalOrders.text = "0"
                tvPendingOrders.text = "0"
            }
    }

    private fun formatRevenue(amount: Double): String {
        return when {
            amount >= 1_000_000_000 -> "${formatter.format(amount / 1_000_000_000)}tỷ"
            amount >= 1_000_000 -> "${formatter.format(amount / 1_000_000)}tr"
            else -> "đ${formatter.format(amount)}"
        }
    }

    private fun renderTopProducts(top: List<Map.Entry<String, Int>>) {
        val container = findViewById<LinearLayout>(R.id.llTopProducts)
        container.removeAllViews()

        if (top.isEmpty()) {
            val tv = TextView(this)
            tv.text = "Chưa có dữ liệu bán hàng"
            tv.textSize = 13f
            tv.setTextColor(android.graphics.Color.parseColor("#999999"))
            container.addView(tv)
            return
        }

        val maxQty = top.first().value.coerceAtLeast(1)

        top.forEachIndexed { index, entry ->
            val row = LayoutInflater.from(this).inflate(R.layout.item_top_product_row, container, false)

            row.findViewById<TextView>(R.id.tvTopRank).text = "${index + 1}"
            row.findViewById<TextView>(R.id.tvTopProductName).text = entry.key
            row.findViewById<TextView>(R.id.tvTopProductQty).text = "${entry.value} sản phẩm"

            val bar = row.findViewById<android.view.View>(R.id.viewTopBar)
            val params = bar.layoutParams
            // Scale bar width proportionally — will be set after layout
            bar.tag = entry.value.toFloat() / maxQty
            bar.post {
                val parentWidth = (bar.parent as android.view.View).width
                params.width = ((entry.value.toFloat() / maxQty) * parentWidth * 0.6f).toInt().coerceAtLeast(8)
                bar.layoutParams = params
            }

            val rankColor = when (index) {
                0 -> "#E64A19"
                1 -> "#EF6C00"
                2 -> "#F9A825"
                else -> "#BDBDBD"
            }
            row.findViewById<TextView>(R.id.tvTopRank)
                .setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor(rankColor)))
            bar.backgroundTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor(rankColor))

            container.addView(row)
        }
    }
}
