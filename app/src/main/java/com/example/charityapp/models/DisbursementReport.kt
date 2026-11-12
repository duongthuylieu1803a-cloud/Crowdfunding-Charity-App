package com.example.charityapp.models

data class DisbursementReport(
    val id: String = "",
    val campaignId: String = "",
    val title: String = "",
    val description: String = "",
    val amount: Long = 0L,
    val recipientName: String = "",
    val proofImageUrl: String = "",
    val approvedBy: String = "",
    val createdAt: Long = System.currentTimeMillis()
)