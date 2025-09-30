package com.checklist.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.checklist.app.databinding.ActivityNotificationsBinding
import java.text.SimpleDateFormat
import java.util.*

data class NotificationItem(
    val id: Long,
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val type: NotificationType = NotificationType.INFO
)

enum class NotificationType {
    INFO, WARNING, SUCCESS, ERROR
}

class NotificationsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityNotificationsBinding
    private lateinit var notificationsAdapter: NotificationsAdapter
    private val notifications = mutableListOf<NotificationItem>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ocultar la ActionBar
        supportActionBar?.hide()
        
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupRecyclerView()
        setupClickListeners()
        loadNotifications()
    }
    
    private fun setupRecyclerView() {
        notificationsAdapter = NotificationsAdapter(
            onNotificationClick = { notification ->
                markAsRead(notification)
            },
            onDeleteClick = { notification ->
                deleteNotification(notification)
            }
        )
        
        binding.recyclerViewNotifications.apply {
            layoutManager = LinearLayoutManager(this@NotificationsActivity)
            adapter = notificationsAdapter
        }
    }
    
    private fun setupClickListeners() {
        binding.backFab.setOnClickListener {
            finish()
        }
        
        binding.clearAllButton.setOnClickListener {
            clearAllNotifications()
        }
        
        binding.markAllReadButton.setOnClickListener {
            markAllAsRead()
        }
    }
    
    private fun loadNotifications() {
        // Cargar notificaciones de ejemplo
        val sampleNotifications = listOf(
            NotificationItem(
                id = 1,
                title = "Bienvenido a la App",
                message = "Has completado el tutorial exitosamente. ¡Comienza a usar la aplicación!",
                timestamp = System.currentTimeMillis() - 3600000, // 1 hora atrás
                type = NotificationType.SUCCESS
            ),
            NotificationItem(
                id = 2,
                title = "Nuevo Cliente Agregado",
                message = "Se ha agregado un nuevo cliente: Juan Pérez",
                timestamp = System.currentTimeMillis() - 7200000, // 2 horas atrás
                type = NotificationType.INFO
            ),
            NotificationItem(
                id = 3,
                title = "Reporte Generado",
                message = "El reporte de clientes se ha generado correctamente",
                timestamp = System.currentTimeMillis() - 10800000, // 3 horas atrás
                type = NotificationType.SUCCESS
            ),
            NotificationItem(
                id = 4,
                title = "Configuración Actualizada",
                message = "Los estados de clientes se han actualizado a PAGADO/PENDIENTE",
                timestamp = System.currentTimeMillis() - 14400000, // 4 horas atrás
                type = NotificationType.INFO
            ),
            NotificationItem(
                id = 5,
                title = "Sincronización Completada",
                message = "Los datos se han sincronizado correctamente con el servidor",
                timestamp = System.currentTimeMillis() - 18000000, // 5 horas atrás
                type = NotificationType.SUCCESS
            )
        )
        
        notifications.clear()
        notifications.addAll(sampleNotifications)
        notificationsAdapter.submitList(notifications.toList())
        
        updateNotificationCount()
    }
    
    private fun markAsRead(notification: NotificationItem) {
        val index = notifications.indexOfFirst { it.id == notification.id }
        if (index != -1) {
            notifications[index] = notification.copy(isRead = true)
            notificationsAdapter.submitList(notifications.toList())
            updateNotificationCount()
            Toast.makeText(this, "Notificación marcada como leída", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun deleteNotification(notification: NotificationItem) {
        notifications.removeAll { it.id == notification.id }
        notificationsAdapter.submitList(notifications.toList())
        updateNotificationCount()
        Toast.makeText(this, "Notificación eliminada", Toast.LENGTH_SHORT).show()
    }
    
    private fun markAllAsRead() {
        notifications.forEachIndexed { index, notification ->
            if (!notification.isRead) {
                notifications[index] = notification.copy(isRead = true)
            }
        }
        notificationsAdapter.submitList(notifications.toList())
        updateNotificationCount()
        Toast.makeText(this, "Todas las notificaciones marcadas como leídas", Toast.LENGTH_SHORT).show()
    }
    
    private fun clearAllNotifications() {
        notifications.clear()
        notificationsAdapter.submitList(emptyList())
        updateNotificationCount()
        Toast.makeText(this, "Todas las notificaciones eliminadas", Toast.LENGTH_SHORT).show()
    }
    
    private fun updateNotificationCount() {
        val unreadCount = notifications.count { !it.isRead }
        binding.notificationCountText.text = "Notificaciones ($unreadCount sin leer)"
        
        if (notifications.isEmpty()) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.recyclerViewNotifications.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.recyclerViewNotifications.visibility = View.VISIBLE
        }
    }
    
    companion object {
        fun addNotification(
            context: android.content.Context,
            title: String,
            message: String,
            type: NotificationType = NotificationType.INFO
        ) {
            // Aquí se podría implementar la lógica para agregar notificaciones
            // desde otras partes de la aplicación
            android.util.Log.d("NotificationsActivity", "Nueva notificación: $title - $message")
        }
    }
}
