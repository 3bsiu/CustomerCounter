package com.customercounter.app

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.customercounter.app.calllog.CallLogSyncWorker
import com.customercounter.app.data.CustomerDatabase
import java.util.concurrent.TimeUnit

class CustomerCounterApplication : Application() {
    val database by lazy { CustomerDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        scheduleReconciliation()
    }

    private fun scheduleReconciliation() {
        val request = PeriodicWorkRequestBuilder<CallLogSyncWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "call_log_reconciliation",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
