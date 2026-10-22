package com.example.kilowatt

import com.example.kilowatt.data.InquilinoConMedidor

data class ConfiguracionUiState(
    val listaInquilinos: List<InquilinoConMedidor> = emptyList(),
    val nombrePropietario: String = "",
    val numeroPago: String = "",
    val diaLimitePago: Int = 5,
    val estaGuardando: Boolean = false,
    val mensajeNotificacion: String? = null,
    val esError: Boolean = false,
)
