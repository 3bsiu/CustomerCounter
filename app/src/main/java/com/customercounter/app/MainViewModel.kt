package com.customercounter.app

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.customercounter.app.calllog.CallLogSyncManager
import com.customercounter.app.data.Customer
import com.customercounter.app.data.CustomerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class MainViewModel(app: CustomerCounterApplication) : AndroidViewModel(app) {
    private val repository = CustomerRepository(app.database)
    private val syncManager = CallLogSyncManager(app, repository)

    val recentSort = MutableStateFlow(false)
    val customersByNumber: StateFlow<List<Customer>> = repository.observeCustomers(false).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val customersByRecent: StateFlow<List<Customer>> = repository.observeCustomers(true).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isSyncing = MutableStateFlow(false)
    val syncMessage = MutableStateFlow<String?>(null)

    fun sync() {
        if (isSyncing.value) return
        viewModelScope.launch {
            isSyncing.value = true
            try {
                val result = syncManager.sync()
                syncMessage.value = "${result.processed}:${result.created}"
            } catch (_: Throwable) {
                syncMessage.value = "error"
            } finally {
                isSyncing.value = false
            }
        }
    }

    suspend fun stats(): Stats {
        val now = Calendar.getInstance()
        val dayStart = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        val week = Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, firstDayOfWeek); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        return Stats(
            totalCustomers = repository.count(),
            newToday = repository.countFirstCallBetween(dayStart, now.timeInMillis + 1),
            newThisWeek = repository.countFirstCallBetween(week, now.timeInMillis + 1),
            totalCalls = repository.totalCalls(),
            callsToday = repository.countCallsBetween(dayStart, now.timeInMillis + 1),
            mostContacted = repository.mostContacted(10)
        )
    }

    fun customer(id: Long) = repository.observeCustomer(id)
    suspend fun allCustomers() = repository.allCustomers()
    suspend fun restore(uri: android.net.Uri) = com.customercounter.app.util.BackupManager(getApplication(), repository).restore(uri)
    suspend fun backup(uri: android.net.Uri) = com.customercounter.app.util.BackupManager(getApplication(), repository).write(uri)

    data class Stats(
        val totalCustomers: Long,
        val newToday: Long,
        val newThisWeek: Long,
        val totalCalls: Long,
        val callsToday: Long,
        val mostContacted: List<Customer>
    )
}

