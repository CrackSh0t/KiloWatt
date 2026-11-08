package com.example.kilowatt

import androidx.work.ListenableWorker
import com.example.kilowatt.data.PendingOperationDao
import com.example.kilowatt.data.PendingOperationEntity
import com.example.kilowatt.data.PendingOperationType
import com.example.kilowatt.worker.SyncWorker
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class SyncWorkerTest {
    @Test
    fun worker_usaUidPersistidoDeLaOperacion() {
        assertEquals("usuario-1", SyncWorker.resolveUid("usuario-1", "usuario-actual"))
    }

    @Test
    fun worker_fallaCuandoNoHayUidAutenticado() {
        assertNull(SyncWorker.resolveUid(null, null))
    }

    @Test
    fun worker_recuperaProcessingDespuesDeCincoMinutos() {
        assertEquals(5 * 60 * 1000L, SyncWorker.PROCESSING_TIMEOUT_MS)
        assertTrue(SyncWorker.PROCESSING_TIMEOUT_MS > 0)
    }

    @Test
    fun worker_eliminaOperacionCuandoProcesaConExito() = runBlocking {
        val dao = FakePendingOperationDao()
        val operation = operation()

        val result = SyncWorker.procesarOperaciones(listOf(operation), dao) {}

        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(dao.eliminatedIds.contains(operation.id))
        assertEquals(PendingOperationEntity.STATUS_PROCESSING, dao.lastStatus)
    }

    @Test
    fun worker_reintentaYDevuelveOperacionAPendingAnteFalloTransitorio() = runBlocking {
        val dao = FakePendingOperationDao()
        val operation = operation()

        val result = SyncWorker.procesarOperaciones(
            listOf(operation),
            dao,
        ) { throw IllegalStateException("sin red") }

        assertEquals(ListenableWorker.Result.retry(), result)
        assertEquals(PendingOperationEntity.STATUS_PENDING, dao.lastStatus)
        assertTrue(dao.eliminatedIds.isEmpty())
    }

    @Test
    fun workerMarcaFailedAnteEntidadNoSoportada() = runBlocking {
        val dao = FakePendingOperationDao()
        val operation = operation()

        val result = SyncWorker.procesarOperaciones(
            listOf(operation),
            dao,
        ) { throw IllegalArgumentException("entidad inválida") }

        assertEquals(ListenableWorker.Result.failure(), result)
        assertEquals(PendingOperationEntity.STATUS_FAILED, dao.lastStatus)
    }

    private fun operation() = PendingOperationEntity(
        id = "op-1",
        uid = "uid-1",
        entityType = "LECTURA",
        operationType = PendingOperationType.UPDATE,
        documentId = "lectura-1",
        payloadJson = "{}",
        timestamp = 1L,
    )

    private class FakePendingOperationDao : PendingOperationDao {
        var lastStatus: String? = null
        val eliminatedIds = mutableListOf<String>()

        override suspend fun insertar(operation: PendingOperationEntity) = Unit

        override suspend fun obtenerPendientes(
            uid: String,
            processingTimeoutTimestamp: Long,
            limit: Int,
        ): List<PendingOperationEntity> = emptyList()

        override suspend fun eliminar(id: String) {
            eliminatedIds += id
        }

        override suspend fun marcarIntento(
            id: String,
            status: String,
            errorReason: String?,
            timestamp: Long,
        ) {
            lastStatus = status
        }

        override suspend fun eliminarPorUid(uid: String) = Unit

        override suspend fun existeOperacionPendiente(
            uid: String,
            entityType: String,
            documentId: String,
        ): Boolean = false
    }
}
