package com.example.buoi1

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Locale

class CartActivity : AppCompatActivity() {

    private val TAG = "CartActivity"
    private val selectedItems = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_cart)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.topBarCart)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, bars.top, v.paddingRight, v.paddingBottom)
            insets
        }
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bottomBarCart)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, bars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnCartBack).setOnClickListener { finish() }

        val cbSelectAll = findViewById<CheckBox>(R.id.cbSelectAll)
        cbSelectAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedItems.addAll(CartManager.getCartItems().map { it.product.name })
            } else {
                selectedItems.clear()
            }
            renderCartItems()
        }

        findViewById<TextView>(R.id.btnCartClear).setOnClickListener {
            selectedItems.forEach { CartManager.removeFromCart(it) }
            selectedItems.clear()
            renderCartItems()
        }

        // Initialize with all selected
        selectedItems.addAll(CartManager.getCartItems().map { it.product.name })

        renderCartItems()
        fetchRecommendedProducts()
    }

    override fun onResume() {
        super.onResume()
        // Re-render cart items when returning from another screen (like Product Detail)
        renderCartItems()
    }

    private fun renderCartItems() {
        val items = CartManager.getCartItems()
        val llCartItems = findViewById<LinearLayout>(R.id.llCartItems)
        val layoutCartEmpty = findViewById<LinearLayout>(R.id.layoutCartEmpty)
        val tvCartTitle = findViewById<TextView>(R.id.tvCartTitle)
        
        llCartItems.removeAllViews()
        val totalCount = CartManager.getTotalItemCount()
        tvCartTitle.text = "Giỏ hàng ($totalCount)"
        
        if (items.isEmpty()) {
            layoutCartEmpty.visibility = View.VISIBLE
            updateTotalPrice()
            return
        }

        layoutCartEmpty.visibility = View.GONE
        val formatter = NumberFormat.getInstance(Locale("vi", "VN"))

        for (item in items) {
            val itemView = layoutInflater.inflate(R.layout.item_cart, llCartItems, false)
            
            val cbCartItem = itemView.findViewById<CheckBox>(R.id.cbCartItem)
            val imgCartProduct = itemView.findViewById<ImageView>(R.id.imgCartProduct)
            val tvCartProductName = itemView.findViewById<TextView>(R.id.tvCartProductName)
            val tvCartProductPrice = itemView.findViewById<TextView>(R.id.tvCartProductPrice)
            val tvMinus = itemView.findViewById<TextView>(R.id.tvMinus)
            val tvPlus = itemView.findViewById<TextView>(R.id.tvPlus)
            val tvQuantity = itemView.findViewById<TextView>(R.id.tvQuantity)

            tvCartProductName.text = item.product.name
            
            val activePrice = if (item.product.discounted > 0) {
                item.product.price * (1 - item.product.discounted / 100.0)
            } else {
                item.product.price
            }
            tvCartProductPrice.text = "đ${formatter.format(activePrice)}"
            tvQuantity.text = item.quantity.toString()

            cbCartItem.isChecked = selectedItems.contains(item.product.name)
            cbCartItem.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedItems.add(item.product.name)
                else selectedItems.remove(item.product.name)
                updateTotalPrice()
                
                val cbSelectAll = findViewById<CheckBox>(R.id.cbSelectAll)
                cbSelectAll.setOnCheckedChangeListener(null)
                cbSelectAll.isChecked = selectedItems.size == items.size
                // Re-arm listener
                cbSelectAll.setOnCheckedChangeListener { _, isAllChecked ->
                    if (isAllChecked) selectedItems.addAll(items.map { it.product.name })
                    else selectedItems.clear()
                    renderCartItems()
                }
            }

            if (item.product.imageUrl.isNotEmpty()) {
                val resId = resources.getIdentifier(item.product.imageUrl, "drawable", packageName)
                if (resId != 0) {
                    imgCartProduct.setImageResource(resId)
                }
            }

            tvMinus.setOnClickListener {
                CartManager.updateQuantity(item.product.name, item.quantity - 1)
                renderCartItems()
            }

            tvPlus.setOnClickListener {
                CartManager.updateQuantity(item.product.name, item.quantity + 1)
                renderCartItems()
            }

            llCartItems.addView(itemView)
        }
        
        val cbSelectAll = findViewById<CheckBox>(R.id.cbSelectAll)
        cbSelectAll.setOnCheckedChangeListener(null)
        cbSelectAll.isChecked = items.isNotEmpty() && selectedItems.size == items.size
        cbSelectAll.setOnCheckedChangeListener { _, isAllChecked ->
            if (isAllChecked) selectedItems.addAll(items.map { it.product.name })
            else selectedItems.clear()
            renderCartItems()
        }

        updateTotalPrice()
    }

    private fun updateTotalPrice() {
        var total = 0.0
        val items = CartManager.getCartItems()
        
        for (item in items) {
            if (selectedItems.contains(item.product.name)) {
                val activePrice = if (item.product.discounted > 0) {
                    item.product.price * (1 - item.product.discounted / 100.0)
                } else {
                    item.product.price
                }
                total += activePrice * item.quantity
            }
        }

        val formatter = NumberFormat.getInstance(Locale("vi", "VN"))
        findViewById<TextView>(R.id.tvCartTotal).text = "đ${formatter.format(total)}"
        
        val btnCheckout = findViewById<Button>(R.id.btnCartCheckout)
        btnCheckout.text = "Mua hàng (${selectedItems.size})"
        btnCheckout.setOnClickListener {
            if (selectedItems.isEmpty()) {
                android.widget.Toast.makeText(this, "Vui lòng chọn sản phẩm để thanh toán", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                val intent = android.content.Intent(this, CheckoutActivity::class.java).apply {
                    putStringArrayListExtra("EXTRA_SELECTED_ITEMS", java.util.ArrayList(selectedItems))
                }
                startActivity(intent)
            }
        }
    }

    private fun fetchRecommendedProducts() {
        val glRecommended = findViewById<GridLayout>(R.id.glCartRecommended)
        val db = FirebaseFirestore.getInstance()
        
        db.collection("products")
            .limit(6) // Fetch arbitrary 6 products for recommendation
            .get()
            .addOnSuccessListener { result ->
                val products = result.mapNotNull { doc ->
                    Product(
                        name = doc.getString("name") ?: "",
                        brand = doc.getString("brand") ?: "",
                        price = doc.getDouble("price") ?: 0.0,
                        description = doc.getString("description") ?: "",
                        specifications = doc.getString("specifications") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: "",
                        isFavorite = doc.getBoolean("isFavorite") ?: false,
                        discounted = doc.getLong("discounted")?.toInt() ?: 0
                    )
                }
                renderRecommendedProducts(products, glRecommended)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching recommended products: ${e.message}", e)
            }
    }

    private fun renderRecommendedProducts(products: List<Product>, glProducts: GridLayout) {
        glProducts.removeAllViews()
        val formatter = NumberFormat.getInstance(Locale("vi", "VN"))
        val padding = 16.dpToPx()
        val gap = 8.dpToPx()
        val screenWidth = resources.displayMetrics.widthPixels
        val itemWidth = (screenWidth - 2 * padding - gap) / 2

        for ((index, product) in products.withIndex()) {
            val itemView = layoutInflater.inflate(R.layout.item_product, glProducts, false)

            val tvName = itemView.findViewById<TextView>(R.id.tvProductName)
            val tvBrand = itemView.findViewById<TextView>(R.id.tvProductBrand)
            val tvPrice = itemView.findViewById<TextView>(R.id.tvProductPrice)
            val tvPriceOriginal = itemView.findViewById<TextView>(R.id.tvProductPriceOriginal)
            val tvDiscountBadge = itemView.findViewById<TextView>(R.id.tvProductDiscountBadge)
            val imgProduct = itemView.findViewById<ImageView>(R.id.imgProduct)

            tvName.text = product.name
            tvBrand.text = product.brand

            if (product.discounted > 0) {
                val discountedPrice = product.price * (1 - product.discounted / 100.0)
                tvPrice.text = "đ${formatter.format(discountedPrice)}"

                tvPriceOriginal.text = "đ${formatter.format(product.price)}"
                tvPriceOriginal.paintFlags = tvPriceOriginal.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                tvPriceOriginal.visibility = View.VISIBLE

                tvDiscountBadge.text = "-${product.discounted}%"
                tvDiscountBadge.visibility = View.VISIBLE
            } else {
                tvPrice.text = "đ${formatter.format(product.price)}"
                tvPriceOriginal.text = "đ0"
                tvPriceOriginal.visibility = View.INVISIBLE
                tvDiscountBadge.visibility = View.INVISIBLE
            }

            if (product.imageUrl.isNotEmpty()) {
                val resId = resources.getIdentifier(product.imageUrl, "drawable", packageName)
                if (resId != 0) {
                    imgProduct.setImageResource(resId)
                    imgProduct.scaleType = ImageView.ScaleType.CENTER_INSIDE
                }
            }

            val row = index / 2
            val col = index % 2
            val params = GridLayout.LayoutParams(
                GridLayout.spec(row),
                GridLayout.spec(col)
            ).apply {
                width = itemWidth
                setMargins(if (col == 0) 0 else gap, 0, 0, gap)
            }
            itemView.layoutParams = params

            itemView.setOnClickListener {
                val intent = Intent(this, ProductDetailActivity::class.java).apply {
                    putExtra("EXTRA_PRODUCT", product)
                }
                startActivity(intent)
            }

            glProducts.addView(itemView)
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}
