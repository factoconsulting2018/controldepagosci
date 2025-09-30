package com.checklist.app

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class CategoryManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("checklist_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    fun getAllCategories(): List<Category> {
        val json = prefs.getString("categories", "[]")
        val type = object : TypeToken<List<Category>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
    
    fun addCategory(category: Category): Long {
        val categories = getAllCategories().toMutableList()
        val newId = if (categories.isEmpty()) 1L else (categories.maxOfOrNull { it.id } ?: 0L) + 1
        val newCategory = category.copy(id = newId)
        categories.add(newCategory)
        saveCategories(categories)
        return newId
    }
    
    fun updateCategory(category: Category) {
        val categories = getAllCategories().toMutableList()
        val index = categories.indexOfFirst { it.id == category.id }
        if (index != -1) {
            categories[index] = category
            saveCategories(categories)
        }
    }
    
    fun deleteCategory(category: Category) {
        val categories = getAllCategories().toMutableList()
        categories.removeAll { it.id == category.id }
        saveCategories(categories)
    }
    
    fun getCategoryCount(): Int = getAllCategories().size
    
    private fun saveCategories(categories: List<Category>) {
        val json = gson.toJson(categories)
        prefs.edit().putString("categories", json).apply()
    }
}
