package com.example.charityapp.ui.detail

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.example.charityapp.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.firestore.FirebaseFirestore

class CampaignDetailActivity : AppCompatActivity() {

    private var campaignId: String? = null
    private var campaignTitle: String? = null
    private val db = FirebaseFirestore.getInstance()

    // Khai báo sẵn các View để dùng chung trong toàn Activity
    private lateinit var imgCampaign: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvRaised: TextView
    private lateinit var tvTarget: TextView
    private lateinit var progressIndicator: LinearProgressIndicator
    private lateinit var btnDonateNow: MaterialButton
    private lateinit var tvStatusMessage: TextView // Nhớ thêm TextView này trong file XML nếu muốn hiện thông báo đạt chỉ tiêu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_campaign_detail)

        // Ánh xạ View từ XML
        imgCampaign = findViewById(R.id.imgDetailCampaign)
        tvTitle = findViewById(R.id.tvDetailTitle)
        tvDescription = findViewById(R.id.tvDetailDescription)
        tvRaised = findViewById(R.id.tvDetailRaised)
        tvTarget = findViewById(R.id.tvDetailTarget)
        progressIndicator = findViewById(R.id.progressDetail)
        btnDonateNow = findViewById(R.id.btnDonateNow)

        // Nếu trong file XML activity_campaign_detail.xml chưa có TextView thông báo trạng thái,
        // bạn có thể thêm một TextView có id là @+id/tvStatusMessage ngay phía trên nút quyên góp.

        // 1. Nhận campaignId truyền qua từ Intent
        campaignId = intent.getStringExtra("CAMPAIGN_ID")

        if (campaignId.isNullOrEmpty()) {
            Toast.makeText(this, "Không tìm thấy thông tin chiến dịch", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 2. Lắng nghe dữ liệu chi tiết theo thời gian thực (Realtime) từ Firestore
        listenCampaignDetailRealtime()
    }

    private fun listenCampaignDetailRealtime() {
        db.collection("campaigns").document(campaignId!!)
            .addSnapshotListener { doc, error ->
                if (error != null) {
                    Toast.makeText(this, "Lỗi tải dữ liệu: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (doc != null && doc.exists()) {
                    val title = doc.getString("title") ?: "Chiến dịch từ thiện"
                    val desc = doc.getString("description") ?: "Chưa có mô tả chi tiết."
                    val targetAmount = doc.getDouble("targetAmount") ?: 100000000.0
                    val currentAmount = doc.getDouble("currentAmount") ?: 0.0
                    val imageUrl = doc.getString("imageUrl")
                    val isActive = doc.getBoolean("active") ?: true

                    campaignTitle = title

                    // Hiển thị lên giao diện
                    tvTitle.text = title
                    tvDescription.text = desc
                    tvRaised.text = "Đã đạt: ${String.format("%,.0f", currentAmount)} đ"
                    tvTarget.text = "Mục tiêu: ${String.format("%,.0f", targetAmount)} đ"

                    // Xử lý hiển thị ảnh (Hỗ trợ URL web hoặc chuỗi Base64)
                    if (!imageUrl.isNullOrEmpty()) {
                        if (imageUrl.startsWith("http")) {
                            Glide.with(this).load(imageUrl).into(imgCampaign)
                        } else {
                            try {
                                val decodedBytes = android.util.Base64.decode(imageUrl, android.util.Base64.DEFAULT)
                                val decodedBitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                                imgCampaign.setImageBitmap(decodedBitmap)
                            } catch (e: Exception) {
                                imgCampaign.setImageResource(android.R.drawable.ic_menu_gallery)
                            }
                        }
                    } else {
                        imgCampaign.setImageResource(android.R.drawable.ic_menu_gallery)
                    }

                    // Tính % hoàn thành
                    val progressPercent = if (targetAmount > 0) ((currentAmount / targetAmount) * 100).toInt() else 0
                    progressIndicator.progress = progressPercent.coerceIn(0, 100)

                    // ⚡ KIỂM TRA ĐIỀU KIỆN HOÀN THÀNH HOẶC ĐÓNG CHIẾN DỊCH
                    if (currentAmount >= targetAmount || !isActive) {
                        // Khóa nút quyên góp
                        btnDonateNow.isEnabled = false
                        btnDonateNow.text = "Đã hoàn thành mục tiêu"
                        btnDonateNow.setBackgroundColor(Color.GRAY)
                    } else {
                        // Mở nút quyên góp bình thường
                        btnDonateNow.isEnabled = true
                        btnDonateNow.text = "Quyên góp ngay"

                        // Nút Ủng hộ ngay chuyển sang màn hình thanh toán
                        btnDonateNow.setOnClickListener {
                            val intent = Intent(this, DonateActivity::class.java).apply {
                                putExtra("CAMPAIGN_ID", campaignId)
                                putExtra("CAMPAIGN_TITLE", title)
                            }
                            startActivity(intent)
                        }
                    }

                } else {
                    Toast.makeText(this, "Chiến dịch không tồn tại trên hệ thống", Toast.LENGTH_SHORT).show()
                }
            }
    }
}