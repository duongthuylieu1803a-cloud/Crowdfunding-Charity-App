package com.example.charityapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.charityapp.admin.AdminDashboardActivity
import com.example.charityapp.databinding.ActivityMainBinding
import com.example.charityapp.models.Campaign
import com.example.charityapp.ui.adapter.CampaignAdapter
import com.example.charityapp.ui.auth.LoginActivity
import com.example.charityapp.ui.detail.CampaignDetailActivity
import com.example.charityapp.ui.detail.NotificationActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.jvm.java

@SuppressLint("SetTextI18n")
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var campaignAdapter: CampaignAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Kiểm tra đăng nhập
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Cấu hình RecyclerView
        binding.rvCampaigns.layoutManager = LinearLayoutManager(this)

        // 1. Tải thông tin "Hello + Tên user" và Avatar cho Header trang chủ
        loadUserHeaderInfo()

        // 2. Kiểm tra quyền Admin để hiện nút vào trang quản trị
        checkAdminRole()

        // 3. Tải danh sách chiến dịch thiện nguyện
        setupRecyclerView()

        // 4. Sự kiện các nút điều hướng trên màn hình chính
        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Nút chuông thông báo
        try {
            binding.btnAdminDashboard.setOnClickListener {
                startActivity(Intent(this, NotificationActivity::class.java))
            }
        } catch (_: Exception) {
        }

        // Nút vào trang Hồ sơ cá nhân (nếu có trong layout)
        try {
            binding.userHeader.ivUserAvatar.setOnClickListener {
                startActivity(Intent(this, UserProfileActivity::class.java))
            }
        } catch (_: Exception) {
        }

        // Nút vào trang Quản trị Admin
        try {
            binding.btnAdminDashboard.setOnClickListener {
                startActivity(Intent(this, AdminDashboardActivity::class.java))
            }
        } catch (_: Exception) {
        }
    }

    private fun loadUserHeaderInfo() {
        val currentUser = auth.currentUser ?: return
        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val name = doc.getString("name") ?: currentUser.displayName ?: "Thành viên"
                    val photoUrl = doc.getString("photoUrl")

                    // Đổ dữ liệu vào Header đúng chuẩn
                    binding.userHeader.tvHelloUser.text = "Hello, $name"
                    binding.userHeader.tvUserRoleBadge.text = "Tài khoản Nhà hảo tâm"

                    if (!photoUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(photoUrl)
                            .circleCrop()
                            .into(binding.userHeader.ivUserAvatar)
                    }
                }
            }
    }

    private fun checkAdminRole() {
        val currentUser = auth.currentUser ?: return
        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { doc ->
                val role = doc.getString("role") ?: "user"
                val isAdmin = role.equals("admin", ignoreCase = true) || (doc.getBoolean("isAdmin") == true)

                try {
                    binding.btnAdminDashboard.visibility = if (isAdmin) View.VISIBLE else View.GONE
                    if (isAdmin) {
                        binding.userHeader.tvUserRoleBadge.text = "Quản trị viên Hệ thống"
                    }
                } catch (_: Exception) {
                }
            }
    }

    private fun setupRecyclerView() {
        db.collection("campaigns")
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Toast.makeText(this, "Lỗi tải dữ liệu: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val campaigns = mutableListOf<Campaign>()
                if (value != null) {
                    for (doc in value.documents) {
                        // Bỏ qua các chiến dịch đã tạm dừng hoặc bị ẩn (hỗ trợ cả field cũ & mới)
                        val isActiveOld = doc.getBoolean("active")
                        val statusNew = doc.getString("status")
                        if (isActiveOld == false || statusNew == "PAUSED" || statusNew == "HIDDEN") {
                            continue
                        }

                        val rawCampaign = doc.toObject(Campaign::class.java) ?: continue

                        // Tự động quét mọi tên trường chứa link ảnh cũ trên Firestore
                        val foundImage = rawCampaign.getDisplayImage().ifEmpty {
                            doc.getString("image")
                                ?: doc.getString("bannerUrl")
                                ?: doc.getString("photoUrl")
                                ?: doc.getString("imgUrl")
                                ?: ""
                        }
                        android.util.Log.d("CHECK_IMG", "Title: ${rawCampaign.title} | Data: ${doc.data}")
                        campaigns.add(
                            rawCampaign.copy(
                                id = doc.id,
                                coverImageUrl = foundImage
                            )
                        )
                    }
                }

                campaignAdapter = CampaignAdapter(campaigns) { campaign ->
                    val intent = Intent(this, CampaignDetailActivity::class.java).apply {
                        putExtra("CAMPAIGN_ID", campaign.id)
                    }
                    startActivity(intent)
                }
                binding.rvCampaigns.adapter = campaignAdapter
            }
    }
}