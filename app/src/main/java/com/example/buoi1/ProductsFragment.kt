package com.example.buoi1

import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Locale

class ProductsFragment : Fragment() {
    private val allProducts = mutableListOf<Product>()
    private val formatter = NumberFormat.getInstance(Locale("vi", "VN"))

    private var selectedBrand: String? = null
    private var onlyDiscount = false
    private var priceFilter = 0
    private var sortMode = 0

    private val priceOptions = arrayOf("Tất cả mức giá", "Dưới 1 triệu", "1 - 2 triệu", "Trên 2 triệu")
    private val sortOptions = arrayOf("Mặc định", "Giá tăng dần", "Giá giảm dần", "Khuyến mãi nhiều")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_products, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.productsRoot)) { root, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            root.setPadding(root.paddingLeft, bars.top, root.paddingRight, root.paddingBottom)
            insets
        }

        view.findViewById<EditText>(R.id.edtProductSearch)
            .addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    applyFilters()
                }
            })

        view.findViewById<ImageButton>(R.id.btnProductFilter).setOnClickListener {
            showFilterDialog()
        }

        fetchProducts()
    }

    private fun fetchProducts() {
        val view = view ?: return
        view.findViewById<View>(R.id.layoutProductsLoading).visibility = View.VISIBLE
        view.findViewById<View>(R.id.layoutProductsEmpty).visibility = View.GONE
        view.findViewById<ScrollView>(R.id.scrollProducts).visibility = View.GONE

        FirebaseFirestore.getInstance().collection("products")
            .get()
            .addOnSuccessListener { result ->
                allProducts.clear()
                for (doc in result) {
                    allProducts.add(
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
                    )
                }

                renderFilterChips()
                applyFilters()
            }
            .addOnFailureListener { e ->
                view.findViewById<View>(R.id.layoutProductsLoading).visibility = View.GONE
                view.findViewById<View>(R.id.layoutProductsEmpty).visibility = View.VISIBLE
                view.findViewById<TextView>(R.id.tvProductsEmptyTitle).text = "Không tải được sản phẩm"
                view.findViewById<TextView>(R.id.tvProductsEmptyDesc).text = e.message ?: "Vui lòng thử lại sau"
                Toast.makeText(requireContext(), "Lỗi tải sản phẩm", Toast.LENGTH_SHORT).show()
            }
    }

    private fun applyFilters() {
        val view = view ?: return
        val query = view.findViewById<EditText>(R.id.edtProductSearch).text.toString().trim().lowercase()

        val filtered = allProducts
            .filter { product ->
                query.isEmpty() ||
                    product.name.lowercase().contains(query) ||
                    product.brand.lowercase().contains(query) ||
                    product.description.lowercase().contains(query) ||
                    product.specifications.lowercase().contains(query)
            }
            .filter { product -> selectedBrand == null || product.brand == selectedBrand }
            .filter { product -> !onlyDiscount || product.discounted > 0 }
            .filter { product ->
                val price = activePrice(product)
                when (priceFilter) {
                    1 -> price < 1_000_000
                    2 -> price in 1_000_000.0..2_000_000.0
                    3 -> price > 2_000_000
                    else -> true
                }
            }
            .let { products ->
                when (sortMode) {
                    1 -> products.sortedBy { activePrice(it) }
                    2 -> products.sortedByDescending { activePrice(it) }
                    3 -> products.sortedByDescending { it.discounted }
                    else -> products
                }
            }

        view.findViewById<View>(R.id.layoutProductsLoading).visibility = View.GONE
        view.findViewById<TextView>(R.id.tvProductCount).text =
            "${filtered.size}/${allProducts.size} sản phẩm phù hợp"
        updateActiveFilterText()

        if (filtered.isEmpty()) {
            view.findViewById<ScrollView>(R.id.scrollProducts).visibility = View.GONE
            view.findViewById<View>(R.id.layoutProductsEmpty).visibility = View.VISIBLE
        } else {
            view.findViewById<View>(R.id.layoutProductsEmpty).visibility = View.GONE
            view.findViewById<ScrollView>(R.id.scrollProducts).visibility = View.VISIBLE
            renderProducts(filtered)
        }
    }

    private fun renderFilterChips() {
        val view = view ?: return
        val container = view.findViewById<LinearLayout>(R.id.llProductFilters)
        container.removeAllViews()

        addChip(container, "Tất cả", selectedBrand == null && !onlyDiscount && priceFilter == 0 && sortMode == 0) {
            selectedBrand = null
            onlyDiscount = false
            priceFilter = 0
            sortMode = 0
            renderFilterChips()
            applyFilters()
        }

        addChip(container, "Khuyến mãi", onlyDiscount) {
            onlyDiscount = !onlyDiscount
            renderFilterChips()
            applyFilters()
        }

        allProducts.map { it.brand }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
            .forEach { brand ->
                addChip(container, brand, selectedBrand == brand) {
                    selectedBrand = if (selectedBrand == brand) null else brand
                    renderFilterChips()
                    applyFilters()
                }
            }
    }

    private fun addChip(container: LinearLayout, label: String, selected: Boolean, onClick: () -> Unit) {
        val chip = TextView(requireContext()).apply {
            text = label
            gravity = Gravity.CENTER
            minHeight = 34.dpToPx()
            setPadding(14.dpToPx(), 0, 14.dpToPx(), 0)
            textSize = 13f
            typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            setTextColor(if (selected) android.graphics.Color.WHITE else resources.getColor(R.color.colorText, requireContext().theme))
            setBackgroundResource(if (selected) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected)
            setOnClickListener { onClick() }
        }
        chip.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            34.dpToPx()
        ).apply {
            marginEnd = 8.dpToPx()
        }
        container.addView(chip)
    }

    private fun showFilterDialog() {
        val context = requireContext()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24.dpToPx(), 8.dpToPx(), 24.dpToPx(), 0)
        }

        val priceGroup = createRadioSection(container, "Khoảng giá", priceOptions, priceFilter)
        val sortGroup = createRadioSection(container, "Sắp xếp", sortOptions, sortMode)

        AlertDialog.Builder(context)
            .setTitle("Bộ lọc sản phẩm")
            .setView(container)
            .setPositiveButton("Áp dụng") { _, _ ->
                priceFilter = priceGroup.checkedIndex().coerceAtLeast(0)
                sortMode = sortGroup.checkedIndex().coerceAtLeast(0)
                renderFilterChips()
                applyFilters()
            }
            .setNeutralButton("Đặt lại") { _, _ ->
                selectedBrand = null
                onlyDiscount = false
                priceFilter = 0
                sortMode = 0
                renderFilterChips()
                applyFilters()
            }
            .setNegativeButton("Huỷ", null)
            .show()
    }

    private fun createRadioSection(
        container: LinearLayout,
        title: String,
        options: Array<String>,
        checkedIndex: Int
    ): RadioGroup {
        val titleView = TextView(requireContext()).apply {
            text = title
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(resources.getColor(R.color.colorText, requireContext().theme))
            setPadding(0, 12.dpToPx(), 0, 4.dpToPx())
        }
        container.addView(titleView)

        val radioGroup = RadioGroup(requireContext()).apply {
            orientation = RadioGroup.VERTICAL
        }

        options.forEachIndexed { index, label ->
            val radio = RadioButton(requireContext()).apply {
                id = View.generateViewId()
                text = label
                textSize = 14f
                tag = index
                setTextColor(resources.getColor(R.color.colorText, requireContext().theme))
            }
            radioGroup.addView(radio)
            if (index == checkedIndex) {
                radioGroup.check(radio.id)
            }
        }

        container.addView(radioGroup)
        return radioGroup
    }

    private fun RadioGroup.checkedIndex(): Int {
        val checked = findViewById<RadioButton>(checkedRadioButtonId)
        return checked?.tag as? Int ?: 0
    }

    private fun updateActiveFilterText() {
        val view = view ?: return
        val activeFilters = mutableListOf<String>()
        selectedBrand?.let { activeFilters.add(it) }
        if (onlyDiscount) activeFilters.add("Khuyến mãi")
        if (priceFilter > 0) activeFilters.add(priceOptions[priceFilter])
        if (sortMode > 0) activeFilters.add(sortOptions[sortMode])

        val tvActiveFilters = view.findViewById<TextView>(R.id.tvActiveFilters)
        if (activeFilters.isEmpty()) {
            tvActiveFilters.visibility = View.GONE
        } else {
            tvActiveFilters.visibility = View.VISIBLE
            tvActiveFilters.text = "Đang lọc: ${activeFilters.joinToString(" • ")}"
        }
    }

    private fun renderProducts(products: List<Product>) {
        val view = view ?: return
        val glProducts = view.findViewById<GridLayout>(R.id.glProducts)
        glProducts.removeAllViews()

        val horizontalPadding = 32.dpToPx()
        val gap = 10.dpToPx()
        val itemWidth = (resources.displayMetrics.widthPixels - horizontalPadding - gap) / 2

        for ((index, product) in products.withIndex()) {
            val itemView = layoutInflater.inflate(R.layout.item_product, glProducts, false)
            bindProductView(itemView, product)

            val row = index / 2
            val col = index % 2
            itemView.layoutParams = GridLayout.LayoutParams(
                GridLayout.spec(row),
                GridLayout.spec(col)
            ).apply {
                width = itemWidth
                setMargins(if (col == 0) 0 else gap, 0, 0, 10.dpToPx())
            }

            itemView.setOnClickListener {
                startActivity(Intent(requireContext(), ProductDetailActivity::class.java).apply {
                    putExtra("EXTRA_PRODUCT", product)
                })
            }

            glProducts.addView(itemView)
        }
    }

    private fun bindProductView(itemView: View, product: Product) {
        val tvName = itemView.findViewById<TextView>(R.id.tvProductName)
        val tvBrand = itemView.findViewById<TextView>(R.id.tvProductBrand)
        val tvPrice = itemView.findViewById<TextView>(R.id.tvProductPrice)
        val tvPriceOriginal = itemView.findViewById<TextView>(R.id.tvProductPriceOriginal)
        val tvDiscountBadge = itemView.findViewById<TextView>(R.id.tvProductDiscountBadge)
        val imgProduct = itemView.findViewById<ImageView>(R.id.imgProduct)

        tvName.text = product.name
        tvBrand.text = product.brand

        if (product.discounted > 0) {
            tvPrice.text = "đ${formatter.format(activePrice(product))}"
            tvPriceOriginal.text = "đ${formatter.format(product.price)}"
            tvPriceOriginal.paintFlags = tvPriceOriginal.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            tvPriceOriginal.visibility = View.VISIBLE
            tvDiscountBadge.text = "-${product.discounted}%"
            tvDiscountBadge.visibility = View.VISIBLE
        } else {
            tvPrice.text = "đ${formatter.format(product.price)}"
            tvPriceOriginal.visibility = View.INVISIBLE
            tvDiscountBadge.visibility = View.INVISIBLE
        }

        val resId = if (product.imageUrl.isNotEmpty()) {
            resources.getIdentifier(product.imageUrl, "drawable", requireContext().packageName)
        } else {
            0
        }
        imgProduct.setImageResource(if (resId != 0) resId else R.drawable.ic_image_placeholder)
        imgProduct.scaleType = ImageView.ScaleType.CENTER_INSIDE
    }

    private fun activePrice(product: Product): Double {
        return if (product.discounted > 0) {
            product.price * (1 - product.discounted / 100.0)
        } else {
            product.price
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}
