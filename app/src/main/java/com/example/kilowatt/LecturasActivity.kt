package com.example.kilowatt

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.kilowatt.data.AppDatabase
import com.example.kilowatt.data.FacturaGeneral
import com.example.kilowatt.data.Lectura
import com.example.kilowatt.data.Submedidor
import com.example.kilowatt.databinding.ActivityLecturasBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LecturasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLecturasBinding
    private lateinit var database: AppDatabase
    private var listaSubmedidores: List<Submedidor> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLecturasBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Mostrar flecha de regreso en la barra superior
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Registrador de Lecturas"

        database = AppDatabase.getDatabase(this)

        database = AppDatabase.getDatabase(this)

        cargarSubmedidoresEnSpinner()

        binding.btnGuardarFactura.setOnClickListener {
            guardarFacturaGeneral()
        }

        binding.btnCalcularYGuardar.setOnClickListener {
            calcularYGuardarLectura()
        }

        binding.btnVerResumen.setOnClickListener {
            val intent = Intent(this, ResumenCobrosActivity::class.java)
            startActivity(intent)
        }
    }
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    private fun cargarSubmedidoresEnSpinner() {
        lifecycleScope.launch {
            database.submedidorDao().obtenerTodosLosSubmedidores().collect { lista ->
                listaSubmedidores = lista
                val nombres = lista.map { it.nombreEspacio }
                val spinnerAdapter = ArrayAdapter(
                    this@LecturasActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    nombres
                )
                binding.spSubmedidores.adapter = spinnerAdapter
            }
        }
    }

    private fun guardarFacturaGeneral() {
        val mes = binding.etMesPeriodo.text.toString().trim()
        val montoStr = binding.etMontoTotalSoles.text.toString()
        val kwhStr = binding.etKwhTotales.text.toString()

        if (mes.isEmpty() || montoStr.isEmpty() || kwhStr.isEmpty()) {
            Toast.makeText(this, "Completa los datos del recibo base", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val factura = FacturaGeneral(
                mesPeriodo = mes,
                montoTotalSoles = montoStr.toDouble(),
                kwhTotalesRecibo = kwhStr.toDouble()
            )
            database.facturaGeneralDao().insertarFactura(factura)
            Toast.makeText(this@LecturasActivity, "¡Recibo Base Guardado!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun calcularYGuardarLectura() {
        val mes = binding.etMesPeriodo.text.toString().trim()
        val lecAntStr = binding.etLecturaAnterior.text.toString()
        val lecActStr = binding.etLecturaActual.text.toString()

        if (mes.isEmpty() || lecAntStr.isEmpty() || lecActStr.isEmpty() || listaSubmedidores.isEmpty()) {
            Toast.makeText(this, "Ingresa las lecturas y asegúrate de tener un mes ingresado", Toast.LENGTH_SHORT).show()
            return
        }

        val posicionSeleccionada = binding.spSubmedidores.selectedItemPosition
        val submedidorSeleccionado = listaSubmedidores[posicionSeleccionada]

        val lecAnt = lecAntStr.toDouble()
        val lecAct = lecActStr.toDouble()

        if (lecAct < lecAnt) {
            Toast.makeText(this, "La lectura actual no puede ser menor a la anterior", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            // 1. Buscamos la factura general del mes
            val facturaMes = database.facturaGeneralDao().obtenerFacturaPorMes(mes)

            if (facturaMes == null) {
                Toast.makeText(this@LecturasActivity, "Primero debes guardar el Recibo Base de este mes", Toast.LENGTH_LONG).show()
                return@launch
            }

            // 2. Buscamos si ya existe una lectura para este submedidor en este mes
            val lecturasDelMes = database.lecturaDao().obtenerLecturasPorMes(mes).first()
            val lecturaExistente = lecturasDelMes.find { it.idSubmedidor == submedidorSeleccionado.idSubmedidor }

            // 3. Fórmulas KiloWatt
            val consumoKwh = lecAct - lecAnt
            val precioPorKwh = facturaMes.montoTotalSoles / facturaMes.kwhTotalesRecibo
            val montoPagar = consumoKwh * precioPorKwh

            // 4. Objeto Lectura (Si ya existía, usamos su idLectura para SOBRESCRIBIR)
            val nuevaLectura = Lectura(
                idLectura = lecturaExistente?.idLectura ?: 0, // 👈 Conserva el ID si ya existe
                idSubmedidor = submedidorSeleccionado.idSubmedidor,
                mesPeriodo = mes,
                lecturaAnterior = lecAnt,
                lecturaActual = lecAct,
                consumoKwh = consumoKwh,
                montoPagarSoles = montoPagar
            )

            database.lecturaDao().insertarLectura(nuevaLectura)

            val mensaje = if (lecturaExistente != null) {
                "¡Lectura actualizada! Consumo: %.1f kWh | Total: S/ %.2f".format(consumoKwh, montoPagar)
            } else {
                "Consumo: %.1f kWh | Total a pagar: S/ %.2f".format(consumoKwh, montoPagar)
            }

            Toast.makeText(this@LecturasActivity, mensaje, Toast.LENGTH_LONG).show()

            // Limpiar campos
            binding.etLecturaAnterior.setText("")
            binding.etLecturaActual.setText("")
        }
    }
}