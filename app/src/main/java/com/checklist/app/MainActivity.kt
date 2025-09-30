package com.checklist.app

import android.Manifest
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.checklist.app.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var questionsAdapter: QuestionsAdapter
    private var isAdminMode = false
    private var isSuperAdminMode = false
    private lateinit var ejecutivoManager: EjecutivoManager
    private lateinit var questionManager: QuestionManager
    private lateinit var clienteManager: ClienteManager
    private lateinit var reportManager: ReportManager
    private lateinit var clienteEstadoManager: ClienteEstadoManager
    
    // BroadcastReceiver para escuchar cambios de configuración
    private val configChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.checklist.app.CONFIG_CHANGED") {
                android.util.Log.d("MainActivity", "ConfigChangeReceiver: Recibido cambio de configuración")
                refreshUIAfterConfigChange()
            }
        }
    }
    private lateinit var prefs: SharedPreferences
    private var currentEjecutivoFilter: Long? = null
    private var allQuestions: List<Question> = emptyList()
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            // Ocultar la ActionBar
            supportActionBar?.hide()
            
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            prefs = getSharedPreferences("checklist_prefs", MODE_PRIVATE)
            ejecutivoManager = EjecutivoManager(this)
            questionManager = QuestionManager(this)
            clienteManager = ClienteManager(this)
            reportManager = ReportManager(this)
            clienteEstadoManager = ClienteEstadoManager(this)
            requestPermissions()
            setupNavigationBar()
            setupQuestionsRecyclerView()
            setupFooter()
            setupFloatingProgressIndicator()
            setupFloatingActionButton()
            
            // Cargar datos esenciales primero (rápido)
            loadEssentialData()
            
            // Operaciones costosas en background (diferidas)
            lifecycleScope.launch {
                try {
                    // Cargar datos no críticos en background
                    loadNonEssentialData()
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error en onCreate background tasks", e)
                }
            }
            
            // Las opciones de configuración ahora están en el menú "Más"
            
            // Verificar si se debe generar un reporte
            if (intent.getBooleanExtra("generateReport", false)) {
                generateReport()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error al inicializar la aplicación: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
    
    private fun setupNavigationBar() {
        // Configurar el Navigation Bar
        binding.navigationBar.setOnItemSelectedListener { item ->
            // Sin animaciones para mejor rendimiento
            when (item.itemId) {
                R.id.nav_questions -> {
                    showClientsActivity()
                    true
                }
                R.id.nav_ejecutivos -> {
                    val intent = Intent(this, EjecutivosActivity::class.java)
                    intent.putExtra("isAdminMode", isAdminMode)
                    intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
                    startActivity(intent)
                    true
                }
                R.id.nav_reports -> {
                    val intent = Intent(this, ReportsActivity::class.java)
                    intent.putExtra("isAdminMode", isAdminMode)
                    intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
                    startActivity(intent)
                    true
                }
                R.id.nav_more -> {
                    showMoreOptionsDialog()
                    true
                }
                else -> false
            }
        }
        
        // Sin animaciones para mejor rendimiento
        // Las opciones de admin ahora están en el menú "Más"
    }
    
    // Métodos de animación eliminados para mejorar el rendimiento
    
    private fun showMoreOptionsDialog() {
        val options = mutableListOf<String>()
        val actions = mutableListOf<() -> Unit>()
        
        // Siempre mostrar configuración
        options.add("Configuración")
        actions.add {
            val intent = Intent(this, ConfigActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
            startActivity(intent)
        }
        
        // Agregar notificaciones
        options.add("Notificaciones")
        actions.add {
            val intent = Intent(this, NotificationsActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
            startActivity(intent)
        }
        
        // Mostrar opciones de admin si está en modo admin
        if (isAdminMode) {
            options.add("Cerrar Sesión Admin")
            actions.add { showLogoutDialog() }
        } else {
            options.add("Modo Admin")
            actions.add { showAdminDialog() }
        }
        
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Más opciones")
        builder.setItems(options.toTypedArray()) { _, which ->
            actions[which].invoke()
        }
        builder.show()
    }
    
    private fun showLogoutDialog() {
        val options = arrayOf("Cerrar Sesión", "Cambiar Contraseña")
        
        AlertDialog.Builder(this)
            .setTitle("Administrador")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // Cerrar sesión
                        logoutAdmin()
                    }
                    1 -> {
                        // Cambiar contraseña
                        showChangePasswordDialog()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun showChangePasswordDialog() {
        val input = EditText(this)
        input.hint = "Nueva contraseña"
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        
        AlertDialog.Builder(this)
            .setTitle("Cambiar Contraseña")
            .setMessage("Ingrese la nueva contraseña de administrador:")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                val newPassword = input.text.toString().trim()
                if (newPassword.isNotEmpty()) {
                    // Guardar nueva contraseña
                    prefs.edit().putString("admin_password", newPassword).apply()
                    Toast.makeText(this, "Contraseña actualizada correctamente", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "La contraseña no puede estar vacía", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    
    private fun showAdminDialog() {
        // Siempre mostrar el diálogo para autenticación
        
        val editText = EditText(this).apply {
            hint = "Contraseña de administrador"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        
        AlertDialog.Builder(this)
            .setTitle("Acceso de Administrador")
            .setMessage("Ingresa la contraseña para acceder al modo administrador")
            .setView(editText)
            .setPositiveButton("Ingresar") { _, _ ->
                val password = editText.text.toString()
                val savedPassword = prefs.getString("admin_password", "")
                
                if (password == "13601360") {
                    // Superadmin
                    isAdminMode = true
                    isSuperAdminMode = true
                    saveAdminSession(true, true)
                    updateAdminButtonText()
                    Toast.makeText(this, "Modo Super Administrador activado", Toast.LENGTH_SHORT).show()
                } else if (password == "morelia") {
                    // Precargar preguntas del checklist
                    isAdminMode = true
                    isSuperAdminMode = false
                    saveAdminSession(true, false)
                    updateAdminButtonText()
                    loadPredefinedQuestions()
                    Toast.makeText(this, "Modo Administrador activado - Preguntas precargadas", Toast.LENGTH_SHORT).show()
                } else if (password == savedPassword && savedPassword.isNotEmpty()) {
                    // Admin normal
                    isAdminMode = true
                    isSuperAdminMode = false
                    saveAdminSession(true, false)
                    updateAdminButtonText()
                    Toast.makeText(this, "Modo Administrador activado", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun showClientsActivity() {
        val intent = Intent(this, QuestionsActivity::class.java)
        intent.putExtra("isAdminMode", isAdminMode)
        intent.putExtra("openNewClientForm", true)
        intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
        startActivity(intent)
    }
    
    private fun initializeDefaultEjecutivos() {
        val ejecutivos = ejecutivoManager.getAllEjecutivos()
        if (ejecutivos.isEmpty()) {
            // Los ejecutivos por defecto se crean automáticamente en EjecutivoManager
            // No necesitamos hacer nada aquí
        }
    }
    
    private fun checkFirstTimeSetup() {
        val isFirstTime = prefs.getBoolean("first_time", true)
        if (isFirstTime) {
            // No mostrar configuración inicial, solo marcar como no primera vez
            prefs.edit().putBoolean("first_time", false).apply()
        } else {
            // Verificar si hay una sesión activa guardada
            val hasActiveSession = prefs.getBoolean("admin_session_active", false)
            if (hasActiveSession) {
                isAdminMode = true
                isSuperAdminMode = prefs.getBoolean("super_admin_session_active", false)
                updateAdminButtonText()
            }
        }
    }
    
    private fun showFirstTimeAdminSetup() {
        val editText = EditText(this).apply {
            hint = "Nueva contraseña de administrador"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        
        AlertDialog.Builder(this)
            .setTitle("Configuración Inicial")
            .setMessage("Esta es la primera vez que usas la aplicación. Por favor, establece una contraseña de administrador personalizada.")
            .setView(editText)
            .setPositiveButton("Guardar") { _, _ ->
                val password = editText.text.toString().trim()
                if (password.length >= 4) {
                    prefs.edit()
                        .putString("admin_password", password)
                        .putBoolean("first_time", false)
                        .apply()
                    Toast.makeText(this, "Contraseña de administrador guardada", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "La contraseña debe tener al menos 4 caracteres", Toast.LENGTH_SHORT).show()
                    showFirstTimeAdminSetup() // Mostrar nuevamente si la contraseña es muy corta
                }
            }
            .setCancelable(false)
            .show()
    }
    
    
    
    private fun logoutAdmin() {
        isAdminMode = false
        isSuperAdminMode = false
        clearAdminSession()
        updateAdminButtonText()
        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
    }
    
    
    private fun setupQuestionsRecyclerView() {
        questionsAdapter = QuestionsAdapter(
            onEditClick = { _ -> 
                // No permitir edición desde la pantalla principal
                Toast.makeText(this, "Edita las preguntas desde la sección Preguntas", Toast.LENGTH_SHORT).show()
            },
            onDeleteClick = { _ -> 
                // No permitir eliminación desde la pantalla principal
                Toast.makeText(this, "Elimina las preguntas desde la sección Preguntas", Toast.LENGTH_SHORT).show()
            },
            onQuestionClick = { question -> 
                // Mostrar detalles de la pregunta
                Toast.makeText(this, "Pregunta: ${question.title}", Toast.LENGTH_SHORT).show()
            },
            onStatusToggle = { question ->
                // Alternar estado de completado
                android.util.Log.d("MainActivity", "onStatusToggle: RECIBIDO - ID: ${question.id}, Título: ${question.title}, Estado actual: ${question.isCompleted}")
                toggleQuestionCompletion(question)
            },
            isAdminMode = { false }, // Siempre false en la pantalla principal
            getEjecutivoName = { ejecutivoId -> getEjecutivoNameById(ejecutivoId) },
            getEjecutivoColor = { ejecutivoId -> getEjecutivoColorById(ejecutivoId) },
            getClienteInfo = { clienteId -> getClienteInfoById(clienteId) },
            getClientePhone = { clienteId -> getClientePhoneById(clienteId) }
        )
        binding.questionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = questionsAdapter
        }
    }
    
    private fun setupEjecutivoButtons() {
        val ejecutivos = ejecutivoManager.getAllEjecutivos()
        
        android.util.Log.d("MainActivity", "setupEjecutivoButtons: ${ejecutivos.size} ejecutivos encontrados")
        ejecutivos.forEach { ejecutivo ->
            android.util.Log.d("MainActivity", "  Ejecutivo: ${ejecutivo.name}, Color: ${ejecutivo.color}")
        }
        
        // Configurar botón "TODOS"
        binding.btnAllEjecutivos.setOnClickListener {
            filterByEjecutivo(null)
        }
        
        // Crear botones dinámicamente para cada ejecutivo
        val layout = binding.ejecutivoFilterLayout
        layout.removeAllViews() // Limpiar botones existentes
        
        // Agregar botón "TODOS" primero
        val btnAll = binding.btnAllEjecutivos
        layout.addView(btnAll)
        
        // Si no hay ejecutivos, mostrar mensaje
        if (ejecutivos.isEmpty()) {
            android.util.Log.w("MainActivity", "setupEjecutivoButtons: No hay ejecutivos disponibles")
            return
        }
        
        // Agregar botones para cada ejecutivo
        ejecutivos.forEach { ejecutivo ->
            val button = android.widget.Button(this).apply {
                id = View.generateViewId()
                text = ejecutivo.name
                textSize = 10f
                // Usar el color asignado al ejecutivo
                try {
                    val color = android.graphics.Color.parseColor(ejecutivo.color)
                    setBackgroundColor(color)
                    setTextColor(android.graphics.Color.WHITE)
                    android.util.Log.d("MainActivity", "Botón creado para ejecutivo '${ejecutivo.name}' con color ${ejecutivo.color}")
                } catch (e: Exception) {
                    // Si hay error con el color, usar color por defecto
                    setBackgroundColor(resources.getColor(com.checklist.app.R.color.gray_300, null))
                    setTextColor(resources.getColor(com.checklist.app.R.color.black, null))
                    android.util.Log.w("MainActivity", "Error al parsear color '${ejecutivo.color}' para ejecutivo '${ejecutivo.name}': ${e.message}")
                }
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(8, 0, 0, 0)
                }
                minWidth = 60
                setPadding(8, 8, 8, 8)

                setOnClickListener {
                    filterByEjecutivo(ejecutivo.id)
                }
            }
            layout.addView(button)
        }
        
        // Configurar botón de generar informe
        binding.generateReportButton.setOnClickListener {
            generateReport()
        }
        
        // Inicializar estado de botones
        updateEjecutivoButtonStates()
    }
    
    
    
    private fun filterByEjecutivo(ejecutivoId: Long?) {
        android.util.Log.d("MainActivity", "filterByEjecutivo: Filtro aplicado: $ejecutivoId")
        currentEjecutivoFilter = ejecutivoId
        updateEjecutivoButtonStates()
        // Solo actualizar la UI con los datos existentes, no recargar
        updateQuestionsUI(allQuestions)
    }
    
    private fun updateEjecutivoButtonStates() {
        val ejecutivos = ejecutivoManager.getAllEjecutivos()
        val layout = binding.ejecutivoFilterLayout
        
        // Actualizar botón "TODOS"
        val isAllSelected = currentEjecutivoFilter == null
        if (isAllSelected) {
            binding.btnAllEjecutivos.setBackgroundColor(resources.getColor(com.checklist.app.R.color.green_500, null))
            binding.btnAllEjecutivos.setTextColor(resources.getColor(com.checklist.app.R.color.white, null))
        } else {
            binding.btnAllEjecutivos.setBackgroundColor(resources.getColor(com.checklist.app.R.color.gray_300, null))
            binding.btnAllEjecutivos.setTextColor(resources.getColor(com.checklist.app.R.color.black, null))
        }
        
        // Actualizar botones de ejecutivos
        for (i in 1 until layout.childCount) {
            val button = layout.getChildAt(i) as android.widget.Button
            val ejecutivo = ejecutivos[i - 1]
            val isSelected = currentEjecutivoFilter == ejecutivo.id
            
            if (isSelected) {
                // Cuando está seleccionado, usar color más oscuro
                try {
                    val color = android.graphics.Color.parseColor(ejecutivo.color)
                    val darkerColor = android.graphics.Color.argb(
                        255,
                        (android.graphics.Color.red(color) * 0.7).toInt(),
                        (android.graphics.Color.green(color) * 0.7).toInt(),
                        (android.graphics.Color.blue(color) * 0.7).toInt()
                    )
                    button.setBackgroundColor(darkerColor)
                    button.setTextColor(android.graphics.Color.WHITE)
                } catch (e: Exception) {
                    button.setBackgroundColor(resources.getColor(com.checklist.app.R.color.green_500, null))
                    button.setTextColor(resources.getColor(com.checklist.app.R.color.white, null))
                }
            } else {
                // Cuando no está seleccionado, usar el color original del ejecutivo
                try {
                    val color = android.graphics.Color.parseColor(ejecutivo.color)
                    button.setBackgroundColor(color)
                    button.setTextColor(android.graphics.Color.WHITE)
                } catch (e: Exception) {
                    button.setBackgroundColor(resources.getColor(com.checklist.app.R.color.gray_300, null))
                    button.setTextColor(resources.getColor(com.checklist.app.R.color.black, null))
                }
            }
        }
    }
    
    
    private fun getEjecutivoNameById(ejecutivoId: Long): String {
        val ejecutivo = ejecutivoManager.getAllEjecutivos().find { it.id == ejecutivoId }
        return ejecutivo?.name ?: "Ejecutivo desconocido"
    }
    
    private fun getEjecutivoColorById(ejecutivoId: Long): String {
        val ejecutivo = ejecutivoManager.getAllEjecutivos().find { it.id == ejecutivoId }
        return ejecutivo?.color ?: "#FF6200EE" // Color por defecto púrpura
    }
    
    private fun getClienteInfoById(clienteId: Long?): String {
        if (clienteId == null) return ""
        if (!::clienteManager.isInitialized) return ""
        val cliente = clienteManager.getClienteById(clienteId)
        return if (cliente != null) {
            "${cliente.nombre} - ${cliente.cedula}"
        } else {
            ""
        }
    }
    
    private fun getClientePhoneById(clienteId: Long?): String {
        if (clienteId == null) return ""
        if (!::clienteManager.isInitialized) return ""
        val cliente = clienteManager.getClienteById(clienteId)
        val telefono = if (cliente != null) {
            cliente.telefono
        } else {
            ""
        }
        android.util.Log.d("MainActivity", "getClientePhoneById: Cliente ID=$clienteId, Teléfono='$telefono'")
        return telefono
    }
    
    private fun getRandomColorForEjecutivo(): String {
        val colors = listOf(
            "#FF2196F3", // Azul
            "#FF4CAF50", // Verde
            "#FFFF9800", // Naranja
            "#FF9C27B0", // Púrpura
            "#FFE91E63", // Rosa
            "#FF00BCD4", // Cian
            "#FF795548", // Marrón
            "#FF607D8B", // Azul gris
            "#FF3F51B5", // Índigo
            "#FF8BC34A"  // Verde claro
        )
        return colors.random()
    }
    
    private fun ensureAllClientsHaveQuestions() {
        if (!::clienteManager.isInitialized) return
        
        val allClientes = clienteManager.getAllClientes()
        val existingQuestions = questionManager.getQuestionsOrderedByPosition()
        val existingClienteIds = existingQuestions.mapNotNull { it.clienteId }.toSet()
        
        // Si no hay clientes nuevos, no hacer nada
        if (allClientes.all { it.id in existingClienteIds }) {
            return
        }
        
        // Obtener ejecutivos disponibles
        val ejecutivos = ejecutivoManager.getAllEjecutivos()
        val defaultEjecutivoId = 0L
        var nextPosition = questionManager.getNextPosition()
        
        // Crear ejecutivos únicos basados en los clientes (solo los necesarios)
        val ejecutivosUnicos = allClientes
            .map { it.ejecutivo }
            .filter { it.isNotBlank() }
            .distinct()
        
        // Crear ejecutivos automáticamente para cada nombre único (en lote)
        val ejecutivosNuevos = mutableListOf<Ejecutivo>()
        ejecutivosUnicos.forEach { nombreEjecutivo ->
            val ejecutivoExistente = ejecutivos.find { it.name.equals(nombreEjecutivo, ignoreCase = true) }
            if (ejecutivoExistente == null) {
                val nuevoEjecutivo = Ejecutivo(
                    name = nombreEjecutivo,
                    color = getRandomColorForEjecutivo()
                )
                ejecutivosNuevos.add(nuevoEjecutivo)
            }
        }
        
        // Agregar todos los ejecutivos nuevos de una vez
        ejecutivosNuevos.forEach { ejecutivoManager.addEjecutivo(it) }
        
        // Actualizar la lista de ejecutivos después de crear los nuevos
        val ejecutivosActualizados = ejecutivoManager.getAllEjecutivos()
        
        // Crear preguntas para clientes que no tienen una entrada (en lote)
        val isInitialStatePendiente = prefs.getBoolean("client_initial_state_pendiente", true)
        val initialCompleted = !isInitialStatePendiente
        val estadoInicial = if (isInitialStatePendiente) {
            ClienteEstadoManager.ESTADO_PENDIENTE
        } else {
            ClienteEstadoManager.ESTADO_PAGADO
        }
        val questionsToAdd = mutableListOf<Question>()
        
        for (cliente in allClientes) {
            if (cliente.id !in existingClienteIds) {
                val ejecutivoCorrecto = ejecutivosActualizados.find { it.name.equals(cliente.ejecutivo, ignoreCase = true) }
                val ejecutivoId = ejecutivoCorrecto?.id ?: defaultEjecutivoId
                
                val question = Question(
                    title = cliente.nombre,
                    subtitle = cliente.cedula,
                    ejecutivoId = ejecutivoId,
                    position = nextPosition++,
                    clienteId = cliente.id,
                    isCompleted = initialCompleted
                )
                questionsToAdd.add(question)
                
                // Inicializar estado en la tabla de estados
                clienteEstadoManager.updateEstadoCliente(cliente.id, estadoInicial, ejecutivoId)
            }
        }
        
        // Agregar todas las preguntas de una vez
        questionsToAdd.forEach { questionManager.addQuestion(it) }
        
        if (questionsToAdd.isNotEmpty()) {
            android.util.Log.d("MainActivity", "ensureAllClientsHaveQuestions: ${questionsToAdd.size} clientes inicializados con estado: $estadoInicial")
        }
    }
    
    private fun precargarClientes() {
        lifecycleScope.launch {
            try {
                val success = clienteManager.precargarClientes()
                if (success) {
                    val clientesCount = clienteManager.getAllClientes().size
                    android.util.Log.d("MainActivity", "precargarClientes: $clientesCount clientes cargados automáticamente")
                    // No mostrar Toast, carga silenciosa
                } else {
                    android.util.Log.d("MainActivity", "precargarClientes: No se encontraron archivos de clientes")
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "precargarClientes: Error al cargar clientes", e)
            }
        }
    }
    
    private fun loadQuestions() {
        // Solo cargar si no hay datos o si es la primera vez
        if (allQuestions.isNotEmpty()) {
            android.util.Log.d("MainActivity", "loadQuestions: Ya hay datos cargados, omitiendo recarga")
            return
        }
        
        // Mostrar indicador de carga
        runOnUiThread {
            binding.progressBarLayout.visibility = View.VISIBLE
            binding.progressText.text = "Cargando datos..."
        }
        
        // Ejecutar operaciones costosas en background
        lifecycleScope.launch {
            try {
                // Primero, asegurar que todos los clientes precargados tengan una entrada en la lista
                ensureAllClientsHaveQuestions()

                val questions = questionManager.getQuestionsOrderedByPosition()
                allQuestions = questions
                
                // Limpiar clientes duplicados (solo si es necesario)
                val allClientes = clienteManager.getAllClientes()
                if (allClientes.size > 100) { // Solo limpiar si hay muchos clientes
                    clienteManager.cleanDuplicateClientes()
                }
                
                // Asegurar que existe el ejecutivo "Todos" (ID 0)
                val ejecutivos = ejecutivoManager.getAllEjecutivos()
                if (ejecutivos.none { it.id == 0L }) {
                    val todosEjecutivo = Ejecutivo(
                        id = 0,
                        name = "Todos",
                        color = "#FF9E9E9E" // Gris
                    )
                    ejecutivoManager.addEjecutivo(todosEjecutivo)
                }
                
                // Actualizar UI en el hilo principal
                runOnUiThread {
                    binding.progressBarLayout.visibility = View.GONE
                    updateQuestionsUI(questions)
                    // Configurar botones de ejecutivo después de cargar datos
                    setupEjecutivoButtons()
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error en loadQuestions", e)
                runOnUiThread {
                    binding.progressBarLayout.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "Error al cargar datos", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun updateQuestionsUI(questions: List<Question>) {
        
        // Aplicar filtros
        var filteredQuestions = questions
        
        // Aplicar filtro por ejecutivo si está activo
        if (currentEjecutivoFilter != null) {
            android.util.Log.d("MainActivity", "updateQuestionsUI: Aplicando filtro por ejecutivo: $currentEjecutivoFilter")
            filteredQuestions = filteredQuestions.filter { question ->
                val matches = question.ejecutivoId == currentEjecutivoFilter
                android.util.Log.d("MainActivity", "updateQuestionsUI: Pregunta ${question.title} - Ejecutivo: ${question.ejecutivoId} - Coincide: $matches")
                matches
            }
            android.util.Log.d("MainActivity", "updateQuestionsUI: Filtro por ejecutivo aplicado: ${filteredQuestions.size} preguntas")
        }
        
        
        // Ordenar alfabéticamente por ejecutivo y luego por nombre del cliente (A-Z)
        filteredQuestions = filteredQuestions.sortedWith(compareBy<Question> { question ->
            // Primero ordenar por ejecutivo
            val ejecutivo = ejecutivoManager.getEjecutivoById(question.ejecutivoId)
            ejecutivo?.name ?: "Sin ejecutivo"
        }.thenBy { question ->
            // Luego ordenar por nombre del cliente dentro de cada ejecutivo
            val cliente = clienteManager.getClienteById(question.clienteId ?: -1)
            if (cliente != null) {
                cliente.nombre.trim().uppercase()
            } else {
                question.title.trim().uppercase()
            }
        })
        
        android.util.Log.d("MainActivity", "loadQuestions: Ordenamiento por ejecutivo y alfabético aplicado: ${filteredQuestions.size} preguntas")
        
        questionsAdapter.submitList(filteredQuestions)
        
        // Mostrar/ocultar título según si hay preguntas
        binding.titleText.visibility = if (filteredQuestions.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        
        // Actualizar indicador de proceso
        updateProgressIndicator(filteredQuestions)
    }
    
    
    private fun updateProgressIndicator(questions: List<Question>) {
        val completedCount = questions.count { it.isCompleted }
        val totalCount = questions.size
        
        if (totalCount == 0) {
            // Si no hay preguntas, mostrar mensaje por defecto
            binding.progressText.text = "Progreso: 0/0 (0%)"
            return
        }
        
        // Actualizar el texto del progreso en formato "Progreso: X/Y (Z%)"
        val progressPercentage = if (totalCount > 0) (completedCount * 100) / totalCount else 0
        binding.progressText.text = "Progreso: $completedCount/$totalCount ($progressPercentage%)"
    }
    
    private fun toggleQuestionCompletion(question: Question) {
        android.util.Log.d("MainActivity", "toggleQuestionCompletion: INICIANDO - ID: ${question.id}, Estado actual: ${question.isCompleted}")
        
        val clienteId = question.clienteId ?: 0L
        val nuevoEstado = if (question.isCompleted) ClienteEstadoManager.ESTADO_PENDIENTE else ClienteEstadoManager.ESTADO_PAGADO
        val ejecutivoId = question.ejecutivoId
        
        // Actualizar estado en la nueva tabla de estados
        clienteEstadoManager.updateEstadoCliente(clienteId, nuevoEstado, ejecutivoId)
        
        // Actualizar también la pregunta para mantener compatibilidad
        val updatedQuestion = question.copy(isCompleted = !question.isCompleted)
        questionManager.updateQuestion(updatedQuestion)
        android.util.Log.d("MainActivity", "toggleQuestionCompletion: Estado actualizado en tabla de estados: $nuevoEstado")
        
        // Actualizar la lista local allQuestions
        val updatedQuestions = allQuestions.map { 
            if (it.id == question.id) updatedQuestion else it 
        }
        allQuestions = updatedQuestions
        
        // Forzar actualización completa del adapter
        android.util.Log.d("MainActivity", "toggleQuestionCompletion: Forzando actualización completa del adapter")
        questionsAdapter.submitList(updatedQuestions.toList())
        
        // Actualizar indicador de progreso
        updateProgressIndicator(updatedQuestions)
        
        // Determinar el mensaje correcto según el estado final
        val status = if (updatedQuestion.isCompleted) "Pagado" else "Pendiente"
        val mensaje = if (updatedQuestion.isCompleted) {
            "Cliente se ha marcado como pagado"
        } else {
            "Cliente se ha marcado como pendiente"
        }
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
        android.util.Log.d("MainActivity", "toggleQuestionCompletion: COMPLETADO - Estado final: $status")
    }
    
    private fun generateReport() {
        val questions = questionManager.getQuestionsOrderedByPosition()
        val ejecutivos = ejecutivoManager.getAllEjecutivos()
        
        if (questions.isEmpty()) {
            Toast.makeText(this, "No hay preguntas para generar el reporte", Toast.LENGTH_SHORT).show()
            return
        }
        
        showReportFormDialog(questions, ejecutivos)
    }
    
    private fun showReportFormDialog(questions: List<Question>, ejecutivos: List<Ejecutivo>) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_report_form, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.nameEditText)
        val positionEditText = dialogView.findViewById<EditText>(R.id.positionEditText)
        val supervisorEditText = dialogView.findViewById<EditText>(R.id.supervisorEditText)
        val commentsEditText = dialogView.findViewById<EditText>(R.id.commentsEditText)
        
        AlertDialog.Builder(this)
            .setTitle("Generar Reporte")
            .setView(dialogView)
            .setPositiveButton("Generar Reporte") { _, _ ->
                val name = nameEditText.text.toString().trim()
                val position = positionEditText.text.toString().trim()
                val supervisor = supervisorEditText.text.toString().trim()
                val comments = commentsEditText.text.toString().trim()
                
                if (name.isEmpty() || position.isEmpty() || supervisor.isEmpty()) {
                    Toast.makeText(this, "Los campos Nombre, Puesto y Jefe Directo son obligatorios", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                val reportInfo = ReportInfo(
                    name = name,
                    position = position,
                    supervisor = supervisor,
                    comments = comments
                )
                
                generateReportWithInfo(questions, ejecutivos, reportInfo)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    
    private fun generateReportWithInfo(questions: List<Question>, ejecutivos: List<Ejecutivo>, reportInfo: ReportInfo) {
        try {
            // SIEMPRE obtener las preguntas más recientes de la base de datos
            // para asegurar que reflejen los cambios de configuración
            val currentQuestions = questionManager.getQuestionsOrderedByPosition()
            android.util.Log.d("MainActivity", "generateReportWithInfo: Obteniendo preguntas frescas de BD (${currentQuestions.size} preguntas)")
            
            // Actualizar allQuestions con los datos frescos
            allQuestions = currentQuestions
            
            val updatedEjecutivos = ejecutivoManager.getAllEjecutivos()
            
            // Obtener estados desde la nueva tabla de estados
            val estados = clienteEstadoManager.getAllEstados()
            android.util.Log.d("MainActivity", "generateReportWithInfo: Estados desde tabla - ${estados.size} registros")
            
            // Log para verificar estados antes de generar PDF
            val pendingCount = currentQuestions.count { question ->
                val clienteId = question.clienteId ?: 0L
                clienteEstadoManager.isClientePendiente(clienteId)
            }
            val paidCount = currentQuestions.count { question ->
                val clienteId = question.clienteId ?: 0L
                clienteEstadoManager.isClientePagado(clienteId)
            }
            android.util.Log.d("MainActivity", "generateReportWithInfo: Estados desde tabla - Pendientes: $pendingCount, Pagados: $paidCount")
            
            // Log detallado de cada pregunta para debugging
            currentQuestions.forEach { question ->
                val clienteId = question.clienteId ?: 0L
                val estado = clienteEstadoManager.getEstadoString(clienteId)
                android.util.Log.d("MainActivity", "generateReportWithInfo: Pregunta ${question.title} - isCompleted: ${question.isCompleted}, Estado tabla: $estado")
            }
            
            // Generar PDF con información del formulario
            val pdfGenerator = PdfGenerator(this)
            val checklistTitle = prefs.getString("checklist_title", "")
            val filePath = pdfGenerator.generateQuestionsReportWithEstados(currentQuestions, updatedEjecutivos, reportInfo, checklistTitle, clienteEstadoManager)
            
            if (filePath != null) {
                // Guardar información del reporte con la ruta del archivo
                val reportWithPath = reportInfo.copy(filePath = filePath)
                val reportId = reportManager.saveReport(reportWithPath)
                
                Toast.makeText(this, "Reporte generado exitosamente", Toast.LENGTH_SHORT).show()
                
                // Mostrar opción para abrir el reporte
                showOpenReportDialog(filePath, reportId)
            } else {
                Toast.makeText(this, "Error al generar el reporte", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error al generar el reporte: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
    
    private fun showOpenReportDialog(filePath: String, reportId: Long) {
        AlertDialog.Builder(this)
            .setTitle("Reporte Generado")
            .setMessage("¿Deseas abrir el reporte PDF ahora?")
            .setPositiveButton("Abrir PDF") { _, _ ->
                openPdfFile(filePath)
            }
            .setNegativeButton("Más tarde", null)
            .show()
    }
    
    private fun openPdfFile(filePath: String) {
        try {
            val file = java.io.File(filePath)
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/pdf")
            intent.flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo abrir el PDF. Instala una aplicación para ver PDFs.", Toast.LENGTH_LONG).show()
        }
    }
    
    
    private fun requestPermissions() {
        try {
            val permissions = mutableListOf<String>()
            
            // Solo solicitar permisos esenciales
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.VIBRATE) 
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.VIBRATE)
            }
            
            if (permissions.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    this,
                    permissions.toTypedArray(),
                    PERMISSION_REQUEST_CODE
                )
            }
        } catch (e: Exception) {
            // Si hay error con permisos, continuar sin ellos
            Toast.makeText(this, "Error al solicitar permisos: ${e.message}", Toast.LENGTH_SHORT).show()
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
                if (!allPermissionsGranted) {
                    Toast.makeText(
                        this,
                        "Algunos permisos fueron denegados. La aplicación puede no funcionar correctamente.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        try {
            // Registrar BroadcastReceiver para cambios de configuración
            val filter = IntentFilter("com.checklist.app.CONFIG_CHANGED")
            registerReceiver(configChangeReceiver, filter)
            
            // Solo recargar si es necesario (evitar recargas innecesarias)
            if (allQuestions.isEmpty()) {
                loadQuestions()
            } else {
                // Si ya hay datos, solo actualizar la UI
                updateQuestionsUI(allQuestions)
            }
            
            // Verificar si debe mostrar el tutorial (solo si no está en modo admin)
            // y solo si no se está mostrando ya
            if (!isAdminMode && !isFinishing) {
                checkTutorial()
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error en onResume", e)
            Toast.makeText(this, "Error al reanudar la aplicación: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Desregistrar BroadcastReceiver
        try {
            unregisterReceiver(configChangeReceiver)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error desregistrando BroadcastReceiver", e)
        }
    }
    
    private fun loadPredefinedQuestions() {
        try {
            // Obtener la categoría "General" o crearla si no existe
            val ejecutivos = ejecutivoManager.getAllEjecutivos()
            val defaultEjecutivoId = ejecutivos.firstOrNull()?.id ?: 1L
            
            val predefinedQuestions = listOf(
                Question(
                    title = "Sistema de Vacío: Es importante verificar que el sistema esté lubricando correctamente. Si se observa que no está funcionando adecuadamente, es necesario identificar la causa del problema y tomar medidas para corregirlo.",
                    subtitle = "Control Niro 40",
                    ejecutivoId = defaultEjecutivoId,
                    position = 1
                ),
                Question(
                    title = "Bombas Nivel 0: Verificar la presencia de daños en los sellos. Se deben realizar una inspección visual para garantizar que cada bomba este lubricando correctamente.",
                    subtitle = "Control Niro 40",
                    ejecutivoId = defaultEjecutivoId,
                    position = 2
                ),
                Question(
                    title = "Prueba Turbidez: Se debe realizar la prueba de Turbidez con uno de los operadores, y el jefe de planta en turno, ya que es crucial para garantizar la seguridad del sistema de desvío pluvial/Aguas Industriales.",
                    subtitle = "Control Niro 40",
                    ejecutivoId = defaultEjecutivoId,
                    position = 3
                ),
                Question(
                    title = "Nivel de Aceite: Es importante verificar que el nivel de aceite sea el correcto. Detrás del equipo tiene una mirilla vertical la cual debe estar marcando entre en el nivel máximo o medio para trabajar correctamente.",
                    subtitle = "Control Niro 40",
                    ejecutivoId = defaultEjecutivoId,
                    position = 4
                ),
                Question(
                    title = "Purga del sistema hidráulico de Pistones: Es fundamental purgar el sistema hidráulico antes de ponerlo en funcionamiento para eliminar el aire presente en el sistema.",
                    subtitle = "Control Niro 40",
                    ejecutivoId = defaultEjecutivoId,
                    position = 5
                ),
                Question(
                    title = "Presión de Compresión: Se debe comprobar que la presión sea la correcta en los \"Pulmones del equipo\" la cual debe de estar en 0.5 bar.",
                    subtitle = "Control Niro 40",
                    ejecutivoId = defaultEjecutivoId,
                    position = 6
                ),
                Question(
                    title = "Estado General: Realice una revisión visual para garantizar que no tengamos desgastes prematuros en fajas, no presenten sonidos extraños, no existan fugas en componentes neumáticos o en elementos del equipo.",
                    subtitle = "Control Niro 40",
                    ejecutivoId = defaultEjecutivoId,
                    position = 7
                ),
                Question(
                    title = "Comprobar que el suministro de aire comprimido esté funcionando correctamente y que la presión del aire esté dentro de los parámetros recomendados para el sistema.",
                    subtitle = "Control Niro 40",
                    ejecutivoId = defaultEjecutivoId,
                    position = 8
                ),
                Question(
                    title = "Verificar que no existan fugas de agua, aire o vapor en equipos. Realizar una revisión general del estado de válvulas (que no presentes luces intermitentes o fallas).",
                    subtitle = "Control Niro 40",
                    ejecutivoId = defaultEjecutivoId,
                    position = 9
                ),
                Question(
                    title = "Verificar que no existan fugas de agua, aire o lecitina en el equipo.",
                    subtitle = "Control Niro 40",
                    ejecutivoId = defaultEjecutivoId,
                    position = 10
                ),
                Question(
                    title = "Realizar una revisión general del estado de válvulas y componentes que lo conforman (sensores, manetas, cableado).",
                    subtitle = "Control Niro 40",
                    ejecutivoId = defaultEjecutivoId,
                    position = 11
                ),
                Question(
                    title = "Comprobar que los valores o parámetros del equipo estén en los rangos operativos correctos. (presión de inyección, suministro de aire, niveles de agua 2bar).",
                    subtitle = "Control Niro 40",
                    ejecutivoId = defaultEjecutivoId,
                    position = 12
                ),
                Question(
                    title = "Verificar que no existan mangas dañas.",
                    subtitle = "Control Niro 40",
                    ejecutivoId = defaultEjecutivoId,
                    position = 13
                ),
                Question(
                    title = "Comprobar que no existan fugas de agua, aire o producto en el equipo.",
                    subtitle = "Control Niro 40",
                    ejecutivoId = defaultEjecutivoId,
                    position = 14
                ),
                Question(
                    title = "Realizar una revisión general del estado de válvulas y componentes que lo conforman (sensores, manetas, cableado, tapas o empaques).",
                    subtitle = "Control Niro 40",
                    ejecutivoId = defaultEjecutivoId,
                    position = 15
                ),
                Question(
                    title = "Realice una revisión visual para garantizar que no tengamos desgastes o daños prematuros en fajas, acoples, muñoneras o mangas.",
                    subtitle = "Control Niro 40",
                    ejecutivoId = defaultEjecutivoId,
                    position = 16
                ),
                Question(
                    title = "Comprobar que no existan fugas de aire en válvulas.",
                    subtitle = "Control Niro 40",
                    ejecutivoId = defaultEjecutivoId,
                    position = 17
                ),
                Question(
                    title = "Verificar que el soplador tenga un nivel correcto de aceite.",
                    subtitle = "Control Niro 40",
                    ejecutivoId = defaultEjecutivoId,
                    position = 18
                ),
                Question(
                    title = "Verificar la presencia de daños en los sellos: Se deben realizar una inspección visual para garantizar que cada bomba este lubricando correctamente.",
                    subtitle = "Control Niro 40",
                    ejecutivoId = defaultEjecutivoId,
                    position = 19
                ),
                Question(
                    title = "Comprobar que no existan fugas de agua, aire o vapor en el sistema.",
                    subtitle = "Control Niro 40",
                    ejecutivoId = defaultEjecutivoId,
                    position = 20
                ),
                Question(
                    title = "Realice una revisión visual para garantizar que no tengamos desgastes o daños prematuros empaques de tapas, mangueras o electroválvulas.",
                    subtitle = "Control Niro 40",
                    ejecutivoId = defaultEjecutivoId,
                    position = 21
                ),
                Question(
                    title = "Compruebe que no existan fugas de aire en mangueras o válvulas.",
                    subtitle = "Control Niro 40",
                    ejecutivoId = defaultEjecutivoId,
                    position = 22
                ),
                Question(
                    title = "Compruebe con ayuda del operador el correcto funcionamiento de las válvulas de desvío, cambiando el sentido de envió manualmente, luego coloque el sistema en automático cuando termine de realizar las pruebas.",
                    subtitle = "Control Niro 40",
                    ejecutivoId = defaultEjecutivoId,
                    position = 23
                ),
                Question(
                    title = "Comprobar que no existan fugas de aire o producto.",
                    subtitle = "Control Niro 40",
                    ejecutivoId = defaultEjecutivoId,
                    position = 24
                ),
                Question(
                    title = "Compruebe que no existan fugas de aire o vapor en los sistemas.",
                    subtitle = "Control Niro 40",
                    ejecutivoId = defaultEjecutivoId,
                    position = 25
                ),
                Question(
                    title = "Observar el estado de los sensores inductivos, y verificar que la configuración a la cual se va a trabajar sea la correcta. (Low-Heat) (High- Heat).",
                    subtitle = "Control Niro 40",
                    ejecutivoId = defaultEjecutivoId,
                    position = 26
                ),
                Question(
                    title = "Verificar que el nivel de agua del tanque sea el correcto.",
                    subtitle = "Control Niro 40",
                    ejecutivoId = defaultEjecutivoId,
                    position = 27
                ),
                Question(
                    title = "Compruebe que no existan fugas de aire en mangueras o válvulas.",
                    subtitle = "Control Niro 40",
                    ejecutivoId = defaultEjecutivoId,
                    position = 28
                ),
                Question(
                    title = "Verificar que la presión principal de entrada se encuentre como mínimo 5,5 Bar.",
                    subtitle = "Control Niro 40",
                    ejecutivoId = defaultEjecutivoId,
                    position = 29
                ),
                Question(
                    title = "Realizar una revisión general del estado de válvulas y componentes que lo conforman (sensores, cableado, tapas o empaques).",
                    subtitle = "Control Niro 40",
                    ejecutivoId = defaultEjecutivoId,
                    position = 30
                ),
                Question(
                    title = "Realice una revisión visual para garantizar que no tengamos desgastes o daños en empaques, mangueras, o electroválvulas.",
                    subtitle = "Control Niro 40",
                    ejecutivoId = defaultEjecutivoId,
                    position = 31
                ),
                Question(
                    title = "Compruebe que los acoples y sistemas de los sopladores se encuentran en buen estado para trabajar.",
                    subtitle = "Control Niro 40",
                    ejecutivoId = defaultEjecutivoId,
                    position = 32
                )
            )
            
            // Agregar todas las preguntas
            for (question in predefinedQuestions) {
                questionManager.addQuestion(question)
            }
            
            // Recargar la lista de preguntas
            loadQuestions()
            
            Toast.makeText(this, "31 preguntas del checklist precargadas exitosamente", Toast.LENGTH_LONG).show()
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error al precargar preguntas: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
    
    private fun saveAdminSession(isAdmin: Boolean, isSuperAdmin: Boolean) {
        prefs.edit()
            .putBoolean("admin_session_active", isAdmin)
            .putBoolean("super_admin_session_active", isSuperAdmin)
            .apply()
    }
    
    private fun clearAdminSession() {
        prefs.edit()
            .putBoolean("admin_session_active", false)
            .putBoolean("super_admin_session_active", false)
            .apply()
    }
    
    private fun updateAdminButtonText() {
        // Las opciones de admin ahora están en el menú "Más"
        // Verificar si debe mostrar el tutorial después de cambiar el modo admin
        checkTutorial()
    }
    
    private fun loadChecklistTitle() {
        val customTitle = prefs.getString("checklist_title", "Lista de Clientes")
        binding.titleText.text = customTitle
    }
    
    private fun setupFooter() {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName
            binding.footerText.text = "Desarrollada por Ing.Ronald Rojas C | 8878-1108 | Ver: $versionName"
        } catch (e: Exception) {
            // Si hay error obteniendo la versión, usar versión por defecto
            binding.footerText.text = "Desarrollada por Ing.Ronald Rojas C | 8878-1108 | Ver: 1.0"
        }
    }
    
    private fun setupFloatingProgressIndicator() {
        val generateReportButton = findViewById<android.widget.Button>(R.id.generateReportButton)
        generateReportButton.setOnClickListener {
            // Generar reporte directamente (igual que el botón + de reportes)
            generateReport()
        }
    }
    
    private fun setupFloatingActionButton() {
        val fabMain = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabMain)
        val fabLimpiar = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabLimpiar)
        val fabAgregarCliente = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAgregarCliente)
        
        var isMenuOpen = false
        
        // Botón principal - alternar menú
        fabMain.setOnClickListener {
            isMenuOpen = !isMenuOpen
            if (isMenuOpen) {
                // Mostrar menú
                fabLimpiar.visibility = android.view.View.VISIBLE
                fabAgregarCliente.visibility = android.view.View.VISIBLE
                fabMain.setImageResource(com.checklist.app.R.drawable.ic_back)
            } else {
                // Ocultar menú
                fabLimpiar.visibility = android.view.View.GONE
                fabAgregarCliente.visibility = android.view.View.GONE
                fabMain.setImageResource(com.checklist.app.R.drawable.ic_add)
            }
        }
        
        // Botón Limpiar
        fabLimpiar.setOnClickListener {
            clearAllClientSelections()
            // Cerrar menú después de la acción
            isMenuOpen = false
            fabLimpiar.visibility = android.view.View.GONE
            fabAgregarCliente.visibility = android.view.View.GONE
            fabMain.setImageResource(com.checklist.app.R.drawable.ic_add)
        }
        
        // Botón Agregar Cliente
        fabAgregarCliente.setOnClickListener {
            showClientsActivity()
            // Cerrar menú después de la acción
            isMenuOpen = false
            fabLimpiar.visibility = android.view.View.GONE
            fabAgregarCliente.visibility = android.view.View.GONE
            fabMain.setImageResource(com.checklist.app.R.drawable.ic_add)
        }
    }
    
    private fun clearAllClientSelections() {
        try {
            val allQuestions = questionManager.getAllQuestions()
            var clearedCount = 0
            
            for (question in allQuestions) {
                if (question.isCompleted) {
                    val updatedQuestion = question.copy(isCompleted = false)
                    questionManager.updateQuestion(updatedQuestion)
                    clearedCount++
                }
            }
            
            // Recargar la lista para mostrar los cambios
            loadQuestions()
            
            if (clearedCount > 0) {
                Toast.makeText(this, "Se limpiaron $clearedCount clientes", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No hay clientes marcados para limpiar", Toast.LENGTH_SHORT).show()
            }
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error al limpiar selecciones: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
    
    private fun checkTutorial() {
        val tutorialCompleted = prefs.getBoolean("tutorial_completed", false)
        val tutorialAutoEnabled = prefs.getBoolean("tutorial_auto_enabled", false)
        
        // Mostrar tutorial si:
        // 1. No se ha completado el tutorial Y
        // 2. (No está en modo admin O el tutorial automático está activado)
        val shouldShowTutorial = !tutorialCompleted && (!isAdminMode || tutorialAutoEnabled)
        
        if (shouldShowTutorial) {
            try {
                android.util.Log.d("MainActivity", "checkTutorial: Mostrando tutorial")
                val intent = Intent(this, TutorialActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
                startActivity(intent)
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error al mostrar tutorial", e)
                Toast.makeText(this, "Error al mostrar tutorial: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            android.util.Log.d("MainActivity", "checkTutorial: Tutorial no mostrado - Completado: $tutorialCompleted, Admin: $isAdminMode, Auto: $tutorialAutoEnabled")
        }
    }
    
    private fun loadEssentialData() {
        // Cargar solo datos críticos para mostrar la UI rápidamente
        try {
            loadChecklistTitle()
            checkFirstTimeSetup()
            initializeDefaultEjecutivos()
            
            // Cargar preguntas existentes (sin procesar clientes)
            val questions = questionManager.getQuestionsOrderedByPosition()
            allQuestions = questions
            
            // Actualizar UI inmediatamente
            runOnUiThread {
                updateQuestionsUI(questions)
                setupEjecutivoButtons()
            }
            
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error cargando datos esenciales", e)
        }
    }
    
    private suspend fun loadNonEssentialData() {
        // Cargar datos no críticos en background
        try {
            // Precargar clientes (operación costosa)
            precargarClientes()
            
            // Corregir estados iniciales
            correctInitialClientStates()
            
            // NO verificar tutorial aquí para evitar conflictos
            // El tutorial se verifica solo en onResume()
            
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error cargando datos no esenciales", e)
        }
    }
    
    private fun correctInitialClientStates() {
        try {
            // Solo corregir una vez, no cada vez que se inicia la app
            val alreadyCorrected = prefs.getBoolean("initial_states_corrected", false)
            if (alreadyCorrected) {
                return
            }
            
            val isInitialStatePendiente = prefs.getBoolean("client_initial_state_pendiente", true)
            val expectedCompletedState = !isInitialStatePendiente
            
            // Determinar el estado correcto para la tabla de estados
            val estadoCorrecto = if (isInitialStatePendiente) {
                ClienteEstadoManager.ESTADO_PENDIENTE
            } else {
                ClienteEstadoManager.ESTADO_PAGADO
            }
            
            val allQuestions = questionManager.getAllQuestions()
            var correctedCount = 0
            
            for (question in allQuestions) {
                val clienteId = question.clienteId ?: 0L
                val ejecutivoId = question.ejecutivoId
                
                // Actualizar tabla de estados
                clienteEstadoManager.updateEstadoCliente(clienteId, estadoCorrecto, ejecutivoId)
                
                // Actualizar pregunta para mantener compatibilidad
                if (question.isCompleted != expectedCompletedState) {
                    val correctedQuestion = question.copy(isCompleted = expectedCompletedState)
                    questionManager.updateQuestion(correctedQuestion)
                    correctedCount++
                }
            }
            
            if (correctedCount > 0) {
                android.util.Log.d("MainActivity", "correctInitialClientStates: $correctedCount clientes corregidos al estado inicial correcto: $estadoCorrecto")
                // Marcar como corregido para no volver a hacerlo
                prefs.edit().putBoolean("initial_states_corrected", true).apply()
                // Recargar la UI para mostrar los cambios
                runOnUiThread {
                    loadQuestions()
                }
            } else {
                // Si no hubo correcciones, marcar como corregido de todas formas
                prefs.edit().putBoolean("initial_states_corrected", true).apply()
            }
            
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error corrigiendo estados iniciales de clientes", e)
        }
    }
    
    // Método público para actualizar UI desde ConfigActivity
    fun refreshUIAfterConfigChange() {
        android.util.Log.d("MainActivity", "refreshUIAfterConfigChange: Actualizando UI después de cambio de configuración")
        lifecycleScope.launch {
            try {
                // Recargar preguntas con los nuevos estados
                val questions = questionManager.getQuestionsOrderedByPosition()
                allQuestions = questions
                
                // Log detallado para verificar la actualización
                android.util.Log.d("MainActivity", "refreshUIAfterConfigChange: ${questions.size} preguntas recargadas")
                questions.forEach { question ->
                    val clienteId = question.clienteId ?: 0L
                    val estado = clienteEstadoManager.getEstadoString(clienteId)
                    android.util.Log.d("MainActivity", "refreshUIAfterConfigChange: ${question.title} - isCompleted: ${question.isCompleted}, Estado: $estado")
                }
                
                // Actualizar UI en el hilo principal
                runOnUiThread {
                    updateQuestionsUI(questions)
                    updateProgressIndicator(questions)
                    android.util.Log.d("MainActivity", "refreshUIAfterConfigChange: UI actualizada con ${questions.size} preguntas")
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error actualizando UI después de cambio de configuración", e)
            }
        }
    }
    
    
}


