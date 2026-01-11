# API Contracts: Voice Navigation for Reference Playback

**Feature**: Voice commands for reference navigation
**Date**: 2026-01-11
**Platform**: Android

## Overview

This feature does NOT introduce new HTTP API endpoints. Voice commands are handled entirely on the client side through Android's MediaSession framework and Google Assistant integration. This document describes the internal contracts between Android app components.

## External API Dependencies (No Changes)

### Episode Metadata API
**Endpoint**: `GET https://api.strollcast.com/episodes`
**Status**: No changes required
**Usage**: Existing endpoint used by feature 003 for reference navigation

Voice commands leverage existing navigation logic - no new API calls needed.

## Internal Android Component Contracts

### 1. MediaSession Voice Query Contract

**Interface**: `MediaSession.Callback.onPlayFromSearch()`
**Provider**: Android MediaSession framework / Google Assistant
**Consumer**: `PlaybackService.MediaSessionCallback`

**Input**:
```kotlin
fun onPlayFromSearch(
    session: MediaSession,
    controller: MediaSession.ControllerInfo,
    query: String,           // Voice query from Google Assistant
    extras: Bundle           // Optional parameters
): ListenableFuture<SessionResult>
```

**Query String Format**:
```
Examples:
- "play reference"
- "jump to reference"
- "play previous"
- "jump to previous"
- "go to 5 minutes"
- "skip to 3:45"
```

**Response**:
```kotlin
SessionResult(
    resultCode = SessionResult.RESULT_SUCCESS | RESULT_ERROR_*,
    extras = Bundle()
)
```

**Error Codes**:
- `RESULT_SUCCESS`: Command processed successfully
- `RESULT_ERROR_BAD_VALUE`: Invalid query format
- `RESULT_ERROR_NOT_SUPPORTED`: Command not supported
- `RESULT_ERROR_INVALID_STATE`: Cannot execute in current state

**Notes**:
- Called by Android system when user issues voice command through Google Assistant
- Must return ListenableFuture for asynchronous processing
- Should not block - delegate to background thread if needed

### 2. Voice Command Broadcast Contract

**Interface**: Broadcast Intent
**Provider**: `PlaybackService`
**Consumer**: `PlayerViewModel`, `TranscriptViewModel`

**Intent Actions**:
```kotlin
companion object {
    const val ACTION_VOICE_COMMAND = "com.strollcast.app.VOICE_COMMAND"
    const val EXTRA_COMMAND_TYPE = "command_type"
    const val EXTRA_COMMAND_PARAMS = "command_params"
}
```

**Command Types**:
```kotlin
enum class VoiceCommandType {
    PLAY_REFERENCE,
    PLAY_PREVIOUS,
    SEEK_TO_TIMESTAMP
}
```

**Intent Extras**:
```kotlin
// Example: Play reference command
Intent(ACTION_VOICE_COMMAND).apply {
    putExtra(EXTRA_COMMAND_TYPE, VoiceCommandType.PLAY_REFERENCE.name)
}

// Example: Seek to timestamp
Intent(ACTION_VOICE_COMMAND).apply {
    putExtra(EXTRA_COMMAND_TYPE, VoiceCommandType.SEEK_TO_TIMESTAMP.name)
    putExtra(EXTRA_COMMAND_PARAMS, Bundle().apply {
        putLong("timestamp_ms", 300000) // 5 minutes
    })
}
```

**Receiver Registration**:
```kotlin
// In Activity or ViewModel
LocalBroadcastManager.getInstance(context)
    .registerReceiver(
        voiceCommandReceiver,
        IntentFilter(ACTION_VOICE_COMMAND)
    )
```

**Notes**:
- Use LocalBroadcastManager for security (internal broadcasts only)
- Receiver should be registered when app is active, unregistered in onPause()
- Commands processed asynchronously in ViewModel

### 3. VoiceCommandParser Contract

**Interface**: Pure function for parsing voice queries
**Provider**: `VoiceCommandParser` utility class
**Consumer**: `PlaybackService.MediaSessionCallback`

**Function Signature**:
```kotlin
object VoiceCommandParser {
    fun parse(query: String): VoiceCommand
}

data class VoiceCommand(
    val type: CommandType,
    val timestamp: Long = System.currentTimeMillis(),
    val query: String,
    val parameters: Map<String, String> = emptyMap()
)
```

