package com.example.buoi1

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.buoi1.databinding.HomeScreenBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Locale

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: HomeScreenBinding
    private val TAG = "HomeActivity"
    private lateinit var firestore: FirebaseFirestore

    private val productList = mutableListOf<Product>()
    private val sliderHandler = Handler(Looper.getMainLooper())
    private lateinit var sliderRunnable: Runnable

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "đang ở onStart nè")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "đang ở onResume nè")
        fetchProducts() // Tự động load lại sản phẩm khi quay lại màn hình
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "đang ở onPause nè")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "đang ở onStop nè")
    }

    override fun onDestroy() {
        super.onDestroy()
        sliderHandler.removeCallbacks(sliderRunnable)
        CartManager.removeListener(cartListener)
        Log.d(TAG, "đang ở onDestroy nè")
    }

    override fun onRestart() {
        super.onRestart()
        Log.d(TAG, "đang ở onRestart nè")
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.home_menu, menu)
        
        val cartItem = menu?.findItem(R.id.action_cart)
        val actionView = cartItem?.actionView
        if (actionView != null) {
            cartBadgeTextView = actionView.findViewById(R.id.tvCartBadge)
            actionView.setOnClickListener {
                startActivity(android.content.Intent(this, CartActivity::class.java))
            }
            updateCartBadge()
        }
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "đang ở onCreate nè")
        enableEdgeToEdge()
        binding = HomeScreenBinding.inflate(layoutInflater)
        setContentView(binding.root) // Changed to binding.root

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        firestore = FirebaseFirestore.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(binding.homeView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Only apply left/right padding to root; top goes to toolbar, bottom goes to bottomNav
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        // Toolbar gets top inset (status bar)
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        // BottomNav extends into system navigation bar area (white background)
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNav) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, systemBars.bottom)
            insets
        }

        // Add Product Button
