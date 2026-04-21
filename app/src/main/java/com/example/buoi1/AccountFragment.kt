package com.example.buoi1

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment

class AccountFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Show user name
        val tvAccountName = view.findViewById<TextView>(R.id.tvAccountName)
        val currentUser = UserManager.currentUser
        if (currentUser != null) {
            tvAccountName.text = currentUser.fullName
        }

        view.findViewById<LinearLayout>(R.id.btnAccountFavorites).setOnClickListener {
            startActivity(Intent(requireContext(), FavoritesActivity::class.java))
        }

        view.findViewById<LinearLayout>(R.id.btnAccountOrderHistory).setOnClickListener {
            startActivity(Intent(requireContext(), OrderHistoryActivity::class.java))
        }

        // Logout button
        view.findViewById<LinearLayout>(R.id.btnAccountLogout).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất không?")
                .setPositiveButton("Đăng xuất") { _, _ ->
                    UserManager.logout()
                    val intent = Intent(requireContext(), MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    requireActivity().finish()
                }
                .setNegativeButton("Huỷ", null)
                .show()
        }
    }
}
