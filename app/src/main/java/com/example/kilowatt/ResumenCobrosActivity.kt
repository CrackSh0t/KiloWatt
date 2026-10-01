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
import com.example.kilowatt.util.PdfGenerator
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ResumenCobrosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResumenCobrosBinding
    private val lecturaRepository = LecturaRepository()
    private val inquilinoRepository = InquilinoRepository()

    private lateinit var adapter: CobroAdapter
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
        adapter = CobroAdapter()
        binding.rvResumenCobros.apply {
            layoutManager = LinearLayoutManager(this@ResumenCobrosActivity)
            adapter = this@ResumenCobrosActivity.adapter
        }
    }

    private fun cargarMesesEnSpinner() {
        lifecycleScope.launch {
            lecturaRepository.obtenerFacturasFlow().collect { facturas ->
                val meses = facturas.map { it.mesPeriodo.trim() }.distinct()

                if (meses.isNotEmpty()) {
                    val mesPrevio = mesActualSeleccionado
                    val spinnerAdapter = ArrayAdapter(
                        this@ResumenCobrosActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        meses.toMutableList()
                    )
                    binding.spMesesResumen.adapter = spinnerAdapter

                    val indicePrevio = if (mesPrevio != null) meses.indexOf(mesPrevio) else -1
                    if (indicePrevio >= 0) {
                        binding.spMesesResumen.setSelection(indicePrevio)
                    } else {
                        binding.spMesesResumen.setSelection(0)
                        mesActualSeleccionado = meses[0]
                        cargarDatosDelMes(meses[0])
                    }

                    binding.spMesesResumen.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            if (position in meses.indices) {
                                val mesSeleccionado = meses[position]
                                if (mesSeleccionado != mesActualSeleccionado) {
                                    cargarDatosDelMes(mesSeleccionado)
                                }
                            }
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {}
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
                    binding.tvReciboTotalGlobal.text = "S/ 0.00"
                    binding.tvKwhTotalesGlobal.text = "0 kWh"
                    binding.tvPrecioKwhGlobal.text = "S/ 0.00"
                    detallesActuales = emptyList()
                    adapter.actualizarLista(emptyList())
                    return@combine
                }

                val submedidoresUnicos = submedidores.distinctBy { it.nombreEspacio.trim().lowercase() }
                val lecturasUnicas = lecturas.distinctBy { it.idSubmedidor }

                // 1. Suma total de kWh de TODOS los submedidores con lectura guardada
                val sumaKwhSubmedidores = submedidoresUnicos.sumOf { sub ->
                    lecturasUnicas.find { it.idSubmedidor == sub.idSubmedidor }?.consumoKwh ?: 0.0
                }

                val montoTotalRecibo = factura.montoTotalSoles
                val kwhReciboPrincipal = factura.kwhTotalesRecibo

                // Indicadores de la cabecera
                val precioKwhPromedio = if (sumaKwhSubmedidores > 0) montoTotalRecibo / sumaKwhSubmedidores else 0.0
                binding.tvReciboTotalGlobal.text = "S/ %.2f".format(montoTotalRecibo)
                binding.tvKwhTotalesGlobal.text = "%.1f kWh".format(kwhReciboPrincipal)
                binding.tvPrecioKwhGlobal.text = "S/ %.2f".format(precioKwhPromedio)

                // 2. Calcular Pago S/ de las Áreas Comunes (Ej: Baño 2do piso)
                val submedidoresAreaComun = submedidoresUnicos.filter { it.esAreaComun }
                var totalSolesAreaComun = 0.0

                submedidoresAreaComun.forEach { subComun ->
                    val lecturaComun = lecturasUnicas.find { it.idSubmedidor == subComun.idSubmedidor }
                    val consumoKwh = lecturaComun?.consumoKwh ?: 0.0
                    val porcentajeConsumo = if (sumaKwhSubmedidores > 0) consumoKwh / sumaKwhSubmedidores else 0.0
                    val pagoSolesAreaComun = porcentajeConsumo * montoTotalRecibo
                    totalSolesAreaComun += pagoSolesAreaComun
                }

                // 3. Dividir el costo del Área Común entre los inquilinos configurados para pagar
                val submedidoresParticulares = submedidoresUnicos.filter { !it.esAreaComun }
                val pagadoresAreaComun = submedidoresParticulares.filter { it.pagaAreaComun }
                val cantidadPagadores = pagadoresAreaComun.size.coerceAtLeast(1)
                val cuotaAreaComunPorInquilino = totalSolesAreaComun / cantidadPagadores

                // 4. Generar el detalle para cada inquilino particular (exactamente como en Excel)
                val listaDetalle = submedidoresParticulares.map { submedidor ->
                    val lectura = lecturasUnicas.find { it.idSubmedidor == submedidor.idSubmedidor }
                    val inquilino = inquilinos.find { it.idInquilino == submedidor.idInquilinoTitular }

                    val consumoKwh = lectura?.consumoKwh ?: 0.0

                    // FÓRMULA EXCEL: (Consumo kWh / Suma Total kWh) * Monto Total Recibo
                    val porcentajeConsumo = if (sumaKwhSubmedidores > 0) consumoKwh / sumaKwhSubmedidores else 0.0
                    val pagoBaseSoles = porcentajeConsumo * montoTotalRecibo

                    // Sumar la cuota del área común si aplica
                    val cuotaComunAplicada = if (submedidor.pagaAreaComun) cuotaAreaComunPorInquilino else 0.0
                    val totalFinalPagar = pagoBaseSoles + cuotaComunAplicada

                    DetalleCobro(
                        nombreInquilino = inquilino?.nombreCompleto?.ifEmpty { submedidor.nombreEspacio } ?: submedidor.nombreEspacio,
                        telefonoWhatsapp = inquilino?.telefonoWhatsapp ?: "",
                        nombreEspacio = submedidor.nombreEspacio,
                        mesPeriodo = mes,
                        lecturaAnterior = lectura?.lecturaAnterior ?: 0.0,
                        lecturaActual = lectura?.lecturaActual ?: 0.0,
                        consumoKwh = consumoKwh,
                        precioKwh = precioKwhPromedio,
                        montoAreaComunSoles = cuotaComunAplicada,
                        montoPagarSoles = totalFinalPagar
                    )
                }

                detallesActuales = listaDetalle
                adapter.actualizarLista(listaDetalle)
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

        val generator = PdfGenerator(this)
        val file = generator.generarReporteGeneral(mes, factura, detallesActuales)

        if (file != null && file.exists()) {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "application/pdf"
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(intent, "Compartir Reporte General PDF"))
        } else {
            Toast.makeText(this, "Error al generar el reporte", Toast.LENGTH_SHORT).show()
        }
    }
}