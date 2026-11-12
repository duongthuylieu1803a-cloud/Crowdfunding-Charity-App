package com.example.charityapp.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.charityapp.R
import com.example.charityapp.databinding.ItemDisbursementReportBinding
import com.example.charityapp.models.DisbursementReport
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DisbursementAdapter(
    private val list: List<DisbursementReport>
) : RecyclerView.Adapter<DisbursementAdapter.DisbursementViewHolder>() {

    inner class DisbursementViewHolder(val binding: ItemDisbursementReportBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisbursementViewHolder {
        val binding = ItemDisbursementReportBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DisbursementViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: DisbursementViewHolder, position: Int) {
        val item = list[position]
        val formatter = NumberFormat.getInstance(Locale("vi", "VN"))
        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            .format(Date(item.createdAt))

        holder.binding.apply {
            tvDisbursementTitle.text = item.title
            tvDisbursementDate.text = "$dateStr • Số tiền: ${formatter.format(item.amount)} VNĐ"
            tvDisbursementContent.text = if (item.recipientName.isNotEmpty()) {
                "${item.description}\nĐơn vị thụ hưởng: ${item.recipientName}"
            } else {
                item.description
            }

            if (item.proofImageUrl.isNotEmpty()) {
                imgDisbursement.isVisible = true
                Glide.with(root.context)
                    .load(item.proofImageUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .centerCrop()
                    .into(imgDisbursement)
            } else {
                imgDisbursement.isVisible = false
            }
        }
    }

    override fun getItemCount(): Int = list.size
}