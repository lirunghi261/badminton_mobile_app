package com.example.buoi1

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
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

        val llOrderHistoryList = findViewById<LinearLayout>(R.id.llOrderHistoryList)
        val layoutOrderHistoryEmpty = findViewById<LinearLayout>(R.id.layoutOrderHistoryEmpty)
        val formatter = NumberFormat.getInstance(Locale("vi", "VN"))

        val orders = OrderManager.orders

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

                // Build a preview string of items
                val itemsPreview = order.items.joinToString(", ") { "${it.product.name} x${it.quantity}" }
                tvOrderItemsInfo.text = itemsPreview

                tvOrderTotal.text = "đ${formatter.format(order.totalAmount)}"

                llOrderHistoryList.addView(itemView)
            }
        }
    }
}
