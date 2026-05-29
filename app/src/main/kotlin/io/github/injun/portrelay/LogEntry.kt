package io.github.injun.portrelay

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

class LogEntry(val message: String, val timestamp: Date = Date()) {
    val id: Long = counter.getAndIncrement()
    val formatted: String get() = "[${formatter.format(timestamp)}] $message"

    private companion object {
        val counter = AtomicLong()
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.US)
    }
}
