package com.strollcast.app.utils

/**
 * Parses voice queries from Google Assistant into structured commands
 */
object VoiceCommandParser {

    private val REFERENCE_PATTERN = """(play|jump\s+to)\s+reference""".toRegex(RegexOption.IGNORE_CASE)
    private val PREVIOUS_PATTERN = """(play|jump\s+to)\s+previous""".toRegex(RegexOption.IGNORE_CASE)
    private val TIMESTAMP_PATTERN = """(?:go\s+to|skip\s+to)\s+(\d+)(?::(\d+))?(?:\s+minutes?)?""".toRegex(RegexOption.IGNORE_CASE)

    fun parse(query: String): VoiceCommand {
        return when {
            REFERENCE_PATTERN.containsMatchIn(query) -> {
                VoiceCommand(CommandType.PLAY_REFERENCE, query = query)
            }
            PREVIOUS_PATTERN.containsMatchIn(query) -> {
                VoiceCommand(CommandType.PLAY_PREVIOUS, query = query)
            }
            TIMESTAMP_PATTERN.containsMatchIn(query) -> {
                val match = TIMESTAMP_PATTERN.find(query)
                val timestampMs = parseTimestamp(match)
                VoiceCommand(
                    type = CommandType.SEEK_TO_TIMESTAMP,
                    query = query,
                    parameters = mapOf("timestamp_ms" to timestampMs.toString())
                )
            }
            else -> VoiceCommand(CommandType.UNKNOWN, query = query)
        }
    }

    private fun parseTimestamp(match: MatchResult?): Long {
        if (match == null) return 0L
        val (minutes, seconds) = match.destructured
        val mins = minutes.toLongOrNull() ?: 0L
        val secs = seconds.toLongOrNull() ?: 0L
        return (mins * 60 + secs) * 1000
    }
}

data class VoiceCommand(
    val type: CommandType,
    val timestamp: Long = System.currentTimeMillis(),
    val query: String,
    val parameters: Map<String, String> = emptyMap()
)

enum class CommandType {
    PLAY_REFERENCE,
    PLAY_PREVIOUS,
    SEEK_TO_TIMESTAMP,
    UNKNOWN
}
