package com.example.buoi1

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class AddressSelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_address_selection)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.topBarAddress)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, bars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnAddressBack).setOnClickListener { finish() }

        renderAddresses()

        val etNewAddress = findViewById<EditText>(R.id.etNewAddress)
        val btnAddAddress = findViewById<Button>(R.id.btnAddAddress)

        btnAddAddress.setOnClickListener {
            val newAddress = etNewAddress.text.toString().trim()
            if (newAddress.isNotEmpty()) {
                UserManager.addresses.add(newAddress)
                UserManager.selectedAddressIndex = UserManager.addresses.size - 1
                etNewAddress.text.clear()
                renderAddresses()
                Toast.makeText(this, "Đã thêm địa chỉ", Toast.LENGTH_SHORT).show()
                finish() // optionally finish immediately after adding
            } else {
                Toast.makeText(this, "Vui lòng nhập địa chỉ", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun renderAddresses() {
        val llAddressList = findViewById<LinearLayout>(R.id.llAddressList)
        llAddressList.removeAllViews()

        for ((index, address) in UserManager.addresses.withIndex()) {
            val itemView = layoutInflater.inflate(R.layout.item_address, llAddressList, false)
            
            val tvAddressLocation = itemView.findViewById<TextView>(R.id.tvAddressLocation)
            val imgCheck = itemView.findViewById<View>(R.id.imgCheck)

            tvAddressLocation.text = address
            
            if (index == UserManager.selectedAddressIndex) {
                imgCheck.visibility = View.VISIBLE
            } else {
                imgCheck.visibility = View.INVISIBLE
            }

            itemView.setOnClickListener {
                UserManager.selectedAddressIndex = index
                finish()
            }

            llAddressList.addView(itemView)
        }
    }
}
