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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class AddressSelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (UserManager.restoreSession(this) == null) {
            Toast.makeText(this, "Bạn cần đăng nhập để quản lý địa chỉ", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

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
            if (newAddress.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập địa chỉ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnAddAddress.isEnabled = false
            UserManager.addAddress(this, newAddress,
                onSuccess = {
                    btnAddAddress.isEnabled = true
                    etNewAddress.text.clear()
                    renderAddresses()
                    Toast.makeText(this, "Đã thêm địa chỉ", Toast.LENGTH_SHORT).show()
                    finish()
                },
                onFailure = { errorMessage ->
                    btnAddAddress.isEnabled = true
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun renderAddresses() {
        val llAddressList = findViewById<LinearLayout>(R.id.llAddressList)
        llAddressList.removeAllViews()

        val addresses = UserManager.getAddresses()
        val selectedAddressIndex = UserManager.getSelectedAddressIndex()

        if (addresses.isEmpty()) {
            val emptyView = TextView(this)
            emptyView.text = "Chưa có địa chỉ. Vui lòng thêm địa chỉ mới."
            emptyView.setTextColor(android.graphics.Color.parseColor("#777777"))
            emptyView.textSize = 14f
            emptyView.setPadding(16, 24, 16, 24)
            llAddressList.addView(emptyView)
            return
        }

        for ((index, address) in addresses.withIndex()) {
            val itemView = layoutInflater.inflate(R.layout.item_address, llAddressList, false)

            val tvAddressLocation = itemView.findViewById<TextView>(R.id.tvAddressLocation)
            val imgCheck = itemView.findViewById<View>(R.id.imgCheck)
            val btnEditAddress = itemView.findViewById<ImageButton>(R.id.btnEditAddress)
            val btnDeleteAddress = itemView.findViewById<ImageButton>(R.id.btnDeleteAddress)

            tvAddressLocation.text = address
            imgCheck.visibility = if (index == selectedAddressIndex) View.VISIBLE else View.INVISIBLE

            itemView.setOnClickListener {
                UserManager.selectAddress(this, index,
                    onSuccess = {
                        finish()
                    },
                    onFailure = { errorMessage ->
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                )
            }

            btnEditAddress.setOnClickListener {
                showEditAddressDialog(index, address)
            }

            btnDeleteAddress.setOnClickListener {
                confirmDeleteAddress(index, address)
            }

            llAddressList.addView(itemView)
        }
    }

    private fun showEditAddressDialog(index: Int, currentAddress: String) {
        val input = EditText(this).apply {
            setText(currentAddress)
            setSelectAllOnFocus(true)
            minHeight = 48.dpToPx()
            setPadding(12.dpToPx(), 8.dpToPx(), 12.dpToPx(), 8.dpToPx())
            background = getDrawable(R.drawable.bg_border_rounded)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Sửa địa chỉ")
            .setView(input)
            .setPositiveButton("Lưu", null)
            .setNegativeButton("Huỷ", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val newAddress = input.text.toString().trim()
                if (newAddress.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập địa chỉ", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                UserManager.updateAddress(this, index, newAddress,
                    onSuccess = {
                        renderAddresses()
                        Toast.makeText(this, "Đã cập nhật địa chỉ", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    },
                    onFailure = { errorMessage ->
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        dialog.show()
    }

    private fun confirmDeleteAddress(index: Int, address: String) {
        AlertDialog.Builder(this)
            .setTitle("Xoá địa chỉ")
            .setMessage("Bạn có chắc muốn xoá địa chỉ này không?\n\n$address")
            .setPositiveButton("Xoá") { _, _ ->
                UserManager.deleteAddress(this, index,
                    onSuccess = {
                        renderAddresses()
                        Toast.makeText(this, "Đã xoá địa chỉ", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { errorMessage ->
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Huỷ", null)
            .show()
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}
