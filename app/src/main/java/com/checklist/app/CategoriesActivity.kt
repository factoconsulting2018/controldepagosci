package com.checklist.app

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.checklist.app.databinding.ActivityCategoriesBinding

class CategoriesActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCategoriesBinding
    private lateinit var categoryManager: CategoryManager
    private lateinit var categoriesAdapter: CategoriesAdapter
    private var isAdminMode = false
    private var selectedColor = "#FF6200EE" // Color por defecto
    private var editingCategory: Category? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ocultar la ActionBar
        supportActionBar?.hide()
        
        try {
            binding = ActivityCategoriesBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            // Obtener el estado de administrador desde el intent
            isAdminMode = intent.getBooleanExtra("isAdminMode", false)
            
            categoryManager = CategoryManager(this)
            setupRecyclerView()
            setupClickListeners()
            loadCategories()
            updateColorPreview()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al inicializar categorías: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
            finish()
        }
    }
    
    private fun setupRecyclerView() {
        categoriesAdapter = CategoriesAdapter(
            onEditClick = { category -> editCategory(category) },
            onDeleteClick = { category -> deleteCategory(category) },
            isAdminMode = { isAdminMode }
        )
        
        binding.categoriesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CategoriesActivity)
            adapter = categoriesAdapter
        }
    }
    
    private fun setupClickListeners() {
        binding.saveCategoryButton.setOnClickListener {
            saveCategory()
        }
        
        binding.clearFormButton.setOnClickListener {
            clearForm()
        }
        
        binding.selectColorButton.setOnClickListener {
            showColorPickerDialog()
        }
        
        binding.backFab.setOnClickListener {
            finish()
        }
    }
    
    private fun loadCategories() {
        val categories = categoryManager.getAllCategories()
        categoriesAdapter.submitList(categories)
    }
    
    private fun saveCategory() {
        val name = binding.categoryNameEditText.text.toString().trim()
        
        if (name.isEmpty()) {
            Toast.makeText(this, "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
            return
        }
        
        val category = if (editingCategory != null) {
            editingCategory!!.copy(
                name = name,
                color = selectedColor
            )
        } else {
            Category(
                name = name,
                color = selectedColor
            )
        }
        
        if (editingCategory != null) {
            categoryManager.updateCategory(category)
            Toast.makeText(this, "Categoría actualizada", Toast.LENGTH_SHORT).show()
        } else {
            categoryManager.addCategory(category)
            Toast.makeText(this, "Categoría agregada", Toast.LENGTH_SHORT).show()
        }
        
        loadCategories()
        clearForm()
    }
    
    private fun clearForm() {
        binding.categoryNameEditText.text.clear()
        selectedColor = "#FF6200EE"
        editingCategory = null
        updateColorPreview()
        binding.saveCategoryButton.text = "Guardar"
    }
    
    private fun updateColorPreview() {
        try {
            binding.selectedColorPreview.setBackgroundColor(Color.parseColor(selectedColor))
        } catch (e: Exception) {
            // Si hay error con el color, usar color por defecto
            binding.selectedColorPreview.setBackgroundColor(Color.parseColor("#FF6200EE"))
        }
    }
    
    private fun showColorPickerDialog() {
        val colors = arrayOf(
            "#FF6200EE", // Púrpura
            "#FF2196F3", // Azul
            "#FF4CAF50", // Verde
            "#FFFF9800", // Naranja
            "#FFF44336", // Rojo
            "#FF9C27B0", // Rosa
            "#FF607D8B", // Azul Gris
            "#FF795548", // Marrón
            "#FF00BCD4", // Cian
            "#FF8BC34A"  // Verde Claro
        )
        
        val colorNames = arrayOf(
            "Púrpura", "Azul", "Verde", "Naranja", "Rojo",
            "Rosa", "Azul Gris", "Marrón", "Cian", "Verde Claro"
        )
        
        AlertDialog.Builder(this)
            .setTitle("Seleccionar Color")
            .setItems(colorNames) { _, which ->
                selectedColor = colors[which]
                updateColorPreview()
            }
            .show()
    }
    
    
    private fun editCategory(category: Category) {
        editingCategory = category
        binding.categoryNameEditText.setText(category.name)
        selectedColor = category.color
        updateColorPreview()
        binding.saveCategoryButton.text = "Actualizar"
        
        // Mostrar mensaje de que se está editando
        Toast.makeText(this, "Editando categoría: ${category.name}", Toast.LENGTH_SHORT).show()
    }
    
    
    private fun deleteCategory(category: Category) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Categoría")
            .setMessage("¿Estás seguro de que quieres eliminar la categoría '${category.name}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                categoryManager.deleteCategory(category)
                loadCategories()
                Toast.makeText(this@CategoriesActivity, "Categoría eliminada", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    fun setAdminMode(adminMode: Boolean) {
        isAdminMode = adminMode
        categoriesAdapter.notifyDataSetChanged()
    }
}
