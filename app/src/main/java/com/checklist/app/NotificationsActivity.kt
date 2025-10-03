package com.checklist.app

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class NotificationsActivity : AppCompatActivity() {
    
    private lateinit var notificacionManager: NotificacionManager
    private lateinit var clienteManager: ClienteManager
    private lateinit var notificacionesRecyclerView: RecyclerView
    private lateinit var notificacionesAdapter: NotificacionesAdapter
    private lateinit var fabCrearNotificacion: FloatingActionButton
    private var isAdminMode = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_notifications)
        
        isAdminMode = intent.getBooleanExtra("isAdminMode", false)
        
        notificacionManager = NotificacionManager(this)
        clienteManager = ClienteManager(this)
        
        setupViews()
        setupRecyclerView()
        loadNotificaciones()
    }
    
    private fun setupViews() {
        notificacionesRecyclerView = findViewById(R.id.notificacionesRecyclerView)
        fabCrearNotificacion = findViewById(R.id.fabCrearNotificacion)
        
        fabCrearNotificacion.setOnClickListener {
            showCrearNotificacionDialog()
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
        notificacionesAdapter = NotificacionesAdapter(
            onEnviarClick = { notificacion -> enviarPorWhatsApp(notificacion) },
            onVerMasClick = { notificacion -> mostrarDetalleNotificacion(notificacion) },
            onCompartirClick = { notificacion -> compartirMensaje(notificacion) },
            onEditarClick = { notificacion -> editarNotificacion(notificacion) },
            onDeleteClick = { notificacion -> eliminarNotificacion(notificacion) },
            isAdminMode = { isAdminMode }
        )
        notificacionesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@NotificationsActivity)
            adapter = notificacionesAdapter
        }
    }
    
    private fun loadNotificaciones() {
        val notificaciones = notificacionManager.getAllNotificaciones().sortedByDescending { it.fechaCreacion }
        notificacionesAdapter.submitList(notificaciones)
    }
    
    private fun showCrearNotificacionDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_crear_notificacion, null)
        
        val clienteSearchEditText = dialogView.findViewById<TextInputEditText>(R.id.clienteSearchEditText)
        val filterNombreChip = dialogView.findViewById<Chip>(R.id.filterNombreChip)
        val filterCedulaChip = dialogView.findViewById<Chip>(R.id.filterCedulaChip)
        val filterTelefonoChip = dialogView.findViewById<Chip>(R.id.filterTelefonoChip)
        val clientesRecyclerView = dialogView.findViewById<RecyclerView>(R.id.clientesRecyclerView)
        val clienteInfoCard = dialogView.findViewById<MaterialCardView>(R.id.clienteInfoCard)
        val clienteInfoNombre = dialogView.findViewById<TextView>(R.id.clienteInfoNombre)
        val clienteInfoCedula = dialogView.findViewById<TextView>(R.id.clienteInfoCedula)
        val clienteInfoTelefono = dialogView.findViewById<TextView>(R.id.clienteInfoTelefono)
        val clienteInfoEjecutivo = dialogView.findViewById<TextView>(R.id.clienteInfoEjecutivo)
        val montoPendienteEditText = dialogView.findViewById<TextInputEditText>(R.id.montoPendienteEditText)
        val mensajeEditText = dialogView.findViewById<TextInputEditText>(R.id.mensajeEditText)
        val caracteresCounter = dialogView.findViewById<TextView>(R.id.caracteresCounter)
        val mensajePreview = dialogView.findViewById<TextView>(R.id.mensajePreview)
        
        var clienteSeleccionado: Cliente? = null
        
        // Configurar RecyclerView de clientes
        val clienteAdapter = ClienteBusquedaAdapter { cliente ->
            clienteSeleccionado = cliente
            clienteInfoCard.visibility = View.VISIBLE
            clienteInfoNombre.text = "Nombre: ${cliente.nombre}"
            clienteInfoCedula.text = "Cédula: ${cliente.cedula}"
            clienteInfoTelefono.text = "Teléfono: ${cliente.telefono}"
            clienteInfoEjecutivo.text = "Ejecutivo: ${cliente.ejecutivo}"
            clientesRecyclerView.visibility = View.GONE
            clienteSearchEditText.setText("${cliente.nombre} - ${cliente.cedula}")
            updateMensajePreview(mensajeEditText.text.toString(), clienteSeleccionado, montoPendienteEditText.text.toString(), mensajePreview)
        }
        
        clientesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@NotificationsActivity)
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
        
        mensajeEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                caracteresCounter.text = "${s?.length ?: 0}/500 caracteres"
                updateMensajePreview(s.toString(), clienteSeleccionado, montoPendienteEditText.text.toString(), mensajePreview)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        
        montoPendienteEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateMensajePreview(mensajeEditText.text.toString(), clienteSeleccionado, s.toString(), mensajePreview)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        
        mensajeEditText.setText("Estimado(a) {nombre}, le recordamos que tiene un saldo pendiente de {monto} colones. Por favor, comuníquese con su ejecutivo {ejecutivo} para más información.\n\nTeléfono: 4070-0485 / 8613-0001")
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Crear Notificación") { _, _ ->
                crearNotificacion(clienteSeleccionado, montoPendienteEditText.text.toString(), mensajeEditText.text.toString())
            }
            .setNegativeButton("Cancelar", null)
            .create()
        
        dialog.show()
    }
    
    private fun crearNotificacion(cliente: Cliente?, montoStr: String, mensaje: String) {
        val monto = montoStr.toDoubleOrNull() ?: 0.0
        
        if (cliente == null) {
            Toast.makeText(this, "Debe seleccionar un cliente", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (monto <= 0) {
            Toast.makeText(this, "Debe ingresar un monto válido", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (mensaje.isEmpty()) {
            Toast.makeText(this, "Debe ingresar un mensaje", Toast.LENGTH_SHORT).show()
            return
        }
        
        val mensajeFinal = replacePlaceholders(mensaje, cliente, monto)
        
        val notificacion = Notificacion(
            clienteId = cliente.id,
            clienteNombre = cliente.nombre,
            clienteTelefono = cliente.telefono,
            montoPendiente = monto,
            mensaje = mensajeFinal
        )
        
        notificacionManager.addNotificacion(notificacion)
        Toast.makeText(this, "Notificación creada exitosamente", Toast.LENGTH_SHORT).show()
        loadNotificaciones()
    }
    
    private fun updateMensajePreview(mensaje: String, cliente: Cliente?, monto: String, previewTextView: TextView) {
        if (cliente == null) {
            previewTextView.text = "Seleccione un cliente para ver la vista previa..."
            return
        }
        
        val montoDouble = monto.toDoubleOrNull() ?: 0.0
        val mensajePreview = replacePlaceholders(mensaje, cliente, montoDouble)
        previewTextView.text = mensajePreview
    }
    
    private fun replacePlaceholders(mensaje: String, cliente: Cliente, monto: Double): String {
        val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CR"))
        val montoFormateado = numberFormat.format(monto)
        
        return mensaje
            .replace("{nombre}", cliente.nombre)
            .replace("{cedula}", cliente.cedula)
            .replace("{monto}", montoFormateado)
            .replace("{ejecutivo}", cliente.ejecutivo)
            .replace("{telefono}", cliente.telefono)
            .replace("{tipo}", cliente.tipoPersona)
    }
    
    private fun enviarPorWhatsApp(notificacion: Notificacion) {
        try {
            var telefono = notificacion.clienteTelefono.replace(Regex("[^0-9]"), "")
            
            if (!telefono.startsWith("506")) {
                telefono = "506$telefono"
            }
            
            val intent = Intent(Intent.ACTION_VIEW)
            val url = "https://api.whatsapp.com/send?phone=$telefono&text=${Uri.encode(notificacion.mensaje)}"
            intent.data = Uri.parse(url)
            
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
                notificacionManager.markAsEnviada(notificacion.id)
                loadNotificaciones()
                Toast.makeText(this, "Abriendo WhatsApp...", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "WhatsApp no está instalado", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error al abrir WhatsApp: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun mostrarDetalleNotificacion(notificacion: Notificacion) {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val fecha = sdf.format(Date(notificacion.fechaCreacion))
        
        val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CR"))
        val montoFormateado = numberFormat.format(notificacion.montoPendiente)
        
        val mensaje = """
            Cliente: ${notificacion.clienteNombre}
            Teléfono: ${notificacion.clienteTelefono}
            Monto Pendiente: $montoFormateado
            Fecha de Creación: $fecha
            Estado: ${if (notificacion.enviada) "Enviada" else "Pendiente"}
            
            Mensaje:
            ${notificacion.mensaje}
        """.trimIndent()
        
        AlertDialog.Builder(this)
            .setTitle("Detalle de Notificación")
            .setMessage(mensaje)
            .setPositiveButton("Cerrar", null)
            .setNeutralButton("Enviar por WhatsApp") { _, _ ->
                enviarPorWhatsApp(notificacion)
            }
            .setNegativeButton("Eliminar") { _, _ ->
                eliminarNotificacion(notificacion)
            }
            .show()
    }
    
    private fun compartirMensaje(notificacion: Notificacion) {
        try {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_SUBJECT, "Notificación de Deuda - ${notificacion.clienteNombre}")
            intent.putExtra(Intent.EXTRA_TEXT, notificacion.mensaje)
            
            startActivity(Intent.createChooser(intent, "Compartir notificación mediante..."))
        } catch (e: Exception) {
            Toast.makeText(this, "Error al compartir: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun editarNotificacion(notificacion: Notificacion) {
        if (!isAdminMode) {
            Toast.makeText(this, "Modo administrador requerido para editar", Toast.LENGTH_SHORT).show()
            return
        }
        
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_crear_notificacion, null)
        
        val clienteSearchEditText = dialogView.findViewById<TextInputEditText>(R.id.clienteSearchEditText)
        val filterNombreChip = dialogView.findViewById<Chip>(R.id.filterNombreChip)
        val filterCedulaChip = dialogView.findViewById<Chip>(R.id.filterCedulaChip)
        val filterTelefonoChip = dialogView.findViewById<Chip>(R.id.filterTelefonoChip)
        val clientesRecyclerView = dialogView.findViewById<RecyclerView>(R.id.clientesRecyclerView)
        val clienteInfoCard = dialogView.findViewById<MaterialCardView>(R.id.clienteInfoCard)
        val clienteInfoNombre = dialogView.findViewById<TextView>(R.id.clienteInfoNombre)
        val clienteInfoCedula = dialogView.findViewById<TextView>(R.id.clienteInfoCedula)
        val clienteInfoTelefono = dialogView.findViewById<TextView>(R.id.clienteInfoTelefono)
        val clienteInfoEjecutivo = dialogView.findViewById<TextView>(R.id.clienteInfoEjecutivo)
        val montoPendienteEditText = dialogView.findViewById<TextInputEditText>(R.id.montoPendienteEditText)
        val mensajeEditText = dialogView.findViewById<TextInputEditText>(R.id.mensajeEditText)
        val caracteresCounter = dialogView.findViewById<TextView>(R.id.caracteresCounter)
        val mensajePreview = dialogView.findViewById<TextView>(R.id.mensajePreview)
        
        val cliente = clienteManager.getClienteById(notificacion.clienteId)
        
        // Configurar RecyclerView de clientes
        val clienteAdapter = ClienteBusquedaAdapter { clienteSeleccionado ->
            clienteInfoCard.visibility = View.VISIBLE
            clienteInfoNombre.text = "Nombre: ${clienteSeleccionado.nombre}"
            clienteInfoCedula.text = "Cédula: ${clienteSeleccionado.cedula}"
            clienteInfoTelefono.text = "Teléfono: ${clienteSeleccionado.telefono}"
            clienteInfoEjecutivo.text = "Ejecutivo: ${clienteSeleccionado.ejecutivo}"
            clientesRecyclerView.visibility = View.GONE
            clienteSearchEditText.setText("${clienteSeleccionado.nombre} - ${clienteSeleccionado.cedula}")
            updateMensajePreview(mensajeEditText.text.toString(), clienteSeleccionado, montoPendienteEditText.text.toString(), mensajePreview)
        }
        
        clientesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@NotificationsActivity)
            adapter = clienteAdapter
        }
        
        // Preseleccionar el cliente actual
        if (cliente != null) {
            clienteSearchEditText.setText("${cliente.nombre} - ${cliente.cedula}")
            clienteInfoCard.visibility = View.VISIBLE
            clienteInfoNombre.text = "Nombre: ${cliente.nombre}"
            clienteInfoCedula.text = "Cédula: ${cliente.cedula}"
            clienteInfoTelefono.text = "Teléfono: ${cliente.telefono}"
            clienteInfoEjecutivo.text = "Ejecutivo: ${cliente.ejecutivo}"
        }
        
        montoPendienteEditText.setText(notificacion.montoPendiente.toString())
        
        // Obtener mensaje original con etiquetas
        val mensajeOriginal = obtenerMensajeConEtiquetas(notificacion, cliente)
        mensajeEditText.setText(mensajeOriginal)
        
        var clienteSeleccionado: Cliente? = cliente
        
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
        
        mensajeEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                caracteresCounter.text = "${s?.length ?: 0}/500 caracteres"
                updateMensajePreview(s.toString(), clienteSeleccionado, montoPendienteEditText.text.toString(), mensajePreview)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        
        montoPendienteEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateMensajePreview(mensajeEditText.text.toString(), clienteSeleccionado, s.toString(), mensajePreview)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        
        val dialog = AlertDialog.Builder(this)
            .setTitle("Editar Notificación")
            .setView(dialogView)
            .setPositiveButton("Guardar Cambios") { _, _ ->
                actualizarNotificacion(notificacion, clienteSeleccionado, montoPendienteEditText.text.toString(), mensajeEditText.text.toString())
            }
            .setNegativeButton("Cancelar", null)
            .create()
        
        dialog.show()
    }
    
    private fun obtenerMensajeConEtiquetas(notificacion: Notificacion, cliente: Cliente?): String {
        if (cliente == null) return notificacion.mensaje
        
        val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CR"))
        val montoFormateado = numberFormat.format(notificacion.montoPendiente)
        
        return notificacion.mensaje
            .replace(cliente.nombre, "{nombre}")
            .replace(cliente.cedula, "{cedula}")
            .replace(montoFormateado, "{monto}")
            .replace(cliente.ejecutivo, "{ejecutivo}")
            .replace(cliente.telefono, "{telefono}")
            .replace(cliente.tipoPersona, "{tipo}")
    }
    
    private fun actualizarNotificacion(notificacionOriginal: Notificacion, cliente: Cliente?, montoStr: String, mensaje: String) {
        val monto = montoStr.toDoubleOrNull() ?: 0.0
        
        if (cliente == null) {
            Toast.makeText(this, "Debe seleccionar un cliente", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (monto <= 0) {
            Toast.makeText(this, "Debe ingresar un monto válido", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (mensaje.isEmpty()) {
            Toast.makeText(this, "Debe ingresar un mensaje", Toast.LENGTH_SHORT).show()
            return
        }
        
        val mensajeFinal = replacePlaceholders(mensaje, cliente, monto)
        
        val notificacionActualizada = notificacionOriginal.copy(
            clienteId = cliente.id,
            clienteNombre = cliente.nombre,
            clienteTelefono = cliente.telefono,
            montoPendiente = monto,
            mensaje = mensajeFinal,
            enviada = false // Reset estado al editar
        )
        
        notificacionManager.updateNotificacion(notificacionActualizada)
        Toast.makeText(this, "Notificación actualizada exitosamente", Toast.LENGTH_SHORT).show()
        loadNotificaciones()
    }
    
    private fun eliminarNotificacion(notificacion: Notificacion) {
        if (!isAdminMode) {
            Toast.makeText(this, "Modo administrador requerido para eliminar", Toast.LENGTH_SHORT).show()
            return
        }
        
        AlertDialog.Builder(this)
            .setTitle("Eliminar Notificación")
            .setMessage("¿Está seguro de que desea eliminar esta notificación?")
            .setPositiveButton("Eliminar") { _, _ ->
                notificacionManager.deleteNotificacion(notificacion)
                Toast.makeText(this, "Notificación eliminada", Toast.LENGTH_SHORT).show()
                loadNotificaciones()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}

