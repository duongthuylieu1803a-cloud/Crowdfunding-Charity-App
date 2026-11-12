package com.example.charityapp.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.charityapp.R
import com.example.charityapp.models.Donation
import java.text.NumberFormat
import java.util.Locale

class AdminDonationAdapter(private val donationList: List<Donation>) :
    RecyclerView.Adapter<AdminDonationAdapter.DonationViewHolder>() {

    class DonationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvDonatorName)
        val tvCampaign: TextView = itemView.findViewById(R.id.tvCampaignTitle)
        val tvAmount: TextView = itemView.findViewById(R.id.tvDonationAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DonationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_donation, parent, false)
        return DonationViewHolder(view)
    }

    override fun onBindViewHolder(holder: DonationViewHolder, position: Int) {
        val donation = donationList[position]
        holder.tvName.text = donation.userName.ifEmpty { "Nhà hảo tâm ẩn danh" }
        holder.tvCampaign.text = "Chiến dịch: ${donation.campaignTitle}"

        val formattedAmount = NumberFormat.getCurrencyInstance(Locale("vi", "VN")).format(donation.amount)
        holder.tvAmount.text = "+$formattedAmount"
    }

    override fun getItemCount(): Int = donationList.size
}