package com.example.kilowatt

import com.example.kilowatt.data.Lectura
import com.example.kilowatt.data.Submedidor

data class LecturasUiState(
    val mesPeriodo: String = "",
    val estaGuardandoReciboBase: Boolean = false,
    val estaGuardandoLectura: Boolean = false,
    val submedidoresDisponibles: List<Submedidor> = emptyList(),
    val ultimaLecturaSubmedidor: Lectura? = null,
    val mensajeNotificacion: String? = null,
    val esError: Boolean = false,
)
