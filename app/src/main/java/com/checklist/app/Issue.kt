package com.checklist.app

enum class EstadoIssue {
    PENDIENTE,
    EN_PROCESO,
    FINALIZADO
}

data class IssueItem(
    val titulo: String,
    val mensaje: String,
    val fechaIssue: Long
)

data class Issue(
    val id: Long = 0,
    val clienteId: Long,
    val clienteNombre: String,
    val issues: List<IssueItem>,
    val fechaCreacion: Long = System.currentTimeMillis(),
    val estado: EstadoIssue = EstadoIssue.PENDIENTE
) {
    fun getTotalIssues(): Int {
        return issues.size
    }
    
    fun getEstadoTexto(): String {
        return when (estado) {
            EstadoIssue.PENDIENTE -> "Pendiente"
            EstadoIssue.EN_PROCESO -> "En Proceso"
            EstadoIssue.FINALIZADO -> "Finalizado"
        }
    }
}

