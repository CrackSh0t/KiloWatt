package com.example.kilowatt

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.kilowatt.data.ConsumoHistoricoResult
import com.example.kilowatt.data.HistoricoSubmedidor
import com.example.kilowatt.data.InquilinoRepository
import com.example.kilowatt.data.LecturaRepository
import com.example.kilowatt.data.PuntoHistorico
import com.example.kilowatt.databinding.ActivityHistorialConsumoBinding
import com.example.kilowatt.util.PeriodoDateUtils
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.launch

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HistorialConsumoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistorialConsumoBinding

    private val viewModel: HistorialConsumoViewModel by viewModels()

    private var resultadoActual: ConsumoHistoricoResult? = null
    private var spinnerSubmedidoresAdapter: ArrayAdapter<String>? = null
    private var listaNombresSubmedidores = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistorialConsumoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_historial_graficos)

        setupSpinner()
        configurarEstiloGraficoInmueble()
        configurarEstiloGraficoSubmedidor()
        observarEstadoViewModel()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun setupSpinner() {
        spinnerSubmedidoresAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listaNombresSubmedidores,
        )
        binding.spSubmedidorHistorial.adapter = spinnerSubmedidoresAdapter

        binding.spSubmedidorHistorial.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val resultado = resultadoActual ?: return
                if (position in resultado.historicoPorSubmedidor.indices) {
                    renderizarGraficoSubmedidor(resultado.historicoPorSubmedidor[position])
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun observarEstadoViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is HistorialConsumoUiState.Cargando -> {
                            // Muestra progreso si es necesario
                        }
                        is HistorialConsumoUiState.Exito -> {
                            resultadoActual = state.resultado
                            actualizarUi(state.resultado)
                        }
                        is HistorialConsumoUiState.Error -> {
                            Toast.makeText(this@HistorialConsumoActivity, state.mensaje, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun actualizarUi(resultado: ConsumoHistoricoResult) {
        // KPIs Globales
        binding.tvPromedioKwh.text = getString(R.string.label_kwh_format, resultado.promedioKwhMensual)
        binding.tvMesMayorConsumo.text = resultado.mesMayorConsumo ?: "-"

        // Gráfico Inmueble
        renderizarGraficoInmueble(resultado.puntosInmuebleMatriz)

        // Spinner de Submedidores
        val nuevosNombres = resultado.historicoPorSubmedidor.map {
            "${it.nombreEspacio} (${it.nombreInquilino})"
        }

        if (nuevosNombres != listaNombresSubmedidores) {
            listaNombresSubmedidores.clear()
            listaNombresSubmedidores.addAll(nuevosNombres)
            spinnerSubmedidoresAdapter?.notifyDataSetChanged()
        }

        if (resultado.historicoPorSubmedidor.isNotEmpty()) {
            val idxSeleccionado = binding.spSubmedidorHistorial.selectedItemPosition.coerceAtLeast(0)
            if (idxSeleccionado in resultado.historicoPorSubmedidor.indices) {
                renderizarGraficoSubmedidor(resultado.historicoPorSubmedidor[idxSeleccionado])
            }
        } else {
            binding.chartSubmedidor.visibility = View.GONE
            binding.tvSinDatosSubmedidor.visibility = View.VISIBLE
        }
    }

    private fun configurarEstiloGraficoInmueble() {
        binding.chartInmueble.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            legend.textColor = Color.parseColor("#757575")

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = Color.parseColor("#757575")
                granularity = 1f
            }

            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#E0E0E0")
                textColor = Color.parseColor("#757575")
            }

            axisRight.isEnabled = false
        }
    }

    private fun configurarEstiloGraficoSubmedidor() {
        binding.chartSubmedidor.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            legend.textColor = Color.parseColor("#757575")

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = Color.parseColor("#757575")
                granularity = 1f
            }

            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#E0E0E0")
                textColor = Color.parseColor("#757575")
            }

            axisRight.isEnabled = false
        }
    }

    private fun renderizarGraficoInmueble(puntos: List<PuntoHistorico>) {
        if (puntos.isEmpty()) {
            binding.chartInmueble.visibility = View.GONE
            binding.tvSinDatosMatriz.visibility = View.VISIBLE
            return
        }

        binding.chartInmueble.visibility = View.VISIBLE
        binding.tvSinDatosMatriz.visibility = View.GONE

        val entries = puntos.mapIndexed { index, punto ->
            Entry(index.toFloat(), punto.consumoKwh.toFloat())
        }

        val dataSet = LineDataSet(entries, "Consumo Inmueble Matriz (kWh)").apply {
            color = Color.parseColor("#673AB7")
            setCircleColor(Color.parseColor("#673AB7"))
            lineWidth = 2.5f
            circleRadius = 4.5f
            setDrawCircleHole(false)
            valueTextColor = Color.parseColor("#673AB7")
            valueTextSize = 10f
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        binding.chartInmueble.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val idx = value.toInt()
                return if (idx in puntos.indices) {
                    PeriodoDateUtils.formatearParaGrafico(puntos[idx].mesPeriodo)
                } else ""
            }
        }

        binding.chartInmueble.data = LineData(dataSet)
        binding.chartInmueble.invalidate()
    }

    private fun renderizarGraficoSubmedidor(historico: HistoricoSubmedidor) {
        val puntos = historico.puntos
        if (puntos.isEmpty()) {
            binding.chartSubmedidor.visibility = View.GONE
            binding.tvSinDatosSubmedidor.visibility = View.VISIBLE
            return
        }

        binding.chartSubmedidor.visibility = View.VISIBLE
        binding.tvSinDatosSubmedidor.visibility = View.GONE

        val entries = puntos.mapIndexed { index, punto ->
            BarEntry(index.toFloat(), punto.consumoKwh.toFloat())
        }

        val dataSet = BarDataSet(entries, "Consumo ${historico.nombreEspacio} (kWh)").apply {
            color = Color.parseColor("#2E7D32")
            valueTextColor = Color.parseColor("#2E7D32")
            valueTextSize = 10f
        }

        binding.chartSubmedidor.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val idx = value.toInt()
                return if (idx in puntos.indices) {
                    PeriodoDateUtils.formatearParaGrafico(puntos[idx].mesPeriodo)
                } else ""
            }
        }

        binding.chartSubmedidor.data = BarData(dataSet)
        binding.chartSubmedidor.invalidate()
    }
}
