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
            title = "Paso 1: Completar el Formulario",
            description = "El primer paso es completar todas las preguntas del checklist. Cada pregunta tiene un check box que cambia de gris a verde cuando la marcas como completada. Haz clic en cualquier parte de la tarjeta para cambiar el estado.",
            imageRes = R.drawable.ic_check_gray
        ),
        TutorialStep(
            title = "Marcar Preguntas",
            description = "Al hacer clic en una pregunta: el check cambia a verde, el estado muestra 'Completado', y toda la tarjeta cambia de gris a verde. El indicador flotante se actualiza automáticamente mostrando tu progreso.",
            imageRes = R.drawable.ic_check_green
        ),
        TutorialStep(
            title = "Paso 2: Generar Informe",
            description = "Una vez completadas todas las preguntas, haz clic en el botón naranja 'Generar Informe' que está junto al indicador de progreso. Esto abrirá directamente el formulario para crear tu reporte PDF.",
            imageRes = R.drawable.ic_reports
        ),
        TutorialStep(
            title = "Formulario de Reporte",
            description = "Completa el formulario con tu nombre, puesto, jefe directo y comentarios opcionales. Al finalizar, se generará automáticamente un PDF con toda la información del checklist completado.",
            imageRes = R.drawable.ic_check_green
        ),
        TutorialStep(
            title = "Navegación y Funciones",
            description = "Usa la barra de navegación inferior para acceder a diferentes secciones: Preguntas, Categorías, Reportes y Configuración. El modo administrador te permite cargar preguntas precargadas y configurar opciones avanzadas.",
            imageRes = R.drawable.ic_admin
        ),
        TutorialStep(
            title = "¡Listo para Usar!",
            description = "Ya conoces cómo funciona la aplicación. Recuerda: Paso 1 (Completar Formulario) → Paso 2 (Generar Informe). El indicador flotante te mostrará siempre tu progreso actual.",
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
