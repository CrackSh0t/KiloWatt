package com.example.kilowatt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kilowatt.data.IInquilinoRepository
import com.example.kilowatt.data.ILecturaRepository
import com.example.kilowatt.util.ConsumoHistoricoCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistorialConsumoViewModel @Inject constructor(
    private val lecturaRepository: ILecturaRepository,
    private val inquilinoRepository: IInquilinoRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HistorialConsumoUiState>(HistorialConsumoUiState.Cargando)
    val uiState: StateFlow<HistorialConsumoUiState> = _uiState.asStateFlow()

    init {
        cargarHistorico()
    }

    fun cargarHistorico() {
        viewModelScope.launch {
            _uiState.value = HistorialConsumoUiState.Cargando
            combine(
                lecturaRepository.obtenerFacturasFlow(),
                lecturaRepository.obtenerTodasLasLecturasFlow(),
                inquilinoRepository.obtenerSubmedidoresFlow(),
                inquilinoRepository.obtenerInquilinosFlow(),
            ) { facturas, lecturas, submedidores, inquilinos ->
                val resultado = ConsumoHistoricoCalculator.procesarHistorico(
                    facturas = facturas,
                    lecturas = lecturas,
                    submedidores = submedidores,
                    inquilinos = inquilinos,
                    dispatcher = dispatcher,
                )
                HistorialConsumoUiState.Exito(resultado)
            }.catch { e ->
                _uiState.value = HistorialConsumoUiState.Error(e.localizedMessage ?: "Error al cargar historial")
            }.collect { estadoExito ->
                _uiState.value = estadoExito
            }
        }
    }
}
