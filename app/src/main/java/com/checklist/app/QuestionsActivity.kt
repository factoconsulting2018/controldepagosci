package com.checklist.app

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.checklist.app.databinding.ActivityQuestionsBinding
import java.io.File

class QuestionsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityQuestionsBinding
    private lateinit var questionManager: QuestionManager
    private lateinit var ejecutivoManager: EjecutivoManager
    private lateinit var clienteManager: ClienteManager
    private lateinit var questionsAdapter: QuestionsAdapter
    private var isAdminMode = false
    private var selectedEjecutivoId: Long = -1
    private var currentAlphabetFilter: String? = null
    private var allQuestions: List<Question> = emptyList()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        android.util.Log.d("QuestionsActivity", "onCreate iniciado")
        
        // Ocultar la ActionBar
        supportActionBar?.hide()
        
        binding = ActivityQuestionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Obtener el estado de administrador desde el intent
        isAdminMode = intent.getBooleanExtra("isAdminMode", false)
        val openNewClientForm = intent.getBooleanExtra("openNewClientForm", false)
        
        questionManager = QuestionManager(this)
        ejecutivoManager = EjecutivoManager(this)
        clienteManager = ClienteManager(this)
        android.util.Log.d("QuestionsActivity", "onCreate: Managers inicializados")
        
        // Precargar clientes en esta actividad también
        precargarClientes()
        
        setupRecyclerView()
        setupClickListeners()
        setupEjecutivoSpinner()
        loadQuestions()
        updateAlphabetButtonStates() // Inicializar estado de botones del abecedario
        android.util.Log.d("QuestionsActivity", "onCreate: loadQuestions completado")
        
        // Si se debe abrir el formulario de nuevo cliente
        if (openNewClientForm) {
            android.util.Log.d("QuestionsActivity", "onCreate: Abriendo formulario de nuevo cliente")
            showAddQuestionDialog()
        }
        
        android.util.Log.d("QuestionsActivity", "onCreate completado")
    }
    
    private fun setupRecyclerView() {
        questionsAdapter = QuestionsAdapter(
            onEditClick = { question -> editQuestion(question) },
            onDeleteClick = { question -> deleteQuestion(question) },
            onQuestionClick = { question -> 
                // No hacer nada en la sección de preguntas, solo en la pantalla principal
            },
            onStatusToggle = { question -> 
                // Alternar estado de completado
                toggleQuestionCompletion(question)
            },
            isAdminMode = { isAdminMode },
            getEjecutivoName = { ejecutivoId -> getEjecutivoNameById(ejecutivoId) },
            getEjecutivoColor = { ejecutivoId -> getEjecutivoColorById(ejecutivoId) },
            getClienteInfo = { clienteId -> getClienteInfoById(clienteId) },
            getClientePhone = { clienteId -> getClientePhoneById(clienteId) }
        )
        
        binding.questionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@QuestionsActivity)
            adapter = questionsAdapter
        }
    }
    
    private fun setupClickListeners() {
        binding.addQuestionButton.setOnClickListener {
            if (isAdminMode) {
                showAddQuestionDialog()
            } else {
                Toast.makeText(this, "Modo administrador requerido", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.backFab.setOnClickListener {
            finish()
        }
        
        // Configurar botones del abecedario
        setupAlphabetButtons()
    }
    
    private fun setupAlphabetButtons() {
        val alphabetButtons = listOf(
            binding.btnAll to null,
            binding.btnA to "A",
            binding.btnB to "B",
            binding.btnC to "C",
            binding.btnD to "D",
            binding.btnE to "E",
            binding.btnF to "F",
            binding.btnG to "G",
            binding.btnH to "H",
            binding.btnI to "I",
            binding.btnJ to "J",
            binding.btnK to "K",
            binding.btnL to "L",
            binding.btnM to "M",
            binding.btnN to "N",
            binding.btnO to "O",
            binding.btnP to "P",
            binding.btnQ to "Q",
            binding.btnR to "R",
            binding.btnS to "S",
            binding.btnT to "T",
            binding.btnU to "U",
            binding.btnV to "V",
            binding.btnW to "W",
            binding.btnX to "X",
            binding.btnY to "Y",
            binding.btnZ to "Z",
            binding.btn3 to "3"
        )
        
        alphabetButtons.forEach { (button, letter) ->
            button.setOnClickListener {
                filterByAlphabet(letter)
            }
        }
    }
    
    private fun filterByAlphabet(letter: String?) {
        android.util.Log.d("QuestionsActivity", "filterByAlphabet: Filtro aplicado: $letter")
        currentAlphabetFilter = letter
        updateAlphabetButtonStates()
        loadQuestions()
    }
    
    private fun updateAlphabetButtonStates() {
        val alphabetButtons = listOf(
            binding.btnAll to null,
            binding.btnA to "A",
            binding.btnB to "B",
            binding.btnC to "C",
            binding.btnD to "D",
            binding.btnE to "E",
            binding.btnF to "F",
            binding.btnG to "G",
            binding.btnH to "H",
            binding.btnI to "I",
            binding.btnJ to "J",
            binding.btnK to "K",
            binding.btnL to "L",
            binding.btnM to "M",
            binding.btnN to "N",
            binding.btnO to "O",
            binding.btnP to "P",
            binding.btnQ to "Q",
            binding.btnR to "R",
            binding.btnS to "S",
            binding.btnT to "T",
            binding.btnU to "U",
            binding.btnV to "V",
            binding.btnW to "W",
            binding.btnX to "X",
            binding.btnY to "Y",
            binding.btnZ to "Z",
            binding.btn3 to "3"
        )
        
        alphabetButtons.forEach { (button, letter) ->
            val isSelected = currentAlphabetFilter == letter
            if (isSelected) {
                button.setBackgroundColor(resources.getColor(com.checklist.app.R.color.green_500, null))
                button.setTextColor(resources.getColor(com.checklist.app.R.color.white, null))
            } else {
                button.setBackgroundColor(resources.getColor(com.checklist.app.R.color.gray_300, null))
                button.setTextColor(resources.getColor(com.checklist.app.R.color.black, null))
            }
        }
    }
    
    private fun setupEjecutivoSpinner() {
        val ejecutivos = ejecutivoManager.getAllEjecutivos()
        val ejecutivoNames = ejecutivos.map { it.name }
        val ejecutivoIds = ejecutivos.map { it.id }
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ejecutivoNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.categorySpinner.adapter = adapter
        
        binding.categorySpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedEjecutivoId = ejecutivoIds[position]
                loadQuestions()
            }
            
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                selectedEjecutivoId = -1
            }
        }
    }
    
    private fun loadQuestions() {
        // Primero, asegurar que todos los clientes precargados tengan una entrada en la lista
        ensureAllClientsHaveQuestions()
        
        val questions = if (selectedEjecutivoId == -1L) {
            questionManager.getQuestionsOrderedByPosition()
        } else {
            questionManager.getQuestionsByEjecutivo(selectedEjecutivoId).sortedBy { it.position }
        }
        
        // Aplicar filtro alfabético si está activo
        val filteredQuestions = if (currentAlphabetFilter != null) {
            android.util.Log.d("QuestionsActivity", "loadQuestions: Aplicando filtro alfabético: $currentAlphabetFilter")
            val filtered = questions.filter { question ->
                val cliente = clienteManager.getClienteById(question.clienteId ?: -1)
                if (cliente != null) {
                    val nombre = cliente.nombre.uppercase()
                    val startsWith = when (currentAlphabetFilter) {
                        "3" -> {
                            // Filtrar por sociedades jurídicas (nombres que empiecen con números)
                            nombre.isNotEmpty() && nombre.first().isDigit()
                        }
                        else -> {
                            // Filtrar por letra específica
                            nombre.startsWith(currentAlphabetFilter!!)
                        }
                    }
                    android.util.Log.d("QuestionsActivity", "loadQuestions: Cliente ${cliente.nombre} - Filtro: $currentAlphabetFilter - Coincide: $startsWith")
                    startsWith
                } else {
                    // Si no hay cliente asociado, usar el título de la pregunta
                    val nombre = question.title.uppercase()
                    val startsWith = when (currentAlphabetFilter) {
                        "3" -> {
                            // Filtrar por sociedades jurídicas (nombres que empiecen con números)
                            nombre.isNotEmpty() && nombre.first().isDigit()
                        }
                        else -> {
                            // Filtrar por letra específica
                            nombre.startsWith(currentAlphabetFilter!!)
                        }
                    }
                    android.util.Log.d("QuestionsActivity", "loadQuestions: Pregunta ${question.title} - Filtro: $currentAlphabetFilter - Coincide: $startsWith")
                    startsWith
                }
            }
            android.util.Log.d("QuestionsActivity", "loadQuestions: Filtro aplicado: ${filtered.size} de ${questions.size} preguntas")
            filtered
        } else {
            android.util.Log.d("QuestionsActivity", "loadQuestions: Sin filtro alfabético")
            questions
        }
        
        // Ordenar alfabéticamente por ejecutivo y luego por nombre del cliente (A-Z)
        val orderedQuestions = filteredQuestions.sortedWith(compareBy<Question> { question ->
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
        
        android.util.Log.d("QuestionsActivity", "Cargando ${orderedQuestions.size} preguntas ordenadas por ejecutivo y alfabéticamente")
        
        // Verificar si hay preguntas
        if (orderedQuestions.isEmpty()) {
            android.util.Log.d("QuestionsActivity", "No hay preguntas en la base de datos")
        }
        
        questionsAdapter.submitList(orderedQuestions)
        questionsAdapter.notifyDataSetChanged()
        android.util.Log.d("QuestionsActivity", "Adapter actualizado con ${filteredQuestions.size} preguntas")
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
        if (clienteId == null) {
            android.util.Log.d("QuestionsActivity", "getClientePhoneById: clienteId es null")
            return ""
        }
        if (!::clienteManager.isInitialized) {
            android.util.Log.d("QuestionsActivity", "getClientePhoneById: clienteManager no inicializado")
            return ""
        }
        val cliente = clienteManager.getClienteById(clienteId)
        val telefono = if (cliente != null) {
            cliente.telefono
        } else {
            ""
        }
        android.util.Log.d("QuestionsActivity", "getClientePhoneById: ID=$clienteId, Cliente=${cliente?.nombre}, Teléfono='$telefono'")
        return telefono
    }
    
    private fun precargarClientes() {
        android.util.Log.d("QuestionsActivity", "precargarClientes: Iniciando carga automática")
        try {
            val success = clienteManager.precargarClientes()
            android.util.Log.d("QuestionsActivity", "precargarClientes: Resultado: $success")
            if (success) {
                val clientesCount = clienteManager.getAllClientes().size
                android.util.Log.d("QuestionsActivity", "precargarClientes: $clientesCount clientes cargados automáticamente")
            }
        } catch (e: Exception) {
            android.util.Log.e("QuestionsActivity", "precargarClientes: Error al cargar clientes", e)
        }
    }
    
    private fun ensureAllClientsHaveQuestions() {
        android.util.Log.d("QuestionsActivity", "ensureAllClientsHaveQuestions: Iniciando")
        
        if (!::clienteManager.isInitialized) {
            android.util.Log.d("QuestionsActivity", "ensureAllClientsHaveQuestions: clienteManager no inicializado")
            return
        }
        
        val allClientes = clienteManager.getAllClientes()
        android.util.Log.d("QuestionsActivity", "ensureAllClientsHaveQuestions: ${allClientes.size} clientes encontrados")
        
        val existingQuestions = questionManager.getQuestionsOrderedByPosition()
        val existingClienteIds = existingQuestions.mapNotNull { it.clienteId }.toSet()
        android.util.Log.d("QuestionsActivity", "ensureAllClientsHaveQuestions: ${existingQuestions.size} preguntas existentes")
        
        // Obtener ejecutivos disponibles
        val ejecutivos = ejecutivoManager.getAllEjecutivos()
        val defaultEjecutivoId = 0L // Usar el ejecutivo "Todos" como por defecto
        
        // Crear ejecutivos únicos basados en los clientes (solo los necesarios)
        val ejecutivosUnicos = allClientes
            .map { it.ejecutivo }
            .filter { it.isNotBlank() }
            .distinct()
        
        android.util.Log.d("QuestionsActivity", "ensureAllClientsHaveQuestions: Ejecutivos únicos encontrados: $ejecutivosUnicos")
        
        // Crear ejecutivos automáticamente para cada nombre único
        ejecutivosUnicos.forEach { nombreEjecutivo ->
            val ejecutivoExistente = ejecutivos.find { it.name.equals(nombreEjecutivo, ignoreCase = true) }
            if (ejecutivoExistente == null) {
                android.util.Log.d("QuestionsActivity", "ensureAllClientsHaveQuestions: Creando ejecutivo '$nombreEjecutivo'...")
                val nuevoEjecutivo = Ejecutivo(
                    name = nombreEjecutivo,
                    color = getRandomColorForEjecutivo()
                )
                ejecutivoManager.addEjecutivo(nuevoEjecutivo)
            }
        }
        
        // Actualizar la lista de ejecutivos después de crear los nuevos
        val ejecutivosActualizados = ejecutivoManager.getAllEjecutivos()
        var nextPosition = questionManager.getNextPosition()
        android.util.Log.d("QuestionsActivity", "ensureAllClientsHaveQuestions: Ejecutivo por defecto: $defaultEjecutivoId, siguiente posición: $nextPosition")
        
        // Crear preguntas para clientes que no tienen una entrada
        var clientesAgregados = 0
        for (cliente in allClientes) {
            if (cliente.id !in existingClienteIds) {
                // Buscar el ejecutivo correcto basado en el nombre del ejecutivo del cliente
                val ejecutivoCorrecto = ejecutivosActualizados.find { it.name.equals(cliente.ejecutivo, ignoreCase = true) }
                val ejecutivoId = ejecutivoCorrecto?.id ?: defaultEjecutivoId
                
                android.util.Log.d("QuestionsActivity", "ensureAllClientsHaveQuestions: Cliente ${cliente.nombre} - Ejecutivo: ${cliente.ejecutivo} - ID asignado: $ejecutivoId")
                
                // Determinar el estado inicial basado en la preferencia
                val prefs = getSharedPreferences("checklist_prefs", MODE_PRIVATE)
                val isInitialStatePendiente = prefs.getBoolean("client_initial_state_pendiente", true)
                val initialCompleted = !isInitialStatePendiente // Si es PAGADO/PENDIENTE, inicia como completado
                
                val question = Question(
                    title = cliente.nombre,
                    subtitle = cliente.cedula,
                    ejecutivoId = ejecutivoId,
                    position = nextPosition++,
                    clienteId = cliente.id,
                    isCompleted = initialCompleted
                )
                questionManager.addQuestion(question)
                clientesAgregados++
                android.util.Log.d("QuestionsActivity", "ensureAllClientsHaveQuestions: Cliente agregado: ${cliente.nombre}")
            }
        }
        android.util.Log.d("QuestionsActivity", "ensureAllClientsHaveQuestions: $clientesAgregados clientes agregados")
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
    
    private fun showAddQuestionDialog() {
        // Verificar que existan categorías
        val ejecutivos = ejecutivoManager.getAllEjecutivos()
        if (ejecutivos.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Ejecutivos Requeridos")
                .setMessage("Debes crear al menos un ejecutivo antes de agregar preguntas. ¿Quieres ir a la sección de ejecutivos?")
                .setPositiveButton("Ir a Ejecutivos") { _, _ ->
                    val intent = Intent(this, EjecutivosActivity::class.java)
                    intent.putExtra("isAdminMode", isAdminMode)
                    startActivity(intent)
                }
                .setNegativeButton("Cancelar", null)
                .show()
            return
        }
        
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_question, null)
        
        // Campos de cliente
        val nombreClienteEditText = dialogView.findViewById<EditText>(R.id.nombreClienteEditText)
        val cedulaEditText = dialogView.findViewById<EditText>(R.id.cedulaEditText)
        val tipoPersonaSpinner = dialogView.findViewById<Spinner>(R.id.tipoPersonaSpinner)
        val representanteEditText = dialogView.findViewById<EditText>(R.id.representanteEditText)
        val telefonoEditText = dialogView.findViewById<EditText>(R.id.telefonoEditText)
        val ciFcSpinner = dialogView.findViewById<Spinner>(R.id.ciFcSpinner)
        val ejecutivoSpinner = dialogView.findViewById<Spinner>(R.id.ejecutivoSpinner)
        val tipoRegimenSpinner = dialogView.findViewById<Spinner>(R.id.tipoRegimenSpinner)
        val patentadoCheckBox = dialogView.findViewById<CheckBox>(R.id.patentadoCheckBox)
        val pendientePagoCheckBox = dialogView.findViewById<CheckBox>(R.id.pendientePagoCheckBox)
        val selectClienteButton = dialogView.findViewById<android.widget.Button>(R.id.selectClienteButton)
        
        // Configurar spinner de tipo de persona
        val tipoPersonaOptions = arrayOf("Físico", "Jurídico")
        val tipoPersonaAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tipoPersonaOptions)
        tipoPersonaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        tipoPersonaSpinner.adapter = tipoPersonaAdapter
        
        // Configurar spinner de CI-FC
        val tiposCiFc = arrayOf("Sin especificar", "CI", "FC")
        val ciFcAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tiposCiFc)
        ciFcAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        ciFcSpinner.adapter = ciFcAdapter
        
        // Configurar spinner de ejecutivos (usando la variable ejecutivos ya declarada al inicio de la función)
        val ejecutivosNombres = ejecutivos.map { it.name }.toMutableList()
        ejecutivosNombres.add(0, "Sin ejecutivo")
        val ejecutivoAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ejecutivosNombres)
        ejecutivoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        ejecutivoSpinner.adapter = ejecutivoAdapter
        
        // Configurar spinner de tipo de régimen
        val tiposRegimen = arrayOf("Sin régimen", "Simplificado", "Tradicional")
        val tipoRegimenAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tiposRegimen)
        tipoRegimenAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        tipoRegimenSpinner.adapter = tipoRegimenAdapter
        
        // Configurar botón de selección de cliente
        selectClienteButton.setOnClickListener {
            showClienteSelectionDialog(
                nombreClienteEditText, cedulaEditText, tipoPersonaSpinner,
                representanteEditText, telefonoEditText, ciFcSpinner,
                ejecutivoSpinner, tipoRegimenSpinner, patentadoCheckBox, pendientePagoCheckBox
            )
        }
        
            AlertDialog.Builder(this)
                .setTitle("Agregar Nuevo Cliente")
                .setView(dialogView)
            .setPositiveButton("Agregar") { _, _ ->
                // Datos del cliente
                val nombreCliente = nombreClienteEditText.text.toString().trim()
                val cedula = cedulaEditText.text.toString().trim()
                val tipoPersona = tipoPersonaOptions[tipoPersonaSpinner.selectedItemPosition]
                val representante = representanteEditText.text.toString().trim()
                val telefono = telefonoEditText.text.toString().trim()
                val ciFcSeleccionado = ciFcSpinner.selectedItem.toString()
                val ciFc = if (ciFcSeleccionado == "Sin especificar") "" else ciFcSeleccionado
                val ejecutivoSeleccionado = ejecutivoSpinner.selectedItem.toString()
                val ejecutivo = if (ejecutivoSeleccionado == "Sin ejecutivo") "" else ejecutivoSeleccionado
                val tipoRegimenSeleccionado = tipoRegimenSpinner.selectedItem.toString()
                val tipoRegimen = if (tipoRegimenSeleccionado == "Sin régimen") "" else tipoRegimenSeleccionado
                val patentado = patentadoCheckBox.isChecked
                val pendientePago = pendientePagoCheckBox.isChecked
                
                if (nombreCliente.isNotEmpty() && cedula.isNotEmpty()) {
                    val ejecutivosList = ejecutivoManager.getAllEjecutivos()
                    addQuestionWithCliente(
                        nombreCliente, cedula, ejecutivosList.first().id, 0, // Usar primer ejecutivo y posición 0
                        nombreCliente, cedula, tipoPersona, representante, telefono,
                        ciFc, ejecutivo, tipoRegimen, patentado, pendientePago
                    )
                } else {
                    Toast.makeText(this, "Nombre del cliente y cédula son obligatorios", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun addQuestion(title: String, subtitle: String, ejecutivoId: Long, position: Int) {
        val question = Question(
            title = title,
            subtitle = subtitle,
            ejecutivoId = ejecutivoId,
            position = position
        )
        questionManager.addQuestion(question)
        loadQuestions()
        Toast.makeText(this@QuestionsActivity, "Pregunta agregada", Toast.LENGTH_SHORT).show()
    }
    
    private fun addQuestionWithCliente(
        title: String, subtitle: String, ejecutivoId: Long, position: Int,
        nombreCliente: String, cedula: String, tipoPersona: String, representante: String,
        telefono: String, ciFc: String, ejecutivo: String, tipoRegimen: String,
        patentado: Boolean, pendientePago: Boolean
    ) {
        // Crear o buscar cliente existente
        val cliente = Cliente(
            nombre = nombreCliente,
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
        
        val clienteId = clienteManager.addCliente(cliente)
        
        // Crear pregunta con título y subtítulo basados en datos del cliente
        val question = Question(
            title = nombreCliente, // Título será el nombre del cliente
            subtitle = cedula, // Subtítulo será la cédula
            ejecutivoId = ejecutivoId,
            position = position,
            clienteId = clienteId
        )
        
            questionManager.addQuestion(question)
            loadQuestions()
            Toast.makeText(this@QuestionsActivity, "Cliente agregado exitosamente", Toast.LENGTH_SHORT).show()
    }
    
    private fun showClienteSelectionDialog(
        nombreClienteEditText: EditText, cedulaEditText: EditText, tipoPersonaSpinner: Spinner,
        representanteEditText: EditText, telefonoEditText: EditText, ciFcSpinner: Spinner,
        ejecutivoSpinner: Spinner, tipoRegimenSpinner: Spinner, patentadoCheckBox: CheckBox, 
        pendientePagoCheckBox: CheckBox
    ) {
        val clientes = clienteManager.getAllClientes()
        if (clientes.isEmpty()) {
            Toast.makeText(this, "No hay clientes precargados. Los clientes se cargan automáticamente al iniciar la aplicación.", Toast.LENGTH_LONG).show()
            return
        }
        
        val clienteNames = clientes.map { "${it.nombre} - ${it.cedula}" }
        
        AlertDialog.Builder(this)
            .setTitle("Seleccionar Cliente")
            .setItems(clienteNames.toTypedArray()) { _, which ->
                val selectedCliente = clientes[which]
                fillClienteFields(selectedCliente, nombreClienteEditText, cedulaEditText, tipoPersonaSpinner,
                    representanteEditText, telefonoEditText, ciFcSpinner,
                    ejecutivoSpinner, tipoRegimenSpinner, patentadoCheckBox, pendientePagoCheckBox)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun fillClienteFields(
        cliente: Cliente,
        nombreClienteEditText: EditText, cedulaEditText: EditText, tipoPersonaSpinner: Spinner,
        representanteEditText: EditText, telefonoEditText: EditText, ciFcSpinner: Spinner,
        ejecutivoSpinner: Spinner, tipoRegimenSpinner: Spinner, patentadoCheckBox: CheckBox, 
        pendientePagoCheckBox: CheckBox
    ) {
        nombreClienteEditText.setText(cliente.nombre)
        cedulaEditText.setText(cliente.cedula)
        
        // Configurar tipo de persona
        val tipoPersonaOptions = arrayOf("Físico", "Jurídico")
        val tipoPersonaIndex = tipoPersonaOptions.indexOf(cliente.tipoPersona)
        if (tipoPersonaIndex >= 0) {
            tipoPersonaSpinner.setSelection(tipoPersonaIndex)
        }
        
        representanteEditText.setText(cliente.representante)
        telefonoEditText.setText(cliente.telefono)
        
        // Seleccionar CI-FC en el spinner
        val tiposCiFc = arrayOf("Sin especificar", "CI", "FC")
        val ciFcIndex = tiposCiFc.indexOf(cliente.ciFc)
        if (ciFcIndex >= 0) {
            ciFcSpinner.setSelection(ciFcIndex)
        } else {
            ciFcSpinner.setSelection(0)
        }
        
        // Seleccionar ejecutivo en el spinner
        val ejecutivosNombres = ejecutivoManager.getAllEjecutivos().map { it.name }.toMutableList()
        ejecutivosNombres.add(0, "Sin ejecutivo")
        val ejecutivoIndex = ejecutivosNombres.indexOf(cliente.ejecutivo)
        if (ejecutivoIndex >= 0) {
            ejecutivoSpinner.setSelection(ejecutivoIndex)
        } else {
            ejecutivoSpinner.setSelection(0)
        }
        
        // Seleccionar tipo de régimen en el spinner
        val tiposRegimen = arrayOf("Sin régimen", "Simplificado", "Tradicional")
        val tipoRegimenIndex = tiposRegimen.indexOf(cliente.tipoRegimen)
        if (tipoRegimenIndex >= 0) {
            tipoRegimenSpinner.setSelection(tipoRegimenIndex)
        } else {
            tipoRegimenSpinner.setSelection(0)
        }
        
        patentadoCheckBox.isChecked = cliente.patentado
        pendientePagoCheckBox.isChecked = cliente.pendientePago
        
        Toast.makeText(this, "Datos del cliente cargados", Toast.LENGTH_SHORT).show()
    }
    
    
    private fun editQuestion(question: Question) {
        if (!isAdminMode) {
            Toast.makeText(this, "Modo administrador requerido", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Obtener datos del cliente asociado
        val cliente = if (question.clienteId != null) {
            clienteManager.getClienteById(question.clienteId)
        } else {
            null
        }
        
        if (cliente == null) {
            Toast.makeText(this, "No se encontró información del cliente", Toast.LENGTH_SHORT).show()
            return
        }
        
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_question, null)
        
        // Campos de cliente
        val nombreClienteEditText = dialogView.findViewById<EditText>(R.id.nombreClienteEditText)
        val cedulaEditText = dialogView.findViewById<EditText>(R.id.cedulaEditText)
        val tipoPersonaSpinner = dialogView.findViewById<Spinner>(R.id.tipoPersonaSpinner)
        val representanteEditText = dialogView.findViewById<EditText>(R.id.representanteEditText)
        val telefonoEditText = dialogView.findViewById<EditText>(R.id.telefonoEditText)
        val ciFcSpinner = dialogView.findViewById<Spinner>(R.id.ciFcSpinner)
        val ejecutivoSpinner = dialogView.findViewById<Spinner>(R.id.ejecutivoSpinner)
        val tipoRegimenSpinner = dialogView.findViewById<Spinner>(R.id.tipoRegimenSpinner)
        val patentadoCheckBox = dialogView.findViewById<CheckBox>(R.id.patentadoCheckBox)
        val pendientePagoCheckBox = dialogView.findViewById<CheckBox>(R.id.pendientePagoCheckBox)
        
        // Configurar spinner de tipo de persona
        val tipoPersonaOptions = arrayOf("Físico", "Jurídico")
        val tipoPersonaAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tipoPersonaOptions)
        tipoPersonaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        tipoPersonaSpinner.adapter = tipoPersonaAdapter
        
        // Configurar spinner de CI-FC
        val tiposCiFc = arrayOf("Sin especificar", "CI", "FC")
        val ciFcAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tiposCiFc)
        ciFcAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        ciFcSpinner.adapter = ciFcAdapter
        
        // Configurar spinner de ejecutivos
        val ejecutivosLista = ejecutivoManager.getAllEjecutivos()
        val ejecutivosNombres = ejecutivosLista.map { it.name }.toMutableList()
        ejecutivosNombres.add(0, "Sin ejecutivo")
        val ejecutivoAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ejecutivosNombres)
        ejecutivoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        ejecutivoSpinner.adapter = ejecutivoAdapter
        
        // Configurar spinner de tipo de régimen
        val tiposRegimen = arrayOf("Sin régimen", "Simplificado", "Tradicional")
        val tipoRegimenAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tiposRegimen)
        tipoRegimenAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        tipoRegimenSpinner.adapter = tipoRegimenAdapter
        
        // Llenar campos con datos existentes
        fillClienteFields(cliente, nombreClienteEditText, cedulaEditText, tipoPersonaSpinner,
            representanteEditText, telefonoEditText, ciFcSpinner,
            ejecutivoSpinner, tipoRegimenSpinner, patentadoCheckBox, pendientePagoCheckBox)
        
            AlertDialog.Builder(this)
                .setTitle("Editar Información del Cliente")
                .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                // Datos del cliente
                val nombreCliente = nombreClienteEditText.text.toString().trim()
                val cedula = cedulaEditText.text.toString().trim()
                val tipoPersona = tipoPersonaOptions[tipoPersonaSpinner.selectedItemPosition]
                val representante = representanteEditText.text.toString().trim()
                val telefono = telefonoEditText.text.toString().trim()
                val ciFcSeleccionado = ciFcSpinner.selectedItem.toString()
                val ciFc = if (ciFcSeleccionado == "Sin especificar") "" else ciFcSeleccionado
                val ejecutivoSeleccionado = ejecutivoSpinner.selectedItem.toString()
                val ejecutivo = if (ejecutivoSeleccionado == "Sin ejecutivo") "" else ejecutivoSeleccionado
                val tipoRegimenSeleccionado = tipoRegimenSpinner.selectedItem.toString()
                val tipoRegimen = if (tipoRegimenSeleccionado == "Sin régimen") "" else tipoRegimenSeleccionado
                val patentado = patentadoCheckBox.isChecked
                val pendientePago = pendientePagoCheckBox.isChecked
                
                if (nombreCliente.isNotEmpty() && cedula.isNotEmpty()) {
                    // Actualizar cliente
                    val updatedCliente = cliente.copy(
                        nombre = nombreCliente,
                        cedula = cedula,
                        tipoPersona = tipoPersona,
                        representante = representante,
                        telefono = telefono,
                        ciFc = ciFc,
                        ejecutivo = ejecutivo,
                        tipoRegimen = tipoRegimen,
                        patentado = patentado,
                        pendientePago = pendientePago
                    )
                    clienteManager.updateCliente(updatedCliente)
                    
                    // Actualizar pregunta con nuevos datos
                    val updatedQuestion = question.copy(
                        title = nombreCliente,
                        subtitle = cedula
                    )
                        questionManager.updateQuestion(updatedQuestion)
                        loadQuestions()
                        Toast.makeText(this, "Cliente actualizado exitosamente", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Nombre del cliente y cédula son obligatorios", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun updateQuestion(question: Question) {
        questionManager.updateQuestion(question)
        loadQuestions()
        Toast.makeText(this@QuestionsActivity, "Pregunta actualizada", Toast.LENGTH_SHORT).show()
    }
    
    private fun deleteQuestion(question: Question) {
        if (!isAdminMode) {
            Toast.makeText(this, "Modo administrador requerido", Toast.LENGTH_SHORT).show()
            return
        }
        
        AlertDialog.Builder(this)
            .setTitle("Eliminar Cliente")
            .setMessage("¿Estás seguro de que quieres eliminar el cliente '${question.title}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                questionManager.deleteQuestion(question)
                loadQuestions()
                Toast.makeText(this@QuestionsActivity, "Cliente eliminado exitosamente", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    fun setAdminMode(adminMode: Boolean) {
        isAdminMode = adminMode
        questionsAdapter.notifyDataSetChanged()
    }
    
    private fun toggleQuestionCompletion(question: Question) {
        val updatedQuestion = question.copy(isCompleted = !question.isCompleted)
        questionManager.updateQuestion(updatedQuestion)
        loadQuestions()
        
        val status = if (updatedQuestion.isCompleted) "completada" else "pendiente"
        Toast.makeText(this, "Pregunta marcada como $status", Toast.LENGTH_SHORT).show()
    }
}
