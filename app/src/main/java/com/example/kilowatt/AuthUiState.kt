package com.example.kilowatt

sealed interface AuthUiState {
    object CargarSesion : AuthUiState
    object Cargando : AuthUiState
    object Autenticado : AuthUiState
    data class Error(val mensaje: String) : AuthUiState
    data class MensajeRecuperacion(val mensaje: String) : AuthUiState
}
