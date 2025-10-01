package com.checklist.app

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class IssueManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("issues_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    fun getAllIssues(): List<Issue> {
        val json = prefs.getString("issues", "[]")
        val type = object : TypeToken<List<Issue>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
    
    fun addIssue(issue: Issue): Long {
        val issues = getAllIssues().toMutableList()
        val newId = if (issues.isEmpty()) 1L else (issues.maxOfOrNull { it.id } ?: 0L) + 1
        val newIssue = issue.copy(id = newId)
        issues.add(newIssue)
        saveIssues(issues)
        return newId
    }
    
    fun updateIssue(issue: Issue) {
        val issues = getAllIssues().toMutableList()
        val index = issues.indexOfFirst { it.id == issue.id }
        if (index != -1) {
            issues[index] = issue
            saveIssues(issues)
        }
    }
    
    fun updateEstado(issueId: Long, nuevoEstado: EstadoIssue) {
        val issues = getAllIssues().toMutableList()
        val index = issues.indexOfFirst { it.id == issueId }
        if (index != -1) {
            issues[index] = issues[index].copy(estado = nuevoEstado)
            saveIssues(issues)
        }
    }
    
    fun deleteIssuesByIds(issueIds: List<Long>) {
        val issues = getAllIssues().toMutableList()
        issues.removeAll { it.id in issueIds }
        saveIssues(issues)
    }
    
    fun getIssuesByEstado(estado: EstadoIssue): List<Issue> {
        return getAllIssues().filter { it.estado == estado }
    }
    
    fun deleteIssue(issue: Issue) {
        val issues = getAllIssues().toMutableList()
        issues.removeAll { it.id == issue.id }
        saveIssues(issues)
    }
    
    fun getIssueById(id: Long): Issue? {
        return getAllIssues().find { it.id == id }
    }
    
    fun getIssuesByClienteId(clienteId: Long): List<Issue> {
        return getAllIssues().filter { it.clienteId == clienteId }
    }
    
    fun getTotalIssuesCount(): Int {
        return getAllIssues().sumOf { it.getTotalIssues() }
    }
    
    private fun saveIssues(issues: List<Issue>) {
        val json = gson.toJson(issues)
        prefs.edit().putString("issues", json).apply()
    }
}

