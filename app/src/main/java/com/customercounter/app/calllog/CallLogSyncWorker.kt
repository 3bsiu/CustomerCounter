package com.customercounter.app.calllog

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.customercounter.app.CustomerCounterApplication
import com.customercounter.app.data.CustomerRepository

class CallLogSyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as CustomerCounterApplication
            CallLogSyncManager(applicationContext, CustomerRepository(app.database)).sync()
            Result.success()
        } catch (_: SecurityException) {
            Result.success()
        } catch (_: Throwable) {
            Result.retry()
        }
    }
}
