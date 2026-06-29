package com.example.buoi1

import android.content.Context
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray

object UserManager {
    private const val PREFS_NAME = "user_session"
    private const val KEY_USERNAME = "username"
    private const val KEY_FULL_NAME = "full_name"
    private const val KEY_ROLE = "role"
    private const val KEY_EMAIL = "email"
    private const val KEY_PHONE = "phone"
    private const val KEY_ADDRESSES = "addresses"
    private const val KEY_SELECTED_ADDRESS_INDEX = "selected_address_index"

    var currentUser: User? = null

    fun getAddresses(): List<String> {
        return currentUser?.addresses.orEmpty()
    }

    fun getSelectedAddress(): String {
        val user = currentUser ?: return ""
        val addresses = user.addresses
        val selectedIndex = normalizeSelectedAddressIndex(user.selectedAddressIndex, addresses)
        return if (addresses.isNotEmpty()) addresses[selectedIndex] else ""
    }

    fun getSelectedAddressIndex(): Int {
        val user = currentUser ?: return 0
        return normalizeSelectedAddressIndex(user.selectedAddressIndex, user.addresses)
    }

    /**
     * Login - check username and password from Firestore "users" collection
     */
    fun login(
        username: String,
        password: String,
        context: Context? = null,
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
                    val user = userFromDocument(doc)
                    currentUser = user
                    ensureAddressFields(doc)
                    context?.let { saveSession(it, user) }
                    onSuccess(user)
                }
            }
            .addOnFailureListener { e ->
                onFailure("Lỗi kết nối: ${e.message}")
            }
    }

    fun register(
        fullName: String,
        email: String,
        phone: String,
        username: String,
        password: String,
        context: Context? = null,
        onSuccess: (User) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    onFailure("Tên đăng nhập đã tồn tại")
                    return@addOnSuccessListener
                }

                val data = hashMapOf<String, Any>(
                    "fullName" to fullName,
                    "email" to email,
                    "phone" to phone,
                    "username" to username,
                    "password" to password,
                    "role" to "user",
                    "addresses" to emptyList<String>(),
                    "selectedAddressIndex" to 0
                )

                db.collection("users")
                    .add(data)
                    .addOnSuccessListener {
                        val user = User(
                            username = username,
                            password = password,
                            fullName = fullName,
                            role = "user",
                            email = email,
                            phone = phone,
                            addresses = emptyList(),
                            selectedAddressIndex = 0
                        )
                        currentUser = user
                        context?.let { saveSession(it, user) }
                        onSuccess(user)
                    }
                    .addOnFailureListener { e ->
                        onFailure("Lỗi tạo tài khoản: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                onFailure("Lỗi kết nối: ${e.message}")
            }
    }

    fun addAddress(
        context: Context,
        address: String,
        onSuccess: (User) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val user = currentUser ?: run {
            onFailure("Bạn cần đăng nhập để thêm địa chỉ")
            return
        }

        val newAddresses = user.addresses.toMutableList()
        newAddresses.add(address)
        updateAddressState(
            context = context,
            addresses = newAddresses,
            selectedIndex = newAddresses.lastIndex,
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    fun updateAddress(
        context: Context,
        index: Int,
        address: String,
        onSuccess: (User) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val user = currentUser ?: run {
            onFailure("Bạn cần đăng nhập để sửa địa chỉ")
            return
        }

        if (index !in user.addresses.indices) {
            onFailure("Địa chỉ không hợp lệ")
            return
        }

        val newAddresses = user.addresses.toMutableList()
        newAddresses[index] = address
        updateAddressState(
            context = context,
            addresses = newAddresses,
            selectedIndex = getSelectedAddressIndex(),
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    fun deleteAddress(
        context: Context,
        index: Int,
        onSuccess: (User) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val user = currentUser ?: run {
            onFailure("Bạn cần đăng nhập để xoá địa chỉ")
            return
        }

        if (index !in user.addresses.indices) {
            onFailure("Địa chỉ không hợp lệ")
            return
        }

        val currentSelectedIndex = getSelectedAddressIndex()
        val newAddresses = user.addresses.toMutableList()
        newAddresses.removeAt(index)

        val newSelectedIndex = when {
            newAddresses.isEmpty() -> 0
            index < currentSelectedIndex -> currentSelectedIndex - 1
            index == currentSelectedIndex -> index.coerceAtMost(newAddresses.lastIndex)
            else -> currentSelectedIndex.coerceAtMost(newAddresses.lastIndex)
        }

        updateAddressState(
            context = context,
            addresses = newAddresses,
            selectedIndex = newSelectedIndex,
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    fun selectAddress(
        context: Context,
        index: Int,
        onSuccess: (User) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val addresses = currentUser?.addresses.orEmpty()
        if (index !in addresses.indices) {
            onFailure("Địa chỉ không hợp lệ")
            return
        }

        updateAddressState(
            context = context,
            addresses = addresses,
            selectedIndex = index,
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    /**
     * Logout - clear current user
     */
    fun logout(context: Context? = null) {
        currentUser = null
        context?.applicationContext
            ?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.clear()
            ?.apply()
    }

    /**
     * Check if current user is admin
     */
    fun isAdmin(): Boolean {
        return currentUser?.role == "admin"
    }

    fun restoreSession(context: Context): User? {
        currentUser?.let { return it }

        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val username = prefs.getString(KEY_USERNAME, null)

        if (username.isNullOrBlank()) {
            return null
        }

        val addresses = addressesFromJson(prefs.getString(KEY_ADDRESSES, null))
        val selectedAddressIndex = normalizeSelectedAddressIndex(
            prefs.getInt(KEY_SELECTED_ADDRESS_INDEX, 0),
            addresses
        )
        val user = User(
            username = username,
            fullName = prefs.getString(KEY_FULL_NAME, "") ?: "",
            role = prefs.getString(KEY_ROLE, "user") ?: "user",
            email = prefs.getString(KEY_EMAIL, "") ?: "",
            phone = prefs.getString(KEY_PHONE, "") ?: "",
            addresses = addresses,
            selectedAddressIndex = selectedAddressIndex
        )
        currentUser = user
        return user
    }

    private fun updateAddressState(
        context: Context,
        addresses: List<String>,
        selectedIndex: Int,
        onSuccess: (User) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val user = currentUser ?: run {
            onFailure("Bạn cần đăng nhập để cập nhật địa chỉ")
            return
        }

        val cleanedAddresses = addresses.map { it.trim() }.filter { it.isNotEmpty() }
        val normalizedIndex = normalizeSelectedAddressIndex(selectedIndex, cleanedAddresses)
        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .whereEqualTo("username", user.username)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    onFailure("Không tìm thấy tài khoản hiện tại")
                    return@addOnSuccessListener
                }

                val updatedUser = user.copy(
                    addresses = cleanedAddresses,
                    selectedAddressIndex = normalizedIndex
                )
                documents.documents[0].reference
                    .update(
                        mapOf(
                            "addresses" to cleanedAddresses,
                            "selectedAddressIndex" to normalizedIndex
                        )
                    )
                    .addOnSuccessListener {
                        currentUser = updatedUser
                        saveSession(context, updatedUser)
                        onSuccess(updatedUser)
                    }
                    .addOnFailureListener { e ->
                        onFailure("Lỗi lưu địa chỉ: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                onFailure("Lỗi kết nối: ${e.message}")
            }
    }

    private fun userFromDocument(doc: DocumentSnapshot): User {
        val addresses = addressesFromFirestore(doc.get("addresses"))
        val selectedAddressIndex = normalizeSelectedAddressIndex(
            doc.getLong("selectedAddressIndex")?.toInt() ?: 0,
            addresses
        )
        return User(
            username = doc.getString("username") ?: "",
            password = doc.getString("password") ?: "",
            fullName = doc.getString("fullName") ?: "",
            role = doc.getString("role") ?: "user",
            email = doc.getString("email") ?: "",
            phone = doc.getString("phone") ?: "",
            addresses = addresses,
            selectedAddressIndex = selectedAddressIndex
        )
    }

    private fun ensureAddressFields(doc: DocumentSnapshot) {
        val updates = mutableMapOf<String, Any>()
        if (!doc.contains("addresses")) {
            updates["addresses"] = emptyList<String>()
        }
        if (!doc.contains("selectedAddressIndex")) {
            updates["selectedAddressIndex"] = 0
        }
        if (updates.isNotEmpty()) {
            doc.reference.update(updates)
        }
    }

    private fun saveSession(context: Context, user: User) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_USERNAME, user.username)
            .putString(KEY_FULL_NAME, user.fullName)
            .putString(KEY_ROLE, user.role)
            .putString(KEY_EMAIL, user.email)
            .putString(KEY_PHONE, user.phone)
            .putString(KEY_ADDRESSES, addressesToJson(user.addresses))
            .putInt(KEY_SELECTED_ADDRESS_INDEX, normalizeSelectedAddressIndex(user.selectedAddressIndex, user.addresses))
            .apply()
    }

    private fun addressesFromFirestore(value: Any?): List<String> {
        return (value as? List<*>)
            ?.mapNotNull { it as? String }
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }

    private fun addressesFromJson(value: String?): List<String> {
        if (value.isNullOrBlank()) {
            return emptyList()
        }

        return try {
            val jsonArray = JSONArray(value)
            buildList {
                for (i in 0 until jsonArray.length()) {
                    val address = jsonArray.optString(i).trim()
                    if (address.isNotEmpty()) add(address)
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun addressesToJson(addresses: List<String>): String {
        val jsonArray = JSONArray()
        addresses.forEach { jsonArray.put(it) }
        return jsonArray.toString()
    }

    private fun normalizeSelectedAddressIndex(index: Int, addresses: List<String>): Int {
        return if (addresses.isEmpty()) 0 else index.coerceIn(0, addresses.lastIndex)
    }
}