**Input Examples**:
```kotlin
parse("play reference")      // → CommandType.PLAY_REFERENCE
parse("jump to reference")   // → CommandType.PLAY_REFERENCE
parse("play previous")       // → CommandType.PLAY_PREVIOUS
parse("go to 5 minutes")     // → CommandType.SEEK_TO_TIMESTAMP, params: {timestamp_ms: 300000}
parse("skip to 3:45")        // → CommandType.SEEK_TO_TIMESTAMP, params: {timestamp_ms: 225000}
parse("hello world")         // → CommandType.UNKNOWN
```

**Pattern Matching Rules**:
```kotlin
private val REFERENCE_PATTERN = """(play|jump to)\s+reference""".toRegex(IGNORE_CASE)
private val PREVIOUS_PATTERN = """(play|jump to)\s+previous""".toRegex(IGNORE_CASE)
private val TIMESTAMP_PATTERN = """(?:go to|skip to)\s+(\d+)(?::(\d+))?(?:\s+minutes?)?""".toRegex(IGNORE_CASE)
```

**Returns**:
- Never null - always returns VoiceCommand
- Unknown queries result in `CommandType.UNKNOWN`
- Parameters map contains parsed values (timestamps, episode IDs, etc.)

### 4. AudioFeedbackManager Contract

**Interface**: Text-to-Speech audio feedback
**Provider**: `AudioFeedbackManager` utility class
**Consumer**: `PlaybackService`, `PlayerViewModel`

**Function Signatures**:
```kotlin
class AudioFeedbackManager(context: Context) {
    fun speak(
        message: String,
        priority: FeedbackPriority = FeedbackPriority.NORMAL
    )

    fun stop()

    fun shutdown()
}

enum class FeedbackPriority {
    LOW,      // Can be interrupted
    NORMAL,   // Standard feedback
    HIGH      // Must complete (error messages)
}
```

**Standard Feedback Messages**:
```kotlin
object FeedbackMessages {
    const val NO_REFERENCE = "No reference found in current segment"
    const val NO_PREVIOUS = "Already at first episode"
    const val PLAYING_REFERENCE = "Playing reference episode"
    const val GOING_BACK = "Going back to previous episode"
    const val INVALID_TIMESTAMP = "Could not understand timestamp"
    const val COMMAND_NOT_RECOGNIZED = "Command not recognized. Try 'play reference' or 'play previous'"
}
```

**TTS Initialization**:
```kotlin
// Initialize in Service.onCreate()
private val tts = TextToSpeech(context) { status ->
    if (status == TextToSpeech.SUCCESS) {
        tts.language = Locale.US
    }
}

// Cleanup in Service.onDestroy()
tts.shutdown()
```

**Volume Ducking**:
- Feedback should not stop playback
- Use `STREAM_MUSIC` with volume 0.7 (70%) to duck under current audio
- Or use `STREAM_NOTIFICATION` to overlay without ducking

### 5. TranscriptViewModel Contract (Extension)

**Interface**: Get current transcript segment context
**Provider**: `TranscriptViewModel`
**Consumer**: `PlaybackService` via ViewModel reference

**Function Signature**:
```kotlin
class TranscriptViewModel {
    // Existing methods...

    // NEW: Get context for voice commands
    fun getCurrentSegmentContext(
        currentPositionMs: Long
    ): TranscriptSegmentContext?
}

data class TranscriptSegmentContext(
    val episodeId: String,
    val currentPositionMs: Long,
    val currentSegment: TranscriptLineEntity?,
    val parsedReferences: List<EpisodeReference>
)
```

**Returns**:
- `null` if no transcript loaded or position not in any segment
- `TranscriptSegmentContext` with empty `parsedReferences` if segment has no links
- Parses references using `MarkdownLinkParser` from feature 003

**Performance**:
- O(log n) lookup using binary search by timestamp
- Caches last-accessed segment to avoid repeated queries
- Parsed references cached per segment

### 6. PlayerViewModel Contract (Extension)

**Interface**: Execute navigation commands
**Provider**: `PlayerViewModel`
**Consumer**: `PlaybackService` via broadcast or shared repository

**Function Signatures**:
```kotlin
class PlayerViewModel {
    // Existing from feature 003:
    fun navigateToReferencedEpisode(episodeId: String)

    // NEW: Handle voice commands
    fun handleVoiceCommand(command: VoiceCommand)
    fun playNextReference()  // Find and play next reference in current segment
    fun playPreviousEpisode()  // Pop from playback history
}
```

