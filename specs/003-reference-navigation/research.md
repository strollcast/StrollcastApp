# Research: Reference Navigation in Transcripts

**Phase**: 0 (Outline & Research)
**Date**: 2026-01-11
**Purpose**: Resolve unknowns, evaluate alternatives, and establish technical approach for clickable markdown links in Jetpack Compose transcripts.

## Research Questions

### Q1: How to render clickable links in Jetpack Compose text?

**Decision**: Use `AnnotatedString` with `UrlAnnotation` (Compose 1.4+) for link styling and click handling.

**Rationale**:
- Native Compose API introduced in 1.4.0 specifically for clickable links
- Automatically handles accessibility (TalkBack announces links)
- Provides built-in link styling (color, underline)
- Supports multiple links per text segment
- Better touch target handling than manual `ClickableText` with offset calculations

**Implementation approach**:
```kotlin
val annotatedString = buildAnnotatedString {
    // Parse markdown and add UrlAnnotation for each link
    addUrlAnnotation(UrlAnnotation("url"), start, end)
    addStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline), start, end)
}

Text(
    text = annotatedString,
    onTextLayout = { ... },
    modifier = Modifier.pointerInput(Unit) {
        detectTapGestures { offset ->
            // Handle link tap via UrlAnnotation
        }
    }
)
```

**Alternatives considered**:
1. **ClickableText with manual offset calculation**: Rejected because it's error-prone (need to manually track click offsets), doesn't handle accessibility well, and is more verbose.
2. **WebView for rendering**: Rejected because it's heavy (loads entire web renderer), doesn't match Material Design styling, and breaks existing transcript features (highlighting, notes).
3. **Custom gesture detection on Card**: Rejected because it conflicts with existing tap-to-seek and long-press-for-notes gestures.

**Best practices**:
- Use `LinkAnnotation.Url` for web links (Compose 1.7+) or `UrlAnnotation` for backward compatibility
- Apply consistent link styling via `MaterialTheme.colorScheme.primary` with `TextDecoration.Underline`
- Ensure 48dp minimum touch target for accessibility
- Test with TalkBack enabled to verify link announcements

---

### Q2: How to parse markdown links from plain text efficiently?

**Decision**: Implement regex-based parser in utility class `MarkdownLinkParser` with single-pass parsing.

**Rationale**:
- Markdown link format is simple and well-defined: `[text](url)`
- Regex is sufficient for this specific case (no nested structures)
- Single-pass parsing minimizes overhead (~200 segments per episode)
- Pure function design enables easy unit testing
- No external markdown library needed (avoids dependency bloat)

**Implementation approach**:
```kotlin
object MarkdownLinkParser {
    private val MARKDOWN_LINK_REGEX = "\\[([^\\]]+)\\]\\(([^\\)]+)\\)".toRegex()

    data class Link(val text: String, val url: String, val start: Int, val end: Int)

    fun parseLinks(text: String): List<Link> {
        return MARKDOWN_LINK_REGEX.findAll(text).map { match ->
            Link(
                text = match.groupValues[1],
                url = match.groupValues[2],
                start = match.range.first,
                end = match.range.last + 1
            )
        }.toList()
    }

    fun buildAnnotatedString(text: String, linkColor: Color): AnnotatedString {
        // Parse links and build AnnotatedString with styling
    }
}
```

**Alternatives considered**:
1. **Full markdown parser library (Markwon, CommonMark)**: Rejected because it's overkill for simple link parsing, adds 500KB+ to APK, and we only need link extraction, not full markdown rendering.
2. **Manual string iteration**: Rejected because it's error-prone, harder to maintain, and slower than compiled regex for this use case.
3. **HTML parsing (converting markdown to HTML first)**: Rejected because it adds unnecessary complexity and requires HTML parser library.

**Best practices**:
- Cache parsed `AnnotatedString` in Compose state to avoid re-parsing on recomposition
- Handle malformed markdown gracefully (partial matches render as plain text)
- Validate URL format before creating clickable annotation

---

### Q3: How to extract episode ID from reference URL?

**Decision**: Use URL path parsing with pattern matching for Strollcast episode URL format.

**Rationale**:
- Episode URLs follow consistent pattern: `https://released.strollcast.com/episodes/{id}/{id}.mp3`
- Episode ID appears twice in path (directory name and filename)
- Simple string parsing is sufficient (no need for full URL parsing library)
- Can validate URL format to ignore non-episode links

**Implementation approach**:
```kotlin
object EpisodeUrlParser {
    private val EPISODE_URL_PATTERN =
        "https://released\\.strollcast\\.com/episodes/([^/]+)/\\1\\.(mp3|m4a)".toRegex()

    fun extractEpisodeId(url: String): String? {
        return EPISODE_URL_PATTERN.matchEntire(url)?.groupValues?.get(1)
    }

    fun isEpisodeUrl(url: String): Boolean {
        return EPISODE_URL_PATTERN.matches(url)
    }
}
```

