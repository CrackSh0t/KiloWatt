package com.example.kilowatt.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PendingOperationType { CREATE, UPDATE, DELETE }

@Entity(tableName = "pending_operations")
data class PendingOperationEntity(
    @PrimaryKey val id: String,
    val uid: String,
    val entityType: String,
    val operationType: PendingOperationType,
    val documentId: String,
    val payloadJson: String,
    val timestamp: Long,
    val attempts: Int = 0,
    val status: String = STATUS_PENDING,
    val errorReason: String? = null,
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_PROCESSING = "PROCESSING"
        const val STATUS_FAILED = "FAILED"
    }
}
