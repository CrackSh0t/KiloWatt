package com.example.kilowatt

import com.example.kilowatt.data.FacturaGeneral
import com.example.kilowatt.data.Inquilino
import com.example.kilowatt.data.Lectura
import com.example.kilowatt.data.Submedidor
import com.example.kilowatt.data.TipoProrrateoComun
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

    @Test
    fun calcularResumenMensual_weightedPersons_prorrateoProporcionalCorrecto() {
        // Factura: S/ 180.0 para 180.0 kWh (S/ 1.0 por kWh)
        val factura = FacturaGeneral(
            idFactura = "Diciembre 2026",
            mesPeriodo = "Diciembre 2026",
            montoTotalSoles = 180.0,
            kwhTotalesRecibo = 180.0
        )

        // Inquilino A: 4 ocupantes, Inquilino B: 2 ocupantes
        val inqA = Inquilino(idInquilino = "inqA", nombreCompleto = "Familia Gomez", numeroPersonas = 4)
        val inqB = Inquilino(idInquilino = "inqB", nombreCompleto = "Carlos Ruiz", numeroPersonas = 2)

        val subA = Submedidor(
            idSubmedidor = "subA",
            nombreEspacio = "Depa 201",
            esAreaComun = false,
            pagaAreaComun = true,
            idInquilinoTitular = "inqA"
        )
        val subB = Submedidor(
            idSubmedidor = "subB",
            nombreEspacio = "Depa 202",
            esAreaComun = false,
            pagaAreaComun = true,
            idInquilinoTitular = "inqB"
        )
        val subComun = Submedidor(
            idSubmedidor = "subComun",
            nombreEspacio = "Bomba y Pasillos",
            esAreaComun = true,
            pagaAreaComun = false,
            idInquilinoTitular = null
        )

        // Consumos: Depa 201 = 60 kWh, Depa 202 = 60 kWh, Área común = 60 kWh -> Total 180 kWh
        val lecturas = listOf(
            Lectura(idLectura = "lA", idSubmedidor = "subA", consumoKwh = 60.0),
            Lectura(idLectura = "lB", idSubmedidor = "subB", consumoKwh = 60.0),
            Lectura(idLectura = "lC", idSubmedidor = "subComun", consumoKwh = 60.0)
        )

        val resultado = CobroCalculator.calcularResumenMensual(
            mes = "Diciembre 2026",
            factura = factura,
            submedidores = listOf(subA, subB, subComun),
            lecturas = lecturas,
            inquilinos = listOf(inqA, inqB),
            tipoProrrateoComun = TipoProrrateoComun.WEIGHTED_PERSONS
        )

        // Verificación de totales globales
        assertEquals(180.0, resultado.montoTotalRecibo, 0.001)
        assertEquals(180.0, resultado.sumaKwhSubmedidores, 0.001)
        assertEquals(60.0, resultado.totalSolesAreaComun, 0.001)
        // 60 S/ común / (4 + 2 personas) = 10.0 S/ por persona
        assertEquals(10.0, resultado.cuotaAreaComunPorInquilino, 0.001)

        val detalleA = resultado.detalles.find { it.idSubmedidor == "subA" }!!
        val detalleB = resultado.detalles.find { it.idSubmedidor == "subB" }!!

        // A (4 personas): Cuota común = 4 * 10 = 40 S/. Consumo propio = 60 S/. Total = 100 S/
        assertEquals(40.0, detalleA.montoAreaComunSoles, 0.001)
        assertEquals(100.0, detalleA.montoPagarSoles, 0.001)

        // B (2 personas): Cuota común = 2 * 10 = 20 S/. Consumo propio = 60 S/. Total = 80 S/
        assertEquals(20.0, detalleB.montoAreaComunSoles, 0.001)
        assertEquals(80.0, detalleB.montoPagarSoles, 0.001)

        // Principio de conservación: Total pagado por inquilinos = Monto total de la factura
        assertEquals(180.0, detalleA.montoPagarSoles + detalleB.montoPagarSoles, 0.001)
    }

    @Test
    fun calcularResumenMensual_weightedPersons_fallbackDefensivoConInquilinoNullOInvalido() {
        val factura = FacturaGeneral(montoTotalSoles = 150.0, kwhTotalesRecibo = 150.0)

        // Inquilino con personas = 0 o negativo (debe sanitizarse a 1)
        val inqInvalido = Inquilino(idInquilino = "inq1", nombreCompleto = "Pedro", numeroPersonas = -2)
        val inqValido = Inquilino(idInquilino = "inq3", nombreCompleto = "Ana", numeroPersonas = 3)

        val sub1 = Submedidor(idSubmedidor = "sub1", nombreEspacio = "Hab 1", pagaAreaComun = true, idInquilinoTitular = "inq1")
        val sub2 = Submedidor(idSubmedidor = "sub2", nombreEspacio = "Hab 2", pagaAreaComun = true, idInquilinoTitular = null) // Inquilino null -> fallback 1 persona
        val sub3 = Submedidor(idSubmedidor = "sub3", nombreEspacio = "Hab 3", pagaAreaComun = true, idInquilinoTitular = "inq3")
        val subComun = Submedidor(idSubmedidor = "subC", nombreEspacio = "Luz Común", esAreaComun = true, pagaAreaComun = false)

        val lecturas = listOf(
            Lectura(idLectura = "l1", idSubmedidor = "sub1", consumoKwh = 30.0),
            Lectura(idLectura = "l2", idSubmedidor = "sub2", consumoKwh = 30.0),
            Lectura(idLectura = "l3", idSubmedidor = "sub3", consumoKwh = 40.0),
            Lectura(idLectura = "lC", idSubmedidor = "subC", consumoKwh = 50.0)
        )

        val resultado = CobroCalculator.calcularResumenMensual(
            mes = "Enero 2027",
            factura = factura,
            submedidores = listOf(sub1, sub2, sub3, subComun),
            lecturas = lecturas,
            inquilinos = listOf(inqInvalido, inqValido),
            tipoProrrateoComun = TipoProrrateoComun.WEIGHTED_PERSONS
        )

        // Personas esperadas: 1 (sanitizado de -2) + 1 (fallback de null) + 3 = 5 personas
        // Área común = 50 S/. Costo por persona = 50 / 5 = 10.0 S/
        assertEquals(50.0, resultado.totalSolesAreaComun, 0.001)
        assertEquals(10.0, resultado.cuotaAreaComunPorInquilino, 0.001)

        val d1 = resultado.detalles.find { it.idSubmedidor == "sub1" }!!
        val d2 = resultado.detalles.find { it.idSubmedidor == "sub2" }!!
        val d3 = resultado.detalles.find { it.idSubmedidor == "sub3" }!!

        assertEquals(10.0, d1.montoAreaComunSoles, 0.001) // 1 persona * 10
        assertEquals(10.0, d2.montoAreaComunSoles, 0.001) // 1 persona * 10
        assertEquals(30.0, d3.montoAreaComunSoles, 0.001) // 3 personas * 10

        // Suma de cobros particulares = 150.0
        val sumaTotal = d1.montoPagarSoles + d2.montoPagarSoles + d3.montoPagarSoles
        assertEquals(150.0, sumaTotal, 0.001)
    }

    @Test
    fun calcularResumenMensual_equityExplicit_mantienePartesIgualesIndependienteDePersonas() {
        val factura = FacturaGeneral(montoTotalSoles = 180.0, kwhTotalesRecibo = 180.0)

        // Inquilino A tiene 4 personas e Inquilino B tiene 2 personas
        val inqA = Inquilino(idInquilino = "inqA", nombreCompleto = "Familia Gomez", numeroPersonas = 4)
        val inqB = Inquilino(idInquilino = "inqB", nombreCompleto = "Carlos Ruiz", numeroPersonas = 2)

        val subA = Submedidor(idSubmedidor = "subA", nombreEspacio = "Depa 201", pagaAreaComun = true, idInquilinoTitular = "inqA")
        val subB = Submedidor(idSubmedidor = "subB", nombreEspacio = "Depa 202", pagaAreaComun = true, idInquilinoTitular = "inqB")
        val subComun = Submedidor(idSubmedidor = "subComun", nombreEspacio = "Común", esAreaComun = true, pagaAreaComun = false)

        val lecturas = listOf(
            Lectura(idLectura = "lA", idSubmedidor = "subA", consumoKwh = 60.0),
            Lectura(idLectura = "lB", idSubmedidor = "subB", consumoKwh = 60.0),
            Lectura(idLectura = "lC", idSubmedidor = "subComun", consumoKwh = 60.0)
        )

        val resultado = CobroCalculator.calcularResumenMensual(
            mes = "Diciembre 2026",
            factura = factura,
            submedidores = listOf(subA, subB, subComun),
            lecturas = lecturas,
            inquilinos = listOf(inqA, inqB),
            tipoProrrateoComun = TipoProrrateoComun.EQUITY
        )

        // En modalidad EQUITY, 60 S/ común se divide entre 2 pagadores = 30 S/ cada uno
        assertEquals(30.0, resultado.cuotaAreaComunPorInquilino, 0.001)

        val dA = resultado.detalles.find { it.idSubmedidor == "subA" }!!
        val dB = resultado.detalles.find { it.idSubmedidor == "subB" }!!

        assertEquals(30.0, dA.montoAreaComunSoles, 0.001)
        assertEquals(30.0, dB.montoAreaComunSoles, 0.001)
        assertEquals(90.0, dA.montoPagarSoles, 0.001)
        assertEquals(90.0, dB.montoPagarSoles, 0.001)
        assertEquals(180.0, dA.montoPagarSoles + dB.montoPagarSoles, 0.001)
    }

    @Test
    fun calcularResumenMensual_weightedPersons_sinAreaComunOSinConsumo_noLanzaExcepcion() {
        val factura = FacturaGeneral(montoTotalSoles = 100.0, kwhTotalesRecibo = 100.0)
        val inq = Inquilino(idInquilino = "inq1", nombreCompleto = "Solo", numeroPersonas = 2)
        val sub = Submedidor(idSubmedidor = "sub1", nombreEspacio = "Local", pagaAreaComun = true, idInquilinoTitular = "inq1")

        // Sin submedidor de área común
        val lecturas = listOf(Lectura(idLectura = "l1", idSubmedidor = "sub1", consumoKwh = 100.0))

        val resultado = CobroCalculator.calcularResumenMensual(
            mes = "Febrero 2027",
            factura = factura,
            submedidores = listOf(sub),
            lecturas = lecturas,
            inquilinos = listOf(inq),
            tipoProrrateoComun = TipoProrrateoComun.WEIGHTED_PERSONS
        )

        assertEquals(0.0, resultado.totalSolesAreaComun, 0.001)
        assertEquals(0.0, resultado.cuotaAreaComunPorInquilino, 0.001)
        assertEquals(0.0, resultado.detalles[0].montoAreaComunSoles, 0.001)
        assertEquals(100.0, resultado.detalles[0].montoPagarSoles, 0.001)
    }
}
