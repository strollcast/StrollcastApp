# Data Model: Reference Navigation in Transcripts

**Phase**: 1 (Design & Contracts)
**Date**: 2026-01-11
**Purpose**: Define data structures and relationships for reference navigation feature.

## Overview

This feature requires **no new database entities**. It leverages existing data structures and adds transient runtime objects for parsing and navigation.

## Existing Entities (No Changes)

### TranscriptLineEntity (Room Database)

**Location**: `android/app/src/main/java/com/strollcast/app/models/TranscriptLineEntity.kt`

Already stores transcript text with markdown formatting intact:

```kotlin
@Entity(tableName = "transcript_lines")
data class TranscriptLineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "transcript_id")
    val transcriptId: String,

    @ColumnInfo(name = "start_ms")
    val startMs: Long,

    @ColumnInfo(name = "end_ms")
    val endMs: Long,

    @ColumnInfo(name = "speaker")
    val speaker: String?,

    @ColumnInfo(name = "text")
    val text: String,  // Contains markdown: "[ZeRO](https://released.strollcast.com/...)"

    @ColumnInfo(name = "line_number")
    val lineNumber: Int
)
```

**Validation rules**: None changed. Text field already accepts any string content including markdown.

**State transitions**: None. This is read-only data after VTT parsing.

---

### Podcast (Data Class)

**Location**: `android/app/src/main/java/com/strollcast/app/models/Podcast.kt`

Represents episode metadata fetched from API:

```kotlin
data class Podcast(
    val id: String,
    val title: String,
    val authors: String,
    val year: Int,
    val description: String,
    val duration: String,
    val durationSeconds: Int,
    val audioUrl: String,
    val transcriptUrl: String?,
    val paperUrl: String?,
    val topics: List<String>
)
```

**Usage**: Fetched via `PodcastRepository.getPodcastById(episodeId)` when reference link is tapped.

**Validation rules**: None changed. Already validated by API response parsing.

---

### PlaybackHistory (Internal to PlaybackHistoryManager)

**Location**: Managed by `PlaybackHistoryManager` (implementation details not relevant to this feature)

**Usage**: `PlaybackHistoryManager.addToHistory(podcast, position)` called before loading referenced episode.

**Validation rules**: None changed. Existing manager handles history stack.

---

## New Transient Data Structures

These are runtime-only objects, not persisted to database:

### ParsedLink (Data Class)

**Purpose**: Represents a single parsed markdown link from transcript text.

**Location**: `android/app/src/main/java/com/strollcast/app/utils/MarkdownLinkParser.kt`

```kotlin
data class ParsedLink(
    val text: String,       // Link display text (e.g., "FlashAttention-2")
    val url: String,        // Full URL (e.g., "https://released.strollcast.com/.../episode.mp3")
    val startIndex: Int,    // Character offset where link starts in original text
    val endIndex: Int       // Character offset where link ends in original text
)
```

**Lifecycle**: Created during markdown parsing, used to build `AnnotatedString`, discarded after rendering.

**Validation rules**:
- `startIndex` < `endIndex`
- `url` matches episode URL pattern (validated by `EpisodeUrlParser`)
- `text` is non-empty (if empty, link is ignored)

---

### EpisodeReference (Data Class)

**Purpose**: Represents a validated episode reference extracted from a clicked link.

**Location**: `android/app/src/main/java/com/strollcast/app/utils/EpisodeUrlParser.kt`

```kotlin
data class EpisodeReference(
    val episodeId: String,  // Extracted from URL path (e.g., "dao-2023-flashattention_2_fa")
    val originalUrl: String // Original link URL for debugging/logging
)
```

**Lifecycle**: Created when user taps a link, passed to `PlayerViewModel.navigateToReferencedEpisode()`, discarded after navigation.

**Validation rules**:
- `episodeId` matches pattern: `[a-z0-9-]+`
- `originalUrl` matches episode URL regex (enforced by `EpisodeUrlParser.extractEpisodeId()`)

---

## Data Flow

```
1. VTT File (API)
   ├── Downloaded and parsed into TranscriptLineEntity
   └── text field contains markdown: "[ZeRO](https://released.strollcast.com/...)"

2. Transcript Rendering (UI Layer)
   ├── MarkdownLinkParser.parseLinks(text) → List<ParsedLink>
   ├── Build AnnotatedString with link styling
   └── Render with Compose Text component

3. Link Tap (User Interaction)
   ├── Extract URL from UrlAnnotation
   ├── EpisodeUrlParser.extractEpisodeId(url) → EpisodeReference?
   └── If valid episode URL:
       ├── PlayerViewModel.navigateToReferencedEpisode(episodeId)
       ├── PodcastRepository.getPodcastById(episodeId) → Podcast?
       ├── PlaybackHistoryManager.addToHistory(currentPodcast, currentPosition)
       └── PlayerViewModel.play()

4. Error Cases
   ├── Invalid URL format → Render as plain text (not clickable)
   ├── Episode not found → Show snackbar: "Referenced episode not found"
   ├── Offline + not downloaded → Show snackbar: "Episode requires internet"
   └── Network error → Show snackbar: "Failed to load episode"
```

## Relationships

```
TranscriptLineEntity (persisted)
    |
    | contains markdown in text field
    v
ParsedLink (transient)
    |
    | parsed from markdown
    v
AnnotatedString (Compose UI)
    |
    | user taps link
    v
EpisodeReference (transient)
    |
    | validated episode URL
    v
Podcast (fetched from API)
    |
    | loaded into PlayerViewModel
    v
Playback (existing flow)
```

## No Database Migrations Required

- ✅ TranscriptLineEntity already stores text with markdown
- ✅ Podcast model unchanged (uses existing API contract)
- ✅ No new tables needed
- ✅ No schema version bump needed

## Validation Summary

| Data Structure | Validation Performed | Location |
|---------------|---------------------|----------|
| TranscriptLineEntity.text | None (accepts any string) | VTT parser |
| ParsedLink.url | Episode URL pattern match | EpisodeUrlParser |
| ParsedLink indices | startIndex < endIndex | MarkdownLinkParser |
| EpisodeReference.episodeId | Extracted from validated URL | EpisodeUrlParser |
| Podcast | API response schema validation | Retrofit + Gson |

## State Transitions

No stateful entities. All data is either:
1. **Persisted immutable** (TranscriptLineEntity after VTT parsing)
2. **Transient runtime** (ParsedLink, EpisodeReference during user interaction)
3. **Existing flow** (Podcast fetching, playback state)
