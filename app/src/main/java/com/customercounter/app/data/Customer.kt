package com.customercounter.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customers",
    indices = [Index(value = ["customerNumber"], unique = true), Index(value = ["normalizedPhone"], unique = true)]
)
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerNumber: Long,
    val normalizedPhone: String,
    val displayPhone: String,
    val firstCallTimestamp: Long,
    val lastCallTimestamp: Long,
    val totalCalls: Long,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "processed_calls")
data class ProcessedCall(
    @PrimaryKey val callId: Long,
    val timestamp: Long
)

@Entity(tableName = "app_state")
data class AppState(
    @PrimaryKey val id: Int = 1,
    val nextCustomerNumber: Long = 1,
    val lastSyncTimestamp: Long = 0,
    val ignoreCallsBefore: Long = 0
)
