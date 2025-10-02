package com.checklist.app

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

data class ImportResult(
    val success: Boolean,
    val errorMessage: String? = null,
    val clientesCount: Int = 0,
    val clientesNuevos: Int = 0,
    val clientesDuplicados: Int = 0,
    val clientesActualizados: Int = 0,
    val erroresDetallados: List<String> = emptyList(),
    val duplicadosDetallados: List<String> = emptyList(),
    val formatoCompletado: Boolean = false
) : android.os.Parcelable {
    constructor(parcel: android.os.Parcel) : this(
        parcel.readByte() != 0.toByte(),
        parcel.readString(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.createStringArrayList() ?: emptyList(),
        parcel.createStringArrayList() ?: emptyList(),
        parcel.readByte() != 0.toByte()
    )
    
    override fun writeToParcel(parcel: android.os.Parcel, flags: Int) {
        parcel.writeByte(if (success) 1 else 0)
        parcel.writeString(errorMessage)
        parcel.writeInt(clientesCount)
        parcel.writeInt(clientesNuevos)
        parcel.writeInt(clientesDuplicados)
        parcel.writeInt(clientesActualizados)
        parcel.writeStringList(erroresDetallados)
        parcel.writeStringList(duplicadosDetallados)
        parcel.writeByte(if (formatoCompletado) 1 else 0)
    }
    
    override fun describeContents(): Int {
        return 0
    }
    
    companion object CREATOR : android.os.Parcelable.Creator<ImportResult> {
        override fun createFromParcel(parcel: android.os.Parcel): ImportResult {
            return ImportResult(parcel)
        }
        
        override fun newArray(size: Int): Array<ImportResult?> {
            return arrayOfNulls(size)
        }
    }
}

class ClienteManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("cliente_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    @Volatile private var cachedClientes: List<Cliente>? = null
    @Volatile private var cachedClientesById: Map<Long, Cliente>? = null
    
    fun getAllClientes(): List<Cliente> {
        cachedClientes?.let { return it }
        val json = prefs.getString("clientes", "[]")
        val type = object : TypeToken<List<Cliente>>() {}.type
        val parsed = gson.fromJson<List<Cliente>>(json, type) ?: emptyList()
        cachedClientes = parsed
        cachedClientesById = parsed.associateBy { it.id }
        return parsed
    }
    
    fun addCliente(cliente: Cliente): Long {
        val clientes = getAllClientes().toMutableList()
        val newId = if (clientes.isEmpty()) 1L else (clientes.maxOfOrNull { it.id } ?: 0L) + 1
        val newCliente = cliente.copy(id = newId)
        clientes.add(newCliente)
        saveClientes(clientes)
        return newId
    }
    
    fun updateCliente(cliente: Cliente) {
        val clientes = getAllClientes().toMutableList()
        val index = clientes.indexOfFirst { it.id == cliente.id }
        if (index != -1) {
            clientes[index] = cliente
            saveClientes(clientes)
        }
    }
    
    fun deleteCliente(cliente: Cliente) {
        val clientes = getAllClientes().toMutableList()
        clientes.removeAll { it.id == cliente.id }
        saveClientes(clientes)
    }
    
    fun getClienteById(id: Long): Cliente? {
        val localCache = cachedClientesById
        if (localCache != null) return localCache[id]
        // fallback: inicializar caché
        val list = getAllClientes()
        return cachedClientesById?.get(id) ?: list.find { it.id == id }
    }
    
    fun searchClientes(query: String): List<Cliente> {
        val clientes = getAllClientes()
        return if (query.isEmpty()) {
            clientes
        } else {
            clientes.filter { 
                it.nombre.contains(query, ignoreCase = true) ||
                it.cedula.contains(query, ignoreCase = true) ||
                it.telefono.contains(query, ignoreCase = true)
            }
        }
    }
    
    fun loadClientesFromExcel(filePath: String, password: String = "celeste"): Boolean {
        return try {
            val file = File(filePath)
            if (!file.exists()) return false
            
            val inputStream = FileInputStream(file)
            val workbook = WorkbookFactory.create(inputStream, password)
            val sheet: Sheet = workbook.getSheetAt(0)
            
            val clientes = mutableListOf<Cliente>()
            var rowIndex = 1 // Saltar la fila de encabezados
            
            while (rowIndex <= sheet.lastRowNum) {
                val row: Row? = sheet.getRow(rowIndex)
                if (row != null) {
                    val cliente = createClienteFromRow(row)
                    if (cliente != null) {
                        clientes.add(cliente)
                    }
                }
                rowIndex++
            }
            
            // Guardar los clientes cargados
            val existingClientes = getAllClientes().toMutableList()
            existingClientes.addAll(clientes)
            saveClientes(existingClientes)
            
            workbook.close()
            inputStream.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun loadClientesFromJson(filePath: String): Boolean {
        return try {
            android.util.Log.d("ClienteManager", "loadClientesFromJson: Iniciando carga automática desde $filePath")

            val file = File(filePath)
            if (!file.exists()) {
                android.util.Log.d("ClienteManager", "loadClientesFromJson: Archivo no existe")
                return false
            }

            val json = file.readText()
            android.util.Log.d("ClienteManager", "loadClientesFromJson: JSON leído, tamaño: ${json.length} caracteres")

            val type = object : TypeToken<List<Cliente>>() {}.type
            val clientes = gson.fromJson<List<Cliente>>(json, type) ?: emptyList()
            android.util.Log.d("ClienteManager", "loadClientesFromJson: ${clientes.size} clientes parseados del JSON")

            // Limpiar clientes existentes y cargar los nuevos
            saveClientes(clientes)
            android.util.Log.d("ClienteManager", "loadClientesFromJson: ${clientes.size} clientes guardados")

            true
        } catch (e: Exception) {
            android.util.Log.e("ClienteManager", "loadClientesFromJson: Error al cargar JSON", e)
            e.printStackTrace()
            false
        }
    }

    fun loadClientesFromJsonContent(jsonContent: String): Boolean {
        return try {
            android.util.Log.d("ClienteManager", "loadClientesFromJsonContent: Iniciando carga desde contenido JSON")

            val type = object : TypeToken<List<Cliente>>() {}.type
            val clientes = gson.fromJson<List<Cliente>>(jsonContent, type) ?: emptyList()
            android.util.Log.d("ClienteManager", "loadClientesFromJsonContent: ${clientes.size} clientes parseados del JSON")

            // Limpiar clientes existentes y cargar los nuevos
            saveClientes(clientes)
            android.util.Log.d("ClienteManager", "loadClientesFromJsonContent: ${clientes.size} clientes guardados")

            true
        } catch (e: Exception) {
            android.util.Log.e("ClienteManager", "loadClientesFromJsonContent: Error al cargar JSON", e)
            e.printStackTrace()
            false
        }
    }
    
    fun precargarClientes(): Boolean {
        val t0 = System.currentTimeMillis()
        android.util.Log.d("ClienteManager", "precargarClientes: Iniciando carga automática")
        
        // Verificar si ya hay clientes cargados para evitar recargas innecesarias
        val existingClientes = getAllClientes()
        if (existingClientes.isNotEmpty()) {
            android.util.Log.d("ClienteManager", "precargarClientes: Ya hay ${existingClientes.size} clientes cargados, omitiendo precarga")
            return true
        }
        
        val controldepagosPath = "C:\\Users\\ronal\\OneDrive\\Escritorio\\CONTROLDEPAGOS"
        val excelPath = "$controldepagosPath\\Clientes de Contabilidad Totales.xlsx"
        val jsonPath = "$controldepagosPath\\Clientes_de_Contabilidad_Totales.json"
        
        android.util.Log.d("ClienteManager", "precargarClientes: Buscando archivos en: $controldepagosPath")
        
        // Intentar cargar desde JSON primero (más rápido)
        val jsonFile = File(jsonPath)
        val excelFile = File(excelPath)
        
        android.util.Log.d("ClienteManager", "precargarClientes: JSON existe: ${jsonFile.exists()}")
        android.util.Log.d("ClienteManager", "precargarClientes: Excel existe: ${excelFile.exists()}")
        
        val result = when {
            jsonFile.exists() -> {
                android.util.Log.d("ClienteManager", "precargarClientes: Cargando desde JSON")
                val success = loadClientesFromJson(jsonPath)
                android.util.Log.d("ClienteManager", "precargarClientes: JSON cargado exitosamente: $success")
                success
            }
            excelFile.exists() -> {
                android.util.Log.d("ClienteManager", "precargarClientes: Cargando desde Excel")
                val success = loadClientesFromExcel(excelPath, "celeste")
                android.util.Log.d("ClienteManager", "precargarClientes: Excel cargado exitosamente: $success")
                success
            }
            else -> {
                android.util.Log.d("ClienteManager", "precargarClientes: No se encontraron archivos")
                false
            }
        }
        android.util.Log.d("Perf", "ClienteManager.precargarClientes: ${System.currentTimeMillis() - t0}ms")
        return result
    }
    
    private fun createClienteFromRow(row: Row): Cliente? {
        return try {
            val nombre = getCellValueAsString(row.getCell(0)) ?: return null
            val cedula = getCellValueAsString(row.getCell(1)) ?: ""
            val tipoPersona = getCellValueAsString(row.getCell(2)) ?: "Físico"
            val representante = getCellValueAsString(row.getCell(3)) ?: ""
            
            // Debug específico para la columna de teléfono
            val telefonoCell = row.getCell(4)
            val telefonoRaw = getCellValueAsString(telefonoCell) ?: ""
            
            // Validar teléfono - debe tener exactamente 8 dígitos
            val telefono = if (telefonoRaw.isBlank()) {
                ""
            } else {
                // Limpiar el teléfono de caracteres no numéricos
                val cleanPhone = telefonoRaw.replace(Regex("[^0-9]"), "")
                if (cleanPhone.length == 8) {
                    cleanPhone
                } else {
                    android.util.Log.w("ClienteManager", "createClienteFromRow: Teléfono inválido: '$telefonoRaw' (${cleanPhone.length} dígitos)")
                    ""
                }
            }
            
            android.util.Log.d("ClienteManager", "createClienteFromRow - Celda teléfono:")
            android.util.Log.d("ClienteManager", "  Tipo de celda: ${telefonoCell?.cellType}")
            android.util.Log.d("ClienteManager", "  Valor numérico: ${if (telefonoCell?.cellType == org.apache.poi.ss.usermodel.CellType.NUMERIC) telefonoCell?.numericCellValue else "N/A"}")
            android.util.Log.d("ClienteManager", "  Valor string: ${telefonoCell?.stringCellValue}")
            android.util.Log.d("ClienteManager", "  Teléfono original: '$telefonoRaw'")
            android.util.Log.d("ClienteManager", "  Teléfono final: '$telefono'")
            
            val ciFc = getCellValueAsString(row.getCell(5)) ?: ""
            val ejecutivo = getCellValueAsString(row.getCell(6)) ?: ""
            val patentado = getCellValueAsBoolean(row.getCell(7)) ?: false
            val pendientePago = getCellValueAsBoolean(row.getCell(8)) ?: false
            val tipoRegimen = getCellValueAsString(row.getCell(9)) ?: ""
            
            // Log para debuggear
            android.util.Log.d("ClienteManager", "createClienteFromRow: Nombre=$nombre, Teléfono=$telefono")
            
            Cliente(
                nombre = nombre,
                cedula = cedula,
                tipoPersona = tipoPersona,
                representante = representante,
                telefono = telefono,
                ciFc = ciFc,
                ejecutivo = ejecutivo,
                patentado = patentado,
                pendientePago = pendientePago,
                tipoRegimen = tipoRegimen
            )
        } catch (e: Exception) {
            android.util.Log.e("ClienteManager", "createClienteFromRow: Error", e)
            e.printStackTrace()
            null
        }
    }
    
    private fun getCellValueAsString(cell: Cell?): String? {
        return when (cell?.cellType) {
            org.apache.poi.ss.usermodel.CellType.STRING -> cell.stringCellValue?.trim()
            org.apache.poi.ss.usermodel.CellType.NUMERIC -> {
                // Para teléfonos y números largos, usar toLong() para evitar pérdida de dígitos
                val numericValue = cell.numericCellValue
                if (numericValue == numericValue.toLong().toDouble()) {
                    // Es un número entero
                    numericValue.toLong().toString()
                } else {
                    // Es un número decimal
                    numericValue.toString()
                }
            }
            org.apache.poi.ss.usermodel.CellType.BOOLEAN -> cell.booleanCellValue.toString()
            else -> null
        }
    }
    
    private fun getCellValueAsBoolean(cell: Cell?): Boolean? {
        return when (cell?.cellType) {
            org.apache.poi.ss.usermodel.CellType.BOOLEAN -> cell.booleanCellValue
            org.apache.poi.ss.usermodel.CellType.STRING -> cell.stringCellValue?.trim()?.lowercase() in listOf("si", "sí", "true", "1", "yes")
            org.apache.poi.ss.usermodel.CellType.NUMERIC -> cell.numericCellValue > 0
            else -> null
        }
    }
    
    private fun saveClientes(clientes: List<Cliente>) {
        val json = gson.toJson(clientes)
        prefs.edit().putString("clientes", json).apply()
        // invalidar y actualizar cachés
        cachedClientes = clientes
        cachedClientesById = clientes.associateBy { it.id }
    }
    
    data class SmartImportResult(
        val clientesFinales: List<Cliente>,
        val clientesNuevos: Int,
        val clientesDuplicados: Int,
        val clientesActualizados: Int,
        val duplicadosDetallados: List<String>
    )
    
    private fun processSmartImport(clientesNuevos: List<Cliente>): SmartImportResult {
        val clientesExistentes = getAllClientes().toMutableList()
        val clientesFinales = clientesExistentes.toMutableList()
        val duplicadosDetallados = mutableListOf<String>()
        
        var clientesNuevosCount = 0
        var clientesDuplicadosCount = 0
        var clientesActualizadosCount = 0
        
        clientesNuevos.forEach { clienteNuevo ->
            // Buscar duplicados por cédula
            val duplicadoPorCedula = clientesExistentes.find { 
                it.cedula.isNotBlank() && it.cedula == clienteNuevo.cedula 
            }
            
            // Buscar duplicados por nombre (si no hay cédula)
            val duplicadoPorNombre = if (clienteNuevo.cedula.isBlank()) {
                clientesExistentes.find { 
                    it.nombre.equals(clienteNuevo.nombre, ignoreCase = true) 
                }
            } else null
            
            val duplicado = duplicadoPorCedula ?: duplicadoPorNombre
            
            if (duplicado != null) {
                // Cliente duplicado encontrado
                clientesDuplicadosCount++
                duplicadosDetallados.add("• ${clienteNuevo.nombre} (Cédula: ${clienteNuevo.cedula}) - Duplicado encontrado")
                
                // Actualizar campos vacíos del cliente existente con datos del nuevo
                val clienteActualizado = duplicado.copy(
                    telefono = if (duplicado.telefono.isBlank() && clienteNuevo.telefono.isNotBlank()) clienteNuevo.telefono else duplicado.telefono,
                    representante = if (duplicado.representante.isBlank() && clienteNuevo.representante.isNotBlank()) clienteNuevo.representante else duplicado.representante,
                    ciFc = if (duplicado.ciFc.isBlank() && clienteNuevo.ciFc.isNotBlank()) clienteNuevo.ciFc else duplicado.ciFc,
                    ejecutivo = if (duplicado.ejecutivo.isBlank() && clienteNuevo.ejecutivo.isNotBlank()) clienteNuevo.ejecutivo else duplicado.ejecutivo,
                    tipoRegimen = if (duplicado.tipoRegimen.isBlank() && clienteNuevo.tipoRegimen.isNotBlank()) clienteNuevo.tipoRegimen else duplicado.tipoRegimen,
                    patentado = duplicado.patentado || clienteNuevo.patentado,
                    pendientePago = duplicado.pendientePago || clienteNuevo.pendientePago
                )
                
                // Reemplazar el cliente existente con el actualizado
                val index = clientesFinales.indexOf(duplicado)
                if (index != -1) {
                    clientesFinales[index] = clienteActualizado
                    if (clienteActualizado != duplicado) {
                        clientesActualizadosCount++
                    }
                }
            } else {
                // Cliente nuevo, asignar ID incremental y agregarlo
                val newId = if (clientesFinales.isEmpty()) 1L else (clientesFinales.maxOfOrNull { it.id } ?: 0L) + 1
                val clienteConId = clienteNuevo.copy(id = newId)
                clientesFinales.add(clienteConId)
                clientesNuevosCount++
                
                android.util.Log.d("ClienteManager", "processSmartImport: Cliente '${clienteNuevo.nombre}' creado con ID=$newId")
            }
        }
        
        // Guardar los clientes finales
        saveClientes(clientesFinales)
        
        return SmartImportResult(
            clientesFinales = clientesFinales,
            clientesNuevos = clientesNuevosCount,
            clientesDuplicados = clientesDuplicadosCount,
            clientesActualizados = clientesActualizadosCount,
            duplicadosDetallados = duplicadosDetallados
        )
    }
    
    fun deleteAllClientes() {
        prefs.edit().putString("clientes", "[]").apply()
        android.util.Log.d("ClienteManager", "deleteAllClientes: Todos los clientes eliminados")
    }
    
    fun cleanDuplicateClientes() {
        val clientes = getAllClientes()
        val uniqueClientes = clientes.distinctBy { it.id }
        
        if (clientes.size != uniqueClientes.size) {
            android.util.Log.w("ClienteManager", "cleanDuplicateClientes: Encontrados ${clientes.size - uniqueClientes.size} clientes duplicados")
            saveClientes(uniqueClientes)
        }
        
        // También verificar teléfonos duplicados
        val telefonosUnicos = uniqueClientes.map { it.telefono }.distinct()
        val telefonosDuplicados = uniqueClientes.size - telefonosUnicos.size
        
        if (telefonosDuplicados > 0) {
            android.util.Log.w("ClienteManager", "cleanDuplicateClientes: Encontrados $telefonosDuplicados teléfonos duplicados")
            
            // Mostrar detalles de teléfonos duplicados
            val telefonosConteo = uniqueClientes.groupBy { it.telefono }
            telefonosConteo.forEach { (telefono, clientesConTelefono) ->
                if (clientesConTelefono.size > 1) {
                    android.util.Log.w("ClienteManager", "Teléfono '$telefono' aparece en ${clientesConTelefono.size} clientes:")
                    clientesConTelefono.forEach { cliente ->
                        android.util.Log.w("ClienteManager", "  - ${cliente.nombre} (ID: ${cliente.id})")
                    }
                }
            }
        }
    }

    fun loadClientesFromJsonContentWithValidation(jsonContent: String?): ImportResult {
        return try {
            android.util.Log.d("ClienteManager", "loadClientesFromJsonContentWithValidation: Iniciando validación JSON")
            
            if (jsonContent == null) {
                return ImportResult(false, "El contenido del archivo JSON es nulo")
            }
            
            if (jsonContent.isBlank()) {
                return ImportResult(false, "El archivo JSON está vacío")
            }
            
            android.util.Log.d("ClienteManager", "loadClientesFromJsonContentWithValidation: Contenido JSON recibido: ${jsonContent.take(200)}...")
            
            val clientes = parseJsonContent(jsonContent)
            
            if (clientes.isEmpty()) {
                return ImportResult(false, "No se encontraron clientes en el archivo JSON")
            }
            
            // Separar clientes válidos e inválidos
            val clientesValidos = mutableListOf<Cliente>()
            val erroresDetallados = mutableListOf<String>()
            
            clientes.forEachIndexed { index, cliente ->
                val errors = validateCliente(cliente, index + 1)
                if (errors.isEmpty()) {
                    clientesValidos.add(cliente)
                } else {
                    erroresDetallados.add("Cliente ${index + 1} (${cliente.nombre}): ${errors.joinToString(", ")}")
                }
            }
            
            // Procesar importación inteligente (mantener existentes, agregar nuevos)
            val result = processSmartImport(clientesValidos)
            
            // Crear estadísticas detalladas
            val estadisticas = generateDetailedStats(result.clientesFinales)
            
            // Crear mensaje de resultado
            val mensajeResultado = buildString {
                if (result.clientesNuevos > 0 || result.clientesActualizados > 0) {
                    append("✅ Importación exitosa:\n")
                    append("• ${result.clientesNuevos} clientes nuevos importados\n")
                    append("• ${result.clientesActualizados} clientes actualizados\n")
                    append("• ${result.clientesDuplicados} clientes duplicados (mantenidos)\n\n")
                    append("📊 ESTADÍSTICAS DETALLADAS:\n")
                    append(estadisticas)
                }
                if (result.duplicadosDetallados.isNotEmpty()) {
                    if (result.clientesNuevos > 0 || result.clientesActualizados > 0) {
                        append("\n\n")
                    }
                    append("⚠️ Clientes duplicados encontrados (${result.duplicadosDetallados.size}):\n")
                    append(result.duplicadosDetallados.joinToString("\n"))
                }
                if (erroresDetallados.isNotEmpty()) {
                    if (result.clientesNuevos > 0 || result.clientesActualizados > 0 || result.duplicadosDetallados.isNotEmpty()) {
                        append("\n\n")
                    }
                    append("❌ Clientes no importados (${erroresDetallados.size}):\n")
                    append(erroresDetallados.joinToString("\n"))
                }
                if (result.clientesNuevos == 0 && result.clientesActualizados == 0 && erroresDetallados.isEmpty()) {
                    append("No se encontraron clientes válidos para importar")
                }
            }
            
            ImportResult(
                success = result.clientesNuevos > 0 || result.clientesActualizados > 0,
                errorMessage = mensajeResultado,
                clientesCount = result.clientesFinales.size,
                clientesNuevos = result.clientesNuevos,
                clientesDuplicados = result.clientesDuplicados,
                clientesActualizados = result.clientesActualizados,
                erroresDetallados = erroresDetallados,
                duplicadosDetallados = result.duplicadosDetallados
            )
        } catch (e: Exception) {
            android.util.Log.e("ClienteManager", "loadClientesFromJsonContentWithValidation: Error", e)
            ImportResult(false, "Error al procesar el archivo JSON: ${e.message}")
        }
    }

    private fun parseJsonContent(jsonContent: String?): List<Cliente> {
        if (jsonContent == null) {
            android.util.Log.e("ClienteManager", "parseJsonContent: jsonContent es null")
            return emptyList()
        }
        
        return try {
            android.util.Log.d("ClienteManager", "parseJsonContent: Intentando parsear como array")
            // Intentar parsear como array primero
            val arrayType = object : TypeToken<List<Cliente>>() {}.type
            val result = gson.fromJson<List<Cliente>>(jsonContent, arrayType) ?: emptyList()
            android.util.Log.d("ClienteManager", "parseJsonContent: Array parseado exitosamente, ${result.size} clientes")
            result
        } catch (e: Exception) {
            android.util.Log.d("ClienteManager", "parseJsonContent: Error parseando como array, intentando como objeto individual: ${e.message}")
            try {
                // Si falla, intentar parsear como objeto individual
                val singleCliente = gson.fromJson(jsonContent, Cliente::class.java)
                if (singleCliente != null) {
                    android.util.Log.d("ClienteManager", "parseJsonContent: Objeto individual parseado exitosamente")
                    listOf(singleCliente)
                } else {
                    android.util.Log.e("ClienteManager", "parseJsonContent: Objeto individual es null")
                    emptyList()
                }
            } catch (e2: Exception) {
                android.util.Log.d("ClienteManager", "parseJsonContent: Error parseando como objeto individual, intentando como objeto con array interno: ${e2.message}")
                // Si también falla, intentar parsear como objeto con array interno
                try {
                    val jsonObject = gson.fromJson(jsonContent, com.google.gson.JsonObject::class.java)
                    
                    // Intentar con "clientes" primero
                    var clientesArray = jsonObject.getAsJsonArray("clientes")
                    if (clientesArray != null) {
                        val arrayType = object : TypeToken<List<Cliente>>() {}.type
                        val result = gson.fromJson<List<Cliente>>(clientesArray, arrayType) ?: emptyList()
                        android.util.Log.d("ClienteManager", "parseJsonContent: Array 'clientes' parseado exitosamente, ${result.size} clientes")
                        return result
                    }
                    
                    // Intentar con "Contabilidad" (formato específico del archivo)
                    clientesArray = jsonObject.getAsJsonArray("Contabilidad")
                    if (clientesArray != null) {
                        android.util.Log.d("ClienteManager", "parseJsonContent: Encontrado array 'Contabilidad', mapeando campos...")
                        val clientes = mutableListOf<Cliente>()
                        
                        for (element in clientesArray) {
                            try {
                                val clienteJson = element.asJsonObject
                                val cliente = mapJsonToCliente(clienteJson)
                                if (cliente != null) {
                                    clientes.add(cliente)
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("ClienteManager", "parseJsonContent: Error mapeando cliente individual", e)
                            }
                        }
                        
                        android.util.Log.d("ClienteManager", "parseJsonContent: Array 'Contabilidad' mapeado exitosamente, ${clientes.size} clientes")
                        return clientes
                    }
                    
                    android.util.Log.e("ClienteManager", "parseJsonContent: No se encontró array 'clientes' ni 'Contabilidad' en el objeto JSON")
                    emptyList()
                } catch (e3: Exception) {
                    android.util.Log.e("ClienteManager", "parseJsonContent: Error al parsear JSON en todos los formatos", e3)
                    emptyList()
                }
            }
        }
    }
    
    private fun mapJsonToCliente(jsonObject: com.google.gson.JsonObject): Cliente? {
        return try {
            val nombre = jsonObject.get("NOMBRE")?.asString ?: ""
            val cedula = jsonObject.get("CEDULA")?.asString ?: jsonObject.get("CEDULA")?.asNumber?.toString() ?: ""
            val tipoPersona = when (jsonObject.get("FISICO/JURIDICO")?.asString?.lowercase()) {
                "físico", "fisico" -> "Físico"
                "jurídico", "juridico" -> "Jurídico"
                else -> "Físico"
            }
            val representante = jsonObject.get("REPRESENTANTE")?.asString ?: ""
            val telefonoRaw = jsonObject.get("TELEFONO")?.asString ?: jsonObject.get("TELEFONO")?.asNumber?.toString() ?: ""
            
            // Validar teléfono - debe tener exactamente 8 dígitos
            val telefono = if (telefonoRaw.isBlank()) {
                ""
            } else {
                // Limpiar el teléfono de caracteres no numéricos
                val cleanPhone = telefonoRaw.replace(Regex("[^0-9]"), "")
                if (cleanPhone.length == 8) {
                    cleanPhone
                } else {
                    android.util.Log.w("ClienteManager", "mapJsonToCliente: Teléfono inválido: '$telefonoRaw' (${cleanPhone.length} dígitos)")
                    ""
                }
            }
            
            val ciFc = jsonObject.get("CI - FC")?.asString ?: ""
            val ejecutivo = jsonObject.get("EJECUTIVO")?.asString ?: ""
            val patentado = (jsonObject.get("PATENTADO")?.asString?.lowercase() ?: "") in listOf("si", "sí", "true", "1", "yes")
            val pendientePago = (jsonObject.get("Pendientes PAGO?")?.asString?.lowercase() ?: "") in listOf("si", "sí", "true", "1", "yes")
            val tipoRegimen = jsonObject.get("Tipo Régimen")?.asString ?: jsonObject.get("Tipo Regmen")?.asString ?: ""
            
            // Log para debuggear
            android.util.Log.d("ClienteManager", "mapJsonToCliente: Nombre=$nombre, Teléfono original='$telefonoRaw', Teléfono final='$telefono'")
            
            Cliente(
                nombre = nombre,
                cedula = cedula,
                tipoPersona = tipoPersona,
                representante = representante,
                telefono = telefono,
                ciFc = ciFc,
                ejecutivo = ejecutivo,
                patentado = patentado,
                pendientePago = pendientePago,
                tipoRegimen = tipoRegimen
            )
        } catch (e: Exception) {
            android.util.Log.e("ClienteManager", "mapJsonToCliente: Error mapeando cliente", e)
            null
        }
    }

    fun generateExcelTemplate(): ByteArray {
        return try {
            android.util.Log.d("ClienteManager", "generateExcelTemplate: Generando plantilla Excel")
            
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Clientes")
            
            // Crear encabezados
            val headerRow = sheet.createRow(0)
            val headers = arrayOf(
                "Nombre", "Cédula", "Tipo Persona", "Representante", "Teléfono",
                "CI-FC", "Ejecutivo", "Patentado", "Pendiente Pago", "Tipo Régimen"
            )
            
            // Aplicar estilo a los encabezados
            val headerStyle = workbook.createCellStyle()
            val headerFont = workbook.createFont()
            headerFont.bold = true
            headerFont.fontHeightInPoints = 12
            headerStyle.setFont(headerFont)
            headerStyle.fillForegroundColor = org.apache.poi.ss.usermodel.IndexedColors.LIGHT_GREEN.index
            headerStyle.fillPattern = org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND
            
            for (i in headers.indices) {
                val cell = headerRow.createCell(i)
                cell.setCellValue(headers[i])
                cell.cellStyle = headerStyle
            }
            
            // Crear filas de ejemplo
            val exampleData = arrayOf(
                arrayOf("Juan Pérez", "12345678", "Físico", "María García", "12345678", "12345678", "Ejecutivo 1", "No", "No", "General"),
                arrayOf("Empresa ABC", "87654321", "Jurídico", "Carlos López", "87654321", "87654321", "Ejecutivo 2", "Sí", "Sí", "Simplificado"),
                arrayOf("Ana Martínez", "11223344", "Físico", "Pedro Rodríguez", "11223344", "11223344", "Ejecutivo 1", "No", "No", "General")
            )
            
            for (rowIndex in exampleData.indices) {
                val row = sheet.createRow(rowIndex + 1)
                for (colIndex in exampleData[rowIndex].indices) {
                    val cell = row.createCell(colIndex)
                    cell.setCellValue(exampleData[rowIndex][colIndex])
                }
            }
            
            // Ajustar ancho de columnas
            for (i in headers.indices) {
                sheet.setColumnWidth(i, when (i) {
                    0 -> 8000  // Nombre
                    1 -> 4000  // Cédula
                    2 -> 3000  // Tipo Persona
                    3 -> 6000  // Representante
                    4 -> 4000  // Teléfono
                    5 -> 3000  // CI-FC
                    6 -> 4000  // Ejecutivo
                    7 -> 2500  // Patentado
                    8 -> 3000  // Pendiente Pago
                    9 -> 4000  // Tipo Régimen
                    else -> 3000
                })
            }
            
            // Guardar en ByteArrayOutputStream
            val outputStream = ByteArrayOutputStream()
            workbook.write(outputStream)
            workbook.close()
            
            val templateBytes = outputStream.toByteArray()
            android.util.Log.d("ClienteManager", "generateExcelTemplate: Plantilla generada - ${templateBytes.size} bytes")
            
            templateBytes
            
        } catch (e: Exception) {
            android.util.Log.e("ClienteManager", "generateExcelTemplate: Error generando plantilla", e)
            ByteArray(0)
        }
    }

    fun loadClientesFromExcelStreamWithValidation(inputStream: InputStream, password: String): ImportResult {
        return try {
            android.util.Log.d("ClienteManager", "loadClientesFromExcelStreamWithValidation: Iniciando validación Excel")
            
            // Formatear el archivo Excel automáticamente
            val formattedBytes = formatExcelFileToBytes(inputStream, password)
            android.util.Log.d("ClienteManager", "loadClientesFromExcelStreamWithValidation: Archivo formateado - ${formattedBytes.size} bytes")
            
            val workbook = WorkbookFactory.create(ByteArrayInputStream(formattedBytes), password)
            val sheet = workbook.getSheetAt(0)
            
            if (sheet == null) {
                return ImportResult(false, "No se pudo acceder a la primera hoja del archivo Excel")
            }
            
            // Diagnóstico del archivo
            val diagnosticResult = performFileDiagnostic(sheet)
            if (!diagnosticResult.isValid) {
                val errorMessage = buildString {
                    append("❌ DIAGNÓSTICO DEL ARCHIVO FALLÓ\n\n")
                    append("Se detectaron problemas en el archivo Excel:\n")
                    append(diagnosticResult.errorMessage ?: "Error desconocido")
                    append("\n\n📋 INFORMACIÓN DEL ARCHIVO:\n")
                    append("• Total de filas: ${diagnosticResult.totalRows}\n")
                    append("• Total de columnas: ${diagnosticResult.totalColumns}\n")
                    append("• Celdas problemáticas: ${diagnosticResult.problematicCells.size}\n\n")
                    append("🔧 SOLUCIONES RECOMENDADAS:\n")
                    append("1. Verificar que el archivo no esté corrupto\n")
                    append("2. Asegurar que todas las celdas tengan el formato correcto\n")
                    append("3. Evitar fórmulas complejas en las celdas de datos\n")
                    append("4. Guardar el archivo como .xlsx (Excel 2007+)\n")
                    append("5. Usar el script de formateo automático incluido\n\n")
                    append("💡 TIP: Usa el archivo 'FormatearExcel.ps1' para formatear automáticamente tu archivo Excel.")
                }
                return ImportResult(false, errorMessage)
            }
            
            val clientesValidos = mutableListOf<Cliente>()
            val erroresDetallados = mutableListOf<String>()
            
            // Saltar la primera fila (encabezados)
            for (rowIndex in 1..sheet.lastRowNum) {
                val row = sheet.getRow(rowIndex) ?: continue
                
                try {
                    val cliente = parseClienteFromRow(row, rowIndex + 1)
                    val errors = validateCliente(cliente, rowIndex + 1)
                    
                    if (errors.isEmpty()) {
                        clientesValidos.add(cliente)
                    } else {
                        erroresDetallados.add("Fila ${rowIndex + 1} (${cliente.nombre}): ${errors.joinToString(", ")}")
                    }
                } catch (e: Exception) {
                    erroresDetallados.add("Fila ${rowIndex + 1}: Error al procesar - ${e.message}")
                }
            }
            
            workbook.close()
            inputStream.close()
            
            // Procesar importación inteligente (mantener existentes, agregar nuevos)
            val result = processSmartImport(clientesValidos)
            
            // Crear estadísticas detalladas
            val estadisticas = generateDetailedStats(result.clientesFinales)
            
            // Crear mensaje de resultado
            val mensajeResultado = buildString {
                if (result.clientesNuevos > 0 || result.clientesActualizados > 0) {
                    append("✅ Importación exitosa:\n")
                    append("• ${result.clientesNuevos} clientes nuevos importados\n")
                    append("• ${result.clientesActualizados} clientes actualizados\n")
                    append("• ${result.clientesDuplicados} clientes duplicados (mantenidos)\n\n")
                    append("📊 ESTADÍSTICAS DETALLADAS:\n")
                    append(estadisticas)
                }
                if (result.duplicadosDetallados.isNotEmpty()) {
                    if (result.clientesNuevos > 0 || result.clientesActualizados > 0) {
                        append("\n\n")
                    }
                    append("⚠️ Clientes duplicados encontrados (${result.duplicadosDetallados.size}):\n")
                    append(result.duplicadosDetallados.joinToString("\n"))
                }
                if (erroresDetallados.isNotEmpty()) {
                    if (result.clientesNuevos > 0 || result.clientesActualizados > 0 || result.duplicadosDetallados.isNotEmpty()) {
                        append("\n\n")
                    }
                    append("❌ Filas no importadas (${erroresDetallados.size}):\n")
                    append(erroresDetallados.joinToString("\n"))
                }
                if (result.clientesNuevos == 0 && result.clientesActualizados == 0 && erroresDetallados.isEmpty()) {
                    append("No se encontraron clientes válidos para importar")
                }
            }
            
            ImportResult(
                success = result.clientesNuevos > 0 || result.clientesActualizados > 0,
                errorMessage = mensajeResultado,
                clientesCount = result.clientesFinales.size,
                clientesNuevos = result.clientesNuevos,
                clientesDuplicados = result.clientesDuplicados,
                clientesActualizados = result.clientesActualizados,
                erroresDetallados = erroresDetallados,
                duplicadosDetallados = result.duplicadosDetallados,
                formatoCompletado = true
            )
        } catch (e: Exception) {
            android.util.Log.e("ClienteManager", "loadClientesFromExcelStreamWithValidation: Error", e)
            ImportResult(false, "Error al procesar el archivo Excel: ${e.message}")
        }
    }

    private fun formatExcelFileToBytes(inputStream: InputStream, password: String): ByteArray {
        return try {
            android.util.Log.d("ClienteManager", "formatExcelFileToBytes: Iniciando formateo automático")
            
            val workbook = WorkbookFactory.create(inputStream, password)
            val sheet = workbook.getSheetAt(0)
            
            if (sheet == null) {
                android.util.Log.w("ClienteManager", "formatExcelFileToBytes: No se pudo acceder a la primera hoja")
                return ByteArray(0)
            }
            
            val lastRow = sheet.lastRowNum + 1
            android.util.Log.d("ClienteManager", "formatExcelFileToBytes: Filas encontradas: $lastRow")
            
            // Definir encabezados
            val encabezados = arrayOf(
                "Nombre", "Cédula", "Tipo Persona", "Representante", "Teléfono",
                "CI-FC", "Ejecutivo", "Patentado", "Pendiente Pago", "Tipo Régimen"
            )
            
            // Crear/actualizar encabezados en la primera fila
            for (col in 0 until 10) {
                val cell = sheet.getRow(0)?.getCell(col) ?: sheet.createRow(0).createCell(col)
                cell.setCellValue(encabezados[col])
                cell.cellStyle = workbook.createCellStyle().apply {
                    val font = workbook.createFont()
                    font.bold = true
                    setFont(font)
                }
            }
            
            // Formatear datos desde la fila 1 (segunda fila)
            for (rowIndex in 1 until lastRow) {
                val row = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)
                
                // Asegurar que la fila tenga al menos 10 columnas
                for (col in 0 until 10) {
                    val cell = row.getCell(col) ?: row.createCell(col)
                    
                    // Convertir todas las celdas a texto para evitar errores de tipo
                    val cellValue = when (cell.cellType) {
                        CellType.NUMERIC -> {
                            val numericValue = cell.numericCellValue
                            if (numericValue == numericValue.toLong().toDouble()) {
                                // Es un número entero
                                numericValue.toLong().toString()
                            } else {
                                // Es un número decimal
                                numericValue.toString()
                            }
                        }
                        CellType.STRING -> cell.stringCellValue ?: ""
                        CellType.BOOLEAN -> cell.booleanCellValue.toString()
                        CellType.BLANK -> ""
                        CellType.FORMULA -> {
                            // Manejar fórmulas
                            try {
                                when (cell.cachedFormulaResultType) {
                                    CellType.STRING -> cell.stringCellValue ?: ""
                                    CellType.NUMERIC -> {
                                        val numericValue = cell.numericCellValue
                                        if (numericValue == numericValue.toLong().toDouble()) {
                                            numericValue.toLong().toString()
                                        } else {
                                            numericValue.toString()
                                        }
                                    }
                                    CellType.BOOLEAN -> cell.booleanCellValue.toString()
                                    else -> ""
                                }
                            } catch (e: Exception) {
                                ""
                            }
                        }
                        else -> ""
                    }
                    
                    // Aplicar valores por defecto según la columna
                    val finalValue = when (col) {
                        2 -> if (cellValue.isBlank()) "Físico" else cellValue // Tipo Persona
                        4 -> { // Teléfono - validar que tenga 8 dígitos
                            if (cellValue.isBlank()) {
                                ""
                            } else {
                                // Limpiar el teléfono de caracteres no numéricos
                                val cleanPhone = cellValue.replace(Regex("[^0-9]"), "")
                                if (cleanPhone.length == 8) {
                                    cleanPhone
                                } else {
                                    android.util.Log.w("ClienteManager", "formatExcelFileToBytes: Teléfono inválido en fila $rowIndex: '$cellValue' (${cleanPhone.length} dígitos)")
                                    ""
                                }
                            }
                        }
                        7 -> if (cellValue.isBlank()) "No" else cellValue // Patentado
                        8 -> if (cellValue.isBlank()) "No" else cellValue // Pendiente Pago
                        else -> cellValue
                    }
                    
                    // Log para la columna de teléfono en las primeras filas
                    if (col == 4 && rowIndex <= 5) {
                        android.util.Log.d("ClienteManager", "formatExcelFileToBytes: Fila $rowIndex, Col $col (Teléfono): '$cellValue' -> '$finalValue'")
                    }
                    
                    cell.setCellValue(finalValue)
                }
            }
            
            // Ajustar ancho de columnas
            for (col in 0 until 10) {
                sheet.setColumnWidth(col, when (col) {
                    0 -> 8000  // Nombre
                    1 -> 4000  // Cédula
                    2 -> 3000  // Tipo Persona
                    3 -> 6000  // Representante
                    4 -> 4000  // Teléfono
                    5 -> 3000  // CI-FC
                    6 -> 4000  // Ejecutivo
                    7 -> 2500  // Patentado
                    8 -> 3000  // Pendiente Pago
                    9 -> 4000  // Tipo Régimen
                    else -> 3000
                })
            }
            
            // Guardar en ByteArrayOutputStream
            val outputStream = ByteArrayOutputStream()
            workbook.write(outputStream)
            workbook.close()
            
            val formattedBytes = outputStream.toByteArray()
            android.util.Log.d("ClienteManager", "formatExcelFileToBytes: Formateo completado exitosamente - ${formattedBytes.size} bytes")
            
            formattedBytes
            
        } catch (e: Exception) {
            android.util.Log.e("ClienteManager", "formatExcelFileToBytes: Error durante el formateo", e)
            ByteArray(0)
        }
    }

    private fun formatExcelFile(inputStream: InputStream, password: String): InputStream {
        return try {
            android.util.Log.d("ClienteManager", "formatExcelFile: Iniciando formateo automático")
            
            val workbook = WorkbookFactory.create(inputStream, password)
            val sheet = workbook.getSheetAt(0)
            
            if (sheet == null) {
                android.util.Log.w("ClienteManager", "formatExcelFile: No se pudo acceder a la primera hoja")
                return inputStream
            }
            
            // Obtener el rango usado
            val usedRange = sheet
            val lastRow = usedRange.lastRowNum + 1
            // Forzar 10 columnas
            
            android.util.Log.d("ClienteManager", "formatExcelFile: Filas encontradas: $lastRow")
            
            // Definir encabezados estándar
            val encabezados = arrayOf(
                "Nombre", "Cédula", "Tipo Persona", "Representante", "Teléfono",
                "CI-FC", "Ejecutivo", "Patentado", "Pendiente Pago", "Tipo Régimen"
            )
            
            // Establecer encabezados en la primera fila
            for (col in 0 until 10) {
                val cell = sheet.getRow(0)?.getCell(col) ?: sheet.createRow(0).createCell(col)
                cell.setCellValue(encabezados[col])
                cell.cellStyle = workbook.createCellStyle().apply {
                    val font = workbook.createFont()
                    font.bold = true
                    setFont(font)
                }
            }
            
            // Formatear datos desde la fila 1 (segunda fila)
            for (rowIndex in 1 until lastRow) {
                val row = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)
                
                // Log para las primeras filas para debug
                if (rowIndex <= 5) {
                    android.util.Log.d("ClienteManager", "formatExcelFile: Procesando fila $rowIndex")
                }
                
                // Asegurar que la fila tenga al menos 10 columnas
                for (col in 0 until 10) {
                    val cell = row.getCell(col) ?: row.createCell(col)
                    
                    // Convertir todas las celdas a texto para evitar errores de tipo
                    val cellValue = when (cell.cellType) {
                        CellType.NUMERIC -> {
                            val numericValue = cell.numericCellValue
                            if (numericValue == numericValue.toLong().toDouble()) {
                                // Es un número entero
                                numericValue.toLong().toString()
                            } else {
                                // Es un número decimal
                                numericValue.toString()
                            }
                        }
                        CellType.STRING -> cell.stringCellValue ?: ""
                        CellType.BOOLEAN -> cell.booleanCellValue.toString()
                        CellType.BLANK -> ""
                        CellType.FORMULA -> {
                            // Manejar fórmulas
                            try {
                                when (cell.cachedFormulaResultType) {
                                    CellType.STRING -> cell.stringCellValue ?: ""
                                    CellType.NUMERIC -> {
                                        val numericValue = cell.numericCellValue
                                        if (numericValue == numericValue.toLong().toDouble()) {
                                            numericValue.toLong().toString()
                                        } else {
                                            numericValue.toString()
                                        }
                                    }
                                    CellType.BOOLEAN -> cell.booleanCellValue.toString()
                                    else -> ""
                                }
                            } catch (e: Exception) {
                                ""
                            }
                        }
                        else -> ""
                    }
                    
                    // Aplicar valores por defecto según la columna
                    val finalValue = when (col) {
                        2 -> if (cellValue.isBlank()) "Físico" else cellValue // Tipo Persona
                        4 -> { // Teléfono - validar que tenga 8 dígitos
                            if (cellValue.isBlank()) {
                                ""
                            } else {
                                // Limpiar el teléfono de caracteres no numéricos
                                val cleanPhone = cellValue.replace(Regex("[^0-9]"), "")
                                if (cleanPhone.length == 8) {
                                    cleanPhone
                                } else {
                                    android.util.Log.w("ClienteManager", "formatExcelFile: Teléfono inválido en fila $rowIndex: '$cellValue' (${cleanPhone.length} dígitos)")
                                    ""
                                }
                            }
                        }
                        7 -> if (cellValue.isBlank()) "No" else cellValue // Patentado
                        8 -> if (cellValue.isBlank()) "No" else cellValue // Pendiente Pago
                        else -> cellValue
                    }
                    
                    // Log para la columna de teléfono en las primeras filas
                    if (col == 4 && rowIndex <= 5) {
                        android.util.Log.d("ClienteManager", "formatExcelFile: Fila $rowIndex, Col $col (Teléfono): '$cellValue' -> '$finalValue'")
                    }
                    
                    // Establecer el valor como texto
                    cell.setCellValue(finalValue)
                }
            }
            
            // Ajustar ancho de columnas
            for (col in 0 until 10) {
                sheet.setColumnWidth(col, when (col) {
                    0 -> 8000  // Nombre
                    1 -> 4000  // Cédula
                    2 -> 3000  // Tipo Persona
                    3 -> 6000  // Representante
                    4 -> 4000  // Teléfono
                    5 -> 3000  // CI-FC
                    6 -> 4000  // Ejecutivo
                    7 -> 2500  // Patentado
                    8 -> 3000  // Pendiente Pago
                    9 -> 4000  // Tipo Régimen
                    else -> 3000
                })
            }
            
            // Guardar en ByteArrayOutputStream
            val outputStream = ByteArrayOutputStream()
            workbook.write(outputStream)
            workbook.close()
            
            val formattedBytes = outputStream.toByteArray()
            android.util.Log.d("ClienteManager", "formatExcelFile: Formateo completado exitosamente - ${formattedBytes.size} bytes")
            
            // Retornar como InputStream
            ByteArrayInputStream(formattedBytes)
            
        } catch (e: Exception) {
            android.util.Log.e("ClienteManager", "formatExcelFile: Error durante el formateo", e)
            // Si hay error en el formateo, retornar el stream original
            inputStream
        }
    }

    data class FileDiagnostic(
        val isValid: Boolean,
        val errorMessage: String? = null,
        val totalRows: Int = 0,
        val totalColumns: Int = 0,
        val problematicCells: List<String> = emptyList()
    )

    private fun performFileDiagnostic(sheet: Sheet): FileDiagnostic {
        return try {
            android.util.Log.d("ClienteManager", "performFileDiagnostic: Iniciando diagnóstico del archivo")
            
            val totalRows = sheet.lastRowNum + 1
            val totalColumns = 10 // Esperamos 10 columnas
            val problematicCells = mutableListOf<String>()
            
            android.util.Log.d("ClienteManager", "performFileDiagnostic: Total de filas: $totalRows")
            
            // Verificar las primeras 5 filas para detectar problemas
            val sampleRows = minOf(5, totalRows - 1)
            for (rowIndex in 1..sampleRows) {
                val row = sheet.getRow(rowIndex) ?: continue
                
                for (colIndex in 0 until totalColumns) {
                    val cell = row.getCell(colIndex)
                    if (cell != null) {
                        try {
                            // Intentar acceder a los valores para detectar problemas
                            when (cell.cellType) {
                                org.apache.poi.ss.usermodel.CellType.STRING -> {
                                    cell.stringCellValue
                                }
                                org.apache.poi.ss.usermodel.CellType.NUMERIC -> {
                                    cell.numericCellValue
                                }
                                org.apache.poi.ss.usermodel.CellType.BOOLEAN -> {
                                    cell.booleanCellValue
                                }
                                org.apache.poi.ss.usermodel.CellType.FORMULA -> {
                                    cell.cachedFormulaResultType
                                }
                                else -> {
                                    // Celda vacía o tipo desconocido
                                }
                            }
                        } catch (e: Exception) {
                            val errorMsg = "Fila ${rowIndex + 1}, Columna ${colIndex + 1}: ${e.message}"
                            problematicCells.add(errorMsg)
                            android.util.Log.w("ClienteManager", "Celda problemática detectada: $errorMsg")
                        }
                    }
                }
            }
            
            val isValid = problematicCells.isEmpty()
            val errorMessage = if (!isValid) {
                "Se detectaron ${problematicCells.size} celdas problemáticas en el archivo:\n" +
                problematicCells.take(10).joinToString("\n") +
                if (problematicCells.size > 10) "\n... y ${problematicCells.size - 10} más" else ""
            } else null
            
            android.util.Log.d("ClienteManager", "performFileDiagnostic: Diagnóstico completado. Válido: $isValid")
            
            FileDiagnostic(
                isValid = isValid,
                errorMessage = errorMessage,
                totalRows = totalRows,
                totalColumns = totalColumns,
                problematicCells = problematicCells
            )
            
        } catch (e: Exception) {
            android.util.Log.e("ClienteManager", "performFileDiagnostic: Error durante diagnóstico", e)
            FileDiagnostic(
                isValid = false,
                errorMessage = "Error durante el diagnóstico del archivo: ${e.message}"
            )
        }
    }

    private fun generateDetailedStats(clientes: List<Cliente>): String {
        if (clientes.isEmpty()) return "No hay clientes para analizar"
        
        val totalClientes = clientes.size
        val conNombre = clientes.count { it.nombre.isNotBlank() }
        val conCedula = clientes.count { it.cedula.isNotBlank() }
        val conTelefono = clientes.count { it.telefono.isNotBlank() }
        val conRepresentante = clientes.count { it.representante.isNotBlank() }
        val conCiFc = clientes.count { it.ciFc.isNotBlank() }
        val conEjecutivo = clientes.count { it.ejecutivo.isNotBlank() }
        val conTipoRegimen = clientes.count { it.tipoRegimen.isNotBlank() }
        
        val fisicos = clientes.count { it.tipoPersona == "Físico" }
        val juridicos = clientes.count { it.tipoPersona == "Jurídico" }
        val patentados = clientes.count { it.patentado }
        val pendientesPago = clientes.count { it.pendientePago }
        
        // Estadísticas de teléfonos únicos
        val telefonosUnicos = clientes.map { it.telefono }.distinct().count { it.isNotBlank() }
        val telefonosDuplicados = totalClientes - telefonosUnicos
        
        return buildString {
            appendLine("📋 CAMPOS COMPLETADOS:")
            appendLine("• Nombres: $conNombre/$totalClientes (${(conNombre * 100 / totalClientes)}%)")
            appendLine("• Cédulas: $conCedula/$totalClientes (${(conCedula * 100 / totalClientes)}%)")
            appendLine("• Teléfonos: $conTelefono/$totalClientes (${(conTelefono * 100 / totalClientes)}%)")
            appendLine("• Representantes: $conRepresentante/$totalClientes (${(conRepresentante * 100 / totalClientes)}%)")
            appendLine("• CI-FC: $conCiFc/$totalClientes (${(conCiFc * 100 / totalClientes)}%)")
            appendLine("• Ejecutivos: $conEjecutivo/$totalClientes (${(conEjecutivo * 100 / totalClientes)}%)")
            appendLine("• Tipo Régimen: $conTipoRegimen/$totalClientes (${(conTipoRegimen * 100 / totalClientes)}%)")
            appendLine()
            appendLine("👥 TIPOS DE PERSONA:")
            appendLine("• Físicos: $fisicos (${(fisicos * 100 / totalClientes)}%)")
            appendLine("• Jurídicos: $juridicos (${(juridicos * 100 / totalClientes)}%)")
            appendLine()
            appendLine("📞 TELÉFONOS:")
            appendLine("• Teléfonos únicos: $telefonosUnicos")
            appendLine("• Teléfonos duplicados: $telefonosDuplicados")
            appendLine()
            appendLine("📊 ESTADOS:")
            appendLine("• Patentados: $patentados (${(patentados * 100 / totalClientes)}%)")
            appendLine("• Pendientes de pago: $pendientesPago (${(pendientesPago * 100 / totalClientes)}%)")
        }
    }

    private fun validateCliente(cliente: Cliente, rowNumber: Int): List<String> {
        val errors = mutableListOf<String>()
        
        if (cliente.nombre.isNullOrBlank()) {
            errors.add("Fila $rowNumber: El nombre del cliente es obligatorio")
        }
        
        if (cliente.cedula.isNullOrBlank()) {
            errors.add("Fila $rowNumber: La cédula es obligatoria")
        }
        
        if (cliente.tipoPersona.isNullOrBlank() || cliente.tipoPersona !in listOf("Físico", "Jurídico")) {
            errors.add("Fila $rowNumber: El tipo de persona debe ser 'Físico' o 'Jurídico'")
        }
        
        return errors
    }

    private fun parseClienteFromRow(row: Row, rowNumber: Int): Cliente {
        fun getCellValueAsString(cellIndex: Int): String {
            val cell = row.getCell(cellIndex)
            return try {
                when (cell?.cellType) {
                    org.apache.poi.ss.usermodel.CellType.STRING -> cell.stringCellValue?.trim() ?: ""
                    org.apache.poi.ss.usermodel.CellType.NUMERIC -> {
                        // Para teléfonos y números largos, usar toLong() para evitar pérdida de dígitos
                        val numericValue = cell.numericCellValue
                        if (numericValue == numericValue.toLong().toDouble()) {
                            // Es un número entero
                            numericValue.toLong().toString()
                        } else {
                            // Es un número decimal
                            numericValue.toString()
                        }
                    }
                    org.apache.poi.ss.usermodel.CellType.BOOLEAN -> cell.booleanCellValue.toString()
                    org.apache.poi.ss.usermodel.CellType.BLANK -> ""
                    org.apache.poi.ss.usermodel.CellType.FORMULA -> {
                        // Manejar celdas de fórmula
                        try {
                            when (cell.cachedFormulaResultType) {
                                org.apache.poi.ss.usermodel.CellType.STRING -> cell.stringCellValue?.trim() ?: ""
                                org.apache.poi.ss.usermodel.CellType.NUMERIC -> {
                                    val numericValue = cell.numericCellValue
                                    if (numericValue == numericValue.toLong().toDouble()) {
                                        numericValue.toLong().toString()
                                    } else {
                                        numericValue.toString()
                                    }
                                }
                                org.apache.poi.ss.usermodel.CellType.BOOLEAN -> cell.booleanCellValue.toString()
                                else -> ""
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("ClienteManager", "Error al procesar fórmula en fila $rowNumber, columna $cellIndex: ${e.message}")
                            ""
                        }
                    }
                    else -> {
                        // Intentar obtener como string si es posible
                        try {
                            cell.stringCellValue?.trim() ?: ""
                        } catch (e: Exception) {
                            android.util.Log.w("ClienteManager", "Error al procesar celda en fila $rowNumber, columna $cellIndex: ${e.message}")
                            ""
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ClienteManager", "Error crítico al procesar celda en fila $rowNumber, columna $cellIndex: ${e.message}")
                ""
            }
        }
        
        val nombre = getCellValueAsString(0)
        val cedula = getCellValueAsString(1)
        val tipoPersona = getCellValueAsString(2).ifBlank { "Físico" }
        val representante = getCellValueAsString(3)
        
        // Debug específico para la columna de teléfono
        val telefonoCell = row.getCell(4)
        val telefonoRaw = getCellValueAsString(telefonoCell)
        
        // Validar teléfono - debe tener exactamente 8 dígitos
        val telefono = if (telefonoRaw.isNullOrBlank()) {
            ""
        } else {
            // Limpiar el teléfono de caracteres no numéricos
            val cleanPhone = telefonoRaw.replace(Regex("[^0-9]"), "")
            if (cleanPhone.length == 8) {
                cleanPhone
            } else {
                android.util.Log.w("ClienteManager", "parseClienteFromRow: Teléfono inválido en fila $rowNumber: '$telefonoRaw' (${cleanPhone.length} dígitos)")
                ""
            }
        }
        
        android.util.Log.d("ClienteManager", "parseClienteFromRow Fila $rowNumber - Celda teléfono:")
        android.util.Log.d("ClienteManager", "  Tipo de celda: ${telefonoCell?.cellType}")
        android.util.Log.d("ClienteManager", "  Valor numérico: ${if (telefonoCell?.cellType == org.apache.poi.ss.usermodel.CellType.NUMERIC) telefonoCell?.numericCellValue else "N/A"}")
        android.util.Log.d("ClienteManager", "  Valor string: ${telefonoCell?.stringCellValue}")
        android.util.Log.d("ClienteManager", "  Teléfono original: '$telefonoRaw'")
        android.util.Log.d("ClienteManager", "  Teléfono final: '$telefono'")
        
        val ciFc = getCellValueAsString(5)
        val ejecutivo = getCellValueAsString(6)
        val patentado = getCellValueAsString(7).equals("Sí", ignoreCase = true)
        val pendientePago = getCellValueAsString(8).equals("Sí", ignoreCase = true)
        val tipoRegimen = getCellValueAsString(9)
        
        // Log detallado para debuggear
        android.util.Log.d("ClienteManager", "parseClienteFromRow Fila $rowNumber:")
        android.util.Log.d("ClienteManager", "  Nombre: '$nombre'")
        android.util.Log.d("ClienteManager", "  Cédula: '$cedula'")
        android.util.Log.d("ClienteManager", "  Teléfono: '$telefono'")
        android.util.Log.d("ClienteManager", "  Ejecutivo: '$ejecutivo'")
        
        return Cliente(
            nombre = nombre,
            cedula = cedula,
            tipoPersona = tipoPersona,
            representante = representante,
            telefono = telefono,
            ciFc = ciFc,
            ejecutivo = ejecutivo,
            patentado = patentado,
            pendientePago = pendientePago,
            tipoRegimen = tipoRegimen
        )
    }
}

