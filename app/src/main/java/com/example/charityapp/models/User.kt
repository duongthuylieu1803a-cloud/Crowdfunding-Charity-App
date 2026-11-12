package com.example.charityapp.models

data class User(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val avatarUrl: String = "",
    val role: String = "USER",           // "USER", "ADMIN"
    val tier: String = "MEMBER",         // "MEMBER", "SILVER", "GOLD", "DIAMOND"
    val totalDonated: Long = 0L,
    val campaignsCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)