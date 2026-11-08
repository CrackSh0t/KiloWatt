package com.example.kilowatt

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.kilowatt.data.AppDatabase
import com.example.kilowatt.data.PendingOperationDao
import com.example.kilowatt.data.PendingOperationEntity
import com.example.kilowatt.data.PendingOperationType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PendingOperationDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: PendingOperationDao

    @Before
    fun crearBaseEnMemoria() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = database.pendingOperationDao()
    }

    @After
    fun cerrarBase() {
        database.close()
    }

    @Test
    fun insertarYActualizarEstadoRecuperaProcessingObsoleto() = runBlocking {
        val operation = operation(timestamp = 1_000L)
        dao.insertar(operation)
        dao.marcarIntento(
            id = operation.id,
            status = PendingOperationEntity.STATUS_PROCESSING,
            errorReason = null,
            timestamp = 2_000L,
        )

        assertTrue(
            dao.obtenerPendientes(uid = operation.uid, processingTimeoutTimestamp = 1_999L).isEmpty(),
        )
        val recovered = dao.obtenerPendientes(
            uid = operation.uid,
            processingTimeoutTimestamp = 2_000L,
        )
        assertEquals(operation.id, recovered.single().id)
        assertEquals(PendingOperationEntity.STATUS_PROCESSING, recovered.single().status)
        assertEquals(1, recovered.single().attempts)
    }

    @Test
    fun eliminarQuitaLaOperacion() = runBlocking {
        val operation = operation()
        dao.insertar(operation)
        dao.eliminar(operation.id)

        assertTrue(
            dao.obtenerPendientes(
                uid = operation.uid,
                processingTimeoutTimestamp = Long.MAX_VALUE,
            ).isEmpty(),
        )
    }

    private fun operation(timestamp: Long = System.currentTimeMillis()) = PendingOperationEntity(
        id = "op-1",
        uid = "uid-1",
        entityType = "LECTURA",
        operationType = PendingOperationType.UPDATE,
        documentId = "lectura-1",
        payloadJson = """{"estadoPago":"PAGADO","fechaPago":null,"metodoPago":null}""",
        timestamp = timestamp,
    )
}
