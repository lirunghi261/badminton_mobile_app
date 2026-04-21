package com.example.buoi1

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Locale

class FavoritesActivity : AppCompatActivity() {

    private val TAG = "FavoritesActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_favorites)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.topBarFav)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, bars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnFavBack).setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        fetchFavoriteProducts()
    }


    private fun fetchFavoriteProducts() {
        val glFavorites = findViewById<GridLayout>(R.id.glFavorites)
        val layoutFavEmpty = findViewById<LinearLayout>(R.id.layoutFavEmpty)
        val db = FirebaseFirestore.getInstance()

        db.collection("products")
            .whereEqualTo("isFavorite", true)
            .get()
            .addOnSuccessListener { result ->
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
                    layoutFavEmpty.visibility = View.VISIBLE
                    glFavorites.visibility = View.GONE
                } else {
                    layoutFavEmpty.visibility = View.GONE
                    glFavorites.visibility = View.VISIBLE
                    renderProducts(products, glFavorites)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching favorite products: ${e.message}", e)
            }
    }

    private fun renderProducts(products: List<Product>, glProducts: GridLayout) {
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
