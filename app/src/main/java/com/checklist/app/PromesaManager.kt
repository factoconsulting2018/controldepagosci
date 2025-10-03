package com.checklist.app

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PromesaManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("promesas_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    fun getAllPromesas(): List<Promesa> {
        val json = prefs.getString("promesas", "[]")
        val type = object : TypeToken<List<Promesa>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
    
    fun addPromesa(promesa: Promesa): Long {
        val promesas = getAllPromesas().toMutableList()
        val newId = if (promesas.isEmpty()) 1L else (promesas.maxOfOrNull { it.id } ?: 0L) + 1
        val newPromesa = promesa.copy(id = newId)
        promesas.add(newPromesa)
        savePromesas(promesas)
        return newId
    }
    
    fun updatePromesa(promesa: Promesa) {
        val promesas = getAllPromesas().toMutableList()
        val index = promesas.indexOfFirst { it.id == promesa.id }
        if (index != -1) {
            promesas[index] = promesa
            savePromesas(promesas)
        }
    }
    
    fun deletePromesa(promesa: Promesa) {
        val promesas = getAllPromesas().toMutableList()
        promesas.removeAll { it.id == promesa.id }
        savePromesas(promesas)
    }
    
    fun getPromesaById(id: Long): Promesa? {
        return getAllPromesas().find { it.id == id }
    }
    
    fun getPromesasByClienteId(clienteId: Long): List<Promesa> {
        return getAllPromesas().filter { it.clienteId == clienteId }
    }
    
    fun getTotalAdeudado(): Double {
        return getAllPromesas().sumOf { it.getTotalMonto() }
    }
    
    private fun savePromesas(promesas: List<Promesa>) {
        val json = gson.toJson(promesas)
        prefs.edit().putString("promesas", json).apply()
    }
}

