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
import com.checklist.app.databinding.ActivityEjecutivosBinding

class EjecutivosActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityEjecutivosBinding
    private lateinit var ejecutivoManager: EjecutivoManager
    private lateinit var ejecutivosAdapter: EjecutivosAdapter
    private var isAdminMode = false
    private var selectedColor = "#FF6200EE" // Color por defecto
    private var editingEjecutivo: Ejecutivo? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ocultar la ActionBar
        supportActionBar?.hide()
        
        try {
            binding = ActivityEjecutivosBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            // Obtener el estado de administrador desde el intent
            isAdminMode = intent.getBooleanExtra("isAdminMode", false)
            
            ejecutivoManager = EjecutivoManager(this)
            setupRecyclerView()
            setupClickListeners()
            loadEjecutivos()
            updateColorPreview()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al inicializar ejecutivos: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
            finish()
        }
    }
    
    private fun setupRecyclerView() {
        ejecutivosAdapter = EjecutivosAdapter(
            onEditClick = { ejecutivo -> editEjecutivo(ejecutivo) },
            onDeleteClick = { ejecutivo -> deleteEjecutivo(ejecutivo) },
            isAdminMode = { isAdminMode }
        )
        
        binding.categoriesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@EjecutivosActivity)
            adapter = ejecutivosAdapter
        }
    }
    
    private fun setupClickListeners() {
        binding.addCategoryButton.setOnClickListener {
            showAddEjecutivoDialog()
        }
        
        binding.backFab.setOnClickListener {
            finish()
        }
    }
    
    private fun loadEjecutivos() {
        val ejecutivos = ejecutivoManager.getAllEjecutivos()
        ejecutivosAdapter.submitList(ejecutivos)
    }
    
    private fun showAddEjecutivoDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_ejecutivo, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.nameEditText)
        val colorButton = dialogView.findViewById<Button>(R.id.colorButton)
        
        // Configurar el botón de color
        colorButton.setOnClickListener {
            showColorPickerDialog { color ->
                selectedColor = color
                updateColorPreview()
            }
        }
        
        updateColorPreview()
        
        AlertDialog.Builder(this)
            .setTitle("Agregar Ejecutivo")
            .setView(dialogView)
            .setPositiveButton("Agregar") { _: android.content.DialogInterface, _: Int ->
                val name = nameEditText.text.toString().trim()
                if (name.isNotEmpty()) {
                    val ejecutivo = Ejecutivo(
                        name = name,
                        color = selectedColor
                    )
                    ejecutivoManager.addEjecutivo(ejecutivo)
                    loadEjecutivos()
                    Toast.makeText(this, "Ejecutivo agregado", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun editEjecutivo(ejecutivo: Ejecutivo) {
        editingEjecutivo = ejecutivo
        selectedColor = ejecutivo.color
        
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_ejecutivo, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.nameEditText)
        val colorButton = dialogView.findViewById<Button>(R.id.colorButton)
        
        nameEditText.setText(ejecutivo.name)
        
        // Configurar el botón de color
        colorButton.setOnClickListener {
            showColorPickerDialog { color ->
                selectedColor = color
                updateColorPreview()
            }
        }
        
        updateColorPreview()
        
        AlertDialog.Builder(this)
            .setTitle("Editar Ejecutivo")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _: android.content.DialogInterface, _: Int ->
                val name = nameEditText.text.toString().trim()
                if (name.isNotEmpty()) {
                    val updatedEjecutivo = ejecutivo.copy(
                        name = name,
                        color = selectedColor
                    )
                    if (ejecutivoManager.updateEjecutivo(updatedEjecutivo)) {
                        loadEjecutivos()
                        Toast.makeText(this, "Ejecutivo actualizado", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Error al actualizar ejecutivo", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun deleteEjecutivo(ejecutivo: Ejecutivo) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Ejecutivo")
            .setMessage("¿Estás seguro de que quieres eliminar el ejecutivo '${ejecutivo.name}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                if (ejecutivoManager.deleteEjecutivo(ejecutivo.id)) {
                    loadEjecutivos()
                    Toast.makeText(this, "Ejecutivo eliminado", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Error al eliminar ejecutivo", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun showColorPickerDialog(onColorSelected: (String) -> Unit) {
        val colors = arrayOf(
            "#FF6200EE", "#FF03DAC5", "#FF018786", "#FF4CAF50",
            "#FF2196F3", "#FFFF9800", "#FFFF5722", "#FFE91E63",
            "#FF9C27B0", "#FF673AB7", "#FF3F51B5", "#FF00BCD4"
        )
        
        val colorNames = arrayOf(
            "Púrpura", "Cian", "Verde", "Verde Claro",
            "Azul", "Naranja", "Rojo", "Rosa",
            "Púrpura Claro", "Púrpura Oscuro", "Índigo", "Cian Claro"
        )
        
        val items = colors.zip(colorNames).map { (color, name) -> "$name" }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("Seleccionar Color")
            .setItems(items) { _, which ->
                onColorSelected(colors[which])
            }
            .show()
    }
    
    private fun updateColorPreview() {
        try {
            val color = Color.parseColor(selectedColor)
            binding.colorPreview.setBackgroundColor(color)
        } catch (e: Exception) {
            // Si hay error con el color, usar color por defecto
            binding.colorPreview.setBackgroundColor(Color.parseColor("#FF6200EE"))
        }
    }
}
