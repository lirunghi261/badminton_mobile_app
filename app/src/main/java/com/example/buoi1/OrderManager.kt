package com.example.buoi1

object OrderManager {
    val orders = mutableListOf<Order>()

    fun addOrder(order: Order) {
        orders.add(0, order) // Add newest first
    }
}
