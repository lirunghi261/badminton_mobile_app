package com.example.buoi1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore

class AdminProductEditActivity : AppCompatActivity() {

    private lateinit var layoutSpecRows: LinearLayout
    private lateinit var tvSpecHint: TextView
    private var editDocId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_product_edit)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.topBarEditProduct)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, bars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        layoutSpecRows = findViewById(R.id.layoutSpecRows)
        tvSpecHint = findViewById(R.id.tvSpecHint)

        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        val edtName = findViewById<EditText>(R.id.edtProductName)
        val edtBrand = findViewById<EditText>(R.id.edtProductBrand)
        val edtPrice = findViewById<EditText>(R.id.edtProductPrice)
        val edtDiscount = findViewById<EditText>(R.id.edtProductDiscount)
        val edtImage = findViewById<EditText>(R.id.edtProductImage)
        val edtDescription = findViewById<EditText>(R.id.edtProductDescription)
        val btnAddSpec = findViewById<Button>(R.id.btnAddSpec)
        val btnSave = findViewById<Button>(R.id.btnSaveProduct)

        // Check if editing existing product
        editDocId = intent.getStringExtra("EDIT_DOC_ID")
        val editProduct = intent.getSerializableExtra("EDIT_PRODUCT") as? Product

        if (editProduct != null && editDocId != null) {
            tvTitle.text = "Sửa sản phẩm"
            btnSave.text = "Cập nhật sản phẩm"
            edtName.setText(editProduct.name)
            edtBrand.setText(editProduct.brand)
            edtPrice.setText(editProduct.price.toLong().toString())
            if (editProduct.discounted > 0) edtDiscount.setText(editProduct.discounted.toString())
            edtImage.setText(editProduct.imageUrl)
            edtDescription.setText(editProduct.description)

            // Parse specifications string and add rows
            if (editProduct.specifications.isNotEmpty()) {
                val specs = editProduct.specifications.split(";")
                for (spec in specs) {
                    val parts = spec.split(":", limit = 2)
                    if (parts.size == 2) {
                        addSpecRow(parts[0].trim(), parts[1].trim())
                    }
                }
            }
        }

        // Add spec row button
        btnAddSpec.setOnClickListener {
            addSpecRow("", "")
        }

        // Save product
        btnSave.setOnClickListener {
            val name = edtName.text.toString().trim()
            val brand = edtBrand.text.toString().trim()
            val priceStr = edtPrice.text.toString().trim()
            val discountStr = edtDiscount.text.toString().trim()
            val imageUrl = edtImage.text.toString().trim()
            val description = edtDescription.text.toString().trim()

            // Validate
            if (name.isEmpty()) {
                edtName.error = "Vui lòng nhập tên sản phẩm"
                edtName.requestFocus()
                return@setOnClickListener
            }
            if (brand.isEmpty()) {
                edtBrand.error = "Vui lòng nhập thương hiệu"
                edtBrand.requestFocus()
                return@setOnClickListener
            }
            if (priceStr.isEmpty()) {
                edtPrice.error = "Vui lòng nhập giá"
                edtPrice.requestFocus()
                return@setOnClickListener
            }

            val price = priceStr.toDoubleOrNull() ?: 0.0
            val discount = discountStr.toIntOrNull() ?: 0

            // Build specifications string
            val specifications = buildSpecificationsString()

            // Create product map
            val productMap = hashMapOf(
                "name" to name,
                "brand" to brand,
                "price" to price,
                "discounted" to discount,
                "imageUrl" to imageUrl,
                "description" to description,
                "specifications" to specifications,
                "isFavorite" to false
            )

            btnSave.isEnabled = false
            btnSave.text = "Đang lưu..."

            val db = FirebaseFirestore.getInstance()

            if (editDocId != null) {
                // Update existing product
                db.collection("products").document(editDocId!!)
                    .set(productMap)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Cập nhật sản phẩm thành công!", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        btnSave.isEnabled = true
                        btnSave.text = "Cập nhật sản phẩm"
                        Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                // Add new product
                db.collection("products")
                    .add(productMap)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Thêm sản phẩm thành công!", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        btnSave.isEnabled = true
                        btnSave.text = "Lưu sản phẩm"
                        Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun addSpecRow(name: String, value: String) {
        tvSpecHint.visibility = View.GONE
        val row = LayoutInflater.from(this).inflate(R.layout.item_spec_row, layoutSpecRows, false)
        val edtSpecName = row.findViewById<EditText>(R.id.edtSpecName)
        val edtSpecValue = row.findViewById<EditText>(R.id.edtSpecValue)
        val btnRemove = row.findViewById<ImageView>(R.id.btnRemoveSpec)

        edtSpecName.setText(name)
        edtSpecValue.setText(value)

        btnRemove.setOnClickListener {
            layoutSpecRows.removeView(row)
            if (layoutSpecRows.childCount == 0) {
                tvSpecHint.visibility = View.VISIBLE
            }
        }

        layoutSpecRows.addView(row)
    }

    private fun buildSpecificationsString(): String {
        val specs = mutableListOf<String>()
        for (i in 0 until layoutSpecRows.childCount) {
            val row = layoutSpecRows.getChildAt(i)
            val specName = row.findViewById<EditText>(R.id.edtSpecName).text.toString().trim()
            val specValue = row.findViewById<EditText>(R.id.edtSpecValue).text.toString().trim()
            if (specName.isNotEmpty() && specValue.isNotEmpty()) {
                specs.add("$specName:$specValue")
            }
        }
        return specs.joinToString(";")
    }
}
