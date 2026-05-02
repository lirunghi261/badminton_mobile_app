package com.example.buoi1

import java.io.Serializable

data class Deal(
    val code: String = "",
    val discountPercent: Int = 0,
    val startDate: String = "",  // "dd/MM/yyyy"
    val endDate: String = "",    // "dd/MM/yyyy"
    val isActive: Boolean = true,
    val usedBy: List<String> = emptyList()
) : Serializable
