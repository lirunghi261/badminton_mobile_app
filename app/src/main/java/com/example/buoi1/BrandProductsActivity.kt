package com.example.buoi1

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Locale

class BrandProductsActivity : AppCompatActivity() {

    private val TAG = "BrandProductsActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_brand_products)

        val brandName = intent.getStringExtra("EXTRA_BRAND") ?: ""

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.brandTopBar)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, bars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        // Title
        val title = "VỢT CẦU LÔNG ${brandName.uppercase()}"
        findViewById<TextView>(R.id.tvBrandTitle).text = title

        // Back button
        findViewById<ImageButton>(R.id.btnBrandBack).setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        val brandName = intent.getStringExtra("EXTRA_BRAND") ?: ""
        if (brandName.isNotEmpty()) {
            fetchProductsByBrand(brandName)
        }
    }


    private fun fetchProductsByBrand(brand: String) {
        val layoutLoading = findViewById<View>(R.id.layoutLoading)
        val layoutEmpty = findViewById<View>(R.id.layoutEmpty)
        val scrollViewProducts = findViewById<View>(R.id.scrollViewProducts)
        val glProducts = findViewById<GridLayout>(R.id.glBrandProducts)

        layoutLoading.visibility = View.VISIBLE
        layoutEmpty.visibility = View.GONE
        scrollViewProducts.visibility = View.GONE

        val db = FirebaseFirestore.getInstance()
        db.collection("products")
            .whereEqualTo("brand", brand)
            .get()
            .addOnSuccessListener { result ->
                layoutLoading.visibility = View.GONE
                val products = result.mapNotNull { doc ->
                    Product(
                        name = doc.getString("name") ?: "",
                        brand = doc.getString("brand") ?: "",
                        categoryId = doc.getString("categoryId") ?: "",
                        price = doc.getDouble("price") ?: 0.0,
                        description = doc.getString("description") ?: "",
                        specifications = doc.getString("specifications") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: "",
                        isFavorite = doc.getBoolean("isFavorite") ?: false,
                        discounted = doc.getLong("discounted")?.toInt() ?: 0
                    )
                }

                if (products.isEmpty()) {
                    layoutEmpty.visibility = View.VISIBLE
                } else {
                    scrollViewProducts.visibility = View.VISIBLE
                    renderProducts(products, glProducts)
                }
            }
            .addOnFailureListener { e ->
                layoutLoading.visibility = View.GONE
                layoutEmpty.visibility = View.VISIBLE
                Log.e(TAG, "Error fetching products: ${e.message}", e)
                Toast.makeText(this, "Lỗi tải sản phẩm", Toast.LENGTH_SHORT).show()
            }
    }

    private fun renderProducts(products: List<Product>, glProducts: GridLayout) {
        glProducts.removeAllViews()
        val formatter = NumberFormat.getInstance(Locale("vi", "VN"))
        val padding = 12.dpToPx()
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
                tvPriceOriginal.visibility = android.view.View.VISIBLE

                tvDiscountBadge.text = "-${product.discounted}%"
                tvDiscountBadge.visibility = android.view.View.VISIBLE
            } else {
                tvPrice.text = "đ${formatter.format(product.price)}"
                tvPriceOriginal.text = "đ0" // preserve text layout size
                tvPriceOriginal.visibility = android.view.View.INVISIBLE
                tvDiscountBadge.visibility = android.view.View.INVISIBLE
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

            // Open product detail on click
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
