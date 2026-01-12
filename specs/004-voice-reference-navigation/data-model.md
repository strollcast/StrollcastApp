# Data Model: Voice Navigation for Reference Playback

**Feature**: Voice commands for reference navigation
**Date**: 2026-01-11
**Platform**: Android

## Overview

This feature does not introduce new persistent entities. Voice commands are handled transiently through the MediaSession framework and communicate with existing ViewModels. All data structures are in-memory and do not require database migrations.

## Existing Entities (No Changes)

### TranscriptLineEntity
**Location**: `android/app/src/main/java/com/strollcast/app/data/TranscriptLineEntity.kt`
**Purpose**: Stores transcript segments with embedded reference links (from feature 003)

```kotlin
@Entity(tableName = "transcript_lines")
data class TranscriptLineEntity(
    @PrimaryKey val id: String,
    val transcriptId: String,
    val startMs: Long,
    val endMs: Long,
    val speaker: String?,
    val text: String,  // Contains markdown links: [text](url)
    val lineNumber: Int
)
```

**Usage**: Voice command "play reference" queries current segment's text field for reference URLs.

### PlaybackHistoryEntry
**Location**: Managed by `PlaybackHistoryManager`
**Purpose**: Tracks episode navigation history for "play previous" command

**Usage**: Voice command "play previous" pops from history stack.

## New Transient Objects (In-Memory Only)

### VoiceCommand
**Purpose**: Represents a parsed voice command
**Lifecycle**: Created when voice query received, processed immediately, discarded

```kotlin
/**
 * Represents a voice command parsed from Google Assistant query
 */
data class VoiceCommand(
    val type: CommandType,
    val timestamp: Long = System.currentTimeMillis(),
    val query: String,
    val parameters: Map<String, String> = emptyMap()
)

enum class CommandType {
    PLAY_REFERENCE,      // "play reference" or "jump to reference"
    PLAY_PREVIOUS,       // "play previous" or "jump to previous"
    SEEK_TO_TIMESTAMP,   // "go to 5 minutes" or "skip to 3:45"
    UNKNOWN              // Unrecognized command
}
```

**Storage**: None - processed immediately in MediaSessionCallback
**Validation**: Command type determined by pattern matching in VoiceCommandParser

### TranscriptSegmentContext
**Purpose**: Provides context about current playback position for reference lookup
**Lifecycle**: Created on demand when processing voice commands, not persisted

```kotlin
/**
 * Context about the current transcript segment during playback
 */
data class TranscriptSegmentContext(
    val episodeId: String,
    val currentPositionMs: Long,
    val currentSegment: TranscriptLineEntity?,
    val parsedReferences: List<EpisodeReference>
)

/**
 * Reference extracted from transcript segment (from feature 003)
 */
data class EpisodeReference(
    val displayText: String,
    val episodeId: String,
    val url: String
)
```

**Storage**: None - computed from existing TranscriptLineEntity
**Validation**: References must match Strollcast episode URL pattern (feature 003)

### AudioFeedbackMessage
**Purpose**: Represents TTS audio feedback for voice command results
**Lifecycle**: Created after command execution, spoken via TTS, discarded

```kotlin
/**
 * Audio feedback message to speak via TTS
 */
data class AudioFeedbackMessage(
    val text: String,
    val priority: FeedbackPriority = FeedbackPriority.NORMAL
)

enum class FeedbackPriority {
    LOW,      // Can be interrupted
    NORMAL,   // Standard feedback
    HIGH      // Must complete (error messages)
}
```

**Storage**: None - spoken immediately via TextToSpeech API
**Validation**: Text length limited to 100 characters for brevity

## State Management

### VoiceCommandState (ViewModel State)
**Purpose**: Track voice command UI state (if optional button added)
**Location**: `PlayerViewModel` or dedicated `VoiceCommandViewModel`

```kotlin
data class VoiceCommandState(
    val isListening: Boolean = false,
    val lastCommand: String? = null,
    val lastFeedback: String? = null,
    val error: String? = null
)
```

**Storage**: In-memory StateFlow in ViewModel
**Persistence**: Not required - resets on app restart

## Data Flow Diagram

