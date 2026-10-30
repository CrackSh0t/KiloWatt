package com.example.kilowatt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kilowatt.data.IInquilinoRepository
import com.example.kilowatt.data.Inquilino
import com.example.kilowatt.data.InquilinoConMedidor
import com.example.kilowatt.data.Submedidor
import com.example.kilowatt.util.ISettingsManager
import com.example.kilowatt.util.PhoneUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfiguracionViewModel @Inject constructor(
    private val repository: IInquilinoRepository,
    private val settingsManager: ISettingsManager,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ConfiguracionUiState(
            nombrePropietario = settingsManager.nombrePropietario,
            numeroPago = settingsManager.numeroPago,
            diaLimitePago = settingsManager.diaLimitePago,
        ),
    )
    val uiState: StateFlow<ConfiguracionUiState> = _uiState.asStateFlow()

    init {
        observarInquilinosYSubmedidores()
        observarPerfilPropietario()
    }

    private fun observarInquilinosYSubmedidores() {
        viewModelScope.launch {
            combine(
                repository.obtenerInquilinosFlow(),
                repository.obtenerSubmedidoresFlow(),
            ) { inquilinos, submedidores ->
                submedidores.map { sub ->
                    val inq = if (sub.esAreaComun) null else inquilinos.find { it.idInquilino == sub.idInquilinoTitular }
                    InquilinoConMedidor(inquilino = inq, submedidor = sub)
                }
            }.collect { lista ->
                _uiState.value = _uiState.value.copy(listaInquilinos = lista)
            }
        }
    }

    private fun observarPerfilPropietario() {
        viewModelScope.launch {
            repository.obtenerPerfilPropietarioFlow().collect { datos ->
                if (datos != null) {
                    val nombre = datos["nombrePropietario"]?.toString() ?: ""
                    val numero = datos["numeroPago"]?.toString() ?: ""
                    val diaLimite = (datos["diaLimitePago"] as? Long)?.toInt()
                        ?: (datos["diaLimitePago"] as? Int) ?: 5

                    _uiState.value = _uiState.value.copy(
                        nombrePropietario = nombre.ifEmpty { _uiState.value.nombrePropietario },
                        numeroPago = numero.ifEmpty { _uiState.value.numeroPago },
                        diaLimitePago = diaLimite,
                    )
                }
            }
        }
    }

    fun guardarDatosInquilino(
        nombre: String,
        telefono: String,
        nombreEspacio: String,
        esAreaComun: Boolean,
        pagaAreaComun: Boolean,
        numeroPersonas: Int = 1,
    ) {
        val espacioLimpio = nombreEspacio.trim()
        val nombreLimpio = nombre.trim()
        val telLimpio = telefono.trim()
        val personasSanitizadas = numeroPersonas.coerceAtLeast(1)

        if (espacioLimpio.isEmpty()) {
            _uiState.value = _uiState.value.copy(mensajeNotificacion = "Por favor ingresa el nombre del espacio", esError = true)
            return
        }

        if (!esAreaComun && nombreLimpio.isEmpty()) {
            _uiState.value = _uiState.value.copy(mensajeNotificacion = "Ingresa el nombre del inquilino", esError = true)
            return
        }

        val telefonoNormalizado = if (!esAreaComun && telLimpio.isNotEmpty()) {
            val (valido, normalizado) = PhoneUtils.formatearYValidarWhatsApp(telLimpio)
            if (!valido) {
                _uiState.value = _uiState.value.copy(
                    mensajeNotificacion = "Número de WhatsApp inválido (Formato E.164, ej: +51987654321 o 987654321)",
                    esError = true,
                )
                return
            }
            normalizado
        } else {
            ""
        }

        _uiState.value = _uiState.value.copy(estaGuardando = true)

        viewModelScope.launch {
            try {
                val (exito, error) = repository.guardarInquilinoYSubmedidor(
                    nombreCompleto = if (esAreaComun) "Área Común" else nombreLimpio,
                    telefono = telefonoNormalizado,
                    nombreEspacio = espacioLimpio,
                    esAreaComun = esAreaComun,
                    pagaAreaComun = if (esAreaComun) false else pagaAreaComun,
                    numeroPersonas = if (esAreaComun) 1 else personasSanitizadas,
                )

                if (exito) {
                    _uiState.value = _uiState.value.copy(
                        estaGuardando = false,
                        mensajeNotificacion = "¡Registrado en la nube con éxito!",
                        esError = false,
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        estaGuardando = false,
                        mensajeNotificacion = "Error al guardar en Firestore: $error",
                        esError = true,
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    estaGuardando = false,
                    mensajeNotificacion = "Error al guardar: ${e.localizedMessage}",
                    esError = true,
                )
            }
        }
    }

    fun guardarDatosPropietario(nombre: String, numero: String, diaStr: String) {
        val nLimpio = nombre.trim()
        val numLimpio = numero.trim()
        val dia = diaStr.trim().toIntOrNull() ?: 5

        if (nLimpio.isEmpty() || numLimpio.isEmpty()) {
            _uiState.value = _uiState.value.copy(mensajeNotificacion = "Por favor completa los datos del propietario", esError = true)
            return
        }

        _uiState.value = _uiState.value.copy(estaGuardando = true)

        viewModelScope.launch {
            try {
                val tipoProrrateo = settingsManager.tipoProrrateoComun
                val (exito, error) = repository.guardarPerfilPropietario(
                    nombre = nLimpio,
                    numero = numLimpio,
                    diaLimite = dia,
                    tipoProrrateoComun = tipoProrrateo,
                )
                if (exito) {
                    _uiState.value = _uiState.value.copy(
                        estaGuardando = false,
                        nombrePropietario = nLimpio,
                        numeroPago = numLimpio,
                        diaLimitePago = dia,
                        mensajeNotificacion = "Datos del propietario guardados con éxito",
                        esError = false,
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        estaGuardando = false,
                        mensajeNotificacion = "Guardado localmente. Error en nube: $error",
                        esError = true,
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    estaGuardando = false,
                    mensajeNotificacion = "Guardado localmente. Error: ${e.localizedMessage}",
                    esError = true,
                )
            }
        }
    }

    fun actualizarInquilinoYSubmedidor(inquilino: Inquilino?, submedidor: Submedidor, onResult: (Boolean, String?) -> Unit) {
        repository.actualizarInquilinoYSubmedidor(inquilino, submedidor, onResult)
    }

    fun eliminarInquilinoYSubmedidor(inquilino: Inquilino?, submedidor: Submedidor, onResult: (Boolean, String?) -> Unit) {
        repository.eliminarInquilinoYSubmedidor(inquilino, submedidor, onResult)
    }

    fun consumirNotificacion() {
        _uiState.value = _uiState.value.copy(mensajeNotificacion = null)
    }

    class Factory(
        private val repository: IInquilinoRepository,
        private val settingsManager: ISettingsManager,
        private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ConfiguracionViewModel::class.java)) {
                return ConfiguracionViewModel(repository, settingsManager, dispatcher) as T
            }
            throw IllegalArgumentException("ViewModel desconocido")
        }
    }
}
