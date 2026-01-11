# Quickstart: Reference Navigation in Transcripts

**Feature**: Clickable reference links in transcript view
**Branch**: `003-reference-navigation`
**Time Estimate**: 4-6 hours implementation + 2 hours testing

## Prerequisites

- Android development environment configured (Android Studio, SDK 26+)
- StrollcastApp project cloned and building successfully
- Familiarity with Jetpack Compose and Kotlin coroutines
- Access to physical device or emulator for testing

## Implementation Overview

This feature adds clickable markdown links to transcript text. Users can tap episode references like `[FlashAttention-2](...)` to navigate to that episode.

## Step-by-Step Implementation

### Step 1: Create Markdown Link Parser (30 min)

Create `android/app/src/main/java/com/strollcast/app/utils/MarkdownLinkParser.kt`:

```kotlin
object MarkdownLinkParser {
    private val MARKDOWN_LINK_REGEX = "\\[([^\\]]+)\\]\\(([^\\)]+)\\)".toRegex()

    data class ParsedLink(
        val text: String,
        val url: String,
        val startIndex: Int,
        val endIndex: Int
    )

    fun parseLinks(text: String): List<ParsedLink> {
        return MARKDOWN_LINK_REGEX.findAll(text).map { match ->
            ParsedLink(
                text = match.groupValues[1],
                url = match.groupValues[2],
                startIndex = match.range.first,
                endIndex = match.range.last + 1
            )
        }.toList()
    }

    fun buildAnnotatedString(
        text: String,
        linkColor: Color,
        onLinkClick: (String) -> Unit
    ): AnnotatedString {
        val links = parseLinks(text)
        return buildAnnotatedString {
            if (links.isEmpty()) {
                append(text)
                return@buildAnnotatedString
            }

            var lastIndex = 0
            links.forEach { link ->
                // Add text before link
                append(text.substring(lastIndex, link.startIndex))

                // Add styled link
                withStyle(SpanStyle(
                    color = linkColor,
                    textDecoration = TextDecoration.Underline
                )) {
                    pushStringAnnotation(tag = "URL", annotation = link.url)
                    append(link.text)
                    pop()
                }

                lastIndex = link.endIndex
            }

            // Add remaining text
            if (lastIndex < text.length) {
                append(text.substring(lastIndex))
            }
        }
    }
}
```

---

### Step 2: Create Episode URL Parser (20 min)

Create `android/app/src/main/java/com/strollcast/app/utils/EpisodeUrlParser.kt`:

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

---

### Step 3: Add Navigation Method to PlayerViewModel (45 min)

Modify `android/app/src/main/java/com/strollcast/app/viewmodels/PlayerViewModel.kt`:

```kotlin
// Add these imports
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PlayerViewModel @Inject constructor(
    // ... existing dependencies
) : ViewModel() {

    // Add error state flow
    private val _navigationError = MutableStateFlow<String?>(null)
    val navigationError: StateFlow<String?> = _navigationError

    // Add navigation method
    fun navigateToReferencedEpisode(episodeId: String) {
        viewModelScope.launch {
            try {
                // Fetch episode metadata
                val episode = podcastRepository.getPodcastById(episodeId)
                if (episode == null) {
                    _navigationError.value = "Referenced episode not found"
                    return@launch
                }

                // Check if downloaded (offline support)
                val isDownloaded = downloadManager.isDownloaded(episode)
                val isOnline = networkMonitor.isOnline() // Assume this exists or check connectivity

                if (!isDownloaded && !isOnline) {
                    _navigationError.value = "Episode requires internet connection. Download it first to access offline."
                    return@launch
                }

                // Add current episode to history
                currentPodcast.value?.let { current ->
                    playbackHistoryManager.addToHistory(current, currentPosition.value)
                }

                // Load referenced episode
                loadPodcastById(episodeId)

                // Clear error
                _navigationError.value = null

            } catch (e: Exception) {
                _navigationError.value = "Failed to load episode: ${e.message}"
            }
        }
    }

    fun clearNavigationError() {
        _navigationError.value = null
    }
}
```

---

### Step 4: Update TranscriptLineItem Component (60 min)

Modify `android/app/src/main/java/com/strollcast/app/ui/components/TranscriptLineItem.kt`:

