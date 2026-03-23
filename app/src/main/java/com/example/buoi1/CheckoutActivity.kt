package com.example.buoi1

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.NumberFormat
import java.util.Locale

class CheckoutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_checkout)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.topBarCheckout)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, bars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bottomBarCheckout)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, bars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnCheckoutBack).setOnClickListener { finish() }

        val llAddressContainer = findViewById<LinearLayout>(R.id.llAddressContainer)
        llAddressContainer.setOnClickListener {
            startActivity(Intent(this, AddressSelectionActivity::class.java))
        }

        val directBuyProduct = intent.getSerializableExtra("EXTRA_DIRECT_BUY_PRODUCT") as? Product

        val itemsToCheckout = if (directBuyProduct != null) {
            listOf(CartItem(directBuyProduct, 1))
        } else {
            val selectedItemNames = intent.getStringArrayListExtra("EXTRA_SELECTED_ITEMS") ?: arrayListOf()
            if (selectedItemNames.isEmpty()) {
                Toast.makeText(this, "Không có sản phẩm nào để thanh toán", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            CartManager.getCartItems().filter { it.product.name in selectedItemNames }
        }

        val llCheckoutItems = findViewById<LinearLayout>(R.id.llCheckoutItems)
        val tvCheckoutTotalItems = findViewById<TextView>(R.id.tvCheckoutTotalItems)
        val tvCheckoutTotalPrice = findViewById<TextView>(R.id.tvCheckoutTotalPrice)
        val formatter = NumberFormat.getInstance(Locale("vi", "VN"))

        var totalPrice = 0.0
        var totalQuantity = 0

        for (item in itemsToCheckout) {
            val itemView = layoutInflater.inflate(R.layout.item_checkout_product, llCheckoutItems, false)

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

            totalPrice += activePrice * item.quantity
            totalQuantity += item.quantity

            llCheckoutItems.addView(itemView)
        }

        tvCheckoutTotalItems.text = "Tổng ($totalQuantity mặt hàng)"
        tvCheckoutTotalPrice.text = "đ${formatter.format(totalPrice)}"

        val btnCheckoutPlaceOrder = findViewById<Button>(R.id.btnCheckoutPlaceOrder)
        btnCheckoutPlaceOrder.setOnClickListener {
            // Process the order
            val note = findViewById<EditText>(R.id.etCheckoutNote).text.toString().trim()
            val promo = findViewById<EditText>(R.id.etCheckoutPromo).text.toString().trim()
            val paymentGroup = findViewById<RadioGroup>(R.id.rgPaymentMethods)
            val isCOD = paymentGroup.checkedRadioButtonId == R.id.rbPaymentCOD

            // Clear purchased items from the cart if not direct buy
            if (directBuyProduct == null) {
                val currentNames = itemsToCheckout.map { it.product.name }
                currentNames.forEach { CartManager.removeFromCart(it) }
            }

            val paymentMethod = if (isCOD) "Thanh toán khi nhận hàng" else "Chuyển khoản ngân hàng"
            
            val order = Order(
                id = java.util.UUID.randomUUID().toString().substring(0, 8).uppercase(java.util.Locale.ROOT),
                date = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
                totalAmount = totalPrice,
                items = itemsToCheckout,
                status = "Chờ xác nhận",
                paymentMethod = paymentMethod,
                address = UserManager.getSelectedAddress().ifEmpty { "Chưa có địa chỉ" }
            )
            OrderManager.addOrder(order)
            
            Toast.makeText(applicationContext, "Đặt hàng thành công! ($paymentMethod)", Toast.LENGTH_LONG).show()

            // Navigate back to Home and clear backstack
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        val tvCheckoutAddress = findViewById<TextView>(R.id.tvCheckoutAddress)
        val selectedAddress = UserManager.getSelectedAddress()
        if (selectedAddress.isNotEmpty()) {
            tvCheckoutAddress.text = selectedAddress
        } else {
            tvCheckoutAddress.text = "Vui lòng thêm địa chỉ"
        }
    }
}
