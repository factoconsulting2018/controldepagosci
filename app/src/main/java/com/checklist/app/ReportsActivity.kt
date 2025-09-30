package com.checklist.app

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.checklist.app.databinding.ActivityReportsBinding
import java.io.File

class ReportsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityReportsBinding
    private lateinit var reportManager: ReportManager
    private lateinit var reportsAdapter: ReportsAdapter
    private lateinit var prefs: SharedPreferences
    private var isAdminMode = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ocultar la ActionBar
        supportActionBar?.hide()
        
        binding = ActivityReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Obtener el estado de administrador desde el intent
        isAdminMode = intent.getBooleanExtra("isAdminMode", false)
        
        prefs = getSharedPreferences("checklist_prefs", MODE_PRIVATE)
        reportManager = ReportManager(this)
        setupRecyclerView()
        setupClickListeners()
        loadReports()
    }
    
    private fun setupRecyclerView() {
        val allowDeleteReports = prefs.getBoolean("allow_delete_reports", false)
        val canDelete = isAdminMode && allowDeleteReports
        
        reportsAdapter = ReportsAdapter(
            onShareClick = { report -> shareReport(report) },
            onDeleteClick = { report -> deleteReport(report) },
            isAdminMode = { isAdminMode },
            canDelete = { canDelete }
        )
        binding.reportsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ReportsActivity)
            adapter = reportsAdapter
        }
    }
    
    private fun setupClickListeners() {
        binding.fabAddReport.setOnClickListener {
            generateNewReport()
        }
    }
    
    private fun loadReports() {
        val reports = reportManager.getAllReports()
        reportsAdapter.submitList(reports)
        
        // Mostrar/ocultar mensaje si no hay reportes
        binding.emptyMessage.visibility = if (reports.isEmpty()) View.VISIBLE else View.GONE
    }
    
    private fun shareReport(report: ReportInfo) {
        try {
            val pdfFile = File(report.filePath)
            if (!pdfFile.exists()) {
                Toast.makeText(this, "El archivo del reporte no existe", Toast.LENGTH_SHORT).show()
                return
            }
            
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                pdfFile
            )
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Reporte de Checklist - ${report.name}")
                putExtra(Intent.EXTRA_TEXT, "Reporte de Checklist\n\n" +
                        "ID: ${String.format("%03d", report.id)}\n" +
                        "Nombre: ${report.name}\n" +
                        "Posición: ${report.position}\n" +
                        "Supervisor: ${report.supervisor}\n" +
                        "Comentarios: ${if (report.comments.isNotEmpty()) report.comments else "Sin comentarios"}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            // Intent específico para WhatsApp
            val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
                setPackage("com.whatsapp")
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Reporte de Checklist - ${report.name}")
                putExtra(Intent.EXTRA_TEXT, "Reporte de Checklist\n\n" +
                        "ID: ${String.format("%03d", report.id)}\n" +
                        "Nombre: ${report.name}\n" +
                        "Posición: ${report.position}\n" +
                        "Supervisor: ${report.supervisor}\n" +
                        "Comentarios: ${if (report.comments.isNotEmpty()) report.comments else "Sin comentarios"}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            // Mostrar opciones de compartir
            val chooserIntent = Intent.createChooser(shareIntent, "Compartir reporte").apply {
                putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(whatsappIntent))
            }
            
            startActivity(chooserIntent)
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error al compartir el reporte: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
    
    private fun deleteReport(report: ReportInfo) {
        // Verificar permisos antes de mostrar el diálogo
        val allowDeleteReports = prefs.getBoolean("allow_delete_reports", false)
        if (!isAdminMode) {
            Toast.makeText(this, "Solo los administradores pueden eliminar reportes", Toast.LENGTH_LONG).show()
            return
        }
        
        if (!allowDeleteReports) {
            Toast.makeText(this, "La eliminación de reportes está deshabilitada en la configuración", Toast.LENGTH_LONG).show()
            return
        }
        
        AlertDialog.Builder(this)
            .setTitle("Eliminar Reporte")
            .setMessage("¿Estás seguro de que quieres eliminar este reporte? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                try {
                    // Eliminar archivo PDF
                    val pdfFile = File(report.filePath)
                    if (pdfFile.exists()) {
                        pdfFile.delete()
                    }
                    
                    // Eliminar de la base de datos
                    reportManager.deleteReport(report.id)
                    
                    // Recargar lista
                    loadReports()
                    
                    Toast.makeText(this, "Reporte eliminado exitosamente", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Error al eliminar el reporte: ${e.message}", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun generateNewReport() {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("generateReport", true)
        startActivity(intent)
        finish()
    }
}
