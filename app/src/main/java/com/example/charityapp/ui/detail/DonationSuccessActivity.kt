package com.example.charityapp.ui.detail

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.charityapp.MainActivity
import com.example.charityapp.databinding.ActivityDonationSuccessBinding

class DonationSuccessActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDonationSuccessBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDonationSuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Nhận dữ liệu truyền sang từ màn hình DonateActivity
        val campaignTitle = intent.getStringExtra("CAMPAIGN_TITLE") ?: "Không rõ"
        val amount = intent.getLongExtra("AMOUNT", 0)
        val transactionId = intent.getStringExtra("TRANSACTION_ID") ?: "N/A"

        // 2. Hiển thị thông tin lên giao diện
        binding.tvSuccessCampaign.text = "Chiến dịch: $campaignTitle"
        binding.tvSuccessAmount.text = "Số tiền: $amount đ"
        binding.tvSuccessTransactionId.text = "Mã giao dịch: $transactionId"

        // 3. Xử lý sự kiện bấm nút "Về Trang Chủ"
        binding.btnBackToHome.setOnClickListener {
            navigateToHome()
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        super.onBackPressed()
        navigateToHome()
    }
    private fun navigateToHome() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}