package com.example.buoi1

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.util.Log
import android.widget.Toast
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
        UserManager.restoreSession(this)?.let { user ->
            openDashboard(user)
            return
        }

        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnLogin.setOnClickListener {
            val username = binding.edtUsername.text.toString().trim()
            val password = binding.edtPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tài khoản và mật khẩu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Disable button to prevent double-tap
            binding.btnLogin.isEnabled = false

            UserManager.login(username, password, this,
                onSuccess = { user ->
                    binding.btnLogin.isEnabled = true

                    when (user.role) {
                        "admin" -> {
                            Toast.makeText(this, "Xin chào Admin: ${user.fullName}", Toast.LENGTH_SHORT).show()
                            openDashboard(user)
                        }
                        else -> {
                            Toast.makeText(this, "Xin chào: ${user.fullName}", Toast.LENGTH_SHORT).show()
                            openDashboard(user)
                        }
                    }
                },
                onFailure = { errorMessage ->
                    binding.btnLogin.isEnabled = true
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                }
            )
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun openDashboard(user: User) {
        val targetActivity = if (user.role == "admin") {
            AdminActivity::class.java
        } else {
            HomeActivity::class.java
        }
        startActivity(Intent(this, targetActivity))
        finish()
    }

}
