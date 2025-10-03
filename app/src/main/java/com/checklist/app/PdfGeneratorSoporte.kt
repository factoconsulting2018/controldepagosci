package com.checklist.app

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class PdfGeneratorSoporte(private val context: Context) {
    
    private val pageWidth = 595 // A4 width in points
    private val pageHeight = 842 // A4 height in points
    private val margin = 40
    
    fun generarReporteSoporte(issues: List<Issue>): String? {
        try {
            android.util.Log.d("PdfGeneratorSoporte", "Iniciando generación de PDF con ${issues.size} issues")
            val pdfDocument = PdfDocument()
            val paint = Paint()
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val sdfHora = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            
            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas
            
            var yPosition = margin + 40
            
            // Título principal
            paint.textSize = 22f
            paint.color = Color.BLACK
            paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
            canvas.drawText("REPORTE DE SOPORTE TÉCNICO", margin.toFloat(), yPosition.toFloat(), paint)
            
            yPosition += 30
            paint.textSize = 12f
            paint.typeface = android.graphics.Typeface.DEFAULT
            val fechaHoy = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            canvas.drawText("Fecha de Generación: $fechaHoy", margin.toFloat(), yPosition.toFloat(), paint)
            
            yPosition += 30
            
            // Resumen por estado
            val pendientes = issues.count { it.estado == EstadoIssue.PENDIENTE }
            val enProceso = issues.count { it.estado == EstadoIssue.EN_PROCESO }
            val finalizados = issues.count { it.estado == EstadoIssue.FINALIZADO }
            var totalIssuesGeneral = 0
            
            paint.textSize = 14f
            paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
            canvas.drawText("RESUMEN:", margin.toFloat(), yPosition.toFloat(), paint)
            
            yPosition += 20
            paint.textSize = 11f
            paint.typeface = android.graphics.Typeface.DEFAULT
            
            paint.color = Color.rgb(244, 67, 54) // red
            canvas.drawText("• Pendientes: $pendientes", (margin + 20).toFloat(), yPosition.toFloat(), paint)
            yPosition += 18
            
            paint.color = Color.rgb(255, 152, 0) // orange
            canvas.drawText("• En Proceso: $enProceso", (margin + 20).toFloat(), yPosition.toFloat(), paint)
            yPosition += 18
            
            paint.color = Color.rgb(76, 175, 80) // green
            canvas.drawText("• Finalizados: $finalizados", (margin + 20).toFloat(), yPosition.toFloat(), paint)
            yPosition += 18
            
            paint.color = Color.BLACK
            canvas.drawText("• Total Tickets: ${issues.size}", (margin + 20).toFloat(), yPosition.toFloat(), paint)
            
            yPosition += 30
            
            // Línea divisoria
            paint.strokeWidth = 2f
            canvas.drawLine(margin.toFloat(), yPosition.toFloat(), (pageWidth - margin).toFloat(), yPosition.toFloat(), paint)
            
            yPosition += 30
            
            issues.forEach { issue ->
                // Verificar si necesitamos nueva página
                if (yPosition > pageHeight - 280) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    yPosition = margin + 40
                }
                
                // Rectángulo de fondo según estado
                val colorFondo = when (issue.estado) {
                    EstadoIssue.PENDIENTE -> Color.argb(30, 244, 67, 54) // red transparente
                    EstadoIssue.EN_PROCESO -> Color.argb(30, 255, 152, 0) // orange transparente
                    EstadoIssue.FINALIZADO -> Color.argb(30, 76, 175, 80) // green transparente
                }
                paint.color = colorFondo
                paint.style = Paint.Style.FILL
                canvas.drawRect(
                    (margin - 5).toFloat(), 
                    (yPosition - 15).toFloat(), 
                    (pageWidth - margin + 5).toFloat(), 
                    (yPosition + 50).toFloat(), 
                    paint
                )
                paint.style = Paint.Style.FILL
                
                // Nombre del cliente
                paint.textSize = 15f
                paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
                paint.color = Color.BLACK
                canvas.drawText("Cliente: ${issue.clienteNombre}", margin.toFloat(), yPosition.toFloat(), paint)
                
                yPosition += 20
                
                // Estado y fecha de creación en la misma línea
                paint.textSize = 11f
                paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
                val (estadoTexto, estadoColor) = when (issue.estado) {
                    EstadoIssue.PENDIENTE -> Pair("● PENDIENTE", Color.rgb(244, 67, 54))
                    EstadoIssue.EN_PROCESO -> Pair("● EN PROCESO", Color.rgb(255, 152, 0))
                    EstadoIssue.FINALIZADO -> Pair("● FINALIZADO", Color.rgb(76, 175, 80))
                }
                paint.color = estadoColor
                canvas.drawText(estadoTexto, margin.toFloat(), yPosition.toFloat(), paint)
                
                // Fecha de creación
                paint.color = Color.DKGRAY
                paint.typeface = android.graphics.Typeface.DEFAULT
                paint.textSize = 10f
                val fechaCreacion = sdfHora.format(Date(issue.fechaCreacion))
                canvas.drawText("Creado: $fechaCreacion", (margin + 150).toFloat(), yPosition.toFloat(), paint)
                
                yPosition += 22
                paint.color = Color.BLACK
                paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
                paint.textSize = 11f
                canvas.drawText("Problemas Reportados:", margin.toFloat(), yPosition.toFloat(), paint)
                
                yPosition += 18
                paint.color = Color.BLACK
                
                // Issues del cliente
                paint.textSize = 10f
                paint.typeface = android.graphics.Typeface.DEFAULT
                paint.color = Color.BLACK
                
                issue.issues.forEachIndexed { index, issueItem ->
                    if (yPosition > pageHeight - 120) {
                        pdfDocument.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        yPosition = margin + 40
                    }
                    
                    // Número y título del problema
                    paint.textSize = 11f
                    paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
                    paint.color = Color.rgb(33, 33, 33)
                    canvas.drawText("${index + 1}. ${issueItem.titulo}", (margin + 15).toFloat(), yPosition.toFloat(), paint)
                    yPosition += 18
                    
                    // Descripción del problema
                    paint.textSize = 10f
                    paint.typeface = android.graphics.Typeface.DEFAULT
                    paint.color = Color.DKGRAY
                    canvas.drawText("Descripción:", (margin + 20).toFloat(), yPosition.toFloat(), paint)
                    yPosition += 14
                    
                    // Mensaje (dividir en múltiples líneas si es necesario)
                    paint.textSize = 9f
                    val mensajeLines = splitTextToFit(issueItem.mensaje, pageWidth - margin * 2 - 30, paint)
                    mensajeLines.forEach { line ->
                        if (yPosition > pageHeight - 80) {
                            pdfDocument.finishPage(page)
                            pageNumber++
                            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                            page = pdfDocument.startPage(pageInfo)
                            canvas = page.canvas
                            yPosition = margin + 40
                        }
                        canvas.drawText(line, (margin + 25).toFloat(), yPosition.toFloat(), paint)
                        yPosition += 13
                    }
                    
                    // Fecha del problema reportado
                    yPosition += 3
                    paint.textSize = 9f
                    paint.color = Color.rgb(100, 100, 100)
                    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC)
                    canvas.drawText("Fecha del Problema: ${sdf.format(Date(issueItem.fechaIssue))}", (margin + 20).toFloat(), yPosition.toFloat(), paint)
                    yPosition += 20
                    paint.color = Color.BLACK
                    paint.typeface = android.graphics.Typeface.DEFAULT
                }
                
                yPosition += 8
                
                // Línea divisoria más delgada
                paint.strokeWidth = 0.5f
                paint.color = Color.LTGRAY
                canvas.drawLine((margin + 10).toFloat(), yPosition.toFloat(), (pageWidth - margin - 10).toFloat(), yPosition.toFloat(), paint)
                
                yPosition += 12
                
                // Total por cliente con badge
                paint.textSize = 11f
                paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
                paint.color = Color.WHITE
                
                // Rectángulo de fondo para el total
                paint.style = Paint.Style.FILL
                paint.color = Color.rgb(21, 101, 192) // blue_700
                canvas.drawRect(
                    (margin + 10).toFloat(), 
                    (yPosition - 12).toFloat(), 
                    (margin + 165).toFloat(), 
                    (yPosition + 5).toFloat(), 
                    paint
                )
                
                paint.color = Color.WHITE
                canvas.drawText("Total Problemas: ${issue.getTotalIssues()}", (margin + 15).toFloat(), yPosition.toFloat(), paint)
                
                yPosition += 25
                totalIssuesGeneral += issue.getTotalIssues()
                
                // Línea divisoria más gruesa entre clientes
                paint.strokeWidth = 2f
                paint.color = Color.LTGRAY
                canvas.drawLine(margin.toFloat(), yPosition.toFloat(), (pageWidth - margin).toFloat(), yPosition.toFloat(), paint)
                
                yPosition += 30
                paint.color = Color.BLACK
                paint.style = Paint.Style.FILL
            }
            
            // Resumen final
            if (yPosition > pageHeight - 150) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPosition = margin + 40
            }
            
            yPosition += 20
            
            // Línea divisoria doble
            paint.strokeWidth = 3f
            paint.color = Color.BLACK
            canvas.drawLine(margin.toFloat(), yPosition.toFloat(), (pageWidth - margin).toFloat(), yPosition.toFloat(), paint)
            
            yPosition += 30
            
            // Título de resumen final
            paint.textSize = 16f
            paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
            paint.color = Color.BLACK
            canvas.drawText("RESUMEN FINAL", margin.toFloat(), yPosition.toFloat(), paint)
            
            yPosition += 25
            
            // Total de issues con fondo
            paint.style = Paint.Style.FILL
            paint.color = Color.rgb(76, 175, 80) // green
            canvas.drawRect(
                (margin + 20).toFloat(), 
                (yPosition - 20).toFloat(), 
                (pageWidth - margin - 20).toFloat(), 
                (yPosition + 15).toFloat(), 
                paint
            )
            
            paint.textSize = 18f
            paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
            paint.color = Color.WHITE
            canvas.drawText("Total de Problemas Reportados: $totalIssuesGeneral", (margin + 30).toFloat(), yPosition.toFloat(), paint)
            
            yPosition += 30
            
            // Desglose por estado
            paint.textSize = 12f
            paint.typeface = android.graphics.Typeface.DEFAULT
            paint.color = Color.BLACK
            canvas.drawText("Total de Clientes: ${issues.size}", (margin + 30).toFloat(), yPosition.toFloat(), paint)
            
            yPosition += 20
            canvas.drawText("Pendientes: $pendientes | En Proceso: $enProceso | Finalizados: $finalizados", (margin + 30).toFloat(), yPosition.toFloat(), paint)
            
            pdfDocument.finishPage(page)
            
            // Guardar PDF en el directorio de documentos de la app
            val fileName = "Reporte_Soporte_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pdf"
            val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            android.util.Log.d("PdfGeneratorSoporte", "Directorio: $directory")
            
            val file = File(directory, fileName)
            android.util.Log.d("PdfGeneratorSoporte", "Ruta completa del archivo: ${file.absolutePath}")
            
            // Crear directorio si no existe
            if (!directory!!.exists()) {
                directory.mkdirs()
                android.util.Log.d("PdfGeneratorSoporte", "Directorio creado")
            }
            
            val fos = FileOutputStream(file)
            pdfDocument.writeTo(fos)
            pdfDocument.close()
            fos.close()
            
            android.util.Log.d("PdfGeneratorSoporte", "PDF generado exitosamente: ${file.absolutePath}")
            return file.absolutePath
        } catch (e: Exception) {
            android.util.Log.e("PdfGeneratorSoporte", "Error al generar PDF: ${e.message}", e)
            e.printStackTrace()
            return null
        }
    }
    
    private fun splitTextToFit(text: String, maxWidth: Int, paint: Paint): List<String> {
        val lines = mutableListOf<String>()
        val words = text.split(" ")
        var currentLine = ""
        
        words.forEach { word ->
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val width = paint.measureText(testLine)
            
            if (width > maxWidth) {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                    currentLine = word
                } else {
                    // Si una sola palabra es más larga que maxWidth, dividirla
                    lines.add(word)
                }
            } else {
                currentLine = testLine
            }
        }
        
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        
        return lines
    }
}

