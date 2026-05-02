package com.example.buoi1

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class AdminWarrantyListActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val allWarranties = mutableListOf<Warranty>()
    private val filteredWarranties = mutableListOf<Warranty>()
    private lateinit var adapter: AdminWarrantyAdapter
    private var selectedStatus = "Tất cả"
    private var activeChip: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_warranty_list)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.topBarAdminWarranty)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, bars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        findViewById<ImageView>(R.id.btnBackAdminWarranty).setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvAdminWarranty)
        adapter = AdminWarrantyAdapter(filteredWarranties) { warranty -> showRespondDialog(warranty) }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        setupChips()
        setupSearch()
        fetchWarranties()
    }

    override fun onResume() {
        super.onResume()
        fetchWarranties()
    }

    private fun setupChips() {
        val chips = listOf<TextView>(
            findViewById(R.id.awChipAll),
            findViewById(R.id.awChipActive),
            findViewById(R.id.awChipClaim),
            findViewById(R.id.awChipDone),
            findViewById(R.id.awChipExpired)
        )
        val statuses = listOf("Tất cả", "Đang bảo hành", "Chờ xử lý", "Đã xử lý", "Hết hạn")

        setActiveChip(chips[0])
        for (i in chips.indices) {
            chips[i].setOnClickListener {
                selectedStatus = statuses[i]
                setActiveChip(chips[i])
                applyFilters()
            }
        }
    }

    private fun setActiveChip(chip: TextView) {
        activeChip?.apply {
            setTextColor(android.graphics.Color.parseColor("#666666"))
            setBackgroundResource(R.drawable.bg_search_bar)
            backgroundTintList = null
        }
        chip.setTextColor(android.graphics.Color.WHITE)
        chip.setBackgroundResource(R.drawable.bg_button_rounded)
        chip.backgroundTintList = android.content.res.ColorStateList.valueOf(
            android.graphics.Color.parseColor("#E64A19")
        )
        activeChip = chip
    }

    private fun setupSearch() {
        findViewById<EditText>(R.id.edtSearchWarranty).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { applyFilters() }
        })
    }

    private fun fetchWarranties() {
        val progress = findViewById<ProgressBar>(R.id.progressAdminWarranty)
        progress.visibility = View.VISIBLE

        db.collection("warranties").get()
            .addOnSuccessListener { result ->
                progress.visibility = View.GONE
                allWarranties.clear()
                for (doc in result) {
                    allWarranties.add(
                        Warranty(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            orderId = doc.getString("orderId") ?: "",
                            productName = doc.getString("productName") ?: "",
                            purchaseDate = doc.getString("purchaseDate") ?: "",
                            expiryDate = doc.getString("expiryDate") ?: "",
                            status = doc.getString("status") ?: "Đang bảo hành",
                            claimNote = doc.getString("claimNote") ?: "",
                            adminNote = doc.getString("adminNote") ?: ""
                        )
                    )
                }
                applyFilters()
            }
            .addOnFailureListener {
                progress.visibility = View.GONE
                Toast.makeText(this, "Lỗi tải dữ liệu bảo hành", Toast.LENGTH_SHORT).show()
            }
    }

    private fun applyFilters() {
        val query = findViewById<EditText>(R.id.edtSearchWarranty).text.toString().lowercase().trim()

        filteredWarranties.clear()
        filteredWarranties.addAll(allWarranties.filter { w ->
            val matchesSearch = query.isEmpty() ||
                    w.productName.lowercase().contains(query) ||
                    w.orderId.lowercase().contains(query) ||
                    w.userId.lowercase().contains(query)
            val matchesStatus = selectedStatus == "Tất cả" || w.status == selectedStatus
            matchesSearch && matchesStatus
        })

        adapter.notifyDataSetChanged()

        val tvCount = findViewById<TextView>(R.id.tvWarrantyCount)
        tvCount.text = "${filteredWarranties.size} yêu cầu"

        val layoutEmpty = findViewById<LinearLayout>(R.id.layoutAdminWarrantyEmpty)
        val rv = findViewById<RecyclerView>(R.id.rvAdminWarranty)
        if (filteredWarranties.isEmpty()) {
            layoutEmpty.visibility = View.VISIBLE
            rv.visibility = View.GONE
        } else {
            layoutEmpty.visibility = View.GONE
            rv.visibility = View.VISIBLE
        }
    }

    private fun showRespondDialog(warranty: Warranty) {
        val input = EditText(this).apply {
            hint = "Nhập phản hồi hoặc hướng xử lý cho khách..."
            minLines = 3
            setPadding(40, 30, 40, 10)
            if (warranty.adminNote.isNotEmpty()) setText(warranty.adminNote)
        }

        AlertDialog.Builder(this)
            .setTitle("Phản hồi bảo hành")
            .setMessage("Khách: ${warranty.userId}\nSản phẩm: ${warranty.productName}\nLỗi: ${warranty.claimNote}")
            .setView(input)
            .setPositiveButton("Xác nhận đã xử lý") { _, _ ->
                val note = input.text.toString().trim()
                if (note.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập phản hồi", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                db.collection("warranties").document(warranty.id)
                    .update(mapOf("status" to "Đã xử lý", "adminNote" to note))
                    .addOnSuccessListener {
                        Toast.makeText(this, "Đã xử lý yêu cầu bảo hành", Toast.LENGTH_SHORT).show()
                        fetchWarranties()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Lỗi cập nhật", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNeutralButton("Lưu phản hồi") { _, _ ->
                val note = input.text.toString().trim()
                if (note.isEmpty()) return@setNeutralButton
                db.collection("warranties").document(warranty.id)
                    .update("adminNote", note)
                    .addOnSuccessListener { fetchWarranties() }
            }
            .setNegativeButton("Huỷ", null)
            .show()
    }

    // ======== Adapter ========
    class AdminWarrantyAdapter(
        private val list: List<Warranty>,
        private val onRespond: (Warranty) -> Unit
    ) : RecyclerView.Adapter<AdminWarrantyAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvProduct: TextView = view.findViewById(R.id.tvAdminWProduct)
            val tvStatus: TextView = view.findViewById(R.id.tvAdminWStatus)
            val tvUser: TextView = view.findViewById(R.id.tvAdminWUser)
            val tvOrderId: TextView = view.findViewById(R.id.tvAdminWOrderId)
            val tvPurchase: TextView = view.findViewById(R.id.tvAdminWPurchase)
            val tvExpiry: TextView = view.findViewById(R.id.tvAdminWExpiry)
            val layoutClaim: LinearLayout = view.findViewById(R.id.layoutAdminWClaim)
            val tvClaimNote: TextView = view.findViewById(R.id.tvAdminWClaimNote)
            val layoutNote: LinearLayout = view.findViewById(R.id.layoutAdminWNote)
            val tvAdminNote: TextView = view.findViewById(R.id.tvAdminWNote)
            val btnRespond: Button = view.findViewById(R.id.btnRespondWarranty)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_admin_warranty, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val w = list[position]
            holder.tvProduct.text = w.productName
            holder.tvUser.text = w.userId
            holder.tvOrderId.text = "#${w.orderId}"
            holder.tvPurchase.text = w.purchaseDate
            holder.tvExpiry.text = w.expiryDate

            val (statusColor, statusBg) = when (w.status) {
                "Đang bảo hành" -> "#2E7D32" to "#E8F5E9"
                "Chờ xử lý" -> "#E65100" to "#FFF3E0"
                "Đã xử lý" -> "#1565C0" to "#E3F2FD"
                "Hết hạn" -> "#999999" to "#F5F5F5"
                else -> "#999999" to "#F5F5F5"
            }
            holder.tvStatus.text = w.status
            holder.tvStatus.setTextColor(android.graphics.Color.parseColor(statusColor))
            holder.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor(statusBg)
            )

            if (w.claimNote.isNotEmpty()) {
                holder.layoutClaim.visibility = View.VISIBLE
                holder.tvClaimNote.text = w.claimNote
            } else {
                holder.layoutClaim.visibility = View.GONE
            }

            if (w.adminNote.isNotEmpty()) {
                holder.layoutNote.visibility = View.VISIBLE
                holder.tvAdminNote.text = w.adminNote
            } else {
                holder.layoutNote.visibility = View.GONE
            }

            if (w.status == "Chờ xử lý") {
                holder.btnRespond.visibility = View.VISIBLE
                holder.btnRespond.setOnClickListener { onRespond(w) }
            } else {
                holder.btnRespond.visibility = View.GONE
            }
        }

        override fun getItemCount() = list.size
    }
}
