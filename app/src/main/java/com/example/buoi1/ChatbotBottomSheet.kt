package com.example.buoi1

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.firebase.firestore.FirebaseFirestore
import java.text.Normalizer
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.min

class ChatbotBottomSheet(private val context: Context) {

    private data class BotProduct(
        val name: String,
        val brand: String,
        val price: Double,
        val discounted: Int,
        val stock: Long?
    )

    private val dialog = BottomSheetDialog(context)
    private val handler = Handler(Looper.getMainLooper())
    private val products = mutableListOf<BotProduct>()
    private val formatter = NumberFormat.getInstance(Locale("vi", "VN"))

    private lateinit var messagesContainer: LinearLayout
    private lateinit var suggestionsContainer: LinearLayout
    private lateinit var scrollView: NestedScrollView
    private lateinit var messageInput: EditText
    private var typingRow: View? = null

    fun show() {
        val view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_chatbot, null)
        dialog.setContentView(view)

        messagesContainer = view.findViewById(R.id.chatMessagesContainer)
        suggestionsContainer = view.findViewById(R.id.chatSuggestionsContainer)
        scrollView = view.findViewById(R.id.chatScrollView)
        messageInput = view.findViewById(R.id.edtChatMessage)

        view.findViewById<View>(R.id.btnCloseChat).setOnClickListener { dialog.dismiss() }
        view.findViewById<View>(R.id.btnSendChat).setOnClickListener { sendCurrentMessage() }
        messageInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendCurrentMessage()
                true
            } else {
                false
            }
        }

        setupSuggestions()
        loadProducts()
        addWelcomeMessage()

        dialog.setOnShowListener {
            val sheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?: return@setOnShowListener
            val targetHeight = min(
                (context.resources.displayMetrics.heightPixels * 0.78f).toInt(),
                680.dp
            )
            sheet.layoutParams = sheet.layoutParams.apply {
                height = targetHeight
            }
            BottomSheetBehavior.from(sheet).apply {
                state = BottomSheetBehavior.STATE_EXPANDED
                peekHeight = targetHeight
                skipCollapsed = false
                isDraggable = false
            }
        }

        dialog.setOnDismissListener {
            handler.removeCallbacksAndMessages(null)
        }
        dialog.show()
    }

    private fun addWelcomeMessage() {
        val firstName = UserManager.currentUser
            ?.fullName
            ?.trim()
            ?.substringBefore(" ")
            ?.takeIf { it.isNotBlank() }
        val greeting = if (firstName == null) {
            "Xin chào! Mình là Trợ lý Lirunghi Shop 🏸\nBạn cần mình hỗ trợ tìm sản phẩm, kiểm tra còn hàng hay tra cứu đơn hàng?"
        } else {
            "Xin chào $firstName! Mình là Trợ lý Lirunghi Shop 🏸\nBạn cần mình hỗ trợ tìm sản phẩm, kiểm tra còn hàng hay tra cứu đơn hàng?"
        }
        addMessage(greeting, isUser = false)
    }

    private fun setupSuggestions() {
        listOf(
            "Sản phẩm nào còn hàng?",
            "Gợi ý vợt cầu lông",
            "Có mã giảm giá không?",
            "Theo dõi đơn hàng"
        ).forEach { suggestion ->
            val chip = Chip(context).apply {
                text = suggestion
                isCheckable = false
                isClickable = true
                textSize = 13f
                setTextColor(ContextCompat.getColor(context, R.color.colorPrimary))
                chipBackgroundColor = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.chatBotBubble)
                )
                chipStrokeColor = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.colorButtonLight)
                )
                chipStrokeWidth = 1f
                setOnClickListener { sendMessage(suggestion) }
            }
            suggestionsContainer.addView(
                chip,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = 8.dp }
            )
        }
    }

    private fun loadProducts() {
        FirebaseFirestore.getInstance()
            .collection("products")
            .get()
            .addOnSuccessListener { result ->
                products.clear()
                result.forEach { document ->
                    products += BotProduct(
                        name = document.getString("name").orEmpty(),
                        brand = document.getString("brand").orEmpty(),
                        price = document.getDouble("price") ?: 0.0,
                        discounted = document.getLong("discounted")?.toInt() ?: 0,
                        stock = document.getLong("stock") ?: document.getLong("quantity")
                    )
                }
            }
    }

    private fun sendCurrentMessage() {
        val message = messageInput.text.toString().trim()
        if (message.isNotEmpty()) {
            sendMessage(message)
        }
    }

    private fun sendMessage(message: String) {
        addMessage(message, isUser = true)
        messageInput.text.clear()
        showTyping()

        handler.postDelayed({
            if (!dialog.isShowing) return@postDelayed
            hideTyping()
            answer(message)
        }, 650)
    }

    private fun answer(message: String) {
        val normalized = normalize(message)
        when {
            normalized.hasAny("xin chao", "chao ban", "hello", "hi ") ->
                addMessage("Xin chào bạn 👋 Hôm nay mình có thể giúp bạn chọn vợt, kiểm tra sản phẩm, deal hoặc đơn hàng.", false)

            normalized.hasAny("con hang", "het hang", "ton kho", "co san pham") ->
                answerProductQuestion(message, checkStock = true)

            normalized.hasAny("goi y", "tu van", "nen mua", "phu hop", "san pham", "vot nao", "gia bao nhieu") ->
                answerProductQuestion(message, checkStock = false)

            normalized.hasAny("ma giam", "khuyen mai", "voucher", "deal", "giam gia") ->
                addMessage(
                    "Các mã đang hoạt động sẽ xuất hiện ở mục “Chọn mã giảm giá” khi bạn thanh toán. " +
                        "Hệ thống tự lọc mã còn hạn và chưa được tài khoản của bạn sử dụng.",
                    false
                )

            normalized.hasAny("don hang", "theo doi", "giao hang", "trang thai don") ->
                addMessage(
                    "Bạn vào Tài khoản → Lịch sử đơn hàng → chọn đơn cần xem. " +
                        "Tại đó có trạng thái hiện tại, sản phẩm, địa chỉ và phương thức thanh toán.",
                    false
                )

            normalized.hasAny("bao hanh", "doi tra", "hong", "loi san pham") ->
                addMessage(
                    "Bạn vào Tài khoản → Bảo hành hoặc mở chi tiết đơn hàng, chọn sản phẩm và gửi yêu cầu. " +
                        "Admin sẽ tiếp nhận rồi cập nhật trạng thái xử lý trên ứng dụng.",
                    false
                )

            normalized.hasAny("thanh toan", "chuyen khoan", "cod", "qr") ->
                addMessage(
                    "Shop hỗ trợ thanh toán khi nhận hàng (COD) và chuyển khoản ngân hàng bằng mã QR. " +
                        "Bạn chọn phương thức mong muốn tại bước thanh toán.",
                    false
                )

            normalized.hasAny("dia chi", "nhan hang") ->
                addMessage(
                    "Tại màn hình thanh toán, bạn nhấn vào phần địa chỉ để thêm, sửa hoặc chọn địa chỉ nhận hàng.",
                    false
                )

            normalized.hasAny("gio hang", "them vao gio", "xoa gio") ->
                addMessage(
                    "Bạn có thể thêm sản phẩm từ trang chi tiết. Trong giỏ hàng, hãy chọn sản phẩm, tăng giảm số lượng rồi nhấn thanh toán.",
                    false
                )

            normalized.hasAny("cam on", "thank", "ok", "duoc roi") ->
                addMessage("Rất vui được hỗ trợ bạn 😊 Nếu cần thêm thông tin sản phẩm, cứ nhắn tên hoặc hãng cho mình nhé!", false)

            else ->
                addMessage(
                    "Mình chưa hiểu rõ câu hỏi này. Bạn có thể hỏi theo mẫu:\n" +
                        "• Vợt Yonex còn hàng không?\n" +
                        "• Gợi ý cho mình một cây vợt giá tốt\n" +
                        "• Cách xem trạng thái đơn hàng\n" +
                        "• Shop có mã giảm giá không?",
                    false
                )
        }
    }

    private fun answerProductQuestion(message: String, checkStock: Boolean) {
        if (products.isEmpty()) {
            addMessage(
                "Mình chưa tải được danh sách sản phẩm. Bạn kiểm tra kết nối mạng rồi thử hỏi lại nhé.",
                false
            )
            return
        }

        val matches = findMatchingProducts(message)
        if (matches.isEmpty()) {
            addMessage(
                "Mình chưa tìm thấy sản phẩm phù hợp. Bạn thử nhập tên hoặc hãng cụ thể như Yonex, Victor, Lining, Mizuno nhé.",
                false
            )
            return
        }

        val reply = if (checkStock) {
            buildString {
                append("Mình kiểm tra được:\n")
                matches.take(3).forEach { product ->
                    append("• ${product.name}: ")
                    when {
                        product.stock == null -> append("đang có trên cửa hàng")
                        product.stock > 0 -> append("còn ${product.stock} sản phẩm")
                        else -> append("hiện đã hết hàng")
                    }
                    append("\n")
                }
                append("Bạn có thể mở tab Sản phẩm để xem chi tiết.")
            }
        } else {
            buildString {
                append("Bạn có thể tham khảo:\n")
                matches.take(3).forEach { product ->
                    val activePrice = product.price * (1 - product.discounted / 100.0)
                    append("• ${product.name} — ${formatter.format(activePrice)}đ")
                    if (product.discounted > 0) append(" (giảm ${product.discounted}%)")
                    append("\n")
                }
                append("Nhấn tab Sản phẩm để xem thông số và đặt mua nhé.")
            }
        }
        addMessage(reply.trim(), false)
    }

    private fun findMatchingProducts(message: String): List<BotProduct> {
        val normalizedMessage = normalize(message)
        val brands = listOf("yonex", "victor", "lining", "li ning", "mizuno", "gosen")
        val requestedBrand = brands.firstOrNull { normalizedMessage.contains(it) }

        val ignoredWords = setOf(
            "san", "pham", "vot", "cau", "long", "con", "hang", "khong", "gia", "bao",
            "nhieu", "minh", "can", "tim", "kiem", "goi", "y", "tu", "van", "nao", "co",
            "cho", "mua", "mot", "cay", "the", "shop", "kiem", "tra", "giup", "voi"
        )
        val keywords = normalizedMessage
            .split(Regex("\\s+"))
            .filter { it.length >= 3 && it !in ignoredWords && it !in brands }

        val scored = products.map { product ->
            val searchable = normalize("${product.name} ${product.brand}")
            var score = keywords.count { searchable.contains(it) }
            if (requestedBrand != null && searchable.contains(requestedBrand)) score += 4
            product to score
        }

        val hasSpecificQuery = requestedBrand != null || keywords.isNotEmpty()
        val candidates = if (hasSpecificQuery) {
            scored.filter { it.second > 0 }
        } else {
            scored
        }

        return candidates
            .sortedWith(
                compareByDescending<Pair<BotProduct, Int>> { it.second }
                    .thenBy { it.first.price * (1 - it.first.discounted / 100.0) }
            )
            .map { it.first }
    }

    private fun addMessage(text: String, isUser: Boolean): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = if (isUser) Gravity.END else Gravity.START or Gravity.BOTTOM
            setPadding(0, 0, 0, 10.dp)
        }

        if (!isUser) {
            row.addView(
                ImageView(context).apply {
                    setImageResource(R.drawable.ic_chat_bubble)
                    setColorFilter(Color.WHITE)
                    setPadding(4.dp, 4.dp, 4.dp, 4.dp)
                    background = ContextCompat.getDrawable(context, R.drawable.bg_chat_bot_avatar)
                },
                LinearLayout.LayoutParams(34.dp, 34.dp).apply { marginEnd = 8.dp }
            )
        }

        val bubble = TextView(context).apply {
            this.text = text
            textSize = 14f
            setTextColor(
                ContextCompat.getColor(
                    context,
                    if (isUser) R.color.white else R.color.colorText
                )
            )
            setLineSpacing(0f, 1.08f)
            setPadding(14.dp, 10.dp, 14.dp, 10.dp)
            background = ContextCompat.getDrawable(
                context,
                if (isUser) R.drawable.bg_chat_user_bubble else R.drawable.bg_chat_bot_bubble
            )
            maxWidth = (context.resources.displayMetrics.widthPixels * 0.78f).toInt()
        }
        row.addView(
            bubble,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        messagesContainer.addView(
            row,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        scrollToBottom()
        return row
    }

    private fun showTyping() {
        hideTyping()
        typingRow = addMessage("Đang nhập…", isUser = false)
    }

    private fun hideTyping() {
        typingRow?.let { messagesContainer.removeView(it) }
        typingRow = null
    }

    private fun scrollToBottom() {
        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
    }

    private fun normalize(value: String): String {
        return Normalizer.normalize(value.lowercase(Locale("vi", "VN")), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .replace('đ', 'd')
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun String.hasAny(vararg values: String): Boolean = values.any { contains(it) }

    private val Int.dp: Int
        get() = (this * context.resources.displayMetrics.density).toInt()
}
