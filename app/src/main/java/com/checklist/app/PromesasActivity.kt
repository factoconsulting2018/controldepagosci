package com.checklist.app

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textfield.TextInputEditText
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
    
    private fun setupRecyclerView() {
        promesasAdapter = PromesasAdapter(
            onVerDetalleClick = { promesa -> mostrarDetallePromesa(promesa) },
            onEditarClick = { promesa -> editarPromesa(promesa) },
            onEliminarClick = { promesa -> eliminarPromesa(promesa) },
            isAdminMode = { isAdminMode }
        )
        promesasRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@PromesasActivity)
            adapter = promesasAdapter
        }
    }
    
    private fun loadPromesas() {
        val promesas = promesaManager.getAllPromesas().sortedByDescending { it.fechaCreacion }
        promesasAdapter.submitList(promesas)
    }
    
    private fun showCrearPromesaDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_crear_promesa, null)
        
        val clienteAutoComplete = dialogView.findViewById<AutoCompleteTextView>(R.id.clienteAutoComplete)
        val clienteInfoCard = dialogView.findViewById<MaterialCardView>(R.id.clienteInfoCard)
        val clienteInfoNombre = dialogView.findViewById<TextView>(R.id.clienteInfoNombre)
        val clienteInfoCedula = dialogView.findViewById<TextView>(R.id.clienteInfoCedula)
        val promesasContainer = dialogView.findViewById<LinearLayout>(R.id.promesasContainer)
        val btnAgregarPromesa = dialogView.findViewById<Button>(R.id.btnAgregarPromesa)
        val totalText = dialogView.findViewById<TextView>(R.id.totalText)
        
        var clienteSeleccionado: Cliente? = null
        val promesasPagoList = mutableListOf<PromesaPagoView>()
        
        val clientes = clienteManager.getAllClientes()
        val clientesNombres = clientes.map { "${it.nombre} - ${it.cedula}" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, clientesNombres)
        clienteAutoComplete.setAdapter(adapter)
        
        clienteAutoComplete.setOnItemClickListener { _, _, position, _ ->
            clienteSeleccionado = clientes[position]
            clienteInfoCard.visibility = View.VISIBLE
            clienteInfoNombre.text = "Nombre: ${clienteSeleccionado?.nombre}"
            clienteInfoCedula.text = "Cédula: ${clienteSeleccionado?.cedula}"
        }
        
        btnAgregarPromesa.setOnClickListener {
            val promesaView = addPromesaPagoView(promesasContainer, promesasPagoList.size + 1, promesasPagoList, totalText)
            promesasPagoList.add(promesaView)
        }
        
        // Agregar al menos una promesa por defecto
        val promesaView = addPromesaPagoView(promesasContainer, 1, promesasPagoList, totalText)
        promesasPagoList.add(promesaView)
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Crear Promesa") { _, _ ->
                crearPromesa(clienteSeleccionado, promesasPagoList)
            }
            .setNegativeButton("Cancelar", null)
            .create()
        
        dialog.show()
    }
    
    data class PromesaPagoView(
        val view: View,
        val tituloEditText: TextInputEditText,
        val montoEditText: TextInputEditText,
        val fechaEditText: TextInputEditText,
        var fechaMillis: Long = 0
    )
    
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
        
        val promesaView = PromesaPagoView(itemView, tituloEditText, montoEditText, fechaEditText)
        
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
            
            if (titulo.isNotEmpty() && monto > 0 && fecha > 0) {
                PromesaPago(titulo, monto, fecha)
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
        Toast.makeText(this, "Promesa creada exitosamente", Toast.LENGTH_SHORT).show()
        loadPromesas()
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

