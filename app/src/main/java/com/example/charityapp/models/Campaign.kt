package com.example.charityapp.models

data class Campaign(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val categoryId: String = "",
    val creatorId: String = "",
    val targetAmount: Long = 0L,
    val currentAmount: Long = 0L,
    val disbursedAmount: Long = 0L,
    val donorCount: Int = 0,
    val status: String = "ACTIVE",
    val coverImageUrl: String = "",
    val imageUrl: String = "", // Dự phòng đọc ảnh từ dữ liệu chiến dịch cũ trên Firestore
    val beneficiaryName: String = "",
    val beneficiaryLocation: String = "",
    val startDate: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getDisplayImage(): String = coverImageUrl.ifEmpty { imageUrl }

    fun getRemainingBalance(): Long = (currentAmount - disbursedAmount).coerceAtLeast(0L)

    fun getProgressPercentage(): Int {
        if (targetAmount <= 0L) return 0
        return ((currentAmount * 100) / targetAmount).toInt().coerceIn(0, 100)
    }
}