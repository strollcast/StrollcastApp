# Tasks: Android Feature Parity

**Feature Branch**: `002-android-parity` | **Date**: 2026-01-11 | **Spec**: [spec.md](./spec.md)

## Task Format

```
- [ ] T### [P?] [Story?] Task description (file/path)
```

- **T###**: Unique task ID (T001, T002, etc.)
- **[P]**: Optional marker for tasks that can run in parallel within the same phase
- **[Story?]**: User story label (US1, US2, US3, US4) or SETUP, FOUNDATION, POLISH
- **Description**: Clear action with file path in parentheses

---

## Phase 1: Setup & Foundation

**Goal**: Prepare database schema and infrastructure for all new features

**Phase Success Criteria**:
- [ ] Database migration from v1 to v2 completes without errors
- [ ] All new DAOs accessible via Hilt injection
- [ ] VTT parser successfully parses sample transcript files
- [ ] Migration tested with existing app data (no data loss)

### Database Schema

- [X] T001 [P] [SETUP] Create TranscriptEntity Room entity with foreign keys and indexes (android/app/src/main/java/com/strollcast/app/models/TranscriptEntity.kt)
- [X] T002 [P] [SETUP] Create TranscriptLineEntity Room entity with composite index on (transcriptId, lineNumber) (android/app/src/main/java/com/strollcast/app/models/TranscriptLineEntity.kt)
- [X] T003 [P] [SETUP] Create NoteEntity Room entity with indexes on transcriptLineId and (episodeId, createdAt) (android/app/src/main/java/com/strollcast/app/models/NoteEntity.kt)
- [X] T004 [P] [SETUP] Create CompletedEpisodeEntity Room entity with DESC index on completedAt (android/app/src/main/java/com/strollcast/app/models/CompletedEpisodeEntity.kt)
- [X] T005 [SETUP] Create Migration_1_2 object with CREATE TABLE and CREATE INDEX statements for all 4 new tables (android/app/src/main/java/com/strollcast/app/data/migrations/Migration5To6.kt)
- [X] T006 [SETUP] Update StrollcastDatabase to version 2, add new entities to @Database annotation, and register MIGRATION_1_2 (android/app/src/main/java/com/strollcast/app/data/StrollcastDatabase.kt)

### DAO Interfaces

- [X] T007 [P] [SETUP] Create TranscriptDao with getTranscript, insertTranscript, deleteOldTranscripts, getTranscriptLines, insertTranscriptLines, getCurrentLine queries (android/app/src/main/java/com/strollcast/app/data/TranscriptDao.kt)
- [X] T008 [P] [SETUP] Create NoteDao with Flow-based getNotesForEpisode, getNotesForLine, insertNote, updateNote, deleteNote, getNoteCount queries (android/app/src/main/java/com/strollcast/app/data/NoteDao.kt)
- [X] T009 [P] [SETUP] Create CompletedEpisodeDao with Flow-based getAllCompletedEpisodes, isEpisodeCompleted, insertCompletedEpisode, removeCompletedEpisode queries (android/app/src/main/java/com/strollcast/app/data/CompletedEpisodeDao.kt)
- [X] T010 [SETUP] Add abstract DAO getters (transcriptDao, noteDao, completedEpisodeDao) to StrollcastDatabase (android/app/src/main/java/com/strollcast/app/data/StrollcastDatabase.kt)

### Dependency Injection

- [X] T011 [SETUP] Create or update RepositoryModule to provide TranscriptRepository, NoteRepository, HistoryRepository with @Singleton scope (android/app/src/main/java/com/strollcast/app/di/RepositoryModule.kt)
- [X] T012 [SETUP] Update DatabaseModule to provide new DAOs via database instance (android/app/src/main/java/com/strollcast/app/di/DatabaseModule.kt)

### Utilities

- [X] T013 [P] [FOUNDATION] Create VttParser utility with parseVTT method that extracts TranscriptCue list from VTT content (android/app/src/main/java/com/strollcast/app/util/VttParser.kt)
- [X] T014 [P] [FOUNDATION] Create TimestampConverter utility with parseTimestamp method to convert VTT timestamps (HH:MM:SS.mmm) to milliseconds (android/app/src/main/java/com/strollcast/app/util/TimestampConverter.kt)

### Testing

