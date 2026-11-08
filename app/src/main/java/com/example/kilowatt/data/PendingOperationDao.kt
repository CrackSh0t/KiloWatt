package com.example.kilowatt.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PendingOperationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(operation: PendingOperationEntity)

    @Query(
        """
        SELECT * FROM pending_operations
        WHERE uid = :uid
          AND (
              status = 'PENDING'
              OR (status = 'PROCESSING' AND timestamp <= :processingTimeoutTimestamp)
          )
        ORDER BY timestamp ASC
        LIMIT :limit
        """,
    )
    suspend fun obtenerPendientes(
        uid: String,
        processingTimeoutTimestamp: Long,
        limit: Int = 50,
    ): List<PendingOperationEntity>

    @Query("DELETE FROM pending_operations WHERE id = :id")
    suspend fun eliminar(id: String)

    @Query(
        "UPDATE pending_operations SET status = :status, attempts = attempts + 1, timestamp = :timestamp, errorReason = :errorReason WHERE id = :id",
    )
    suspend fun marcarIntento(
        id: String,
        status: String,
        errorReason: String?,
        timestamp: Long = System.currentTimeMillis(),
    )

    @Query("DELETE FROM pending_operations WHERE uid = :uid")
    suspend fun eliminarPorUid(uid: String)

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM pending_operations
            WHERE uid = :uid
              AND entityType = :entityType
              AND documentId = :documentId
              AND status IN ('PENDING', 'PROCESSING', 'FAILED')
        )
        """,
    )
    suspend fun existeOperacionPendiente(
        uid: String,
        entityType: String,
        documentId: String,
    ): Boolean
}
