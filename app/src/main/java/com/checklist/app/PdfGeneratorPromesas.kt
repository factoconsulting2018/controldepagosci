package com.checklist.app

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class PdfGeneratorPromesas(private val context: Context) {
    
    private val pageWidth = 595 // A4 width in points
    private val pageHeight = 842 // A4 height in points
    private val margin = 40
    
    fun generarReportePromesas(promesas: List<Promesa>): String? {
        try {
            val pdfDocument = PdfDocument()
            val paint = Paint()
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CR"))
            
            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas
            
            var yPosition = margin + 40
            
            // Título
            paint.textSize = 20f
            paint.color = Color.BLACK
            paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
            canvas.drawText("REPORTE DE PROMESAS DE PAGO", margin.toFloat(), yPosition.toFloat(), paint)
            
            yPosition += 30
            paint.textSize = 12f
            paint.typeface = android.graphics.Typeface.DEFAULT
            val fechaHoy = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            canvas.drawText("Fecha: $fechaHoy", margin.toFloat(), yPosition.toFloat(), paint)
            
            yPosition += 40
            
            var totalGeneral = 0.0
            
            promesas.forEach { promesa ->
                // Verificar si necesitamos nueva página
                if (yPosition > pageHeight - 200) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    yPosition = margin + 40
                }
                
                // Nombre del cliente
                paint.textSize = 14f
                paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
                paint.color = Color.rgb(46, 125, 50) // green_700
                canvas.drawText(promesa.clienteNombre, margin.toFloat(), yPosition.toFloat(), paint)
                
                yPosition += 25
                
                // Headers de la tabla
                paint.textSize = 10f
                paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
                paint.color = Color.BLACK
                canvas.drawText("Título", (margin + 10).toFloat(), yPosition.toFloat(), paint)
                canvas.drawText("Monto", (margin + 250).toFloat(), yPosition.toFloat(), paint)
                canvas.drawText("Fecha", (margin + 400).toFloat(), yPosition.toFloat(), paint)
                
                yPosition += 20
                
                // Línea
                paint.strokeWidth = 1f
                canvas.drawLine(margin.toFloat(), yPosition.toFloat(), (pageWidth - margin).toFloat(), yPosition.toFloat(), paint)
                
                yPosition += 15
                
                // Promesas de pago
                paint.textSize = 10f
                paint.typeface = android.graphics.Typeface.DEFAULT
                
                promesa.promesasPago.forEach { promesaPago ->
                    if (yPosition > pageHeight - 100) {
                        pdfDocument.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        yPosition = margin + 40
                    }
                    
                    canvas.drawText(promesaPago.titulo, (margin + 10).toFloat(), yPosition.toFloat(), paint)
                    canvas.drawText(numberFormat.format(promesaPago.monto), (margin + 250).toFloat(), yPosition.toFloat(), paint)
                    canvas.drawText(sdf.format(Date(promesaPago.fecha)), (margin + 400).toFloat(), yPosition.toFloat(), paint)
                    
                    yPosition += 18
                }
                
                yPosition += 10
                
                // Línea divisoria
                canvas.drawLine(margin.toFloat(), yPosition.toFloat(), (pageWidth - margin).toFloat(), yPosition.toFloat(), paint)
                
                yPosition += 15
                
                // Total por cliente
                paint.textSize = 12f
                paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
                paint.color = Color.rgb(21, 101, 192) // blue_700
                canvas.drawText("Total por Cobrar: ${numberFormat.format(promesa.getTotalMonto())}", (margin + 250).toFloat(), yPosition.toFloat(), paint)
                
                yPosition += 30
                totalGeneral += promesa.getTotalMonto()
                
                paint.color = Color.BLACK
            }
            
            // Total general
            if (yPosition > pageHeight - 100) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPosition = margin + 40
            }
            
            yPosition += 20
            
            paint.strokeWidth = 2f
            canvas.drawLine(margin.toFloat(), yPosition.toFloat(), (pageWidth - margin).toFloat(), yPosition.toFloat(), paint)
            
            yPosition += 25
            
            paint.textSize = 16f
            paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
            paint.color = Color.rgb(76, 175, 80) // green_500
            canvas.drawText("TOTAL ADEUDADO: ${numberFormat.format(totalGeneral)}", (margin + 100).toFloat(), yPosition.toFloat(), paint)
            
            pdfDocument.finishPage(page)
            
            // Guardar PDF en el directorio de documentos de la app (igual que PdfGenerator)
            val fileName = "Reporte_Promesas_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pdf"
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
            
            val fos = FileOutputStream(file)
            pdfDocument.writeTo(fos)
            pdfDocument.close()
            fos.close()
            
            return file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}