- [X] T015 [FOUNDATION] Write migration test using MigrationTestHelper to verify v5→v6 schema migration (android/app/src/androidTest/java/com/strollcast/app/data/MigrationTest.kt)
- [X] T016 [P] [FOUNDATION] Write unit tests for VttParser with sample VTT content including speaker tags (android/app/src/test/java/com/strollcast/app/util/VttParserTest.kt)
- [X] T017 [P] [FOUNDATION] Write unit tests for TimestampConverter covering HH:MM:SS.mmm and MM:SS.mmm formats (android/app/src/test/java/com/strollcast/app/util/TimestampConverterTest.kt)

---

## Phase 2: User Story 1 - Transcript View with Tap-to-Seek (P1)

**Goal**: Implement transcript display, synchronized highlighting, and tap-to-seek functionality

**Phase Success Criteria**:
- [ ] Transcript view displays parsed VTT content for any episode with transcriptUrl
- [ ] Tapping any transcript line seeks playback to that timestamp (< 200ms latency)
- [ ] Current line highlights in real-time during playback
- [ ] Cached transcripts load instantly without network request
- [ ] Error handling displays "Transcript unavailable" when VTT missing

### Data Layer

- [X] T018 [US1] Update Podcast model to include transcriptUrl: String? field from API response (android/app/src/main/java/com/strollcast/app/models/Podcast.kt)
- [X] T019 [US1] Create TranscriptRepository with getTranscript method implementing 3-tier caching (memory → Room → network) (android/app/src/main/java/com/strollcast/app/repository/TranscriptRepository.kt)
- [X] T020 [US1] Implement downloadVTT method in TranscriptRepository using OkHttpClient to fetch from transcriptUrl (android/app/src/main/java/com/strollcast/app/repository/TranscriptRepository.kt)
- [X] T021 [US1] Implement parseAndCache method in TranscriptRepository to parse VTT with VttParser and insert TranscriptEntity + TranscriptLineEntity into Room (android/app/src/main/java/com/strollcast/app/repository/TranscriptRepository.kt)

### ViewModel

- [X] T022 [US1] Create TranscriptUiState data class with transcript: List<TranscriptCue>, isLoading, error, currentLineIndex fields (android/app/src/main/java/com/strollcast/app/viewmodels/TranscriptViewModel.kt)
- [X] T023 [US1] Create TranscriptViewModel with @HiltViewModel annotation, inject TranscriptRepository (android/app/src/main/java/com/strollcast/app/viewmodels/TranscriptViewModel.kt)
- [X] T024 [US1] Implement loadTranscript method in TranscriptViewModel to fetch from repository and update StateFlow<TranscriptUiState> (android/app/src/main/java/com/strollcast/app/viewmodels/TranscriptViewModel.kt)
- [X] T025 [US1] Implement updateCurrentPosition method in TranscriptViewModel to find and highlight current line based on playback position (android/app/src/main/java/com/strollcast/app/viewmodels/TranscriptViewModel.kt)

### UI Components

- [X] T026 [US1] Create TranscriptLineItem composable displaying speaker, text, and highlighted state with Card background (android/app/src/main/java/com/strollcast/app/ui/components/TranscriptLineItem.kt)
- [X] T027 [US1] Add clickable modifier to TranscriptLineItem that triggers onSeekTo callback with cue.startMs (android/app/src/main/java/com/strollcast/app/ui/components/TranscriptLineItem.kt)

### Screens

- [X] T028 [US1] Create TranscriptScreen composable with LazyColumn using stable keys (key = { it.startMs }) for transcript lines (android/app/src/main/java/com/strollcast/app/ui/screens/TranscriptScreen.kt)
- [X] T029 [US1] Implement loading/error/content states in TranscriptScreen based on uiState (android/app/src/main/java/com/strollcast/app/ui/screens/TranscriptScreen.kt)
- [X] T030 [US1] Add LaunchedEffect to TranscriptScreen to call viewModel.loadTranscript when episodeId changes (android/app/src/main/java/com/strollcast/app/ui/screens/TranscriptScreen.kt)
- [X] T031 [US1] Implement auto-scroll feature using LazyListState.animateScrollToItem when currentLineIndex changes (android/app/src/main/java/com/strollcast/app/ui/screens/TranscriptScreen.kt)

### Integration

