package com.checklist.app

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.content.FileProvider
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.checklist.app.databinding.ActivityConfigBinding
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class ConfigActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityConfigBinding
    private lateinit var prefs: SharedPreferences
    private lateinit var reportManager: ReportManager
    private lateinit var clienteManager: ClienteManager
    private lateinit var ejecutivoManager: EjecutivoManager
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 1002
        private const val REQUEST_CODE_IMPORT_JSON = 1003
        private const val REQUEST_CODE_IMPORT_EXCEL = 1004
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ocultar la ActionBar
        supportActionBar?.hide()
        
        binding = ActivityConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefs = getSharedPreferences("checklist_prefs", MODE_PRIVATE)
        reportManager = ReportManager(this)
        clienteManager = ClienteManager(this)
        ejecutivoManager = EjecutivoManager(this)
        
        setupClickListeners()
        loadCurrentSettings()
        requestStoragePermission()
        updateImportStatus()
    }
    
    private fun saveOrderPreference(isPendienteFirst: Boolean) {
        prefs.edit().putBoolean("client_initial_state_pendiente", isPendienteFirst).apply()
        val stateText = if (isPendienteFirst) "PENDIENTE/PAGADO" else "PAGADO/PENDIENTE"
        
        // Actualizar todos los clientes existentes según el nuevo estado
        updateAllClientsState(isPendienteFirst)
        
        // Notificar a MainActivity que debe actualizar la UI
        val intent = Intent("com.checklist.app.CONFIG_CHANGED")
        intent.putExtra("refresh_ui", true)
        sendBroadcast(intent)
        
        Toast.makeText(this, "Estado cambiado a: $stateText. Todos los clientes actualizados.", Toast.LENGTH_LONG).show()
    }
    
    private fun updateToggleButtonColor(isPendienteFirst: Boolean) {
        if (isPendienteFirst) {
            // PENDIENTE/PAGADO - Color rojo
            binding.orderToggleButton.setBackgroundColor(android.graphics.Color.parseColor("#FFD32F2F"))
        } else {
            // PAGADO/PENDIENTE - Color verde
            binding.orderToggleButton.setBackgroundColor(android.graphics.Color.parseColor("#FF4CAF50"))
        }
    }
    
    private fun updateAllClientsState(isPendienteFirst: Boolean) {
        try {
            val questionManager = QuestionManager(this)
            val clienteEstadoManager = ClienteEstadoManager(this)
            val allQuestions = questionManager.getAllQuestions()
            var updatedCount = 0
            
            // Determinar el nuevo estado
            val nuevoEstado = if (isPendienteFirst) {
                ClienteEstadoManager.ESTADO_PENDIENTE
            } else {
                ClienteEstadoManager.ESTADO_PAGADO
            }
            
            // Actualizar tabla de estados
            clienteEstadoManager.updateAllEstados(nuevoEstado)
            
            // Actualizar también las preguntas para mantener compatibilidad
            for (question in allQuestions) {
                val newCompletedState = !isPendienteFirst // Si es PAGADO/PENDIENTE, marcar como completado
                if (question.isCompleted != newCompletedState) {
                    val updatedQuestion = question.copy(isCompleted = newCompletedState)
                    questionManager.updateQuestion(updatedQuestion)
                    updatedCount++
                }
            }
            
            android.util.Log.d("ConfigActivity", "updateAllClientsState: $updatedCount clientes actualizados, nuevo estado: $nuevoEstado")
            
        } catch (e: Exception) {
            android.util.Log.e("ConfigActivity", "Error actualizando estados de clientes", e)
            Toast.makeText(this, "Error al actualizar estados: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun showDeleteAllClientsConfirmation() {
        val clientesCount = clienteManager.getAllClientes().size
        
        if (clientesCount == 0) {
            Toast.makeText(this, "No hay clientes para eliminar", Toast.LENGTH_SHORT).show()
            return
        }
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Eliminar Todos los Clientes")
            .setMessage("¿Estás seguro de que quieres eliminar TODOS los $clientesCount clientes?\n\nEsta acción NO se puede deshacer.")
            .setPositiveButton("ELIMINAR TODOS") { _, _ ->
                deleteAllClients()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun deleteAllClients() {
        try {
            val clientesCount = clienteManager.getAllClientes().size
            
            // Eliminar todos los clientes
            clienteManager.deleteAllClientes()
            
            // También eliminar todas las preguntas asociadas
            val questionManager = QuestionManager(this)
            val allQuestions = questionManager.getAllQuestions()
            allQuestions.forEach { question ->
                questionManager.deleteQuestion(question)
            }
            
            Toast.makeText(this, "Se eliminaron $clientesCount clientes y sus preguntas asociadas", Toast.LENGTH_LONG).show()
            
            // Actualizar el estado de importación
            updateImportStatus()
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error al eliminar clientes: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
    
    private fun setupClickListeners() {
        binding.saveButton.setOnClickListener {
            saveSettings()
        }
        
        binding.backupButton.setOnClickListener {
            createBackup()
        }
        
        binding.restoreButton.setOnClickListener {
            restoreFromBackup()
        }
        
        binding.backFab.setOnClickListener {
            finish()
        }
        
        binding.importClientsButton.setOnClickListener {
            importClientsFromJson()
        }
        
        binding.downloadTemplateButton.setOnClickListener {
            downloadExcelTemplate()
        }
        
        binding.verifyImportButton.setOnClickListener {
            verifyImportStatus()
        }
        
        binding.deleteAllClientsButton.setOnClickListener {
            showDeleteAllClientsConfirmation()
        }
        
        binding.orderToggleButton.setOnCheckedChangeListener { _, isChecked ->
            saveOrderPreference(isChecked)
            updateToggleButtonColor(isChecked)
        }
    }
    
    private fun loadCurrentSettings() {
        val currentTitle = prefs.getString("checklist_title", "Preguntas del Checklist")
        binding.titleEditText.setText(currentTitle)
        
        // Cargar preferencia del estado inicial de clientes (siempre inicia en PENDIENTE/PAGADO)
        val isPendienteFirst = prefs.getBoolean("client_initial_state_pendiente", true)
        binding.orderToggleButton.isChecked = isPendienteFirst
        updateToggleButtonColor(isPendienteFirst)
        
        // Cargar configuración del tutorial automático
        val tutorialAutoEnabled = prefs.getBoolean("tutorial_auto_enabled", false)
        binding.tutorialAutoSwitch.isChecked = tutorialAutoEnabled
        
        // Cargar configuración de eliminación de informes
        val allowDeleteReports = prefs.getBoolean("allow_delete_reports", false)
        binding.allowDeleteReportsSwitch.isChecked = allowDeleteReports
    }
    
    private fun saveSettings() {
        val newTitle = binding.titleEditText.text.toString().trim()
        val tutorialAutoEnabled = binding.tutorialAutoSwitch.isChecked
        val allowDeleteReports = binding.allowDeleteReportsSwitch.isChecked
        
        if (newTitle.isEmpty()) {
            Toast.makeText(this, "El título no puede estar vacío", Toast.LENGTH_SHORT).show()
            return
        }
        
        prefs.edit()
            .putString("checklist_title", newTitle)
            .putBoolean("tutorial_auto_enabled", tutorialAutoEnabled)
            .putBoolean("allow_delete_reports", allowDeleteReports)
            .apply()
        
        Toast.makeText(this, "Configuración guardada", Toast.LENGTH_SHORT).show()
        finish()
    }
    
    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ (API 33+) - usar permisos de medios granulares
                val hasImages = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
                val hasVideo = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
                val hasAudio = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
                hasImages && hasVideo && hasAudio
            } else {
                // Android 12 y anteriores - usar permisos de almacenamiento tradicionales
                val hasWritePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                val hasReadPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                hasWritePermission && hasReadPermission
            }
        } else {
            true
        }
    }
    
    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissionsToRequest = mutableListOf<String>()
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ (API 33+) - usar permisos de medios granulares
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) 
                    != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
                }
                
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) 
                    != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
                }
                
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) 
                    != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
                }
            } else {
                // Android 12 y anteriores - usar permisos de almacenamiento tradicionales
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                    != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
                
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                    != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
            
            if (permissionsToRequest.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toTypedArray(),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                val allPermissionsGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                
                if (allPermissionsGranted) {
                    Toast.makeText(this, "Todos los permisos de almacenamiento concedidos", Toast.LENGTH_SHORT).show()
                } else {
                    val deniedPermissions = permissions.filterIndexed { index, _ -> 
                        grantResults[index] != PackageManager.PERMISSION_GRANTED 
                    }
                    Toast.makeText(this, "Se necesitan permisos de almacenamiento para crear backups y descargar plantillas. Permisos denegados: ${deniedPermissions.size}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun createBackup() {
        try {
            val reports = reportManager.getAllReports()
            if (reports.isEmpty()) {
                Toast.makeText(this, "No hay reportes para respaldar", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Crear directorio backupchecklist en Downloads
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val backupDir = File(downloadsDir, "backupchecklist")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            
            // Generar nombre de archivo con timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFile = File(backupDir, "backup_checklist_$timestamp.txt")
            
            // Obtener datos necesarios para el backup completo
            val questionManager = QuestionManager(this)
            val categoryManager = CategoryManager(this)
            val allQuestions = questionManager.getQuestionsOrderedByPosition()
            val allEjecutivos = ejecutivoManager.getAllEjecutivos()
            val checklistTitle = prefs.getString("checklist_title", "Preguntas del Checklist")
            
            // Escribir backup
            FileWriter(backupFile).use { writer ->
                writer.write("=== BACKUP CHECKLIST REPORTES COMPLETO ===\n")
                writer.write("Fecha de creación: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}\n")
                writer.write("Total de reportes: ${reports.size}\n")
                writer.write("Título del checklist: $checklistTitle\n")
                writer.write("Total de preguntas disponibles: ${allQuestions.size}\n")
                writer.write("Total de ejecutivos: ${allEjecutivos.size}\n\n")
                
                // Información de ejecutivos
                writer.write("=== EJECUTIVOS DISPONIBLES ===\n")
                allEjecutivos.forEach { ejecutivo ->
                    writer.write("ID: ${ejecutivo.id} | Nombre: ${ejecutivo.name} | Color: ${ejecutivo.color}\n")
                }
                writer.write("\n")
                
                // Información de preguntas disponibles
                writer.write("=== PREGUNTAS DISPONIBLES ===\n")
                allQuestions.forEach { question ->
                    val ejecutivo = allEjecutivos.find { it.id == question.ejecutivoId }
                    val ejecutivoName = ejecutivo?.name ?: "Sin ejecutivo"
                    writer.write("Posición: ${question.position} | Ejecutivo: $ejecutivoName\n")
                    writer.write("Título: ${question.title}\n")
                    if (question.subtitle.isNotEmpty()) {
                        writer.write("Subtítulo: ${question.subtitle}\n")
                    }
                    writer.write("Estado actual: ${if (question.isCompleted) "COMPLETADA" else "PENDIENTE"}\n")
                    writer.write("---\n")
                }
                writer.write("\n")
                
                // Información detallada de cada reporte
                reports.forEachIndexed { index, report ->
                    writer.write("=== REPORTE ${index + 1} ===\n")
                    writer.write("ID: ${String.format("%03d", report.id)}\n")
                    writer.write("Nombre: ${report.name}\n")
                    writer.write("Posición: ${report.position}\n")
                    writer.write("Supervisor: ${report.supervisor}\n")
                    writer.write("Comentarios: ${if (report.comments.isNotEmpty()) report.comments else "Sin comentarios"}\n")
                    writer.write("Fecha de creación: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(report.createdAt))}\n")
                    writer.write("Ruta del archivo PDF: ${report.filePath}\n\n")
                    
                    // Resumen del reporte (simulando el contenido del PDF)
                    writer.write("--- RESUMEN DEL REPORTE (CONTENIDO DEL PDF) ---\n")
                    writer.write("TÍTULO: $checklistTitle\n\n")
                    
                    writer.write("INFORMACIÓN DEL REPORTE:\n")
                    writer.write("Nombre: ${report.name}\n")
                    writer.write("Puesto: ${report.position}\n")
                    writer.write("Jefe Directo: ${report.supervisor}\n")
                    writer.write("Comentarios: ${if (report.comments.isNotEmpty()) report.comments else "Sin comentarios"}\n\n")
                    
                    writer.write("Generado el: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(report.createdAt))}\n\n")
                    
                    // Tabla de preguntas con respuestas
                    writer.write("RESPUESTAS DEL CHECKLIST:\n")
                    writer.write("Pos | Pregunta | Ejecutivo | Estado | Completada\n")
                    writer.write("----|----------|-----------|--------|-----------\n")
                    
                    allQuestions.forEach { question ->
                        val ejecutivo = allEjecutivos.find { it.id == question.ejecutivoId }
                        val ejecutivoName = ejecutivo?.name ?: "Sin ejecutivo"
                        val status = if (question.isCompleted) "Completada" else "Pendiente"
                        val checkText = if (question.isCompleted) "✓" else "○"
                        
                        writer.write("${String.format("%3d", question.position)} | ")
                        
                        // Pregunta (título + subtítulo)
                        val questionText = if (question.subtitle.isNotEmpty()) {
                            "${question.title} | ${question.subtitle}"
                        } else {
                            question.title
                        }
                        writer.write("$questionText | ")
                        writer.write("$ejecutivoName | ")
                        writer.write("$status | ")
                        writer.write("$checkText\n")
                    }
                    
                    // Resumen estadístico
                    val completedCount = allQuestions.count { it.isCompleted }
                    val pendingCount = allQuestions.size - completedCount
                    val completionPercentage = if (allQuestions.isNotEmpty()) {
                        (completedCount * 100) / allQuestions.size
                    } else 0
                    
                    writer.write("\nRESUMEN:\n")
                    writer.write("Total de preguntas: ${allQuestions.size}\n")
                    writer.write("Completadas: $completedCount\n")
                    writer.write("Pendientes: $pendingCount\n")
                    writer.write("Porcentaje de completado: $completionPercentage%\n")
                    
                    writer.write("\n" + "=".repeat(80) + "\n\n")
                }
                
                writer.write("=== FIN DEL BACKUP ===\n")
            }
            
            Toast.makeText(this, "Backup completo creado exitosamente en: ${backupFile.absolutePath}", Toast.LENGTH_LONG).show()
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error al crear backup: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
    
    private fun restoreFromBackup() {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val backupDir = File(downloadsDir, "backupchecklist")
            
            if (!backupDir.exists() || !backupDir.isDirectory) {
                Toast.makeText(this, "No se encontró la carpeta backupchecklist en Descargas", Toast.LENGTH_LONG).show()
                return
            }
            
            val backupFiles = backupDir.listFiles { _, name -> name.endsWith(".txt") }
            
            if (backupFiles.isNullOrEmpty()) {
                Toast.makeText(this, "No se encontraron archivos de backup (.txt) en la carpeta backupchecklist", Toast.LENGTH_LONG).show()
                return
            }
            
            // Mostrar lista de archivos de backup disponibles
            val fileNames = backupFiles.map { it.name }.toTypedArray()
            
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Seleccionar Backup")
                .setMessage("Selecciona el archivo de backup a restaurar:")
                .setItems(fileNames) { _, which ->
                    val selectedFile = backupFiles[which]
                    performRestore(selectedFile)
                }
                .setNegativeButton("Cancelar", null)
                .show()
                
        } catch (e: Exception) {
            Toast.makeText(this, "Error al buscar backups: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
    
    private fun performRestore(backupFile: File) {
        try {
            val content = backupFile.readText()
            
            // Verificar que es un archivo de backup válido
            if (!content.contains("=== BACKUP CHECKLIST REPORTES ===")) {
                Toast.makeText(this, "El archivo seleccionado no es un backup válido", Toast.LENGTH_LONG).show()
                return
            }
            
            // Extraer información de los reportes del backup
            val reportSections = content.split("--- REPORTE").drop(1) // Ignorar el header
            val restoredReports = mutableListOf<ReportInfo>()
            
            reportSections.forEach { section ->
                try {
                    val lines = section.split("\n").filter { it.isNotBlank() }
                    if (lines.isNotEmpty()) {
                        var id = 0L
                        var name = ""
                        var position = ""
                        var supervisor = ""
                        var comments = ""
                        var createdAt = System.currentTimeMillis()
                        var filePath = ""
                        
                        lines.forEach { line ->
                            when {
                                line.startsWith("ID:") -> id = line.substring(3).trim().toLongOrNull() ?: 0L
                                line.startsWith("Nombre:") -> name = line.substring(7).trim()
                                line.startsWith("Posición:") -> position = line.substring(9).trim()
                                line.startsWith("Supervisor:") -> supervisor = line.substring(11).trim()
                                line.startsWith("Comentarios:") -> comments = line.substring(12).trim()
                                line.startsWith("Fecha de creación:") -> {
                                    // Parsear fecha si es posible
                                    try {
                                        val dateStr = line.substring(18).trim()
                                        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                                        createdAt = formatter.parse(dateStr)?.time ?: System.currentTimeMillis()
                                    } catch (e: Exception) {
                                        // Usar timestamp actual si no se puede parsear
                                        createdAt = System.currentTimeMillis()
                                    }
                                }
                                line.startsWith("Ruta del archivo:") -> filePath = line.substring(17).trim()
                            }
                        }
                        
                        if (name.isNotEmpty() && position.isNotEmpty() && supervisor.isNotEmpty()) {
                            val report = ReportInfo(
                                id = id,
                                name = name,
                                position = position,
                                supervisor = supervisor,
                                comments = comments,
                                filePath = filePath,
                                createdAt = createdAt
                            )
                            restoredReports.add(report)
                        }
                    }
                } catch (e: Exception) {
                    // Continuar con el siguiente reporte si hay error
                    e.printStackTrace()
                }
            }
            
            if (restoredReports.isNotEmpty()) {
                // Limpiar reportes existentes y restaurar
                val currentReports = reportManager.getAllReports()
                currentReports.forEach { report ->
                    reportManager.deleteReport(report.id)
                }
                
                // Agregar reportes restaurados
                restoredReports.forEach { report ->
                    reportManager.saveReport(report)
                }
                
                Toast.makeText(this, "Backup restaurado exitosamente. ${restoredReports.size} reportes restaurados.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "No se pudieron restaurar reportes del backup", Toast.LENGTH_LONG).show()
            }
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error al restaurar backup: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
    
    private fun updateImportStatus() {
        try {
            val clientes = clienteManager.getAllClientes()
            val statusText = if (clientes.isNotEmpty()) {
                "Estado: ${clientes.size} clientes importados"
            } else {
                "Estado: No importado"
            }
            binding.importStatusText.text = statusText
        } catch (e: Exception) {
            binding.importStatusText.text = "Estado: Error al verificar"
        }
    }
    
    private fun importClientsFromJson() {
        try {
            // Mostrar diálogo para seleccionar tipo de archivo
            val options = arrayOf("Archivo JSON", "Archivo Excel (.xlsx)")
            
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Seleccionar Tipo de Archivo")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> openFilePicker("application/json", REQUEST_CODE_IMPORT_JSON)
                        1 -> openFilePicker("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", REQUEST_CODE_IMPORT_EXCEL)
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
            
        } catch (e: Exception) {
            showErrorModal("Error al abrir explorador de archivos", e.message ?: "Error desconocido")
            e.printStackTrace()
        }
    }

    private fun openFilePicker(mimeType: String, requestCode: Int) {
        try {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = mimeType
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            
            val chooser = Intent.createChooser(intent, "Seleccionar archivo de clientes")
            startActivityForResult(chooser, requestCode)
            
        } catch (e: Exception) {
            showErrorModal("Error al abrir explorador de archivos", e.message ?: "Error desconocido")
            e.printStackTrace()
        }
    }
    
    private fun performImport(jsonPath: String) {
        try {
            val success = clienteManager.loadClientesFromJson(jsonPath)
            
            if (success) {
                val clientes = clienteManager.getAllClientes()
                Toast.makeText(this, "Importación exitosa: ${clientes.size} clientes importados", Toast.LENGTH_LONG).show()
                updateImportStatus()
                
                // Mostrar resumen de la importación
                showImportSummary(clientes)
            } else {
                Toast.makeText(this, "Error al importar clientes desde el archivo JSON", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error durante la importación: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
    
    private fun showImportSummary(clientes: List<Cliente>) {
        val summary = buildString {
            appendLine("=== RESUMEN DE IMPORTACIÓN ===")
            appendLine("Total de clientes importados: ${clientes.size}")
            appendLine()
            appendLine("Tipos de persona:")
            val fisicos = clientes.count { it.tipoPersona == "Físico" }
            val juridicos = clientes.count { it.tipoPersona == "Jurídico" }
            appendLine("- Físicos: $fisicos")
            appendLine("- Jurídicos: $juridicos")
            appendLine()
            appendLine("Estados:")
            val patentados = clientes.count { it.patentado }
            val pendientesPago = clientes.count { it.pendientePago }
            appendLine("- Patentados: $patentados")
            appendLine("- Pendientes de pago: $pendientesPago")
            appendLine()
            appendLine("Los clientes ahora están disponibles en Gestión de Clientes.")
        }
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Importación Completada")
            .setMessage(summary)
            .setPositiveButton("Ver Clientes") { _, _ ->
                // Abrir Gestión de Clientes
                val intent = android.content.Intent(this, QuestionsActivity::class.java)
                intent.putExtra("isAdminMode", true)
                startActivity(intent)
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }
    
    private fun verifyImportStatus() {
        try {
            val clientes = clienteManager.getAllClientes()

            if (clientes.isEmpty()) {
                Toast.makeText(this, "No hay clientes importados", Toast.LENGTH_SHORT).show()
                return
            }

            val verification = buildString {
                appendLine("=== VERIFICACIÓN DE IMPORTACIÓN ===")
                appendLine("Total de clientes: ${clientes.size}")
                appendLine()
                appendLine("Primeros 5 clientes:")
                clientes.take(5).forEachIndexed { index, cliente ->
                    appendLine("${index + 1}. ${cliente.nombre} (${cliente.cedula})")
                }
                if (clientes.size > 5) {
                    appendLine("... y ${clientes.size - 5} más")
                }
                appendLine()
                appendLine("Archivo fuente: Clientes_de_Contabilidad_Totales.json")
                appendLine("Estado: Importación exitosa")
            }

            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Estado de Importación")
                .setMessage(verification)
                .setPositiveButton("Ver Todos los Clientes") { _, _ ->
                    val intent = android.content.Intent(this, QuestionsActivity::class.java)
                    intent.putExtra("isAdminMode", true)
                    startActivity(intent)
                }
                .setNegativeButton("Cerrar", null)
                .show()

        } catch (e: Exception) {
            Toast.makeText(this, "Error al verificar importación: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                when (requestCode) {
                        REQUEST_CODE_IMPORT_JSON -> {
                            try {
                                val inputStream = contentResolver.openInputStream(uri)
                                if (inputStream != null) {
                                    val jsonContent = inputStream.bufferedReader().use { it.readText() }
                                    inputStream.close()

                                    android.util.Log.d("ConfigActivity", "JSON leído exitosamente, longitud: ${jsonContent.length}")
                                    android.util.Log.d("ConfigActivity", "Primeros 200 caracteres: ${jsonContent.take(200)}")

                                    if (jsonContent.isNotBlank()) {
                                        showImportConfirmation("JSON") {
                                            performImportFromJsonContent(jsonContent)
                                        }
                                    } else {
                                        showErrorModal("Error de lectura", "El archivo JSON está vacío")
                                    }
                                } else {
                                    showErrorModal("Error de lectura", "No se pudo abrir el archivo JSON seleccionado")
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("ConfigActivity", "Error al leer archivo JSON", e)
                                showErrorModal("Error al leer archivo JSON", e.message ?: "Error desconocido")
                                e.printStackTrace()
                            }
                        }
                    REQUEST_CODE_IMPORT_EXCEL -> {
                        try {
                            val inputStream = contentResolver.openInputStream(uri)
                            if (inputStream != null) {
                                showImportConfirmation("Excel") {
                                    performImportFromExcelStream(inputStream)
                                }
                            } else {
                                showErrorModal("Error de lectura", "No se pudo leer el archivo Excel seleccionado")
                            }
                        } catch (e: Exception) {
                            showErrorModal("Error al leer archivo Excel", e.message ?: "Error desconocido")
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    private fun showImportConfirmation(fileType: String, onConfirm: () -> Unit) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Importar Clientes")
            .setMessage("¿Estás seguro de que quieres importar los clientes desde el archivo $fileType seleccionado? Esto reemplazará los clientes existentes.")
            .setPositiveButton("Importar") { _, _ ->
                onConfirm()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showErrorModal(title: String, message: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Aceptar", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun showProgressDialog(): androidx.appcompat.app.AlertDialog {
        val progressDialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Importando Clientes")
            .setMessage("Procesando archivo...")
            .setCancelable(false)
            .create()
        
        progressDialog.show()
        return progressDialog
    }
    
    private fun showFormatProgressDialog(): androidx.appcompat.app.AlertDialog {
        val progressDialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Formateando Archivo Excel")
            .setMessage("Configurando formato para importación...")
            .setCancelable(false)
            .create()
        
        progressDialog.show()
        return progressDialog
    }
    
    private fun showImportProgressDialog(): androidx.appcompat.app.AlertDialog {
        val progressDialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Importando Clientes")
            .setMessage("Procesando datos del archivo...")
            .setCancelable(false)
            .create()
        
        progressDialog.show()
        return progressDialog
    }
    
    private fun showFormatCompletedDialog(onContinue: () -> Unit) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("✅ Formateo Completado")
            .setMessage("El archivo Excel ha sido formateado correctamente.\n\n" +
                      "• Encabezados configurados\n" +
                      "• Columnas formateadas\n" +
                      "• Valores por defecto establecidos\n\n" +
                      "¿Continuar con la importación?")
            .setPositiveButton("Continuar Importación") { _, _ ->
                onContinue()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun performImportFromJsonContent(jsonContent: String) {
        val progressDialog = showProgressDialog()
        
        Thread {
            try {
                val result = clienteManager.loadClientesFromJsonContentWithValidation(jsonContent)
                
                runOnUiThread {
                    progressDialog.dismiss()
                    updateImportStatus()
                    // Mostrar diálogo full-screen con el resultado detallado
                    ImportResultDialogActivity.start(this@ConfigActivity, result)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    progressDialog.dismiss()
                    showErrorModal("Error durante la importación", e.message ?: "Error desconocido")
                    e.printStackTrace()
                }
            }
        }.start()
    }

    private fun performImportFromExcelStream(inputStream: java.io.InputStream) {
        // Mostrar diálogo de formateo
        val formatDialog = showFormatProgressDialog()
        
        Thread {
            try {
                // Simular tiempo de formateo para mostrar el indicador
                Thread.sleep(1500)
                
                runOnUiThread {
                    formatDialog.dismiss()
                    
                    // Mostrar mensaje de formateo completado
                    showFormatCompletedDialog {
                        // Después de confirmar, proceder con la importación
                        val importDialog = showImportProgressDialog()
                        
                        Thread {
                            try {
                                val result = clienteManager.loadClientesFromExcelStreamWithValidation(inputStream, "celeste")
                                
                                runOnUiThread {
                                    importDialog.dismiss()
                                    updateImportStatus()
                                    // Mostrar diálogo full-screen con el resultado detallado
                                    ImportResultDialogActivity.start(this@ConfigActivity, result)
                                }
                            } catch (e: Exception) {
                                runOnUiThread {
                                    importDialog.dismiss()
                                    showErrorModal("Error durante la importación", e.message ?: "Error desconocido")
                                    e.printStackTrace()
                                }
                            }
                        }.start()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    formatDialog.dismiss()
                    showErrorModal("Error durante el formateo", e.message ?: "Error desconocido")
                    e.printStackTrace()
                }
            }
        }.start()
    }
    
    private fun downloadExcelTemplate() {
        if (checkStoragePermission()) {
            performTemplateDownload()
        } else {
            requestStoragePermission()
        }
    }
    
    private fun performTemplateDownload() {
        try {
            // Generar la plantilla Excel
            val templateBytes = clienteManager.generateExcelTemplate()
            
            if (templateBytes.isEmpty()) {
                showErrorModal("Error", "No se pudo generar la plantilla Excel")
                return
            }
            
            val fileName = "Plantilla de importacion de clientes FACTO.xlsx"
            val file: File
            
            // Intentar usar la carpeta de Descargas pública primero
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ - usar MediaStore para acceso a Descargas
                file = saveToDownloadsUsingMediaStore(fileName, templateBytes)
            } else {
                // Android 9 y anteriores - usar acceso directo
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                file = File(downloadsDir, fileName)
                file.writeBytes(templateBytes)
            }
            
            if (file.exists()) {
                // Abrir la carpeta de Descargas
                openDownloadsFolder(file)
                
                // Mostrar mensaje de éxito
                runOnUiThread {
                    Toast.makeText(this, "Plantilla descargada y carpeta abierta", Toast.LENGTH_LONG).show()
                }
                
                android.util.Log.d("ConfigActivity", "Plantilla descargada: ${file.absolutePath}")
            } else {
                showErrorModal("Error", "No se pudo crear el archivo en la carpeta de Descargas")
            }
            
        } catch (e: Exception) {
            android.util.Log.e("ConfigActivity", "Error descargando plantilla", e)
            showErrorModal("Error", "No se pudo descargar la plantilla: ${e.message}")
        }
    }
    
    @Suppress("DEPRECATION")
    private fun saveToDownloadsUsingMediaStore(fileName: String, content: ByteArray): File {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val resolver = contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(content)
                    }
                    // Crear un File temporal para la función openDownloadsFolder
                    File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
                } else {
                    throw Exception("No se pudo crear el archivo en MediaStore")
                }
            } catch (e: Exception) {
                android.util.Log.e("ConfigActivity", "Error usando MediaStore, intentando método alternativo", e)
                // Fallback: usar directorio de la aplicación
                val appDir = File(getExternalFilesDir(null), "Downloads")
                if (!appDir.exists()) {
                    appDir.mkdirs()
                }
                val file = File(appDir, fileName)
                file.writeBytes(content)
                file
            }
        } else {
            // Android 9 y anteriores
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            file.writeBytes(content)
            file
        }
    }
    
    private fun openDownloadsFolder(file: File) {
        try {
            // Intentar abrir la carpeta de Descargas con el administrador de archivos
            val intent = Intent(Intent.ACTION_VIEW)
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file.parentFile ?: file)
            intent.setDataAndType(uri, "resource/folder")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            
            // Intentar abrir el archivo específico
            val fileIntent = Intent(Intent.ACTION_VIEW)
            val fileUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            fileIntent.setDataAndType(fileUri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            fileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            
            // Crear un chooser para que el usuario elija cómo abrir
            val chooserIntent = Intent.createChooser(intent, "Abrir carpeta de Descargas")
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(fileIntent))
            
            startActivity(chooserIntent)
        } catch (e: Exception) {
            android.util.Log.e("ConfigActivity", "Error abriendo carpeta de descargas", e)
            // Fallback: mostrar la ruta del archivo
            runOnUiThread {
                Toast.makeText(this, "Archivo guardado en: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
