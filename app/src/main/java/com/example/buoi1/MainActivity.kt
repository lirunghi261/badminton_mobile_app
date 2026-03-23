package com.example.buoi1

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.util.Log
import android.widget.Toast.makeText
import com.example.buoi1.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val TAG = "MainActivity"

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "đang ở onStart nè")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "đang ở onResume nè")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "đang ở onPause nè")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "đang ở onStop nè")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "đang ở onDestroy nè")
    }

    override fun onRestart() {
        super.onRestart()
        Log.d(TAG, "đang ở onRestart nè")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "đang ở onCreate nè")
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnLogin.setOnClickListener {

            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }

        binding.tvRegister.setOnClickListener {
           makeText(this, "Register Clicked", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

}