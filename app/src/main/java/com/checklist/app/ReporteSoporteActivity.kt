package com.checklist.app

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import java.text.SimpleDateFormat
import java.util.*

class ReporteSoporteActivity : AppCompatActivity() {
    
    private lateinit var issueManager: IssueManager
    private lateinit var reporteContainer: LinearLayout
    private lateinit var fabGenerarPdf: ExtendedFloatingActionButton
    private lateinit var fabCompartirPdf: ExtendedFloatingActionButton
    private lateinit var fabEliminarMasivo: ExtendedFloatingActionButton
    private lateinit var filterChipGroup: ChipGroup
    private var ultimoPdfGenerado: String? = null
    private var isAdminMode = false
    private val issuesSeleccionados = mutableSetOf<Long>()
    private var filtroEstadoActual: EstadoIssue? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_reporte_soporte)
        
        isAdminMode = intent.getBooleanExtra("isAdminMode", false)
        
        issueManager = IssueManager(this)
        reporteContainer = findViewById(R.id.reporteContainer)
        fabGenerarPdf = findViewById(R.id.fabGenerarPdf)
        fabCompartirPdf = findViewById(R.id.fabCompartirPdf)
        
        setupFilterChips()
        setupAdminMode()
        generarReporte()
        
        fabGenerarPdf.setOnClickListener {
            generarPdfSoporte()
        }
        
        fabCompartirPdf.setOnClickListener {
            compartirPdf()
        }
        
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName
            findViewById<TextView>(R.id.footerText).text = "Desarrollada por Ing.Ronald Rojas C | 8878-1108 | Ver: $versionName"
        } catch (e: Exception) {
            findViewById<TextView>(R.id.footerText).text = "Desarrollada por Ing.Ronald Rojas C | 8878-1108 | Ver: 1.0"
        }
    }
    
    private fun setupFilterChips() {
        filterChipGroup = findViewById(R.id.filterChipGroup)
        
        // Chip "Todos"
        addFilterChip("Todos", null, true)
        
        // Chips por estado
        addFilterChip("Pendiente", EstadoIssue.PENDIENTE, false)
        addFilterChip("En Proceso", EstadoIssue.EN_PROCESO, false)
        addFilterChip("Finalizado", EstadoIssue.FINALIZADO, false)
    }
    
    private fun addFilterChip(texto: String, estado: EstadoIssue?, isChecked: Boolean) {
        val chip = Chip(this).apply {
            text = texto
            isCheckable = true
            this.isChecked = isChecked
            setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    filtroEstadoActual = estado
                    generarReporte()
                    // Deseleccionar otros chips
                    for (i in 0 until filterChipGroup.childCount) {
                        val otherChip = filterChipGroup.getChildAt(i) as? Chip
                        if (otherChip != this) {
                            otherChip?.isChecked = false
                        }
                    }
                }
            }
        }
        filterChipGroup.addView(chip)
    }
    
    private fun setupAdminMode() {
        if (isAdminMode) {
            fabEliminarMasivo = findViewById(R.id.fabEliminarMasivo)
            fabEliminarMasivo.setOnClickListener {
                eliminarSeleccionados()
            }
        }
    }
    
    private fun generarReporte() {
        reporteContainer.removeAllViews()
        issuesSeleccionados.clear()
        
        val allIssues = if (filtroEstadoActual != null) {
            issueManager.getIssuesByEstado(filtroEstadoActual!!)
        } else {
            issueManager.getAllIssues()
        }
        
        // Ordenar por estado: Pendiente -> En Proceso -> Finalizado
        val issues = allIssues.sortedWith(
            compareBy<Issue> { 
                when (it.estado) {
                    EstadoIssue.PENDIENTE -> 0
                    EstadoIssue.EN_PROCESO -> 1
                    EstadoIssue.FINALIZADO -> 2
                }
            }.thenBy { it.clienteNombre }
        )
        
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        
        var totalIssuesGeneral = 0
        
        issues.forEach { issue ->
            val clienteCard = createIssueCard(issue, sdf)
            reporteContainer.addView(clienteCard)
            totalIssuesGeneral += issue.getTotalIssues()
        }
        
        // Total general al final
        if (issues.isNotEmpty()) {
            val totalGeneralCard = createTotalGeneralCard(totalIssuesGeneral)
            reporteContainer.addView(totalGeneralCard)
        } else {
            val emptyText = TextView(this).apply {
                text = "No hay issues para mostrar"
                textSize = 16f
                gravity = Gravity.CENTER
                setPadding(16, 32, 16, 32)
            }
            reporteContainer.addView(emptyText)
        }
    }
    
    private fun createIssueCard(issue: Issue, sdf: SimpleDateFormat): MaterialCardView {
        val clienteCard = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
            radius = 12f
            cardElevation = 4f
            setCardBackgroundColor(getColorForEstado(issue.estado))
        }
        
        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        // CheckBox para selección masiva (solo en modo admin)
        if (isAdminMode) {
            val checkBox = CheckBox(this).apply {
                text = "Seleccionar para eliminar"
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        issuesSeleccionados.add(issue.id)
                    } else {
                        issuesSeleccionados.remove(issue.id)
                    }
                    updateEliminarButton()
                }
            }
            contentLayout.addView(checkBox)
        }
        
        // Nombre del cliente
        val nombreText = TextView(this).apply {
            text = issue.clienteNombre
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.white))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 12)
            }
        }
        contentLayout.addView(nombreText)
        
        // Selector de estado
        val estadoLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 12)
            }
        }
        
        val estadoLabel = TextView(this).apply {
            text = "Estado: "
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.white))
        }
        estadoLayout.addView(estadoLabel)
        
        val estadoSpinner = Spinner(this).apply {
            val estados = arrayOf("Pendiente", "En Proceso", "Finalizado")
            val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, estados)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            setAdapter(adapter)
            
            setSelection(when (issue.estado) {
                EstadoIssue.PENDIENTE -> 0
                EstadoIssue.EN_PROCESO -> 1
                EstadoIssue.FINALIZADO -> 2
            })
            
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val nuevoEstado = when (position) {
                        0 -> EstadoIssue.PENDIENTE
                        1 -> EstadoIssue.EN_PROCESO
                        else -> EstadoIssue.FINALIZADO
                    }
                    
                    if (nuevoEstado != issue.estado) {
                        issueManager.updateEstado(issue.id, nuevoEstado)
                        Toast.makeText(context, "Estado actualizado", Toast.LENGTH_SHORT).show()
                        generarReporte()
                    }
                }
                
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
        estadoLayout.addView(estadoSpinner)
        contentLayout.addView(estadoLayout)
        
        // Tabla de issues
        issue.issues.forEach { issueItem ->
            val issueLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 4, 0, 8)
                }
            }
            
            val tituloText = TextView(this).apply {
                text = "• ${issueItem.titulo}"
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(context, R.color.white))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            
            val mensajeText = TextView(this).apply {
                text = "  ${issueItem.mensaje}"
                textSize = 12f
                setTextColor(ContextCompat.getColor(context, R.color.white))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 4, 0, 2)
                }
            }
            
            val fechaText = TextView(this).apply {
                text = "  Fecha: ${sdf.format(Date(issueItem.fechaIssue))}"
                textSize = 11f
                setTextColor(ContextCompat.getColor(context, R.color.white))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            
            issueLayout.addView(tituloText)
            issueLayout.addView(mensajeText)
            issueLayout.addView(fechaText)
            
            contentLayout.addView(issueLayout)
        }
        
        // Línea divisoria
        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                2
            ).apply {
                setMargins(0, 8, 0, 8)
            }
            setBackgroundColor(ContextCompat.getColor(context, R.color.white))
        }
        contentLayout.addView(divider)
        
        // Total por cliente
        val totalText = TextView(this).apply {
            text = "Total de Issues: ${issue.getTotalIssues()}"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.white))
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        contentLayout.addView(totalText)
        
        clienteCard.addView(contentLayout)
        return clienteCard
    }
    
    private fun getColorForEstado(estado: EstadoIssue): Int {
        return ContextCompat.getColor(this, when (estado) {
            EstadoIssue.PENDIENTE -> R.color.red_500
            EstadoIssue.EN_PROCESO -> R.color.orange_500
            EstadoIssue.FINALIZADO -> R.color.green_500
        })
    }
    
    private fun createTotalGeneralCard(totalIssuesGeneral: Int): MaterialCardView {
        val totalGeneralCard = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 16)
            }
            radius = 12f
            cardElevation = 6f
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.blue_700))
        }
        
        val totalGeneralText = TextView(this).apply {
            text = "TOTAL DE ISSUES: $totalIssuesGeneral"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.white))
            gravity = Gravity.CENTER
            setPadding(16, 24, 16, 24)
        }
        
        totalGeneralCard.addView(totalGeneralText)
        return totalGeneralCard
    }
    
    private fun updateEliminarButton() {
        if (::fabEliminarMasivo.isInitialized) {
            fabEliminarMasivo.visibility = if (issuesSeleccionados.isNotEmpty()) View.VISIBLE else View.GONE
            fabEliminarMasivo.text = "Eliminar (${issuesSeleccionados.size})"
        }
    }
    
    private fun eliminarSeleccionados() {
        if (issuesSeleccionados.isEmpty()) {
            Toast.makeText(this, "No hay issues seleccionados", Toast.LENGTH_SHORT).show()
            return
        }
        
        AlertDialog.Builder(this)
            .setTitle("Eliminar Issues")
            .setMessage("¿Está seguro de eliminar ${issuesSeleccionados.size} issue(s) seleccionado(s)?")
            .setPositiveButton("Eliminar") { _, _ ->
                issueManager.deleteIssuesByIds(issuesSeleccionados.toList())
                Toast.makeText(this, "Issues eliminados exitosamente", Toast.LENGTH_SHORT).show()
                issuesSeleccionados.clear()
                generarReporte()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun generarPdfSoporte() {
        try {
            android.util.Log.d("ReporteSoporteActivity", "Iniciando generación de PDF...")
            val issues = if (filtroEstadoActual != null) {
                issueManager.getIssuesByEstado(filtroEstadoActual!!).sortedWith(
                    compareBy<Issue> { 
                        when (it.estado) {
                            EstadoIssue.PENDIENTE -> 0
                            EstadoIssue.EN_PROCESO -> 1
                            EstadoIssue.FINALIZADO -> 2
                        }
                    }.thenBy { it.clienteNombre }
                )
            } else {
                issueManager.getAllIssues().sortedWith(
                    compareBy<Issue> { 
                        when (it.estado) {
                            EstadoIssue.PENDIENTE -> 0
                            EstadoIssue.EN_PROCESO -> 1
                            EstadoIssue.FINALIZADO -> 2
                        }
                    }.thenBy { it.clienteNombre }
                )
            }
            
            android.util.Log.d("ReporteSoporteActivity", "Issues obtenidos: ${issues.size}")
            
            if (issues.isEmpty()) {
                android.util.Log.d("ReporteSoporteActivity", "No hay issues para generar PDF")
                Toast.makeText(this, "No hay issues para generar PDF", Toast.LENGTH_SHORT).show()
                return
            }
            
            android.util.Log.d("ReporteSoporteActivity", "Llamando a PdfGeneratorSoporte...")
            val pdfGenerator = PdfGeneratorSoporte(this)
            val filePath = pdfGenerator.generarReporteSoporte(issues)
            
            android.util.Log.d("ReporteSoporteActivity", "Resultado: $filePath")
            
            if (filePath != null) {
                ultimoPdfGenerado = filePath
                Toast.makeText(this, "PDF generado exitosamente en:\n$filePath", Toast.LENGTH_LONG).show()
                mostrarDialogoAbrirPdf(filePath)
            } else {
                Toast.makeText(this, "Error al generar PDF", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("ReporteSoporteActivity", "Error al generar PDF: ${e.message}", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
    
    private fun compartirPdf() {
        if (ultimoPdfGenerado == null) {
            AlertDialog.Builder(this)
                .setTitle("Compartir PDF")
                .setMessage("Primero debes generar el PDF. ¿Deseas generarlo ahora?")
                .setPositiveButton("Generar y Compartir") { _, _ ->
                    generarPdfSoporte()
                    ultimoPdfGenerado?.let { compartirArchivoPdf(it) }
                }
                .setNegativeButton("Cancelar", null)
                .show()
            return
        }
        
        compartirArchivoPdf(ultimoPdfGenerado!!)
    }
    
    private fun compartirArchivoPdf(filePath: String) {
        try {
            val file = java.io.File(filePath)
            
            if (!file.exists()) {
                Toast.makeText(this, "El archivo PDF no existe. Genera uno nuevo.", Toast.LENGTH_LONG).show()
                return
            }
            
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "application/pdf"
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.putExtra(Intent.EXTRA_SUBJECT, "Reporte de Soporte")
            intent.putExtra(Intent.EXTRA_TEXT, "Adjunto el reporte de soporte generado desde la aplicación.")
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            
            startActivity(Intent.createChooser(intent, "Compartir Reporte PDF mediante..."))
        } catch (e: Exception) {
            Toast.makeText(this, "Error al compartir PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun mostrarDialogoAbrirPdf(filePath: String) {
        AlertDialog.Builder(this)
            .setTitle("Reporte Generado")
            .setMessage("¿Deseas abrir el reporte PDF ahora?")
            .setPositiveButton("Abrir PDF") { _, _ ->
                abrirPdf(filePath)
            }
            .setNegativeButton("Más tarde", null)
            .show()
    }
    
    private fun abrirPdf(filePath: String) {
        try {
            val file = java.io.File(filePath)
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/pdf")
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo abrir el PDF. Instala una aplicación para ver PDFs.", Toast.LENGTH_LONG).show()
        }
    }
}
