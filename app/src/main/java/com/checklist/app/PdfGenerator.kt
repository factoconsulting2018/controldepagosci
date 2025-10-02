package com.checklist.app

import android.content.Context
import android.os.Environment
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class PdfGenerator(private val context: Context) {
    private val clienteManager = ClienteManager(context)
    
    fun generateQuestionsReport(questions: List<Question>, ejecutivos: List<Ejecutivo>): String? {
        return generateQuestionsReportWithInfo(questions, ejecutivos, null, null)
    }
    
    fun generateQuestionsReportWithInfo(questions: List<Question>, ejecutivos: List<Ejecutivo>, reportInfo: ReportInfo?): String? {
        return generateQuestionsReportWithInfo(questions, ejecutivos, reportInfo, null)
    }
    
    fun generateQuestionsReportWithEstados(questions: List<Question>, ejecutivos: List<Ejecutivo>, reportInfo: ReportInfo?, checklistTitle: String?, clienteEstadoManager: ClienteEstadoManager): String? {
        return try {
            val fileName = "Reporte_Clientes_Estados_${getCurrentDate()}.pdf"
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
            
            val pdfWriter = PdfWriter(FileOutputStream(file))
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument)
            
            // Título del reporte (usar título personalizado si está disponible)
            val reportTitle = if (!checklistTitle.isNullOrEmpty()) {
                checklistTitle.uppercase()
            } else {
                "REPORTE DE CLIENTES CON ESTADOS"
            }
            
            val title = Paragraph(reportTitle)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(20f)
                .setBold()
                .setMarginBottom(10f)
            document.add(title)
            
            // Información del formulario si está disponible
            if (reportInfo != null) {
                document.add(Paragraph("").setMarginBottom(10f))
                
                val infoTitle = Paragraph("INFORMACIÓN DEL REPORTE")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(14f)
                    .setBold()
                    .setBackgroundColor(ColorConstants.BLACK)
                    .setFontColor(ColorConstants.WHITE)
                    .setMarginBottom(10f)
                    .setPadding(10f)
                document.add(infoTitle)
                
                val infoTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 2f)))
                    .useAllAvailableWidth()
                    .setMarginBottom(20f)
                
                // Información del reporte
                infoTable.addCell(Cell().add(Paragraph("Ejecutivo:").setBold()))
                infoTable.addCell(Cell().add(Paragraph(reportInfo.ejecutivo)))
                
                infoTable.addCell(Cell().add(Paragraph("Comentarios:").setBold()))
                infoTable.addCell(Cell().add(Paragraph(if (reportInfo.comments.isNotEmpty()) reportInfo.comments else "Sin comentarios")))
                
                document.add(infoTable)
            }
            
            // Fecha de generación
            val date = Paragraph("Generado el: ${getCurrentDateTime()}")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(12f)
                .setMarginBottom(20f)
            document.add(date)
            
            // Separar clientes por estado usando la tabla de estados
            val pendingClients = questions.filter { question ->
                val clienteId = question.clienteId ?: 0L
                clienteEstadoManager.isClientePendiente(clienteId)
            }
            val paidClients = questions.filter { question ->
                val clienteId = question.clienteId ?: 0L
                clienteEstadoManager.isClientePagado(clienteId)
            }
            
            // Log para verificar separación de estados
            android.util.Log.d("PdfGenerator", "Separación de estados desde tabla - Total: ${questions.size}, Pendientes: ${pendingClients.size}, Pagados: ${paidClients.size}")
            
            // Log detallado de cada cliente
            questions.forEach { question ->
                val clienteId = question.clienteId ?: 0L
                val estado = clienteEstadoManager.getEstadoString(clienteId)
                android.util.Log.d("PdfGenerator", "Cliente: ${question.title}, Estado desde tabla: $estado")
            }
            
            // SECCIÓN 1: CLIENTES PENDIENTES
            if (pendingClients.isNotEmpty()) {
                val pendingTitle = Paragraph("NO PRESENTAR DECLARACIONES, CLIENTES PENDIENTES")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(16f)
                    .setBold()
                    .setBackgroundColor(ColorConstants.RED)
                    .setFontColor(ColorConstants.WHITE)
                    .setMarginTop(20f)
                    .setMarginBottom(15f)
                    .setPadding(10f)
                document.add(pendingTitle)
                
                // Tabla de clientes pendientes
                val pendingTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 3f, 2f, 2f, 1f)))
                    .useAllAvailableWidth()
                    .setMarginBottom(20f)
                
                // Encabezados de la tabla
                val headers = arrayOf("Pos", "Cliente", "Teléfono", "Ejecutivo", "Estado")
                for (header in headers) {
                    val cell = Cell().add(Paragraph(header).setBold())
                        .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                        .setTextAlignment(TextAlignment.CENTER)
                    pendingTable.addCell(cell)
                }
                
                // Agregar clientes pendientes a la tabla
                for (question in pendingClients) {
                    val ejecutivo = ejecutivos.find { it.id == question.ejecutivoId }
                    val ejecutivoName = ejecutivo?.name ?: "Sin ejecutivo"
                    
                    // Obtener información del cliente
                    val cliente = clienteManager.getClienteById(question.clienteId ?: -1)
                    val telefono = cliente?.telefono ?: "Sin teléfono"
                    
                    // Obtener estado desde la tabla
                    val clienteId = question.clienteId ?: 0L
                    val estado = clienteEstadoManager.getEstadoString(clienteId)
                    
                    // Log para verificar teléfonos en PDF
                    android.util.Log.d("PdfGenerator", "Cliente Pendiente: ${question.title}, Teléfono: '$telefono', Estado: $estado")
                    
                    // Posición
                    pendingTable.addCell(Cell().add(Paragraph(question.position.toString()))
                        .setTextAlignment(TextAlignment.CENTER))
                    
                    // Cliente (título + subtítulo)
                    val clientText = if (question.subtitle.isNotEmpty()) {
                        "${question.title}\nCédula: ${question.subtitle}"
                    } else {
                        question.title
                    }
                    pendingTable.addCell(Cell().add(Paragraph(clientText)))
                    
                    // Teléfono
                    pendingTable.addCell(Cell().add(Paragraph(telefono))
                        .setTextAlignment(TextAlignment.CENTER))
                    
                    // Ejecutivo
                    pendingTable.addCell(Cell().add(Paragraph(ejecutivoName))
                        .setTextAlignment(TextAlignment.CENTER))
                    
                    // Estado desde tabla
                    val status = estado
                    val statusColor = ColorConstants.RED
                    pendingTable.addCell(Cell().add(Paragraph(status).setFontColor(statusColor))
                        .setTextAlignment(TextAlignment.CENTER))
                }
                
                document.add(pendingTable)
            }
            
            // SECCIÓN 2: CLIENTES PAGADOS
            if (paidClients.isNotEmpty()) {
                val paidTitle = Paragraph("CLIENTES PAGADOS")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(16f)
                    .setBold()
                    .setBackgroundColor(DeviceRgb(0, 100, 0)) // Verde oscuro
                    .setFontColor(ColorConstants.WHITE)
                    .setMarginTop(20f)
                    .setMarginBottom(15f)
                    .setPadding(10f)
                document.add(paidTitle)
                
                // Tabla de clientes pagados
                val paidTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 3f, 2f, 2f, 1f)))
                    .useAllAvailableWidth()
                    .setMarginBottom(20f)
                
                // Encabezados de la tabla
                val headers = arrayOf("Pos", "Cliente", "Teléfono", "Ejecutivo", "Estado")
                for (header in headers) {
                    val cell = Cell().add(Paragraph(header).setBold())
                        .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                        .setTextAlignment(TextAlignment.CENTER)
                    paidTable.addCell(cell)
                }
                
                // Agregar clientes pagados a la tabla
                for (question in paidClients) {
                    val ejecutivo = ejecutivos.find { it.id == question.ejecutivoId }
                    val ejecutivoName = ejecutivo?.name ?: "Sin ejecutivo"
                    
                    // Obtener información del cliente
                    val cliente = clienteManager.getClienteById(question.clienteId ?: -1)
                    val telefono = cliente?.telefono ?: "Sin teléfono"
                    
                    // Obtener estado desde la tabla
                    val clienteId = question.clienteId ?: 0L
                    val estado = clienteEstadoManager.getEstadoString(clienteId)
                    
                    // Log para verificar teléfonos en PDF
                    android.util.Log.d("PdfGenerator", "Cliente Pagado: ${question.title}, Teléfono: '$telefono', Estado: $estado")
                    
                    // Posición
                    paidTable.addCell(Cell().add(Paragraph(question.position.toString()))
                        .setTextAlignment(TextAlignment.CENTER))
                    
                    // Cliente (título + subtítulo)
                    val clientText = if (question.subtitle.isNotEmpty()) {
                        "${question.title}\nCédula: ${question.subtitle}"
                    } else {
                        question.title
                    }
                    paidTable.addCell(Cell().add(Paragraph(clientText)))
                    
                    // Teléfono
                    paidTable.addCell(Cell().add(Paragraph(telefono))
                        .setTextAlignment(TextAlignment.CENTER))
                    
                    // Ejecutivo
                    paidTable.addCell(Cell().add(Paragraph(ejecutivoName))
                        .setTextAlignment(TextAlignment.CENTER))
                    
                    // Estado desde tabla
                    val status = estado
                    val statusColor = DeviceRgb(0, 100, 0) // Verde oscuro
                    paidTable.addCell(Cell().add(Paragraph(status).setFontColor(statusColor))
                        .setTextAlignment(TextAlignment.CENTER))
                }
                
                document.add(paidTable)
            }
            
            // Resumen
            val pendingCount = pendingClients.size
            val paidCount = paidClients.size
            val totalCount = questions.size
            
            val summary = Paragraph("\n\nRESUMEN:")
                .setBold()
                .setFontSize(14f)
                .setMarginTop(20f)
            document.add(summary)
            
            val summaryText = """
                Total de clientes: $totalCount
                Clientes pendientes: $pendingCount
                Clientes pagados: $paidCount
                Porcentaje de pagados: ${if (totalCount > 0) (paidCount * 100) / totalCount else 0}%
            """.trimIndent()
            
            document.add(Paragraph(summaryText).setFontSize(12f))
            
            document.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun generateQuestionsReportWithInfo(questions: List<Question>, ejecutivos: List<Ejecutivo>, reportInfo: ReportInfo?, checklistTitle: String?): String? {
        return try {
            val fileName = "Reporte_Clientes_${getCurrentDate()}.pdf"
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
            
            val pdfWriter = PdfWriter(FileOutputStream(file))
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument)
            
            // Título del reporte (usar título personalizado si está disponible)
            val reportTitle = if (!checklistTitle.isNullOrEmpty()) {
                checklistTitle.uppercase()
            } else {
                "REPORTE DE CLIENTES"
            }
            
            val title = Paragraph(reportTitle)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(20f)
                .setBold()
                .setMarginBottom(10f)
            document.add(title)
            
            // Información del formulario si está disponible
            if (reportInfo != null) {
                document.add(Paragraph("").setMarginBottom(10f))
                
                val infoTitle = Paragraph("INFORMACIÓN DEL REPORTE")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(14f)
                    .setBold()
                    .setBackgroundColor(ColorConstants.BLACK)
                    .setFontColor(ColorConstants.WHITE)
                    .setMarginBottom(10f)
                    .setPadding(10f)
                document.add(infoTitle)
                
                val infoTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 2f)))
                    .useAllAvailableWidth()
                    .setMarginBottom(20f)
                
                // Información del reporte
                infoTable.addCell(Cell().add(Paragraph("Ejecutivo:").setBold()))
                infoTable.addCell(Cell().add(Paragraph(reportInfo.ejecutivo)))
                
                infoTable.addCell(Cell().add(Paragraph("Comentarios:").setBold()))
                infoTable.addCell(Cell().add(Paragraph(if (reportInfo.comments.isNotEmpty()) reportInfo.comments else "Sin comentarios")))
                
                document.add(infoTable)
            }
            
            // Fecha de generación
            val date = Paragraph("Generado el: ${getCurrentDateTime()}")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(12f)
                .setMarginBottom(20f)
            document.add(date)
            
            // Separar clientes por estado de pago
            val pendingClients = questions.filter { !it.isCompleted }
            val paidClients = questions.filter { it.isCompleted }
            
            // Log para verificar separación de estados
            android.util.Log.d("PdfGenerator", "Separación de estados - Total: ${questions.size}, Pendientes: ${pendingClients.size}, Pagados: ${paidClients.size}")
            
            // Log detallado de cada cliente
            questions.forEach { question ->
                android.util.Log.d("PdfGenerator", "Cliente: ${question.title}, isCompleted: ${question.isCompleted}, Estado: ${if (question.isCompleted) "PAGADO" else "PENDIENTE"}")
            }
            
            // SECCIÓN 1: CLIENTES PENDIENTES
            if (pendingClients.isNotEmpty()) {
                val pendingTitle = Paragraph("NO PRESENTAR DECLARACIONES, CLIENTES PENDIENTES")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(16f)
                    .setBold()
                    .setBackgroundColor(ColorConstants.RED)
                    .setFontColor(ColorConstants.WHITE)
                    .setMarginTop(20f)
                    .setMarginBottom(15f)
                    .setPadding(10f)
                document.add(pendingTitle)
                
                // Tabla de clientes pendientes
                val pendingTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 3f, 2f, 2f, 1f)))
                    .useAllAvailableWidth()
                    .setMarginBottom(20f)
                
                // Encabezados de la tabla
                val headers = arrayOf("Pos", "Cliente", "Teléfono", "Ejecutivo", "Estado")
                for (header in headers) {
                    val cell = Cell().add(Paragraph(header).setBold())
                        .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                        .setTextAlignment(TextAlignment.CENTER)
                    pendingTable.addCell(cell)
                }
                
                // Agregar clientes pendientes a la tabla
                for (question in pendingClients) {
                    val ejecutivo = ejecutivos.find { it.id == question.ejecutivoId }
                    val ejecutivoName = ejecutivo?.name ?: "Sin ejecutivo"
                    
                    // Obtener información del cliente
                    val cliente = clienteManager.getClienteById(question.clienteId ?: -1)
                    val telefono = cliente?.telefono ?: "Sin teléfono"
                    
                    // Log para verificar teléfonos en PDF
                    android.util.Log.d("PdfGenerator", "Cliente Pendiente: ${question.title}, Teléfono: '$telefono'")
                    
                    // Posición
                    pendingTable.addCell(Cell().add(Paragraph(question.position.toString()))
                        .setTextAlignment(TextAlignment.CENTER))
                    
                    // Cliente (título + subtítulo)
                    val clientText = if (question.subtitle.isNotEmpty()) {
                        "${question.title}\nCédula: ${question.subtitle}"
                    } else {
                        question.title
                    }
                    pendingTable.addCell(Cell().add(Paragraph(clientText)))
                    
                    // Teléfono
                    pendingTable.addCell(Cell().add(Paragraph(telefono))
                        .setTextAlignment(TextAlignment.CENTER))
                    
                    // Ejecutivo
                    pendingTable.addCell(Cell().add(Paragraph(ejecutivoName))
                        .setTextAlignment(TextAlignment.CENTER))
                    
                    // Estado
                    val status = "Pendiente"
                    val statusColor = ColorConstants.RED
                    pendingTable.addCell(Cell().add(Paragraph(status).setFontColor(statusColor))
                        .setTextAlignment(TextAlignment.CENTER))
                }
                
                document.add(pendingTable)
            }
            
            // SECCIÓN 2: CLIENTES PAGADOS
            if (paidClients.isNotEmpty()) {
                val paidTitle = Paragraph("CLIENTES PAGADOS")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(16f)
                    .setBold()
                    .setBackgroundColor(DeviceRgb(0, 100, 0)) // Verde oscuro
                    .setFontColor(ColorConstants.WHITE)
                    .setMarginTop(20f)
                    .setMarginBottom(15f)
                    .setPadding(10f)
                document.add(paidTitle)
                
                // Tabla de clientes pagados
                val paidTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 3f, 2f, 2f, 1f)))
                    .useAllAvailableWidth()
                    .setMarginBottom(20f)
                
                // Encabezados de la tabla
                val headers = arrayOf("Pos", "Cliente", "Teléfono", "Ejecutivo", "Estado")
                for (header in headers) {
                    val cell = Cell().add(Paragraph(header).setBold())
                        .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                        .setTextAlignment(TextAlignment.CENTER)
                    paidTable.addCell(cell)
                }
                
                // Agregar clientes pagados a la tabla
                for (question in paidClients) {
                    val ejecutivo = ejecutivos.find { it.id == question.ejecutivoId }
                    val ejecutivoName = ejecutivo?.name ?: "Sin ejecutivo"
                    
                    // Obtener información del cliente
                    val cliente = clienteManager.getClienteById(question.clienteId ?: -1)
                    val telefono = cliente?.telefono ?: "Sin teléfono"
                    
                    // Log para verificar teléfonos en PDF
                    android.util.Log.d("PdfGenerator", "Cliente Pagado: ${question.title}, Teléfono: '$telefono'")
                    
                    // Posición
                    paidTable.addCell(Cell().add(Paragraph(question.position.toString()))
                        .setTextAlignment(TextAlignment.CENTER))
                    
                    // Cliente (título + subtítulo)
                    val clientText = if (question.subtitle.isNotEmpty()) {
                        "${question.title}\nCédula: ${question.subtitle}"
                    } else {
                        question.title
                    }
                    paidTable.addCell(Cell().add(Paragraph(clientText)))
                    
                    // Teléfono
                    paidTable.addCell(Cell().add(Paragraph(telefono))
                        .setTextAlignment(TextAlignment.CENTER))
                    
                    // Ejecutivo
                    paidTable.addCell(Cell().add(Paragraph(ejecutivoName))
                        .setTextAlignment(TextAlignment.CENTER))
                    
                    // Estado
                    val status = "Pagado"
                    val statusColor = DeviceRgb(0, 100, 0) // Verde oscuro
                    paidTable.addCell(Cell().add(Paragraph(status).setFontColor(statusColor))
                        .setTextAlignment(TextAlignment.CENTER))
                }
                
                document.add(paidTable)
            }
            
            // Resumen
            val pendingCount = pendingClients.size
            val paidCount = paidClients.size
            val totalCount = questions.size
            
            val summary = Paragraph("\n\nRESUMEN:")
                .setBold()
                .setFontSize(14f)
                .setMarginTop(20f)
            document.add(summary)
            
            val summaryText = """
                Total de clientes: $totalCount
                Clientes pendientes: $pendingCount
                Clientes pagados: $paidCount
                Porcentaje de pagados: ${if (totalCount > 0) (paidCount * 100) / totalCount else 0}%
            """.trimIndent()
            
            document.add(Paragraph(summaryText).setFontSize(12f))
            
            document.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return sdf.format(Date())
    }
    
    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }
}
