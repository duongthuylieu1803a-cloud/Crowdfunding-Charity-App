package com.example.charityapp.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.charityapp.R
import com.example.charityapp.models.NotificationModel
import java.text.SimpleDateFormat
import java.util.Locale

class NotificationAdapter(private var list: List<NotificationModel>) :
    RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvNotifTitle)
        val tvMessage: TextView = view.findViewById(R.id.tvNotifMessage)
        val tvTime: TextView = view.findViewById(R.id.tvNotifTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.tvTitle.text = item.title
        holder.tvMessage.text = item.message

        if (item.timestamp != null) {
            val date = item.timestamp!!.toDate()
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            holder.tvTime.text = sdf.format(date)
        } else {
            holder.tvTime.text = "Vừa xong"
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<NotificationModel>) {
        list = newList
        notifyDataSetChanged()
    }
}