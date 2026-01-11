# Implementation Plan: Android Feature Parity

**Branch**: `002-android-parity` | **Date**: 2026-01-11 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/002-android-parity/spec.md`

## Summary

Implement missing features in the Android app to achieve full parity with iOS. The Android app currently has 1 service (PlaybackService) while iOS has 7 services, and is missing critical features: transcript view with tap-to-seek, inline notes on transcripts, played episodes history, and voice command integration. This plan implements these features using Android's native architecture (Jetpack Compose, Room, MVVM, Hilt DI) while maintaining platform-specific patterns.

**Primary Technical Approach**: Extend existing Android architecture with new repositories (TranscriptRepository, NoteRepository, HistoryRepository), ViewModels for each feature, Compose UI screens following Material Design 3, and Room database tables for persistence. Leverage existing PodcastRepository pattern and PlaybackService for integration.

## Technical Context

**Language/Version**: Kotlin 1.9+ (current Android app standard)
**Primary Dependencies**:
- Jetpack Compose with Material Design 3 (UI framework - already in use)
- Room 2.6+ (local database - already in use)
- Hilt (dependency injection - already in use)
- Retrofit + Gson (networking - already in use)
- Media3/ExoPlayer (playback - already in use via PlaybackService)
- Kotlin Coroutines + Flow (async operations - already in use)

**Storage**: Room SQLite database (already configured)
**Testing**: JUnit 4, Espresso for UI tests, MockK for mocking (standard Android testing)
**Target Platform**: Android 8.0 (SDK 26) to Android 15 (SDK 36)
**Project Type**: Mobile (Android app only)
**Performance Goals**:
- Transcript load/parse: < 3 seconds on 4G
- Tap-to-seek latency: < 200ms
- Note creation/edit: < 100ms local operation
- Played list query: < 500ms for 100+ episodes
- UI: 60fps Compose rendering, no jank during playback

**Constraints**:
- Offline-first: All features must work without network (except initial transcript download)
- Memory: < 200MB RAM for transcript caching
- Database: < 50MB growth for notes/history data per 100 episodes
- Battery: No significant drain from new features during background playback

**Scale/Scope**:
- Expected users: 1,000-10,000 Android users
- Episode library: 50-200 episodes
- Transcript size: 10KB-100KB per episode (VTT format)
- Notes per user: 10-500 across all episodes
- Played history: 50-200 completed episodes per active user

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Verify compliance with principles from `.specify/memory/constitution.md`:

- [x] **Cross-Platform Parity**: This feature DIRECTLY ADDRESSES Android's parity gap with iOS. Brings Android from 1 service to equivalent of iOS's 7 services. Implements all missing features identified in spec (transcripts, notes, history, voice commands). **FULLY ALIGNED** with Constitution Principle I.

- [x] **API Contract Stability**: Uses existing API endpoint (`https://api.strollcast.com/episodes`). Transcripts are fetched from same source iOS uses (likely VTT files linked in episode metadata). No breaking API changes required. Only adds client-side consumption of existing transcript URLs. **COMPLIANT** - no API changes needed.

- [x] **Offline-First Architecture**: All features designed for offline:
  - Transcripts cached in Room after first load
  - Notes saved to local database first (Room)
  - Played history tracked in local database
  - Voice commands work via MediaSession (no network)
  - UI indicates cached vs fresh data
  **FULLY COMPLIANT** - meets all offline-first requirements.

- [x] **Platform-Native UI**: Uses Jetpack Compose with Material Design 3 (Android native), follows Android platform patterns:
  - Material3 components (TextField, LazyColumn, Card, etc.)
  - Android navigation patterns
  - Dynamic color theming (Material You)
  - TalkBack accessibility support
  **FULLY COMPLIANT** - pure native Android implementation.

- [x] **Service-Oriented Architecture**: Follows Android's repository + ViewModel pattern (equivalent to iOS services):
  - TranscriptRepository (business logic for transcripts)
  - NoteRepository (business logic for notes)
  - HistoryRepository (business logic for completion tracking)
  - ViewModels orchestrate UI state
  - PlaybackService remains for media playback
  **COMPLIANT** - proper separation of concerns matching iOS architecture philosophy.

- [x] **Feature Flags & Graceful Degradation**:
  - Voice commands optional (graceful if Assistant not available)
  - Transcript view shows error if VTT missing (playback continues)
  - Notes work independently of network
  **COMPLIANT** - features degrade gracefully.

**Violations requiring justification**: None - All constitution principles are satisfied.

## Project Structure

### Documentation (this feature)

```text
specs/002-android-parity/
├── plan.md              # This file
├── research.md          # Phase 0: VTT parsing, Room schema, MediaSession patterns
├── data-model.md        # Phase 1: Transcript, Note, PlaybackHistory entities
├── quickstart.md        # Phase 1: Developer setup for new features
├── contracts/           # Phase 1: API response formats (existing), internal data contracts
└── tasks.md             # Phase 2: Task breakdown (NOT created by this command)
```

