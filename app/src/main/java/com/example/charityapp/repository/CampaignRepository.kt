package com.example.charityapp.repository

import com.example.charityapp.models.Campaign
import com.example.charityapp.models.CampaignUpdate
import com.example.charityapp.models.DisbursementReport
import com.example.charityapp.models.Donation
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class CampaignRepository {
    private val db = FirebaseFirestore.getInstance()
    private val campaignsRef = db.collection("campaigns")
    private val donationsRef = db.collection("donations")
    private val usersRef = db.collection("users")
    private val disbursementsRef = db.collection("disbursements")

    // 1. Tạo chiến dịch mới (Admin)
    suspend fun createCampaign(campaign: Campaign): Result<String> {
        return try {
            val docRef = campaignsRef.document()
            val newCampaign = campaign.copy(
                id = docRef.id,
                startDate = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            docRef.set(newCampaign).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 2. Lấy toàn bộ danh sách chiến dịch
    suspend fun getAllCampaigns(): List<Campaign> {
        return campaignsRef
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .get()
            .await()
            .toObjects(Campaign::class.java)
    }

    // 3. Cập nhật trạng thái chiến dịch (ACTIVE / PAUSED / COMPLETED)
    suspend fun updateCampaignStatus(campaignId: String, newStatus: String): Result<Unit> {
        return try {
            campaignsRef.document(campaignId).update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 4. Lấy danh sách giao dịch quyên góp gần nhất
    suspend fun getRecentDonations(limit: Long = 50): List<Donation> {
        return donationsRef
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()
            .toObjects(Donation::class.java)
    }

    // 5. Xác nhận quyên góp thành công (Dùng Transaction chống sai lệch tiền)
    suspend fun confirmDonationSuccess(donation: Donation): Result<Unit> {
        return try {
            val donationDocRef = donationsRef.document(donation.id)
            val campaignDocRef = campaignsRef.document(donation.campaignId)
            val userDocRef = if (donation.userId.isNotEmpty()) usersRef.document(donation.userId) else null

            db.runTransaction { transaction ->
                val campaignSnapshot = transaction.get(campaignDocRef)
                val currentAmt = campaignSnapshot.getLong("currentAmount") ?: 0L
                val targetAmt = campaignSnapshot.getLong("targetAmount") ?: 0L
                val newAmount = currentAmt + donation.amount

                val completedDonation = donation.copy(
                    transactionStatus = "SUCCESS",
                    completedAt = System.currentTimeMillis()
                )
                transaction.set(donationDocRef, completedDonation)

                val newStatus = if (targetAmt > 0 && newAmount >= targetAmt) "COMPLETED" else "ACTIVE"
                transaction.update(
                    campaignDocRef,
                    mapOf(
                        "currentAmount" to FieldValue.increment(donation.amount),
                        "donorCount" to FieldValue.increment(1),
                        "status" to newStatus,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )

                if (userDocRef != null) {
                    val userSnap = transaction.get(userDocRef)
                    if (userSnap.exists()) {
                        val oldTotal = userSnap.getLong("totalDonated") ?: 0L
                        val newTotal = oldTotal + donation.amount
                        val newTier = calculateUserTier(newTotal)
                        transaction.update(
                            userDocRef,
                            mapOf(
                                "totalDonated" to FieldValue.increment(donation.amount),
                                "campaignsCount" to FieldValue.increment(1),
                                "tier" to newTier
                            )
                        )
                    }
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 6. Tạo báo cáo giải ngân (Admin)
    suspend fun createDisbursement(report: DisbursementReport): Result<Unit> {
        return try {
            val reportDocRef = disbursementsRef.document()
            val campaignDocRef = campaignsRef.document(report.campaignId)

            db.runTransaction { transaction ->
                val campaignSnap = transaction.get(campaignDocRef)
                val currentAmt = campaignSnap.getLong("currentAmount") ?: 0L
                val disbursedAmt = campaignSnap.getLong("disbursedAmount") ?: 0L

                if (disbursedAmt + report.amount > currentAmt) {
                    throw IllegalStateException("Số tiền giải ngân vượt quá số dư hiện có của chiến dịch!")
                }

                transaction.set(reportDocRef, report.copy(id = reportDocRef.id))
                transaction.update(
                    campaignDocRef,
                    mapOf(
                        "disbursedAmount" to FieldValue.increment(report.amount),
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 7. Lấy Bảng vàng Top nhà hảo tâm
    suspend fun getTopDonors(campaignId: String, limit: Long = 10): List<Donation> {
        return donationsRef
            .whereEqualTo("campaignId", campaignId)
            .whereEqualTo("transactionStatus", "SUCCESS")
            .orderBy("amount", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()
            .toObjects(Donation::class.java)
    }

    // 8. Đăng nhật ký cập nhật tiến độ chiến dịch
    suspend fun postCampaignUpdate(campaignId: String, update: CampaignUpdate): Result<Unit> {
        return try {
            val docRef = campaignsRef.document(campaignId).collection("updates").document()
            docRef.set(update.copy(id = docRef.id)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun calculateUserTier(totalDonated: Long): String = when {
        totalDonated >= 50_000_000L -> "DIAMOND"
        totalDonated >= 10_000_000L -> "GOLD"
        totalDonated >= 2_000_000L -> "SILVER"
        else -> "MEMBER"
    }
}