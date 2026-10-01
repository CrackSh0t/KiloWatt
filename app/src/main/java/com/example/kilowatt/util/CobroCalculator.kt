package com.example.kilowatt.util

import com.example.kilowatt.data.DetalleCobro
import com.example.kilowatt.data.FacturaGeneral
import com.example.kilowatt.data.Inquilino
import com.example.kilowatt.data.Lectura
import com.example.kilowatt.data.Submedidor

data class ResumenCobroResult(
    val montoTotalRecibo: Double,
    val kwhReciboPrincipal: Double,
    val sumaKwhSubmedidores: Double,
    val precioKwhPromedio: Double,
    val totalSolesAreaComun: Double,
    val cuotaAreaComunPorInquilino: Double,
    val detalles: List<DetalleCobro>
)

sealed class ValidacionLecturaResult {
    object Valido : ValidacionLecturaResult()
    data class Invalido(val mensaje: String) : ValidacionLecturaResult()
}

object CobroCalculator {

    /**
     * Valida que las lecturas del medidor cumplan con las reglas de negocio.
     */
    fun validarLecturas(lecturaAnterior: Double, lecturaActual: Double): ValidacionLecturaResult {
        if (lecturaAnterior < 0.0 || lecturaActual < 0.0) {
            return ValidacionLecturaResult.Invalido("Las lecturas no pueden ser valores negativos")
        }
        if (lecturaActual < lecturaAnterior) {
            return ValidacionLecturaResult.Invalido("La lectura actual no puede ser menor a la anterior")
        }
        return ValidacionLecturaResult.Valido
    }

    /**
     * Calcula el consumo y monto preliminar para una lectura individual.
     */
    fun calcularConsumoYMontoIndividual(
        lecturaAnterior: Double,
        lecturaActual: Double,
        montoTotalRecibo: Double,
        kwhTotalesRecibo: Double
    ): Pair<Double, Double> {
        val consumoKwh = (lecturaActual - lecturaAnterior).coerceAtLeast(0.0)
        val precioPorKwh = if (kwhTotalesRecibo > 0.0) montoTotalRecibo / kwhTotalesRecibo else 0.0
        val montoPagar = consumoKwh * precioPorKwh
        return Pair(consumoKwh, montoPagar)
    }

    /**
     * Calcula el prorrateo general mensual, aplicando la fórmula de distribución de consumo y cuotas de área común.
     */
    fun calcularResumenMensual(
        mes: String,
        factura: FacturaGeneral,
        submedidores: List<Submedidor>,
        lecturas: List<Lectura>,
        inquilinos: List<Inquilino>
    ): ResumenCobroResult {
        val submedidoresUnicos = submedidores.distinctBy { it.nombreEspacio.trim().lowercase() }
        val lecturasUnicas = lecturas.distinctBy { it.idSubmedidor }

        // 1. Suma total de kWh de TODOS los submedidores con lectura guardada
        val sumaKwhSubmedidores = submedidoresUnicos.sumOf { sub ->
            lecturasUnicas.find { it.idSubmedidor == sub.idSubmedidor }?.consumoKwh ?: 0.0
        }

        val montoTotalRecibo = factura.montoTotalSoles
        val kwhReciboPrincipal = factura.kwhTotalesRecibo

        // Indicador del precio promedio por kWh
        val precioKwhPromedio = if (sumaKwhSubmedidores > 0.0) {
            montoTotalRecibo / sumaKwhSubmedidores
        } else {
            0.0
        }

        // 2. Calcular Pago S/ de las Áreas Comunes
        val submedidoresAreaComun = submedidoresUnicos.filter { it.esAreaComun }
        var totalSolesAreaComun = 0.0

        submedidoresAreaComun.forEach { subComun ->
            val lecturaComun = lecturasUnicas.find { it.idSubmedidor == subComun.idSubmedidor }
            val consumoKwh = lecturaComun?.consumoKwh ?: 0.0
            val porcentajeConsumo = if (sumaKwhSubmedidores > 0.0) {
                consumoKwh / sumaKwhSubmedidores
            } else {
                0.0
            }
            val pagoSolesAreaComun = porcentajeConsumo * montoTotalRecibo
            totalSolesAreaComun += pagoSolesAreaComun
        }

        // 3. Dividir el costo del Área Común entre los inquilinos que pagan área común
        val submedidoresParticulares = submedidoresUnicos.filter { !it.esAreaComun }
        val pagadoresAreaComun = submedidoresParticulares.filter { it.pagaAreaComun }
        val cantidadPagadores = pagadoresAreaComun.size
        val cuotaAreaComunPorInquilino = if (cantidadPagadores > 0) {
            totalSolesAreaComun / cantidadPagadores
        } else {
            0.0
        }

        // 4. Generar el detalle para cada inquilino particular (Fórmula Excel)
        val listaDetalle = submedidoresParticulares.map { submedidor ->
            val lectura = lecturasUnicas.find { it.idSubmedidor == submedidor.idSubmedidor }
            val inquilino = inquilinos.find { it.idInquilino == submedidor.idInquilinoTitular }

            val consumoKwh = lectura?.consumoKwh ?: 0.0

            // FÓRMULA EXCEL: (Consumo kWh / Suma Total kWh) * Monto Total Recibo
            val porcentajeConsumo = if (sumaKwhSubmedidores > 0.0) {
                consumoKwh / sumaKwhSubmedidores
            } else {
                0.0
            }
            val pagoBaseSoles = porcentajeConsumo * montoTotalRecibo

            // Sumar la cuota del área común si aplica
            val cuotaComunAplicada = if (submedidor.pagaAreaComun) cuotaAreaComunPorInquilino else 0.0
            val totalFinalPagar = pagoBaseSoles + cuotaComunAplicada

            val nombreFinal = when {
                inquilino != null && inquilino.nombreCompleto.isNotBlank() -> inquilino.nombreCompleto
                submedidor.nombreEspacio.isNotBlank() -> submedidor.nombreEspacio
                else -> "Inquilino / Espacio"
            }

            DetalleCobro(
                nombreInquilino = nombreFinal,
                telefonoWhatsapp = inquilino?.telefonoWhatsapp ?: "",
                nombreEspacio = submedidor.nombreEspacio,
                mesPeriodo = mes,
                lecturaAnterior = lectura?.lecturaAnterior ?: 0.0,
                lecturaActual = lectura?.lecturaActual ?: 0.0,
                consumoKwh = consumoKwh,
                precioKwh = precioKwhPromedio,
                montoAreaComunSoles = cuotaComunAplicada,
                montoPagarSoles = totalFinalPagar
            )
        }

        return ResumenCobroResult(
            montoTotalRecibo = montoTotalRecibo,
            kwhReciboPrincipal = kwhReciboPrincipal,
            sumaKwhSubmedidores = sumaKwhSubmedidores,
            precioKwhPromedio = precioKwhPromedio,
            totalSolesAreaComun = totalSolesAreaComun,
            cuotaAreaComunPorInquilino = cuotaAreaComunPorInquilino,
            detalles = listaDetalle
        )
    }
}
