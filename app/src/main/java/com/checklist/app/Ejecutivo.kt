package com.checklist.app

data class Ejecutivo(
    val id: Long = 0,
    val number: String = "",
    val name: String,
    val color: String = "#FF4CAF50", // Color por defecto verde
    val createdAt: Long = System.currentTimeMillis()
)
