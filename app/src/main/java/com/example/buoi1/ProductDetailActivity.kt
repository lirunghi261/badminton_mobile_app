package com.example.buoi1

import android.content.Intent
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Locale

class ProductDetailActivity : AppCompatActivity() {
    private var isFavorited: Boolean = false
    private lateinit var btnFavorite: ImageButton


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_product_detail)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.topBar)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, bars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bottomBar)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, bars.bottom)
            insets
        }

        // Get product from intent
        val product = intent.getSerializableExtra("EXTRA_PRODUCT") as? Product ?: return

        val formatter = NumberFormat.getInstance(Locale("vi", "VN"))
        
        val tvDetailPrice = findViewById<TextView>(R.id.tvDetailPrice)
        val tvDetailPriceOriginal = findViewById<TextView>(R.id.tvDetailPriceOriginal)
        val tvBuyNowPrice = findViewById<TextView>(R.id.tvBuyNowPrice)

        if (product.discounted > 0) {
            val discountedPrice = product.price * (1 - product.discounted / 100.0)
            val discountedStr = "đ${formatter.format(discountedPrice)}"
            
            tvDetailPrice.text = discountedStr
            tvBuyNowPrice.text = discountedStr
            
            tvDetailPriceOriginal.text = "đ${formatter.format(product.price)}"
            tvDetailPriceOriginal.paintFlags = tvDetailPriceOriginal.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            tvDetailPriceOriginal.visibility = View.VISIBLE
        } else {
            val priceFormatted = "đ${formatter.format(product.price)}"
            tvDetailPrice.text = priceFormatted
            tvBuyNowPrice.text = priceFormatted
            tvDetailPriceOriginal.visibility = View.GONE
        }

        // Bind data to views
        findViewById<TextView>(R.id.tvDetailName).text = product.name
        findViewById<TextView>(R.id.tvDetailBrand).text = product.brand

        val tvDetailDescription = findViewById<TextView>(R.id.tvDetailDescription)
        val tvSeeMore = findViewById<TextView>(R.id.tvSeeMore)

        if (product.description.trim().isNotEmpty()) {
            tvDetailDescription.text = product.description
            tvSeeMore.visibility = View.VISIBLE
        } else {
            tvDetailDescription.text = "Đang cập nhật..."
            tvSeeMore.visibility = View.GONE
        }

        setupTabsAndSpecs(product.specifications)

        val imgProductDetail = findViewById<ImageView>(R.id.imgProductDetail)
        val imgThumb1 = findViewById<ImageView>(R.id.imgThumb1)
        val imgThumb2 = findViewById<ImageView>(R.id.imgThumb2)
        val imgThumb3 = findViewById<ImageView>(R.id.imgThumb3)

        fun setupGallery(drawable: Drawable) {
            imgProductDetail.setImageDrawable(drawable)
            imgThumb1.setImageDrawable(drawable)
            imgThumb2.setImageDrawable(drawable)
            imgThumb3.setImageDrawable(drawable)

            // Setup Thumbs after layout width/height is calculated
            imgThumb2.post {
                val matrix2 = Matrix()
                val scale2 = imgThumb2.width.toFloat() / drawable.intrinsicWidth
                val zoom2 = scale2 * 2.5f // zoom in strongly on the racket head
                matrix2.setScale(zoom2, zoom2)
                val tx2 = (imgThumb2.width - (drawable.intrinsicWidth * zoom2)) / 2f
                matrix2.postTranslate(tx2, 0f)
                imgThumb2.imageMatrix = matrix2

                val matrix3 = Matrix()
                val scale3 = imgThumb3.width.toFloat() / drawable.intrinsicWidth
                val zoom3 = scale3 * 2.5f // zoom in strongly on the handle
                matrix3.setScale(zoom3, zoom3)
                val tx3 = (imgThumb3.width - (drawable.intrinsicWidth * zoom3)) / 2f
                val ty3 = imgThumb3.height - (drawable.intrinsicHeight * zoom3)
                matrix3.postTranslate(tx3, ty3)
                imgThumb3.imageMatrix = matrix3
            }

            fun selectThumb(index: Int) {
                imgThumb1.alpha = if (index == 1) 1.0f else 0.4f
                imgThumb2.alpha = if (index == 2) 1.0f else 0.4f
                imgThumb3.alpha = if (index == 3) 1.0f else 0.4f

                if (index == 1) {
                    imgProductDetail.scaleType = ImageView.ScaleType.CENTER_INSIDE
                    imgProductDetail.imageMatrix = null
                } else if (index == 2) {
                    imgProductDetail.scaleType = ImageView.ScaleType.MATRIX
                    val m = Matrix()
                    val s = imgProductDetail.width.toFloat() / drawable.intrinsicWidth
                    val zoom = s * 1.5f
                    m.setScale(zoom, zoom)
                    val tx = (imgProductDetail.width - (drawable.intrinsicWidth * zoom)) / 2f
                    m.postTranslate(tx, 0f)
                    imgProductDetail.imageMatrix = m
                } else if (index == 3) {
                    imgProductDetail.scaleType = ImageView.ScaleType.MATRIX
                    val m = Matrix()
                    val s = imgProductDetail.width.toFloat() / drawable.intrinsicWidth
                    val zoom = s * 1.5f
                    m.setScale(zoom, zoom)
                    val tx = (imgProductDetail.width - (drawable.intrinsicWidth * zoom)) / 2f
                    val ty = imgProductDetail.height - (drawable.intrinsicHeight * zoom)
                    m.postTranslate(tx, ty)
                    imgProductDetail.imageMatrix = m
                }
            }

            // Initial selection
            selectThumb(1)

            imgThumb1.setOnClickListener { selectThumb(1) }
            imgThumb2.setOnClickListener { selectThumb(2) }
            imgThumb3.setOnClickListener { selectThumb(3) }
        }

        // Load image from local drawable by name (same as home screen)
        if (product.imageUrl.isNotEmpty()) {
            val resId = resources.getIdentifier(product.imageUrl, "drawable", packageName)
            if (resId != 0) {
                val drawable = ContextCompat.getDrawable(this, resId)
                if (drawable != null) {
                    setupGallery(drawable)
                } else {
                    imgProductDetail.setImageResource(resId)
                }
            } else {
                 // Fallback to Glide if it's considered an external URL
                 Glide.with(this)
                    .load(product.imageUrl)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(object : CustomTarget<Drawable>() {
                        override fun onResourceReady(
                            resource: Drawable,
                            transition: Transition<in Drawable>?
                        ) {
                            setupGallery(resource)
                        }
                        override fun onLoadCleared(placeholder: Drawable?) {}
                    })
            }
        }

        // Back button
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Favorite / Heart toggle
        isFavorited = product.isFavorite
        btnFavorite = findViewById<ImageButton>(R.id.btnFavorite)
        updateFavoriteIcon()
        
        btnFavorite.setOnClickListener {
            toggleFavorite(product.name)
        }

        // Tự động load lại trạng thái mới nhất từ Firebase
        syncFavoriteStatus(product.name)


        // Open full description
        tvSeeMore.setOnClickListener {
            val intent = Intent(this, ProductDescriptionActivity::class.java).apply {
                putExtra("EXTRA_DESC", product.description)
            }
            startActivity(intent)
        }

        val btnAddToCart = findViewById<LinearLayout>(R.id.btnAddToCart)
        btnAddToCart.setOnClickListener {
            CartManager.addToCart(product, 1)
            Toast.makeText(this, "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show()
        }

        val btnBuyNow = findViewById<LinearLayout>(R.id.btnBuyNow)
        btnBuyNow.setOnClickListener {
            val intent = Intent(this, CheckoutActivity::class.java).apply {
                putExtra("EXTRA_DIRECT_BUY_PRODUCT", product)
            }
            startActivity(intent)
        }

        val btnCartIcon = findViewById<ImageButton>(R.id.btnCart)
        btnCartIcon.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        // Setup Cart Badge Listener
        CartManager.addListener(cartListener)
        updateCartBadge()

        // Load related products
        loadRelatedProducts(product.brand, product.name)
    }

    private val cartListener = { updateCartBadge() }

    private fun updateCartBadge() {
        val tvCartBadge = findViewById<TextView>(R.id.tvCartBadge)
        val count = CartManager.getTotalItemCount()
        if (count > 0) {
            tvCartBadge.visibility = View.VISIBLE
            tvCartBadge.text = count.toString()
        } else {
            tvCartBadge.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        CartManager.removeListener(cartListener)
    }

    private fun setupTabsAndSpecs(specifications: String) {
        val tabDescription = findViewById<LinearLayout>(R.id.tabDescription)
        val tabSpecs = findViewById<LinearLayout>(R.id.tabSpecs)

        val tvTabDescriptionTitle = findViewById<TextView>(R.id.tvTabDescriptionTitle)
        val tabDescriptionIndicator = findViewById<View>(R.id.tabDescriptionIndicator)

        val tvTabSpecsTitle = findViewById<TextView>(R.id.tvTabSpecsTitle)
        val tabSpecsIndicator = findViewById<View>(R.id.tabSpecsIndicator)

        val llDescriptionContainer = findViewById<LinearLayout>(R.id.llDescriptionContainer)
        val llSpecsContainer = findViewById<LinearLayout>(R.id.llSpecsContainer)

        fun selectTab(isDescription: Boolean) {
            if (isDescription) {
                tvTabDescriptionTitle.setTextColor(Color.parseColor("#E64A19"))
                tabDescriptionIndicator.setBackgroundColor(Color.parseColor("#E64A19"))
                tvTabSpecsTitle.setTextColor(Color.parseColor("#666666"))
                tabSpecsIndicator.setBackgroundColor(Color.parseColor("#EEEEEE"))

                llDescriptionContainer.visibility = View.VISIBLE
                llSpecsContainer.visibility = View.GONE
            } else {
                tvTabSpecsTitle.setTextColor(Color.parseColor("#E64A19"))
                tabSpecsIndicator.setBackgroundColor(Color.parseColor("#E64A19"))
                tvTabDescriptionTitle.setTextColor(Color.parseColor("#666666"))
                tabDescriptionIndicator.setBackgroundColor(Color.parseColor("#EEEEEE"))

                llDescriptionContainer.visibility = View.GONE
                llSpecsContainer.visibility = View.VISIBLE
            }
        }

        tabDescription.setOnClickListener { selectTab(true) }
        tabSpecs.setOnClickListener { selectTab(false) }

        // Initial state
        selectTab(true)

        // Populate Specs Table
        val tableSpecs = findViewById<TableLayout>(R.id.tableSpecs)
        val tvEmptySpecs = findViewById<TextView>(R.id.tvEmptySpecs)

        if (specifications.isNotBlank()) {
            tableSpecs.visibility = View.VISIBLE
            tvEmptySpecs.visibility = View.GONE
            tableSpecs.removeAllViews()

            val specsList = specifications.split(";")
            for (spec in specsList) {
                if (!spec.contains(":")) continue
                val parts = spec.split(":", limit = 2)
                val key = parts[0].trim()
                val value = parts[1].trim()

                val row = TableRow(this).apply {
                    setPadding(0, 12.dpToPx(), 0, 12.dpToPx())
                }

                val tvKey = TextView(this).apply {
                    text = key
                    setTextColor(Color.parseColor("#333333"))
                    setTypeface(null, Typeface.BOLD)
                    textSize = 14f
                    layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
                    setPadding(8.dpToPx(), 0, 8.dpToPx(), 0)
                }

                val tvValue = TextView(this).apply {
                    text = value
                    setTextColor(Color.parseColor("#666666"))
                    textSize = 14f
                    layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1.5f)
                    setPadding(8.dpToPx(), 0, 8.dpToPx(), 0)
                }

                row.addView(tvKey)
                row.addView(tvValue)
                tableSpecs.addView(row)

                // Divider line
                val dividerRow = TableRow(this)
                val dividerView = View(this).apply {
                    setBackgroundColor(Color.parseColor("#EEEEEE"))
                    layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 1.dpToPx())
                }
                val dividerParams = TableRow.LayoutParams().apply { span = 2 }
                dividerView.layoutParams = dividerParams
                dividerRow.addView(dividerView)
                tableSpecs.addView(dividerRow)
            }
        } else {
            tableSpecs.visibility = View.GONE
            tvEmptySpecs.visibility = View.VISIBLE
        }
    }

    private fun loadRelatedProducts(brand: String, currentProductName: String) {
        val glRelated = findViewById<GridLayout>(R.id.glRelatedProducts)
        val tvRelatedTitle = findViewById<TextView>(R.id.tvRelatedTitle)

        // Hide initially
        glRelated.visibility = View.GONE
        tvRelatedTitle.visibility = View.GONE

        val db = FirebaseFirestore.getInstance()
        db.collection("products")
            .whereEqualTo("brand", brand)
            .get()
            .addOnSuccessListener { result ->
                val relatedProducts = result.mapNotNull { doc ->
                    Product(
                        name = doc.getString("name") ?: "",
                        brand = doc.getString("brand") ?: "",
                        price = doc.getDouble("price") ?: 0.0,
                        description = doc.getString("description") ?: "",
                        specifications = doc.getString("specifications") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: ""
                    )
                }.filter { it.name != currentProductName }.take(4) // Take up to 4

                if (relatedProducts.isNotEmpty()) {
                    tvRelatedTitle.visibility = View.VISIBLE
                    glRelated.visibility = View.VISIBLE
                    renderRelatedProducts(relatedProducts, glRelated)
                }
            }
            .addOnFailureListener { e ->
                Log.e("ProductDetail", "Error fetching related products", e)
            }
    }

    private fun renderRelatedProducts(products: List<Product>, glProducts: GridLayout) {
        glProducts.removeAllViews()
        val formatter = NumberFormat.getInstance(Locale("vi", "VN"))
        val padding = 12.dpToPx()
        val gap = 8.dpToPx()
        val screenWidth = resources.displayMetrics.widthPixels
        val itemWidth = (screenWidth - 2 * padding - gap) / 2

        for ((index, product) in products.withIndex()) {
            val itemView = layoutInflater.inflate(R.layout.item_product, glProducts, false)

            itemView.findViewById<TextView>(R.id.tvProductName).text = product.name
            itemView.findViewById<TextView>(R.id.tvProductBrand).text = product.brand
            itemView.findViewById<TextView>(R.id.tvProductPrice).text = "đ${formatter.format(product.price)}"

            val imgProduct = itemView.findViewById<ImageView>(R.id.imgProduct)
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

    private fun toggleFavorite(productName: String) {
        isFavorited = !isFavorited
        updateFavoriteIcon()

        val db = FirebaseFirestore.getInstance()
        db.collection("products")
            .whereEqualTo("name", productName)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    db.collection("products").document(document.id)
                        .update("isFavorite", isFavorited)
                        .addOnSuccessListener {
                            Log.d("ProductDetail", "Cập nhật yêu thích thành công!")
                        }
                        .addOnFailureListener { e ->
                            Log.e("ProductDetail", "Lỗi cập nhật yêu thích", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ProductDetail", "Lỗi tìm sản phẩm", e)
            }
    }

    private fun syncFavoriteStatus(productName: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("products")
            .whereEqualTo("name", productName)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val freshFavorite = document.getBoolean("isFavorite") ?: false
                    if (isFavorited != freshFavorite) {
                        isFavorited = freshFavorite
                        updateFavoriteIcon()
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ProductDetail", "Lỗi đồng bộ yêu thích", e)
            }
    }

    private fun updateFavoriteIcon() {
        btnFavorite.setImageResource(
            if (isFavorited) R.drawable.ic_heart_filled
            else R.drawable.ic_heart_outline
        )
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}
