package com.example.buoi1

import java.io.Serializable

data class Order(
    val id: String,
    val date: String,
    val totalAmount: Double,
    val items: List<CartItem>,
    val status: String,
    val paymentMethod: String,
    val address: String
) : Serializable
