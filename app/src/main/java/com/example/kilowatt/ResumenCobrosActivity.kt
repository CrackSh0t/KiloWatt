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
import com.example.kilowatt.data.AppDatabase
import com.example.kilowatt.data.DetalleCobro
import com.example.kilowatt.data.FacturaGeneral
import com.example.kilowatt.databinding.ActivityResumenCobrosBinding
import com.example.kilowatt.util.PdfGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ResumenCobrosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResumenCobrosBinding
    private lateinit var database: AppDatabase
    private lateinit var adapter: CobroAdapter
    private var mesActualSeleccionado: String? = null
    private var facturaActual: FacturaGeneral? = null
    private var detallesActuales: List<DetalleCobro> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResumenCobrosBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Mostrar flecha de regreso en la barra superior
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Resumen de Cobros"

        database = AppDatabase.getDatabase(this)

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
            val facturas = database.facturaGeneralDao().obtenerTodasLasFacturas().first()
            val meses = facturas.map { it.mesPeriodo }

            if (meses.isNotEmpty()) {
                val spinnerAdapter = ArrayAdapter(
                    this@ResumenCobrosActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    meses.toMutableList()
                )
                binding.spMesesResumen.adapter = spinnerAdapter

                binding.spMesesResumen.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        val mesSeleccionado = meses[position]
                        cargarDatosDelMes(mesSeleccionado)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }
        }
    }

    private fun cargarDatosDelMes(mes: String) {
        mesActualSeleccionado = mes
        lifecycleScope.launch {
            val factura = database.facturaGeneralDao().obtenerFacturaPorMes(mes)
            facturaActual = factura

            if (factura != null) {
                val lecturas = database.lecturaDao().obtenerLecturasPorMes(mes).first()
                val submedidores = database.submedidorDao().obtenerTodosLosSubmedidores().first()
                val inquilinos = database.inquilinoDao().obtenerTodosLosInquilinos().first()

                // Eliminar duplicados si un submedidor tiene más de una lectura en el mismo mes
                val lecturasUnicas = lecturas.distinctBy { it.idSubmedidor }

                // 1. Obtener la suma TOTAL de kWh medidos en submedidores únicos
                val totalKwhSubmedidores = lecturasUnicas.sumOf { it.consumoKwh }

                // 2. Calcular el Precio Efectivo por kWh
                val precioKwhEfectivo = if (totalKwhSubmedidores > 0) {
                    factura.montoTotalSoles / totalKwhSubmedidores
                } else {
                    0.0
                }

                binding.tvReciboTotalGlobal.text = "S/ %.2f".format(factura.montoTotalSoles)
                binding.tvKwhTotalesGlobal.text = "%.0f kWh".format(totalKwhSubmedidores)
                binding.tvPrecioKwhGlobal.text = "S/ %.2f".format(precioKwhEfectivo)

                // 3. Separar lecturas únicas
                val lecturasAreaComun = lecturasUnicas.filter { lectura ->
                    val sub = submedidores.find { it.idSubmedidor == lectura.idSubmedidor }
                    sub?.esAreaComun == true
                }

                val lecturasParticulares = lecturasUnicas.filter { lectura ->
                    val sub = submedidores.find { it.idSubmedidor == lectura.idSubmedidor }
                    sub?.esAreaComun == false
                }

                // 4. Calcular el monto en Soles del Área Común
                val totalKwhAreaComun = lecturasAreaComun.sumOf { it.consumoKwh }
                val totalSolesAreaComun = totalKwhAreaComun * precioKwhEfectivo

                // 5. Dividir cuota de área común solo entre los pagadores activos
                val cantidadPagadores = lecturasParticulares.count { lectura ->
                    val sub = submedidores.find { it.idSubmedidor == lectura.idSubmedidor }
                    sub?.pagaAreaComun == true
                }.coerceAtLeast(1)

                val cuotaAreaComunPorInquilino = totalSolesAreaComun / cantidadPagadores

                // 6. Generar lista de cobro limpia
                val listaDetalle = lecturasParticulares.map { lectura ->
                    val submedidor = submedidores.find { it.idSubmedidor == lectura.idSubmedidor }
                    val inquilino = inquilinos.find { it.idInquilino == submedidor?.idInquilinoTitular }

                    val consumoPropioSoles = lectura.consumoKwh * precioKwhEfectivo
                    val cuotaAplicada = if (submedidor?.pagaAreaComun == true) cuotaAreaComunPorInquilino else 0.0
                    val totalFinalPagar = consumoPropioSoles + cuotaAplicada

                    DetalleCobro(
                        nombreInquilino = inquilino?.nombreCompleto ?: submedidor?.nombreEspacio ?: "Inquilino",
                        telefonoWhatsapp = inquilino?.telefonoWhatsapp ?: "",
                        nombreEspacio = submedidor?.nombreEspacio ?: "Espacio",
                        mesPeriodo = mes,
                        lecturaAnterior = lectura.lecturaAnterior,
                        lecturaActual = lectura.lecturaActual,
                        consumoKwh = lectura.consumoKwh,
                        precioKwh = precioKwhEfectivo,
                        montoAreaComunSoles = cuotaAplicada,
                        montoPagarSoles = totalFinalPagar
                    )
                }

                detallesActuales = listaDetalle
                adapter.actualizarLista(listaDetalle)
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