- [ ] T032 [US1] Update PlayerScreen to add "Transcript" tab using TabRow alongside player controls (android/app/src/main/java/com/strollcast/app/ui/screens/PlayerScreen.kt)
- [ ] T033 [US1] Add navigation route "transcript/{episodeId}" to NavHost with TranscriptScreen composable (android/app/src/main/java/com/strollcast/app/ui/StrollcastApp.kt)
- [ ] T034 [US1] Connect PlayerViewModel playback position updates to TranscriptViewModel.updateCurrentPosition via SharedFlow or StateFlow (android/app/src/main/java/com/strollcast/app/viewmodels/PlayerViewModel.kt)

### Testing

- [X] T035 [US1] Write TranscriptDao instrumentation tests using in-memory database for insert/query operations (android/app/src/androidTest/java/com/strollcast/app/data/TranscriptDaoTest.kt)
- [X] T036 [US1] Write TranscriptRepository unit tests with mocked DAO and OkHttpClient for caching logic (android/app/src/test/java/com/strollcast/app/repository/TranscriptRepositoryTest.kt)
- [X] T037 [US1] Write Compose UI test for TranscriptScreen verifying tap-to-seek triggers callback with correct timestamp (android/app/src/androidTest/java/com/strollcast/app/ui/screens/TranscriptScreenTest.kt)

---

## Phase 3: User Story 2 - Inline Notes on Transcripts (P2)

**Goal**: Enable users to create, edit, delete, and view notes attached to transcript lines

**Phase Success Criteria**:
- [ ] Users can long-press any transcript line to create a note
- [ ] Note indicator appears on lines with attached notes
- [ ] Tapping note indicator opens detail view with edit/delete options
- [ ] Notes persist across app restarts
- [ ] NotesScreen shows all notes across episodes with context

### Data Layer

- [ ] T038 [US2] Create NoteRepository with createNote, updateNote, deleteNote, getNotesForEpisode, getNotesForLine methods (android/app/src/main/java/com/strollcast/app/repository/NoteRepository.kt)
- [ ] T039 [US2] Implement note validation in NoteRepository (content not empty, max 5000 chars, createdAt <= updatedAt) (android/app/src/main/java/com/strollcast/app/repository/NoteRepository.kt)

### ViewModel

- [ ] T040 [US2] Create NoteUiState data class with notes: List<Note>, isLoading, error, selectedNote fields (android/app/src/main/java/com/strollcast/app/viewmodels/NoteViewModel.kt)
- [ ] T041 [US2] Create NoteViewModel with @HiltViewModel annotation, inject NoteRepository (android/app/src/main/java/com/strollcast/app/viewmodels/NoteViewModel.kt)
- [ ] T042 [US2] Implement createNote, updateNote, deleteNote, loadNotesForEpisode methods in NoteViewModel (android/app/src/main/java/com/strollcast/app/viewmodels/NoteViewModel.kt)
- [ ] T043 [US2] Add note count tracking to TranscriptViewModel to display note indicators on lines (android/app/src/main/java/com/strollcast/app/viewmodels/TranscriptViewModel.kt)

### UI Components

- [ ] T044 [US2] Create NoteDialog composable with TextField, Save/Cancel buttons for note creation/editing (android/app/src/main/java/com/strollcast/app/ui/components/NoteDialog.kt)
- [ ] T045 [US2] Add note indicator icon to TranscriptLineItem when line has notes (use getNoteCount from DAO) (android/app/src/main/java/com/strollcast/app/ui/components/TranscriptLineItem.kt)
- [ ] T046 [US2] Update TranscriptLineItem to show long-press menu with "Add Note" option (android/app/src/main/java/com/strollcast/app/ui/components/TranscriptLineItem.kt)

### Screens

- [ ] T047 [US2] Create NotesScreen composable displaying all notes across episodes in LazyColumn sorted by createdAt (android/app/src/main/java/com/strollcast/app/ui/screens/NotesScreen.kt)
- [ ] T048 [US2] Add episode title and transcript line text context to each note card in NotesScreen (android/app/src/main/java/com/strollcast/app/ui/screens/NotesScreen.kt)
- [ ] T049 [US2] Implement edit/delete actions on note cards in NotesScreen using swipe or menu (android/app/src/main/java/com/strollcast/app/ui/screens/NotesScreen.kt)

### Integration

