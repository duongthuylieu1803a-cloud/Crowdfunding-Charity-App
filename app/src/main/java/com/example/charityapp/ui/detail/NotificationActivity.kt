package com.example.charityapp.ui.detail

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.charityapp.R
import com.example.charityapp.models.NotificationModel
import com.example.charityapp.ui.adapter.NotificationAdapter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class NotificationActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var rvNotifications: RecyclerView
    private lateinit var adapter: NotificationAdapter
    private val notificationList = mutableListOf<NotificationModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        db = FirebaseFirestore.getInstance()
        rvNotifications = findViewById(R.id.rvNotifications)

        // ⚡ Xử lý sự kiện bấm nút quay lại (Back)
        val btnBack = findViewById<View>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish() // Đóng màn hình hiện tại để quay về trang chủ
        }

        adapter = NotificationAdapter(notificationList)
        rvNotifications.layoutManager = LinearLayoutManager(this)
        rvNotifications.adapter = adapter

        // Lắng nghe dữ liệu realtime từ collection "notifications" trên Firestore
        loadNotificationsRealtime()
    }

    private fun loadNotificationsRealtime() {
        db.collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                notificationList.clear()
                if (snapshot != null) {
                    for (doc in snapshot.documents) {
                        val notif = doc.toObject(NotificationModel::class.java)
                        if (notif != null) {
                            notificationList.add(notif)
                        }
                    }
                    adapter.updateData(notificationList)
                }
            }
    }
}