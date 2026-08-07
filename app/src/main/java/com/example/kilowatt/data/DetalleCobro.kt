package com.example.kilowatt.data

data class DetalleCobro(
    val nombreInquilino: String,
    val telefonoWhatsapp: String,
    val nombreEspacio: String,
    val mesPeriodo: String,
    val lecturaAnterior: Double,
    val lecturaActual: Double,
    val consumoKwh: Double,
    val precioKwh: Double,
    val montoAreaComunSoles: Double = 0.0,
    val montoPagarSoles: Double
)
