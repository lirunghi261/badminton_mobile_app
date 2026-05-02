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

class AdminOrderDetailActivity : AppCompatActivity() {

    private val statusList = listOf("Chờ xác nhận", "Đang giao", "Thành công", "Đã huỷ")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_order_detail)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.topBarAdminOrderDetail)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, bars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bottomBarAdminOrderDetail)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, bars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnAdminOrderDetailBack).setOnClickListener { finish() }

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
        val tvOrderDetailId = findViewById<TextView>(R.id.tvAdminOrderDetailId)
        val tvOrderDetailStatus = findViewById<TextView>(R.id.tvAdminOrderDetailStatus)
        val tvOrderDetailDate = findViewById<TextView>(R.id.tvAdminOrderDetailDate)
        val tvOrderDetailAddress = findViewById<TextView>(R.id.tvAdminOrderDetailAddress)
        val tvOrderDetailPaymentMethod = findViewById<TextView>(R.id.tvAdminOrderDetailPaymentMethod)
        val tvOrderDetailTotalItems = findViewById<TextView>(R.id.tvAdminOrderDetailTotalItems)
        val tvOrderDetailTotalPrice = findViewById<TextView>(R.id.tvAdminOrderDetailTotalPrice)
        val tvOrderDetailBottomTotalPrice = findViewById<TextView>(R.id.tvAdminOrderDetailBottomTotalPrice)
        val btnUpdateStatus = findViewById<Button>(R.id.btnAdminUpdateStatus)

        tvOrderDetailId.text = "Đơn: #${order.id}"
        tvOrderDetailDate.text = order.date
        tvOrderDetailAddress.text = order.address
        tvOrderDetailPaymentMethod.text = order.paymentMethod

        // Update status badge UI
        fun updateStatusBadge(status: String) {
            tvOrderDetailStatus.text = status
            val statusColor = when (status) {
                "Chờ xác nhận" -> "#EF6C00"
                "Đang giao" -> "#1565C0"
                "Thành công" -> "#2E7D32"
                "Đã huỷ" -> "#999999"
                else -> "#E64A19"
            }
            tvOrderDetailStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor(statusColor)
            )
        }

        updateStatusBadge(order.status)

        // Populate product items
        val llOrderDetailItems = findViewById<LinearLayout>(R.id.llAdminOrderDetailItems)
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

        // Admin: Update Status button
        btnUpdateStatus.setOnClickListener {
            val statusOptions = statusList.toTypedArray()
            val currentIndex = statusList.indexOf(order.status).coerceAtLeast(0)

            AlertDialog.Builder(this)
                .setTitle("Cập nhật trạng thái\nĐơn #${order.id}")
                .setSingleChoiceItems(statusOptions, currentIndex) { dialog, which ->
                    val newStatus = statusList[which]
                    if (newStatus != tvOrderDetailStatus.text.toString()) {
                        btnUpdateStatus.isEnabled = false
                        OrderManager.updateOrderStatus(order.id, newStatus,
                            onSuccess = {
                                btnUpdateStatus.isEnabled = true
                                updateStatusBadge(newStatus)
                                Toast.makeText(this, "Đã cập nhật trạng thái thành \"$newStatus\"", Toast.LENGTH_SHORT).show()
                            },
                            onFailure = { e ->
                                btnUpdateStatus.isEnabled = true
                                Toast.makeText(this, "Lỗi cập nhật: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Huỷ", null)
                .show()
        }
    }
}
