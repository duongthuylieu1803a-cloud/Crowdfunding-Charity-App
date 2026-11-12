package com.example.charityapp.ui.adapter

import android.annotation.SuppressLint
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.charityapp.R
import com.example.charityapp.databinding.ItemCampaignBinding
import com.example.charityapp.models.Campaign
import java.text.NumberFormat
import java.util.Locale

class CampaignAdapter(
    private val campaignList: List<Campaign>,
    private val onItemClick: (Campaign) -> Unit
) : RecyclerView.Adapter<CampaignAdapter.CampaignViewHolder>() {

    inner class CampaignViewHolder(private val binding: ItemCampaignBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(campaign: Campaign) {
            binding.tvCampaignTitle.text = campaign.title

            val percent = campaign.getProgressPercentage()
            binding.progressDonation.progress = percent

            val formatter = NumberFormat.getInstance(Locale("vi", "VN"))
            val currentFormatted = formatter.format(campaign.currentAmount)
            val targetFormatted = formatter.format(campaign.targetAmount)

            try {
                binding.tvProgressText.text = "$currentFormatted / $targetFormatted VNĐ ($percent%)"
            } catch (_: Exception) {
            }

            // Tải ảnh bìa: hỗ trợ cả Link URL (http/https) và chuỗi ảnh Base64 cũ
            val rawImage = campaign.getDisplayImage().trim()
            if (rawImage.isNotEmpty()) {
                val imageModel: Any = when {
                    rawImage.startsWith("http://") || rawImage.startsWith("https://") -> rawImage
                    rawImage.startsWith("data:image") -> {
                        val pureBase64 = rawImage.substringAfter(",")
                        try {
                            Base64.decode(pureBase64, Base64.DEFAULT)
                        } catch (_: Exception) {
                            rawImage
                        }
                    }
                    else -> {
                        // Giải mã chuỗi Base64 thuần lưu từ phiên bản cũ
                        try {
                            Base64.decode(rawImage, Base64.DEFAULT)
                        } catch (_: Exception) {
                            rawImage
                        }
                    }
                }

                Glide.with(binding.root.context)
                    .load(imageModel)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(binding.ivCampaignImage)
            } else {
                binding.ivCampaignImage.setImageResource(R.drawable.placeholder_image)
            }

            binding.root.setOnClickListener {
                onItemClick(campaign)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CampaignViewHolder {
        val binding = ItemCampaignBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CampaignViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CampaignViewHolder, position: Int) {
        holder.bind(campaignList[position])
    }

    override fun getItemCount(): Int = campaignList.size
}