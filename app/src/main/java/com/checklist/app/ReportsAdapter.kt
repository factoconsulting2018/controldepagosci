package com.checklist.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.checklist.app.databinding.ItemReportBinding
import java.text.SimpleDateFormat
import java.util.*

class ReportsAdapter(
    private val onShareClick: (ReportInfo) -> Unit,
    private val onDeleteClick: (ReportInfo) -> Unit,
    private val isAdminMode: () -> Boolean,
    private val canDelete: () -> Boolean
) : ListAdapter<ReportInfo, ReportsAdapter.ReportViewHolder>(ReportDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val binding = ItemReportBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ReportViewHolder(private val binding: ItemReportBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(report: ReportInfo) {
            binding.apply {
                idText.text = "ID: ${String.format("%03d", report.id)}"
                nameText.text = report.name
                positionText.text = report.position
                supervisorText.text = report.supervisor
                dateText.text = if (report.comments.isNotEmpty()) report.comments else "Sin comentarios"
                
                // Mostrar/ocultar botones según los permisos
                val adminMode = isAdminMode()
                val deleteAllowed = canDelete()
                shareButton.visibility = android.view.View.VISIBLE
                deleteButton.visibility = if (adminMode && deleteAllowed) android.view.View.VISIBLE else android.view.View.GONE
                
                shareButton.setOnClickListener {
                    onShareClick(report)
                }
                
                deleteButton.setOnClickListener {
                    onDeleteClick(report)
                }
            }
        }
    }

    class ReportDiffCallback : DiffUtil.ItemCallback<ReportInfo>() {
        override fun areItemsTheSame(oldItem: ReportInfo, newItem: ReportInfo): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ReportInfo, newItem: ReportInfo): Boolean {
            return oldItem == newItem
        }
    }
}
