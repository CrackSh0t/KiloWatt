package com.example.kilowatt

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.kilowatt.data.InquilinoRepository
import com.example.kilowatt.data.LecturaRepository
import com.example.kilowatt.data.Submedidor
import com.example.kilowatt.databinding.ActivityLecturasBinding
import kotlinx.coroutines.launch

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LecturasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLecturasBinding

    private val viewModel: LecturasViewModel by viewModels()

    private var listaSubmedidores: List<Submedidor> = emptyList()
    private var spinnerAdapter: ArrayAdapter<String>? = null
    private val nombresSubmedidores = mutableListOf<String>()

    private var isUpdatingFieldsFromState = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLecturasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Registrador de Lecturas"

        setupSpinner()
        observarEstadoViewModel()

        binding.etMesPeriodo.addTextChangedListener {
            if (!isUpdatingFieldsFromState) {
                solicitarLecturaPrevia()
            }
        }

        binding.btnGuardarFactura.setOnClickListener {
            val mes = binding.etMesPeriodo.text.toString()
            val monto = binding.etMontoTotalSoles.text.toString()
            val kwh = binding.etKwhTotales.text.toString()
            viewModel.guardarFacturaGeneral(mes, monto, kwh)
        }

        binding.btnCalcularYGuardar.setOnClickListener {
            val mes = binding.etMesPeriodo.text.toString()
            val lecAnt = binding.etLecturaAnterior.text.toString()
            val lecAct = binding.etLecturaActual.text.toString()
            val pos = binding.spSubmedidores.selectedItemPosition
            val submedidor = if (pos in listaSubmedidores.indices) listaSubmedidores[pos] else null

            viewModel.calcularYGuardarLectura(mes, lecAnt, lecAct, submedidor)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun setupSpinner() {
        spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            nombresSubmedidores,
        )
        binding.spSubmedidores.adapter = spinnerAdapter

        binding.spSubmedidores.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isUpdatingFieldsFromState) {
                    solicitarLecturaPrevia()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun solicitarLecturaPrevia() {
        val mes = binding.etMesPeriodo.text.toString()
        val pos = binding.spSubmedidores.selectedItemPosition
        val submedidor = if (pos in listaSubmedidores.indices) listaSubmedidores[pos] else null
        viewModel.cargarLecturaPreviaSubmedidor(mes, submedidor)
    }

    private fun observarEstadoViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    actualizarUi(state)
                }
            }
        }
    }

    private fun actualizarUi(state: LecturasUiState) {
        binding.btnGuardarFactura.isEnabled = !state.estaGuardandoReciboBase
        binding.btnCalcularYGuardar.isEnabled = !state.estaGuardandoLectura

        // Actualizar Spinner
        if (state.submedidoresDisponibles != listaSubmedidores) {
            val posActual = binding.spSubmedidores.selectedItemPosition
            listaSubmedidores = state.submedidoresDisponibles
            val nuevosNombres = state.submedidoresDisponibles.map { it.nombreEspacio }

            nombresSubmedidores.clear()
            nombresSubmedidores.addAll(nuevosNombres)
            spinnerAdapter?.notifyDataSetChanged()

            if (posActual >= 0 && posActual < nuevosNombres.size) {
                binding.spSubmedidores.setSelection(posActual)
            }
        }

        // Actualizar lecturas en pantalla si cambió la lectura previa
        isUpdatingFieldsFromState = true
        val prev = state.ultimaLecturaSubmedidor
        if (prev != null) {
            binding.etLecturaAnterior.setText(prev.lecturaAnterior.toString())
            binding.etLecturaActual.setText(prev.lecturaActual.toString())
        }
        isUpdatingFieldsFromState = false

        // Notificaciones Toast/Snackbar
        state.mensajeNotificacion?.let { msj ->
            Toast.makeText(this, msj, if (state.esError) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()

            if (!state.esError) {
                binding.etLecturaAnterior.text?.clear()
                binding.etLecturaActual.text?.clear()
                binding.etLecturaAnterior.clearFocus()
                binding.etLecturaActual.clearFocus()
            }

            viewModel.consumirNotificacion()
        }
    }
}
