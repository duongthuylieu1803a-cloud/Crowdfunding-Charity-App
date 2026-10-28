package com.example.charityapp.repository

import android.net.Uri
import com.example.charityapp.models.Campaign
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction
import com.google.firebase.storage.FirebaseStorage

class CampaignRepository {
    private val db = FirebaseFirestore.getInstance()
    private val storageRef = FirebaseStorage.getInstance().reference

    // 1. Lấy danh sách tất cả chiến dịch từ Firestore
    fun getCampaigns(onSuccess: (List<Campaign>) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("campaigns")
            .get()
            .addOnSuccessListener { result ->
                val list = result.toObjects(Campaign::class.java)
                onSuccess(list)
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    // 2. Thêm mới một chiến dịch (có kèm tải ảnh lên Cloud Storage)
    fun addCampaign(campaign: Campaign, imageUri: Uri?, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val docId = db.collection("campaigns").document().id
        campaign.campaignId = docId

        if (imageUri != null) {
            val fileRef = storageRef.child("campaign_images/$docId.jpg")
            fileRef.putFile(imageUri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let { throw it }
                    }
                    fileRef.downloadUrl
                }
                .addOnSuccessListener { downloadUri ->
                    campaign.imageUrl = downloadUri.toString()
                    saveCampaignToFirestore(campaign, onSuccess, onFailure)
                }
                .addOnFailureListener { e -> onFailure(e) }
        } else {
            saveCampaignToFirestore(campaign, onSuccess, onFailure)
        }
    }

    private fun saveCampaignToFirestore(campaign: Campaign, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("campaigns").document(campaign.campaignId)
            .set(campaign)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    // 3. Xử lý quyên góp an toàn bằng Firestore Transaction
    fun donateToCampaign(
        campaignId: String,
        donationAmount: Double,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val campaignRef = db.collection("campaigns").document(campaignId)

        db.runTransaction { transaction: Transaction ->
            val snapshot = transaction.get(campaignRef)
            val currentTotal = snapshot.getDouble("currentAmount") ?: 0.0
            val newTotal = currentTotal + donationAmount

            transaction.update(campaignRef, "currentAmount", newTotal)
        }.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener { e ->
            onFailure(e)
        }
    }
}
