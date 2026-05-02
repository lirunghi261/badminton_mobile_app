package com.example.buoi1

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class OrderHistoryActivity : AppCompatActivity() {

    private var allOrders: List<Order> = emptyList()
    private var activeChip: TextView? = null
    private val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val sdfFallback = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_order_history)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.topBarOrderHistory)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, bars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnOrderHistoryBack).setOnClickListener { finish() }

        val chipAll = findViewById<TextView>(R.id.chipAllOrders)
        val chipPending = findViewById<TextView>(R.id.chipPendingOrders)
        val chipShipping = findViewById<TextView>(R.id.chipShippingOrders)
        val chipCompleted = findViewById<TextView>(R.id.chipCompletedOrders)
        val chipCancelled = findViewById<TextView>(R.id.chipCancelledOrders)

        setActiveChip(chipAll)

        chipAll.setOnClickListener { setActiveChip(chipAll); renderOrders(null) }
        chipPending.setOnClickListener { setActiveChip(chipPending); renderOrders("Chờ xác nhận") }
        chipShipping.setOnClickListener { setActiveChip(chipShipping); renderOrders("Đang giao") }
        chipCompleted.setOnClickListener { setActiveChip(chipCompleted); renderOrders("Thành công") }
        chipCancelled.setOnClickListener { setActiveChip(chipCancelled); renderOrders("Đã huỷ") }
    }

    override fun onResume() {
        super.onResume()
        loadOrdersFromFirebase()
    }

    private fun setActiveChip(chip: TextView) {
        activeChip?.apply {
            setTextColor(android.graphics.Color.parseColor("#666666"))
            setBackgroundResource(R.drawable.bg_search_bar)
            backgroundTintList = null
        }
        chip.setTextColor(android.graphics.Color.WHITE)
        chip.setBackgroundResource(R.drawable.bg_button_rounded)
        chip.backgroundTintList = android.content.res.ColorStateList.valueOf(
            android.graphics.Color.parseColor("#E64A19")
        )
        activeChip = chip
    }

    private fun parseOrderDate(dateStr: String): Long {
        return try {
            sdf.parse(dateStr)?.time ?: 0L
        } catch (_: Exception) {
            try { sdfFallback.parse(dateStr)?.time ?: 0L } catch (_: Exception) { 0L }
        }
    }

    private fun renderOrders(statusFilter: String?) {
        val llOrderHistoryList = findViewById<LinearLayout>(R.id.llOrderHistoryList)
        val layoutOrderHistoryEmpty = findViewById<LinearLayout>(R.id.layoutOrderHistoryEmpty)
        val formatter = NumberFormat.getInstance(Locale("vi", "VN"))

        llOrderHistoryList.removeAllViews()

        val filtered = if (statusFilter == null) {
            allOrders.sortedByDescending { parseOrderDate(it.date) }
        } else {
            allOrders.filter { it.status == statusFilter }
        }

        if (filtered.isEmpty()) {
            layoutOrderHistoryEmpty.visibility = View.VISIBLE
            llOrderHistoryList.visibility = View.GONE
        } else {
            layoutOrderHistoryEmpty.visibility = View.GONE
            llOrderHistoryList.visibility = View.VISIBLE

            for (order in filtered) {
                val itemView = layoutInflater.inflate(R.layout.item_order_history, llOrderHistoryList, false)

                itemView.findViewById<TextView>(R.id.tvOrderId).text = "Đơn: #${order.id}"
                itemView.findViewById<TextView>(R.id.tvOrderDate).text = order.date
                val totalQty = order.items.sumOf { it.quantity }
                itemView.findViewById<TextView>(R.id.tvOrderItemCount).text =
                    "$totalQty sản phẩm"
                itemView.findViewById<TextView>(R.id.tvOrderItemsInfo).text =
                    order.items.joinToString("\n") { "• ${it.product.name}  x${it.quantity}" }
                itemView.findViewById<TextView>(R.id.tvOrderTotal).text =
                    "đ${formatter.format(order.totalAmount)}"

                val tvStatus = itemView.findViewById<TextView>(R.id.tvOrderStatus)
                tvStatus.text = order.status
                tvStatus.setTextColor(
                    android.graphics.Color.parseColor(
                        when (order.status) {
                            "Đã huỷ" -> "#999999"
                            "Chờ xác nhận" -> "#EF6C00"
                            "Đang giao" -> "#1565C0"
                            "Thành công" -> "#2E7D32"
                            else -> "#4CAF50"
                        }
                    )
                )

                itemView.setOnClickListener {
                    val intent = Intent(this, OrderDetailActivity::class.java)
                    intent.putExtra("EXTRA_ORDER_ID", order.id)
                    startActivity(intent)
                }

                llOrderHistoryList.addView(itemView)
            }
        }
    }

    private fun loadOrdersFromFirebase() {
        val progressBar = findViewById<ProgressBar>(R.id.progressBarOrderHistory)
        val llOrderHistoryList = findViewById<LinearLayout>(R.id.llOrderHistoryList)
        val layoutOrderHistoryEmpty = findViewById<LinearLayout>(R.id.layoutOrderHistoryEmpty)

        progressBar.visibility = View.VISIBLE
        llOrderHistoryList.visibility = View.GONE
        layoutOrderHistoryEmpty.visibility = View.GONE

        OrderManager.fetchOrders(
            onSuccess = { orders ->
                progressBar.visibility = View.GONE
                allOrders = orders
                // Default view: all orders, newest first
                renderOrders(null)
            },
            onFailure = { e ->
                progressBar.visibility = View.GONE
                layoutOrderHistoryEmpty.visibility = View.VISIBLE
                llOrderHistoryList.visibility = View.GONE
                android.widget.Toast.makeText(this, "Lỗi tải đơn hàng: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }
}
