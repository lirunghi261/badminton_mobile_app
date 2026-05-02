package com.example.buoi1

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AdminDealListActivity : AppCompatActivity() {

    data class DealWithId(val docId: String, val deal: Deal)

    private val dealList = mutableListOf<DealWithId>()
    private val filteredList = mutableListOf<DealWithId>()
    private lateinit var adapter: AdminDealAdapter
    private lateinit var rvDeals: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvDealCount: TextView
    private val db = FirebaseFirestore.getInstance()
    private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_deal_list)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.topBarAdminDeals)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, bars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        findViewById<ImageView>(R.id.btnBackDeals).setOnClickListener { finish() }

        rvDeals = findViewById(R.id.rvAdminDeals)
        layoutEmpty = findViewById(R.id.layoutDealEmpty)
        progressBar = findViewById(R.id.progressBarDeals)
        tvDealCount = findViewById(R.id.tvDealCount)

        adapter = AdminDealAdapter(
            filteredList,
            onEditClick = { item -> showEditDialog(item) },
            onDeleteClick = { item -> confirmDelete(item) }
        )
        rvDeals.layoutManager = LinearLayoutManager(this)
        rvDeals.adapter = adapter

        findViewById<EditText>(R.id.edtSearchDeal).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { applyFilter() }
        })

        findViewById<FloatingActionButton>(R.id.fabAddDeal).setOnClickListener {
            showAddDialog()
        }

        fetchDeals()
    }

    override fun onResume() {
        super.onResume()
        fetchDeals()
    }

    private fun fetchDeals() {
        progressBar.visibility = View.VISIBLE
        rvDeals.visibility = View.GONE
        layoutEmpty.visibility = View.GONE

        db.collection("deals").get()
            .addOnSuccessListener { result ->
                progressBar.visibility = View.GONE
                dealList.clear()
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
                    dealList.add(DealWithId(doc.id, deal))
                }
                applyFilter()
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                layoutEmpty.visibility = View.VISIBLE
                Toast.makeText(this, "Lỗi tải dữ liệu: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun applyFilter() {
        val query = findViewById<EditText>(R.id.edtSearchDeal).text.toString().uppercase().trim()
        filteredList.clear()
        filteredList.addAll(dealList.filter { item ->
            query.isEmpty() || item.deal.code.uppercase().contains(query)
        })
        adapter.notifyDataSetChanged()
        tvDealCount.text = "${filteredList.size} mã deal"
        rvDeals.visibility = if (filteredList.isEmpty()) View.GONE else View.VISIBLE
        layoutEmpty.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showAddDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_deal_edit, null)
        setupDatePickers(dialogView)

        AlertDialog.Builder(this)
            .setTitle("Thêm mã deal")
            .setView(dialogView)
            .setPositiveButton("Thêm") { _, _ ->
                val deal = readDealFromDialog(dialogView) ?: return@setPositiveButton
                checkCodeExists(deal.code) { exists ->
                    if (exists) {
                        Toast.makeText(this, "Mã deal đã tồn tại", Toast.LENGTH_SHORT).show()
                    } else {
                        saveDeal(null, deal)
                    }
                }
            }
            .setNegativeButton("Huỷ", null)
            .show()
    }

    private fun showEditDialog(item: DealWithId) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_deal_edit, null)
        setupDatePickers(dialogView)

        dialogView.findViewById<EditText>(R.id.edtDealCode).setText(item.deal.code)
        dialogView.findViewById<EditText>(R.id.edtDealDiscount).setText(item.deal.discountPercent.toString())
        dialogView.findViewById<TextView>(R.id.tvDealStartDate).text = item.deal.startDate
        dialogView.findViewById<TextView>(R.id.tvDealEndDate).text = item.deal.endDate
        dialogView.findViewById<Switch>(R.id.switchDealActive).isChecked = item.deal.isActive

        AlertDialog.Builder(this)
            .setTitle("Sửa mã deal")
            .setView(dialogView)
            .setPositiveButton("Lưu") { _, _ ->
                val deal = readDealFromDialog(dialogView) ?: return@setPositiveButton
                // If code changed, check uniqueness
                if (deal.code != item.deal.code) {
                    checkCodeExists(deal.code) { exists ->
                        if (exists) {
                            Toast.makeText(this, "Mã deal đã tồn tại", Toast.LENGTH_SHORT).show()
                        } else {
                            saveDeal(item.docId, deal.copy(usedBy = item.deal.usedBy))
                        }
                    }
                } else {
                    saveDeal(item.docId, deal.copy(usedBy = item.deal.usedBy))
                }
            }
            .setNegativeButton("Huỷ", null)
            .show()
    }

    private fun setupDatePickers(view: View) {
        val tvStart = view.findViewById<TextView>(R.id.tvDealStartDate)
        val tvEnd = view.findViewById<TextView>(R.id.tvDealEndDate)

        view.findViewById<View>(R.id.btnPickStartDate).setOnClickListener {
            showDatePicker(tvStart)
        }
        view.findViewById<View>(R.id.btnPickEndDate).setOnClickListener {
            showDatePicker(tvEnd)
        }
    }

    private fun showDatePicker(target: TextView) {
        val cal = Calendar.getInstance()
        val existing = target.text.toString()
        if (existing.isNotEmpty() && existing != "Chọn ngày") {
            try {
                val d = sdf.parse(existing)
                if (d != null) cal.time = d
            } catch (_: Exception) {}
        }
        DatePickerDialog(this, { _, y, m, d ->
            cal.set(y, m, d)
            target.text = sdf.format(cal.time)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun readDealFromDialog(view: View): Deal? {
        val code = view.findViewById<EditText>(R.id.edtDealCode).text.toString().trim().uppercase()
        val discountStr = view.findViewById<EditText>(R.id.edtDealDiscount).text.toString().trim()
        val startDate = view.findViewById<TextView>(R.id.tvDealStartDate).text.toString()
        val endDate = view.findViewById<TextView>(R.id.tvDealEndDate).text.toString()
        val isActive = view.findViewById<Switch>(R.id.switchDealActive).isChecked

        if (code.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập mã deal", Toast.LENGTH_SHORT).show()
            return null
        }
        val discount = discountStr.toIntOrNull()
        if (discount == null || discount !in 1..100) {
            Toast.makeText(this, "Phần trăm giảm phải từ 1 đến 100", Toast.LENGTH_SHORT).show()
            return null
        }
        if (startDate == "Chọn ngày" || startDate.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ngày bắt đầu", Toast.LENGTH_SHORT).show()
            return null
        }
        if (endDate == "Chọn ngày" || endDate.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ngày kết thúc", Toast.LENGTH_SHORT).show()
            return null
        }
        // Validate end >= start
        try {
            val start = sdf.parse(startDate)!!
            val end = sdf.parse(endDate)!!
            if (end.before(start)) {
                Toast.makeText(this, "Ngày kết thúc phải sau ngày bắt đầu", Toast.LENGTH_SHORT).show()
                return null
            }
        } catch (_: Exception) {}

        return Deal(code, discount, startDate, endDate, isActive)
    }

    private fun checkCodeExists(code: String, callback: (Boolean) -> Unit) {
        db.collection("deals").whereEqualTo("code", code).get()
            .addOnSuccessListener { callback(!it.isEmpty) }
            .addOnFailureListener { callback(false) }
    }

    private fun saveDeal(docId: String?, deal: Deal) {
        val data = hashMapOf(
            "code" to deal.code,
            "discountPercent" to deal.discountPercent,
            "startDate" to deal.startDate,
            "endDate" to deal.endDate,
            "isActive" to deal.isActive,
            "usedBy" to deal.usedBy
        )
        val task = if (docId != null) {
            db.collection("deals").document(docId).set(data)
        } else {
            db.collection("deals").add(data).continueWith { }
        }
        task.addOnSuccessListener {
            Toast.makeText(this, if (docId != null) "Đã cập nhật mã deal" else "Đã thêm mã deal", Toast.LENGTH_SHORT).show()
            fetchDeals()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDelete(item: DealWithId) {
        AlertDialog.Builder(this)
            .setTitle("Xoá mã deal")
            .setMessage("Bạn có chắc muốn xoá mã \"${item.deal.code}\"?\nHành động này không thể hoàn tác.")
            .setPositiveButton("Xoá") { _, _ ->
                db.collection("deals").document(item.docId).delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Đã xoá mã deal", Toast.LENGTH_SHORT).show()
                        fetchDeals()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Huỷ", null)
            .show()
    }

    // ======== Adapter ========
    class AdminDealAdapter(
        private val deals: List<DealWithId>,
        private val onEditClick: (DealWithId) -> Unit,
        private val onDeleteClick: (DealWithId) -> Unit
    ) : RecyclerView.Adapter<AdminDealAdapter.ViewHolder>() {

        private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvCode: TextView = view.findViewById(R.id.tvDealCode)
            val tvDiscount: TextView = view.findViewById(R.id.tvDealDiscount)
            val tvDates: TextView = view.findViewById(R.id.tvDealDates)
            val tvUsed: TextView = view.findViewById(R.id.tvDealUsed)
            val tvStatus: TextView = view.findViewById(R.id.tvDealStatus)
            val btnEdit: ImageView = view.findViewById(R.id.btnEditDeal)
            val btnDelete: ImageView = view.findViewById(R.id.btnDeleteDeal)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_admin_deal, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = deals[position]
            val deal = item.deal
            holder.tvCode.text = deal.code
            holder.tvDiscount.text = "-${deal.discountPercent}%"
            holder.tvDates.text = "${deal.startDate} → ${deal.endDate}"
            holder.tvUsed.text = "Đã dùng: ${deal.usedBy.size} lần"

            val today = java.util.Date()
            var statusText: String
            var statusColor: String
            var statusBg: Int
            if (!deal.isActive) {
                statusText = "Đã tắt"
                statusColor = "#FFFFFF"
                statusBg = R.drawable.bg_role_user
            } else {
                try {
                    val start = sdf.parse(deal.startDate)!!
                    val end = sdf.parse(deal.endDate)!!
                    when {
                        today.before(start) -> {
                            statusText = "Chưa bắt đầu"
                            statusColor = "#FFFFFF"
                            statusBg = R.drawable.bg_role_admin
                        }
                        today.after(end) -> {
                            statusText = "Hết hạn"
                            statusColor = "#FFFFFF"
                            statusBg = R.drawable.bg_role_user
                        }
                        else -> {
                            statusText = "Đang hoạt động"
                            statusColor = "#FFFFFF"
                            statusBg = R.drawable.bg_role_user
                        }
                    }
                } catch (_: Exception) {
                    statusText = "Không rõ"
                    statusColor = "#FFFFFF"
                    statusBg = R.drawable.bg_role_user
                }
            }

            // Use bg_role_user (green) for active, bg_role_admin (orange-red) for others
            val bgResId = if (statusText == "Đang hoạt động") R.drawable.bg_deal_active
                          else if (statusText == "Hết hạn" || statusText == "Đã tắt") R.drawable.bg_deal_expired
                          else R.drawable.bg_deal_upcoming

            holder.tvStatus.text = statusText
            holder.tvStatus.setBackgroundResource(bgResId)

            holder.btnEdit.setOnClickListener { onEditClick(item) }
            holder.btnDelete.setOnClickListener { onDeleteClick(item) }
        }

        override fun getItemCount() = deals.size
    }
}
