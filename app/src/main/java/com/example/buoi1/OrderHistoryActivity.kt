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
import java.util.Locale

class OrderHistoryActivity : AppCompatActivity() {

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
    }

    override fun onResume() {
        super.onResume()
        loadOrdersFromFirebase()
    }

    private fun loadOrdersFromFirebase() {
        val llOrderHistoryList = findViewById<LinearLayout>(R.id.llOrderHistoryList)
        val layoutOrderHistoryEmpty = findViewById<LinearLayout>(R.id.layoutOrderHistoryEmpty)
        val progressBar = findViewById<ProgressBar>(R.id.progressBarOrderHistory)
        val formatter = NumberFormat.getInstance(Locale("vi", "VN"))

        // Show loading
        progressBar.visibility = View.VISIBLE
        llOrderHistoryList.visibility = View.GONE
        layoutOrderHistoryEmpty.visibility = View.GONE

        OrderManager.fetchOrders(
            onSuccess = { orders ->
                progressBar.visibility = View.GONE
                llOrderHistoryList.removeAllViews()

                if (orders.isEmpty()) {
                    layoutOrderHistoryEmpty.visibility = View.VISIBLE
                    llOrderHistoryList.visibility = View.GONE
                } else {
                    layoutOrderHistoryEmpty.visibility = View.GONE
                    llOrderHistoryList.visibility = View.VISIBLE

                    for (order in orders) {
                        val itemView = layoutInflater.inflate(R.layout.item_order_history, llOrderHistoryList, false)

                        val tvOrderId = itemView.findViewById<TextView>(R.id.tvOrderId)
                        val tvOrderStatus = itemView.findViewById<TextView>(R.id.tvOrderStatus)
                        val tvOrderDate = itemView.findViewById<TextView>(R.id.tvOrderDate)
                        val tvOrderItemsInfo = itemView.findViewById<TextView>(R.id.tvOrderItemsInfo)
                        val tvOrderTotal = itemView.findViewById<TextView>(R.id.tvOrderTotal)

                        tvOrderId.text = "Đơn: #${order.id}"
                        tvOrderStatus.text = order.status
                        tvOrderDate.text = order.date

                        // Update status color based on order status
                        when (order.status) {
                            "Đã huỷ" -> tvOrderStatus.setTextColor(android.graphics.Color.parseColor("#999999"))
                            "Chờ xác nhận" -> tvOrderStatus.setTextColor(android.graphics.Color.parseColor("#EF6C00"))
                            "Đang giao" -> tvOrderStatus.setTextColor(android.graphics.Color.parseColor("#1565C0"))
                            "Thành công" -> tvOrderStatus.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
                            else -> tvOrderStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                        }

                        // Build a preview string of items
                        val itemsPreview = order.items.joinToString(", ") { "${it.product.name} x${it.quantity}" }
                        tvOrderItemsInfo.text = itemsPreview

                        tvOrderTotal.text = "đ${formatter.format(order.totalAmount)}"

                        // Navigate to order detail on click
                        itemView.setOnClickListener {
                            val intent = Intent(this, OrderDetailActivity::class.java)
                            intent.putExtra("EXTRA_ORDER_ID", order.id)
                            startActivity(intent)
                        }

                        llOrderHistoryList.addView(itemView)
                    }
                }
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
