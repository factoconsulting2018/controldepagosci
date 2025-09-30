package com.checklist.app

data class ChecklistItem(
    val id: Long,
    val text: String,
    var isChecked: Boolean = false
)
