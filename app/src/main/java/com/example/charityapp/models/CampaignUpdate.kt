package com.example.charityapp.models

data class CampaignUpdate(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val imageUrls: List<String> = emptyList(),
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis()
)