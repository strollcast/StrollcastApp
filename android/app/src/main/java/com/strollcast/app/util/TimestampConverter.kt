package com.strollcast.app.util

object TimestampConverter {
    /**
     * Parse VTT timestamp to milliseconds
     * Supports formats: HH:MM:SS.mmm and MM:SS.mmm
     *
     * @param timestamp VTT timestamp string (e.g., "00:01:23.456" or "01:23.456")
     * @return Milliseconds from start
     */
    fun parseTimestamp(timestamp: String): Long {
        val parts = timestamp.trim().split(":")
        var ms: Long = 0

        when (parts.size) {
            3 -> {  // HH:MM:SS.mmm
                ms += (parts[0].toLongOrNull() ?: 0) * 3600000  // hours
                ms += (parts[1].toLongOrNull() ?: 0) * 60000    // minutes
                ms += (parts[2].toDoubleOrNull()?.times(1000)?.toLong() ?: 0)  // seconds
            }
            2 -> {  // MM:SS.mmm
                ms += (parts[0].toLongOrNull() ?: 0) * 60000    // minutes
                ms += (parts[1].toDoubleOrNull()?.times(1000)?.toLong() ?: 0)  // seconds
            }
            else -> {
                throw IllegalArgumentException("Invalid timestamp format: $timestamp")
            }
        }

        return ms
    }
}
