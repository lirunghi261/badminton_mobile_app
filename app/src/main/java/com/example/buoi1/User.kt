package com.example.buoi1

import java.io.Serializable

data class User(
    val username: String = "",
    val password: String = "",
    val fullName: String = "",
    val role: String = "user" // "user" hoặc "admin"
) : Serializable
