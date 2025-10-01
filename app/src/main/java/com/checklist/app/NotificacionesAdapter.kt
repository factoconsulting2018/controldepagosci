package com.checklist.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class NotificacionesAdapter(
    private val onEnviarClick: (Notificacion) -> Unit,
    private val onVerMasClick: (Notificacion) -> Unit,
    private val onCompartirClick: (Notificacion) -> Unit,
    private val onEditarClick: (Notificacion) -> Unit,
    private val onDeleteClick: (Notificacion) -> Unit,
    private val isAdminMode: () -> Boolean
) : RecyclerView.Adapter<NotificacionesAdapter.NotificacionViewHolder>() {
    
    private var notificaciones = listOf<Notificacion>()
    
    fun submitList(newList: List<Notificacion>) {
        notificaciones = newList
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificacionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notificacion, parent, false)
        return NotificacionViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: NotificacionViewHolder, position: Int) {
        holder.bind(notificaciones[position])
    }
    
    override fun getItemCount() = notificaciones.size
    
    inner class NotificacionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val clienteNombreText: TextView = itemView.findViewById(R.id.clienteNombreText)
        private val estadoChip: Chip = itemView.findViewById(R.id.estadoChip)
        private val clienteInfoText: TextView = itemView.findViewById(R.id.clienteInfoText)
        private val telefonoText: TextView = itemView.findViewById(R.id.telefonoText)
        private val montoPendienteText: TextView = itemView.findViewById(R.id.montoPendienteText)
        private val mensajeText: TextView = itemView.findViewById(R.id.mensajeText)
        private val fechaText: TextView = itemView.findViewById(R.id.fechaText)
        private val btnVerMas: Button = itemView.findViewById(R.id.btnVerMas)
        private val btnCompartir: Button = itemView.findViewById(R.id.btnCompartir)
        private val btnEnviarWhatsApp: Button = itemView.findViewById(R.id.btnEnviarWhatsApp)
        private val adminButtonsLayout: android.widget.LinearLayout = itemView.findViewById(R.id.adminButtonsLayout)
        private val btnEditarNotificacion: Button = itemView.findViewById(R.id.btnEditarNotificacion)
        private val btnEliminarNotificacion: Button = itemView.findViewById(R.id.btnEliminarNotificacion)
        
        fun bind(notificacion: Notificacion) {
            clienteNombreText.text = notificacion.clienteNombre
            
            if (notificacion.enviada) {
                estadoChip.text = "Enviada"
                estadoChip.setChipBackgroundColorResource(R.color.green_300)
            } else {
                estadoChip.text = "Pendiente"
                estadoChip.setChipBackgroundColorResource(R.color.orange_300)
            }
            
            clienteInfoText.text = "Cliente ID: ${notificacion.clienteId}"
            telefonoText.text = "Teléfono: ${notificacion.clienteTelefono}"
            
            val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CR"))
            montoPendienteText.text = "Monto: ${numberFormat.format(notificacion.montoPendiente)}"
            
            mensajeText.text = notificacion.mensaje
            
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            fechaText.text = "Creada: ${sdf.format(Date(notificacion.fechaCreacion))}"
            
            btnVerMas.setOnClickListener { onVerMasClick(notificacion) }
            btnCompartir.setOnClickListener { onCompartirClick(notificacion) }
            btnEnviarWhatsApp.setOnClickListener { onEnviarClick(notificacion) }
            
            // Mostrar botones de admin solo en modo administrador
            if (isAdminMode()) {
                adminButtonsLayout.visibility = View.VISIBLE
                btnEditarNotificacion.setOnClickListener { onEditarClick(notificacion) }
                btnEliminarNotificacion.setOnClickListener { onDeleteClick(notificacion) }
            } else {
                adminButtonsLayout.visibility = View.GONE
            }
        }
    }
}

