package com.checklist.app

data class ReportInfo(
    val id: Long = 0,
    val ejecutivo: String,
    val comments: String,
    val filePath: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
