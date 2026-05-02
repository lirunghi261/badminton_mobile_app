package com.example.buoi1

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.NumberFormat
import java.util.Locale

class OrderDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_order_detail)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.topBarOrderDetail)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, bars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bottomBarOrderDetail)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, bars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnOrderDetailBack).setOnClickListener { finish() }

        // Get order ID from intent
        val orderId = intent.getStringExtra("EXTRA_ORDER_ID")
        if (orderId == null) {
            Toast.makeText(this, "Không tìm thấy đơn hàng", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val order = OrderManager.getOrderById(orderId)
        if (order == null) {
            Toast.makeText(this, "Không tìm thấy đơn hàng", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val formatter = NumberFormat.getInstance(Locale("vi", "VN"))

        // Set order info
        val tvOrderDetailId = findViewById<TextView>(R.id.tvOrderDetailId)
        val tvOrderDetailStatus = findViewById<TextView>(R.id.tvOrderDetailStatus)
        val tvOrderDetailDate = findViewById<TextView>(R.id.tvOrderDetailDate)
        val tvOrderDetailAddress = findViewById<TextView>(R.id.tvOrderDetailAddress)
        val tvOrderDetailPaymentMethod = findViewById<TextView>(R.id.tvOrderDetailPaymentMethod)
        val tvOrderDetailTotalItems = findViewById<TextView>(R.id.tvOrderDetailTotalItems)
        val tvOrderDetailTotalPrice = findViewById<TextView>(R.id.tvOrderDetailTotalPrice)
        val tvOrderDetailBottomTotalPrice = findViewById<TextView>(R.id.tvOrderDetailBottomTotalPrice)
        val btnCancelOrder = findViewById<Button>(R.id.btnCancelOrder)
        val btnReorder = findViewById<Button>(R.id.btnReorder)

        tvOrderDetailId.text = "Đơn: #${order.id}"
        tvOrderDetailStatus.text = order.status
        tvOrderDetailDate.text = order.date
        tvOrderDetailAddress.text = order.address
        tvOrderDetailPaymentMethod.text = order.paymentMethod

        // Set status color and button visibility
        fun updateStatusUI(status: String) {
            tvOrderDetailStatus.text = status
            when (status) {
                "Đã huỷ" -> {
                    tvOrderDetailStatus.setTextColor(android.graphics.Color.parseColor("#999999"))
                    btnCancelOrder.visibility = View.GONE
                    btnReorder.visibility = View.VISIBLE
                }
                "Chờ xác nhận" -> {
                    tvOrderDetailStatus.setTextColor(android.graphics.Color.parseColor("#EF6C00"))
                    btnCancelOrder.visibility = View.VISIBLE
                    btnReorder.visibility = View.GONE
                }
                "Đang giao" -> {
                    tvOrderDetailStatus.setTextColor(android.graphics.Color.parseColor("#1565C0"))
                    btnCancelOrder.visibility = View.GONE
                    btnReorder.visibility = View.GONE
                }
                "Thành công" -> {
                    tvOrderDetailStatus.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
                    btnCancelOrder.visibility = View.GONE
                    btnReorder.visibility = View.GONE
                }
                else -> {
                    tvOrderDetailStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                    btnCancelOrder.visibility = View.GONE
                    btnReorder.visibility = View.GONE
                }
            }
        }

        updateStatusUI(order.status)

        // Populate product items
        val llOrderDetailItems = findViewById<LinearLayout>(R.id.llOrderDetailItems)
        var totalQuantity = 0

        for (item in order.items) {
            val itemView = layoutInflater.inflate(R.layout.item_checkout_product, llOrderDetailItems, false)

            val tvCheckoutProductName = itemView.findViewById<TextView>(R.id.tvCheckoutProductName)
            val tvCheckoutProductPrice = itemView.findViewById<TextView>(R.id.tvCheckoutProductPrice)
            val tvCheckoutProductPriceOriginal = itemView.findViewById<TextView>(R.id.tvCheckoutProductPriceOriginal)
            val tvCheckoutQuantity = itemView.findViewById<TextView>(R.id.tvCheckoutQuantity)
            val imgCheckoutProduct = itemView.findViewById<ImageView>(R.id.imgCheckoutProduct)

            tvCheckoutProductName.text = item.product.name
            tvCheckoutQuantity.text = "x${item.quantity}"

            if (item.product.imageUrl.isNotEmpty()) {
                val resId = resources.getIdentifier(item.product.imageUrl, "drawable", packageName)
                if (resId != 0) {
                    imgCheckoutProduct.setImageResource(resId)
                }
            }

            val activePrice = if (item.product.discounted > 0) {
                val discounted = item.product.price * (1 - item.product.discounted / 100.0)
                tvCheckoutProductPriceOriginal.text = "đ${formatter.format(item.product.price)}"
                tvCheckoutProductPriceOriginal.paintFlags = tvCheckoutProductPriceOriginal.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                tvCheckoutProductPriceOriginal.visibility = View.VISIBLE
                discounted
            } else {
                item.product.price
            }

            tvCheckoutProductPrice.text = "đ${formatter.format(activePrice)}"
            totalQuantity += item.quantity

            llOrderDetailItems.addView(itemView)
        }

        tvOrderDetailTotalItems.text = "Tổng ($totalQuantity mặt hàng)"
        tvOrderDetailTotalPrice.text = "đ${formatter.format(order.totalAmount)}"
        tvOrderDetailBottomTotalPrice.text = "đ${formatter.format(order.totalAmount)}"

        // Cancel order button
        btnCancelOrder.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Huỷ đơn hàng")
                .setMessage("Bạn có chắc chắn muốn huỷ đơn hàng #${order.id} không?")
                .setPositiveButton("Huỷ đơn") { _, _ ->
                    btnCancelOrder.isEnabled = false
                    OrderManager.cancelOrder(order.id,
                        onSuccess = {
                            updateStatusUI("Đã huỷ")
                            Toast.makeText(this, "Đã huỷ đơn hàng #${order.id}", Toast.LENGTH_SHORT).show()
                        },
                        onFailure = { e ->
                            btnCancelOrder.isEnabled = true
                            Toast.makeText(this, "Huỷ đơn thất bại: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                .setNegativeButton("Không", null)
                .show()
        }

        // Reorder button
        btnReorder.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Đặt lại đơn hàng")
                .setMessage("Bạn có muốn đặt lại đơn hàng #${order.id} không?")
                .setPositiveButton("Đặt lại") { _, _ ->
                    btnReorder.isEnabled = false
                    OrderManager.reorderOrder(order.id,
                        onSuccess = {
                            updateStatusUI("Chờ xác nhận")
                            btnReorder.isEnabled = true
                            Toast.makeText(this, "Đã đặt lại đơn hàng #${order.id}", Toast.LENGTH_SHORT).show()
                        },
                        onFailure = { e ->
                            btnReorder.isEnabled = true
                            Toast.makeText(this, "Đặt lại thất bại: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                .setNegativeButton("Không", null)
                .show()
        }
    }
}
