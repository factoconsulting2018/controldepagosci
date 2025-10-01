package com.checklist.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class IssuesAdapter(
    private val onVerDetalleClick: (Issue) -> Unit,
    private val onEditarClick: (Issue) -> Unit,
    private val onEliminarClick: (Issue) -> Unit,
    private val onEstadoChanged: (Issue, EstadoIssue) -> Unit,
    private val isAdminMode: () -> Boolean
) : RecyclerView.Adapter<IssuesAdapter.IssueViewHolder>() {
    
    private var issues = listOf<Issue>()
    
    fun submitList(newList: List<Issue>) {
        issues = newList
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IssueViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_issue_card, parent, false)
        return IssueViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: IssueViewHolder, position: Int) {
        holder.bind(issues[position])
    }
    
    override fun getItemCount() = issues.size
    
    inner class IssueViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: MaterialCardView = itemView as MaterialCardView
        private val clienteNombreText: TextView = itemView.findViewById(R.id.clienteNombreText)
        private val issuesLayout: LinearLayout = itemView.findViewById(R.id.issuesLayout)
        private val totalIssuesText: TextView = itemView.findViewById(R.id.totalIssuesText)
        private val fechaCreacionText: TextView = itemView.findViewById(R.id.fechaCreacionText)
        private val btnVerDetalle: Button = itemView.findViewById(R.id.btnVerDetalle)
        private val estadoSpinner: Spinner = itemView.findViewById(R.id.estadoSpinner)
        private val adminButtonsLayout: LinearLayout = itemView.findViewById(R.id.adminButtonsLayout)
        private val btnEditarIssue: Button = itemView.findViewById(R.id.btnEditarIssue)
        private val btnEliminarIssue: Button = itemView.findViewById(R.id.btnEliminarIssue)
        
        fun bind(issue: Issue) {
            // Cambiar color del card según el estado
            val colorEstado = when (issue.estado) {
                EstadoIssue.PENDIENTE -> R.color.red_500
                EstadoIssue.EN_PROCESO -> R.color.orange_500
                EstadoIssue.FINALIZADO -> R.color.green_500
            }
            card.setCardBackgroundColor(ContextCompat.getColor(itemView.context, colorEstado))
            
            clienteNombreText.text = issue.clienteNombre
            clienteNombreText.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
            
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            
            issuesLayout.removeAllViews()
            
            issue.issues.forEachIndexed { index, issueItem ->
                val issueText = TextView(itemView.context).apply {
                    val mensajePreview = if (issueItem.mensaje.length > 50) {
                        issueItem.mensaje.substring(0, 50) + "..."
                    } else {
                        issueItem.mensaje
                    }
                    text = "${index + 1}. ${issueItem.titulo} - ${sdf.format(Date(issueItem.fechaIssue))}\n   ${mensajePreview}"
                    textSize = 12f
                    setPadding(0, 4, 0, 4)
                    setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
                }
                issuesLayout.addView(issueText)
            }
            
            totalIssuesText.text = "Total de Issues: ${issue.getTotalIssues()}"
            totalIssuesText.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
            
            val sdfCreacion = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            fechaCreacionText.text = "Creado: ${sdfCreacion.format(Date(issue.fechaCreacion))}"
            fechaCreacionText.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
            
            btnVerDetalle.setOnClickListener { onVerDetalleClick(issue) }
            
            // Configurar Spinner de estado
            val estados = listOf("Pendiente", "En Proceso", "Finalizado")
            val estadoAdapter = ArrayAdapter(itemView.context, android.R.layout.simple_spinner_item, estados)
            estadoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            estadoSpinner.adapter = estadoAdapter
            
            // Establecer el estado actual
            val estadoIndex = when (issue.estado) {
                EstadoIssue.PENDIENTE -> 0
                EstadoIssue.EN_PROCESO -> 1
                EstadoIssue.FINALIZADO -> 2
            }
            estadoSpinner.setSelection(estadoIndex)
            
            // Manejar cambio de estado
            var isFirstSelection = true
            estadoSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    // Evitar trigger en la primera selección (cuando se establece el valor inicial)
                    if (isFirstSelection) {
                        isFirstSelection = false
                        return
                    }
                    
                    val nuevoEstado = when (position) {
                        0 -> EstadoIssue.PENDIENTE
                        1 -> EstadoIssue.EN_PROCESO
                        2 -> EstadoIssue.FINALIZADO
                        else -> EstadoIssue.PENDIENTE
                    }
                    
                    // Solo actualizar si cambió
                    if (nuevoEstado != issue.estado) {
                        // Cambiar color del card inmediatamente
                        val colorEstado = when (nuevoEstado) {
                            EstadoIssue.PENDIENTE -> R.color.red_500
                            EstadoIssue.EN_PROCESO -> R.color.orange_500
                            EstadoIssue.FINALIZADO -> R.color.green_500
                        }
                        card.setCardBackgroundColor(ContextCompat.getColor(itemView.context, colorEstado))
                        
                        onEstadoChanged(issue, nuevoEstado)
                    }
                }
                
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            
            if (isAdminMode()) {
                adminButtonsLayout.visibility = View.VISIBLE
                btnEditarIssue.setOnClickListener { onEditarClick(issue) }
                btnEliminarIssue.setOnClickListener { onEliminarClick(issue) }
            } else {
                adminButtonsLayout.visibility = View.GONE
            }
        }
    }
}

