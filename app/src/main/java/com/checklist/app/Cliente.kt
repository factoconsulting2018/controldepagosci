package com.checklist.app

data class Cliente(
    val id: Long = 0,
    val nombre: String,
    val cedula: String,
    val tipoPersona: String, // "Físico" o "Jurídico"
    val representante: String = "",
    val telefono: String = "",
    val ciFc: String = "", // CI-FC
    val ejecutivo: String = "",
    val patentado: Boolean = false,
    val pendientePago: Boolean = false,
    val tipoRegimen: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
