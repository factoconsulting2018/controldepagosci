package com.checklist.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.checklist.app.databinding.ItemNotificationBinding
import java.text.SimpleDateFormat
import java.util.*

class NotificationsAdapter(
    private val onNotificationClick: (NotificationItem) -> Unit,
    private val onDeleteClick: (NotificationItem) -> Unit
) : ListAdapter<NotificationItem, NotificationsAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NotificationViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: NotificationItem) {
            binding.apply {
                notificationTitle.text = notification.title
                notificationMessage.text = notification.message
                notificationTime.text = formatTimestamp(notification.timestamp)
                
                // Configurar estado de leído/no leído
                if (notification.isRead) {
                    root.setBackgroundColor(root.context.getColor(android.R.color.white))
                    notificationTitle.setTextColor(root.context.getColor(com.checklist.app.R.color.gray))
                    notificationMessage.setTextColor(root.context.getColor(com.checklist.app.R.color.gray))
                } else {
                    root.setBackgroundColor(root.context.getColor(com.checklist.app.R.color.green_50))
                    notificationTitle.setTextColor(root.context.getColor(com.checklist.app.R.color.green_900))
                    notificationMessage.setTextColor(root.context.getColor(com.checklist.app.R.color.green_700))
                }
                
                // Configurar ícono según el tipo
                when (notification.type) {
                    NotificationType.SUCCESS -> {
                        notificationIcon.setImageResource(com.checklist.app.R.drawable.ic_check_green)
                        notificationIcon.setColorFilter(root.context.getColor(com.checklist.app.R.color.green_500))
                    }
                    NotificationType.WARNING -> {
                        notificationIcon.setImageResource(com.checklist.app.R.drawable.ic_info)
                        notificationIcon.setColorFilter(root.context.getColor(com.checklist.app.R.color.orange_500))
                    }
                    NotificationType.ERROR -> {
                        notificationIcon.setImageResource(com.checklist.app.R.drawable.ic_close)
                        notificationIcon.setColorFilter(root.context.getColor(com.checklist.app.R.color.red_500))
                    }
                    NotificationType.INFO -> {
                        notificationIcon.setImageResource(com.checklist.app.R.drawable.ic_info)
                        notificationIcon.setColorFilter(root.context.getColor(com.checklist.app.R.color.blue_500))
                    }
                }
                
                // Configurar click listeners
                root.setOnClickListener {
                    onNotificationClick(notification)
                }
                
                deleteButton.setOnClickListener {
                    onDeleteClick(notification)
                }
            }
        }
        
        private fun formatTimestamp(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            
            return when {
                diff < 60000 -> "Hace un momento" // Menos de 1 minuto
                diff < 3600000 -> "${diff / 60000} min atrás" // Menos de 1 hora
                diff < 86400000 -> "${diff / 3600000} h atrás" // Menos de 1 día
                else -> {
                    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    formatter.format(Date(timestamp))
                }
            }
        }
    }
}

class NotificationDiffCallback : DiffUtil.ItemCallback<NotificationItem>() {
    override fun areItemsTheSame(oldItem: NotificationItem, newItem: NotificationItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: NotificationItem, newItem: NotificationItem): Boolean {
        return oldItem == newItem
    }
}
