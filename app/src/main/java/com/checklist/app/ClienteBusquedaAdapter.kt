package com.checklist.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class ClienteBusquedaAdapter(
    private val onClienteClick: (Cliente) -> Unit
) : RecyclerView.Adapter<ClienteBusquedaAdapter.ClienteViewHolder>() {

    private var clientes = listOf<Cliente>()

    fun updateClientes(clientes: List<Cliente>) {
        this.clientes = clientes
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClienteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cliente_busqueda, parent, false)
        return ClienteViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClienteViewHolder, position: Int) {
        holder.bind(clientes[position])
    }

    override fun getItemCount(): Int = clientes.size

    inner class ClienteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.clienteCard)
        private val nombre: TextView = itemView.findViewById(R.id.clienteNombre)
        private val cedula: TextView = itemView.findViewById(R.id.clienteCedula)
        private val telefono: TextView = itemView.findViewById(R.id.clienteTelefono)
        private val tipoPersona: TextView = itemView.findViewById(R.id.clienteTipoPersona)
        private val ejecutivo: TextView = itemView.findViewById(R.id.clienteEjecutivo)

        fun bind(cliente: Cliente) {
            nombre.text = cliente.nombre
            cedula.text = "Cédula: ${cliente.cedula}"
            telefono.text = "Tel: ${cliente.telefono}"
            tipoPersona.text = cliente.tipoPersona
            ejecutivo.text = "Ejecutivo: ${cliente.ejecutivo}"

            cardView.setOnClickListener {
                onClienteClick(cliente)
            }
        }
    }
}
