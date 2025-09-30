package com.checklist.app

data class Question(
    val id: Long = 0,
    val title: String, // Ahora será el nombre del cliente
    val subtitle: String = "", // Ahora será la cédula del cliente
    val ejecutivoId: Long, // Cambiado de categoryId a ejecutivoId
    val position: Int = 0,
    val isCompleted: Boolean = false,
    val clienteId: Long? = null, // Referencia al cliente asociado
    val createdAt: Long = System.currentTimeMillis()
)
