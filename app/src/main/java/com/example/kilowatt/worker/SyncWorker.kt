package com.example.kilowatt.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.kilowatt.data.PendingOperationDao
import com.example.kilowatt.data.PendingOperationEntity
import com.example.kilowatt.data.PendingOperationType
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val pendingOperationDao: PendingOperationDao,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val uid = resolveUid(inputData.getString(UID_KEY), auth.currentUser?.uid)
            ?: return@withContext Result.failure()
        if (auth.currentUser?.uid != uid) return@withContext Result.failure()

        val processingTimeoutTimestamp = System.currentTimeMillis() - PROCESSING_TIMEOUT_MS
        val operations = pendingOperationDao.obtenerPendientes(uid, processingTimeoutTimestamp)
        procesarOperaciones(operations, pendingOperationDao) { procesar(it, uid) }
    }

    private fun procesar(operation: PendingOperationEntity, uid: String) {
        val base = firestore.collection("usuarios").document(uid)
        val payload = JSONObject(operation.payloadJson)
        val reference = when (operation.entityType) {
            "INQUILINO" -> base.collection("inquilinos").document(operation.documentId)
            "SUBMEDIDOR" -> base.collection("submedidores").document(operation.documentId)
            "FACTURA" -> base.collection("facturas").document(operation.documentId)
            "LECTURA" -> base.collection("lecturas").document(operation.documentId)
            else -> throw IllegalArgumentException("Entidad de outbox no soportada: ${operation.entityType}")
        }
        when (operation.operationType) {
            PendingOperationType.DELETE -> Tasks.await(reference.delete())
            PendingOperationType.CREATE -> Tasks.await(reference.set(payload.toMap()))
            PendingOperationType.UPDATE -> {
                if (operation.entityType == "LECTURA") {
                    Tasks.await(reference.set(payload.toPaymentUpdateMap(), SetOptions.merge()))
                } else {
                    Tasks.await(reference.set(payload.toMap()))
                }
            }
        }
    }

    private fun JSONObject.toMap(): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        keys().forEach { key ->
            if (!isNull(key)) result[key] = get(key)
        }
        return result
    }

    private fun JSONObject.toPaymentUpdateMap(): Map<String, Any?> = mapOf(
        "estadoPago" to getString("estadoPago"),
        "fechaPago" to if (isNull("fechaPago")) null else getLong("fechaPago"),
        "metodoPago" to if (isNull("metodoPago")) null else getString("metodoPago"),
    )

    companion object {
        const val UID_KEY = "uid"
        const val PROCESSING_TIMEOUT_MS = 5 * 60 * 1000L

        internal fun resolveUid(inputUid: String?, authenticatedUid: String?): String? =
            inputUid ?: authenticatedUid

        internal suspend fun procesarOperaciones(
            operations: List<PendingOperationEntity>,
            dao: PendingOperationDao,
            procesar: suspend (PendingOperationEntity) -> Unit,
        ): Result {
            return try {
                operations.forEach { operation ->
                    dao.marcarIntento(
                        operation.id,
                        PendingOperationEntity.STATUS_PROCESSING,
                        null,
                    )
                    try {
                        procesar(operation)
                        dao.eliminar(operation.id)
                    } catch (e: IllegalArgumentException) {
                        dao.marcarIntento(
                            operation.id,
                            PendingOperationEntity.STATUS_FAILED,
                            e.localizedMessage,
                        )
                        return Result.failure()
                    } catch (e: Exception) {
                        dao.marcarIntento(
                            operation.id,
                            PendingOperationEntity.STATUS_PENDING,
                            e.localizedMessage,
                        )
                        return Result.retry()
                    }
                }
                Result.success()
            } catch (e: Exception) {
                android.util.Log.e("SyncWorker", "No se pudo procesar la outbox", e)
                Result.retry()
            }
        }
    }
}