**Alternatives considered**:
1. **Full URL parsing with URI class**: Considered but overkill - we only need pattern validation and ID extraction.
2. **String splitting on "/"**: Rejected because it's fragile (doesn't validate full pattern, could extract wrong segment).
3. **Accept any URL**: Rejected because we should only handle episode links (external links could open browser, causing unexpected behavior).

**Best practices**:
- Validate full URL pattern before extracting ID
- Ignore non-episode URLs (render as plain text)
- Handle both .mp3 and .m4a extensions

---

### Q4: How to integrate with existing PlayerViewModel for playback?

**Decision**: Add `navigateToReferencedEpisode(episodeId: String)` method to PlayerViewModel that fetches episode and initiates playback.

**Rationale**:
- PlayerViewModel already handles episode loading via `loadPodcastById()`
- Existing playback history management via `PlaybackHistoryManager.addToHistory()`
- DownloadManager integration already exists for checking local availability
- Maintains separation of concerns (ViewModel handles business logic, UI handles user interaction)

**Implementation approach**:
```kotlin
// In PlayerViewModel
suspend fun navigateToReferencedEpisode(episodeId: String): Result<Unit> {
    return try {
        // Fetch episode metadata
        val episode = podcastRepository.getPodcastById(episodeId)
            ?: return Result.failure(Exception("Episode not found"))

        // Add current episode to history before switching
        currentPodcast.value?.let { current ->
            playbackHistoryManager.addToHistory(current, currentPosition.value)
        }

        // Check if downloaded
        val audioUrl = downloadManager.getDownloadedPath(episode)
            ?: episode.audioUrl

        // Load and play
        loadPodcastById(episodeId)
        play()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

**Alternatives considered**:
1. **Create separate ReferenceNavigationService**: Rejected because functionality is tightly coupled to playback (adds unnecessary abstraction).
2. **Handle navigation in TranscriptViewModel**: Rejected because TranscriptViewModel shouldn't know about playback (violates separation of concerns).
3. **Direct repository calls from UI**: Rejected because it bypasses ViewModel layer and business logic (playback history, download checking).

**Best practices**:
- Return `Result<Unit>` for error handling at UI layer
- Check network connectivity before attempting to fetch non-downloaded episodes
- Show appropriate error messages based on failure type (not found, offline, etc.)

---

### Q5: How to handle offline scenario when referenced episode not downloaded?

**Decision**: Check network connectivity and download status before navigation, show specific error message for offline+not-downloaded case.

**Rationale**:
- Follows Constitution principle III (Offline-First Architecture)
- Provides clear user feedback about why action failed
- Guides user toward solution (download episode first)
- Matches iOS behavior (graceful degradation)

**Implementation approach**:
```kotlin
suspend fun navigateToReferencedEpisode(episodeId: String): Result<Unit> {
    val episode = podcastRepository.getPodcastById(episodeId)
        ?: return Result.failure(EpisodeNotFoundException())

    val isDownloaded = downloadManager.isDownloaded(episode)
    val isOnline = networkMonitor.isOnline()

    if (!isDownloaded && !isOnline) {
        return Result.failure(OfflineEpisodeNotDownloadedException())
    }

    // Proceed with navigation...
}

// In UI
viewModel.navigateToReferencedEpisode(episodeId).onFailure { error ->
    when (error) {
        is EpisodeNotFoundException ->
            showSnackbar("Referenced episode not found")
        is OfflineEpisodeNotDownloadedException ->
            showSnackbar("Episode requires internet. Download it first to access offline.")
        else ->
            showSnackbar("Failed to load episode: ${error.message}")
    }
}
```

**Alternatives considered**:
1. **Silently fail**: Rejected because user has no feedback about why nothing happened.
2. **Always attempt to stream**: Rejected because it provides poor UX when offline (long timeout, unclear error).
3. **Disable all links when offline**: Rejected because downloaded episodes should still be navigable.

**Best practices**:
- Use specific exception types for different error cases
- Provide actionable error messages
- Don't block UI while checking network/download status

---

## Summary of Technical Decisions

| Decision Area | Choice | Key Rationale |
|--------------|--------|---------------|
| Link Rendering | AnnotatedString with UrlAnnotation | Native Compose API, accessibility support |
| Markdown Parsing | Regex-based custom parser | Lightweight, sufficient for simple link format |
| URL Validation | Pattern matching for episode URLs | Filters out external links, extracts ID safely |
| Playback Integration | PlayerViewModel method | Maintains existing architecture patterns |
| Offline Handling | Check download + network before navigation | Clear error messages, graceful degradation |

## Open Questions

None remaining. All technical approaches are well-defined and align with existing codebase patterns.