### Source Code (Android App Only)

```text
android/app/src/main/java/com/strollcast/app/
├── data/                         # Database (Room) layer
│   ├── StrollcastDatabase.kt     # [MODIFY] Add new tables
│   ├── PodcastDao.kt             # [EXISTING] Podcast queries
│   ├── PlaybackHistoryDao.kt     # [EXISTING] Playback position
│   ├── DownloadDao.kt            # [EXISTING] Downloads
│   ├── TranscriptDao.kt          # [NEW] Transcript caching queries
│   ├── NoteDao.kt                # [NEW] Note CRUD operations
│   └── CompletedEpisodeDao.kt    # [NEW] Played episodes tracking
├── di/                           # Dependency injection (Hilt)
│   ├── DatabaseModule.kt         # [MODIFY] Provide new DAOs
│   ├── NetworkModule.kt          # [EXISTING] API client
│   └── RepositoryModule.kt       # [NEW] Provide new repositories
├── models/                       # Data models
│   ├── Podcast.kt                # [EXISTING]
│   ├── PlaybackHistoryEntry.kt   # [EXISTING]
│   ├── DownloadedEpisode.kt      # [EXISTING]
│   ├── Transcript.kt             # [NEW] Cached transcript with lines
│   ├── TranscriptLine.kt         # [NEW] Individual line with timestamp
│   ├── Note.kt                   # [NEW] User note on transcript line
│   └── CompletedEpisode.kt       # [NEW] Played episode record
├── network/                      # API client (Retrofit)
│   └── StrollcastApi.kt          # [EXISTING] Episode API calls
├── repository/                   # Data repository pattern
│   ├── PodcastRepository.kt      # [EXISTING] Episode data
│   ├── TranscriptRepository.kt   # [NEW] Fetch + cache transcripts
│   ├── NoteRepository.kt         # [NEW] Note CRUD + queries
│   └── HistoryRepository.kt      # [NEW] Completion tracking
├── services/                     # Background services
│   └── PlaybackService.kt        # [MODIFY] Add voice command intents
├── ui/                           # Jetpack Compose UI
│   ├── screens/
│   │   ├── PodcastListScreen.kt         # [EXISTING] Browse episodes
│   │   ├── PlayerScreen.kt              # [MODIFY] Add transcript tab
│   │   ├── TranscriptScreen.kt          # [NEW] Transcript view + notes
│   │   ├── PlayedListScreen.kt          # [NEW] Completed episodes
│   │   └── NotesScreen.kt               # [NEW] All notes view
│   ├── components/
│   │   ├── TranscriptLineItem.kt        # [NEW] Line with tap-to-seek
│   │   ├── NoteDialog.kt                # [NEW] Create/edit note UI
│   │   └── CompletedEpisodeCard.kt      # [NEW] Played episode card
│   ├── theme/                           # [EXISTING] Material3 theme
│   └── StrollcastApp.kt                 # [MODIFY] Add new screens to nav
├── viewmodels/                   # ViewModels (MVVM)
│   ├── PodcastViewModel.kt       # [EXISTING] Episode list state
│   ├── PlayerViewModel.kt        # [MODIFY] Add transcript state
│   ├── TranscriptViewModel.kt    # [NEW] Transcript + notes state
│   ├── PlayedViewModel.kt        # [NEW] Completed episodes state
│   └── NoteViewModel.kt          # [NEW] Notes management state
├── util/                         # [NEW] Utility classes
│   ├── VttParser.kt              # [NEW] Parse WebVTT transcript files
│   └── TimestampConverter.kt     # [NEW] Convert VTT timestamps to ms
├── MainActivity.kt               # [MODIFY] Register voice command receiver
└── StrollcastApplication.kt      # [EXISTING] App entry point
```

**Structure Decision**: Android-only implementation using existing app architecture. Extends current MVVM + Repository pattern with 3 new repositories (Transcript, Note, History), 3 new ViewModels, 3 new Compose screens, and 3 new Room DAOs. Modifies existing PlayerScreen to add transcript tab and PlaybackService to handle voice command intents via MediaSession.

## Complexity Tracking

> **No violations** - All constitution principles satisfied. This implementation follows established Android patterns and aligns with cross-platform parity goals.

---

## Phase 0: Research

**Prerequisites**: None (starting phase)

**Research Tasks**:

1. **VTT Format Research**
   - **Question**: What transcript format does iOS use? How to parse it on Android?
   - **Action**: Investigate WebVTT (VTT) format specification, existing Android parsing libraries
   - **Output**: Parsing strategy (custom parser vs library like AndroidVTT)

2. **Room Database Schema Design**
   - **Question**: How to model transcripts, notes, and history in Room efficiently?
   - **Action**: Design entity relationships, indexes for performance, migration strategy
   - **Output**: Entity class definitions with relationships (@Relation, foreign keys)

