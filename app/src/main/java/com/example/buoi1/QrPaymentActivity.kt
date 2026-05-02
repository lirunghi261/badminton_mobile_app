package com.example.buoi1

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import java.text.NumberFormat
import java.util.Locale

class QrPaymentActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ORDER_ID = "EXTRA_ORDER_ID"
        const val EXTRA_AMOUNT = "EXTRA_AMOUNT"

        private const val BANK_ID = "TPB"
        private const val ACCOUNT_NO = "10000745087"
        private const val ACCOUNT_NAME = "NGUYEN NGOC THIEN"
        private const val TEMPLATE = "compact2"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_qr_payment)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.topBarQrPayment)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, bars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        val orderId = intent.getStringExtra(EXTRA_ORDER_ID) ?: ""
        val amount = intent.getLongExtra(EXTRA_AMOUNT, 0L)

        val formatter = NumberFormat.getInstance(Locale("vi", "VN"))
        val transferContent = "Thanh toan don #$orderId"

        findViewById<TextView>(R.id.tvQrAccountName).text = ACCOUNT_NAME
        findViewById<TextView>(R.id.tvQrAccountNumber).text = ACCOUNT_NO
        findViewById<TextView>(R.id.tvQrAmount).text = "đ${formatter.format(amount)}"
        findViewById<TextView>(R.id.tvQrContent).text = transferContent

        loadQrCode(amount, transferContent)

        findViewById<ImageButton>(R.id.btnQrBack).setOnClickListener { finish() }

        findViewById<Button>(R.id.btnCancelQrPayment).setOnClickListener { finish() }

        findViewById<Button>(R.id.btnConfirmPayment).setOnClickListener {
            Toast.makeText(this, "Đơn hàng #$orderId đang chờ xác nhận thanh toán", Toast.LENGTH_LONG).show()
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun loadQrCode(amount: Long, description: String) {
        val imgQr = findViewById<ImageView>(R.id.imgQrCode)
        val progress = findViewById<ProgressBar>(R.id.progressQr)

        // VietQR API: https://img.vietqr.io/image/{bankId}-{accountNo}-{template}.png?amount=X&addInfo=Y&accountName=Z
        val encodedDesc = java.net.URLEncoder.encode(description, "UTF-8")
        val encodedName = java.net.URLEncoder.encode(ACCOUNT_NAME, "UTF-8")
        val url = "https://img.vietqr.io/image/$BANK_ID-$ACCOUNT_NO-$TEMPLATE.png" +
                "?amount=$amount&addInfo=$encodedDesc&accountName=$encodedName"

        progress.visibility = View.VISIBLE
        imgQr.visibility = View.INVISIBLE

        Glide.with(this)
            .load(url)
            .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                override fun onLoadFailed(
                    e: com.bumptech.glide.load.engine.GlideException?,
                    model: Any?,
                    target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    progress.visibility = View.GONE
                    imgQr.visibility = View.VISIBLE
                    Toast.makeText(this@QrPaymentActivity, "Không tải được mã QR", Toast.LENGTH_SHORT).show()
                    return false
                }

                override fun onResourceReady(
                    resource: android.graphics.drawable.Drawable,
                    model: Any,
                    target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                    dataSource: com.bumptech.glide.load.DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    progress.visibility = View.GONE
                    imgQr.visibility = View.VISIBLE
                    return false
                }
            })
            .into(imgQr)
    }
}