- [ ] T050 [US2] Add long-press gesture detector to TranscriptScreen items to open NoteDialog (android/app/src/main/java/com/strollcast/app/ui/screens/TranscriptScreen.kt)
- [ ] T051 [US2] Add navigation route "notes" and "notes/{episodeId}" to NavHost for NotesScreen (android/app/src/main/java/com/strollcast/app/ui/StrollcastApp.kt)
- [ ] T052 [US2] Add "Notes" navigation item to bottom navigation or drawer menu (android/app/src/main/java/com/strollcast/app/ui/StrollcastApp.kt)

### Testing

- [ ] T053 [US2] Write NoteDao instrumentation tests for insert, update, delete, and query operations (android/app/src/androidTest/java/com/strollcast/app/data/NoteDaoTest.kt)
- [ ] T054 [US2] Write NoteRepository unit tests verifying validation rules and DAO interactions (android/app/src/test/java/com/strollcast/app/repository/NoteRepositoryTest.kt)
- [ ] T055 [US2] Write Compose UI test for NoteDialog verifying create/edit/save flows (android/app/src/androidTest/java/com/strollcast/app/ui/components/NoteDialogTest.kt)

---

## Phase 4: User Story 3 - Listening History and Played Episodes (P3)

**Goal**: Display completed episodes in dedicated "Played" list sorted by completion date

**Phase Success Criteria**:
- [ ] Episodes are marked complete when user reaches 90% playback
- [ ] Played list displays completed episodes with completion badge and date
- [ ] Played list sorted by most recently completed
- [ ] Tapping played episode opens details with "Replay from Start" option
- [ ] Offline access to played history

### Data Layer

- [ ] T056 [US3] Create HistoryRepository with markEpisodeComplete, getCompletedEpisodes, removeFromCompleted methods (android/app/src/main/java/com/strollcast/app/repository/HistoryRepository.kt)
- [ ] T057 [US3] Implement completion logic in HistoryRepository to insert CompletedEpisodeEntity when playbackPosition >= (duration * 0.9) (android/app/src/main/java/com/strollcast/app/repository/HistoryRepository.kt)

### ViewModel

- [ ] T058 [US3] Create PlayedUiState data class with completedEpisodes: List<CompletedEpisode>, isLoading, error fields (android/app/src/main/java/com/strollcast/app/viewmodels/PlayedViewModel.kt)
- [ ] T059 [US3] Create PlayedViewModel with @HiltViewModel annotation, inject HistoryRepository (android/app/src/main/java/com/strollcast/app/viewmodels/PlayedViewModel.kt)
- [ ] T060 [US3] Implement loadCompletedEpisodes method in PlayedViewModel collecting Flow from DAO (android/app/src/main/java/com/strollcast/app/viewmodels/PlayedViewModel.kt)

### UI Components

- [ ] T061 [US3] Create CompletedEpisodeCard composable displaying episode title, completion badge, completedAt date, and duration (android/app/src/main/java/com/strollcast/app/ui/components/CompletedEpisodeCard.kt)

### Screens

- [ ] T062 [US3] Create PlayedListScreen composable with LazyColumn of CompletedEpisodeCard sorted by completedAt DESC (android/app/src/main/java/com/strollcast/app/ui/screens/PlayedListScreen.kt)
- [ ] T063 [US3] Implement empty state in PlayedListScreen when no completed episodes exist (android/app/src/main/java/com/strollcast/app/ui/screens/PlayedListScreen.kt)
- [ ] T064 [US3] Add click handler to CompletedEpisodeCard to navigate to episode details with "Replay" option (android/app/src/main/java/com/strollcast/app/ui/screens/PlayedListScreen.kt)

### Integration

- [ ] T065 [US3] Update PlayerViewModel or PlaybackService to call HistoryRepository.markEpisodeComplete when playback reaches 90% (android/app/src/main/java/com/strollcast/app/viewmodels/PlayerViewModel.kt)
- [ ] T066 [US3] Add navigation route "played" to NavHost for PlayedListScreen (android/app/src/main/java/com/strollcast/app/ui/StrollcastApp.kt)
- [ ] T067 [US3] Add "Played" tab or menu item to main navigation (android/app/src/main/java/com/strollcast/app/ui/StrollcastApp.kt)

### Testing

- [ ] T068 [US3] Write CompletedEpisodeDao instrumentation tests for insert, query, and delete operations (android/app/src/androidTest/java/com/strollcast/app/data/CompletedEpisodeDaoTest.kt)
- [ ] T069 [US3] Write HistoryRepository unit tests verifying 90% completion threshold logic (android/app/src/test/java/com/strollcast/app/repository/HistoryRepositoryTest.kt)
- [ ] T070 [US3] Write Compose UI test for PlayedListScreen verifying episode display and sorting (android/app/src/androidTest/java/com/strollcast/app/ui/screens/PlayedListScreenTest.kt)

