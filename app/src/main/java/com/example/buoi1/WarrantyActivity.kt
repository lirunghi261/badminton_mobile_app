package com.example.buoi1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
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

class WarrantyActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val allWarranties = mutableListOf<Warranty>()
    private lateinit var adapter: WarrantyAdapter
    private var activeChip: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_warranty)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.topBarWarranty)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, bars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnWarrantyBack).setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvWarranty)
        adapter = WarrantyAdapter(emptyList()) { warranty -> showClaimDialog(warranty) }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        setupChips()
        fetchWarranties()
    }

    private fun setupChips() {
        val chipAll = findViewById<TextView>(R.id.wChipAll)
        val chipActive = findViewById<TextView>(R.id.wChipActive)
        val chipClaim = findViewById<TextView>(R.id.wChipClaim)
        val chipDone = findViewById<TextView>(R.id.wChipDone)
        val chipExpired = findViewById<TextView>(R.id.wChipExpired)

        setActiveChip(chipAll)
        chipAll.setOnClickListener { setActiveChip(chipAll); renderList(null) }
        chipActive.setOnClickListener { setActiveChip(chipActive); renderList("Đang bảo hành") }
        chipClaim.setOnClickListener { setActiveChip(chipClaim); renderList("Chờ xử lý") }
        chipDone.setOnClickListener { setActiveChip(chipDone); renderList("Đã xử lý") }
        chipExpired.setOnClickListener { setActiveChip(chipExpired); renderList("Hết hạn") }
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

    private fun fetchWarranties() {
        val progress = findViewById<ProgressBar>(R.id.progressWarranty)
        val username = UserManager.currentUser?.username ?: return

        progress.visibility = View.VISIBLE

        db.collection("warranties")
            .whereEqualTo("userId", username)
            .get()
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
                renderList(null)
            }
            .addOnFailureListener {
                progress.visibility = View.GONE
                Toast.makeText(this, "Lỗi tải bảo hành", Toast.LENGTH_SHORT).show()
            }
    }

    private fun renderList(statusFilter: String?) {
        val layoutEmpty = findViewById<LinearLayout>(R.id.layoutWarrantyEmpty)
        val rv = findViewById<RecyclerView>(R.id.rvWarranty)

        val list = if (statusFilter == null) allWarranties.toList()
        else allWarranties.filter { it.status == statusFilter }

        if (list.isEmpty()) {
            layoutEmpty.visibility = View.VISIBLE
            rv.visibility = View.GONE
        } else {
            layoutEmpty.visibility = View.GONE
            rv.visibility = View.VISIBLE
        }
        adapter.updateData(list)
    }

    private fun showClaimDialog(warranty: Warranty) {
        val input = EditText(this).apply {
            hint = "Mô tả lỗi hoặc vấn đề cần bảo hành..."
            minLines = 3
            setPadding(40, 30, 40, 10)
        }
        AlertDialog.Builder(this)
            .setTitle("Gửi yêu cầu bảo hành")
            .setMessage("Sản phẩm: ${warranty.productName}")
            .setView(input)
            .setPositiveButton("Gửi") { _, _ ->
                val note = input.text.toString().trim()
                if (note.isEmpty()) {
                    Toast.makeText(this, "Vui lòng mô tả lỗi", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                db.collection("warranties").document(warranty.id)
                    .update(mapOf("status" to "Chờ xử lý", "claimNote" to note))
                    .addOnSuccessListener {
                        Toast.makeText(this, "Đã gửi yêu cầu bảo hành", Toast.LENGTH_SHORT).show()
                        fetchWarranties()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Lỗi gửi yêu cầu", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Huỷ", null)
            .show()
    }

    // ======== Adapter ========
    class WarrantyAdapter(
        private var list: List<Warranty>,
        private val onClaim: (Warranty) -> Unit
    ) : RecyclerView.Adapter<WarrantyAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvProduct: TextView = view.findViewById(R.id.tvWarrantyProduct)
            val tvStatus: TextView = view.findViewById(R.id.tvWarrantyStatus)
            val tvOrderId: TextView = view.findViewById(R.id.tvWarrantyOrderId)
            val tvPurchase: TextView = view.findViewById(R.id.tvWarrantyPurchaseDate)
            val tvExpiry: TextView = view.findViewById(R.id.tvWarrantyExpiry)
            val layoutClaim: LinearLayout = view.findViewById(R.id.layoutClaimNote)
            val tvClaimNote: TextView = view.findViewById(R.id.tvClaimNote)
            val layoutAdmin: LinearLayout = view.findViewById(R.id.layoutAdminNote)
            val tvAdminNote: TextView = view.findViewById(R.id.tvAdminNote)
            val btnClaim: Button = view.findViewById(R.id.btnWarrantyClaim)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_warranty, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val w = list[position]
            holder.tvProduct.text = w.productName
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
                holder.layoutAdmin.visibility = View.VISIBLE
                holder.tvAdminNote.text = w.adminNote
            } else {
                holder.layoutAdmin.visibility = View.GONE
            }

            // Show claim button only if active warranty and no pending claim
            if (w.status == "Đang bảo hành") {
                holder.btnClaim.visibility = View.VISIBLE
                holder.btnClaim.setOnClickListener { onClaim(w) }
            } else {
                holder.btnClaim.visibility = View.GONE
            }
        }

        override fun getItemCount() = list.size

        fun updateData(newList: List<Warranty>) {
            list = newList
            notifyDataSetChanged()
        }
    }
}
