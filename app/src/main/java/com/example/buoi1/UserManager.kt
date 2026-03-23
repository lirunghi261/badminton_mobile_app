package com.example.buoi1

object UserManager {
    val addresses = mutableListOf("Chung cư Flora Kikyo, Đỗ Xuân Hợp, Phường Phú Hữu, Quận 9, TP.HCM")
    var selectedAddressIndex = 0

    fun getSelectedAddress(): String {
        return if (addresses.isNotEmpty() && selectedAddressIndex in addresses.indices) {
            addresses[selectedAddressIndex]
        } else ""
    }
}
