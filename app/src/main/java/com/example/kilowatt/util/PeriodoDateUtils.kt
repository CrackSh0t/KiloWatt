package com.example.kilowatt.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object PeriodoDateUtils {

    private val mesesEspanol = mapOf(
        "enero" to Calendar.JANUARY,
        "febrero" to Calendar.FEBRUARY,
        "marzo" to Calendar.MARCH,
        "abril" to Calendar.APRIL,
        "mayo" to Calendar.MAY,
        "junio" to Calendar.JUNE,
        "julio" to Calendar.JULY,
        "agosto" to Calendar.AUGUST,
        "septiembre" to Calendar.SEPTEMBER,
        "setiembre" to Calendar.SEPTEMBER,
        "octubre" to Calendar.OCTOBER,
        "noviembre" to Calendar.NOVEMBER,
        "diciembre" to Calendar.DECEMBER,
    )

    private val mesesCortosEspanol = arrayOf(
        "Ene", "Feb", "Mar", "Abr", "May", "Jun",
        "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
    )

    /**
     * Parsea un 'mesPeriodo' en formato texto (ej. "Agosto 2026") o numérico (ej. "2026-08")
     * y retorna el objeto Date correspondiente al primer día del mes a las 00:00:00.
     */
    fun convertirAFechaOrdenable(mesPeriodo: String): Date? {
        if (mesPeriodo.isBlank()) return null

        val textoLimpio = mesPeriodo.trim().lowercase(Locale.getDefault())
            .replace("mes de", "")
            .replace("recibo", "")
            .trim()

        var mesEncontrado: Int? = null
        var anioEncontrado: Int? = null

        // 1. Buscar mes por nombre en español
        for ((nombreMes, idMes) in mesesEspanol) {
            if (textoLimpio.contains(nombreMes)) {
                mesEncontrado = idMes
                break
            }
        }

        // 2. Buscar año de 4 dígitos
        val regexAnio = Regex("\\b(20\\d{2})\\b")
        val matchAnio = regexAnio.find(textoLimpio)
        if (matchAnio != null) {
            anioEncontrado = matchAnio.value.toIntOrNull()
        }

        // 3. Si falta mes o año, probar formatos numéricos estándar
        if (mesEncontrado == null || anioEncontrado == null) {
            val formatosNumericos = arrayOf("yyyy-MM", "MM/yyyy", "MM-yyyy", "yyyy/MM")
            for (formato in formatosNumericos) {
                try {
                    val sdf = SimpleDateFormat(formato, Locale.getDefault())
                    sdf.isLenient = false
                    val fechaParsed = sdf.parse(textoLimpio)
                    if (fechaParsed != null) {
                        val calTmp = Calendar.getInstance().apply { time = fechaParsed }
                        mesEncontrado = calTmp.get(Calendar.MONTH)
                        anioEncontrado = calTmp.get(Calendar.YEAR)
                        break
                    }
                } catch (_: Exception) {
                    // Siguiente formato
                }
            }
        }

        if (mesEncontrado == null || anioEncontrado == null) {
            return null
        }

        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, anioEncontrado)
            set(Calendar.MONTH, mesEncontrado)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return cal.time
    }

    /**
     * Genera una etiqueta corta para el eje X de gráficos (ej. "Agosto 2026" -> "Ago 26").
     */
    fun formatearParaGrafico(mesPeriodo: String): String {
        val fecha = convertirAFechaOrdenable(mesPeriodo) ?: return mesPeriodo
        val cal = Calendar.getInstance().apply { time = fecha }
        val mesIdx = cal.get(Calendar.MONTH)
        val anioCorto = cal.get(Calendar.YEAR) % 100
        val nombreMesCorto = if (mesIdx in mesesCortosEspanol.indices) mesesCortosEspanol[mesIdx] else "Mes"
        return "$nombreMesCorto $anioCorto"
    }

    /**
     * Intenta obtener la fecha límite de pago para un 'mesPeriodo' y 'diaLimite'.
     */
    fun obtenerFechaLimitePago(mesPeriodo: String, diaLimite: Int): Date? {
        val fechaBase = convertirAFechaOrdenable(mesPeriodo) ?: return null
        val cal = Calendar.getInstance().apply {
            time = fechaBase
            add(Calendar.MONTH, 1) // Vencimiento al mes siguiente
        }

        val maxDiasEnMes = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val diaAjustado = diaLimite.coerceIn(1, maxDiasEnMes)

        cal.set(Calendar.DAY_OF_MONTH, diaAjustado)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)

        return cal.time
    }

    /**
     * Determina si la fecha actual ha superado la fecha límite del periodo.
     */
    fun esFechaVencida(
        mesPeriodo: String,
        diaLimite: Int,
        fechaActual: Date = Date(),
    ): Boolean {
        val fechaLimite = obtenerFechaLimitePago(mesPeriodo, diaLimite) ?: return false
        return fechaActual.after(fechaLimite)
    }

    /**
     * Formatea la fecha límite en un texto legible (ej. "05/09/2026").
     */
    fun formatearFechaLimite(mesPeriodo: String, diaLimite: Int): String? {
        val fechaLimite = obtenerFechaLimitePago(mesPeriodo, diaLimite) ?: return null
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(fechaLimite)
    }
}