3. **MediaSession Voice Command Integration**
   - **Question**: How to handle Assistant voice commands via MediaSession?
   - **Action**: Research MediaSession custom actions, Android 13+ media controls API
   - **Output**: Implementation approach for voice command intents

4. **Transcript Caching Strategy**
   - **Question**: Cache full transcript in DB or filesystem? Memory limits?
   - **Action**: Compare Room BLOB storage vs file caching, test with 100KB VTT files
   - **Output**: Caching approach (recommendation: Room with LRU eviction for old transcripts)

5. **Compose LazyColumn Performance**
   - **Question**: Can LazyColumn handle 500+ transcript lines smoothly? Virtual scrolling?
   - **Action**: Test LazyColumn with large item counts, measure frame rates
   - **Output**: UI optimization strategy (e.g., chunked loading, stable keys)

6. **iOS Transcript API Investigation**
   - **Question**: Where does iOS fetch transcripts? API endpoint or file URL?
   - **Action**: Check iOS TranscriptService.swift for API calls, check episode API response
   - **Output**: Transcript URL location in API response (likely `transcriptUrl` field)

**Deliverable**: `research.md` with decisions, rationales, and alternatives for each research area.

---

## Phase 1: Design & Contracts

**Prerequisites**: `research.md` complete with all research questions answered

**Design Tasks**:

1. **Data Model Design** → `data-model.md`
   - Transcript entity (id, episodeId, vttContent, cachedAt)
   - TranscriptLine entity (id, transcriptId, text, startMs, endMs, lineNumber)
   - Note entity (id, transcriptLineId, content, createdAt, updatedAt)
   - CompletedEpisode entity (id, episodeId, completionPercent, completedAt)
   - PlaybackHistory enhancement (add completionStatus field)
   - Entity relationships and Room @Relation annotations
   - Database migration plan (version 1 → version 2)

2. **API Contracts** → `contracts/`
   - Document existing API response format (episodes endpoint)
   - Define transcript file format (VTT structure)
   - Define internal data flow contracts (Repository → ViewModel → UI)
   - No new API endpoints needed (uses existing)

3. **Integration Quickstart** → `quickstart.md`
   - How to add new Room entities (migration steps)
   - How to create new Repository + ViewModel
   - How to wire up new Compose screen
   - How to test new features (local transcript files for testing)
   - How to handle PlaybackService modifications

4. **Agent Context Update**
   - Run `.specify/scripts/bash/update-agent-context.sh claude`
   - Add Android-specific technologies:
     - WebVTT parsing
     - Room database migrations
     - MediaSession custom actions
     - Jetpack Compose LazyColumn optimization

**Deliverables**:
- `data-model.md` with complete entity definitions
- `contracts/` directory with API and internal contracts
- `quickstart.md` for developer onboarding
- Updated agent context file with new technologies

---

## Phase 2: Task Generation

**Prerequisites**: Phase 1 complete (data-model.md, contracts/, quickstart.md exist)

**Command**: `/speckit.tasks` (separate command, not part of this plan)

**Expected Output**: `tasks.md` with:
- Setup phase: Database migration, new Hilt modules
- P1 tasks: Transcript view implementation (US1)
- P2 tasks: Notes system implementation (US2)
- P3 tasks: Played episodes list (US3)
- P4 tasks: Voice commands (US4)
- Polish phase: Testing, performance optimization

---

## Notes for Implementation

### Key Architectural Decisions

1. **Transcript Storage**: Room database with TEXT column for VTT content, parsed lines in separate table
2. **Offline-First**: All writes go to Room first, UI reads from Room (single source of truth)
3. **ViewModel State**: Use StateFlow for UI state, collect in Compose with `collectAsState()`
4. **Dependency Injection**: Hilt provides repositories to ViewModels automatically
5. **Navigation**: Use Compose Navigation with typed routes for new screens

### Integration Points

1. **PlayerScreen**: Add "Transcript" tab alongside existing player controls
2. **PlaybackService**: Listen for custom MediaSession actions (skip, play/pause via voice)
3. **PodcastRepository**: May need to expose transcript URLs from episode API response
4. **Database**: Increment version, write migration code in StrollcastDatabase

### Performance Considerations

1. **Transcript Parsing**: Do on background thread (withContext(Dispatchers.IO))
2. **Large Lists**: Use LazyColumn with key() for stable item identity
3. **Database Queries**: Add indexes on episodeId, timestamp fields
4. **Memory**: Cache only active transcript, evict old ones after 7 days

### Testing Strategy

1. **Unit Tests**: Repository logic, VTT parser, timestamp conversion
2. **Integration Tests**: Room DAO operations with in-memory database
3. **UI Tests**: Compose test for transcript tap interactions
4. **Manual Testing**: Voice commands with real Assistant on physical device

---

**Next Step**: Run `/speckit.tasks` after reviewing and approving this plan to generate the detailed task breakdown.
