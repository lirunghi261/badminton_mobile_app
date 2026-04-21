package com.example.buoi1

import java.io.Serializable

data class Category(
    val name: String = "",
    val imageUrl: String = "",  // drawable resource name (e.g., "yonex", "lining")
    val description: String = ""
) : Serializable
