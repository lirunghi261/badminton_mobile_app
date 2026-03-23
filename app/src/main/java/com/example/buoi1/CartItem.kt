package com.example.buoi1

import java.io.Serializable

data class CartItem(
    val product: Product,
    var quantity: Int = 1
) : Serializable
