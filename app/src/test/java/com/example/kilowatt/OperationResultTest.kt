package com.example.kilowatt

import com.example.kilowatt.domain.OperationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OperationResultTest {
    @Test
    fun estadosRepresentanSincronizacionYErrorDeFormaExplicita() {
        val synced = OperationResult.Synced("dato")
        val pending = OperationResult.PendingSync("dato", "sin conexión")
        val exception = IllegalStateException("fallo")
        val failed = OperationResult.Failed<String>(exception, "No se pudo guardar")

        assertEquals("dato", synced.data)
        assertEquals("dato", pending.data)
        assertEquals("sin conexión", pending.reason)
        assertEquals(exception, failed.exception)
        assertEquals("No se pudo guardar", failed.message)
    }

    @Test
    fun pendingSyncPermiteMotivoAusente() {
        assertNull(OperationResult.PendingSync(Unit).reason)
    }
}
