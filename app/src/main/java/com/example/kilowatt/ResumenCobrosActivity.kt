package com.example.kilowatt

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kilowatt.data.DetalleCobro
import com.example.kilowatt.data.FacturaGeneral
import com.example.kilowatt.data.InquilinoRepository
import com.example.kilowatt.data.LecturaRepository
import com.example.kilowatt.databinding.ActivityResumenCobrosBinding
import com.example.kilowatt.util.CobroCalculator
import com.example.kilowatt.util.PdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ResumenCobrosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResumenCobrosBinding
    private val lecturaRepository = LecturaRepository()
    private val inquilinoRepository = InquilinoRepository()

    private lateinit var adapter: CobroAdapter
    private var spinnerAdapter: ArrayAdapter<String>? = null
    private var listaMesesDisponibles = mutableListOf<String>()

    private var mesActualSeleccionado: String? = null
    private var facturaActual: FacturaGeneral? = null
    private var detallesActuales: List<DetalleCobro> = emptyList()

    private var jobCargarDatosMes: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResumenCobrosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Resumen de Cobros"

        setupRecyclerView()
        setupSpinner()
        cargarMesesEnSpinner()

        binding.btnExportarReportePdf.setOnClickListener {
            exportarReporteGeneral()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun setupRecyclerView() {
        adapter = CobroAdapter { detalle ->
            cambiarEstadoPago(detalle)
        }
        binding.rvResumenCobros.apply {
            layoutManager = LinearLayoutManager(this@ResumenCobrosActivity)
            adapter = this@ResumenCobrosActivity.adapter
        }
    }

    private fun cambiarEstadoPago(detalle: DetalleCobro) {
        if (detalle.idLectura.isBlank()) {
            Toast.makeText(this, "Debes ingresar la lectura de este espacio antes de registrar su pago", Toast.LENGTH_SHORT).show()
            return
        }

        if (detalle.estadoPago.equals("PAGADO", ignoreCase = true)) {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_revertir_pago_title)
                .setMessage(R.string.dialog_revertir_pago_msg)
                .setPositiveButton("Revertir") { _, _ ->
                    ejecutarCambioEstado(detalle.idLectura, "PENDIENTE", null, null)
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
        } else {
            val metodos = arrayOf("Yape", "Plin", "Efectivo", "Transferencia Bancaria")
            var seleccionIndice = 0

            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_metodo_pago_title)
                .setSingleChoiceItems(metodos, 0) { _, which ->
                    seleccionIndice = which
                }
                .setPositiveButton(R.string.btn_send) { _, _ ->
                    val metodoElegido = metodos[seleccionIndice]
                    val fechaActual = System.currentTimeMillis()
                    ejecutarCambioEstado(detalle.idLectura, "PAGADO", fechaActual, metodoElegido)
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
        }
    }

    private fun ejecutarCambioEstado(idLectura: String, nuevoEstado: String, fechaPago: Long?, metodoPago: String?) {
        lifecycleScope.launch {
            val (exito, error) = lecturaRepository.actualizarEstadoPago(idLectura, nuevoEstado, fechaPago, metodoPago)
            if (exito) {
                Toast.makeText(this@ResumenCobrosActivity, "Estado actualizado: $nuevoEstado", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@ResumenCobrosActivity, "Error al actualizar: $error", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupSpinner() {
        spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listaMesesDisponibles
        )
        binding.spMesesResumen.adapter = spinnerAdapter

        binding.spMesesResumen.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position in listaMesesDisponibles.indices) {
                    val mesSeleccionado = listaMesesDisponibles[position]
                    if (mesSeleccionado != mesActualSeleccionado) {
                        cargarDatosDelMes(mesSeleccionado)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun cargarMesesEnSpinner() {
        lifecycleScope.launch {
            lecturaRepository.obtenerFacturasFlow().collect { facturas ->
                val nuevosMeses = facturas.map { it.mesPeriodo.trim() }.distinct()

                if (nuevosMeses.isNotEmpty() && nuevosMeses != listaMesesDisponibles) {
                    val mesPrevio = mesActualSeleccionado
                    listaMesesDisponibles.clear()
                    listaMesesDisponibles.addAll(nuevosMeses)
                    spinnerAdapter?.notifyDataSetChanged()

                    val indicePrevio = if (mesPrevio != null) nuevosMeses.indexOf(mesPrevio) else -1
                    if (indicePrevio >= 0) {
                        binding.spMesesResumen.setSelection(indicePrevio)
                    } else {
                        binding.spMesesResumen.setSelection(0)
                        val mesInicial = nuevosMeses[0]
                        mesActualSeleccionado = mesInicial
                        cargarDatosDelMes(mesInicial)
                    }
                }
            }
        }
    }

    private fun cargarDatosDelMes(mes: String) {
        mesActualSeleccionado = mes
        jobCargarDatosMes?.cancel()

        jobCargarDatosMes = lifecycleScope.launch {
            combine(
                lecturaRepository.obtenerFacturasFlow(),
                lecturaRepository.obtenerLecturasPorMesFlow(mes),
                inquilinoRepository.obtenerSubmedidoresFlow(),
                inquilinoRepository.obtenerInquilinosFlow()
            ) { facturas, lecturas, submedidores, inquilinos ->
                val factura = facturas.find { it.mesPeriodo.trim().equals(mes.trim(), ignoreCase = true) }
                facturaActual = factura

                if (factura == null) {
                    withContext(Dispatchers.Main) {
                        binding.tvReciboTotalGlobal.text = "S/ 0.00"
                        binding.tvKwhTotalesGlobal.text = "0 kWh"
                        binding.tvPrecioKwhGlobal.text = "S/ 0.00"
                        detallesActuales = emptyList()
                        adapter.actualizarLista(emptyList())
                    }
                    return@combine
                }

                // Ejecución del motor de cálculo en Dispatchers.Default fuera del hilo principal
                val resultado = withContext(Dispatchers.Default) {
                    CobroCalculator.calcularResumenMensual(
                        mes = mes,
                        factura = factura,
                        submedidores = submedidores,
                        lecturas = lecturas,
                        inquilinos = inquilinos
                    )
                }

                withContext(Dispatchers.Main) {
                    binding.tvReciboTotalGlobal.text = "S/ %.2f".format(resultado.montoTotalRecibo)
                    binding.tvKwhTotalesGlobal.text = "%.1f kWh".format(resultado.kwhReciboPrincipal)
                    binding.tvPrecioKwhGlobal.text = "S/ %.2f".format(resultado.precioKwhPromedio)

                    detallesActuales = resultado.detalles
                    adapter.actualizarLista(resultado.detalles)
                }
            }.collect {}
        }
    }

    private fun exportarReporteGeneral() {
        val mes = mesActualSeleccionado ?: return
        val factura = facturaActual ?: return

        if (detallesActuales.isEmpty()) {
            Toast.makeText(this, "No hay datos para exportar", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnExportarReportePdf.isEnabled = false
        Toast.makeText(this, "Generando reporte general...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                val generator = PdfGenerator(this@ResumenCobrosActivity)
                val file = generator.generarReporteGeneral(mes, factura, detallesActuales)

                if (file != null && file.exists()) {
                    val uri = FileProvider.getUriForFile(this@ResumenCobrosActivity, "$packageName.fileprovider", file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(intent, "Compartir Reporte General PDF"))
                } else {
                    Toast.makeText(this@ResumenCobrosActivity, "Error al generar el reporte", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ResumenCobrosActivity, "Error al generar reporte: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                binding.btnExportarReportePdf.isEnabled = true
            }
        }
    }
}