package com.customercounter.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY customerNumber ASC")
    fun observeByNumber(): Flow<List<Customer>>

    @Query("SELECT * FROM customers ORDER BY lastCallTimestamp DESC, customerNumber ASC")
    fun observeByRecent(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<Customer?>

    @Query("SELECT * FROM customers WHERE normalizedPhone = :phone LIMIT 1")
    suspend fun findByNormalizedPhone(phone: String): Customer?

    @Query("SELECT * FROM customers WHERE customerNumber = :number LIMIT 1")
    suspend fun findByCustomerNumber(number: Long): Customer?

    @Query("SELECT COUNT(*) FROM customers")
    suspend fun count(): Long

    @Query("SELECT SUM(totalCalls) FROM customers")
    suspend fun totalCalls(): Long?

    @Query("SELECT COUNT(*) FROM customers WHERE firstCallTimestamp >= :start AND firstCallTimestamp < :end")
    suspend fun countFirstCallBetween(start: Long, end: Long): Long

    @Query("SELECT * FROM customers ORDER BY totalCalls DESC, customerNumber ASC LIMIT :limit")
    suspend fun mostContacted(limit: Int): List<Customer>

    @Insert
    suspend fun insert(customer: Customer): Long

    @Update
    suspend fun update(customer: Customer)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM customers ORDER BY customerNumber ASC")
    suspend fun all(): List<Customer>

    @Query("DELETE FROM customers")
    suspend fun deleteAll()
}

@Dao
interface ProcessedCallDao {
    @Query("SELECT 1 FROM processed_calls WHERE callId = :callId LIMIT 1")
    suspend fun exists(callId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(call: ProcessedCall)

    @Query("SELECT COUNT(*) FROM processed_calls WHERE timestamp >= :start AND timestamp < :end")
    suspend fun countBetween(start: Long, end: Long): Long

    @Query("DELETE FROM processed_calls WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM processed_calls")
    suspend fun deleteAll()
}

@Dao
interface AppStateDao {
    @Query("SELECT * FROM app_state WHERE id = 1 LIMIT 1")
    suspend fun get(): AppState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: AppState)
}
