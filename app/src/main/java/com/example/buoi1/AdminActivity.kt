package com.example.buoi1

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore

class AdminActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.topBarAdmin)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, bars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        // Show admin name
        val tvAdminWelcome = findViewById<TextView>(R.id.tvAdminWelcome)
        val currentUser = UserManager.currentUser
        if (currentUser != null) {
            tvAdminWelcome.text = "Xin chào, ${currentUser.fullName}!"
        }

        // Load stats from Firebase
        loadStats()

        // Manage Products button
        findViewById<LinearLayout>(R.id.btnManageProducts).setOnClickListener {
            startActivity(Intent(this, AdminProductListActivity::class.java))
        }

        // Manage Categories button
        findViewById<LinearLayout>(R.id.btnManageCategories).setOnClickListener {
            startActivity(Intent(this, AdminCategoryActivity::class.java))
        }

        // Logout button
        val btnAdminLogout = findViewById<Button>(R.id.btnAdminLogout)
        btnAdminLogout.setOnClickListener {
            UserManager.logout()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun loadStats() {
        val db = FirebaseFirestore.getInstance()
        val tvTotalProducts = findViewById<TextView>(R.id.tvAdminTotalProducts)
        val tvTotalOrders = findViewById<TextView>(R.id.tvAdminTotalOrders)
        val tvTotalUsers = findViewById<TextView>(R.id.tvAdminTotalUsers)
        val tvPendingOrders = findViewById<TextView>(R.id.tvAdminPendingOrders)

        // Count products
        db.collection("products").get()
            .addOnSuccessListener { tvTotalProducts.text = it.size().toString() }
            .addOnFailureListener { tvTotalProducts.text = "0" }

        // Count orders and pending orders
        db.collection("orders").get()
            .addOnSuccessListener { documents ->
                tvTotalOrders.text = documents.size().toString()
                val pending = documents.count { doc ->
                    doc.getString("status") == "Chờ xác nhận"
                }
                tvPendingOrders.text = pending.toString()
            }
            .addOnFailureListener {
                tvTotalOrders.text = "0"
                tvPendingOrders.text = "0"
            }

        // Count users
        db.collection("users").get()
            .addOnSuccessListener { tvTotalUsers.text = it.size().toString() }
            .addOnFailureListener { tvTotalUsers.text = "0" }
    }
}
