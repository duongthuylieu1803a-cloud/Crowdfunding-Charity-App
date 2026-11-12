package com.example.charityapp.admin

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.charityapp.databinding.ActivityAdminBinding
import com.example.charityapp.databinding.ItemAdminDonationBinding
import com.example.charityapp.databinding.ItemCampaignBinding
import com.example.charityapp.models.Campaign
import com.example.charityapp.models.DisbursementReport
import com.example.charityapp.models.Donation
import com.example.charityapp.repository.CampaignRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@SuppressLint("SetTextI18n")
class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val repository = CampaignRepository()

    private var selectedImageUri: Uri? = null
    private var campaignList = mutableListOf<Campaign>()
    private var donationList = mutableListOf<Donation>()

    private val pickCampaignImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                selectedImageUri = uri
                Glide.with(this)
                    .load(uri)
                    .into(binding.ivCampaignImagePreview)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        loadAdminHeaderInfo()
        setupMenuToggles()
        setupListeners()
        loadDashboardData()
    }

    private fun loadAdminHeaderInfo() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            goToLogin()
            return
        }

        val displayName = currentUser.displayName?.takeIf { it.isNotBlank() }
            ?: currentUser.email?.substringBefore("@")
            ?: "Admin"

        binding.adminHeader.tvHelloUser.text = "Hello, $displayName"
        binding.adminHeader.tvUserRoleBadge.text = "Quản trị viên Hệ thống"
        binding.adminHeader.btnLogoutText.setOnClickListener {
            logoutAdmin()
        }
    }

    private fun setupMenuToggles() {
        binding.cardCreateCampaignMenu.setOnClickListener {
            binding.layoutCreateCampaignDetail.isVisible = !binding.layoutCreateCampaignDetail.isVisible
        }

        binding.cardBroadcastMenu.setOnClickListener {
            binding.layoutBroadcastDetail.isVisible = !binding.layoutBroadcastDetail.isVisible
        }

        binding.cardManageCampaignsMenu.setOnClickListener {
            val isNowVisible = !binding.layoutManageCampaignsDetail.isVisible
            binding.layoutManageCampaignsDetail.isVisible = isNowVisible
            if (isNowVisible) {
                loadAdminCampaignsList()
            }
        }

        binding.cardDonatorsMenu.setOnClickListener {
            val isNowVisible = !binding.layoutDonatorsDetail.isVisible
            binding.layoutDonatorsDetail.isVisible = isNowVisible
            if (isNowVisible) {
                loadAdminDonationsList()
            }
        }
    }

    private fun setupListeners() {
        binding.btnSelectCampaignImage.setOnClickListener {
            pickCampaignImageLauncher.launch("image/*")
        }

        binding.btnSubmitCreateCampaign.setOnClickListener {
            createNewCampaign()
        }

        binding.btnSubmitBroadcast.setOnClickListener {
            sendBroadcastNotification()
        }

        binding.btnLogoutAdmin.setOnClickListener {
            logoutAdmin()
        }

        binding.btnQuickTestUser.setOnClickListener {
            openActivityByClassName("com.example.charityapp.MainActivity")
        }
    }

    private fun loadDashboardData() {
        loadAdminCampaignsList()
        loadAdminDonationsList()
    }

    private fun createNewCampaign() {
        val title = binding.etCampaignTitle.text.toString().trim()
        val targetStr = binding.etTargetAmount.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()

        if (title.isEmpty() || targetStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên chiến dịch và số tiền mục tiêu!", Toast.LENGTH_SHORT).show()
            return
        }

        val targetAmount = targetStr.toLongOrNull() ?: 0L
        if (targetAmount <= 0L) {
            Toast.makeText(this, "Số tiền mục tiêu phải lớn hơn 0!", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSubmitCreateCampaign.isEnabled = false
        lifecycleScope.launch {
            try {
                val imageUrl = if (selectedImageUri != null) {
                    uploadImageToStorage(selectedImageUri!!, "campaigns")
                } else {
                    ""
                }

                val newCampaign = Campaign(
                    title = title,
                    description = description,
                    categoryId = "general",
                    creatorId = auth.currentUser?.uid.orEmpty(),
                    targetAmount = targetAmount,
                    currentAmount = 0L,
                    disbursedAmount = 0L,
                    donorCount = 0,
                    status = "ACTIVE",
                    coverImageUrl = imageUrl
                )

                repository.createCampaign(newCampaign).onSuccess {
                    Toast.makeText(this@AdminDashboardActivity, "Phát hành chiến dịch thành công!", Toast.LENGTH_SHORT).show()
                    binding.etCampaignTitle.setText("")
                    binding.etTargetAmount.setText("")
                    binding.etDescription.setText("")
                    binding.ivCampaignImagePreview.setImageResource(android.R.drawable.ic_menu_gallery)
                    selectedImageUri = null
                    loadAdminCampaignsList()
                }.onFailure { err ->
                    Toast.makeText(this@AdminDashboardActivity, "Lỗi: ${err.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AdminDashboardActivity, "Lỗi tải ảnh: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.btnSubmitCreateCampaign.isEnabled = true
            }
        }
    }
    private fun loadAdminCampaignsList() {
        lifecycleScope.launch {
            try {
                val list = repository.getAllCampaigns()
                campaignList.clear()
                campaignList.addAll(list)

                val totalSystemDonations = list.sumOf { it.currentAmount }
                val formatter = NumberFormat.getInstance(Locale("vi", "VN"))
                binding.tvTotalDonations.text = "${formatter.format(totalSystemDonations)} VNĐ"

                binding.rvAdminManageCampaigns.layoutManager = LinearLayoutManager(this@AdminDashboardActivity)
                binding.rvAdminManageCampaigns.adapter = SimpleAdminCampaignAdapter(campaignList) { selectedCampaign ->
                    showCampaignActionDialog(selectedCampaign)
                }
            } catch (e: Exception) {
                Toast.makeText(this@AdminDashboardActivity, "Lỗi tải chiến dịch: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showCampaignActionDialog(campaign: Campaign) {
        val options = arrayOf(
            "Mở hoạt động (ACTIVE)",
            "Tạm dừng (PAUSED)",
            "Hoàn thành (COMPLETED)",
            "Lập báo cáo giải ngân đợt mới"
        )

        AlertDialog.Builder(this)
            .setTitle(campaign.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> updateCampaignStatus(campaign.id, "ACTIVE")
                    1 -> updateCampaignStatus(campaign.id, "PAUSED")
                    2 -> updateCampaignStatus(campaign.id, "COMPLETED")
                    3 -> showDisbursementDialog(campaign)
                }
            }
            .show()
    }

    private fun updateCampaignStatus(campaignId: String, newStatus: String) {
        lifecycleScope.launch {
            repository.updateCampaignStatus(campaignId, newStatus).onSuccess {
                Toast.makeText(this@AdminDashboardActivity, "Đã đổi trạng thái sang $newStatus", Toast.LENGTH_SHORT).show()
                loadAdminCampaignsList()
            }
        }
    }

    private fun showDisbursementDialog(campaign: Campaign) {
        val remaining = campaign.getRemainingBalance()
        if (remaining <= 0L) {
            Toast.makeText(this, "Chiến dịch này hiện không còn số dư khả dụng!", Toast.LENGTH_SHORT).show()
            return
        }

        val inputAmount = EditText(this).apply {
            hint = "Số tiền giải ngân (Tối đa: $remaining VNĐ)"
            inputType = InputType.TYPE_CLASS_NUMBER
        }

        AlertDialog.Builder(this)
            .setTitle("Giải ngân: ${campaign.title}")
            .setMessage("Số dư khả dụng còn lại: $remaining VNĐ")
            .setView(inputAmount)
            .setPositiveButton("Xác nhận") { _, _ ->
                val amount = inputAmount.text.toString().toLongOrNull() ?: 0L
                if (amount <= 0L || amount > remaining) {
                    Toast.makeText(this, "Số tiền giải ngân không hợp lệ!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch {
                    val report = DisbursementReport(
                        campaignId = campaign.id,
                        title = "Giải ngân - ${campaign.title}",
                        description = "Chi trả hỗ trợ thực tế cho chiến dịch",
                        amount = amount,
                        recipientName = campaign.beneficiaryName.ifEmpty { "Đối tượng thụ hưởng" },
                        approvedBy = auth.currentUser?.uid.orEmpty()
                    )
                    repository.createDisbursement(report).onSuccess {
                        Toast.makeText(this@AdminDashboardActivity, "Lập phiếu giải ngân thành công!", Toast.LENGTH_SHORT).show()
                        loadAdminCampaignsList()
                    }.onFailure { err ->
                        Toast.makeText(this@AdminDashboardActivity, "Lỗi: ${err.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun sendBroadcastNotification() {
        val title = binding.etBroadcastTitle.text.toString().trim()
        val message = binding.etBroadcastMessage.text.toString().trim()

        if (title.isEmpty() || message.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ tiêu đề và nội dung thông báo!", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSubmitBroadcast.isEnabled = false
        lifecycleScope.launch {
            try {
                val docRef = db.collection("notifications").document()
                val notificationData = hashMapOf(
                    "id" to docRef.id,
                    "title" to title,
                    "message" to message,
                    "timestamp" to System.currentTimeMillis()
                )
                docRef.set(notificationData).await()

                Toast.makeText(this@AdminDashboardActivity, "Đã phát thông báo tới toàn bộ người dùng!", Toast.LENGTH_SHORT).show()
                binding.etBroadcastTitle.setText("")
                binding.etBroadcastMessage.setText("")
            } catch (e: Exception) {
                Toast.makeText(this@AdminDashboardActivity, "Lỗi gửi thông báo: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnSubmitBroadcast.isEnabled = true
            }
        }
    }

    private fun loadAdminDonationsList() {
        lifecycleScope.launch {
            try {
                val list = repository.getRecentDonations(50)
                donationList.clear()
                donationList.addAll(list)

                binding.rvAdminDonations.layoutManager = LinearLayoutManager(this@AdminDashboardActivity)
                binding.rvAdminDonations.adapter = SimpleAdminDonationAdapter(donationList)
            } catch (e: Exception) {
                Toast.makeText(this@AdminDashboardActivity, "Lỗi tải danh sách đóng góp: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun uploadImageToStorage(uri: Uri, folder: String): String {
        val ref = FirebaseStorage.getInstance().reference.child("$folder/${UUID.randomUUID()}.jpg")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    private fun logoutAdmin() {
        auth.signOut()
        goToLogin()
    }

    private fun goToLogin() {
        openActivityByClassName("com.example.charityapp.ui.auth.LoginActivity", clearStack = true)
    }

    private fun openActivityByClassName(className: String, clearStack: Boolean = false) {
        try {
            val clazz = Class.forName(className)
            val intent = Intent(this, clazz)
            if (clearStack) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            if (clearStack) finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Không tìm thấy màn hình: $className", Toast.LENGTH_SHORT).show()
        }
    }

    private class SimpleAdminCampaignAdapter(
        private val items: List<Campaign>,
        private val onClick: (Campaign) -> Unit
    ) : RecyclerView.Adapter<SimpleAdminCampaignAdapter.VH>() {

        inner class VH(val binding: ItemCampaignBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val binding = ItemCampaignBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(binding)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            val formatter = NumberFormat.getInstance(Locale("vi", "VN"))
            holder.binding.root.setOnClickListener { onClick(item) }
            try {
                holder.binding.tvCampaignTitle.text = "[${item.status}] ${item.title}"
                holder.binding.tvProgressText.text =
                    "Đã đạt: ${formatter.format(item.currentAmount)} / ${formatter.format(item.targetAmount)} đ"
            } catch (_: Exception) {
            }
        }

        override fun getItemCount(): Int = items.size
    }

    private class SimpleAdminDonationAdapter(
        private val items: List<Donation>
    ) : RecyclerView.Adapter<SimpleAdminDonationAdapter.VH>() {

        inner class VH(val binding: ItemAdminDonationBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val binding = ItemAdminDonationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(binding)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            val formatter = NumberFormat.getInstance(Locale("vi", "VN"))
            val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(item.createdAt))
            try {
                holder.binding.tvDonatorName.text = item.getDisplayDonorName()
                holder.binding.tvDonationAmount.text = "+${formatter.format(item.amount)} VNĐ"
                holder.binding.tvCampaignTitle.text = "$dateStr • ${item.campaignTitle}"
            } catch (_: Exception) {
            }
        }

        override fun getItemCount(): Int = items.size
    }
}