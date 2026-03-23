package com.example.buoi1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        
        view.findViewById<android.widget.LinearLayout>(R.id.btnAccountFavorites).setOnClickListener {
            startActivity(android.content.Intent(requireContext(), FavoritesActivity::class.java))
        }

        view.findViewById<android.widget.LinearLayout>(R.id.btnAccountOrderHistory).setOnClickListener {
            startActivity(android.content.Intent(requireContext(), OrderHistoryActivity::class.java))
        }
    }
}
