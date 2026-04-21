package com.example.buoi1

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Locale

class AdminProductListActivity : AppCompatActivity() {

    private val productList = mutableListOf<Pair<String, Product>>() // docId to Product
    private val filteredList = mutableListOf<Pair<String, Product>>()
    private lateinit var adapter: AdminProductAdapter
    private lateinit var rvProducts: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvProductCount: TextView
    private lateinit var spinnerBrand: Spinner
    private lateinit var spinnerPrice: Spinner

    private var selectedBrand = "Tất cả"
    private var selectedPriceRange = "Tất cả"

    private val priceRanges = listOf(
        "Tất cả",
        "Dưới 1 triệu",
        "1 - 3 triệu",
        "3 - 5 triệu",
        "Trên 5 triệu"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_product_list)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.topBarAdminProducts)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, bars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        rvProducts = findViewById(R.id.rvAdminProducts)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        progressBar = findViewById(R.id.progressBar)
        tvProductCount = findViewById(R.id.tvProductCount)
        spinnerBrand = findViewById(R.id.spinnerBrand)
        spinnerPrice = findViewById(R.id.spinnerPrice)

        // Setup RecyclerView
        adapter = AdminProductAdapter(
            filteredList,
            onEditClick = { docId, product -> openEditProduct(docId, product) },
            onDeleteClick = { docId, product -> confirmDeleteProduct(docId, product) }
        )
        rvProducts.layoutManager = LinearLayoutManager(this)
        rvProducts.adapter = adapter

        // Search
        val edtSearch = findViewById<EditText>(R.id.edtSearch)
        edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                applyFilters()
            }
        })

        // Price filter spinner
        val priceAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, priceRanges)
        priceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPrice.adapter = priceAdapter
        spinnerPrice.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                selectedPriceRange = priceRanges[pos]
                applyFilters()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // FAB add product
        findViewById<FloatingActionButton>(R.id.fabAddProduct).setOnClickListener {
            startActivity(Intent(this, AdminProductEditActivity::class.java))
        }

        // Load categories for brand filter
        loadCategoriesForFilter()

        fetchProducts()
    }

    override fun onResume() {
        super.onResume()
        loadCategoriesForFilter()
        fetchProducts()
    }

    private fun loadCategoriesForFilter() {
        val db = FirebaseFirestore.getInstance()
        db.collection("categories")
            .get()
            .addOnSuccessListener { result ->
                val brandList = mutableListOf("Tất cả")
                for (doc in result) {
                    val name = doc.getString("name") ?: ""
                    if (name.isNotEmpty()) brandList.add(name)
                }
                brandList.sort()
                // Keep "Tất cả" at top
                brandList.remove("Tất cả")
                brandList.add(0, "Tất cả")

                val brandAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, brandList)
                brandAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerBrand.adapter = brandAdapter

                // Restore selected brand
                val brandIndex = brandList.indexOf(selectedBrand)
                if (brandIndex >= 0) spinnerBrand.setSelection(brandIndex)

                spinnerBrand.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                        selectedBrand = brandList[pos]
                        applyFilters()
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }
    }

    private fun fetchProducts() {
        progressBar.visibility = View.VISIBLE
        rvProducts.visibility = View.GONE
        layoutEmpty.visibility = View.GONE

        val db = FirebaseFirestore.getInstance()
        db.collection("products")
            .get()
            .addOnSuccessListener { result ->
                progressBar.visibility = View.GONE
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
                    productList.add(Pair(document.id, product))
                }

                applyFilters()
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                layoutEmpty.visibility = View.VISIBLE
                Toast.makeText(this, "Lỗi tải sản phẩm: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun applyFilters() {
        val edtSearch = findViewById<EditText>(R.id.edtSearch)
        val query = edtSearch.text.toString().lowercase()

        filteredList.clear()
        filteredList.addAll(productList.filter { (_, product) ->
            val matchesSearch = query.isEmpty() ||
                    product.name.lowercase().contains(query) ||
                    product.brand.lowercase().contains(query)

            val matchesBrand = selectedBrand == "Tất cả" ||
                    product.brand.equals(selectedBrand, ignoreCase = true)

            val effectivePrice = if (product.discounted > 0)
                product.price * (1 - product.discounted / 100.0)
            else product.price

            val matchesPrice = when (selectedPriceRange) {
                "Dưới 1 triệu" -> effectivePrice < 1_000_000
                "1 - 3 triệu" -> effectivePrice in 1_000_000.0..3_000_000.0
                "3 - 5 triệu" -> effectivePrice in 3_000_000.0..5_000_000.0
                "Trên 5 triệu" -> effectivePrice > 5_000_000
                else -> true
            }

            matchesSearch && matchesBrand && matchesPrice
        })

        adapter.notifyDataSetChanged()
        tvProductCount.text = "${filteredList.size} sản phẩm"

        if (filteredList.isEmpty()) {
            rvProducts.visibility = View.GONE
            layoutEmpty.visibility = View.VISIBLE
        } else {
            rvProducts.visibility = View.VISIBLE
            layoutEmpty.visibility = View.GONE
        }
    }

    private fun openEditProduct(docId: String, product: Product) {
        val intent = Intent(this, AdminProductEditActivity::class.java)
        intent.putExtra("EDIT_DOC_ID", docId)
        intent.putExtra("EDIT_PRODUCT", product)
        startActivity(intent)
    }

    private fun confirmDeleteProduct(docId: String, product: Product) {
        AlertDialog.Builder(this)
            .setTitle("Xoá sản phẩm")
            .setMessage("Bạn có chắc muốn xoá \"${product.name}\"?\nHành động này không thể hoàn tác.")
            .setPositiveButton("Xoá") { _, _ -> deleteProduct(docId) }
            .setNegativeButton("Huỷ", null)
            .show()
    }

    private fun deleteProduct(docId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("products").document(docId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Đã xoá sản phẩm", Toast.LENGTH_SHORT).show()
                fetchProducts()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi xoá: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // ======== RecyclerView Adapter ========
    class AdminProductAdapter(
        private val products: List<Pair<String, Product>>,
        private val onEditClick: (String, Product) -> Unit,
        private val onDeleteClick: (String, Product) -> Unit
    ) : RecyclerView.Adapter<AdminProductAdapter.ViewHolder>() {

        private val formatter = NumberFormat.getInstance(Locale("vi", "VN"))

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imgProduct: ImageView = view.findViewById(R.id.imgAdminProduct)
            val tvName: TextView = view.findViewById(R.id.tvAdminProductName)
            val tvBrand: TextView = view.findViewById(R.id.tvAdminProductBrand)
            val tvPrice: TextView = view.findViewById(R.id.tvAdminProductPrice)
            val tvDiscount: TextView = view.findViewById(R.id.tvAdminProductDiscount)
            val btnEdit: ImageView = view.findViewById(R.id.btnEditProduct)
            val btnDelete: ImageView = view.findViewById(R.id.btnDeleteProduct)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_admin_product, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val (docId, product) = products[position]

            holder.tvName.text = product.name
            holder.tvBrand.text = product.brand

            if (product.discounted > 0) {
                val discountedPrice = product.price * (1 - product.discounted / 100.0)
                holder.tvPrice.text = "đ${formatter.format(discountedPrice)}"
                holder.tvDiscount.text = "-${product.discounted}%"
                holder.tvDiscount.visibility = View.VISIBLE
            } else {
                holder.tvPrice.text = "đ${formatter.format(product.price)}"
                holder.tvDiscount.visibility = View.GONE
            }

            if (product.imageUrl.isNotEmpty()) {
                val context = holder.imgProduct.context
                val resId = context.resources.getIdentifier(product.imageUrl, "drawable", context.packageName)
                if (resId != 0) {
                    holder.imgProduct.setImageResource(resId)
                    holder.imgProduct.scaleType = ImageView.ScaleType.CENTER_INSIDE
                } else {
                    holder.imgProduct.setImageResource(R.drawable.ic_image_placeholder)
                }
            } else {
                holder.imgProduct.setImageResource(R.drawable.ic_image_placeholder)
            }

            holder.btnEdit.setOnClickListener { onEditClick(docId, product) }
            holder.btnDelete.setOnClickListener { onDeleteClick(docId, product) }
        }

        override fun getItemCount() = products.size
    }
}
