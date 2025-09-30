package com.checklist.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.checklist.app.databinding.ItemCategoryBinding

class CategoriesAdapter(
    private val onEditClick: (Category) -> Unit,
    private val onDeleteClick: (Category) -> Unit,
    private val isAdminMode: () -> Boolean
) : ListAdapter<Category, CategoriesAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    class CategoryViewHolder(private val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            category: Category,
            onEditClick: (Category) -> Unit,
            onDeleteClick: (Category) -> Unit,
            isAdminMode: Boolean
        ) {
            binding.categoryName.text = category.name
            binding.categoryId.text = "ID: ${category.id}"
            
            // Configurar color del indicador
            binding.colorIndicator.setBackgroundColor(android.graphics.Color.parseColor(category.color))
            
            // Configurar visibilidad de botones según modo admin
            val buttonVisibility = if (isAdminMode) android.view.View.VISIBLE else android.view.View.GONE
            binding.editButton.visibility = buttonVisibility
            binding.deleteButton.visibility = buttonVisibility
            
            binding.editButton.setOnClickListener { onEditClick(category) }
            binding.deleteButton.setOnClickListener { onDeleteClick(category) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position), onEditClick, onDeleteClick, isAdminMode())
    }

    class CategoryDiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem == newItem
        }
    }
}
