package com.checklist.app

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class QuestionManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("checklist_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    fun getAllQuestions(): List<Question> {
        val json = prefs.getString("questions", "[]")
        val type = object : TypeToken<List<Question>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
    
    fun getQuestionsByEjecutivo(ejecutivoId: Long): List<Question> {
        return getAllQuestions().filter { it.ejecutivoId == ejecutivoId }
    }
    
    fun addQuestion(question: Question): Long {
        val questions = getAllQuestions().toMutableList()
        val newId = if (questions.isEmpty()) 1L else (questions.maxOfOrNull { it.id } ?: 0L) + 1
        val newQuestion = question.copy(id = newId)
        
        // Insertar en la posición correcta y reordenar
        insertQuestionAndReorder(questions, newQuestion)
        saveQuestions(questions)
        return newId
    }
    
    fun addQuestionAtPosition(question: Question, position: Int): Long {
        val questions = getAllQuestions().toMutableList()
        val newId = if (questions.isEmpty()) 1L else (questions.maxOfOrNull { it.id } ?: 0L) + 1
        val newQuestion = question.copy(id = newId, position = position)
        
        // Insertar en la posición específica y reordenar
        insertQuestionAndReorder(questions, newQuestion)
        saveQuestions(questions)
        return newId
    }
    
    private fun insertQuestionAndReorder(questions: MutableList<Question>, newQuestion: Question) {
        // Insertar la nueva pregunta en la posición correcta
        val insertIndex = questions.indexOfFirst { it.position >= newQuestion.position }
        if (insertIndex == -1) {
            questions.add(newQuestion)
        } else {
            questions.add(insertIndex, newQuestion)
        }
        
        // Reordenar todas las preguntas para mantener la secuencia
        reorderQuestions(questions)
    }
    
    private fun reorderQuestions(questions: MutableList<Question>) {
        // Ordenar por posición
        questions.sortBy { it.position }
        
        // Renumerar posiciones para mantener secuencia consecutiva
        for (i in questions.indices) {
            questions[i] = questions[i].copy(position = i + 1)
        }
    }
    
    fun updateQuestion(question: Question) {
        val questions = getAllQuestions().toMutableList()
        val index = questions.indexOfFirst { it.id == question.id }
        if (index != -1) {
            questions[index] = question
            // Reordenar después de actualizar
            reorderQuestions(questions)
            saveQuestions(questions)
        }
    }
    
    fun deleteQuestion(question: Question) {
        val questions = getAllQuestions().toMutableList()
        questions.removeAll { it.id == question.id }
        // Reordenar después de eliminar
        reorderQuestions(questions)
        saveQuestions(questions)
    }
    
    fun getQuestionCount(): Int = getAllQuestions().size
    
    fun getNextPosition(): Int {
        val questions = getAllQuestions()
        return if (questions.isEmpty()) 1 else (questions.maxOfOrNull { it.position } ?: 0) + 1
    }
    
    fun getQuestionsOrderedByPosition(): List<Question> {
        return getAllQuestions().sortedBy { it.position }
    }
    
    fun clearAllCompletedQuestions() {
        val questions = getAllQuestions().toMutableList()
        val updatedQuestions = questions.map { question ->
            if (question.isCompleted) {
                question.copy(isCompleted = false)
            } else {
                question
            }
        }
        saveQuestions(updatedQuestions)
    }
    
    private fun saveQuestions(questions: List<Question>) {
        val json = gson.toJson(questions)
        prefs.edit().putString("questions", json).apply()
    }
}