//        binding.btnAddProduct.setOnClickListener {
//            showAddProductDialog()
//        }

        // Load categories from Firebase
        loadCategories()

        // Setup banner slider
        setupBannerSlider()

        // Fetch products from Firestore
        fetchProducts()

        // Setup Bottom Navigation
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    binding.scrollViewHome.visibility = android.view.View.VISIBLE
                    binding.toolbar.visibility = android.view.View.VISIBLE
                    binding.fragmentContainer.visibility = android.view.View.GONE
                    true
                }
                R.id.nav_products -> {
                    binding.scrollViewHome.visibility = android.view.View.GONE
                    binding.toolbar.visibility = android.view.View.GONE
                    binding.fragmentContainer.visibility = android.view.View.VISIBLE
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, ProductsFragment())
                        .commit()
                    true
                }
                R.id.nav_account -> {
                    binding.scrollViewHome.visibility = android.view.View.GONE
                    binding.toolbar.visibility = android.view.View.GONE
                    binding.fragmentContainer.visibility = android.view.View.VISIBLE
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, AccountFragment())
                        .commit()
                    true
                }
                else -> false
            }
        }
        
        CartManager.addListener(cartListener)
    }

    private val cartListener = { updateCartBadge() }

    private var cartBadgeTextView: TextView? = null

    private fun updateCartBadge() {
        val count = CartManager.getTotalItemCount()
        if (count > 0) {
            cartBadgeTextView?.visibility = android.view.View.VISIBLE
            cartBadgeTextView?.text = count.toString()
        } else {
            cartBadgeTextView?.visibility = android.view.View.GONE
        }
    }

    private fun loadCategories() {
        val db = FirebaseFirestore.getInstance()
        db.collection("categories")
            .get()
            .addOnSuccessListener { result ->
                binding.llBrands.removeAllViews()
                for (document in result) {
                    val category = Category(
                        name = document.getString("name") ?: "",
                        imageUrl = document.getString("imageUrl") ?: "",
                        description = document.getString("description") ?: ""
                    )
                    addCategoryView(category)
                }
                Log.d(TAG, "Loaded ${result.size()} categories from Firebase")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading categories", e)
            }
    }

    private fun addCategoryView(category: Category) {
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER_HORIZONTAL
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 16.dpToPx()
            }
        }

        val imgView = ImageView(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(64.dpToPx(), 64.dpToPx())
            background = resources.getDrawable(R.drawable.bg_circle, theme)
            clipToOutline = true
            scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = category.name
        }

        // Load image from drawable by name
        if (category.imageUrl.isNotEmpty()) {
            val resId = resources.getIdentifier(category.imageUrl, "drawable", packageName)
            if (resId != 0) {
                imgView.setImageResource(resId)
            } else {
                imgView.setImageResource(R.drawable.ic_image_placeholder)
            }
        } else {
            imgView.setImageResource(R.drawable.ic_image_placeholder)
        }

        val tvName = TextView(this).apply {
            text = category.name
            setTextColor(resources.getColor(R.color.colorText, theme))
            textSize = 12f
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 6.dpToPx()
            }
        }

        container.addView(imgView)
        container.addView(tvName)

        container.setOnClickListener {
            val intent = android.content.Intent(this, BrandProductsActivity::class.java).apply {
                putExtra("EXTRA_BRAND", category.name)
            }
            startActivity(intent)
        }

        binding.llBrands.addView(container)
    }

    private fun setupBannerSlider() {
        // Banner images
        val bannerList = listOf(
            R.drawable.banner1,
            R.drawable.banner2,
            R.drawable.banner3,
            R.drawable.banner4,
            R.drawable.banner5
        )

        val adapter = BannerAdapter(bannerList)
        binding.vpBanner.adapter = adapter

        // Dot indicators
        updateDots(0, bannerList.size)

        binding.vpBanner.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position, bannerList.size)
                sliderHandler.removeCallbacks(sliderRunnable)
                sliderHandler.postDelayed(sliderRunnable, 3000)
            }
        })

        // Auto scroll every 3 seconds
        sliderRunnable = Runnable {
            val nextItem = (binding.vpBanner.currentItem + 1) % bannerList.size
            binding.vpBanner.currentItem = nextItem
        }
        sliderHandler.postDelayed(sliderRunnable, 3000)
    }

    private fun updateDots(selected: Int, total: Int) {
        binding.llDots.removeAllViews()
        for (i in 0 until total) {
            val dot = android.widget.ImageView(this)
            val size = if (i == selected) 10 else 8
            val params = android.widget.LinearLayout.LayoutParams(size.dpToPx(), size.dpToPx())
            params.setMargins(6, 0, 6, 0)
            dot.layoutParams = params
            dot.setImageResource(
                if (i == selected) android.R.drawable.radiobutton_on_background
                else android.R.drawable.radiobutton_off_background
            )
            binding.llDots.addView(dot)
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun showAddProductDialog() {
        try {
            val layout = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(48, 32, 48, 16)
            }

            val edtName = android.widget.EditText(this).apply { hint = "Tên vợt" }
            layout.addView(edtName)

            val edtBrand = android.widget.EditText(this).apply { hint = "Hãng (VD: Yonex)" }
            layout.addView(edtBrand)

            val edtPrice = android.widget.EditText(this).apply {
                hint = "Giá (VD: 1500000)"
                inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            }
            layout.addView(edtPrice)

            val edtDescription = android.widget.EditText(this).apply { hint = "Mô tả (Description)" }
            layout.addView(edtDescription)

            val edtImageUrl = android.widget.EditText(this).apply { hint = "URL hình ảnh" }
            layout.addView(edtImageUrl)

            val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Thêm sản phẩm")
                .setView(layout)
                .setPositiveButton("Lưu") { dlg, _ ->
                    val name = edtName.text.toString().trim()
                    val brand = edtBrand.text.toString().trim()
                    val priceText = edtPrice.text.toString().trim()
                    val description = edtDescription.text.toString().trim()
                    val imageUrl = edtImageUrl.text.toString().trim()

                    if (name.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập tên vợt", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    val price = priceText.toDoubleOrNull() ?: 0.0
                    addProductToFirestore(Product(name = name, brand = brand, price = price, description = description, imageUrl = imageUrl))
                    dlg.dismiss()
                }
                .setNegativeButton("Hủy", null)
                .create()

            dialog.show()
        } catch (e: Exception) {
            Log.e(TAG, "Dialog error: ${e.message}", e)
            Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun addProductToFirestore(product: Product) {
        val db = FirebaseFirestore.getInstance()
        val data = hashMapOf(
            "name" to product.name,
            "brand" to product.brand,
            "price" to product.price,
            "description" to product.description,
            "imageUrl" to product.imageUrl
        )

        db.collection("products")
            .add(data)
            .addOnSuccessListener {
                Log.d(TAG, "Product added successfully")
                Toast.makeText(this, "Thêm sản phẩm thành công", Toast.LENGTH_SHORT).show()
                fetchProducts()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error adding product", e)
                Toast.makeText(this, "Lỗi thêm sản phẩm", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchProducts() {
        val db = FirebaseFirestore.getInstance()
        db.collection("products")
            .get()
            .addOnSuccessListener { result ->
                productList.clear()
                for (document in result) {
                    val product = Product(
                        name = document.getString("name") ?: "",
                        brand = document.getString("brand") ?: "",
                        categoryId = document.getString("categoryId") ?: "",
                        price = document.getDouble("price") ?: 0.0,
                        description = document.getString("description") ?: "",
                        specifications = document.getString("specifications") ?: "",
                        imageUrl = document.getString("imageUrl") ?: "",
                        isFavorite = document.getBoolean("isFavorite") ?: false,
                        discounted = document.getLong("discounted")?.toInt() ?: 0
                    )
                    productList.add(product)
                }
                updateProductListUI()
                Log.d(TAG, "Fetched ${productList.size} products")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error fetching products", exception)
                Toast.makeText(this, "Lỗi tải sản phẩm", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateProductListUI() {
        binding.glProducts.removeAllViews()
        val formatter = NumberFormat.getInstance(Locale("vi", "VN"))
        val padding = 16.dpToPx()
        val gap = 8.dpToPx()
        val screenWidth = resources.displayMetrics.widthPixels
        val itemWidth = (screenWidth - 2 * padding - gap) / 2

        for ((index, product) in productList.withIndex()) {
            val itemView = layoutInflater.inflate(R.layout.item_product, binding.glProducts, false)

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

            // Load image from local drawable by name
            if (product.imageUrl.isNotEmpty()) {
                val resId = resources.getIdentifier(product.imageUrl, "drawable", packageName)
                if (resId != 0) {
                    imgProduct.setImageResource(resId)
                    imgProduct.scaleType = ImageView.ScaleType.CENTER_INSIDE
                }
            }

            val row = index / 2
            val col = index % 2
            val params = android.widget.GridLayout.LayoutParams(
                android.widget.GridLayout.spec(row),
                android.widget.GridLayout.spec(col)
            ).apply {
                width = itemWidth
                setMargins(if (col == 0) 0 else gap, 0, 0, gap)
            }
            itemView.layoutParams = params

            // Handle product click
            itemView.setOnClickListener {
                val intent = android.content.Intent(this, ProductDetailActivity::class.java).apply {
                    putExtra("EXTRA_PRODUCT", product)
                }
                startActivity(intent)
            }

            binding.glProducts.addView(itemView)
        }
    }
}