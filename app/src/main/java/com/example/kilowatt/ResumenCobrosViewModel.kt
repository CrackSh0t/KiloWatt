package com.example.kilowatt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kilowatt.data.DetalleCobro
import com.example.kilowatt.data.IInquilinoRepository
import com.example.kilowatt.data.ILecturaRepository
import com.example.kilowatt.util.CobroCalculator
import com.example.kilowatt.util.ISettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ResumenCobrosViewModel @Inject constructor(
    private val lecturaRepository: ILecturaRepository,
    private val inquilinoRepository: IInquilinoRepository,
    private val settingsManager: ISettingsManager,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResumenCobrosUiState())
    val uiState: StateFlow<ResumenCobrosUiState> = _uiState.asStateFlow()

    private var jobCargarDatosMes: Job? = null

    init {
        observarFacturasYMeses()
    }

    private fun observarFacturasYMeses() {
        viewModelScope.launch {
            lecturaRepository.obtenerFacturasFlow().collect { facturas ->
                val nuevosMeses = facturas.map { it.mesPeriodo.trim() }.distinct()
                val estadoActual = _uiState.value

                if (nuevosMeses.isNotEmpty()) {
                    val mesSeleccionado = if (estadoActual.mesSeleccionado in nuevosMeses) {
                        estadoActual.mesSeleccionado
                    } else {
                        nuevosMeses[0]
                    }

                    _uiState.value = estadoActual.copy(
                        listaMesesDisponibles = nuevosMeses,
                        mesSeleccionado = mesSeleccionado,
                    )

                    if (mesSeleccionado != null && mesSeleccionado != estadoActual.mesSeleccionado) {
                        cargarDatosDelMes(mesSeleccionado)
                    } else if (estadoActual.mesSeleccionado == null && mesSeleccionado != null) {
                        cargarDatosDelMes(mesSeleccionado)
                    }
                } else {
                    _uiState.value = estadoActual.copy(
                        listaMesesDisponibles = emptyList(),
                        mesSeleccionado = null,
                        detalles = emptyList(),
                        estaCargando = false,
                    )
                }
            }
        }
    }

    fun seleccionarMes(mes: String) {
        if (mes != _uiState.value.mesSeleccionado) {
            _uiState.value = _uiState.value.copy(mesSeleccionado = mes)
            cargarDatosDelMes(mes)
        }
    }

    private fun cargarDatosDelMes(mes: String) {
        jobCargarDatosMes?.cancel()

        jobCargarDatosMes = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(estaCargando = true)
            combine(
                lecturaRepository.obtenerFacturasFlow(),
                lecturaRepository.obtenerLecturasPorMesFlow(mes),
                inquilinoRepository.obtenerSubmedidoresFlow(),
                inquilinoRepository.obtenerInquilinosFlow(),
                inquilinoRepository.obtenerPerfilPropietarioFlow(),
            ) { facturas, lecturas, submedidores, inquilinos, perfilMap ->
                val factura = facturas.find { it.mesPeriodo.trim().equals(mes.trim(), ignoreCase = true) }

                if (factura == null) {
                    _uiState.value = _uiState.value.copy(
                        estaCargando = false,
                        facturaActual = null,
                        montoTotalRecibo = 0.0,
                        kwhReciboPrincipal = 0.0,
                        precioKwhPromedio = 0.0,
                        detalles = emptyList(),
                    )
                    return@combine
                }

                val diaLimite = (perfilMap?.get("diaLimitePago") as? Long)?.toInt()
                    ?: (perfilMap?.get("diaLimitePago") as? Int)
                    ?: settingsManager.diaLimitePago

                val tipoProrrateoStr = perfilMap?.get("tipoProrrateoComun")?.toString()
                    ?: settingsManager.tipoProrrateoComun

                val tipoProrrateoEnum = if (tipoProrrateoStr == "WEIGHTED_PERSONS") {
                    com.example.kilowatt.data.TipoProrrateoComun.WEIGHTED_PERSONS
                } else {
                    com.example.kilowatt.data.TipoProrrateoComun.EQUITY
                }

                val resultado = withContext(dispatcher) {
                    CobroCalculator.calcularResumenMensual(
                        mes = mes,
                        factura = factura,
                        submedidores = submedidores,
                        lecturas = lecturas,
                        inquilinos = inquilinos,
                        diaLimitePago = diaLimite,
                        tipoProrrateoComun = tipoProrrateoEnum,
                    )
                }

                _uiState.value = _uiState.value.copy(
                    estaCargando = false,
                    facturaActual = factura,
                    montoTotalRecibo = resultado.montoTotalRecibo,
                    kwhReciboPrincipal = resultado.kwhReciboPrincipal,
                    precioKwhPromedio = resultado.precioKwhPromedio,
                    detalles = resultado.detalles,
                )
            }.collect {}
        }
    }

    fun actualizarEstadoPago(
        detalle: DetalleCobro,
        nuevoEstado: String,
        fechaPago: Long?,
        metodoPago: String?,
        onResult: (Boolean, String?) -> Unit,
    ) {
        if (detalle.idLectura.isBlank()) {
            onResult(false, "Debes ingresar la lectura de este espacio antes de registrar su pago")
            return
        }

        viewModelScope.launch {
            val (exito, error) = lecturaRepository.actualizarEstadoPago(detalle.idLectura, nuevoEstado, fechaPago, metodoPago)
            onResult(exito, error)
        }
    }

    class Factory(
        private val lecturaRepository: ILecturaRepository,
        private val inquilinoRepository: IInquilinoRepository,
        private val settingsManager: ISettingsManager,
        private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ResumenCobrosViewModel::class.java)) {
                return ResumenCobrosViewModel(lecturaRepository, inquilinoRepository, settingsManager, dispatcher) as T
            }
            throw IllegalArgumentException("ViewModel desconocido")
        }
    }
}
