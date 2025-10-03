package com.checklist.app

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class NotificacionManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("notificaciones_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    fun getAllNotificaciones(): List<Notificacion> {
        val json = prefs.getString("notificaciones", "[]")
        val type = object : TypeToken<List<Notificacion>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
    
    fun addNotificacion(notificacion: Notificacion): Long {
        val notificaciones = getAllNotificaciones().toMutableList()
        val newId = if (notificaciones.isEmpty()) 1L else (notificaciones.maxOfOrNull { it.id } ?: 0L) + 1
        val newNotificacion = notificacion.copy(id = newId)
        notificaciones.add(newNotificacion)
        saveNotificaciones(notificaciones)
        return newId
    }
    
    fun updateNotificacion(notificacion: Notificacion) {
        val notificaciones = getAllNotificaciones().toMutableList()
        val index = notificaciones.indexOfFirst { it.id == notificacion.id }
        if (index != -1) {
            notificaciones[index] = notificacion
            saveNotificaciones(notificaciones)
        }
    }
    
    fun deleteNotificacion(notificacion: Notificacion) {
        val notificaciones = getAllNotificaciones().toMutableList()
        notificaciones.removeAll { it.id == notificacion.id }
        saveNotificaciones(notificaciones)
    }
    
    fun getNotificacionById(id: Long): Notificacion? {
        return getAllNotificaciones().find { it.id == id }
    }
    
    fun getNotificacionesByClienteId(clienteId: Long): List<Notificacion> {
        return getAllNotificaciones().filter { it.clienteId == clienteId }
    }
    
    fun markAsEnviada(notificacionId: Long) {
        val notificacion = getNotificacionById(notificacionId)
        if (notificacion != null) {
            val updatedNotificacion = notificacion.copy(enviada = true)
            updateNotificacion(updatedNotificacion)
        }
    }
    
    private fun saveNotificaciones(notificaciones: List<Notificacion>) {
        val json = gson.toJson(notificaciones)
        prefs.edit().putString("notificaciones", json).apply()
    }
    
    fun getNotificacionesNoEnviadas(): List<Notificacion> {
        return getAllNotificaciones().filter { !it.enviada }
    }
    
    fun getNotificacionesEnviadas(): List<Notificacion> {
        return getAllNotificaciones().filter { it.enviada }
    }
}

