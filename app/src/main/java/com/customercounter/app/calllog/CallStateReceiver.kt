package com.customercounter.app.calllog

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class CallStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.intent.action.PHONE_STATE") return
        val state = intent.getStringExtra("state") ?: return
        if (state == "IDLE") {
            val request = OneTimeWorkRequestBuilder<CallLogSyncWorker>()
                .setInitialDelay(2, TimeUnit.SECONDS)
                .setInputData(Data.Builder().putString("reason", "call_state").build())
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
