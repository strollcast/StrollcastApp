# Implementation Plan: Reference Navigation in Transcripts

**Branch**: `003-reference-navigation` | **Date**: 2026-01-11 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/003-reference-navigation/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Enable Android users to tap markdown-formatted reference links in transcript views to navigate to other episodes. When a transcript segment contains a link like `[FlashAttention-2](https://released.strollcast.com/episodes/dao-2023-flashattention_2_fa/dao-2023-flashattention_2_fa.mp3)`, the link will be rendered as clickable blue underlined text. Tapping the link extracts the episode ID, fetches the episode metadata from the API, and loads it for playback using existing PlayerViewModel functionality. The current episode is added to playback history to enable returning via existing "Play Previous" controls.

**Technical Approach**: Use Jetpack Compose's `AnnotatedString` with `UrlAnnotation` to parse markdown links from transcript text and make them clickable. Implement regex-based markdown parser utility to extract link text and URLs. Add click handler to `TranscriptLineItem` that processes episode URLs, validates format, fetches episode from repository, and triggers playback via ViewModel.

## Technical Context

**Language/Version**: Kotlin 1.9+ (Android)
**Primary Dependencies**:
- Jetpack Compose with Material3
- Hilt for dependency injection
- Room for local database (existing TranscriptLineEntity)
- Retrofit for API calls (existing PodcastRepository)
- Kotlin Coroutines for async operations

**Storage**: Room database (existing TranscriptLineEntity already stores text with markdown)
**Testing**: Manual testing on physical device and emulator (unit tests optional for markdown parser)
**Target Platform**: Android 8.0+ (SDK 26), targeting SDK 36
**Project Type**: Mobile (Android app)
**Performance Goals**:
- Link parsing: < 50ms per transcript segment
- Episode navigation: < 3s from tap to playback start (network dependent)
- UI rendering: 60 fps scrolling with clickable links

**Constraints**:
- Must work offline (only for downloaded episodes, with appropriate error messaging)
- Minimum 48dp touch targets for accessibility
- Must not break existing transcript functionality (notes, seek, auto-scroll)
- Links must be visible against both normal and highlighted backgrounds

**Scale/Scope**:
- ~200 transcript segments per episode
- Multiple links possible per segment
- Episode catalog currently ~50 episodes (growing)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Verify compliance with principles from `.specify/memory/constitution.md`:

- [x] **Cross-Platform Parity**: Android-only implementation to bring platform to parity with iOS. iOS already has this via voice command ("Hey Siri, go to reference"). Android implements via direct tap interaction which is more natural for mobile UI. Feature documented in spec as Android-specific with iOS already having equivalent functionality.

- [x] **API Contract Stability**: No API changes required. Uses existing `GET /episodes` endpoint to fetch referenced episode metadata. Transcript VTT files already contain markdown links (confirmed by examining actual transcripts).

- [x] **Offline-First Architecture**:
  - Link parsing and rendering works offline
  - Navigation only allowed to downloaded episodes when offline
  - Error message shown when tapping link to non-downloaded episode offline: "Referenced episode requires internet connection. Download it first to access offline."
  - Uses existing DownloadManager to check episode availability

- [x] **Platform-Native UI**:
  - Uses Jetpack Compose `AnnotatedString` (native Android text formatting)
  - Follows Material Design 3 with `MaterialTheme.colorScheme.primary` for link color
  - Uses `ClickableText` or `Text` with `UrlAnnotation` (Compose best practices)
  - Respects minimum touch target size (48dp)

- [x] **Service-Oriented Architecture**:
  - Business logic in PlayerViewModel (existing)
  - Data fetching in PodcastRepository (existing)
  - UI layer (TranscriptLineItem, TranscriptScreen) only handles rendering and user interaction
  - New utility class: `MarkdownLinkParser` for parsing logic (pure function, testable)

- [x] **Feature Flags & Graceful Degradation**:
  - No external dependencies required
  - Degrades gracefully when offline (shows error, doesn't crash)
  - Malformed links or non-episode URLs render as plain text (no crash)
  - Episode not found shows error toast (doesn't crash)

**Violations requiring justification**: None. All principles satisfied.

## Project Structure

### Documentation (this feature)

```text
specs/003-reference-navigation/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (API contracts - none needed, uses existing)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
android/app/src/main/java/com/strollcast/app/
├── ui/
│   ├── components/
│   │   └── TranscriptLineItem.kt         # MODIFY: Add clickable link support
│   └── screens/
│       └── TranscriptScreen.kt            # MODIFY: Add reference navigation handler
├── utils/
│   └── MarkdownLinkParser.kt              # NEW: Parse markdown links from text
├── viewmodels/
│   └── TranscriptViewModel.kt             # MODIFY: Add reference navigation method
└── repository/
    └── PodcastRepository.kt               # EXISTING: getPodcastById() already available
```

**Structure Decision**: Android-only implementation (Option 2 from template). iOS already has this feature via voice commands. This implementation uses direct UI interaction which is more intuitive for mobile users. No tracking issue needed as iOS already has feature parity through different interaction model (voice vs tap).

## Complexity Tracking

No violations requiring justification. All Constitution principles are satisfied.
