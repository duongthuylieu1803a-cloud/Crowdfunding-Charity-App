package com.example.charityapp.models

import com.google.firebase.firestore.Exclude

data class Donation(
    val id: String = "",
    val campaignId: String = "",
    val campaignTitle: String = "",
    val userId: String = "",
    val donorName: String = "",
    val isAnonymous: Boolean = false,
    val amount: Long = 0L,
    val message: String = "",
    val paymentMethod: String = "ZALOPAY",
    val zpTransToken: String = "",
    val appTransId: String = "",
    val transactionStatus: String = "PENDING", // "PENDING", "SUCCESS", "FAILED"
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long = 0L
) {
    // --- Cầu nối tương thích với code cũ ---
    @get:Exclude
    val userName: String
        get() = donorName

    @get:Exclude
    val timestamp: Long
        get() = createdAt

    // --- Hàm tiện ích mới ---
    fun getDisplayDonorName(): String = if (isAnonymous) "Nhà hảo tâm ẩn danh" else donorName
}