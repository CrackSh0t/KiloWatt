package com.example.kilowatt

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.kilowatt.data.FacturaGeneral
import com.example.kilowatt.data.InquilinoRepository
import com.example.kilowatt.data.Lectura
import com.example.kilowatt.data.LecturaRepository
import com.example.kilowatt.data.Submedidor
import com.example.kilowatt.databinding.ActivityLecturasBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

class LecturasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLecturasBinding
    private val lecturaRepository = LecturaRepository()
    private val inquilinoRepository = InquilinoRepository()

    private var listaSubmedidores: List<Submedidor> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLecturasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Registrador de Lecturas"

        cargarSubmedidoresEnSpinner()

        binding.spSubmedidores.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                cargarLecturaPreviaDelSubmedidor()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.btnGuardarFactura.setOnClickListener {
            guardarFacturaGeneral()
        }

        binding.btnCalcularYGuardar.setOnClickListener {
            calcularYGuardarLectura()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun cargarSubmedidoresEnSpinner() {
        lifecycleScope.launch {
            inquilinoRepository.obtenerSubmedidoresFlow().collect { lista ->
                listaSubmedidores = lista
                val nombres = lista.map { it.nombreEspacio }
                val spinnerAdapter = ArrayAdapter(
                    this@LecturasActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    nombres,
                )
                binding.spSubmedidores.adapter = spinnerAdapter
            }
        }
    }

    private fun cargarLecturaPreviaDelSubmedidor() {
        val mes = binding.etMesPeriodo.text.toString().trim()
        val pos = binding.spSubmedidores.selectedItemPosition
        if (mes.isEmpty() || pos < 0 || pos >= listaSubmedidores.size) return

        val submedidor = listaSubmedidores[pos]

        lifecycleScope.launch {
            val lecturas = lecturaRepository.obtenerLecturasPorMesFlow(mes).first()
            val lecturaPrev = lecturas.find { it.idSubmedidor == submedidor.idSubmedidor }

            if (lecturaPrev != null) {
                binding.etLecturaAnterior.setText(lecturaPrev.lecturaAnterior.toString())
                binding.etLecturaActual.setText(lecturaPrev.lecturaActual.toString())
            } else {
                binding.etLecturaAnterior.setText("")
                binding.etLecturaActual.setText("")
            }
        }
    }

    private fun guardarFacturaGeneral() {
        val mes = binding.etMesPeriodo.text.toString().trim()
        val montoStr = binding.etMontoTotalSoles.text.toString().trim()
        val kwhStr = binding.etKwhTotales.text.toString().trim()

        if (mes.isEmpty() || montoStr.isEmpty() || kwhStr.isEmpty()) {
            Toast.makeText(this, "Completa los datos del recibo base", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnGuardarFactura.isEnabled = false

        val factura = FacturaGeneral(
            idFactura = mes,
            mesPeriodo = mes,
            montoTotalSoles = montoStr.toDoubleOrNull() ?: 0.0,
            kwhTotalesRecibo = kwhStr.toDoubleOrNull() ?: 0.0,
        )

        lifecycleScope.launch {
            try {
                val (exito, error) = lecturaRepository.guardarFactura(factura)
                if (exito) {
                    Toast.makeText(this@LecturasActivity, "¡Recibo Base guardado para $mes!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@LecturasActivity, "Error al guardar recibo: $error", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LecturasActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                binding.btnGuardarFactura.isEnabled = true
            }
        }
    }

    private fun calcularYGuardarLectura() {
        val mes = binding.etMesPeriodo.text.toString().trim()
        val lecAntStr = binding.etLecturaAnterior.text.toString().trim()
        val lecActStr = binding.etLecturaActual.text.toString().trim()

        if (mes.isEmpty() || lecAntStr.isEmpty() || lecActStr.isEmpty() || listaSubmedidores.isEmpty()) {
            Toast.makeText(this, "Ingresa las lecturas e indica el mes exacto", Toast.LENGTH_SHORT).show()
            return
        }

        val posicionSeleccionada = binding.spSubmedidores.selectedItemPosition
        if (posicionSeleccionada < 0 || posicionSeleccionada >= listaSubmedidores.size) {
            Toast.makeText(this, "Selecciona un medidor válido", Toast.LENGTH_SHORT).show()
            return
        }
        val submedidorSeleccionado = listaSubmedidores[posicionSeleccionada]

        val lecAnt = lecAntStr.toDoubleOrNull() ?: 0.0
        val lecAct = lecActStr.toDoubleOrNull() ?: 0.0

        if (lecAct < lecAnt) {
            Toast.makeText(this, "La lectura actual no puede ser menor a la anterior", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnCalcularYGuardar.isEnabled = false

        lifecycleScope.launch {
            try {
                val facturas = lecturaRepository.obtenerFacturasFlow().first()
                val facturaMes = facturas.find { it.mesPeriodo.trim().equals(mes, ignoreCase = true) }

                if (facturaMes == null) {
                    Toast.makeText(this@LecturasActivity, "Primero debes guardar el Recibo Base de este mes ($mes)", Toast.LENGTH_LONG).show()
                    return@launch
                }

                val lecturasDelMes = lecturaRepository.obtenerLecturasPorMesFlow(mes).first()
                val lecturaExistente = lecturasDelMes.find { it.idSubmedidor == submedidorSeleccionado.idSubmedidor }

                val consumoKwh = lecAct - lecAnt
                val precioPorKwh = if (facturaMes.kwhTotalesRecibo > 0) facturaMes.montoTotalSoles / facturaMes.kwhTotalesRecibo else 0.0
                val montoPagar = consumoKwh * precioPorKwh

                val nuevaLectura = Lectura(
                    idLectura = lecturaExistente?.idLectura.takeIf { !it.isNullOrEmpty() } ?: UUID.randomUUID().toString(),
                    idSubmedidor = submedidorSeleccionado.idSubmedidor,
                    mesPeriodo = mes,
                    lecturaAnterior = lecAnt,
                    lecturaActual = lecAct,
                    consumoKwh = consumoKwh,
                    montoPagarSoles = montoPagar,
                )

                val (exito, error) = lecturaRepository.guardarLectura(nuevaLectura)
                if (exito) {
                    val msj = "Guardado: %.1f kWh | Total: S/ %.2f".format(consumoKwh, montoPagar)
                    Toast.makeText(this@LecturasActivity, msj, Toast.LENGTH_LONG).show()

                    binding.etLecturaAnterior.text?.clear()
                    binding.etLecturaActual.text?.clear()
                    binding.etLecturaAnterior.clearFocus()
                    binding.etLecturaActual.clearFocus()
                } else {
                    Toast.makeText(this@LecturasActivity, "Error al guardar lectura: $error", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LecturasActivity, "Error al procesar lectura: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                binding.btnCalcularYGuardar.isEnabled = true
            }
        }
    }
}