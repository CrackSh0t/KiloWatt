package com.example.kilowatt

import com.example.kilowatt.data.DetalleCobro
import com.example.kilowatt.data.FacturaGeneral

data class ResumenCobrosUiState(
    val estaCargando: Boolean = true,
    val listaMesesDisponibles: List<String> = emptyList(),
    val mesSeleccionado: String? = null,
    val facturaActual: FacturaGeneral? = null,
    val detalles: List<DetalleCobro> = emptyList(),
    val montoTotalRecibo: Double = 0.0,
    val kwhReciboPrincipal: Double = 0.0,
    val precioKwhPromedio: Double = 0.0,
    val mensajeError: String? = null,
)
