package com.checklist.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class PromesasAdapter(
    private val onVerDetalleClick: (Promesa) -> Unit,
    private val onEditarClick: (Promesa) -> Unit,
    private val onEliminarClick: (Promesa) -> Unit,
    private val onImageClick: (Bitmap) -> Unit,
    private val isAdminMode: () -> Boolean
) : RecyclerView.Adapter<PromesasAdapter.PromesaViewHolder>() {
    
    private var promesas = listOf<Promesa>()
    
    fun submitList(newList: List<Promesa>) {
        promesas = newList
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PromesaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_promesa, parent, false)
        return PromesaViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: PromesaViewHolder, position: Int) {
        holder.bind(promesas[position])
    }
    
    override fun getItemCount() = promesas.size
    
    inner class PromesaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val clienteNombreText: TextView = itemView.findViewById(R.id.clienteNombreText)
        private val promesasPagoLayout: LinearLayout = itemView.findViewById(R.id.promesasPagoLayout)
        private val totalClienteText: TextView = itemView.findViewById(R.id.totalClienteText)
        private val fechaCreacionText: TextView = itemView.findViewById(R.id.fechaCreacionText)
        private val btnVerDetalle: Button = itemView.findViewById(R.id.btnVerDetalle)
        private val adminButtonsLayout: LinearLayout = itemView.findViewById(R.id.adminButtonsLayout)
        private val btnEditarPromesa: Button = itemView.findViewById(R.id.btnEditarPromesa)
        private val btnEliminarPromesa: Button = itemView.findViewById(R.id.btnEliminarPromesa)
        
        fun bind(promesa: Promesa) {
            clienteNombreText.text = promesa.clienteNombre
            
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CR"))
            
            promesasPagoLayout.removeAllViews()
            
            promesa.promesasPago.forEachIndexed { index, promesaPago ->
                val promesaView = LayoutInflater.from(itemView.context).inflate(R.layout.item_promesa_pago_card, promesasPagoLayout, false)
                
                val tituloText = promesaView.findViewById<TextView>(R.id.tituloText)
                val montoText = promesaView.findViewById<TextView>(R.id.montoText)
                val fechaText = promesaView.findViewById<TextView>(R.id.fechaText)
                val imagePreview = promesaView.findViewById<ImageView>(R.id.imagePreview)
                
                tituloText.text = "${index + 1}. ${promesaPago.titulo}"
                montoText.text = numberFormat.format(promesaPago.monto)
                fechaText.text = sdf.format(Date(promesaPago.fecha))
                
                // Mostrar imagen si existe
                promesaPago.imageData?.let { imageData ->
                    val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                    imagePreview.setImageBitmap(bitmap)
                    imagePreview.visibility = View.VISIBLE
                    
                    // Hacer clic para ver imagen en grande
                    imagePreview.setOnClickListener {
                        onImageClick(bitmap)
                    }
                } ?: run {
                    imagePreview.visibility = View.GONE
                }
                
                promesasPagoLayout.addView(promesaView)
            }
            
            totalClienteText.text = "Total: ${numberFormat.format(promesa.getTotalMonto())}"
            
            val sdfCreacion = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            fechaCreacionText.text = "Creada: ${sdfCreacion.format(Date(promesa.fechaCreacion))}"
            
            btnVerDetalle.setOnClickListener { onVerDetalleClick(promesa) }
            
            if (isAdminMode()) {
                adminButtonsLayout.visibility = View.VISIBLE
                btnEditarPromesa.setOnClickListener { onEditarClick(promesa) }
                btnEliminarPromesa.setOnClickListener { onEliminarClick(promesa) }
            } else {
                adminButtonsLayout.visibility = View.GONE
            }
        }
    }
}

