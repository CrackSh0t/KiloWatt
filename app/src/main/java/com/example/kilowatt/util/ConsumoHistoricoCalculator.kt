package com.example.kilowatt.util

import com.example.kilowatt.data.ConsumoHistoricoResult
import com.example.kilowatt.data.FacturaGeneral
import com.example.kilowatt.data.HistoricoSubmedidor
import com.example.kilowatt.data.Inquilino
import com.example.kilowatt.data.Lectura
import com.example.kilowatt.data.PuntoHistorico
import com.example.kilowatt.data.Submedidor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

object ConsumoHistoricoCalculator {

    /**
     * Procesa y agrupa el historial de consumo del inmueble y de cada submedidor de forma asíncrona,
     * ordenando cronológicamente mediante PeriodoDateUtils sin bloquear el hilo principal.
     */
    suspend fun procesarHistorico(
        facturas: List<FacturaGeneral>,
        lecturas: List<Lectura>,
        submedidores: List<Submedidor>,
        inquilinos: List<Inquilino>,
        dispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.Default,
    ): ConsumoHistoricoResult = withContext(dispatcher) {

        // 1. Puntos históricos del inmueble matriz (Facturas Generales)
        val puntosMatriz = facturas.map { factura ->
            val mesTrimmed = factura.mesPeriodo.trim()
            PuntoHistorico(
                mesPeriodo = mesTrimmed,
                fechaSort = PeriodoDateUtils.convertirAFechaOrdenable(mesTrimmed),
                consumoKwh = factura.kwhTotalesRecibo,
                montoSoles = factura.montoTotalSoles,
            )
        }.sortedWith(compareBy<PuntoHistorico> { it.fechaSort ?: Date(0) }.thenBy { it.mesPeriodo })

        // Métricas globales del recibo matriz
        val promedioKwhMensual = if (puntosMatriz.isNotEmpty()) {
            puntosMatriz.map { it.consumoKwh }.average()
        } else {
            0.0
        }

        val mesMayorConsumo = puntosMatriz.maxByOrNull { it.consumoKwh }?.mesPeriodo

        // 2. Puntos históricos agrupados por submedidor / inquilino
        val submedidoresUnicos = submedidores.distinctBy { it.idSubmedidor }

        val listaHistoricoSubmedidores = submedidoresUnicos.map { submedidor ->
            val inquilino = inquilinos.find { it.idInquilino == submedidor.idInquilinoTitular }
            val lecturasSub = lecturas.filter { it.idSubmedidor == submedidor.idSubmedidor }

            val puntosSub = lecturasSub.map { lectura ->
                val mesTrimmed = lectura.mesPeriodo.trim()
                PuntoHistorico(
                    mesPeriodo = mesTrimmed,
                    fechaSort = PeriodoDateUtils.convertirAFechaOrdenable(mesTrimmed),
                    consumoKwh = lectura.consumoKwh,
                    montoSoles = lectura.montoPagarSoles,
                )
            }.sortedWith(compareBy<PuntoHistorico> { it.fechaSort ?: Date(0) }.thenBy { it.mesPeriodo })

            val nombreInquilinoFinal = when {
                inquilino != null && inquilino.nombreCompleto.isNotBlank() -> inquilino.nombreCompleto
                submedidor.esAreaComun -> "Área Común"
                else -> "Sin Inquilino Asignado"
            }

            HistoricoSubmedidor(
                idSubmedidor = submedidor.idSubmedidor,
                nombreEspacio = submedidor.nombreEspacio.ifEmpty { "Espacio" },
                nombreInquilino = nombreInquilinoFinal,
                puntos = puntosSub,
            )
        }

        ConsumoHistoricoResult(
            puntosInmuebleMatriz = puntosMatriz,
            historicoPorSubmedidor = listaHistoricoSubmedidores,
            promedioKwhMensual = promedioKwhMensual,
            mesMayorConsumo = mesMayorConsumo,
        )
    }
}
