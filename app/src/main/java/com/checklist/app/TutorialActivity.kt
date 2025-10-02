package com.checklist.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.checklist.app.databinding.ActivityTutorialBinding

class TutorialActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityTutorialBinding
    private var currentStep = 0
    
    private val tutorialSteps = listOf(
        TutorialStep(
            title = "Bienvenido a la App Checklist",
            description = "Esta aplicación te ayuda a completar checklists de manera eficiente. Verás las preguntas organizadas en tarjetas con indicadores visuales de progreso.",
            imageRes = R.drawable.ic_question
        ),
        TutorialStep(
            title = "Indicador de Progreso Flotante",
            description = "En la esquina superior derecha verás un indicador flotante que muestra tu progreso en formato 'X/Y' (ejemplo: '3/10'). También incluye una barra de progreso visual que se llena conforme avanzas.",
            imageRes = R.drawable.ic_check_gray
        ),
        TutorialStep(
            title = "Lista de clientes",
            description = "Gestiona tus clientes en la lista principal. Puedes filtrarlos por ejecutivo usando los botones superiores o ver 'Todos'. Cada cliente aparece como una tarjeta con su estado y datos clave.",
            imageRes = R.drawable.ic_check_gray
        ),
        TutorialStep(
            title = "Agregar cliente",
            description = "Usa el botón flotante '+' y selecciona 'Agregar cliente' para crear uno nuevo. Completa el formulario, elige el ejecutivo (o 'Sin ejecutivo' para 'Todos') y se añadirá a la lista automáticamente.",
            imageRes = R.drawable.ic_check_green
        ),
        TutorialStep(
            title = "Estados: Pendiente / Pagado",
            description = "Toca una tarjeta para alternar el estado. El color y la barra de progreso se actualizan. El orden inicial (PENDIENTE/PAGADO o PAGADO/PENDIENTE) se configura en 'Configuración'.",
            imageRes = R.drawable.ic_reports
        ),
        TutorialStep(
            title = "Generar reporte PDF",
            description = "Pulsa el botón naranja 'Generar reporte' para crear un PDF con el resumen de tus clientes y estados. Completa el formulario y se guardará el archivo para compartirlo.",
            imageRes = R.drawable.ic_check_green
        ),
        TutorialStep(
            title = "Navegación y funciones",
            description = "Usa la barra inferior para acceder a Clientes, Ejecutivos, Reportes y Configuración. Desde Configuración puedes importar clientes, cambiar el orden de estados y activar el tutorial automático.",
            imageRes = R.drawable.ic_admin
        ),
        TutorialStep(
            title = "¡Listo para Usar!",
            description = "Ya conoces lo esencial: gestionar clientes, filtrar por ejecutivo, cambiar estados y generar reportes. Puedes reactivar este tutorial desde Configuración cuando quieras.",
            imageRes = R.drawable.ic_check_green
        )
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            supportActionBar?.hide()
            
            binding = ActivityTutorialBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            setupTutorial()
        } catch (e: Exception) {
            android.util.Log.e("TutorialActivity", "Error en onCreate", e)
            // Si hay error, cerrar el tutorial y volver a MainActivity
            finishTutorial()
        }
    }
    
    private fun setupTutorial() {
        binding.btnSkip.setOnClickListener {
            finishTutorial()
        }
        
        binding.btnNext.setOnClickListener {
            if (currentStep < tutorialSteps.size - 1) {
                currentStep++
                updateTutorialStep()
            } else {
                finishTutorial()
            }
        }
        
        updateTutorialStep()
    }
    
    private fun updateTutorialStep() {
        val step = tutorialSteps[currentStep]
        
        binding.tutorialTitle.text = step.title
        binding.tutorialDescription.text = step.description
        binding.tutorialImage.setImageResource(step.imageRes)
        
        updateProgressIndicators()
        updateButtonText()
    }
    
    private fun updateProgressIndicators() {
        val indicators = listOf(
            binding.indicator1,
            binding.indicator2,
            binding.indicator3,
            binding.indicator4,
            binding.indicator5,
            binding.indicator6,
            binding.indicator7,
            binding.indicator8
        )
        
        indicators.forEachIndexed { index, indicator ->
            if (index <= currentStep) {
                indicator.setBackgroundResource(R.drawable.indicator_active)
            } else {
                indicator.setBackgroundResource(R.drawable.indicator_inactive)
            }
        }
    }
    
    private fun updateButtonText() {
        if (currentStep == tutorialSteps.size - 1) {
            binding.btnNext.text = "Finalizar"
        } else {
            binding.btnNext.text = "Siguiente"
        }
    }
    
    private fun finishTutorial() {
        try {
            // Marcar que el tutorial ya se mostró
            val prefs = getSharedPreferences("checklist_prefs", MODE_PRIVATE)
            prefs.edit().putBoolean("tutorial_completed", true).apply()
            
            android.util.Log.d("TutorialActivity", "Tutorial completado, regresando a MainActivity")
            
            // Ir a MainActivity
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            android.util.Log.e("TutorialActivity", "Error al finalizar tutorial", e)
            // Si hay error, al menos cerrar la actividad
            finish()
        }
    }
}

data class TutorialStep(
    val title: String,
    val description: String,
    val imageRes: Int
)
