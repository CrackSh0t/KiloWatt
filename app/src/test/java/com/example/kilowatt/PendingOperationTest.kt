package com.example.kilowatt

import com.example.kilowatt.data.PendingOperationEntity
import com.example.kilowatt.data.PendingOperationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingOperationTest {
    @Test
    fun entity_generaOperacionConUidPayloadYEstadoPendiente() {
        val operation = PendingOperationEntity(
            id = "op-1",
            uid = "usuario-1",
            entityType = "INQUILINO",
            operationType = PendingOperationType.CREATE,
            documentId = "inq-1",
            payloadJson = """{"idInquilino":"inq-1","nombreCompleto":"Ana"}""",
            timestamp = 1L,
        )

        assertEquals("usuario-1", operation.uid)
        assertEquals(PendingOperationType.CREATE, operation.operationType)
        assertEquals(PendingOperationEntity.STATUS_PENDING, operation.status)
        assertTrue(operation.payloadJson.contains("\"idInquilino\":\"inq-1\""))
    }
}
