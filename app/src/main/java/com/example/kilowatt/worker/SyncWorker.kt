package com.example.kilowatt.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Sincronización en segundo plano con Firestore
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
