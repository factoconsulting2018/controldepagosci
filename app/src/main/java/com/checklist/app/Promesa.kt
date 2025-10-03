package com.checklist.app

data class PromesaPago(
    val titulo: String,
    val monto: Double,
    val fecha: Long,
    val imageData: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PromesaPago

        if (titulo != other.titulo) return false
        if (monto != other.monto) return false
        if (fecha != other.fecha) return false
        if (!imageData.contentEquals(other.imageData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = titulo.hashCode()
        result = 31 * result + monto.hashCode()
        result = 31 * result + fecha.hashCode()
        result = 31 * result + (imageData?.contentHashCode() ?: 0)
        return result
    }
}

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

