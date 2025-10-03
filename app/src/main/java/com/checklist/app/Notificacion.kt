package com.checklist.app

data class Notificacion(
    val id: Long = 0,
    val clienteId: Long,
    val clienteNombre: String,
    val clienteTelefono: String,
    val montoPendiente: Double,
    val mensaje: String,
    val fechaCreacion: Long = System.currentTimeMillis(),
    val enviada: Boolean = false
)

