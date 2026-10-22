package com.example.kilowatt

import com.example.kilowatt.util.PhoneUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneUtilsTest {

    @Test
    fun formatearYValidarWhatsApp_numeroCelularPeruanoSinCodigo_retornaE164Valido() {
        val (valido, resultado) = PhoneUtils.formatearYValidarWhatsApp("987654321")
        assertTrue(valido)
        assertEquals("+51987654321", resultado)
    }

    @Test
    fun formatearYValidarWhatsApp_numeroConEspaciosYGuiones_limpiaYValida() {
        val (valido, resultado) = PhoneUtils.formatearYValidarWhatsApp(" 987 - 654 - 321 ")
        assertTrue(valido)
        assertEquals("+51987654321", resultado)
    }

    @Test
    fun formatearYValidarWhatsApp_numeroConMas51_respetaFormato() {
        val (valido, resultado) = PhoneUtils.formatearYValidarWhatsApp("+51987654321")
        assertTrue(valido)
        assertEquals("+51987654321", resultado)
    }

    @Test
    fun formatearYValidarWhatsApp_numeroVacio_retornaInvalido() {
        val (valido, _) = PhoneUtils.formatearYValidarWhatsApp("")
        assertFalse(valido)
    }
}
