package com.checklist.app

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ReportManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("checklist_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveReport(reportInfo: ReportInfo): Long {
        val reports = getAllReports().toMutableList()
        val newId = getNextConsecutiveId()
        val newReport = reportInfo.copy(id = newId)
        reports.add(newReport)
        saveReports(reports)
        return newId
    }
    
    private fun getNextConsecutiveId(): Long {
        val lastId = prefs.getLong("last_report_id", 0L)
        val nextId = lastId + 1
        prefs.edit().putLong("last_report_id", nextId).apply()
        return nextId
    }

    fun getAllReports(): List<ReportInfo> {
        val json = prefs.getString("reports", "[]")
        val type = object : TypeToken<List<ReportInfo>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun getReportById(id: Long): ReportInfo? {
        return getAllReports().find { it.id == id }
    }

    fun deleteReport(reportInfo: ReportInfo) {
        val reports = getAllReports().toMutableList()
        reports.removeAll { it.id == reportInfo.id }
        saveReports(reports)
    }

    fun deleteReport(id: Long) {
        val reports = getAllReports().toMutableList()
        reports.removeAll { it.id == id }
        saveReports(reports)
    }

    private fun saveReports(reports: List<ReportInfo>) {
        val json = gson.toJson(reports)
        prefs.edit().putString("reports", json).apply()
    }
}
