package com.example.buoi1

import android.util.Log

object CartManager {
    private const val TAG = "CartManager"
    private val cartItems = mutableListOf<CartItem>()

    // Optional listener to notify UI when cart changes (like updating badges)
    private val listeners = mutableListOf<() -> Unit>()

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
        // trigger immediately for the current state
        listener.invoke()
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        listeners.forEach { it.invoke() }
    }

    fun addToCart(product: Product, quantity: Int = 1) {
        val existingItem = cartItems.find { it.product.name == product.name }
        if (existingItem != null) {
            existingItem.quantity += quantity
        } else {
            cartItems.add(CartItem(product, quantity))
        }
        Log.d(TAG, "Added ${product.name} to cart. Total distinct items: ${cartItems.size}")
        notifyListeners()
    }

    fun removeFromCart(productName: String) {
        val removed = cartItems.removeIf { it.product.name == productName }
        if (removed) {
            notifyListeners()
        }
    }

    fun updateQuantity(productName: String, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeFromCart(productName)
            return
        }
        val item = cartItems.find { it.product.name == productName }
        if (item != null) {
            item.quantity = newQuantity
            notifyListeners()
        }
    }

    fun getCartItems(): List<CartItem> {
        return cartItems.toList()
    }

    fun getTotalItemCount(): Int {
        return cartItems.sumOf { it.quantity }
    }

    fun clearCart() {
        cartItems.clear()
        notifyListeners()
    }
}
