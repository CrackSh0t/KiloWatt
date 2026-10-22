package com.example.kilowatt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kilowatt.data.FacturaGeneral
import com.example.kilowatt.data.IInquilinoRepository
import com.example.kilowatt.data.ILecturaRepository
import com.example.kilowatt.data.Lectura
import com.example.kilowatt.data.Submedidor
import com.example.kilowatt.util.CobroCalculator
import com.example.kilowatt.util.ValidacionLecturaResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
            inquilinoRepository.obtenerSubmedidoresFlow().collect { lista ->
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
            try {
                val lecturas = lecturaRepository.obtenerLecturasPorMesFlow(mesLimpio).first()
                val lecturaPrev = lecturas.find { it.idSubmedidor == submedidor.idSubmedidor }
                _uiState.value = _uiState.value.copy(ultimaLecturaSubmedidor = lecturaPrev)
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(ultimaLecturaSubmedidor = null)
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
            val (exito, error) = lecturaRepository.guardarFactura(factura)
            _uiState.value = if (exito) {
                _uiState.value.copy(
                    estaGuardandoReciboBase = false,
                    mensajeNotificacion = "¡Recibo Base guardado para $mesLimpio!",
                    esError = false,
                )
            } else {
                _uiState.value.copy(
                    estaGuardandoReciboBase = false,
                    mensajeNotificacion = "Error al guardar recibo: $error",
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
                )

                val (exito, error) = lecturaRepository.guardarLectura(nuevaLectura)
                if (exito) {
                    val msj = "Guardado: %.1f kWh | Total: S/ %.2f".format(consumoKwh, montoPagar)
                    _uiState.value = _uiState.value.copy(
                        estaGuardandoLectura = false,
                        mensajeNotificacion = msj,
                        esError = false,
                        ultimaLecturaSubmedidor = nuevaLectura,
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        estaGuardandoLectura = false,
                        mensajeNotificacion = "Error al guardar lectura: $error",
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
