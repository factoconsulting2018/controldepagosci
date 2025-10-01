package com.checklist.app

data class PromesaPago(
    val titulo: String,
    val monto: Double,
    val fecha: Long
)

data class Promesa(
    val id: Long = 0,
    val clienteId: Long,
    val clienteNombre: String,
    val promesasPago: List<PromesaPago>,
    val fechaCreacion: Long = System.currentTimeMillis()
) {
    fun getTotalMonto(): Double {
        return promesasPago.sumOf { it.monto }
    }
}

