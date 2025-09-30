package com.checklist.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.checklist.app.databinding.ItemCategoryBinding

class EjecutivosAdapter(
    private val onEditClick: (Ejecutivo) -> Unit,
    private val onDeleteClick: (Ejecutivo) -> Unit,
    private val isAdminMode: () -> Boolean
) : ListAdapter<Ejecutivo, EjecutivosAdapter.EjecutivoViewHolder>(EjecutivoDiffCallback()) {

    class EjecutivoViewHolder(private val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            ejecutivo: Ejecutivo,
            onEditClick: (Ejecutivo) -> Unit,
            onDeleteClick: (Ejecutivo) -> Unit,
            isAdminMode: Boolean
        ) {
            binding.categoryName.text = ejecutivo.name
            binding.categoryId.text = "ID: ${ejecutivo.id}"
            
            // Configurar color del indicador
            binding.colorIndicator.setBackgroundColor(android.graphics.Color.parseColor(ejecutivo.color))
            
            // Configurar visibilidad de botones según modo admin
            val buttonVisibility = if (isAdminMode) android.view.View.VISIBLE else android.view.View.GONE
            binding.editButton.visibility = buttonVisibility
            binding.deleteButton.visibility = buttonVisibility
            
            binding.editButton.setOnClickListener { onEditClick(ejecutivo) }
            binding.deleteButton.setOnClickListener { onDeleteClick(ejecutivo) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EjecutivoViewHolder {
        val binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EjecutivoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EjecutivoViewHolder, position: Int) {
        holder.bind(getItem(position), onEditClick, onDeleteClick, isAdminMode())
    }

    class EjecutivoDiffCallback : DiffUtil.ItemCallback<Ejecutivo>() {
        override fun areItemsTheSame(oldItem: Ejecutivo, newItem: Ejecutivo): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Ejecutivo, newItem: Ejecutivo): Boolean {
            return oldItem == newItem
        }
    }
}
