package com.example.kilowatt

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kilowatt.data.AppDatabase
import com.example.kilowatt.data.DetalleCobro
import com.example.kilowatt.databinding.ActivityResumenCobrosBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ResumenCobrosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResumenCobrosBinding
    private lateinit var database: AppDatabase
    private lateinit var adapter: CobroAdapter

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
        lifecycleScope.launch {
            val factura = database.facturaGeneralDao().obtenerFacturaPorMes(mes)

            if (factura != null) {
                val precioKwh = factura.montoTotalSoles / factura.kwhTotalesRecibo

                binding.tvReciboTotalGlobal.text = "S/ %.2f".format(factura.montoTotalSoles)
                binding.tvKwhTotalesGlobal.text = "%.0f kWh".format(factura.kwhTotalesRecibo)
                binding.tvPrecioKwhGlobal.text = "S/ %.2f".format(precioKwh)

                val lecturas = database.lecturaDao().obtenerLecturasPorMes(mes).first()
                val submedidores = database.submedidorDao().obtenerTodosLosSubmedidores().first()
                val inquilinos = database.inquilinoDao().obtenerTodosLosInquilinos().first()

                // 1. Separar lecturas de áreas comunes y lecturas particulares
                val lecturasAreaComun = lecturas.filter { lectura ->
                    val sub = submedidores.find { it.idSubmedidor == lectura.idSubmedidor }
                    sub?.esAreaComun == true
                }

                val lecturasParticulares = lecturas.filter { lectura ->
                    val sub = submedidores.find { it.idSubmedidor == lectura.idSubmedidor }
                    sub?.esAreaComun == false
                }

                // 2. Calcular el costo total acumulado de todas las áreas comunes
                val totalKwhAreaComun = lecturasAreaComun.sumOf { it.consumoKwh }
                val totalSolesAreaComun = totalKwhAreaComun * precioKwh

                // 3. Contar SOLO los espacios configurados para pagar cuota común (evita división por cero)
                val cantidadPagadores = lecturasParticulares.count { lectura ->
                    val sub = submedidores.find { it.idSubmedidor == lectura.idSubmedidor }
                    sub?.pagaAreaComun == true
                }.coerceAtLeast(1)

                val cuotaAreaComunPorInquilino = totalSolesAreaComun / cantidadPagadores

                // 4. Construir la lista asignando la cuota solo a los que les corresponde
                val listaDetalle = lecturasParticulares.map { lectura ->
                    val submedidor = submedidores.find { it.idSubmedidor == lectura.idSubmedidor }
                    val inquilino = inquilinos.find { it.idInquilino == submedidor?.idInquilinoTitular }

                    val consumoPropioSoles = lectura.consumoKwh * precioKwh

                    // Si el espacio paga área común recibe la cuota, de lo contrario S/ 0.00
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
                        precioKwh = precioKwh,
                        montoAreaComunSoles = cuotaAplicada,
                        montoPagarSoles = totalFinalPagar
                    )
                }

                adapter.actualizarLista(listaDetalle)
            }
        }
    }
}