```
1. User: "Hey Google, play reference"
   ↓
2. Google Assistant → MediaSession.onPlayFromSearch(query = "reference")
   ↓
3. VoiceCommandParser.parse(query)
   ↓ creates VoiceCommand(type = PLAY_REFERENCE)
   ↓
4. TranscriptViewModel.getCurrentSegment()
   ↓ returns TranscriptSegmentContext with parsed references
   ↓
5. PlayerViewModel.navigateToReferencedEpisode(episodeId)
   ↓ (existing method from feature 003)
   ↓
6. AudioFeedbackManager.speak("Playing reference episode")
   ↓
7. Episode loads and plays
```

## Database Schema Changes

**None required**. This feature uses existing tables:
- `transcript_lines` - Already stores reference links (feature 003)
- `playback_history` - Already tracks navigation history
- `podcasts` - Already stores episode metadata

## Validation Rules

### Voice Command Parsing
- Command string must contain recognized keywords: "reference", "previous", "go to", "skip to"
- Timestamp patterns must match: `\d+:\d+` or `\d+ minutes`
- Unrecognized commands result in `CommandType.UNKNOWN` with error feedback

### Reference Lookup
- Current position must be within a transcript segment (not between segments)
- Reference URLs must match pattern: `https://released.strollcast.com/episodes/{id}/{id}.(mp3|m4a)`
- If no references in current segment, provide feedback: "No reference found in current segment"

### Audio Feedback
- Messages limited to 100 characters for brevity
- TTS initialization must succeed before attempting feedback
- Feedback should not interrupt existing playback (speak at reduced volume)

## Performance Considerations

### Voice Command Processing
- **Target latency**: <100ms from query to command identification
- **Pattern matching**: Precompile regex patterns for efficiency
- **Avoid blocking**: Process commands on background coroutine

### Reference Lookup
- **Index optimization**: Current segment lookup should be O(log n) using binary search by timestamp
- **Cache current context**: Store last-accessed segment to avoid repeated queries
- **Lazy parsing**: Only parse references when "play reference" command issued

### Audio Feedback
- **TTS initialization**: Initialize TextToSpeech engine on service startup, reuse across commands
- **Queue management**: Queue feedback messages if multiple commands issued rapidly
- **Resource cleanup**: Release TTS engine when service stops

## Error Handling

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

**Feedback Messages**:
- `NoReferenceInSegment` → "No reference found in current segment"
- `NoCurrentSegment` → "Not currently in a transcript segment"
- `NoPreviousEpisode` → "Already at first episode"
- `InvalidTimestamp` → "Could not parse timestamp"
- `NavigationFailed` → "Failed to load episode: {reason}"

## Testing Data Requirements

### Test Transcript Segments
Need transcript data with:
- Segments containing reference links (episode: "strollcast-2026-overview")
- Segments without references
- Segments with multiple references

### Test Voice Queries
- "play reference"
- "jump to reference"
- "play previous"
- "jump to previous"
- "go to 5 minutes"
- "skip to 3:45"
- Unrecognized queries for error handling

### Test Scenarios
- Voice command when no segment is playing
- Voice command when segment has no references
- Voice command when offline and episode not downloaded
- Rapid succession of voice commands

## Migration Plan

**Not applicable** - no database changes required.

## Rollback Strategy

**Simple rollback**: Feature can be disabled by:
1. Removing `onPlayFromSearch()` implementation from MediaSessionCallback
2. Removing voice command UI components (if added)
3. No data cleanup needed - no persistent storage

## Future Considerations

### Potential Enhancements (Out of Current Scope)
- **Voice command history**: Log voice commands for analytics (would require new table)
- **Custom wake word**: "Hey Strollcast" (requires always-on listening, battery impact)
- **Multi-language support**: Parse commands in multiple languages (complex NLP)
- **Voice command preferences**: User can enable/disable specific commands (settings table)

### Data Model Changes for Future Features
If adding voice command history:
```kotlin
@Entity(tableName = "voice_command_history")
data class VoiceCommandHistoryEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val query: String,
    val commandType: String,
    val success: Boolean,
    val episodeId: String?
)
```

Not needed for current MVP - listed for reference only.
