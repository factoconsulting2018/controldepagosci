package com.checklist.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.checklist.app.databinding.ItemQuestionBinding

class QuestionsAdapter(
    private val onEditClick: (Question) -> Unit,
    private val onDeleteClick: (Question) -> Unit,
    private val onQuestionClick: (Question) -> Unit,
    private val onStatusToggle: (Question) -> Unit,
    private val isAdminMode: () -> Boolean,
    private val getEjecutivoName: (Long) -> String,
    private val getEjecutivoColor: (Long) -> String,
    private val getClienteInfo: (Long?) -> String,
    private val getClientePhone: (Long?) -> String
) : ListAdapter<Question, QuestionsAdapter.QuestionViewHolder>(QuestionDiffCallback()) {

    class QuestionViewHolder(private val binding: ItemQuestionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            question: Question,
            onEditClick: (Question) -> Unit,
            onDeleteClick: (Question) -> Unit,
            onQuestionClick: (Question) -> Unit,
            onStatusToggle: (Question) -> Unit,
            isAdminMode: Boolean,
            ejecutivoName: String,
            ejecutivoColor: String,
            clienteInfo: String,
            clientePhone: String
        ) {
            binding.questionTitle.text = question.title
            binding.questionCategory.text = ejecutivoName
            // Mostrar ID del cliente en lugar del número de pregunta
            val clienteId = question.clienteId ?: 0L
            binding.questionPosition.text = "#$clienteId"
            
            // Mostrar información del cliente (cédula) como subtítulo
            binding.questionSubtitle.text = if (question.subtitle.isNotEmpty()) "Cédula: ${question.subtitle}" else "Sin cédula"
            
            // Mostrar teléfono del cliente en el cuadro azul
            binding.questionPhone.text = if (clientePhone.isNotEmpty()) clientePhone else "Sin teléfono"
            
            // Configurar color del ejecutivo
            try {
                val color = android.graphics.Color.parseColor(ejecutivoColor)
                binding.questionCategory.setBackgroundColor(color)
            } catch (e: Exception) {
                // Si hay error con el color, usar color por defecto
                binding.questionCategory.setBackgroundColor(android.graphics.Color.parseColor("#FF4CAF50"))
            }
            
            // Configurar estado de completado según la lógica PAGADO/PENDIENTE
            android.util.Log.d("QuestionsAdapter", "bind: Configurando estado para pregunta ${question.id}: ${question.title}, isCompleted: ${question.isCompleted}")
            if (question.isCompleted) {
                // PAGADO = Verde con check activado
                binding.root.setBackgroundResource(R.drawable.item_background_completed)
                binding.checkBox.setImageResource(R.drawable.ic_check_green)
                binding.statusText.text = "Pagado"
                binding.statusText.setBackgroundResource(R.drawable.status_completed_background)
                android.util.Log.d("QuestionsAdapter", "bind: Aplicando estilo PAGADO - Fondo verde, check verde")
            } else {
                // PENDIENTE = Gris sin check
                binding.root.setBackgroundResource(R.drawable.item_background_pending)
                binding.checkBox.setImageResource(R.drawable.ic_check_gray)
                binding.statusText.text = "Pendiente"
                binding.statusText.setBackgroundResource(R.drawable.status_pending_background)
                android.util.Log.d("QuestionsAdapter", "bind: Aplicando estilo PENDIENTE - Fondo gris, check gris")
            }
            
            // Configurar visibilidad de botones según modo admin
            val buttonVisibility = if (isAdminMode) android.view.View.VISIBLE else android.view.View.GONE
            binding.actionButtons.visibility = buttonVisibility
            
            // Configurar clic en toda la tarjeta para cambiar estado
            binding.root.setOnClickListener { 
                android.util.Log.d("QuestionsAdapter", "onClick: Card clickeado para pregunta ${question.id}, estado actual: ${question.isCompleted}")
                onStatusToggle(question) 
            }
            
            // Configurar clic en botones de acción
            binding.editButton.setOnClickListener { onEditClick(question) }
            binding.deleteButton.setOnClickListener { onDeleteClick(question) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        android.util.Log.d("QuestionsAdapter", "onCreateViewHolder llamado")
        val binding = ItemQuestionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return QuestionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        val question = getItem(position)
        android.util.Log.d("QuestionsAdapter", "Binding question ${question.id}: ${question.title}")
        val ejecutivoName = getEjecutivoName(question.ejecutivoId)
        val ejecutivoColor = getEjecutivoColor(question.ejecutivoId)
        val clienteInfo = getClienteInfo(question.clienteId)
        val clientePhone = getClientePhone(question.clienteId)
        android.util.Log.d("QuestionsAdapter", "Cliente ID: ${question.clienteId}, Teléfono: '$clientePhone'")
        holder.bind(question, onEditClick, onDeleteClick, onQuestionClick, onStatusToggle, isAdminMode(), ejecutivoName, ejecutivoColor, clienteInfo, clientePhone)
    }

    class QuestionDiffCallback : DiffUtil.ItemCallback<Question>() {
        override fun areItemsTheSame(oldItem: Question, newItem: Question): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Question, newItem: Question): Boolean {
            return oldItem == newItem
        }
    }
}
