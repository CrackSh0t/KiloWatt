package com.example.kilowatt.data

import java.util.Date

data class PuntoHistorico(
    val mesPeriodo: String,
    val fechaSort: Date?,
    val consumoKwh: Double,
    val montoSoles: Double,
)

data class HistoricoSubmedidor(
    val idSubmedidor: String,
    val nombreEspacio: String,
    val nombreInquilino: String,
    val puntos: List<PuntoHistorico>,
)

data class ConsumoHistoricoResult(
    val puntosInmuebleMatriz: List<PuntoHistorico>,
    val historicoPorSubmedidor: List<HistoricoSubmedidor>,
    val promedioKwhMensual: Double,
    val mesMayorConsumo: String?,
)
