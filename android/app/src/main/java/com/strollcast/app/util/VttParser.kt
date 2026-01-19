package com.strollcast.app.util

import com.strollcast.app.models.TranscriptCue

object VttParser {
    private const val WEBVTT_HEADER = "WEBVTT"
    private val TIMESTAMP_PATTERN = Regex("""([\d:\.]+)\s*-->\s*([\d:\.]+)""")
    private val VOICE_TAG_PATTERN = Regex("""<v\s+([^>]+)>""")
    private val HTML_LINK_PATTERN = Regex("""<a\s+href=["']([^"']+)["'][^>]*>([^<]+)</a>""", RegexOption.IGNORE_CASE)

    /**
     * Parse WebVTT content into a list of TranscriptCue objects
     *
     * @param vttContent Raw VTT file content
     * @return List of parsed transcript cues
     * @throws IllegalArgumentException if VTT content is malformed
     */
    fun parseVTT(vttContent: String): List<TranscriptCue> {
        if (!vttContent.trim().startsWith(WEBVTT_HEADER)) {
            throw IllegalArgumentException("Invalid VTT file: Missing WEBVTT header")
        }

        val cues = mutableListOf<TranscriptCue>()
        val lines = vttContent.lines()
        var i = 0

        // Skip header and find first cue
        while (i < lines.size && !TIMESTAMP_PATTERN.matches(lines[i])) {
            i++
        }

        while (i < lines.size) {
            val line = lines[i].trim()

            // Parse timestamp line
            val timestampMatch = TIMESTAMP_PATTERN.find(line)
            if (timestampMatch != null) {
                val startTimestamp = timestampMatch.groupValues[1]
                val endTimestamp = timestampMatch.groupValues[2]
                val startMs = TimestampConverter.parseTimestamp(startTimestamp)
                val endMs = TimestampConverter.parseTimestamp(endTimestamp)

                i++

                // Parse cue text (may span multiple lines)
                val textLines = mutableListOf<String>()
                var speaker: String? = null

                while (i < lines.size && lines[i].trim().isNotEmpty()) {
                    var cueLine = lines[i].trim()

                    // Extract speaker from voice tag if present
                    val voiceMatch = VOICE_TAG_PATTERN.find(cueLine)
                    if (voiceMatch != null && speaker == null) {
                        speaker = voiceMatch.groupValues[1].trim()
                        cueLine = cueLine.replace(VOICE_TAG_PATTERN, "").trim()
                    }

                    // Convert HTML links to markdown format before stripping other tags
                    cueLine = HTML_LINK_PATTERN.replace(cueLine) { match ->
                        val url = match.groupValues[1]
                        val text = match.groupValues[2]
                        "[$text]($url)"
                    }

                    // Remove remaining HTML tags (but not our markdown links)
                    cueLine = cueLine.replace(Regex("<[^>]+>"), "")

                    if (cueLine.isNotEmpty()) {
                        textLines.add(cueLine)
                    }

                    i++
                }

                // Create cue if we have text
                if (textLines.isNotEmpty()) {
                    cues.add(
                        TranscriptCue(
                            startTime = startMs,
                            endTime = endMs,
                            speaker = speaker,
                            text = textLines.joinToString(" ")
                        )
                    )
                }
            }

            i++
        }

        return cues
    }
}
