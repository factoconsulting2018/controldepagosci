package com.checklist.app

data class ReportInfo(
    val id: Long = 0,
    val name: String,
    val position: String,
    val supervisor: String,
    val comments: String,
    val filePath: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