---

## Phase 5: User Story 4 - Voice Commands for Playback Control (P4)

**Goal**: Enable hands-free voice control via Android Assistant MediaSession integration

**Phase Success Criteria**:
- [ ] "Hey Google, pause in Strollcast" pauses playback
- [ ] "Hey Google, play in Strollcast" resumes playback
- [ ] "Hey Google, skip forward 15 seconds in Strollcast" seeks forward
- [ ] "Hey Google, what's playing in Strollcast" reports episode title
- [ ] Voice commands work with screen off via foreground service

### Service Enhancement

- [ ] T071 [US4] Update PlaybackService to create custom MediaSession.Callback overrides for onPlay, onPause, onSkipToNext, onSkipToPrevious (android/app/src/main/java/com/strollcast/app/services/PlaybackService.kt)
- [ ] T072 [US4] Add custom actions to MediaSession for "SKIP_FORWARD_15" and "SKIP_BACKWARD_15" with icons (android/app/src/main/java/com/strollcast/app/services/PlaybackService.kt)
- [ ] T073 [US4] Implement onCustomAction callback in PlaybackService to handle skip forward/backward intents (android/app/src/main/java/com/strollcast/app/services/PlaybackService.kt)
- [ ] T074 [US4] Update MediaSession metadata with episode title, authors, and artwork for "what's playing" queries (android/app/src/main/java/com/strollcast/app/services/PlaybackService.kt)

### Integration

- [ ] T075 [US4] Register MediaSession actions in PlaybackService onCreate and update PlaybackState with custom actions (android/app/src/main/java/com/strollcast/app/services/PlaybackService.kt)
- [ ] T076 [US4] Ensure PlaybackService runs as foreground service with notification for voice command support when screen off (android/app/src/main/java/com/strollcast/app/services/PlaybackService.kt)

### Testing

- [ ] T077 [US4] Manual test voice commands on physical device with Google Assistant while screen on (documented in test plan)
- [ ] T078 [US4] Manual test voice commands on physical device with Google Assistant while screen off (documented in test plan)
- [ ] T079 [US4] Verify MediaSession metadata appears correctly in lock screen controls and Assistant (documented in test plan)

---

## Phase 6: Polish & Performance

**Goal**: Optimize performance, handle edge cases, and ensure production quality

**Phase Success Criteria**:
- [ ] Transcript load time < 3 seconds on 4G
- [ ] LazyColumn scrolling at 60fps with 500+ lines
- [ ] No memory leaks in ViewModels or coroutines
- [ ] All error states handled gracefully with user-friendly messages
- [ ] Accessibility (TalkBack) tested and working

### Performance Optimization

- [ ] T080 [POLISH] Implement WorkManager periodic job to clean up old transcripts (cachedAt < 30 days) (android/app/src/main/java/com/strollcast/app/workers/TranscriptCleanupWorker.kt)
- [ ] T081 [POLISH] Add database size check and LRU eviction in TranscriptRepository if total size > 10MB (android/app/src/main/java/com/strollcast/app/repository/TranscriptRepository.kt)
- [ ] T082 [P] [POLISH] Profile LazyColumn performance with 500+ transcript lines using Layout Inspector recomposition counts (performance testing)
- [ ] T083 [P] [POLISH] Profile memory usage with multiple cached transcripts using Android Profiler (performance testing)

### Error Handling