**Error Handling**:
```kotlin
sealed class VoiceCommandResult {
    object Success : VoiceCommandResult()
    data class Error(val message: String) : VoiceCommandResult()
}
```

## Permission Requirements

### Android Manifest
**No new permissions required** for MediaSession + Google Assistant approach.

If future enhancement adds custom SpeechRecognizer:
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

### Runtime Permissions
Not needed for MediaSession approach - Google Assistant handles permissions.

## Error Response Formats

### Voice Command Errors
```kotlin
sealed class VoiceCommandError {
    object NoReferenceInSegment : VoiceCommandError()
    object NoCurrentSegment : VoiceCommandError()
    object NoPreviousEpisode : VoiceCommandError()
    object InvalidTimestamp : VoiceCommandError()
    data class NavigationFailed(val reason: String) : VoiceCommandError()
}
```

**Audio Feedback Mapping**:
| Error | Feedback Message |
|-------|------------------|
| NoReferenceInSegment | "No reference found in current segment" |
| NoCurrentSegment | "Not currently in a transcript segment" |
| NoPreviousEpisode | "Already at first episode" |
| InvalidTimestamp | "Could not parse timestamp. Try 'go to 5 minutes'" |
| NavigationFailed | "Failed to load episode: {reason}" |

## Testing Contracts

### Mock Voice Queries for Testing
```kotlin
val testQueries = listOf(
    "play reference" to CommandType.PLAY_REFERENCE,
    "jump to reference" to CommandType.PLAY_REFERENCE,
    "play previous" to CommandType.PLAY_PREVIOUS,
    "go to 5 minutes" to CommandType.SEEK_TO_TIMESTAMP,
    "skip to 3:45" to CommandType.SEEK_TO_TIMESTAMP,
    "hello world" to CommandType.UNKNOWN
)
```

### Mock TranscriptSegmentContext
```kotlin
val mockContextWithReferences = TranscriptSegmentContext(
    episodeId = "strollcast-2026-overview",
    currentPositionMs = 30000,
    currentSegment = TranscriptLineEntity(...),
    parsedReferences = listOf(
        EpisodeReference(
            displayText = "FlashAttention-2",
            episodeId = "dao-2023-flashattention_2_fa",
            url = "https://released.strollcast.com/episodes/dao-2023-flashattention_2_fa/dao-2023-flashattention_2_fa.mp3"
        )
    )
)

val mockContextNoReferences = mockContextWithReferences.copy(
    parsedReferences = emptyList()
)
```

## Backward Compatibility

**Fully backward compatible** - feature adds new capabilities without breaking existing functionality:
- Existing tap navigation (feature 003) continues to work
- Existing playback controls unchanged
- MediaSession already implemented - only extending callbacks
- No API version requirements or breaking changes

## Security Considerations

### Voice Command Validation
- All commands validated before execution
- Invalid episode IDs rejected (prevent unauthorized access)
- URL pattern matching prevents arbitrary navigation
- Broadcast intents use LocalBroadcastManager (internal only)

### Permission Scope
- No microphone access required (Google Assistant handles it)
- No user data transmitted to external services
- All processing happens on-device (except episode metadata fetch)

## Performance SLAs

### Voice Command Processing
- **Query parsing**: <50ms
- **Context lookup**: <100ms
- **Total command-to-action latency**: <500ms
- **Audio feedback start**: <1s from command recognition

### Resource Usage
- **Battery**: <1% per hour (MediaSession only active during playback)
- **Memory**: <10MB additional (TTS engine + cached context)
- **Network**: No additional API calls beyond existing navigation

## Versioning

**Contract Version**: 1.0.0
**Compatibility**: Android 8.0+ (SDK 26+)
**Dependencies**:
- Media3 MediaSession (already integrated)
- Android TextToSpeech API (platform-provided)
- Feature 003 reference navigation (dependency)

## Future Enhancements (Not in Scope)

Potential future contract extensions:
1. **Voice command preferences API**: Enable/disable specific commands
2. **Custom wake word registration**: Register app-specific activation phrase
3. **Voice command analytics**: Log usage for feature improvement
4. **Multi-language support**: Parse commands in user's language
5. **Conversation-style queries**: "Tell me more about this episode"

None of these require API changes - would be client-side enhancements only.
