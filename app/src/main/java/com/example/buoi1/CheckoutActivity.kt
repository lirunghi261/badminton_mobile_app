package com.example.buoi1

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CheckoutActivity : AppCompatActivity() {

    private val formatter = NumberFormat.getInstance(Locale("vi", "VN"))
    private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private var baseTotal = 0.0
    private var appliedDiscountPercent = 0
    private var appliedDealDocId: String? = null
    private var appliedDealCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_checkout)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.topBarCheckout)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, bars.top, v.paddingRight, v.paddingBottom)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bottomBarCheckout)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, bars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnCheckoutBack).setOnClickListener { finish() }
        findViewById<LinearLayout>(R.id.llAddressContainer).setOnClickListener {
            startActivity(Intent(this, AddressSelectionActivity::class.java))
        }

        val directBuyProduct = intent.getSerializableExtra("EXTRA_DIRECT_BUY_PRODUCT") as? Product
        val itemsToCheckout = if (directBuyProduct != null) {
            listOf(CartItem(directBuyProduct, 1))
        } else {
            val selectedItemNames = intent.getStringArrayListExtra("EXTRA_SELECTED_ITEMS") ?: arrayListOf()
            if (selectedItemNames.isEmpty()) {
                Toast.makeText(this, "Không có sản phẩm nào để thanh toán", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            CartManager.getCartItems().filter { it.product.name in selectedItemNames }
        }

        // Build item list
        val llCheckoutItems = findViewById<LinearLayout>(R.id.llCheckoutItems)
        val tvCheckoutTotalItems = findViewById<TextView>(R.id.tvCheckoutTotalItems)
        val tvCheckoutTotalPrice = findViewById<TextView>(R.id.tvCheckoutTotalPrice)
        val tvCheckoutDiscountLine = findViewById<TextView>(R.id.tvCheckoutDiscountLine)
        var totalQuantity = 0

        for (item in itemsToCheckout) {
            val itemView = layoutInflater.inflate(R.layout.item_checkout_product, llCheckoutItems, false)
            itemView.findViewById<TextView>(R.id.tvCheckoutProductName).text = item.product.name
            itemView.findViewById<TextView>(R.id.tvCheckoutQuantity).text = "x${item.quantity}"

            if (item.product.imageUrl.isNotEmpty()) {
                val resId = resources.getIdentifier(item.product.imageUrl, "drawable", packageName)
                if (resId != 0) itemView.findViewById<ImageView>(R.id.imgCheckoutProduct).setImageResource(resId)
            }

            val tvPrice = itemView.findViewById<TextView>(R.id.tvCheckoutProductPrice)
            val tvOriginal = itemView.findViewById<TextView>(R.id.tvCheckoutProductPriceOriginal)
            val activePrice = if (item.product.discounted > 0) {
                val discounted = item.product.price * (1 - item.product.discounted / 100.0)
                tvOriginal.text = "đ${formatter.format(item.product.price)}"
                tvOriginal.paintFlags = tvOriginal.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                tvOriginal.visibility = View.VISIBLE
                discounted
            } else {
                item.product.price
            }
            tvPrice.text = "đ${formatter.format(activePrice)}"
            baseTotal += activePrice * item.quantity
            totalQuantity += item.quantity
            llCheckoutItems.addView(itemView)
        }

        tvCheckoutTotalItems.text = "Tổng ($totalQuantity mặt hàng)"
        tvCheckoutTotalPrice.text = "đ${formatter.format(baseTotal)}"

        // ---- Promo picker ----
        val llPickPromo = findViewById<LinearLayout>(R.id.llPickPromo)
        val tvPromoPickerLabel = findViewById<TextView>(R.id.tvPromoPickerLabel)
        val llPromoResult = findViewById<LinearLayout>(R.id.llPromoResult)
        val tvPromoDesc = findViewById<TextView>(R.id.tvPromoDesc)
        val tvPromoSaving = findViewById<TextView>(R.id.tvPromoSaving)
        val btnRemovePromo = findViewById<ImageView>(R.id.btnRemovePromo)

        llPickPromo.setOnClickListener {
            showDealPicker { docId, code, discount ->
                appliedDealDocId = docId
                appliedDealCode = code
                appliedDiscountPercent = discount
                val saving = baseTotal * discount / 100.0
                val finalTotal = baseTotal - saving

                tvPromoPickerLabel.text = code
                tvPromoPickerLabel.setTextColor(android.graphics.Color.parseColor("#E64A19"))
                tvPromoDesc.text = "Mã $code — Giảm $discount%"
                tvPromoSaving.text = "-đ${formatter.format(saving)}"
                llPromoResult.visibility = View.VISIBLE

                tvCheckoutDiscountLine.text = "Giảm $discount% (-đ${formatter.format(saving)})"
                tvCheckoutDiscountLine.visibility = View.VISIBLE
                tvCheckoutTotalPrice.text = "đ${formatter.format(finalTotal)}"
            }
        }

        btnRemovePromo.setOnClickListener {
            appliedDealDocId = null
            appliedDealCode = null
            appliedDiscountPercent = 0
            llPromoResult.visibility = View.GONE
            tvCheckoutDiscountLine.visibility = View.GONE
            tvCheckoutTotalPrice.text = "đ${formatter.format(baseTotal)}"
            tvPromoPickerLabel.text = "Chọn mã giảm giá"
            tvPromoPickerLabel.setTextColor(android.graphics.Color.parseColor("#888888"))
        }

        // ---- Place order ----
        val btnPlaceOrder = findViewById<Button>(R.id.btnCheckoutPlaceOrder)
        btnPlaceOrder.setOnClickListener {
            val paymentGroup = findViewById<RadioGroup>(R.id.rgPaymentMethods)
            val isCOD = paymentGroup.checkedRadioButtonId == R.id.rbPaymentCOD
            val paymentMethod = if (isCOD) "Thanh toán khi nhận hàng" else "Chuyển khoản ngân hàng"
            val finalTotal = if (appliedDiscountPercent > 0) baseTotal * (1 - appliedDiscountPercent / 100.0) else baseTotal

            val order = Order(
                id = java.util.UUID.randomUUID().toString().substring(0, 8).uppercase(java.util.Locale.ROOT),
                date = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(Date()),
                totalAmount = finalTotal,
                items = itemsToCheckout,
                status = "Chờ xác nhận",
                paymentMethod = paymentMethod,
                address = UserManager.getSelectedAddress().ifEmpty { "Chưa có địa chỉ" }
            )

            btnPlaceOrder.isEnabled = false
            OrderManager.addOrder(order,
                onSuccess = {
                    val username = UserManager.currentUser?.username
                    val dealDoc = appliedDealDocId
                    if (dealDoc != null && username != null) {
                        FirebaseFirestore.getInstance()
                            .collection("deals").document(dealDoc)
                            .update("usedBy", FieldValue.arrayUnion(username))
                    }
                    if (directBuyProduct == null) {
                        itemsToCheckout.map { it.product.name }.forEach { CartManager.removeFromCart(it) }
                    }
                    if (!isCOD) {
                        // Open QR payment screen
                        val qrIntent = Intent(this, QrPaymentActivity::class.java)
                        qrIntent.putExtra(QrPaymentActivity.EXTRA_ORDER_ID, order.id)
                        qrIntent.putExtra(QrPaymentActivity.EXTRA_AMOUNT, finalTotal.toLong())
                        qrIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(qrIntent)
                        finish()
                    } else {
                        Toast.makeText(applicationContext, "Đặt hàng thành công!", Toast.LENGTH_LONG).show()
                        val intent = Intent(this, HomeActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        finish()
                    }
                },
                onFailure = { e ->
                    btnPlaceOrder.isEnabled = true
                    Toast.makeText(applicationContext, "Đặt hàng thất bại: ${e.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun showDealPicker(onSelected: (docId: String, code: String, discount: Int) -> Unit) {
        val username = UserManager.currentUser?.username ?: run {
            Toast.makeText(this, "Bạn cần đăng nhập để dùng mã deal", Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_deal_picker, null)
        dialog.setContentView(sheetView)

        val rv = sheetView.findViewById<RecyclerView>(R.id.rvDealPicker)
        val layoutEmpty = sheetView.findViewById<LinearLayout>(R.id.layoutDealPickerEmpty)
        val progress = sheetView.findViewById<ProgressBar>(R.id.progressDealPicker)

        rv.layoutManager = LinearLayoutManager(this)
        progress.visibility = View.VISIBLE

        sheetView.findViewById<ImageView>(R.id.btnCloseDealPicker).setOnClickListener {
            dialog.dismiss()
        }

        FirebaseFirestore.getInstance().collection("deals")
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { result ->
                progress.visibility = View.GONE
                val today = Date()
                val availableDeals = mutableListOf<Pair<String, Deal>>() // docId to Deal

                for (doc in result) {
                    @Suppress("UNCHECKED_CAST")
                    val deal = Deal(
                        code = doc.getString("code") ?: "",
                        discountPercent = (doc.getLong("discountPercent") ?: 0L).toInt(),
                        startDate = doc.getString("startDate") ?: "",
                        endDate = doc.getString("endDate") ?: "",
                        isActive = doc.getBoolean("isActive") ?: true,
                        usedBy = (doc.get("usedBy") as? List<String>) ?: emptyList()
                    )
                    // Filter: within date range and not used by current user
                    try {
                        val start = sdf.parse(deal.startDate) ?: continue
                        val endCal = java.util.Calendar.getInstance()
                        endCal.time = sdf.parse(deal.endDate) ?: continue
                        endCal.set(java.util.Calendar.HOUR_OF_DAY, 23)
                        endCal.set(java.util.Calendar.MINUTE, 59)
                        endCal.set(java.util.Calendar.SECOND, 59)

                        if (today.before(start) || today.after(endCal.time)) continue
                        if (username in deal.usedBy) continue
                    } catch (_: Exception) { continue }

                    availableDeals.add(Pair(doc.id, deal))
                }

                if (availableDeals.isEmpty()) {
                    layoutEmpty.visibility = View.VISIBLE
                } else {
                    rv.adapter = DealPickerAdapter(availableDeals) { docId, deal ->
                        dialog.dismiss()
                        onSelected(docId, deal.code, deal.discountPercent)
                    }
                }
            }
            .addOnFailureListener { e ->
                progress.visibility = View.GONE
                Toast.makeText(this, "Lỗi tải mã deal: ${e.message}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }

        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        val selectedAddress = UserManager.getSelectedAddress()
        findViewById<TextView>(R.id.tvCheckoutAddress).text =
            if (selectedAddress.isNotEmpty()) selectedAddress else "Vui lòng thêm địa chỉ"
    }

    // ======== Adapter ========
    class DealPickerAdapter(
        private val deals: List<Pair<String, Deal>>,
        private val onPick: (String, Deal) -> Unit
    ) : RecyclerView.Adapter<DealPickerAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvDiscount: TextView = view.findViewById(R.id.tvPickerDiscount)
            val tvCode: TextView = view.findViewById(R.id.tvPickerCode)
            val tvDates: TextView = view.findViewById(R.id.tvPickerDates)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_deal_picker, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val (docId, deal) = deals[position]
            holder.tvDiscount.text = "-${deal.discountPercent}%"
            holder.tvCode.text = deal.code
            holder.tvDates.text = "HSD: ${deal.endDate}"
            holder.itemView.setOnClickListener { onPick(docId, deal) }
        }

        override fun getItemCount() = deals.size
    }
}