- [ ] T084 [POLISH] Add error handling in TranscriptRepository for 404 VTT downloads (display "Transcript unavailable") (android/app/src/main/java/com/strollcast/app/repository/TranscriptRepository.kt)
- [ ] T085 [POLISH] Add error handling for malformed VTT content (log error, cache empty, show message) (android/app/src/main/java/com/strollcast/app/util/VttParser.kt)
- [ ] T086 [POLISH] Add network timeout handling in TranscriptRepository with fallback to cached data (android/app/src/main/java/com/strollcast/app/repository/TranscriptRepository.kt)
- [ ] T087 [POLISH] Display user-friendly error messages in all UI screens for network/parsing errors (android/app/src/main/java/com/strollcast/app/ui/screens/*.kt)

### Edge Cases

- [ ] T088 [POLISH] Handle null transcriptUrl gracefully (hide transcript tab, show unavailable message) (android/app/src/main/java/com/strollcast/app/ui/screens/PlayerScreen.kt)
- [ ] T089 [POLISH] Handle very long transcripts (2+ hours) with virtual scrolling optimization (android/app/src/main/java/com/strollcast/app/ui/screens/TranscriptScreen.kt)
- [ ] T090 [POLISH] Handle concurrent note creation/editing on same line (last-write-wins strategy) (android/app/src/main/java/com/strollcast/app/repository/NoteRepository.kt)
- [ ] T091 [POLISH] Handle playback position seeking while transcript is loading (queue seek action) (android/app/src/main/java/com/strollcast/app/viewmodels/TranscriptViewModel.kt)

### Accessibility

- [ ] T092 [P] [POLISH] Add contentDescription to all transcript UI elements for TalkBack support (android/app/src/main/java/com/strollcast/app/ui/screens/TranscriptScreen.kt)
- [ ] T093 [P] [POLISH] Add contentDescription to note UI elements and dialogs for TalkBack support (android/app/src/main/java/com/strollcast/app/ui/components/NoteDialog.kt)
- [ ] T094 [POLISH] Test all features with TalkBack enabled on physical device (documented in test plan)

### Documentation

- [ ] T095 [POLISH] Update README.md or android/README.md with new features (transcript view, notes, played list, voice commands) (android/README.md)
- [ ] T096 [POLISH] Add inline code comments to complex VTT parsing logic and migration code (android/app/src/main/java/com/strollcast/app/util/VttParser.kt)

### Final Testing

- [ ] T097 [POLISH] Run full regression test suite covering all user stories with acceptance scenarios (test plan execution)
- [ ] T098 [POLISH] Test on multiple Android versions (API 26, 30, 34) and screen sizes (test plan execution)
- [ ] T099 [POLISH] Test offline mode by enabling airplane mode and verifying all cached features work (test plan execution)
- [ ] T100 [POLISH] Verify constitution compliance: cross-platform parity achieved, offline-first works, platform-native UI (constitution check)

---

## Task Summary

**Total Tasks**: 100
- **Phase 1 (Setup & Foundation)**: 17 tasks
- **Phase 2 (US1 - Transcript)**: 20 tasks
- **Phase 3 (US2 - Notes)**: 18 tasks
- **Phase 4 (US3 - History)**: 15 tasks
- **Phase 5 (US4 - Voice)**: 9 tasks
- **Phase 6 (Polish)**: 21 tasks

**Parallel Tasks**: 21 tasks marked with [P] for concurrent execution
**User Story Distribution**:
- US1 (Transcript): 20 tasks
- US2 (Notes): 18 tasks
- US3 (History): 15 tasks
- US4 (Voice): 9 tasks
- SETUP: 12 tasks
- FOUNDATION: 5 tasks
- POLISH: 21 tasks

---

## Implementation Notes

### Phase Dependencies

1. **Phase 1 must complete first** - Foundation for all features (database schema, DAOs, utilities)
2. **Phase 2 (US1) independent** - Can implement transcript view without notes/history/voice
3. **Phase 3 (US2) depends on Phase 2** - Notes attach to transcript lines
4. **Phase 4 (US3) independent** - Played list can implement without transcripts
5. **Phase 5 (US4) independent** - Voice commands work with existing playback service
6. **Phase 6 last** - Polish after all features implemented

### Testing Strategy

- **Unit tests**: Write alongside each repository/utility (T016, T017, T036, T054, T069)
- **Instrumentation tests**: Test DAOs with in-memory database (T015, T035, T053, T068)
- **Compose UI tests**: Test interactive components (T037, T055, T070)
- **Manual tests**: Voice commands and accessibility (T077-T079, T094)
- **Regression tests**: Final validation before merge (T097-T100)

### File Creation Order

1. Models & entities (T001-T004)
2. Migration & database (T005-T006)
3. DAOs (T007-T010)
4. Dependency injection (T011-T012)
5. Utilities (T013-T014)
6. Repositories (per user story)
7. ViewModels (per user story)
8. UI components & screens (per user story)
9. Integration & navigation
10. Polish & optimization

---

**Next Step**: Begin implementation with Phase 1 (Setup & Foundation) tasks T001-T017.
