package com.checklist.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

data class LogEntry(
    val timestamp: Long,
    val nivel: String, // INFO, WARNING, ERROR
    val actividad: String,
    val mensaje: String,
    val stackTrace: String = ""
)

class AppLogger(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_logs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val maxLogs = 500 // Máximo de logs a mantener
    
    companion object {
        const val NIVEL_INFO = "INFO"
        const val NIVEL_WARNING = "WARNING"
        const val NIVEL_ERROR = "ERROR"
        
        @Volatile
        private var instance: AppLogger? = null
        
        fun getInstance(context: Context): AppLogger {
            return instance ?: synchronized(this) {
                instance ?: AppLogger(context.applicationContext).also { instance = it }
            }
        }
    }
    
    fun log(nivel: String, actividad: String, mensaje: String, stackTrace: String = "") {
        try {
            val logs = getAllLogs().toMutableList()
            
            val logEntry = LogEntry(
                timestamp = System.currentTimeMillis(),
                nivel = nivel,
                actividad = actividad,
                mensaje = mensaje,
                stackTrace = stackTrace
            )
            
            logs.add(logEntry)
            
            // Mantener solo los últimos maxLogs registros
            if (logs.size > maxLogs) {
                logs.removeAt(0)
            }
            
            saveLogs(logs)
            
            // También hacer log en Logcat
            when (nivel) {
                NIVEL_ERROR -> android.util.Log.e("AppLogger", "[$actividad] $mensaje")
                NIVEL_WARNING -> android.util.Log.w("AppLogger", "[$actividad] $mensaje")
                else -> android.util.Log.d("AppLogger", "[$actividad] $mensaje")
            }
        } catch (e: Exception) {
            android.util.Log.e("AppLogger", "Error al guardar log: ${e.message}")
        }
    }
    
    fun logInfo(actividad: String, mensaje: String) {
        log(NIVEL_INFO, actividad, mensaje)
    }
    
    fun logWarning(actividad: String, mensaje: String) {
        log(NIVEL_WARNING, actividad, mensaje)
    }
    
    fun logError(actividad: String, mensaje: String, exception: Exception? = null) {
        val stackTrace = exception?.stackTraceToString() ?: ""
        log(NIVEL_ERROR, actividad, mensaje, stackTrace)
    }
    
    fun getAllLogs(): List<LogEntry> {
        val json = prefs.getString("logs", "[]")
        val type = object : TypeToken<List<LogEntry>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
    
    fun clearLogs() {
        saveLogs(emptyList())
    }
    
    private fun saveLogs(logs: List<LogEntry>) {
        val json = gson.toJson(logs)
        prefs.edit().putString("logs", json).apply()
    }
    
    fun exportLogsToFile(): String? {
        try {
            val logs = getAllLogs()
            
            if (logs.isEmpty()) {
                return null
            }
            
            // Guardar en el directorio de documentos de la app
            val fileName = "App_Logs_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.txt"
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
            
            FileWriter(file).use { writer ->
                writer.write("═══════════════════════════════════════════════════\n")
                writer.write("    REGISTRO DE LOGS - GESTIÓN DE CLIENTES\n")
                writer.write("═══════════════════════════════════════════════════\n\n")
                writer.write("Fecha de exportación: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}\n")
                writer.write("Total de registros: ${logs.size}\n\n")
                writer.write("═══════════════════════════════════════════════════\n\n")
                
                logs.forEach { log ->
                    val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                    
                    writer.write("─────────────────────────────────────────────────\n")
                    writer.write("Fecha: $fecha\n")
                    writer.write("Nivel: ${log.nivel}\n")
                    writer.write("Actividad: ${log.actividad}\n")
                    writer.write("Mensaje: ${log.mensaje}\n")
                    
                    if (log.stackTrace.isNotEmpty()) {
                        writer.write("\nStack Trace:\n")
                        writer.write(log.stackTrace)
                        writer.write("\n")
                    }
                    
                    writer.write("\n")
                }
                
                writer.write("═══════════════════════════════════════════════════\n")
                writer.write("            FIN DEL REGISTRO\n")
                writer.write("═══════════════════════════════════════════════════\n")
            }
            
            return file.absolutePath
        } catch (e: Exception) {
            android.util.Log.e("AppLogger", "Error al exportar logs: ${e.message}")
            return null
        }
    }
    
    fun getLogsCount(): Int {
        return getAllLogs().size
    }
    
    fun getErrorsCount(): Int {
        return getAllLogs().count { it.nivel == NIVEL_ERROR }
    }
    
    fun getWarningsCount(): Int {
        return getAllLogs().count { it.nivel == NIVEL_WARNING }
    }
}

