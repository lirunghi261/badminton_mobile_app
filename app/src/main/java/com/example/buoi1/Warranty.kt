package com.example.buoi1

import java.io.Serializable

data class Warranty(
    val id: String = "",
    val userId: String = "",
    val orderId: String = "",
    val productName: String = "",
    val purchaseDate: String = "",
    val expiryDate: String = "",
    val status: String = "Đang bảo hành", // Đang bảo hành | Hết hạn | Chờ xử lý | Đã xử lý
    val claimNote: String = "",
    val adminNote: String = ""
) : Serializable
