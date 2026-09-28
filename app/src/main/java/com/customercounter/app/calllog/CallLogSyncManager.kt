package com.customercounter.app.calllog

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import androidx.core.content.ContextCompat
import com.customercounter.app.data.CustomerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CallLogSyncManager(
    private val context: Context,
    private val repository: CustomerRepository
) {
    data class Result(val processed: Int, val created: Int, val skipped: Int)

    suspend fun sync(): Result = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            return@withContext Result(0, 0, 0)
        }

        val state = (context.applicationContext as com.customercounter.app.CustomerCounterApplication)
            .database.appStateDao().get()
        val fullImport = state == null || repository.count() == 0L
        val lowerBound = if (fullImport) 0L else maxOf(state?.ignoreCallsBefore ?: 0L, (state?.lastSyncTimestamp ?: 0L) - 24L * 60L * 60L * 1000L)

        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.DATE
        )
        val selection = if (fullImport) null else "${CallLog.Calls.DATE} >= ?"
        val args = if (fullImport) null else arrayOf(lowerBound.toString())
        var processedCount = 0
        var createdEstimate = 0
        var skipped = 0

        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            selection,
            args,
            "${CallLog.Calls.DATE} ASC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(CallLog.Calls._ID)
            val numberIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val dateIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
            while (cursor.moveToNext()) {
                val callId = cursor.getLong(idIndex)
                val raw = cursor.getString(numberIndex)
                val timestamp = cursor.getLong(dateIndex)
                val normalized = CallLogNormalizer.normalize(raw)
                if (normalized == null) {
                    repository.markSkippedCall(callId, timestamp)
                    skipped++
                    continue
                }
                val before = repository.count()
                val changed = repository.processCall(callId, normalized, raw ?: normalized, timestamp)
                if (changed) {
                    processedCount++
                    val after = repository.count()
                    if (after > before) createdEstimate++
                }
            }
        }
        Result(processedCount, createdEstimate, skipped)
    }
}

