package com.checklist.app

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
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
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class PromesasActivity : AppCompatActivity() {
    
    private lateinit var promesaManager: PromesaManager
    private lateinit var clienteManager: ClienteManager
    private lateinit var promesasRecyclerView: RecyclerView
    private lateinit var promesasAdapter: PromesasAdapter
    private lateinit var fabNuevaPromesa: ExtendedFloatingActionButton
    private lateinit var fabGenerarReporte: ExtendedFloatingActionButton
    private var isAdminMode = false
    private var isImageUploadEnabled = false
    private var currentPromesaView: PromesaPagoView? = null
    private val PICK_IMAGE_REQUEST = 1001
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_promesas)
        
        isAdminMode = intent.getBooleanExtra("isAdminMode", false)
        
        promesaManager = PromesaManager(this)
        clienteManager = ClienteManager(this)
        
        setupViews()
        setupRecyclerView()
        loadPromesas()
    }
    
    private fun setupViews() {
        promesasRecyclerView = findViewById(R.id.promesasRecyclerView)
        fabNuevaPromesa = findViewById(R.id.fabNuevaPromesa)
        fabGenerarReporte = findViewById(R.id.fabGenerarReporte)
        
        // Cargar configuración de subida de imágenes desde ConfigActivity
        loadImageUploadSetting()
        
        fabNuevaPromesa.setOnClickListener {
            showCrearPromesaDialog()
        }
        
        fabGenerarReporte.setOnClickListener {
            generarReportePromesas()
        }
        
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName
            findViewById<TextView>(R.id.footerText).text = "Desarrollada por Ing.Ronald Rojas C | 8878-1108 | Ver: $versionName"
        } catch (e: Exception) {
            findViewById<TextView>(R.id.footerText).text = "Desarrollada por Ing.Ronald Rojas C | 8878-1108 | Ver: 1.0"
        }
    }
    
    private fun loadImageUploadSetting() {
        // Cargar configuración desde las preferencias de ConfigActivity
        val prefs = getSharedPreferences("checklist_prefs", MODE_PRIVATE)
        isImageUploadEnabled = prefs.getBoolean("image_upload_enabled", false)
        
        android.util.Log.d("PromesasActivity", "Image upload setting loaded: $isImageUploadEnabled")
    }
    
    private fun setupRecyclerView() {
        promesasAdapter = PromesasAdapter(
            onVerDetalleClick = { promesa -> mostrarDetallePromesa(promesa) },
            onEditarClick = { promesa -> editarPromesa(promesa) },
            onEliminarClick = { promesa -> eliminarPromesa(promesa) },
            onImageClick = { bitmap -> showImageDialog(bitmap) },
            isAdminMode = { isAdminMode }
        )
        promesasRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@PromesasActivity)
            adapter = promesasAdapter
        }
    }
    
    fun isImageUploadEnabled(): Boolean {
        return isImageUploadEnabled
    }
    
    companion object {
        fun isImageUploadEnabled(context: android.content.Context): Boolean {
            val prefs = context.getSharedPreferences("checklist_prefs", android.content.Context.MODE_PRIVATE)
            return prefs.getBoolean("image_upload_enabled", false)
        }
    }
    
    private fun loadPromesas() {
        val promesas = promesaManager.getAllPromesas().sortedByDescending { it.fechaCreacion }
        promesasAdapter.submitList(promesas)
    }
    
    private fun showCrearPromesaDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_crear_promesa, null)
        
        val clienteSearchEditText = dialogView.findViewById<TextInputEditText>(R.id.clienteSearchEditText)
        val filterNombreChip = dialogView.findViewById<Chip>(R.id.filterNombreChip)
        val filterCedulaChip = dialogView.findViewById<Chip>(R.id.filterCedulaChip)
        val filterTelefonoChip = dialogView.findViewById<Chip>(R.id.filterTelefonoChip)
        val clientesRecyclerView = dialogView.findViewById<RecyclerView>(R.id.clientesRecyclerView)
        val clienteInfoCard = dialogView.findViewById<MaterialCardView>(R.id.clienteInfoCard)
        val clienteInfoNombre = dialogView.findViewById<TextView>(R.id.clienteInfoNombre)
        val clienteInfoCedula = dialogView.findViewById<TextView>(R.id.clienteInfoCedula)
        val promesasContainer = dialogView.findViewById<LinearLayout>(R.id.promesasContainer)
        val btnAgregarPromesa = dialogView.findViewById<Button>(R.id.btnAgregarPromesa)
        val totalText = dialogView.findViewById<TextView>(R.id.totalText)
        val btnGuardarPromesa = dialogView.findViewById<Button>(R.id.btnGuardarPromesa)
        
        var clienteSeleccionado: Cliente? = null
        val promesasPagoList = mutableListOf<PromesaPagoView>()
        
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
            layoutManager = LinearLayoutManager(this@PromesasActivity)
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
        
        // Agregar al menos una promesa por defecto
        val promesaView = addPromesaPagoView(promesasContainer, 1, promesasPagoList, totalText)
        promesasPagoList.add(promesaView)
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("Cancelar", null)
            .create()
        
        btnAgregarPromesa.setOnClickListener {
            val promesaView = addPromesaPagoView(promesasContainer, promesasPagoList.size + 1, promesasPagoList, totalText)
            promesasPagoList.add(promesaView)
        }
        
        btnGuardarPromesa.setOnClickListener {
            crearPromesa(clienteSeleccionado, promesasPagoList)
            dialog.dismiss()
        }
        
        // Mostrar información sobre subida de imágenes si está habilitada
        if (isImageUploadEnabled) {
            dialog.setMessage("La subida de imágenes está activada. Podrás agregar imágenes a las promesas de pago.")
        }
        
        dialog.show()
    }
    
    data class PromesaPagoView(
        val view: View,
        val tituloEditText: TextInputEditText,
        val montoEditText: TextInputEditText,
        val fechaEditText: TextInputEditText,
        val imagePreview: ImageView
    ) {
        var fechaMillis: Long = 0L
        var imageBitmap: Bitmap? = null
    }
    
    private fun addPromesaPagoView(
        container: LinearLayout, 
        numero: Int, 
        promesasList: MutableList<PromesaPagoView>,
        totalText: TextView
    ): PromesaPagoView {
        val itemView = LayoutInflater.from(this).inflate(R.layout.item_promesa_pago, container, false)
        
        val tituloEditText = itemView.findViewById<TextInputEditText>(R.id.tituloEditText)
        val montoEditText = itemView.findViewById<TextInputEditText>(R.id.montoEditText)
        val fechaEditText = itemView.findViewById<TextInputEditText>(R.id.fechaEditText)
        val btnEliminar = itemView.findViewById<ImageButton>(R.id.btnEliminarPromesaItem)
        val imageUploadLayout = itemView.findViewById<LinearLayout>(R.id.imageUploadLayout)
        val btnSelectImage = itemView.findViewById<Button>(R.id.btnSelectImage)
        val imagePreview = itemView.findViewById<ImageView>(R.id.imagePreview)
        
        val promesaView = PromesaPagoView(itemView, tituloEditText, montoEditText, fechaEditText, imagePreview)
        
        // Mostrar/ocultar opciones de imagen según configuración
        if (isImageUploadEnabled) {
            imageUploadLayout.visibility = View.VISIBLE
            btnSelectImage.setOnClickListener {
                currentPromesaView = promesaView
                selectImage()
            }
        } else {
            imageUploadLayout.visibility = View.GONE
        }
        
        fechaEditText.setOnClickListener {
            showDatePicker(fechaEditText, promesaView)
        }
        
        btnEliminar.setOnClickListener {
            container.removeView(itemView)
            promesasList.remove(promesaView)
            actualizarTotal(promesasList, totalText)
        }
        
        montoEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                actualizarTotal(promesasList, totalText)
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        
        container.addView(itemView)
        return promesaView
    }
    
    private fun showDatePicker(fechaEditText: TextInputEditText, promesaView: PromesaPagoView) {
        val calendar = Calendar.getInstance()
        
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                promesaView.fechaMillis = calendar.timeInMillis
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                fechaEditText.setText(sdf.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        datePickerDialog.show()
    }
    
    private fun selectImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            val selectedImageUri: Uri? = data.data
            selectedImageUri?.let { uri ->
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    
                    currentPromesaView?.let { promesaView ->
                        promesaView.imageBitmap = bitmap
                        promesaView.imagePreview.setImageBitmap(bitmap)
                        promesaView.imagePreview.visibility = View.VISIBLE
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PromesasActivity", "Error loading image: ${e.message}")
                    Toast.makeText(this, "Error al cargar la imagen", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun actualizarTotal(promesasList: List<PromesaPagoView>, totalText: TextView) {
        val total = promesasList.sumOf {
            it.montoEditText.text.toString().toDoubleOrNull() ?: 0.0
        }
        val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CR"))
        totalText.text = "Total: ${numberFormat.format(total)}"
    }
    
    private fun crearPromesa(cliente: Cliente?, promesasPagoList: List<PromesaPagoView>) {
        if (cliente == null) {
            Toast.makeText(this, "Debe seleccionar un cliente", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (promesasPagoList.isEmpty()) {
            Toast.makeText(this, "Debe agregar al menos una promesa de pago", Toast.LENGTH_SHORT).show()
            return
        }
        
        val promesasPago = promesasPagoList.mapNotNull { promesaView ->
            val titulo = promesaView.tituloEditText.text.toString().trim()
            val monto = promesaView.montoEditText.text.toString().toDoubleOrNull() ?: 0.0
            val fecha = promesaView.fechaMillis
            val imageData = promesaView.imageBitmap?.let { bitmap ->
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                stream.toByteArray()
            }
            
            if (titulo.isNotEmpty() && monto > 0 && fecha > 0) {
                PromesaPago(titulo, monto, fecha, imageData)
            } else {
                null
            }
        }
        
        if (promesasPago.isEmpty()) {
            Toast.makeText(this, "Debe completar al menos una promesa válida (título, monto y fecha)", Toast.LENGTH_LONG).show()
            return
        }
        
        val promesa = Promesa(
            clienteId = cliente.id,
            clienteNombre = cliente.nombre,
            promesasPago = promesasPago
        )
        
        promesaManager.addPromesa(promesa)
        loadPromesas()
        
        // Mostrar diálogo de confirmación con ícono de check
        showSuccessDialog()
    }
    
    private fun showSuccessDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_success, null)
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Aceptar") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        
        dialog.show()
    }
    
    private fun showImageDialog(bitmap: Bitmap) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_image_viewer, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.imageView)
        
        imageView.setImageBitmap(bitmap)
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Cerrar") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        
        dialog.show()
    }
    
    private fun editarPromesa(promesa: Promesa) {
        if (!isAdminMode) {
            Toast.makeText(this, "Modo administrador requerido", Toast.LENGTH_SHORT).show()
            return
        }
        
        Toast.makeText(this, "Función de editar en desarrollo", Toast.LENGTH_SHORT).show()
    }
    
    private fun eliminarPromesa(promesa: Promesa) {
        if (!isAdminMode) {
            Toast.makeText(this, "Modo administrador requerido", Toast.LENGTH_SHORT).show()
            return
        }
        
        AlertDialog.Builder(this)
            .setTitle("Eliminar Promesa")
            .setMessage("¿Está seguro de eliminar la promesa de ${promesa.clienteNombre}?")
            .setPositiveButton("Eliminar") { _, _ ->
                promesaManager.deletePromesa(promesa)
                Toast.makeText(this, "Promesa eliminada", Toast.LENGTH_SHORT).show()
                loadPromesas()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun mostrarDetallePromesa(promesa: Promesa) {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CR"))
        
        val detalles = StringBuilder()
        detalles.append("Cliente: ${promesa.clienteNombre}\n\n")
        detalles.append("Promesas de Pago:\n")
        
        promesa.promesasPago.forEachIndexed { index, promesaPago ->
            detalles.append("\n${index + 1}. ${promesaPago.titulo}\n")
            detalles.append("   Monto: ${numberFormat.format(promesaPago.monto)}\n")
            detalles.append("   Fecha: ${sdf.format(Date(promesaPago.fecha))}\n")
        }
        
        detalles.append("\nTotal: ${numberFormat.format(promesa.getTotalMonto())}")
        
        AlertDialog.Builder(this)
            .setTitle("Detalle de Promesa")
            .setMessage(detalles.toString())
            .setPositiveButton("Cerrar", null)
            .show()
    }
    
    private fun generarReportePromesas() {
        val promesas = promesaManager.getAllPromesas()
        
        if (promesas.isEmpty()) {
            Toast.makeText(this, "No hay promesas para generar reporte", Toast.LENGTH_SHORT).show()
            return
        }
        
        val intent = Intent(this, ReportePromesasActivity::class.java)
        startActivity(intent)
    }
}

