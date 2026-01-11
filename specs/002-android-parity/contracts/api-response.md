# API Contracts: Episode Data

**Endpoint**: `https://api.strollcast.com/episodes`
**Method**: GET
**Authentication**: None (public API)

## Response Format

### Episodes List

```json
{
  "episodes": [
    {
      "id": "vaswani-2017-attention",
      "title": "Attention Is All You Need",
      "authors": ["Vaswani et al."],
      "year": 2017,
      "duration": 3420,
      "audioUrl": "https://strollcast.com/episodes/vaswani-2017-attention/audio.m4a",
      "transcriptUrl": "https://strollcast.com/episodes/vaswani-2017-attention/transcript.vtt",
      "thumbnailUrl": "https://strollcast.com/episodes/vaswani-2017-attention/thumbnail.jpg",
      "description": "Introducing the Transformer architecture...",
      "tags": ["transformers", "attention", "nlp"]
    }
  ]
}
```

### Fields

- `id` (string, required): Unique episode identifier (slug format)
- `title` (string, required): Episode title
- `authors` (array of strings, required): Paper authors
- `year` (integer, required): Publication year
- `duration` (integer, required): Audio duration in seconds
- `audioUrl` (string, required): Direct URL to M4A audio file
- `transcriptUrl` (string, **optional**): URL to VTT transcript file
- `thumbnailUrl` (string, optional): Episode artwork URL
- `description` (string, optional): Episode description
- `tags` (array of strings, optional): Topic tags

### Android Model Mapping

```kotlin
data class Podcast(
    val id: String,
    val title: String,
    val authors: List<String>,
    val year: Int,
    val duration: Int,  // seconds
    val audioUrl: String,
    val transcriptUrl: String?,  // NEW: Add this field
    val thumbnailUrl: String?,
    val description: String?,
    val tags: List<String>?
)
```

## VTT Transcript Format

**File Format**: WebVTT (Web Video Text Tracks)
**MIME Type**: `text/vtt`
**Encoding**: UTF-8

### Example VTT File

```vtt
WEBVTT

00:00:00.000 --> 00:00:05.123
<v Eric>Welcome to Strollcast, where we transform machine learning research papers into audio conversations.

00:00:05.123 --> 00:00:10.456
<v Maya>Today we're diving into "Attention Is All You Need" by Vaswani and colleagues from Google Brain.

00:00:10.456 --> 00:00:18.789
<v Eric>This 2017 paper introduced the Transformer architecture, which revolutionized natural language processing.
```

### VTT Cue Structure

- **Header**: `WEBVTT` (first line, required)
- **Blank Line**: Separates header from cues
- **Timestamp**: `HH:MM:SS.mmm --> HH:MM:SS.mmm`
  - Format: Hours:Minutes:Seconds.Milliseconds
  - Hours optional for short videos
  - Milliseconds separated by period (`.`)
- **Voice Tag**: `<v SpeakerName>` (optional, at start of cue text)
- **Text Content**: One or more lines of transcript text
- **Blank Line**: Separates cues

### Parsing Contract

Android app parses VTT into this structure:

```kotlin
data class TranscriptCue(
    val startTime: Long,    // Milliseconds from start
    val endTime: Long,      // Milliseconds from start
    val speaker: String?,   // Extracted from <v> tag
    val text: String        // Cue text (without tags)
)
```

**Conversion Rules**:
- `startTime` = VTT start timestamp in milliseconds
- `endTime` = VTT end timestamp in milliseconds
- `speaker` = text between `<v ` and `>`, or null if no tag
- `text` = cue content with HTML tags stripped

## Error Handling

### API Errors

- **404 Not Found**: Episodes endpoint unavailable → Show cached episodes, display offline banner
- **Network Timeout**: No internet → Use cached data, retry on reconnect
- **JSON Parse Error**: Malformed response → Log error, show user-friendly message

### Transcript Errors

- **transcriptUrl is null**: Episode has no transcript → Hide transcript tab, show "Transcript unavailable" message
- **404 on VTT download**: Transcript file missing → Show error message, allow playback to continue
- **Malformed VTT**: Parse error → Log error, show "Transcript unavailable", cache empty
- **Network error**: Can't download VTT → Check cache first, show cached version or error message

## Caching Strategy

### Episodes
- **Cache Duration**: 24 hours
- **Storage**: Room database
- **Refresh**: On app launch if stale

### Transcripts
- **Cache Duration**: 30 days
- **Storage**: Room database (vttContent TEXT column)
- **Eviction**: LRU (oldest first) if size > 10MB

## Backwards Compatibility

**Adding `transcriptUrl` field**:
- Field is optional (`String?` in Kotlin)
- Existing episodes without transcripts return `null`
- No breaking change to API contract
- iOS and Android can consume independently