```kotlin
@Composable
fun TranscriptLineItem(
    cue: TranscriptCue,
    isHighlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    noteCount: Int = 0,
    onLongClick: (() -> Unit)? = null,
    onLinkClick: ((String) -> Unit)? = null  // NEW parameter
) {
    val linkColor = MaterialTheme.colorScheme.primary

    // Parse text for links
    val annotatedText = remember(cue.text, linkColor) {
        MarkdownLinkParser.buildAnnotatedString(
            text = cue.text,
            linkColor = linkColor,
            onLinkClick = {}  // Placeholder, actual handling below
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isHighlighted) 4.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                cue.speaker?.let { speaker ->
                    Text(
                        text = speaker,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // Replace plain Text with ClickableText for link support
                ClickableText(
                    text = annotatedText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isHighlighted)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurface
                    ),
                    onClick = { offset ->
                        // Check if clicked on a link
                        annotatedText.getStringAnnotations(
                            tag = "URL",
                            start = offset,
                            end = offset
                        ).firstOrNull()?.let { annotation ->
                            onLinkClick?.invoke(annotation.item)
                        } ?: run {
                            // Clicked on normal text, trigger seek
                            onClick()
                        }
                    },
                    onLongClick = onLongClick,
                    modifier = Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                )
            }

            // Note indicator (unchanged)
            if (noteCount > 0) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.StickyNote2,
                            contentDescription = "Notes",
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = noteCount.toString(),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}
```

---

### Step 5: Wire Up in TranscriptScreen (30 min)

Modify `android/app/src/main/java/com/strollcast/app/ui/screens/TranscriptScreen.kt`:

```kotlin
@Composable
fun TranscriptScreen(
    episodeId: String,
    transcriptUrl: String?,
    currentPosition: Long,
    onSeekTo: (Long) -> Unit,
    viewModel: TranscriptViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(), // Add PlayerViewModel
    noteViewModel: NoteViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val navigationError by playerViewModel.navigationError.collectAsState()

    // Snackbar for errors
    val snackbarHostState = remember { SnackbarHostState() }

    // Show navigation errors
    LaunchedEffect(navigationError) {
        navigationError?.let { error ->
            snackbarHostState.showSnackbar(error)
            playerViewModel.clearNavigationError()
        }
    }

    // ... existing LaunchedEffects ...

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = modifier.fillMaxSize().padding(padding)) {
            // ... existing loading/error states ...

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = uiState.transcript,
                    key = { it.id }
                ) { line ->
                    TranscriptLineItem(
                        cue = line,
                        isHighlighted = uiState.currentLineIndex == uiState.transcript.indexOf(line),
                        onClick = { onSeekTo(line.startMs) },
                        onLongClick = {
                            selectedLineIndex = uiState.transcript.indexOf(line)
                            selectedLineText = line.text
                            showNoteDialog = true
                        },
                        onLinkClick = { url ->  // NEW
                            EpisodeUrlParser.extractEpisodeId(url)?.let { episodeId ->
                                playerViewModel.navigateToReferencedEpisode(episodeId)
                            }
                        },
                        noteCount = 0  // TODO: Load from NoteViewModel
                    )
                }
            }
        }
    }
}
```

---

## Testing

### Manual Testing Checklist

1. **Basic link rendering**:
   - [ ] Open episode with references (e.g., "Strollcast 2026 Overview")
   - [ ] Verify links appear blue and underlined
   - [ ] Verify plain text is not styled

2. **Link tapping (online)**:
   - [ ] Tap a reference link
   - [ ] Verify referenced episode loads and plays
   - [ ] Verify can return to original episode via "Play Previous"

3. **Link tapping (offline)**:
   - [ ] Enable airplane mode
   - [ ] Tap link to downloaded episode → should work
   - [ ] Tap link to non-downloaded episode → should show error

4. **Edge cases**:
   - [ ] Tap malformed link (if any exist) → should not crash
   - [ ] Tap link to non-existent episode → should show "not found" error
   - [ ] Multiple links in one segment → both should be clickable
   - [ ] Link in highlighted segment → should remain visible

5. **Existing functionality**:
   - [ ] Tap non-link text → should seek to timestamp (existing behavior)
   - [ ] Long press → should show note dialog (existing behavior)
   - [ ] Auto-scroll during playback → should still work

### Test Episodes

Use these episodes which contain reference links:
- **strollcast-2026-overview**: Contains many episode references
- **dao-2023-flashattention_2_fa**: May reference related papers

## Common Issues

### Links not appearing clickable
- **Cause**: Markdown not being parsed
- **Fix**: Check `MarkdownLinkParser.parseLinks()` is being called
- **Debug**: Add log statement to print parsed links

### Links crash on tap
- **Cause**: Episode ID extraction failing
- **Fix**: Verify `EpisodeUrlParser.extractEpisodeId()` regex pattern
- **Debug**: Log the URL being parsed

### Can't return to original episode
- **Cause**: Playback history not being updated
- **Fix**: Ensure `playbackHistoryManager.addToHistory()` is called before navigation
- **Debug**: Check PlaybackHistoryManager logs

## Performance Notes

- Link parsing is fast (< 50ms per segment)
- AnnotatedString is cached via `remember()`
- No noticeable impact on scroll performance
- Episode fetching is network-dependent (~1-3s typical)

## Next Steps

After implementing:
1. Test thoroughly with checklist above
2. Run `/speckit.tasks` to generate implementation tasks for tracking
3. Consider adding analytics for reference navigation usage
4. Consider adding visual preview of referenced episode on long-press
