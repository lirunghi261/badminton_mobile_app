package com.example.buoi1

import com.google.firebase.firestore.FirebaseFirestore

object OrderManager {
    // Local cache for quick access
    val orders = mutableListOf<Order>()

    private fun getDb() = FirebaseFirestore.getInstance()

    /**
     * Add order to Firestore and local cache
     */
    fun addOrder(order: Order, onSuccess: (() -> Unit)? = null, onFailure: ((Exception) -> Unit)? = null) {
        val orderData = hashMapOf(
            "orderId" to order.id,
            "date" to order.date,
            "totalAmount" to order.totalAmount,
            "status" to order.status,
            "paymentMethod" to order.paymentMethod,
            "address" to order.address,
            "items" to order.items.map { cartItem ->
                hashMapOf(
                    "productName" to cartItem.product.name,
                    "productBrand" to cartItem.product.brand,
                    "productPrice" to cartItem.product.price,
                    "productImageUrl" to cartItem.product.imageUrl,
                    "productDiscounted" to cartItem.product.discounted,
                    "quantity" to cartItem.quantity
                )
            }
        )

        getDb().collection("orders")
            .document(order.id)
            .set(orderData)
            .addOnSuccessListener {
                orders.add(0, order)
                onSuccess?.invoke()
            }
            .addOnFailureListener { e ->
                onFailure?.invoke(e)
            }
    }

    /**
     * Get order by ID from local cache
     */
    fun getOrderById(id: String): Order? {
        return orders.find { it.id == id }
    }

    /**
     * Cancel order - update status in Firestore and local cache
     */
    fun cancelOrder(id: String, onSuccess: (() -> Unit)? = null, onFailure: ((Exception) -> Unit)? = null) {
        getDb().collection("orders")
            .document(id)
            .update("status", "Đã huỷ")
            .addOnSuccessListener {
                val index = orders.indexOfFirst { it.id == id }
                if (index != -1) {
                    val order = orders[index]
                    orders[index] = order.copy(status = "Đã huỷ")
                }
                onSuccess?.invoke()
            }
            .addOnFailureListener { e ->
                onFailure?.invoke(e)
            }
    }

    /**
     * Reorder - change status back to "Chờ xác nhận" on Firestore and local cache
     */
    fun reorderOrder(id: String, onSuccess: (() -> Unit)? = null, onFailure: ((Exception) -> Unit)? = null) {
        getDb().collection("orders")
            .document(id)
            .update("status", "Chờ xác nhận")
            .addOnSuccessListener {
                val index = orders.indexOfFirst { it.id == id }
                if (index != -1) {
                    val order = orders[index]
                    orders[index] = order.copy(status = "Chờ xác nhận")
                }
                onSuccess?.invoke()
            }
            .addOnFailureListener { e ->
                onFailure?.invoke(e)
            }
    }

    /**
     * Update order status - for admin to change status to any value
     */
    fun updateOrderStatus(id: String, newStatus: String, onSuccess: (() -> Unit)? = null, onFailure: ((Exception) -> Unit)? = null) {
        getDb().collection("orders")
            .document(id)
            .update("status", newStatus)
            .addOnSuccessListener {
                val index = orders.indexOfFirst { it.id == id }
                if (index != -1) {
                    val order = orders[index]
                    orders[index] = order.copy(status = newStatus)
                }
                onSuccess?.invoke()
            }
            .addOnFailureListener { e ->
                onFailure?.invoke(e)
            }
    }

    /**
     * Delete order from Firestore and local cache
     */
    fun deleteOrder(id: String, onSuccess: (() -> Unit)? = null, onFailure: ((Exception) -> Unit)? = null) {
        getDb().collection("orders")
            .document(id)
            .delete()
            .addOnSuccessListener {
                orders.removeAll { it.id == id }
                onSuccess?.invoke()
            }
            .addOnFailureListener { e ->
                onFailure?.invoke(e)
            }
    }

    /**
     * Fetch all orders from Firestore and update local cache
     */
    fun fetchOrders(onSuccess: ((List<Order>) -> Unit)? = null, onFailure: ((Exception) -> Unit)? = null) {
        getDb().collection("orders")
            .get()
            .addOnSuccessListener { documents ->
                orders.clear()
                for (doc in documents) {
                    try {
                        val orderId = doc.getString("orderId") ?: doc.id
                        val date = doc.getString("date") ?: ""
                        val totalAmount = doc.getDouble("totalAmount") ?: 0.0
                        val status = doc.getString("status") ?: ""
                        val paymentMethod = doc.getString("paymentMethod") ?: ""
                        val address = doc.getString("address") ?: ""

                        val itemsList = doc.get("items") as? List<Map<String, Any>> ?: emptyList()
                        val cartItems = itemsList.map { itemMap ->
                            val product = Product(
                                name = itemMap["productName"] as? String ?: "",
                                brand = itemMap["productBrand"] as? String ?: "",
                                price = (itemMap["productPrice"] as? Number)?.toDouble() ?: 0.0,
                                imageUrl = itemMap["productImageUrl"] as? String ?: "",
                                discounted = (itemMap["productDiscounted"] as? Number)?.toInt() ?: 0
                            )
                            CartItem(
                                product = product,
                                quantity = (itemMap["quantity"] as? Number)?.toInt() ?: 1
                            )
                        }

                        val order = Order(
                            id = orderId,
                            date = date,
                            totalAmount = totalAmount,
                            items = cartItems,
                            status = status,
                            paymentMethod = paymentMethod,
                            address = address
                        )
                        orders.add(order)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                // Sort by date descending (newest first)
                orders.sortByDescending { it.date }
                onSuccess?.invoke(orders)
            }
            .addOnFailureListener { e ->
                onFailure?.invoke(e)
            }
    }
}
