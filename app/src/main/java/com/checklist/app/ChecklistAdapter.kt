package com.checklist.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.checklist.app.databinding.ItemChecklistBinding

class ChecklistAdapter(
    private val items: MutableList<ChecklistItem>,
    private val onItemChecked: (ChecklistItem, Boolean) -> Unit,
    private val onItemDeleted: (ChecklistItem) -> Unit
) : RecyclerView.Adapter<ChecklistAdapter.ChecklistViewHolder>() {

    class ChecklistViewHolder(private val binding: ItemChecklistBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(
            item: ChecklistItem,
            onItemChecked: (ChecklistItem, Boolean) -> Unit,
            onItemDeleted: (ChecklistItem) -> Unit
        ) {
            binding.itemText.text = item.text
            binding.checkBox.isChecked = item.isChecked
            
            binding.checkBox.setOnCheckedChangeListener { _, isChecked ->
                onItemChecked(item, isChecked)
            }
            
            binding.deleteButton.setOnClickListener {
                onItemDeleted(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChecklistViewHolder {
        val binding = ItemChecklistBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChecklistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChecklistViewHolder, position: Int) {
        holder.bind(items[position], onItemChecked, onItemDeleted)
    }

    override fun getItemCount(): Int = items.size
}
