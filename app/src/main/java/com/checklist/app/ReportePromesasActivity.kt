package com.checklist.app

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class ReportePromesasActivity : AppCompatActivity() {
    
    private lateinit var promesaManager: PromesaManager
    private lateinit var reporteContainer: LinearLayout
    private lateinit var fabGenerarPdf: ExtendedFloatingActionButton
    private lateinit var fabCompartirPdf: ExtendedFloatingActionButton
    private var ultimoPdfGenerado: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_reporte_promesas)
        
        promesaManager = PromesaManager(this)
        reporteContainer = findViewById(R.id.reporteContainer)
        fabGenerarPdf = findViewById(R.id.fabGenerarPdf)
        fabCompartirPdf = findViewById(R.id.fabCompartirPdf)
        
        generarReporte()
        
        fabGenerarPdf.setOnClickListener {
            generarPdfPromesas()
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
    
    private fun generarReporte() {
        val promesas = promesaManager.getAllPromesas().sortedBy { it.clienteNombre }
        val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CR"))
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        
        var totalGeneral = 0.0
        
        promesas.forEach { promesa ->
            // Card para cada cliente
            val clienteCard = com.google.android.material.card.MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 16)
                }
                radius = 12f
                cardElevation = 4f
                setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
            }
            
            val contentLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 16, 16, 16)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            
            // Nombre del cliente
            val nombreText = TextView(this).apply {
                text = promesa.clienteNombre
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(context, R.color.green_700))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 12)
                }
            }
            contentLayout.addView(nombreText)
            
            // Tabla de promesas
            promesa.promesasPago.forEach { promesaPago ->
                val promesaLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 4, 0, 4)
                    }
                }
                
                val tituloText = TextView(this).apply {
                    text = promesaPago.titulo
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(context, R.color.black))
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1.5f
                    )
                }
                
                val montoText = TextView(this).apply {
                    text = numberFormat.format(promesaPago.monto)
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(context, R.color.blue_700))
                    setTypeface(null, Typeface.BOLD)
                    gravity = Gravity.END
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                }
                
                val fechaText = TextView(this).apply {
                    text = sdf.format(Date(promesaPago.fecha))
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(context, R.color.gray_600))
                    gravity = Gravity.END
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                }
                
                promesaLayout.addView(tituloText)
                promesaLayout.addView(montoText)
                promesaLayout.addView(fechaText)
                
                contentLayout.addView(promesaLayout)
            }
            
            // Línea divisoria
            val divider = android.view.View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    2
                ).apply {
                    setMargins(0, 8, 0, 8)
                }
                setBackgroundColor(ContextCompat.getColor(context, R.color.gray_300))
            }
            contentLayout.addView(divider)
            
            // Total por cliente
            val totalText = TextView(this).apply {
                text = "Total por Cobrar: ${numberFormat.format(promesa.getTotalMonto())}"
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(context, R.color.blue_700))
                gravity = Gravity.END
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            contentLayout.addView(totalText)
            
            clienteCard.addView(contentLayout)
            reporteContainer.addView(clienteCard)
            
            totalGeneral += promesa.getTotalMonto()
        }
        
        // Total general al final
        val totalGeneralCard = com.google.android.material.card.MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 16)
            }
            radius = 12f
            cardElevation = 6f
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.green_500))
        }
        
        val totalGeneralText = TextView(this).apply {
            text = "TOTAL ADEUDADO: ${numberFormat.format(totalGeneral)}"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.white))
            gravity = Gravity.CENTER
            setPadding(16, 24, 16, 24)
        }
        
        totalGeneralCard.addView(totalGeneralText)
        reporteContainer.addView(totalGeneralCard)
    }
    
    private fun generarPdfPromesas() {
        try {
            val promesas = promesaManager.getAllPromesas().sortedBy { it.clienteNombre }
            
            if (promesas.isEmpty()) {
                Toast.makeText(this, "No hay promesas para generar PDF", Toast.LENGTH_SHORT).show()
                return
            }
            
            val pdfGenerator = PdfGeneratorPromesas(this)
            val filePath = pdfGenerator.generarReportePromesas(promesas)
            
            if (filePath != null) {
                ultimoPdfGenerado = filePath
                Toast.makeText(this, "PDF generado exitosamente", Toast.LENGTH_SHORT).show()
                mostrarDialogoAbrirPdf(filePath)
            } else {
                Toast.makeText(this, "Error al generar PDF", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
    
    private fun compartirPdf() {
        if (ultimoPdfGenerado == null) {
            // Si no hay PDF generado, generarlo primero
            AlertDialog.Builder(this)
                .setTitle("Compartir PDF")
                .setMessage("Primero debes generar el PDF. ¿Deseas generarlo ahora?")
                .setPositiveButton("Generar y Compartir") { _, _ ->
                    generarPdfPromesas()
                    // Después de generar, compartir automáticamente
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
            intent.putExtra(Intent.EXTRA_SUBJECT, "Reporte de Promesas de Pago")
            intent.putExtra(Intent.EXTRA_TEXT, "Adjunto el reporte de promesas de pago generado desde la aplicación.")
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

