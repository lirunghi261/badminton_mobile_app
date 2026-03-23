package com.example.buoi1

import java.io.Serializable

data class Product(
    val name: String = "",
    val brand: String = "",
    val price: Double = 0.0,
    val description: String = "",
    val specifications: String = "",
    val imageUrl: String = "",
    val isFavorite: Boolean = false,
    val discounted: Int = 0
) : Serializable
