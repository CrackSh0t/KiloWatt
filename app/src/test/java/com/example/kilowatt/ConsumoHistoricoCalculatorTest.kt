package com.example.kilowatt

import com.example.kilowatt.data.FacturaGeneral
import com.example.kilowatt.data.Inquilino
import com.example.kilowatt.data.Lectura
import com.example.kilowatt.data.Submedidor
import com.example.kilowatt.util.ConsumoHistoricoCalculator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ConsumoHistoricoCalculatorTest {

    @Test
    fun procesarHistorico_ordenaPuntosCronologicamente() = runBlocking {
        val facturas = listOf(
            FacturaGeneral(idFactura = "f3", mesPeriodo = "Octubre 2026", kwhTotalesRecibo = 300.0, montoTotalSoles = 300.0),
            FacturaGeneral(idFactura = "f1", mesPeriodo = "Agosto 2026", kwhTotalesRecibo = 100.0, montoTotalSoles = 100.0),
            FacturaGeneral(idFactura = "f2", mesPeriodo = "Septiembre 2026", kwhTotalesRecibo = 200.0, montoTotalSoles = 200.0),
        )

        val resultado = ConsumoHistoricoCalculator.procesarHistorico(
            facturas = facturas,
            lecturas = emptyList(),
            submedidores = emptyList(),
            inquilinos = emptyList(),
        )

        val puntos = resultado.puntosInmuebleMatriz
        assertEquals(3, puntos.size)
        assertEquals("Agosto 2026", puntos[0].mesPeriodo)
        assertEquals("Septiembre 2026", puntos[1].mesPeriodo)
        assertEquals("Octubre 2026", puntos[2].mesPeriodo)
    }

    @Test
    fun procesarHistorico_calculaPromedioYMesMayorConsumo() = runBlocking {
        val facturas = listOf(
            FacturaGeneral(idFactura = "f1", mesPeriodo = "Agosto 2026", kwhTotalesRecibo = 100.0, montoTotalSoles = 100.0),
            FacturaGeneral(idFactura = "f2", mesPeriodo = "Septiembre 2026", kwhTotalesRecibo = 300.0, montoTotalSoles = 300.0),
        )

        val resultado = ConsumoHistoricoCalculator.procesarHistorico(
            facturas = facturas,
            lecturas = emptyList(),
            submedidores = emptyList(),
            inquilinos = emptyList(),
        )

        assertEquals(200.0, resultado.promedioKwhMensual, 0.001)
        assertEquals("Septiembre 2026", resultado.mesMayorConsumo)
    }

    @Test
    fun procesarHistorico_agrupaYOrdenaPorSubmedidor() = runBlocking {
        val submedidores = listOf(
            Submedidor(idSubmedidor = "sub1", nombreEspacio = "Depa 101", idInquilinoTitular = "inq1"),
        )

        val inquilinos = listOf(
            Inquilino(idInquilino = "inq1", nombreCompleto = "Juan Pérez"),
        )

        val lecturas = listOf(
            Lectura(idLectura = "l2", idSubmedidor = "sub1", mesPeriodo = "Septiembre 2026", consumoKwh = 150.0, montoPagarSoles = 150.0),
            Lectura(idLectura = "l1", idSubmedidor = "sub1", mesPeriodo = "Agosto 2026", consumoKwh = 80.0, montoPagarSoles = 80.0),
        )

        val resultado = ConsumoHistoricoCalculator.procesarHistorico(
            facturas = emptyList(),
            lecturas = lecturas,
            submedidores = submedidores,
            inquilinos = inquilinos,
        )

        assertEquals(1, resultado.historicoPorSubmedidor.size)
        val historicoSub = resultado.historicoPorSubmedidor[0]
        assertEquals("Depa 101", historicoSub.nombreEspacio)
        assertEquals("Juan Pérez", historicoSub.nombreInquilino)

        val puntosSub = historicoSub.puntos
        assertEquals(2, puntosSub.size)
        assertEquals("Agosto 2026", puntosSub[0].mesPeriodo)
        assertEquals(80.0, puntosSub[0].consumoKwh, 0.001)
        assertEquals("Septiembre 2026", puntosSub[1].mesPeriodo)
        assertEquals(150.0, puntosSub[1].consumoKwh, 0.001)
    }
}
