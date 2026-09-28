package com.customercounter.app.util

import android.content.Context
import android.net.Uri
import com.customercounter.app.data.Customer
import com.customercounter.app.data.CustomerRepository
import org.json.JSONArray
import org.json.JSONObject

class BackupManager(private val context: Context, private val repository: CustomerRepository) {
    suspend fun write(uri: Uri) {
        val app = context.applicationContext as com.customercounter.app.CustomerCounterApplication
        val customers = repository.allCustomers()
        val state = app.database.appStateDao().get()
        val root = JSONObject()
            .put("format", "customer-counter-backup")
            .put("version", 1)
            .put("nextCustomerNumber", state?.nextCustomerNumber ?: ((customers.maxOfOrNull { it.customerNumber } ?: 0) + 1))
        val array = JSONArray()
        customers.forEach { c ->
            array.put(JSONObject()
                .put("customerNumber", c.customerNumber)
                .put("normalizedPhone", c.normalizedPhone)
                .put("displayPhone", c.displayPhone)
                .put("firstCallTimestamp", c.firstCallTimestamp)
                .put("lastCallTimestamp", c.lastCallTimestamp)
                .put("totalCalls", c.totalCalls)
                .put("createdAt", c.createdAt)
                .put("updatedAt", c.updatedAt))
        }
        root.put("customers", array)
        context.contentResolver.openOutputStream(uri)?.use { it.write(root.toString(2).toByteArray(Charsets.UTF_8)) }
            ?: error("Unable to open backup destination")
    }

    suspend fun restore(uri: Uri) {
        val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: error("Unable to read backup")
        val root = JSONObject(text)
        require(root.optString("format") == "customer-counter-backup")
        require(root.optInt("version", -1) == 1)
        val array = root.getJSONArray("customers")
        val list = buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                add(
                    Customer(
                        customerNumber = o.getLong("customerNumber"),
                        normalizedPhone = o.getString("normalizedPhone"),
                        displayPhone = o.optString("displayPhone", o.getString("normalizedPhone")),
                        firstCallTimestamp = o.getLong("firstCallTimestamp"),
                        lastCallTimestamp = o.getLong("lastCallTimestamp"),
                        totalCalls = o.getLong("totalCalls"),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
        }
        val numbers = list.map { it.customerNumber }
        val phones = list.map { it.normalizedPhone }
        require(numbers.size == numbers.toSet().size)
        require(phones.size == phones.toSet().size)
        val next = maxOf(root.optLong("nextCustomerNumber", 1), (numbers.maxOrNull() ?: 0) + 1)
        repository.restore(list, next)
    }
}
