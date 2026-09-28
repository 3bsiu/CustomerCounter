package com.customercounter.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Customer::class, ProcessedCall::class, AppState::class],
    version = 2,
    exportSchema = false
)
abstract class CustomerDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun processedCallDao(): ProcessedCallDao
    abstract fun appStateDao(): AppStateDao

    companion object {
        @Volatile private var INSTANCE: CustomerDatabase? = null

        fun getInstance(context: Context): CustomerDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CustomerDatabase::class.java,
                    "customer_counter.db"
                ).build().also { INSTANCE = it }
            }
    }
}
