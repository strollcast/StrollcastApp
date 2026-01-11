package com.strollcast.app.util

import com.strollcast.app.models.TranscriptCue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class VttParserTest {

    @Test
    fun `parseVTT with simple cue`() {
        val vtt = """
            WEBVTT

            00:00:00.000 --> 00:00:05.000
            Welcome to Strollcast
        """.trimIndent()

        val cues = VttParser.parseVTT(vtt)

        assertEquals(1, cues.size)
        assertEquals(0L, cues[0].startTime)
        assertEquals(5000L, cues[0].endTime)
        assertEquals("Welcome to Strollcast", cues[0].text)
        assertNull(cues[0].speaker)
    }

    @Test
    fun `parseVTT with speaker voice tags`() {
        val vtt = """
            WEBVTT

            00:00:00.000 --> 00:00:05.000
            <v Eric>Welcome to Strollcast

            00:00:05.000 --> 00:00:10.500
            <v Maya>Today we're discussing attention mechanisms
        """.trimIndent()

        val cues = VttParser.parseVTT(vtt)

        assertEquals(2, cues.size)

        assertEquals("Eric", cues[0].speaker)
        assertEquals("Welcome to Strollcast", cues[0].text)
        assertEquals(0L, cues[0].startTime)
        assertEquals(5000L, cues[0].endTime)

        assertEquals("Maya", cues[1].speaker)
        assertEquals("Today we're discussing attention mechanisms", cues[1].text)
        assertEquals(5000L, cues[1].startTime)
        assertEquals(10500L, cues[1].endTime)
    }

    @Test
    fun `parseVTT with multiline cue text`() {
        val vtt = """
            WEBVTT

            00:00:00.000 --> 00:00:10.000
            <v Eric>This is a longer cue
            that spans multiple lines
            in the transcript
        """.trimIndent()

        val cues = VttParser.parseVTT(vtt)

        assertEquals(1, cues.size)
        assertEquals("Eric", cues[0].speaker)
        assertEquals("This is a longer cue that spans multiple lines in the transcript", cues[0].text)
    }

    @Test
    fun `parseVTT with HTML tags removed`() {
        val vtt = """
            WEBVTT

            00:00:00.000 --> 00:00:05.000
            <v Eric><b>Bold text</b> and <i>italic text</i>
        """.trimIndent()

        val cues = VttParser.parseVTT(vtt)

        assertEquals(1, cues.size)
        assertEquals("Eric", cues[0].speaker)
        assertEquals("Bold text and italic text", cues[0].text)
    }

    @Test
    fun `parseVTT with multiple cues`() {
        val vtt = """
            WEBVTT

            00:00:00.000 --> 00:00:05.123
            <v Eric>First cue

            00:00:05.123 --> 00:00:10.456
            <v Maya>Second cue

            00:00:10.456 --> 00:00:18.789
            <v Eric>Third cue
        """.trimIndent()

        val cues = VttParser.parseVTT(vtt)

        assertEquals(3, cues.size)
        assertEquals("Eric", cues[0].speaker)
        assertEquals("Maya", cues[1].speaker)
        assertEquals("Eric", cues[2].speaker)
    }

    @Test
    fun `parseVTT with cue numbers ignored`() {
        val vtt = """
            WEBVTT

            1
            00:00:00.000 --> 00:00:05.000
            <v Eric>First cue

            2
            00:00:05.000 --> 00:00:10.000
            <v Maya>Second cue
        """.trimIndent()

        val cues = VttParser.parseVTT(vtt)

        assertEquals(2, cues.size)
        assertEquals("First cue", cues[0].text)
        assertEquals("Second cue", cues[1].text)
    }

    @Test
    fun `parseVTT with NOTE blocks ignored`() {
        val vtt = """
            WEBVTT

            NOTE This is a comment

            00:00:00.000 --> 00:00:05.000
            <v Eric>Actual cue
        """.trimIndent()

        val cues = VttParser.parseVTT(vtt)

        assertEquals(1, cues.size)
        assertEquals("Actual cue", cues[0].text)
    }

    @Test
    fun `parseVTT with empty lines between cues`() {
        val vtt = """
            WEBVTT



            00:00:00.000 --> 00:00:05.000
            <v Eric>First cue


            00:00:05.000 --> 00:00:10.000
            <v Maya>Second cue


        """.trimIndent()

        val cues = VttParser.parseVTT(vtt)

        assertEquals(2, cues.size)
    }

    @Test
    fun `parseVTT with hour format timestamps`() {
        val vtt = """
            WEBVTT

            01:30:00.000 --> 01:30:05.000
            <v Eric>At the 1.5 hour mark
        """.trimIndent()

        val cues = VttParser.parseVTT(vtt)

        assertEquals(1, cues.size)
        assertEquals(5400000L, cues[0].startTime) // 1.5 hours in ms
        assertEquals(5405000L, cues[0].endTime)
    }

    @Test
    fun `parseVTT throws exception for missing WEBVTT header`() {
        val vtt = """
            00:00:00.000 --> 00:00:05.000
            Invalid VTT without header
        """.trimIndent()

        assertThrows(IllegalArgumentException::class.java) {
            VttParser.parseVTT(vtt)
        }
    }

    @Test
    fun `parseVTT with empty VTT file`() {
        val vtt = "WEBVTT\n"

        val cues = VttParser.parseVTT(vtt)

        assertEquals(0, cues.size)
    }

    @Test
    fun `parseVTT with cue without speaker tag`() {
        val vtt = """
            WEBVTT

            00:00:00.000 --> 00:00:05.000
            Text without speaker tag
        """.trimIndent()

        val cues = VttParser.parseVTT(vtt)

        assertEquals(1, cues.size)
        assertNull(cues[0].speaker)
        assertEquals("Text without speaker tag", cues[0].text)
    }

    @Test
    fun `parseVTT real world example`() {
        val vtt = """
            WEBVTT

            00:00:00.000 --> 00:00:05.123
            <v Eric>Welcome to Strollcast, where we transform machine learning research papers into audio conversations.

            00:00:05.123 --> 00:00:10.456
            <v Maya>Today we're diving into "Attention Is All You Need" by Vaswani and colleagues from Google Brain.

            00:00:10.456 --> 00:00:18.789
            <v Eric>This 2017 paper introduced the Transformer architecture, which revolutionized natural language processing.
        """.trimIndent()

        val cues = VttParser.parseVTT(vtt)

        assertEquals(3, cues.size)

        // First cue
        assertEquals("Eric", cues[0].speaker)
        assertEquals(0L, cues[0].startTime)
        assertEquals(5123L, cues[0].endTime)
        assertEquals("Welcome to Strollcast, where we transform machine learning research papers into audio conversations.", cues[0].text)

        // Second cue
        assertEquals("Maya", cues[1].speaker)
        assertEquals(5123L, cues[1].startTime)
        assertEquals(10456L, cues[1].endTime)

        // Third cue
        assertEquals("Eric", cues[2].speaker)
        assertEquals(10456L, cues[2].startTime)
        assertEquals(18789L, cues[2].endTime)
    }

    @Test
    fun `parseVTT with whitespace variations`() {
        val vtt = """
            WEBVTT

            00:00:00.000   -->   00:00:05.000
            <v Eric>  Text with extra whitespace
        """.trimIndent()

        val cues = VttParser.parseVTT(vtt)

        assertEquals(1, cues.size)
        assertEquals("Text with extra whitespace", cues[0].text)
    }
}
