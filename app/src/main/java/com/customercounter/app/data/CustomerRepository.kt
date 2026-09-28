package com.customercounter.app.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class CustomerRepository(private val db: CustomerDatabase) {
    private val customers = db.customerDao()
    private val processed = db.processedCallDao()
    private val state = db.appStateDao()

    fun observeCustomers(recent: Boolean): Flow<List<Customer>> =
        if (recent) customers.observeByRecent() else customers.observeByNumber()

    fun observeCustomer(id: Long): Flow<Customer?> = customers.observeById(id)

    suspend fun count() = customers.count()
    suspend fun totalCalls() = customers.totalCalls() ?: 0
    suspend fun countFirstCallBetween(start: Long, end: Long) = customers.countFirstCallBetween(start, end)
    suspend fun countCallsBetween(start: Long, end: Long) = processed.countBetween(start, end)
    suspend fun mostContacted(limit: Int) = customers.mostContacted(limit)
    suspend fun allCustomers() = customers.all()

    suspend fun processCall(callId: Long, normalizedPhone: String, displayPhone: String, timestamp: Long): Boolean =
        db.withTransaction {
            if (processed.exists(callId)) return@withTransaction false

            val now = System.currentTimeMillis()
            val existing = customers.findByNormalizedPhone(normalizedPhone)
            if (existing == null) {
                val current = state.get() ?: AppState()
                customers.insert(
                    Customer(
                        customerNumber = current.nextCustomerNumber,
                        normalizedPhone = normalizedPhone,
                        displayPhone = displayPhone,
                        firstCallTimestamp = timestamp,
                        lastCallTimestamp = timestamp,
                        totalCalls = 1,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                state.upsert(current.copy(nextCustomerNumber = current.nextCustomerNumber + 1))
            } else {
                val first = minOf(existing.firstCallTimestamp, timestamp)
                val last = maxOf(existing.lastCallTimestamp, timestamp)
                customers.update(
                    existing.copy(
                        firstCallTimestamp = first,
                        lastCallTimestamp = last,
                        totalCalls = existing.totalCalls + 1,
                        updatedAt = now
                    )
                )
            }
            processed.insert(ProcessedCall(callId, timestamp))
            val current = state.get() ?: AppState()
            state.upsert(current.copy(lastSyncTimestamp = maxOf(current.lastSyncTimestamp, timestamp)))
            true
        }

    suspend fun markSkippedCall(callId: Long, timestamp: Long) {
        db.withTransaction {
            if (!processed.exists(callId)) {
                processed.insert(ProcessedCall(callId, timestamp))
                val current = state.get() ?: AppState()
                state.upsert(current.copy(lastSyncTimestamp = maxOf(current.lastSyncTimestamp, timestamp)))
            }
        }
    }

    suspend fun deleteCustomer(id: Long) = customers.deleteById(id)

    suspend fun restore(customersToRestore: List<Customer>, nextNumber: Long) {
        db.withTransaction {
            customers.deleteAll()
            processed.deleteAll()
            customersToRestore.forEach { customers.insert(it.copy(id = 0)) }
            state.upsert(AppState(nextCustomerNumber = nextNumber, lastSyncTimestamp = System.currentTimeMillis(), ignoreCallsBefore = System.currentTimeMillis()))
        }
    }
}
