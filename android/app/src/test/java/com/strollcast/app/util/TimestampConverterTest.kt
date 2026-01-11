package com.strollcast.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TimestampConverterTest {

    @Test
    fun `parseTimestamp with HH_MM_SS_mmm format`() {
        val result = TimestampConverter.parseTimestamp("00:01:23.456")
        assertEquals(83456L, result)
    }

    @Test
    fun `parseTimestamp with MM_SS_mmm format`() {
        val result = TimestampConverter.parseTimestamp("01:23.456")
        assertEquals(83456L, result)
    }

    @Test
    fun `parseTimestamp with hours`() {
        val result = TimestampConverter.parseTimestamp("01:30:45.123")
        assertEquals(5445123L, result) // 1*3600000 + 30*60000 + 45.123*1000
    }

    @Test
    fun `parseTimestamp with zero timestamp`() {
        val result = TimestampConverter.parseTimestamp("00:00:00.000")
        assertEquals(0L, result)
    }

    @Test
    fun `parseTimestamp with minutes only`() {
        val result = TimestampConverter.parseTimestamp("00:05.500")
        assertEquals(5500L, result)
    }

    @Test
    fun `parseTimestamp with milliseconds precision`() {
        val result = TimestampConverter.parseTimestamp("00:00:01.001")
        assertEquals(1001L, result)
    }

    @Test
    fun `parseTimestamp with whitespace`() {
        val result = TimestampConverter.parseTimestamp("  00:01:23.456  ")
        assertEquals(83456L, result)
    }

    @Test
    fun `parseTimestamp with large hour value`() {
        val result = TimestampConverter.parseTimestamp("10:15:30.500")
        assertEquals(36930500L, result) // 10*3600000 + 15*60000 + 30.5*1000
    }

    @Test
    fun `parseTimestamp with fractional seconds less than 3 digits`() {
        val result = TimestampConverter.parseTimestamp("00:00:05.5")
        assertEquals(5500L, result)
    }

    @Test
    fun `parseTimestamp throws exception for invalid format`() {
        assertThrows(IllegalArgumentException::class.java) {
            TimestampConverter.parseTimestamp("invalid")
        }
    }

    @Test
    fun `parseTimestamp throws exception for single value`() {
        assertThrows(IllegalArgumentException::class.java) {
            TimestampConverter.parseTimestamp("123")
        }
    }

    @Test
    fun `parseTimestamp throws exception for too many parts`() {
        assertThrows(IllegalArgumentException::class.java) {
            TimestampConverter.parseTimestamp("01:02:03:04.567")
        }
    }

    @Test
    fun `parseTimestamp with no fractional seconds`() {
        val result = TimestampConverter.parseTimestamp("00:00:05")
        assertEquals(5000L, result)
    }

    @Test
    fun `parseTimestamp real world example from VTT`() {
        // Example from typical VTT file
        val result1 = TimestampConverter.parseTimestamp("00:00:00.000")
        val result2 = TimestampConverter.parseTimestamp("00:00:05.123")

        assertEquals(0L, result1)
        assertEquals(5123L, result2)
    }
}
