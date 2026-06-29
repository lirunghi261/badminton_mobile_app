package com.example.buoi1

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.buoi1.databinding.RegisterScreenBinding

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: RegisterScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = RegisterScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.register) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left + 24.dpToPx(), systemBars.top + 24.dpToPx(), systemBars.right + 24.dpToPx(), systemBars.bottom + 24.dpToPx())
            insets
        }

        binding.btnRegister.setOnClickListener {
            registerAccount()
        }

        binding.tvLogin.setOnClickListener {
            finish()
        }
    }

    private fun registerAccount() {
        val fullName = binding.edtFullName.text.toString().trim()
        val email = binding.edtEmail.text.toString().trim()
        val phone = binding.edtPhone.text.toString().trim()
        val username = binding.edtUsername.text.toString().trim()
        val password = binding.edtPassword.text.toString().trim()
        val confirmPassword = binding.edtConfirmPassword.text.toString().trim()

        when {
            fullName.isEmpty() || email.isEmpty() || phone.isEmpty() || username.isEmpty() ||
                password.isEmpty() || confirmPassword.isEmpty() -> {
                Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                Toast.makeText(this, "Email không hợp lệ", Toast.LENGTH_SHORT).show()
                return
            }
            phone.length < 9 -> {
                Toast.makeText(this, "Số điện thoại không hợp lệ", Toast.LENGTH_SHORT).show()
                return
            }
            password.length < 6 -> {
                Toast.makeText(this, "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show()
                return
            }
            password != confirmPassword -> {
                Toast.makeText(this, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show()
                return
            }
        }

        binding.btnRegister.isEnabled = false

        UserManager.register(
            fullName = fullName,
            email = email,
            phone = phone,
            username = username,
            password = password,
            context = this,
            onSuccess = { user ->
                binding.btnRegister.isEnabled = true
                Toast.makeText(this, "Đăng ký thành công. Xin chào ${user.fullName}", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, HomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            },
            onFailure = { errorMessage ->
                binding.btnRegister.isEnabled = true
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}
