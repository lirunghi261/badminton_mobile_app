package com.example.buoi1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
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

class AdminCategoryActivity : AppCompatActivity() {

    private val categoryList = mutableListOf<Pair<String, Category>>() // docId to Category
    private lateinit var adapter: CategoryAdapter
    private lateinit var rvCategories: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvCount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_category)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.topBarAdminCategories)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, bars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        rvCategories = findViewById(R.id.rvCategories)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        progressBar = findViewById(R.id.progressBar)
        tvCount = findViewById(R.id.tvCategoryCount)

        adapter = CategoryAdapter(
            categoryList,
            onEditClick = { docId, category -> showEditDialog(docId, category) },
            onDeleteClick = { docId, category -> confirmDelete(docId, category) }
        )
        rvCategories.layoutManager = LinearLayoutManager(this)
        rvCategories.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fabAddCategory).setOnClickListener {
            showAddDialog()
        }

        fetchCategories()
    }

    override fun onResume() {
        super.onResume()
        fetchCategories()
    }

    private fun fetchCategories() {
        progressBar.visibility = View.VISIBLE
        rvCategories.visibility = View.GONE
        layoutEmpty.visibility = View.GONE

        val db = FirebaseFirestore.getInstance()
        db.collection("categories")
            .get()
            .addOnSuccessListener { result ->
                progressBar.visibility = View.GONE
                categoryList.clear()
                for (doc in result) {
                    val category = Category(
                        name = doc.getString("name") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: "",
                        description = doc.getString("description") ?: ""
                    )
                    categoryList.add(Pair(doc.id, category))
                }
                adapter.notifyDataSetChanged()
                tvCount.text = "${categoryList.size} danh mục"

                if (categoryList.isEmpty()) {
                    rvCategories.visibility = View.GONE
                    layoutEmpty.visibility = View.VISIBLE
                } else {
                    rvCategories.visibility = View.VISIBLE
                    layoutEmpty.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                layoutEmpty.visibility = View.VISIBLE
                Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showAddDialog() {
        showCategoryDialog("Thêm danh mục", Category()) { category ->
            val db = FirebaseFirestore.getInstance()
            val data = hashMapOf(
                "name" to category.name,
                "imageUrl" to category.imageUrl,
                "description" to category.description
            )
            db.collection("categories").add(data)
                .addOnSuccessListener {
                    Toast.makeText(this, "Thêm danh mục thành công!", Toast.LENGTH_SHORT).show()
                    fetchCategories()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showEditDialog(docId: String, category: Category) {
        showCategoryDialog("Sửa danh mục", category) { updated ->
            val db = FirebaseFirestore.getInstance()
            val data = hashMapOf(
                "name" to updated.name,
                "imageUrl" to updated.imageUrl,
                "description" to updated.description
            )
            db.collection("categories").document(docId).set(data)
                .addOnSuccessListener {
                    Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
                    fetchCategories()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showCategoryDialog(title: String, category: Category, onSave: (Category) -> Unit) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }

        val edtName = EditText(this).apply {
            hint = "Tên danh mục (VD: Yonex)"
            setText(category.name)
        }
        layout.addView(edtName)

        val edtImage = EditText(this).apply {
            hint = "Tên ảnh drawable (VD: yonex)"
            setText(category.imageUrl)
        }
        layout.addView(edtImage)

        val edtDesc = EditText(this).apply {
            hint = "Mô tả (tuỳ chọn)"
            setText(category.description)
        }
        layout.addView(edtDesc)

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(layout)
            .setPositiveButton("Lưu") { dlg, _ ->
                val name = edtName.text.toString().trim()
                val imageUrl = edtImage.text.toString().trim()
                val desc = edtDesc.text.toString().trim()

                if (name.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập tên danh mục", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                onSave(Category(name, imageUrl, desc))
                dlg.dismiss()
            }
            .setNegativeButton("Huỷ", null)
            .show()
    }

    private fun confirmDelete(docId: String, category: Category) {
        AlertDialog.Builder(this)
            .setTitle("Xoá danh mục")
            .setMessage("Bạn có chắc muốn xoá \"${category.name}\"?")
            .setPositiveButton("Xoá") { _, _ ->
                val db = FirebaseFirestore.getInstance()
                db.collection("categories").document(docId).delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Đã xoá danh mục", Toast.LENGTH_SHORT).show()
                        fetchCategories()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Huỷ", null)
            .show()
    }

    // ======== Adapter ========
    class CategoryAdapter(
        private val categories: List<Pair<String, Category>>,
        private val onEditClick: (String, Category) -> Unit,
        private val onDeleteClick: (String, Category) -> Unit
    ) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imgCategory: ImageView = view.findViewById(R.id.imgCategory)
            val tvName: TextView = view.findViewById(R.id.tvCategoryName)
            val tvImage: TextView = view.findViewById(R.id.tvCategoryImage)
            val tvDesc: TextView = view.findViewById(R.id.tvCategoryDesc)
            val btnEdit: ImageView = view.findViewById(R.id.btnEditCategory)
            val btnDelete: ImageView = view.findViewById(R.id.btnDeleteCategory)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_admin_category, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val (docId, category) = categories[position]
            holder.tvName.text = category.name
            holder.tvImage.text = "drawable: ${category.imageUrl}"
            holder.tvDesc.text = if (category.description.isNotEmpty()) category.description else "Không có mô tả"

            if (category.imageUrl.isNotEmpty()) {
                val context = holder.imgCategory.context
                val resId = context.resources.getIdentifier(category.imageUrl, "drawable", context.packageName)
                if (resId != 0) {
                    holder.imgCategory.setImageResource(resId)
                } else {
                    holder.imgCategory.setImageResource(R.drawable.ic_image_placeholder)
                }
            } else {
                holder.imgCategory.setImageResource(R.drawable.ic_image_placeholder)
            }

            holder.btnEdit.setOnClickListener { onEditClick(docId, category) }
            holder.btnDelete.setOnClickListener { onDeleteClick(docId, category) }
        }

        override fun getItemCount() = categories.size
    }
}
