package com.example.kilowatt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kilowatt.data.FacturaGeneral
import com.example.kilowatt.data.IInquilinoRepository
import com.example.kilowatt.data.ILecturaRepository
import com.example.kilowatt.data.Lectura
import com.example.kilowatt.data.Submedidor
import com.example.kilowatt.domain.OperationResult
import com.example.kilowatt.util.CobroCalculator
import com.example.kilowatt.util.ValidacionLecturaResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class LecturasViewModel @Inject constructor(
    private val lecturaRepository: ILecturaRepository,
    private val inquilinoRepository: IInquilinoRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LecturasUiState())
    val uiState: StateFlow<LecturasUiState> = _uiState.asStateFlow()

    init {
        observarSubmedidores()
    }

    private fun observarSubmedidores() {
        viewModelScope.launch {
            inquilinoRepository.obtenerSubmedidoresFlow()
                .catch { exception ->
                    _uiState.value = _uiState.value.copy(
                        mensajeNotificacion = "Error al cargar medidores: ${exception.localizedMessage}",
                        esError = true,
                    )
                }
                .collect { lista ->
                _uiState.value = _uiState.value.copy(submedidoresDisponibles = lista)
            }
        }
    }

    fun cargarLecturaPreviaSubmedidor(mes: String, submedidor: Submedidor?) {
        val mesLimpio = mes.trim()
        if (mesLimpio.isEmpty() || submedidor == null) {
            _uiState.value = _uiState.value.copy(mesPeriodo = mesLimpio, ultimaLecturaSubmedidor = null)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(mesPeriodo = mesLimpio)
            lecturaRepository.obtenerLecturasPorMesFlow(mesLimpio)
                .catch { exception ->
                    _uiState.value = _uiState.value.copy(
                        ultimaLecturaSubmedidor = null,
                        mensajeNotificacion = "Error al cargar la lectura previa: ${exception.localizedMessage}",
                        esError = true,
                    )
                }
                .firstOrNull()
                ?.let { lecturas ->
                    val lecturaPrev = lecturas.find { it.idSubmedidor == submedidor.idSubmedidor }
                    _uiState.value = _uiState.value.copy(ultimaLecturaSubmedidor = lecturaPrev)
                }
        }
    }

    fun guardarFacturaGeneral(mes: String, montoStr: String, kwhStr: String) {
        val mesLimpio = mes.trim()
        val monto = montoStr.trim().toDoubleOrNull()
        val kwh = kwhStr.trim().toDoubleOrNull()

        if (mesLimpio.isEmpty() || monto == null || kwh == null) {
            _uiState.value = _uiState.value.copy(
                mensajeNotificacion = "Completa los datos del recibo base",
                esError = true,
            )
            return
        }

        _uiState.value = _uiState.value.copy(estaGuardandoReciboBase = true)

        val factura = FacturaGeneral(
            idFactura = mesLimpio,
            mesPeriodo = mesLimpio,
            montoTotalSoles = monto,
            kwhTotalesRecibo = kwh,
        )

        viewModelScope.launch {
            when (val resultado = lecturaRepository.guardarFactura(factura)) {
                is OperationResult.Synced -> _uiState.value = _uiState.value.copy(
                    estaGuardandoReciboBase = false,
                    mensajeNotificacion = "¡Recibo Base guardado para $mesLimpio!",
                    esError = false,
                )
                is OperationResult.PendingSync -> _uiState.value = _uiState.value.copy(
                    estaGuardandoReciboBase = false,
                    mensajeNotificacion = resultado.reason ?: "Recibo guardado localmente; pendiente de sincronización",
                    esError = false,
                )
                is OperationResult.Failed -> _uiState.value = _uiState.value.copy(
                    estaGuardandoReciboBase = false,
                    mensajeNotificacion = "Error al guardar recibo: ${resultado.message}",
                    esError = true,
                )
            }
        }
    }

    fun calcularYGuardarLectura(
        mes: String,
        lecAntStr: String,
        lecActStr: String,
        submedidor: Submedidor?,
    ) {
        val mesLimpio = mes.trim()
        val lecAnt = lecAntStr.trim().toDoubleOrNull()
        val lecAct = lecActStr.trim().toDoubleOrNull()

        if (mesLimpio.isEmpty() || lecAnt == null || lecAct == null || submedidor == null) {
            _uiState.value = _uiState.value.copy(
                mensajeNotificacion = "Ingresa las lecturas e indica el mes exacto y un medidor válido",
                esError = true,
            )
            return
        }

        val validacion = CobroCalculator.validarLecturas(lecAnt, lecAct)
        if (validacion is ValidacionLecturaResult.Invalido) {
            _uiState.value = _uiState.value.copy(
                mensajeNotificacion = validacion.mensaje,
                esError = true,
            )
            return
        }

        _uiState.value = _uiState.value.copy(estaGuardandoLectura = true)

        viewModelScope.launch {
            try {
                val facturas = lecturaRepository.obtenerFacturasFlow().first()
                val facturaMes = facturas.find { it.mesPeriodo.trim().equals(mesLimpio, ignoreCase = true) }

                if (facturaMes == null) {
                    _uiState.value = _uiState.value.copy(
                        estaGuardandoLectura = false,
                        mensajeNotificacion = "Primero debes guardar el Recibo Base de este mes ($mesLimpio)",
                        esError = true,
                    )
                    return@launch
                }

                val lecturasDelMes = lecturaRepository.obtenerLecturasPorMesFlow(mesLimpio).first()
                val lecturaExistente = lecturasDelMes.find { it.idSubmedidor == submedidor.idSubmedidor }

                val (consumoKwh, montoPagar) = withContext(dispatcher) {
                    CobroCalculator.calcularConsumoYMontoIndividual(
                        lecturaAnterior = lecAnt,
                        lecturaActual = lecAct,
                        montoTotalRecibo = facturaMes.montoTotalSoles,
                        kwhTotalesRecibo = facturaMes.kwhTotalesRecibo,
                    )
                }

                val nuevaLectura = Lectura(
                    idLectura = lecturaExistente?.idLectura.takeIf { !it.isNullOrEmpty() } ?: UUID.randomUUID().toString(),
                    idSubmedidor = submedidor.idSubmedidor,
                    mesPeriodo = mesLimpio,
                    lecturaAnterior = lecAnt,
                    lecturaActual = lecAct,
                    consumoKwh = consumoKwh,
                    montoPagarSoles = montoPagar,
                    estadoPago = lecturaExistente?.estadoPago ?: "PENDIENTE",
                    fechaPago = lecturaExistente?.fechaPago,
                    metodoPago = lecturaExistente?.metodoPago,
                )

                when (val resultado = lecturaRepository.guardarLectura(nuevaLectura)) {
                    is OperationResult.Synced -> {
                        val msj = "Guardado: %.1f kWh | Total: S/ %.2f".format(consumoKwh, montoPagar)
                        _uiState.value = _uiState.value.copy(
                            estaGuardandoLectura = false,
                            mensajeNotificacion = msj,
                            esError = false,
                            ultimaLecturaSubmedidor = nuevaLectura,
                        )
                    }
                    is OperationResult.PendingSync -> _uiState.value = _uiState.value.copy(
                        estaGuardandoLectura = false,
                        mensajeNotificacion = resultado.reason ?: "Lectura guardada localmente; pendiente de sincronización",
                        esError = false,
                        ultimaLecturaSubmedidor = nuevaLectura,
                    )
                    is OperationResult.Failed -> _uiState.value = _uiState.value.copy(
                        estaGuardandoLectura = false,
                        mensajeNotificacion = "Error al guardar lectura: ${resultado.message}",
                        esError = true,
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    estaGuardandoLectura = false,
                    mensajeNotificacion = "Error al procesar lectura: ${e.localizedMessage}",
                    esError = true,
                )
            }
        }
    }

    fun consumirNotificacion() {
        _uiState.value = _uiState.value.copy(mensajeNotificacion = null)
    }

    class Factory(
        private val lecturaRepository: ILecturaRepository,
        private val inquilinoRepository: IInquilinoRepository,
        private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LecturasViewModel::class.java)) {
                return LecturasViewModel(lecturaRepository, inquilinoRepository, dispatcher) as T
            }
            throw IllegalArgumentException("ViewModel desconocido")
        }
    }
}
