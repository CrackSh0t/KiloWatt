package com.example.kilowatt.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

object OutboxScheduler {
    fun programar(context: Context, uid: String) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInputData(androidx.work.workDataOf(SyncWorker.UID_KEY to uid))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "sync_outbox_$uid",
            ExistingWorkPolicy.KEEP,
            request,
        )
    }
}
