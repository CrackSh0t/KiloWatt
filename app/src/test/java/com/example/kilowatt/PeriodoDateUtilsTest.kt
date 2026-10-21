package com.example.kilowatt

import com.example.kilowatt.util.PeriodoDateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class PeriodoDateUtilsTest {

    @Test
    fun convertirAFechaOrdenable_formatoTextoEspanol_convierteCorrectamente() {
        val fecha = PeriodoDateUtils.convertirAFechaOrdenable("Agosto 2026")
        assertNotNull(fecha)

        val cal = Calendar.getInstance().apply { time = fecha!! }
        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(Calendar.AUGUST, cal.get(Calendar.MONTH))
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun convertirAFechaOrdenable_formatoNumerico_convierteCorrectamente() {
        val fecha1 = PeriodoDateUtils.convertirAFechaOrdenable("2026-08")
        assertNotNull(fecha1)

        val cal1 = Calendar.getInstance().apply { time = fecha1!! }
        assertEquals(2026, cal1.get(Calendar.YEAR))
        assertEquals(Calendar.AUGUST, cal1.get(Calendar.MONTH))

        val fecha2 = PeriodoDateUtils.convertirAFechaOrdenable("08/2026")
        assertNotNull(fecha2)

        val cal2 = Calendar.getInstance().apply { time = fecha2!! }
        assertEquals(2026, cal2.get(Calendar.YEAR))
        assertEquals(Calendar.AUGUST, cal2.get(Calendar.MONTH))
    }

    @Test
    fun convertirAFechaOrdenable_formatoInvalido_retornaNull() {
        val fecha = PeriodoDateUtils.convertirAFechaOrdenable("TextoInvalido")
        assertNull(fecha)
    }

    @Test
    fun formatearParaGrafico_etiquetaCorta_correcta() {
        val etiqueta = PeriodoDateUtils.formatearParaGrafico("Agosto 2026")
        assertEquals("Ago 26", etiqueta)
    }

    @Test
    fun obtenerFechaLimitePago_formatoEspanol_calculaCorrectamente() {
        val fecha = PeriodoDateUtils.obtenerFechaLimitePago("Agosto 2026", 5)
        assertNotNull(fecha)

        val cal = Calendar.getInstance().apply { time = fecha!! }
        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(Calendar.SEPTEMBER, cal.get(Calendar.MONTH))
        assertEquals(5, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun esFechaVencida_antesDeFechaLimite_retornaFalse() {
        val calAntes = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 28)
        }
        val esVencido = PeriodoDateUtils.esFechaVencida("Agosto 2026", 5, calAntes.time)
        assertFalse(esVencido)
    }

    @Test
    fun esFechaVencida_despuesDeFechaLimite_retornaTrue() {
        val calDespues = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 10)
        }
        val esVencido = PeriodoDateUtils.esFechaVencida("Agosto 2026", 5, calDespues.time)
        assertTrue(esVencido)
    }

    @Test
    fun formatearFechaLimite_formatoCorrecto() {
        val texto = PeriodoDateUtils.formatearFechaLimite("Agosto 2026", 5)
        assertEquals("05/09/2026", texto)
    }
}
