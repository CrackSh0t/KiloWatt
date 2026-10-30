package com.example.kilowatt.util

interface ISettingsManager {
    val diaLimitePago: Int
    val nombrePropietario: String
    val numeroPago: String
    var tipoProrrateoComun: String
    fun limpiarConfiguracion()
}
