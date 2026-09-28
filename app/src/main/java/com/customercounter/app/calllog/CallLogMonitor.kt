package com.customercounter.app.calllog

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.database.ContentObserver
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.atomic.AtomicBoolean

object CallLogMonitor {
    private val started = AtomicBoolean(false)

    fun start(context: Context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) return
        if (!started.compareAndSet(false, true)) return
        context.contentResolver.registerContentObserver(
            CallLog.Calls.CONTENT_URI,
            true,
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<CallLogSyncWorker>().build())
                }
            }
        )
    }
}
