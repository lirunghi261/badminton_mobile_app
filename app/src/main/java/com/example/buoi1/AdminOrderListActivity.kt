package com.example.buoi1

import android.content.Intent
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
import java.text.NumberFormat
import java.util.Locale

class AdminOrderListActivity : AppCompatActivity() {

    private val allOrders = mutableListOf<Order>()
    private val filteredOrders = mutableListOf<Order>()
    private lateinit var adapter: AdminOrderAdapter
    private lateinit var rvOrders: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvOrderCount: TextView

    private var selectedStatus = "Tất cả"
    private var filterBankOnly = false

    private val statusList = listOf("Chờ xác nhận", "Đang giao", "Thành công", "Đã huỷ")

    // Chip references
    private lateinit var chipAll: TextView
    private lateinit var chipPending: TextView
    private lateinit var chipShipping: TextView
    private lateinit var chipCompleted: TextView
    private lateinit var chipCancelled: TextView
    private lateinit var chipBankTransfer: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_order_list)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.topBarAdminOrders)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, bars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        findViewById<ImageView>(R.id.btnBackOrders).setOnClickListener { finish() }

        rvOrders = findViewById(R.id.rvAdminOrders)
        layoutEmpty = findViewById(R.id.layoutOrderEmpty)
        progressBar = findViewById(R.id.progressBarOrders)
        tvOrderCount = findViewById(R.id.tvOrderCount)

        // Setup RecyclerView
        adapter = AdminOrderAdapter(
            filteredOrders,
            onItemClick = { order ->
                val intent = Intent(this, AdminOrderDetailActivity::class.java)
                intent.putExtra("EXTRA_ORDER_ID", order.id)
                startActivity(intent)
            },
            onUpdateStatusClick = { order -> showUpdateStatusDialog(order) }
        )
        rvOrders.layoutManager = LinearLayoutManager(this)
        rvOrders.adapter = adapter

        // Search
        val edtSearch = findViewById<EditText>(R.id.edtSearchOrder)
        edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                applyFilters()
            }
        })

        // Setup filter chips
        chipAll = findViewById(R.id.chipAll)
        chipPending = findViewById(R.id.chipPending)
        chipShipping = findViewById(R.id.chipShipping)
        chipCompleted = findViewById(R.id.chipCompleted)
        chipCancelled = findViewById(R.id.chipCancelled)
        chipBankTransfer = findViewById(R.id.chipBankTransfer)

        val statusChips = listOf(chipAll, chipPending, chipShipping, chipCompleted, chipCancelled)
        val chipStatuses = listOf("Tất cả", "Chờ xác nhận", "Đang giao", "Thành công", "Đã huỷ")

        for (i in statusChips.indices) {
            statusChips[i].setOnClickListener {
                selectedStatus = chipStatuses[i]
                filterBankOnly = false
                updateChipUI(statusChips + listOf(chipBankTransfer), i)
                applyFilters()
            }
        }

        chipBankTransfer.setOnClickListener {
            filterBankOnly = !filterBankOnly
            if (filterBankOnly) {
                selectedStatus = "Tất cả"
                updateChipUI(statusChips + listOf(chipBankTransfer), statusChips.size)
            } else {
                updateChipUI(statusChips + listOf(chipBankTransfer), 0)
            }
            applyFilters()
        }

        fetchOrders()
    }

    override fun onResume() {
        super.onResume()
        fetchOrders()
    }

    private fun updateChipUI(chips: List<TextView>, selectedIndex: Int) {
        for (i in chips.indices) {
            if (i == selectedIndex) {
                chips[i].setBackgroundResource(R.drawable.bg_button_rounded)
                chips[i].backgroundTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#E64A19")
                )
                chips[i].setTextColor(android.graphics.Color.WHITE)
            } else {
                chips[i].setBackgroundResource(R.drawable.bg_search_bar)
                chips[i].backgroundTintList = null
                chips[i].setTextColor(android.graphics.Color.parseColor("#666666"))
            }
        }
    }

    private fun fetchOrders() {
        progressBar.visibility = View.VISIBLE
        rvOrders.visibility = View.GONE
        layoutEmpty.visibility = View.GONE

        OrderManager.fetchOrders(
            onSuccess = { orders ->
                progressBar.visibility = View.GONE
                allOrders.clear()
                allOrders.addAll(orders)
                applyFilters()
            },
            onFailure = { e ->
                progressBar.visibility = View.GONE
                layoutEmpty.visibility = View.VISIBLE
                Toast.makeText(this, "Lỗi tải đơn hàng: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun applyFilters() {
        val edtSearch = findViewById<EditText>(R.id.edtSearchOrder)
        val query = edtSearch.text.toString().lowercase().trim()

        filteredOrders.clear()
        filteredOrders.addAll(allOrders.filter { order ->
            val matchesSearch = query.isEmpty() ||
                    order.id.lowercase().contains(query) ||
                    order.items.any { it.product.name.lowercase().contains(query) }

            val matchesStatus = selectedStatus == "Tất cả" || order.status == selectedStatus

            val matchesPayment = !filterBankOnly || order.paymentMethod == "Chuyển khoản ngân hàng"

            matchesSearch && matchesStatus && matchesPayment
        })

        adapter.notifyDataSetChanged()
        tvOrderCount.text = "${filteredOrders.size} đơn hàng"

        if (filteredOrders.isEmpty()) {
            rvOrders.visibility = View.GONE
            layoutEmpty.visibility = View.VISIBLE
        } else {
            rvOrders.visibility = View.VISIBLE
            layoutEmpty.visibility = View.GONE
        }
    }

    private fun showUpdateStatusDialog(order: Order) {
        val statusOptions = statusList.toTypedArray()
        val currentIndex = statusList.indexOf(order.status).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Cập nhật trạng thái\nĐơn #${order.id}")
            .setSingleChoiceItems(statusOptions, currentIndex) { dialog, which ->
                val newStatus = statusList[which]
                if (newStatus != order.status) {
                    OrderManager.updateOrderStatus(order.id, newStatus,
                        onSuccess = {
                            // Update local list
                            val index = allOrders.indexOfFirst { it.id == order.id }
                            if (index != -1) {
                                allOrders[index] = allOrders[index].copy(status = newStatus)
                            }
                            applyFilters()
                            Toast.makeText(this, "Đã cập nhật trạng thái thành \"$newStatus\"", Toast.LENGTH_SHORT).show()
                        },
                        onFailure = { e ->
                            Toast.makeText(this, "Lỗi cập nhật: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                dialog.dismiss()
            }
            .setNegativeButton("Huỷ", null)
            .show()
    }

    // ======== RecyclerView Adapter ========
    class AdminOrderAdapter(
        private val orders: List<Order>,
        private val onItemClick: (Order) -> Unit,
        private val onUpdateStatusClick: (Order) -> Unit
    ) : RecyclerView.Adapter<AdminOrderAdapter.ViewHolder>() {

        private val formatter = NumberFormat.getInstance(Locale("vi", "VN"))

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvOrderId: TextView = view.findViewById(R.id.tvAdminOrderId)
            val tvOrderStatus: TextView = view.findViewById(R.id.tvAdminOrderStatus)
            val tvOrderDate: TextView = view.findViewById(R.id.tvAdminOrderDate)
            val tvOrderItems: TextView = view.findViewById(R.id.tvAdminOrderItems)
            val tvOrderAddress: TextView = view.findViewById(R.id.tvAdminOrderAddress)
            val tvOrderPayment: TextView = view.findViewById(R.id.tvAdminOrderPayment)
            val tvOrderTotal: TextView = view.findViewById(R.id.tvAdminOrderTotal)
            val btnUpdateStatus: Button = view.findViewById(R.id.btnUpdateStatus)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_admin_order, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val order = orders[position]

            holder.tvOrderId.text = "Đơn: #${order.id}"
            holder.tvOrderDate.text = order.date
            holder.tvOrderAddress.text = order.address
            holder.tvOrderPayment.text = order.paymentMethod
            holder.tvOrderTotal.text = "đ${formatter.format(order.totalAmount)}"

            // Items preview
            val itemsPreview = order.items.joinToString(", ") { "${it.product.name} x${it.quantity}" }
            holder.tvOrderItems.text = itemsPreview

            // Status badge styling
            holder.tvOrderStatus.text = order.status
            val statusColor = when (order.status) {
                "Chờ xác nhận" -> "#EF6C00"
                "Đang giao" -> "#1565C0"
                "Thành công" -> "#2E7D32"
                "Đã huỷ" -> "#999999"
                else -> "#E64A19"
            }
            holder.tvOrderStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor(statusColor)
            )

            holder.itemView.setOnClickListener { onItemClick(order) }
            holder.btnUpdateStatus.setOnClickListener { onUpdateStatusClick(order) }
        }

        override fun getItemCount() = orders.size
    }
}
