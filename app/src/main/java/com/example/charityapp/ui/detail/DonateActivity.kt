package com.example.charityapp.ui.detail

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.charityapp.Api.CreateOrder
import com.example.charityapp.databinding.ActivityDonateBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.zalopay.sdk.ZaloPayError
import vn.zalopay.sdk.ZaloPaySDK
import vn.zalopay.sdk.listeners.PayOrderListener

class DonateActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDonateBinding
    private var campaignId: String? = null
    private var campaignTitle: String? = null
    private var selectedAmount: Long = 0
    private var finalAmount: Long = 0

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDonateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Nhận dữ liệu truyền từ CampaignDetailActivity
        campaignId = intent.getStringExtra("CAMPAIGN_ID")
        campaignTitle = intent.getStringExtra("CAMPAIGN_TITLE")

        if (!campaignTitle.isNullOrEmpty()) {
            binding.tvCampaignTitle.text = campaignTitle
        } else {
            binding.tvCampaignTitle.text = "Chiến dịch từ thiện"
        }

        // 2. Xử lý sự kiện chọn nhanh số tiền qua các Chip
        binding.chip50k.setOnClickListener { selectAmount(50000) }
        binding.chip100k.setOnClickListener { selectAmount(100000) }
        binding.chip200k.setOnClickListener { selectAmount(200000) }
        binding.chip500k.setOnClickListener { selectAmount(500000) }

        // 3. Xử lý sự kiện bấm nút "Tiếp tục thanh toán"
        binding.btnProceedPayment.setOnClickListener {
            val inputStr = binding.etAmount.text.toString().trim()

            finalAmount = inputStr.toLongOrNull() ?: selectedAmount
            val message = binding.etMessage.text.toString().trim()

            if (finalAmount < 1000) {
                Toast.makeText(this, "Vui lòng nhập số tiền hợp lệ (Tối thiểu 1.000đ)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Gọi hàm tạo đơn hàng và mở ZaloPay SDK
            requestZaloPayPayment(finalAmount, message)
        }
    }

    private fun selectAmount(amount: Long) {
        selectedAmount = amount
        binding.etAmount.setText(amount.toString())
    }

    private fun requestZaloPayPayment(amount: Long, message: String) {
        Toast.makeText(this, "Đang khởi tạo giao dịch...", Toast.LENGTH_SHORT).show()

        // Sử dụng Coroutine chạy ngầm để gọi API tạo đơn hàng, tránh nghẽn luồng chính
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val orderApi = CreateOrder()
                val data = orderApi.createOrder(amount.toString())

                if (data != null && data.optInt("returncode") == 1) {
                    val zpTransToken = data.optString("zptranstoken")

                    withContext(Dispatchers.Main) {
                        ZaloPaySDK.getInstance().payOrder(
                            this@DonateActivity,
                            zpTransToken,
                            "demozpdk://app",
                            object : PayOrderListener {
                                override fun onPaymentSucceeded(transactionId: String, transToken: String, appTransID: String) {
                                    Toast.makeText(this@DonateActivity, "Thanh toán thành công!", Toast.LENGTH_LONG).show()

                                    // ⚡ 1. CẬP NHẬT TIẾN ĐỘ VÀO FIRESTORE NGAY KHI THANH TOÁN THÀNH CÔNG
                                    if (!campaignId.isNullOrEmpty()) {
                                        val campaignRef = db.collection("campaigns").document(campaignId!!)

                                        // Cộng dồn tiền vào tổng quỹ hiện tại của chiến dịch
                                        campaignRef.update("currentAmount", FieldValue.increment(finalAmount.toDouble()))

                                        // ⚡ 2. LƯU LỊCH SỬ VÀO COLLECTION "donations" ĐỂ ADMIN THỐNG KÊ
                                        val donationData = hashMapOf(
                                            "campaignId" to campaignId,
                                            "campaignTitle" to (campaignTitle ?: "Chiến dịch từ thiện"),
                                            "userName" to (auth.currentUser?.displayName ?: "Nhà hảo tâm"),
                                            "amount" to finalAmount.toDouble(),
                                            "timestamp" to com.google.firebase.Timestamp.now()
                                        )
                                        db.collection("donations").add(donationData)
                                    }

                                    // ⚡ 3. CHUYỂN SANG MÀN HÌNH THÀNH CÔNG VÀ TRUYỀN ĐẦY ĐỦ TIÊU ĐỀ
                                    val successIntent = Intent(this@DonateActivity, DonationSuccessActivity::class.java).apply {
                                        putExtra("TRANSACTION_ID", transactionId)
                                        putExtra("AMOUNT", finalAmount)
                                        putExtra("CAMPAIGN_ID", campaignId)
                                        putExtra("CAMPAIGN_TITLE", campaignTitle ?: "Chiến dịch từ thiện")
                                    }
                                    startActivity(successIntent)
                                    finish()
                                }

                                override fun onPaymentCanceled(transToken: String, appTransID: String) {
                                    Toast.makeText(this@DonateActivity, "Đã hủy thanh toán", Toast.LENGTH_SHORT).show()
                                    finish() // Đóng Activity khi hủy hoặc bấm Đóng trên ZaloPay
                                }

                                override fun onPaymentError(zaloPayError: ZaloPayError, transToken: String, appTransID: String) {
                                    Toast.makeText(this@DonateActivity, "Lỗi thanh toán: $zaloPayError", Toast.LENGTH_LONG).show()
                                    finish() // Đóng Activity khi xảy ra lỗi thanh toán
                                }
                            }
                        )
                    }
                } else {
                    val returnMessage = data?.optString("return_message") ?: "Không thể tạo đơn hàng"
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@DonateActivity, "Lỗi ZaloPay: $returnMessage", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("DONATE_EXCEPTION", "Lỗi ngoại lệ: ", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DonateActivity, "Lỗi kết nối: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // ⚡ QUAN TRỌNG: Cập nhật hàm này để nhận kết quả trả về từ scheme deep-link khi bấm "Đóng" hoặc "Chi tiết giao dịch"
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        ZaloPaySDK.getInstance().onResult(intent)
    }
}