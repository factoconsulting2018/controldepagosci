package com.checklist.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.checklist.app.databinding.DialogImportResultBinding
import com.google.android.material.tabs.TabLayoutMediator

class ImportResultDialogActivity : AppCompatActivity() {
    
    private lateinit var binding: DialogImportResultBinding
    private lateinit var importResult: ImportResult
    
    companion object {
        const val EXTRA_IMPORT_RESULT = "import_result"
        
        fun start(activity: Activity, importResult: ImportResult) {
            val intent = Intent(activity, ImportResultDialogActivity::class.java)
            intent.putExtra(EXTRA_IMPORT_RESULT, importResult)
            activity.startActivity(intent)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogImportResultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Obtener el resultado de importación
        importResult = intent.getParcelableExtra(EXTRA_IMPORT_RESULT) ?: return
        
        setupUI()
        setupViewPager()
        setupClickListeners()
    }
    
    private fun setupUI() {
        // Configurar icono según el resultado
        val iconRes = if (importResult.success) {
            android.R.drawable.ic_dialog_info
        } else {
            android.R.drawable.ic_dialog_alert
        }
        binding.icon.setImageResource(iconRes)
        
        // Configurar título
        binding.title.text = if (importResult.success) {
            "Importación Completada"
        } else {
            "Error en Importación"
        }
        
        // Configurar contadores
        binding.newClientsCount.text = importResult.clientesNuevos.toString()
        binding.updatedClientsCount.text = importResult.clientesActualizados.toString()
        binding.duplicatesCount.text = importResult.clientesDuplicados.toString()
        
        // Ocultar tarjetas si no hay datos
        if (importResult.clientesNuevos == 0) {
            binding.newClientsCard.visibility = View.GONE
        }
        if (importResult.clientesActualizados == 0) {
            binding.updatedClientsCard.visibility = View.GONE
        }
        if (importResult.clientesDuplicados == 0) {
            binding.duplicatesCard.visibility = View.GONE
        }
    }
    
    private fun setupViewPager() {
        val adapter = ImportResultPagerAdapter(this, importResult)
        binding.viewPager.adapter = adapter
        
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Detalles"
                1 -> "Duplicados"
                2 -> "Errores"
                else -> ""
            }
        }.attach()
    }
    
    private fun setupClickListeners() {
        binding.closeButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        
        binding.closeDialogButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        
        binding.viewClientsButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("isAdminMode", true)
            startActivity(intent)
            finish()
        }
    }
}

class ImportResultPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val importResult: ImportResult
) : FragmentStateAdapter(fragmentActivity) {
    
    override fun getItemCount(): Int = 3
    
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ImportDetailsFragment.newInstance(importResult.errorMessage ?: "")
            1 -> ImportDuplicatesFragment.newInstance(importResult.duplicadosDetallados)
            2 -> ImportErrorsFragment.newInstance(importResult.erroresDetallados)
            else -> ImportDetailsFragment.newInstance("")
        }
    }
}

class ImportDetailsFragment : Fragment() {
    
    companion object {
        private const val ARG_DETAILS = "details"
        
        fun newInstance(details: String): ImportDetailsFragment {
            val fragment = ImportDetailsFragment()
            val args = Bundle()
            args.putString(ARG_DETAILS, details)
            fragment.arguments = args
            return fragment
        }
    }
    
    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = android.widget.ScrollView(requireContext())
        val textView = android.widget.TextView(requireContext())
        
        val details = arguments?.getString(ARG_DETAILS) ?: ""
        textView.text = details
        textView.textSize = 14f
        textView.setPadding(16, 16, 16, 16)
        textView.setTextColor(android.graphics.Color.BLACK)
        
        view.addView(textView)
        return view
    }
}

class ImportDuplicatesFragment : Fragment() {
    
    companion object {
        private const val ARG_DUPLICATES = "duplicates"
        
        fun newInstance(duplicates: List<String>): ImportDuplicatesFragment {
            val fragment = ImportDuplicatesFragment()
            val args = Bundle()
            args.putStringArray(ARG_DUPLICATES, duplicates.toTypedArray())
            fragment.arguments = args
            return fragment
        }
    }
    
    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = android.widget.ScrollView(requireContext())
        val textView = android.widget.TextView(requireContext())
        
        val duplicates = arguments?.getStringArray(ARG_DUPLICATES) ?: emptyArray()
        val text = if (duplicates.isEmpty()) {
            "No se encontraron duplicados."
        } else {
            duplicates.joinToString("\n\n")
        }
        
        textView.text = text
        textView.textSize = 14f
        textView.setPadding(16, 16, 16, 16)
        textView.setTextColor(android.graphics.Color.BLACK)
        
        view.addView(textView)
        return view
    }
}

class ImportErrorsFragment : Fragment() {
    
    companion object {
        private const val ARG_ERRORS = "errors"
        
        fun newInstance(errors: List<String>): ImportErrorsFragment {
            val fragment = ImportErrorsFragment()
            val args = Bundle()
            args.putStringArray(ARG_ERRORS, errors.toTypedArray())
            fragment.arguments = args
            return fragment
        }
    }
    
    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = android.widget.ScrollView(requireContext())
        val textView = android.widget.TextView(requireContext())
        
        val errors = arguments?.getStringArray(ARG_ERRORS) ?: emptyArray()
        val text = if (errors.isEmpty()) {
            "No se encontraron errores."
        } else {
            errors.joinToString("\n\n")
        }
        
        textView.text = text
        textView.textSize = 14f
        textView.setPadding(16, 16, 16, 16)
        textView.setTextColor(android.graphics.Color.BLACK)
        
        view.addView(textView)
        return view
    }
}
