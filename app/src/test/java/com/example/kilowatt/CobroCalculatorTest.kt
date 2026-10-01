package com.example.kilowatt

import com.example.kilowatt.data.FacturaGeneral
import com.example.kilowatt.data.Inquilino
import com.example.kilowatt.data.Lectura
import com.example.kilowatt.data.Submedidor
import com.example.kilowatt.util.CobroCalculator
import com.example.kilowatt.util.ValidacionLecturaResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CobroCalculatorTest {

    @Test
    fun validarLecturas_valoresValidos_retornaValido() {
        val resultado = CobroCalculator.validarLecturas(lecturaAnterior = 100.0, lecturaActual = 150.0)
        assertTrue(resultado is ValidacionLecturaResult.Valido)
    }

    @Test
    fun validarLecturas_lecturasIguales_retornaValido() {
        val resultado = CobroCalculator.validarLecturas(lecturaAnterior = 100.0, lecturaActual = 100.0)
        assertTrue(resultado is ValidacionLecturaResult.Valido)
    }

    @Test
    fun validarLecturas_actualMenorQueAnterior_retornaInvalido() {
        val resultado = CobroCalculator.validarLecturas(lecturaAnterior = 150.0, lecturaActual = 100.0)
        assertTrue(resultado is ValidacionLecturaResult.Invalido)
        assertEquals(
            "La lectura actual no puede ser menor a la anterior",
            (resultado as ValidacionLecturaResult.Invalido).mensaje
        )
    }

    @Test
    fun validarLecturas_valoresNegativos_retornaInvalido() {
        val resultado = CobroCalculator.validarLecturas(lecturaAnterior = -10.0, lecturaActual = 50.0)
        assertTrue(resultado is ValidacionLecturaResult.Invalido)
        assertEquals(
            "Las lecturas no pueden ser valores negativos",
            (resultado as ValidacionLecturaResult.Invalido).mensaje
        )
    }

    @Test
    fun calcularConsumoYMontoIndividual_calculoCorrecto() {
        val (consumo, monto) = CobroCalculator.calcularConsumoYMontoIndividual(
            lecturaAnterior = 100.0,
            lecturaActual = 150.0,
            montoTotalRecibo = 200.0,
            kwhTotalesRecibo = 200.0
        )
        assertEquals(50.0, consumo, 0.001)
        assertEquals(50.0, monto, 0.001) // 50 kWh * (200 / 200) S/ = 50.0
    }

    @Test
    fun calcularConsumoYMontoIndividual_reciboCeroKwh_noProduceDivisionPorCero() {
        val (consumo, monto) = CobroCalculator.calcularConsumoYMontoIndividual(
            lecturaAnterior = 100.0,
            lecturaActual = 150.0,
            montoTotalRecibo = 200.0,
            kwhTotalesRecibo = 0.0
        )
        assertEquals(50.0, consumo, 0.001)
        assertEquals(0.0, monto, 0.001)
    }

    @Test
    fun calcularResumenMensual_casoEstandarProrrateoConAreaComun() {
        val factura = FacturaGeneral(
            idFactura = "Agosto 2026",
            mesPeriodo = "Agosto 2026",
            montoTotalSoles = 120.0,
            kwhTotalesRecibo = 120.0
        )

        val inquilinoA = Inquilino(idInquilino = "inq1", nombreCompleto = "Juan Perez", telefonoWhatsapp = "987654321")
        val inquilinoB = Inquilino(idInquilino = "inq2", nombreCompleto = "Maria Lopez", telefonoWhatsapp = "912345678")

        val submedidorA = Submedidor(
            idSubmedidor = "sub1",
            nombreEspacio = "Depa 101",
            esAreaComun = false,
            pagaAreaComun = true,
            idInquilinoTitular = "inq1"
        )
        val submedidorB = Submedidor(
            idSubmedidor = "sub2",
            nombreEspacio = "Depa 102",
            esAreaComun = false,
            pagaAreaComun = false, // Maria no paga cuota de área común
            idInquilinoTitular = "inq2"
        )
        val submedidorComun = Submedidor(
            idSubmedidor = "subComun",
            nombreEspacio = "Bomba de Agua",
            esAreaComun = true,
            pagaAreaComun = false,
            idInquilinoTitular = null
        )

        val lecturas = listOf(
            Lectura(idLectura = "l1", idSubmedidor = "sub1", mesPeriodo = "Agosto 2026", lecturaAnterior = 100.0, lecturaActual = 160.0, consumoKwh = 60.0, montoPagarSoles = 60.0),
            Lectura(idLectura = "l2", idSubmedidor = "sub2", mesPeriodo = "Agosto 2026", lecturaAnterior = 200.0, lecturaActual = 240.0, consumoKwh = 40.0, montoPagarSoles = 40.0),
            Lectura(idLectura = "l3", idSubmedidor = "subComun", mesPeriodo = "Agosto 2026", lecturaAnterior = 0.0, lecturaActual = 20.0, consumoKwh = 20.0, montoPagarSoles = 20.0)
        )

        val resultado = CobroCalculator.calcularResumenMensual(
            mes = "Agosto 2026",
            factura = factura,
            submedidores = listOf(submedidorA, submedidorB, submedidorComun),
            lecturas = lecturas,
            inquilinos = listOf(inquilinoA, inquilinoB)
        )

        // Verificaciones globales
        assertEquals(120.0, resultado.montoTotalRecibo, 0.001)
        assertEquals(120.0, resultado.sumaKwhSubmedidores, 0.001) // 60 + 40 + 20 = 120 kWh
        assertEquals(1.0, resultado.precioKwhPromedio, 0.001) // S/ 120 / 120 kWh = 1.0 S//kWh
        assertEquals(20.0, resultado.totalSolesAreaComun, 0.001) // (20/120)*120 = S/ 20
        assertEquals(20.0, resultado.cuotaAreaComunPorInquilino, 0.001) // 1 pagador -> S/ 20

        // Verificación de detalles por inquilino
        assertEquals(2, resultado.detalles.size)

        val detalleA = resultado.detalles.find { it.nombreEspacio == "Depa 101" }!!
        assertEquals("Juan Perez", detalleA.nombreInquilino)
        assertEquals(60.0, detalleA.consumoKwh, 0.001)
        assertEquals(20.0, detalleA.montoAreaComunSoles, 0.001)
        assertEquals(80.0, detalleA.montoPagarSoles, 0.001) // 60 base + 20 común = 80

        val detalleB = resultado.detalles.find { it.nombreEspacio == "Depa 102" }!!
        assertEquals("Maria Lopez", detalleB.nombreInquilino)
        assertEquals(40.0, detalleB.consumoKwh, 0.001)
        assertEquals(0.0, detalleB.montoAreaComunSoles, 0.001)
        assertEquals(40.0, detalleB.montoPagarSoles, 0.001) // 40 base + 0 común = 40

        // La suma de cobros debe igualar el monto total de la factura
        assertEquals(120.0, detalleA.montoPagarSoles + detalleB.montoPagarSoles, 0.001)
    }

    @Test
    fun calcularResumenMensual_consumoTotalCero_evitaDivisionPorCero() {
        val factura = FacturaGeneral(
            idFactura = "Septiembre 2026",
            mesPeriodo = "Septiembre 2026",
            montoTotalSoles = 150.0,
            kwhTotalesRecibo = 0.0
        )

        val submedidor = Submedidor(
            idSubmedidor = "sub1",
            nombreEspacio = "Habitación 1",
            esAreaComun = false,
            pagaAreaComun = true
        )

        val resultado = CobroCalculator.calcularResumenMensual(
            mes = "Septiembre 2026",
            factura = factura,
            submedidores = listOf(submedidor),
            lecturas = emptyList(),
            inquilinos = emptyList()
        )

        assertEquals(0.0, resultado.sumaKwhSubmedidores, 0.001)
        assertEquals(0.0, resultado.precioKwhPromedio, 0.001)
        assertEquals(0.0, resultado.totalSolesAreaComun, 0.001)
        assertEquals(0.0, resultado.cuotaAreaComunPorInquilino, 0.001)
        assertEquals(0.0, resultado.detalles[0].montoPagarSoles, 0.001)
    }

    @Test
    fun calcularResumenMensual_sinPagadoresAreaComun_cuotaEsCero() {
        val factura = FacturaGeneral(
            idFactura = "Octubre 2026",
            mesPeriodo = "Octubre 2026",
            montoTotalSoles = 100.0,
            kwhTotalesRecibo = 100.0
        )

        val submedidorComun = Submedidor(
            idSubmedidor = "subComun",
            nombreEspacio = "Luces Pasillo",
            esAreaComun = true,
            pagaAreaComun = false
        )
        val submedidorParticular = Submedidor(
            idSubmedidor = "sub1",
            nombreEspacio = "Tienda",
            esAreaComun = false,
            pagaAreaComun = false // No paga área común
        )

        val lecturas = listOf(
            Lectura(idLectura = "l1", idSubmedidor = "subComun", consumoKwh = 10.0),
            Lectura(idLectura = "l2", idSubmedidor = "sub1", consumoKwh = 90.0)
        )

        val resultado = CobroCalculator.calcularResumenMensual(
            mes = "Octubre 2026",
            factura = factura,
            submedidores = listOf(submedidorComun, submedidorParticular),
            lecturas = lecturas,
            inquilinos = emptyList()
        )

        assertEquals(0.0, resultado.cuotaAreaComunPorInquilino, 0.001)
        assertEquals(0.0, resultado.detalles[0].montoAreaComunSoles, 0.001)
        assertEquals(90.0, resultado.detalles[0].montoPagarSoles, 0.001)
    }

    @Test
    fun calcularResumenMensual_inquilinoSinNombre_usaNombreEspacio() {
        val factura = FacturaGeneral(montoTotalSoles = 50.0, kwhTotalesRecibo = 50.0)
        val submedidor = Submedidor(
            idSubmedidor = "sub1",
            nombreEspacio = "Mini Depa 302",
            idInquilinoTitular = null
        )

        val resultado = CobroCalculator.calcularResumenMensual(
            mes = "Noviembre 2026",
            factura = factura,
            submedidores = listOf(submedidor),
            lecturas = emptyList(),
            inquilinos = emptyList()
        )

        assertEquals("Mini Depa 302", resultado.detalles[0].nombreInquilino)
    }
}
