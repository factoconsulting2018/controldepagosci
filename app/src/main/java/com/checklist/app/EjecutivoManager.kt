package com.checklist.app

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class EjecutivoManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ejecutivos_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val ejecutivosKey = "ejecutivos_list"

    fun getAllEjecutivos(): List<Ejecutivo> {
        val json = prefs.getString(ejecutivosKey, null)
        val ejecutivos: List<Ejecutivo> = if (json != null) {
            try {
                val type = object : TypeToken<List<Ejecutivo>>() {}.type
                gson.fromJson<List<Ejecutivo>>(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            // Crear solo el ejecutivo "Todos" con ID 0
            createTodosEjecutivo()
        }
        
        // Asegurar que siempre existe el ejecutivo "Todos" con ID 0
        val todosEjecutivo = ejecutivos.find { it.id == 0L }
        val ejecutivosConTodos = if (todosEjecutivo == null) {
            val todos = Ejecutivo(
                id = 0,
                name = "Todos",
                color = "#FF9E9E9E" // Gris
            )
            listOf(todos) + ejecutivos
        } else {
            ejecutivos
        }
        
        android.util.Log.d("EjecutivoManager", "getAllEjecutivos: ${ejecutivosConTodos.size} ejecutivos encontrados")
        ejecutivosConTodos.forEach { ejecutivo ->
            android.util.Log.d("EjecutivoManager", "  Ejecutivo ID=${ejecutivo.id}, Nombre='${ejecutivo.name}', Color='${ejecutivo.color}'")
        }
        
        return ejecutivosConTodos
    }

    fun getEjecutivoById(id: Long): Ejecutivo? {
        return getAllEjecutivos().find { it.id == id }
    }

    fun addEjecutivo(ejecutivo: Ejecutivo): Long {
        val ejecutivos = getAllEjecutivos().toMutableList()
        // Filtrar el ejecutivo "Todos" (ID 0) para el cálculo del siguiente ID
        val ejecutivosSinTodos = ejecutivos.filter { it.id != 0L }
        val newId = if (ejecutivosSinTodos.isEmpty()) 1L else (ejecutivosSinTodos.maxOfOrNull { it.id } ?: 0L) + 1
        val newEjecutivo = ejecutivo.copy(id = newId)
        ejecutivos.add(newEjecutivo)
        saveEjecutivos(ejecutivos)
        
        android.util.Log.d("EjecutivoManager", "addEjecutivo: Ejecutivo '${ejecutivo.name}' creado con ID=$newId")
        android.util.Log.d("EjecutivoManager", "addEjecutivo: Total de ejecutivos: ${ejecutivos.size}")
        
        return newId
    }

    fun updateEjecutivo(ejecutivo: Ejecutivo): Boolean {
        val ejecutivos = getAllEjecutivos().toMutableList()
        val index = ejecutivos.indexOfFirst { it.id == ejecutivo.id }
        return if (index != -1) {
            ejecutivos[index] = ejecutivo
            saveEjecutivos(ejecutivos)
            true
        } else {
            false
        }
    }

    fun deleteEjecutivo(id: Long): Boolean {
        val ejecutivos = getAllEjecutivos().toMutableList()
        val removed = ejecutivos.removeAll { it.id == id }
        if (removed) {
            saveEjecutivos(ejecutivos)
        }
        return removed
    }

    fun searchEjecutivos(query: String): List<Ejecutivo> {
        val ejecutivos = getAllEjecutivos()
        return if (query.isEmpty()) {
            ejecutivos
        } else {
            ejecutivos.filter { 
                it.name.contains(query, ignoreCase = true) ||
                it.number.contains(query, ignoreCase = true)
            }
        }
    }

    private fun saveEjecutivos(ejecutivos: List<Ejecutivo>) {
        val json = gson.toJson(ejecutivos)
        prefs.edit().putString(ejecutivosKey, json).apply()
    }
    
    fun deleteAllEjecutivos() {
        prefs.edit().remove(ejecutivosKey).apply()
        android.util.Log.d("EjecutivoManager", "deleteAllEjecutivos: Todos los ejecutivos eliminados")
    }
    
    private fun createTodosEjecutivo(): List<Ejecutivo> {
        val todosEjecutivo = Ejecutivo(
            id = 0,
            name = "Todos",
            color = "#FF9E9E9E" // Gris
        )
        saveEjecutivos(listOf(todosEjecutivo))
        android.util.Log.d("EjecutivoManager", "createTodosEjecutivo: Ejecutivo 'Todos' creado con ID=0")
        return listOf(todosEjecutivo)
    }

}
