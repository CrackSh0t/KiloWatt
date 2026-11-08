package com.example.kilowatt.domain

sealed interface OperationResult<out T> {
    data class Synced<T>(val data: T) : OperationResult<T>

    data class PendingSync<T>(
        val data: T,
        val reason: String? = null,
    ) : OperationResult<T>

    data class Failed<T>(
        val exception: Throwable,
        val message: String,
    ) : OperationResult<T>
}
