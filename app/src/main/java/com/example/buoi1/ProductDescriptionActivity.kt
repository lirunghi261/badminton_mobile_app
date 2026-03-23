package com.example.buoi1

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ProductDescriptionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_product_description)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.descTopBar)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, bars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnDescBack).setOnClickListener { finish() }

        val desc = intent.getStringExtra("EXTRA_DESC") ?: "Đang cập nhật..."
        findViewById<TextView>(R.id.tvFullDescription).text = desc
    }
}
