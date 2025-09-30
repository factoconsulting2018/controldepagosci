package com.checklist.app

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class ClienteEstado(
    val clienteId: Long,
    val estado: String, // "PENDIENTE" o "PAGADO"
    val fechaActualizacion: Long = System.currentTimeMillis(),
    val ejecutivoId: Long = 0L
)

class ClienteEstadoManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("checklist_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val KEY_ESTADOS = "cliente_estados"
        const val ESTADO_PENDIENTE = "PENDIENTE"
        const val ESTADO_PAGADO = "PAGADO"
    }
    
    fun getAllEstados(): List<ClienteEstado> {
        val json = prefs.getString(KEY_ESTADOS, "[]")
        val type = object : TypeToken<List<ClienteEstado>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
    
    fun getEstadoByClienteId(clienteId: Long): ClienteEstado? {
        return getAllEstados().find { it.clienteId == clienteId }
    }
    
    fun getEstadosByEstado(estado: String): List<ClienteEstado> {
        return getAllEstados().filter { it.estado == estado }
    }
    
    fun getCountByEstado(estado: String): Int {
        return getAllEstados().count { it.estado == estado }
    }
    
    fun insertOrUpdateEstado(clienteEstado: ClienteEstado) {
        val estados = getAllEstados().toMutableList()
        val existingIndex = estados.indexOfFirst { it.clienteId == clienteEstado.clienteId }
        
        if (existingIndex != -1) {
            estados[existingIndex] = clienteEstado
        } else {
            estados.add(clienteEstado)
        }
        
        saveEstados(estados)
        android.util.Log.d("ClienteEstadoManager", "Estado actualizado para cliente ${clienteEstado.clienteId}: ${clienteEstado.estado}")
    }
    
    fun updateEstadoCliente(clienteId: Long, nuevoEstado: String, ejecutivoId: Long = 0L) {
        val estadoActual = getEstadoByClienteId(clienteId)
        val fechaActualizacion = System.currentTimeMillis()
        
        val nuevoClienteEstado = if (estadoActual != null) {
            estadoActual.copy(
                estado = nuevoEstado,
                fechaActualizacion = fechaActualizacion,
                ejecutivoId = ejecutivoId
            )
        } else {
            ClienteEstado(
                clienteId = clienteId,
                estado = nuevoEstado,
                fechaActualizacion = fechaActualizacion,
                ejecutivoId = ejecutivoId
            )
        }
        
        insertOrUpdateEstado(nuevoClienteEstado)
    }
    
    fun updateAllEstados(nuevoEstado: String) {
        val estados = getAllEstados().toMutableList()
        val fechaActualizacion = System.currentTimeMillis()
        
        estados.forEach { estado ->
            val index = estados.indexOf(estado)
            estados[index] = estado.copy(
                estado = nuevoEstado,
                fechaActualizacion = fechaActualizacion
            )
        }
        
        saveEstados(estados)
        android.util.Log.d("ClienteEstadoManager", "Todos los estados actualizados a: $nuevoEstado")
    }
    
    fun deleteEstadoByClienteId(clienteId: Long) {
        val estados = getAllEstados().toMutableList()
        estados.removeAll { it.clienteId == clienteId }
        saveEstados(estados)
    }
    
    fun deleteAllEstados() {
        saveEstados(emptyList())
        android.util.Log.d("ClienteEstadoManager", "Todos los estados eliminados")
    }
    
    fun getEstadoString(clienteId: Long): String {
        return getEstadoByClienteId(clienteId)?.estado ?: ESTADO_PENDIENTE
    }
    
    fun isClientePagado(clienteId: Long): Boolean {
        return getEstadoString(clienteId) == ESTADO_PAGADO
    }
    
    fun isClientePendiente(clienteId: Long): Boolean {
        return getEstadoString(clienteId) == ESTADO_PENDIENTE
    }
    
    private fun saveEstados(estados: List<ClienteEstado>) {
        val json = gson.toJson(estados)
        prefs.edit().putString(KEY_ESTADOS, json).apply()
    }
    
    fun getResumenEstados(): Map<String, Int> {
        val estados = getAllEstados()
        return mapOf(
            ESTADO_PENDIENTE to estados.count { it.estado == ESTADO_PENDIENTE },
            ESTADO_PAGADO to estados.count { it.estado == ESTADO_PAGADO }
        )
    }
}
