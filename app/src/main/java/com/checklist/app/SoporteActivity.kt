package com.checklist.app

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class SoporteActivity : AppCompatActivity() {
    
    private lateinit var issueManager: IssueManager
    private lateinit var clienteManager: ClienteManager
    private lateinit var issuesRecyclerView: RecyclerView
    private lateinit var issuesAdapter: IssuesAdapter
    private lateinit var fabNuevoIssue: ExtendedFloatingActionButton
    private lateinit var fabGenerarReporte: ExtendedFloatingActionButton
    private var isAdminMode = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_soporte)
        
        isAdminMode = intent.getBooleanExtra("isAdminMode", false)
        
        issueManager = IssueManager(this)
        clienteManager = ClienteManager(this)
        
        setupViews()
        setupRecyclerView()
        loadIssues()
    }
    
    private fun setupViews() {
        issuesRecyclerView = findViewById(R.id.issuesRecyclerView)
        fabNuevoIssue = findViewById(R.id.fabNuevoIssue)
        fabGenerarReporte = findViewById(R.id.fabGenerarReporte)
        
        fabNuevoIssue.setOnClickListener {
            showCrearIssueDialog()
        }
        
        fabGenerarReporte.setOnClickListener {
            generarReporteIssues()
        }
        
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName
            findViewById<TextView>(R.id.footerText).text = "Desarrollada por Ing.Ronald Rojas C | 8878-1108 | Ver: $versionName"
        } catch (e: Exception) {
            findViewById<TextView>(R.id.footerText).text = "Desarrollada por Ing.Ronald Rojas C | 8878-1108 | Ver: 1.0"
        }
    }
    
    private fun setupRecyclerView() {
        issuesAdapter = IssuesAdapter(
            onVerDetalleClick = { issue -> mostrarDetalleIssue(issue) },
            onEditarClick = { issue -> editarIssue(issue) },
            onEliminarClick = { issue -> eliminarIssue(issue) },
            onEstadoChanged = { issue, nuevoEstado -> cambiarEstadoIssue(issue, nuevoEstado) },
            onCompartirClick = { issue -> compartirIssue(issue) },
            isAdminMode = { isAdminMode }
        )
        issuesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@SoporteActivity)
            adapter = issuesAdapter
        }
    }
    
    private fun loadIssues() {
        // Ordenar por estado: Pendiente -> En Proceso -> Finalizado, y luego por fecha
        val issues = issueManager.getAllIssues().sortedWith(
            compareBy<Issue> { 
                when (it.estado) {
                    EstadoIssue.PENDIENTE -> 0
                    EstadoIssue.EN_PROCESO -> 1
                    EstadoIssue.FINALIZADO -> 2
                }
            }.thenByDescending { it.fechaCreacion }
        )
        issuesAdapter.submitList(issues)
    }
    
    private fun showCrearIssueDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_crear_issue, null)
        
        val clienteSearchEditText = dialogView.findViewById<TextInputEditText>(R.id.clienteSearchEditText)
        val filterNombreChip = dialogView.findViewById<Chip>(R.id.filterNombreChip)
        val filterCedulaChip = dialogView.findViewById<Chip>(R.id.filterCedulaChip)
        val filterTelefonoChip = dialogView.findViewById<Chip>(R.id.filterTelefonoChip)
        val clientesRecyclerView = dialogView.findViewById<RecyclerView>(R.id.clientesRecyclerView)
        val clienteInfoCard = dialogView.findViewById<MaterialCardView>(R.id.clienteInfoCard)
        val clienteInfoNombre = dialogView.findViewById<TextView>(R.id.clienteInfoNombre)
        val clienteInfoCedula = dialogView.findViewById<TextView>(R.id.clienteInfoCedula)
        val issuesContainer = dialogView.findViewById<LinearLayout>(R.id.issuesContainer)
        val btnAgregarIssue = dialogView.findViewById<Button>(R.id.btnAgregarIssue)
        val totalText = dialogView.findViewById<TextView>(R.id.totalText)
        
        var clienteSeleccionado: Cliente? = null
        val issuesList = mutableListOf<IssueItemView>()
        
        // Configurar RecyclerView de clientes
        val clienteAdapter = ClienteBusquedaAdapter { cliente ->
            clienteSeleccionado = cliente
            clienteInfoCard.visibility = View.VISIBLE
            clienteInfoNombre.text = "Nombre: ${cliente.nombre}"
            clienteInfoCedula.text = "Cédula: ${cliente.cedula}"
            clientesRecyclerView.visibility = View.GONE
            clienteSearchEditText.setText("${cliente.nombre} - ${cliente.cedula}")
        }
        
        clientesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@SoporteActivity)
            adapter = clienteAdapter
        }
        
        // Función para filtrar clientes
        fun filtrarClientes(query: String) {
            if (query.isEmpty()) {
                clientesRecyclerView.visibility = View.GONE
                return
            }
            
            val clientes = clienteManager.getAllClientes()
            val clientesFiltrados = clientes.filter { cliente ->
                var matches = false
                
                if (filterNombreChip.isChecked && cliente.nombre.contains(query, ignoreCase = true)) {
                    matches = true
                }
                if (filterCedulaChip.isChecked && cliente.cedula.contains(query, ignoreCase = true)) {
                    matches = true
                }
                if (filterTelefonoChip.isChecked && cliente.telefono.contains(query, ignoreCase = true)) {
                    matches = true
                }
                
                matches
            }
            
            clienteAdapter.updateClientes(clientesFiltrados)
            clientesRecyclerView.visibility = if (clientesFiltrados.isNotEmpty()) View.VISIBLE else View.GONE
        }
        
        // Configurar búsqueda en tiempo real
        clienteSearchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtrarClientes(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        
        // Configurar filtros
        val filterChips = listOf(filterNombreChip, filterCedulaChip, filterTelefonoChip)
        filterChips.forEach { chip ->
            chip.setOnCheckedChangeListener { _, _ ->
                filtrarClientes(clienteSearchEditText.text.toString())
            }
        }
        
        // Limpiar selección cuando se edita el texto
        clienteSearchEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && clienteSeleccionado != null) {
                clienteSeleccionado = null
                clienteInfoCard.visibility = View.GONE
            }
        }
        
        btnAgregarIssue.setOnClickListener {
            val issueView = addIssueItemView(issuesContainer, issuesList.size + 1, issuesList, totalText)
            issuesList.add(issueView)
        }
        
        // Agregar al menos un issue por defecto
        val issueView = addIssueItemView(issuesContainer, 1, issuesList, totalText)
        issuesList.add(issueView)
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Crear Issue") { _, _ ->
                crearIssue(clienteSeleccionado, issuesList)
            }
            .setNegativeButton("Cancelar", null)
            .create()
        
        dialog.show()
    }
    
    data class IssueItemView(
        val view: View,
        val tituloEditText: TextInputEditText,
        val mensajeEditText: TextInputEditText,
        val fechaEditText: TextInputEditText,
        var fechaMillis: Long = 0
    )
    
    private fun addIssueItemView(
        container: LinearLayout, 
        numero: Int, 
        issuesList: MutableList<IssueItemView>,
        totalText: TextView
    ): IssueItemView {
        val itemView = LayoutInflater.from(this).inflate(R.layout.item_issue, container, false)
        
        val tituloEditText = itemView.findViewById<TextInputEditText>(R.id.tituloEditText)
        val mensajeEditText = itemView.findViewById<TextInputEditText>(R.id.mensajeEditText)
        val fechaEditText = itemView.findViewById<TextInputEditText>(R.id.fechaEditText)
        val btnEliminar = itemView.findViewById<ImageButton>(R.id.btnEliminarIssueItem)
        
        val issueItemView = IssueItemView(itemView, tituloEditText, mensajeEditText, fechaEditText)
        
        fechaEditText.setOnClickListener {
            showDatePicker(fechaEditText, issueItemView)
        }
        
        btnEliminar.setOnClickListener {
            container.removeView(itemView)
            issuesList.remove(issueItemView)
            actualizarTotal(issuesList, totalText)
        }
        
        mensajeEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                actualizarTotal(issuesList, totalText)
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        
        container.addView(itemView)
        return issueItemView
    }
    
    private fun showDatePicker(fechaEditText: TextInputEditText, issueView: IssueItemView) {
        val calendar = Calendar.getInstance()
        
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                issueView.fechaMillis = calendar.timeInMillis
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                fechaEditText.setText(sdf.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        datePickerDialog.show()
    }
    
    private fun actualizarTotal(issuesList: List<IssueItemView>, totalText: TextView) {
        val total = issuesList.count { it.mensajeEditText.text.toString().isNotEmpty() }
        totalText.text = "Total de Issues: $total"
    }
    
    private fun crearIssue(cliente: Cliente?, issuesList: List<IssueItemView>) {
        if (cliente == null) {
            Toast.makeText(this, "Debe seleccionar un cliente", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (issuesList.isEmpty()) {
            Toast.makeText(this, "Debe agregar al menos un issue", Toast.LENGTH_SHORT).show()
            return
        }
        
        val issueItems = issuesList.mapNotNull { issueView ->
            val titulo = issueView.tituloEditText.text.toString().trim()
            val mensaje = issueView.mensajeEditText.text.toString().trim()
            val fecha = issueView.fechaMillis
            
            if (titulo.isNotEmpty() && mensaje.isNotEmpty() && fecha > 0) {
                IssueItem(titulo, mensaje, fecha)
            } else {
                null
            }
        }
        
        if (issueItems.isEmpty()) {
            Toast.makeText(this, "Debe completar al menos un issue válido (título, mensaje y fecha)", Toast.LENGTH_LONG).show()
            return
        }
        
        val issue = Issue(
            clienteId = cliente.id,
            clienteNombre = cliente.nombre,
            issues = issueItems
        )
        
        issueManager.addIssue(issue)
        Toast.makeText(this, "Issue creado exitosamente", Toast.LENGTH_SHORT).show()
        loadIssues()
    }
    
    private fun editarIssue(issue: Issue) {
        if (!isAdminMode) {
            Toast.makeText(this, "Modo administrador requerido", Toast.LENGTH_SHORT).show()
            return
        }
        
        Toast.makeText(this, "Función de editar en desarrollo", Toast.LENGTH_SHORT).show()
    }
    
    private fun eliminarIssue(issue: Issue) {
        if (!isAdminMode) {
            Toast.makeText(this, "Modo administrador requerido", Toast.LENGTH_SHORT).show()
            return
        }
        
        AlertDialog.Builder(this)
            .setTitle("Eliminar Issue")
            .setMessage("¿Está seguro de eliminar el issue de ${issue.clienteNombre}?")
            .setPositiveButton("Eliminar") { _, _ ->
                issueManager.deleteIssue(issue)
                Toast.makeText(this, "Issue eliminado", Toast.LENGTH_SHORT).show()
                loadIssues()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun cambiarEstadoIssue(issue: Issue, nuevoEstado: EstadoIssue) {
        issueManager.updateEstado(issue.id, nuevoEstado)
        Toast.makeText(this, "Estado actualizado a: ${getEstadoTexto(nuevoEstado)}", Toast.LENGTH_SHORT).show()
        loadIssues()
    }
    
    private fun getEstadoTexto(estado: EstadoIssue): String {
        return when (estado) {
            EstadoIssue.PENDIENTE -> "Pendiente"
            EstadoIssue.EN_PROCESO -> "En Proceso"
            EstadoIssue.FINALIZADO -> "Finalizado"
        }
    }
    
    private fun mostrarDetalleIssue(issue: Issue) {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        
        val detalles = StringBuilder()
        detalles.append("Cliente: ${issue.clienteNombre}\n\n")
        detalles.append("Estado: ${issue.getEstadoTexto()}\n\n")
        detalles.append("Issues:\n")
        
        issue.issues.forEachIndexed { index, issueItem ->
            detalles.append("\n${index + 1}. ${issueItem.titulo}\n")
            detalles.append("   Mensaje: ${issueItem.mensaje}\n")
            detalles.append("   Fecha: ${sdf.format(Date(issueItem.fechaIssue))}\n")
        }
        
        detalles.append("\nTotal de Issues: ${issue.getTotalIssues()}")
        
        AlertDialog.Builder(this)
            .setTitle("Detalle de Issue")
            .setMessage(detalles.toString())
            .setPositiveButton("Cerrar", null)
            .show()
    }
    
    private fun generarReporteIssues() {
        val issues = issueManager.getAllIssues()
        
        if (issues.isEmpty()) {
            Toast.makeText(this, "No hay issues para generar reporte", Toast.LENGTH_SHORT).show()
            return
        }
        
        val intent = Intent(this, ReporteSoporteActivity::class.java)
        intent.putExtra("isAdminMode", isAdminMode)
        startActivity(intent)
    }
    
    private fun compartirIssue(issue: Issue) {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val sdfCreacion = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        
        val shareText = buildString {
            append("🔧 *REPORTE DE SOPORTE*\n\n")
            append("👤 *Cliente:* ${issue.clienteNombre}\n")
            append("📊 *Estado:* ${issue.getEstadoTexto()}\n")
            append("📅 *Creado:* ${sdfCreacion.format(Date(issue.fechaCreacion))}\n")
            append("📋 *Total de Issues:* ${issue.getTotalIssues()}\n\n")
            append("📝 *DETALLES DE ISSUES:*\n")
            
            issue.issues.forEachIndexed { index, issueItem ->
                append("\n${index + 1}. *${issueItem.titulo}*\n")
                append("   💬 *Mensaje:* ${issueItem.mensaje}\n")
                append("   📅 *Fecha:* ${sdf.format(Date(issueItem.fechaIssue))}\n")
            }
            
            append("\n📱 *Generado por:* CRM Checklist App")
        }
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        
        val chooser = Intent.createChooser(shareIntent, "Compartir Issue")
        startActivity(chooser)
    }
}

