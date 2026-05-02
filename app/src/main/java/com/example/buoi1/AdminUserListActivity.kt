package com.example.buoi1

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

data class UserWithId(
    val docId: String = "",
    val username: String = "",
    val password: String = "",
    val fullName: String = "",
    val role: String = "user"
)

class AdminUserListActivity : AppCompatActivity() {

    private val userList = mutableListOf<UserWithId>()
    private val filteredList = mutableListOf<UserWithId>()
    private lateinit var adapter: AdminUserAdapter
    private lateinit var rvUsers: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvUserCount: TextView
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_user_list)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.topBarAdminUsers)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, bars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        findViewById<ImageView>(R.id.btnBackUsers).setOnClickListener { finish() }

        rvUsers = findViewById(R.id.rvAdminUsers)
        layoutEmpty = findViewById(R.id.layoutUserEmpty)
        progressBar = findViewById(R.id.progressBarUsers)
        tvUserCount = findViewById(R.id.tvUserCount)

        adapter = AdminUserAdapter(
            filteredList,
            onEditClick = { user -> showEditDialog(user) },
            onDeleteClick = { user -> confirmDeleteUser(user) }
        )
        rvUsers.layoutManager = LinearLayoutManager(this)
        rvUsers.adapter = adapter

        val edtSearch = findViewById<EditText>(R.id.edtSearchUser)
        edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { applyFilter() }
        })

        findViewById<FloatingActionButton>(R.id.fabAddUser).setOnClickListener {
            showAddDialog()
        }

        fetchUsers()
    }

    override fun onResume() {
        super.onResume()
        fetchUsers()
    }

    private fun fetchUsers() {
        progressBar.visibility = View.VISIBLE
        rvUsers.visibility = View.GONE
        layoutEmpty.visibility = View.GONE

        db.collection("users").get()
            .addOnSuccessListener { result ->
                progressBar.visibility = View.GONE
                userList.clear()
                for (doc in result) {
                    userList.add(
                        UserWithId(
                            docId = doc.id,
                            username = doc.getString("username") ?: "",
                            password = doc.getString("password") ?: "",
                            fullName = doc.getString("fullName") ?: "",
                            role = doc.getString("role") ?: "user"
                        )
                    )
                }
                applyFilter()
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                layoutEmpty.visibility = View.VISIBLE
                Toast.makeText(this, "Lỗi tải dữ liệu: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun applyFilter() {
        val query = findViewById<EditText>(R.id.edtSearchUser).text.toString().lowercase()
        filteredList.clear()
        filteredList.addAll(userList.filter { user ->
            query.isEmpty() ||
                user.fullName.lowercase().contains(query) ||
                user.username.lowercase().contains(query)
        })
        adapter.notifyDataSetChanged()
        tvUserCount.text = "${filteredList.size} người dùng"
        if (filteredList.isEmpty()) {
            rvUsers.visibility = View.GONE
            layoutEmpty.visibility = View.VISIBLE
        } else {
            rvUsers.visibility = View.VISIBLE
            layoutEmpty.visibility = View.GONE
        }
    }

    private fun showAddDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_user_edit, null)
        val edtFullName = dialogView.findViewById<EditText>(R.id.edtUserFullName)
        val edtUsername = dialogView.findViewById<EditText>(R.id.edtUserUsername)
        val edtPassword = dialogView.findViewById<EditText>(R.id.edtUserPassword)
        val spinnerRole = dialogView.findViewById<android.widget.Spinner>(R.id.spinnerUserRole)

        val roles = arrayOf("user", "admin")
        spinnerRole.adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, roles).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        AlertDialog.Builder(this)
            .setTitle("Thêm người dùng")
            .setView(dialogView)
            .setPositiveButton("Thêm") { _, _ ->
                val fullName = edtFullName.text.toString().trim()
                val username = edtUsername.text.toString().trim()
                val password = edtPassword.text.toString().trim()
                val role = spinnerRole.selectedItem.toString()

                if (fullName.isEmpty() || username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                checkUsernameExists(username) { exists ->
                    if (exists) {
                        Toast.makeText(this, "Tên đăng nhập đã tồn tại", Toast.LENGTH_SHORT).show()
                    } else {
                        val data = hashMapOf(
                            "fullName" to fullName,
                            "username" to username,
                            "password" to password,
                            "role" to role
                        )
                        db.collection("users").add(data)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Đã thêm người dùng", Toast.LENGTH_SHORT).show()
                                fetchUsers()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
            }
            .setNegativeButton("Huỷ", null)
            .show()
    }

    private fun showEditDialog(user: UserWithId) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_user_edit, null)
        val edtFullName = dialogView.findViewById<EditText>(R.id.edtUserFullName)
        val edtUsername = dialogView.findViewById<EditText>(R.id.edtUserUsername)
        val edtPassword = dialogView.findViewById<EditText>(R.id.edtUserPassword)
        val spinnerRole = dialogView.findViewById<android.widget.Spinner>(R.id.spinnerUserRole)

        edtFullName.setText(user.fullName)
        edtUsername.setText(user.username)
        edtPassword.setText(user.password)

        val roles = arrayOf("user", "admin")
        spinnerRole.adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, roles).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerRole.setSelection(if (user.role == "admin") 1 else 0)

        AlertDialog.Builder(this)
            .setTitle("Sửa người dùng")
            .setView(dialogView)
            .setPositiveButton("Lưu") { _, _ ->
                val fullName = edtFullName.text.toString().trim()
                val username = edtUsername.text.toString().trim()
                val password = edtPassword.text.toString().trim()
                val role = spinnerRole.selectedItem.toString()

                if (fullName.isEmpty() || username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // If username changed, check uniqueness
                if (username != user.username) {
                    checkUsernameExists(username) { exists ->
                        if (exists) {
                            Toast.makeText(this, "Tên đăng nhập đã tồn tại", Toast.LENGTH_SHORT).show()
                        } else {
                            updateUser(user.docId, fullName, username, password, role)
                        }
                    }
                } else {
                    updateUser(user.docId, fullName, username, password, role)
                }
            }
            .setNegativeButton("Huỷ", null)
            .show()
    }

    private fun updateUser(docId: String, fullName: String, username: String, password: String, role: String) {
        val data = hashMapOf(
            "fullName" to fullName,
            "username" to username,
            "password" to password,
            "role" to role
        )
        db.collection("users").document(docId).set(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Đã cập nhật người dùng", Toast.LENGTH_SHORT).show()
                fetchUsers()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkUsernameExists(username: String, callback: (Boolean) -> Unit) {
        db.collection("users").whereEqualTo("username", username).get()
            .addOnSuccessListener { callback(!it.isEmpty) }
            .addOnFailureListener { callback(false) }
    }

    private fun confirmDeleteUser(user: UserWithId) {
        if (user.username == UserManager.currentUser?.username) {
            Toast.makeText(this, "Không thể xoá tài khoản đang đăng nhập", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Xoá người dùng")
            .setMessage("Bạn có chắc muốn xoá \"${user.fullName}\"?\nHành động này không thể hoàn tác.")
            .setPositiveButton("Xoá") { _, _ ->
                db.collection("users").document(user.docId).delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Đã xoá người dùng", Toast.LENGTH_SHORT).show()
                        fetchUsers()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Huỷ", null)
            .show()
    }

    class AdminUserAdapter(
        private val users: List<UserWithId>,
        private val onEditClick: (UserWithId) -> Unit,
        private val onDeleteClick: (UserWithId) -> Unit
    ) : RecyclerView.Adapter<AdminUserAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvFullName: TextView = view.findViewById(R.id.tvUserFullName)
            val tvUsername: TextView = view.findViewById(R.id.tvUserUsername)
            val tvRole: TextView = view.findViewById(R.id.tvUserRole)
            val btnEdit: ImageView = view.findViewById(R.id.btnEditUser)
            val btnDelete: ImageView = view.findViewById(R.id.btnDeleteUser)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_admin_user, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val user = users[position]
            holder.tvFullName.text = user.fullName
            holder.tvUsername.text = "@${user.username}"
            holder.tvRole.text = if (user.role == "admin") "Admin" else "Người dùng"
            holder.tvRole.setBackgroundResource(
                if (user.role == "admin") R.drawable.bg_role_admin else R.drawable.bg_role_user
            )
            holder.btnEdit.setOnClickListener { onEditClick(user) }
            holder.btnDelete.setOnClickListener { onDeleteClick(user) }
        }

        override fun getItemCount() = users.size
    }
}
