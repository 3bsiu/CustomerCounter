package com.customercounter.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {
    private val dateTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    fun dateTime(timestamp: Long): String = synchronized(dateTime) { dateTime.format(Date(timestamp)) }
    fun date(timestamp: Long): String = synchronized(date) { date.format(Date(timestamp)) }
}
