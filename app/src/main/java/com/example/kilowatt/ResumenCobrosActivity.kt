package com.example.kilowatt

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kilowatt.data.DetalleCobro
import com.example.kilowatt.data.FacturaGeneral
import com.example.kilowatt.data.InquilinoRepository
import com.example.kilowatt.data.LecturaRepository
import com.example.kilowatt.databinding.ActivityResumenCobrosBinding
import com.example.kilowatt.util.PdfGenerator
import com.example.kilowatt.util.SettingsManager
import kotlinx.coroutines.launch

class ResumenCobrosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResumenCobrosBinding
    private val lecturaRepository = LecturaRepository()
    private val inquilinoRepository = InquilinoRepository()

    private val viewModel: ResumenCobrosViewModel by viewModels {
        ResumenCobrosViewModel.Factory(lecturaRepository, inquilinoRepository, SettingsManager(this))
    }

    private lateinit var adapter: CobroAdapter
    private var spinnerAdapter: ArrayAdapter<String>? = null
    private var listaMesesDisponibles = mutableListOf<String>()

    private var mesActualSeleccionado: String? = null
    private var facturaActual: FacturaGeneral? = null
    private var detallesActuales: List<DetalleCobro> = emptyList()

    private var isSpinnerInitializing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResumenCobrosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Resumen de Cobros"

        setupRecyclerView()
        setupSpinner()
        observarEstadoViewModel()

        binding.btnExportarReportePdf.setOnClickListener {
            exportarReporteGeneral()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun setupRecyclerView() {
        adapter = CobroAdapter(
            onSendWhatsAppClicked = { detalle ->
                enviarMensajeWhatsapp(detalle)
            },
            onGeneratePdfClicked = { detalle ->
                exportarPdfIndividual(detalle)
            },
            onEstadoChange = { detalle ->
                cambiarEstadoPago(detalle)
            },
        )
        binding.rvResumenCobros.apply {
            layoutManager = LinearLayoutManager(this@ResumenCobrosActivity)
            adapter = this@ResumenCobrosActivity.adapter
        }
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

    private fun actualizarUi(state: ResumenCobrosUiState) {
        facturaActual = state.facturaActual
        detallesActuales = state.detalles
        mesActualSeleccionado = state.mesSeleccionado

        binding.tvReciboTotalGlobal.text = "S/ %.2f".format(state.montoTotalRecibo)
        binding.tvKwhTotalesGlobal.text = "%.1f kWh".format(state.kwhReciboPrincipal)
        binding.tvPrecioKwhGlobal.text = "S/ %.2f".format(state.precioKwhPromedio)

        adapter.actualizarLista(state.detalles)

        // Actualizar Spinner de meses si cambió la lista
        if (state.listaMesesDisponibles != listaMesesDisponibles) {
            isSpinnerInitializing = true
            listaMesesDisponibles.clear()
            listaMesesDisponibles.addAll(state.listaMesesDisponibles)
            spinnerAdapter?.notifyDataSetChanged()
            isSpinnerInitializing = false

            if (state.mesSeleccionado != null) {
                val idx = state.listaMesesDisponibles.indexOf(state.mesSeleccionado)
                if (idx >= 0 && binding.spMesesResumen.selectedItemPosition != idx) {
                    isSpinnerInitializing = true
                    binding.spMesesResumen.setSelection(idx)
                    isSpinnerInitializing = false
                }
            }
        }
    }

    private fun setupSpinner() {
        spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listaMesesDisponibles,
        )
        binding.spMesesResumen.adapter = spinnerAdapter

        binding.spMesesResumen.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isSpinnerInitializing && position in listaMesesDisponibles.indices) {
                    val mesSeleccionado = listaMesesDisponibles[position]
                    if (mesSeleccionado != mesActualSeleccionado) {
                        viewModel.seleccionarMes(mesSeleccionado)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
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
                    viewModel.actualizarEstadoPago(detalle, "PENDIENTE", null, null) { exito, error ->
                        manejarResultadoPago(exito, error, "PENDIENTE")
                    }
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
                    viewModel.actualizarEstadoPago(detalle, "PAGADO", fechaActual, metodoElegido) { exito, error ->
                        manejarResultadoPago(exito, error, "PAGADO")
                    }
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
        }
    }

    private fun manejarResultadoPago(exito: Boolean, error: String?, nuevoEstado: String) {
        if (!isFinishing && !isDestroyed) {
            if (exito) {
                Toast.makeText(this, "Estado actualizado: $nuevoEstado", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Error al actualizar: $error", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun enviarMensajeWhatsapp(cobro: DetalleCobro) {
        val detalleAreaComun = if (cobro.montoAreaComunSoles > 0) {
            "\n🏢 *Cuota Área Común:* S/ ${"%.2f".format(cobro.montoAreaComunSoles)}"
        } else ""

        val mensaje = """
        ⚡ *RESUMEN DE CONSUMO DE LUZ - KILOWATT*
        📅 *Periodo:* ${cobro.mesPeriodo}
        👤 *Inquilino:* ${cobro.nombreInquilino} (${cobro.nombreEspacio})
        
        🔹 *Lectura Anterior:* ${"%.1f".format(cobro.lecturaAnterior)} kWh
        🔹 *Lectura Actual:* ${"%.1f".format(cobro.lecturaActual)} kWh
        ⚡ *Consumo Propio:* ${"%.1f".format(cobro.consumoKwh)} kWh (S/ ${"%.2f".format(cobro.consumoKwh * cobro.precioKwh)})$detalleAreaComun
        💵 *Precio x kWh:* S/ ${"%.2f".format(cobro.precioKwh)}
        
        💰 *TOTAL A PAGAR:* *S/ ${"%.2f".format(cobro.montoPagarSoles)}*
        
        Por favor realizar el Yape/Plin o transferencia al número acostumbrado. ¡Muchas gracias!
        """.trimIndent()

        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val url = "https://api.whatsapp.com/send?phone=51${cobro.telefonoWhatsapp}&text=" + java.net.URLEncoder.encode(mensaje, "UTF-8")
            intent.data = android.net.Uri.parse(url)
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportarPdfIndividual(cobro: DetalleCobro) {
        Toast.makeText(this, "Generando PDF...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                val generator = PdfGenerator(this@ResumenCobrosActivity)
                val file = generator.generarComprobanteIndividual(cobro)

                if (file != null && file.exists()) {
                    val uri = FileProvider.getUriForFile(this@ResumenCobrosActivity, "$packageName.fileprovider", file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(intent, "Compartir Comprobante PDF"))
                } else {
                    Toast.makeText(this@ResumenCobrosActivity, "Error al generar el PDF", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ResumenCobrosActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
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
