package com.example.buoi1

import java.io.Serializable

data class User(
    val username: String = "",
    val password: String = "",
    val fullName: String = "",
    val role: String = "user",
    val email: String = "",
    val phone: String = "",
    val addresses: List<String> = emptyList(),
    val selectedAddressIndex: Int = 0
) : Serializable
