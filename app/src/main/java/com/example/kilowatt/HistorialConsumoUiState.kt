package com.example.kilowatt

import com.example.kilowatt.data.ConsumoHistoricoResult

sealed interface HistorialConsumoUiState {
    object Cargando : HistorialConsumoUiState
    data class Exito(val resultado: ConsumoHistoricoResult) : HistorialConsumoUiState
    data class Error(val mensaje: String) : HistorialConsumoUiState
}
