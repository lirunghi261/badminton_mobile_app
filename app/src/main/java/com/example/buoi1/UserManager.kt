package com.example.buoi1

import com.google.firebase.firestore.FirebaseFirestore

object UserManager {
    val addresses = mutableListOf("Chung cư Flora Kikyo, Đỗ Xuân Hợp, Phường Phú Hữu, Quận 9, TP.HCM")
    var selectedAddressIndex = 0
    var currentUser: User? = null

    fun getSelectedAddress(): String {
        return if (addresses.isNotEmpty() && selectedAddressIndex in addresses.indices) {
            addresses[selectedAddressIndex]
        } else ""
    }

    /**
     * Login - check username and password from Firestore "users" collection
     */
    fun login(
        username: String,
        password: String,
        onSuccess: (User) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .whereEqualTo("username", username)
            .whereEqualTo("password", password)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    onFailure("Sai tài khoản hoặc mật khẩu")
                } else {
                    val doc = documents.documents[0]
                    val user = User(
                        username = doc.getString("username") ?: "",
                        password = doc.getString("password") ?: "",
                        fullName = doc.getString("fullName") ?: "",
                        role = doc.getString("role") ?: "user"
                    )
                    currentUser = user
                    onSuccess(user)
                }
            }
            .addOnFailureListener { e ->
                onFailure("Lỗi kết nối: ${e.message}")
            }
    }

    /**
     * Logout - clear current user
     */
    fun logout() {
        currentUser = null
    }

    /**
     * Check if current user is admin
     */
    fun isAdmin(): Boolean {
        return currentUser?.role == "admin"
    }
}
