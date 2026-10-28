package com.example.charityapp

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserProfileActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val tvName = findViewById<TextView>(R.id.tvName)
        val tvEmail = findViewById<TextView>(R.id.tvEmail)
        val btnBack = findViewById<Button>(R.id.btnBack)

        val userId = auth.currentUser?.uid

        if (userId != null) {
            // Lấy dữ liệu từ Firestore collection "users" theo ID của tài khoản
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val name = document.getString("name") ?: "Chưa cập nhật"
                        val email = document.getString("email") ?: "Chưa cập nhật"

                        tvName.text = "Họ tên: $name"
                        tvEmail.text = "Email: $email"
                    } else {
                        Toast.makeText(this, "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Lỗi tải dữ liệu: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        btnBack.setOnClickListener {
            finish() // Đóng màn hình này để quay về MainActivity
        }
    }
}