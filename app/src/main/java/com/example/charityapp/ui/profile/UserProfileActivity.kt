package com.example.charityapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.io.InputStream

class UserProfileActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var ivAvatar: ImageView
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvRole: TextView
    private lateinit var btnBack: Button

    // Trình chọn ảnh từ thiết bị
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            // Hiển thị preview ngay lập tức
            ivAvatar.setImageURI(uri)
            // Chuyển ảnh thành Base64 và lưu vào Firestore
            uploadAvatarAsBase64(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Ánh xạ view
        ivAvatar = findViewById(R.id.ivAvatar)
        tvName = findViewById(R.id.tvName)
        tvEmail = findViewById(R.id.tvEmail)
        tvRole = findViewById(R.id.tvRole)
        btnBack = findViewById(R.id.btnBack)

        loadUserData()

        // Bấm vào ảnh đại diện để thay đổi ảnh mới
        ivAvatar.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val name = document.getString("name") ?: "Chưa cập nhật"
                        val email = document.getString("email") ?: "Chưa cập nhật"
                        val role = document.getString("role") ?: "user"
                        val photoUrl = document.getString("photoUrl")

                        tvName.text = "Họ tên: $name"
                        tvEmail.text = "Email: $email"
                        tvRole.text = "Vai trò: ${role.replaceFirstChar { it.uppercase() }}"

                        // Hỗ trợ hiển thị ảnh từ URL web hoặc chuỗi mã hóa Base64
                        if (!photoUrl.isNullOrEmpty()) {
                            if (photoUrl.startsWith("http")) {
                                Glide.with(this)
                                    .load(photoUrl)
                                    .circleCrop()
                                    .into(ivAvatar)
                            } else {
                                try {
                                    val decodedBytes = Base64.decode(photoUrl, Base64.DEFAULT)
                                    val decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                                    ivAvatar.setImageBitmap(decodedBitmap)
                                } catch (e: Exception) {
                                    ivAvatar.setImageResource(android.R.drawable.ic_menu_camera)
                                }
                            }
                        }
                    } else {
                        Toast.makeText(this, "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Lỗi tải dữ liệu: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun uploadAvatarAsBase64(uri: Uri) {
        val userId = auth.currentUser?.uid ?: return
        Toast.makeText(this, "Đang cập nhật ảnh đại diện...", Toast.LENGTH_SHORT).show()

        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return

            // Thu gọn kích thước ảnh avatar (chiều rộng 400px là vừa đủ nét cho avatar)
            val targetWidth = 400
            val targetHeight = (originalBitmap.height * (targetWidth.toDouble() / originalBitmap.width)).toInt()
            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val byteArray = outputStream.toByteArray()

            // Mã hóa thành chuỗi Base64
            val base64String = Base64.encodeToString(byteArray, Base64.DEFAULT)

            // Cập nhật chuỗi Base64 trực tiếp vào Firestore thay vì dùng Storage
            db.collection("users").document(userId)
                .update("photoUrl", base64String)
                .addOnSuccessListener {
                    Toast.makeText(this, "Cập nhật ảnh đại diện thành công!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Lỗi lưu vào Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
                }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Lỗi xử lý ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}