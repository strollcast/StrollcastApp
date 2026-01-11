# Tasks: Reference Navigation in Transcripts

**Input**: Design documents from `/specs/003-reference-navigation/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅

**Tests**: Tests are OPTIONAL for this feature. The spec specifies "Manual testing on physical device and emulator (unit tests optional for markdown parser)". These tasks focus on implementation with manual testing checkpoints.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Android**: `android/app/src/main/java/com/strollcast/app/` (utils/, ui/, viewmodels/)
- **Specs**: `specs/003-reference-navigation/` (documentation for this feature)

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create utility classes that all user stories depend on

- [ ] T001 [P] Create MarkdownLinkParser utility in android/app/src/main/java/com/strollcast/app/utils/MarkdownLinkParser.kt
- [ ] T002 [P] Create EpisodeUrlParser utility in android/app/src/main/java/com/strollcast/app/utils/EpisodeUrlParser.kt

**Checkpoint**: Parsing utilities ready - user story implementation can now begin

---

## Phase 2: User Story 2 - Visual Indication of Reference Links (Priority: P1) 🎯 MVP

**Goal**: Users can visually identify reference links in transcripts through blue underlined styling

**Why First**: Without visible links, users won't discover the tap-to-navigate feature. Visual indication is prerequisite for interaction.

**Independent Test**: Load episode "strollcast-2026-overview" (contains many reference links), view transcript, verify links appear blue and underlined. NO interaction required yet - this phase is visual only.

### Implementation for User Story 2

- [ ] T003 [US2] Add ClickableText import to TranscriptLineItem.kt in android/app/src/main/java/com/strollcast/app/ui/components/TranscriptLineItem.kt
- [ ] T004 [US2] Modify TranscriptLineItem to accept onLinkClick callback parameter in android/app/src/main/java/com/strollcast/app/ui/components/TranscriptLineItem.kt
- [ ] T005 [US2] Add annotatedText state using remember() with MarkdownLinkParser.buildAnnotatedString() in android/app/src/main/java/com/strollcast/app/ui/components/TranscriptLineItem.kt
- [ ] T006 [US2] Replace Text with ClickableText component for cue.text rendering in android/app/src/main/java/com/strollcast/app/ui/components/TranscriptLineItem.kt
- [ ] T007 [US2] Configure ClickableText onClick to handle URL annotations and regular text clicks in android/app/src/main/java/com/strollcast/app/ui/components/TranscriptLineItem.kt
- [ ] T008 [US2] Test link styling in normal (non-highlighted) transcript segments
- [ ] T009 [US2] Test link styling in highlighted (currently playing) transcript segments
- [ ] T010 [US2] Test multiple links in single transcript segment

**Checkpoint**: User Story 2 complete - links are now visually discoverable. Users can see which segments contain references, but tapping does nothing yet.

---

## Phase 3: User Story 1 - Tap Reference Link to Navigate (Priority: P1)

**Goal**: Users can tap reference links to navigate to and play referenced episodes

**Independent Test**: Load episode with references, tap a blue link, verify referenced episode loads and plays. Verify downloaded episodes work offline.

### Implementation for User Story 1

- [ ] T011 [US1] Add navigateToReferencedEpisode() method to PlayerViewModel in android/app/src/main/java/com/strollcast/app/viewmodels/PlayerViewModel.kt
- [ ] T012 [US1] Add _navigationError MutableStateFlow and navigationError StateFlow to PlayerViewModel in android/app/src/main/java/com/strollcast/app/viewmodels/PlayerViewModel.kt
- [ ] T013 [US1] Add clearNavigationError() method to PlayerViewModel in android/app/src/main/java/com/strollcast/app/viewmodels/PlayerViewModel.kt
- [ ] T014 [US1] Implement episode fetching via podcastRepository.getPodcastById() in navigateToReferencedEpisode() in android/app/src/main/java/com/strollcast/app/viewmodels/PlayerViewModel.kt
- [ ] T015 [US1] Implement download status check via downloadManager in navigateToReferencedEpisode() in android/app/src/main/java/com/strollcast/app/viewmodels/PlayerViewModel.kt
- [ ] T016 [US1] Implement network connectivity check in navigateToReferencedEpisode() in android/app/src/main/java/com/strollcast/app/viewmodels/PlayerViewModel.kt
- [ ] T017 [US1] Add current episode to playback history before navigation in navigateToReferencedEpisode() in android/app/src/main/java/com/strollcast/app/viewmodels/PlayerViewModel.kt
- [ ] T018 [US1] Call loadPodcastById() and play() for referenced episode in navigateToReferencedEpisode() in android/app/src/main/java/com/strollcast/app/viewmodels/PlayerViewModel.kt
- [ ] T019 [US1] Add error handling for "episode not found" case in android/app/src/main/java/com/strollcast/app/viewmodels/PlayerViewModel.kt
- [ ] T020 [US1] Add error handling for "offline + not downloaded" case in android/app/src/main/java/com/strollcast/app/viewmodels/PlayerViewModel.kt
- [ ] T021 [US1] Wire up PlayerViewModel in TranscriptScreen via hiltViewModel() in android/app/src/main/java/com/strollcast/app/ui/screens/TranscriptScreen.kt
- [ ] T022 [US1] Add SnackbarHostState to TranscriptScreen for error messages in android/app/src/main/java/com/strollcast/app/ui/screens/TranscriptScreen.kt
- [ ] T023 [US1] Add LaunchedEffect to observe navigationError and show snackbar in android/app/src/main/java/com/strollcast/app/ui/screens/TranscriptScreen.kt
- [ ] T024 [US1] Pass onLinkClick handler to TranscriptLineItem that calls EpisodeUrlParser.extractEpisodeId() and playerViewModel.navigateToReferencedEpisode() in android/app/src/main/java/com/strollcast/app/ui/screens/TranscriptScreen.kt
- [ ] T025 [US1] Test navigation to referenced episode (online, episode exists)
- [ ] T026 [US1] Test navigation to downloaded episode (offline, episode exists locally)
- [ ] T027 [US1] Test error handling: episode not found
- [ ] T028 [US1] Test error handling: offline + episode not downloaded
- [ ] T029 [US1] Test error handling: malformed or non-episode URL (should render as plain text)

**Checkpoint**: User Stories 1 AND 2 complete - full navigation feature working. Users can discover and tap reference links.

---

## Phase 4: User Story 3 - Return to Original Episode (Priority: P2)

**Goal**: Users can return to original episode after following reference links

**Why Not MVP**: PlaybackHistoryManager already exists and handles this. This phase just verifies integration.

**Independent Test**: Follow reference link from Episode A to Episode B, press "Play Previous" (or back button), verify Episode A resumes at previous position.

### Verification for User Story 3

- [ ] T030 [US3] Verify playbackHistoryManager.addToHistory() is called in navigateToReferencedEpisode() (already implemented in T017)
- [ ] T031 [US3] Test single navigation: A → B, then back to A at previous position
- [ ] T032 [US3] Test multiple navigations: A → B → C, then back through C → B → A
- [ ] T033 [US3] Test that position is preserved when returning to original episode

**Checkpoint**: User Story 3 complete - full bidirectional navigation working with existing playback history

---

## Phase 5: Polish & Edge Cases

**Purpose**: Handle remaining edge cases and ensure production quality

- [ ] T034 [P] Verify minimum 48dp touch targets for accessibility compliance
- [ ] T035 [P] Test link visibility against highlighted segment background colors
- [ ] T036 [P] Test performance with episodes containing many links (~10+ per segment)
- [ ] T037 [P] Test with episodes containing no links (should work as before)
- [ ] T038 Verify existing transcript features still work (notes, seek, auto-scroll)
- [ ] T039 Test on physical device with TalkBack enabled (accessibility)
- [ ] T040 Final manual testing on emulator and physical device

**Checkpoint**: Feature complete and production-ready

---

## Implementation Strategy

### MVP Scope (Minimum Viable Product)

**Phases 1-3 only** (T001-T029):
- Setup: Parsing utilities (T001-T002)
- Visual links (T003-T010)
- Tap to navigate (T011-T029)

**Time Estimate**: 4-6 hours implementation

This delivers core value: users can discover and follow reference links.

### Full Feature (All Phases)

**Phases 1-5** (T001-T040):
- MVP scope above
- Return to original (T030-T033)
- Polish & edge cases (T034-T040)

**Time Estimate**: 6-8 hours total (implementation + testing)

### Recommended Approach

1. **Day 1**: Implement MVP (Phases 1-3)
2. **Day 1**: Manual testing of MVP scope
3. **Day 2**: Implement remaining phases (4-5)
4. **Day 2**: Comprehensive testing and polish

## Dependencies & Execution Order

### User Story Dependencies

```
Setup (Phase 1: T001-T002)
    ↓
US2: Visual Links (Phase 2: T003-T010) ← Can implement first, independent
    ↓
US1: Navigation (Phase 3: T011-T029) ← Depends on US2 for UI, uses T017 for history
    ↓
US3: Return to Original (Phase 4: T030-T033) ← Verification only, leverages T017
    ↓
Polish (Phase 5: T034-T040)
```

**Critical Path**: T001-T002 → T003-T010 → T011-T029

**Why This Order**:
1. Setup must complete first (utilities needed by all)
2. Visual indication (US2) is prerequisite for interaction (US1)
3. Navigation (US1) automatically enables return (US3) via playback history
4. Polish verifies everything works together

### Parallel Execution Opportunities

**Phase 1 (Setup)**: Both tasks can run in parallel
- T001 (MarkdownLinkParser) || T002 (EpisodeUrlParser)

**Phase 2 (US2)**: T003-T007 are sequential (modifying same file), but testing tasks can be parallel:
- T008 || T009 || T010

**Phase 3 (US1)**: Some parallelization possible:
- T011-T020 are sequential (all in PlayerViewModel)
- T021-T024 are sequential (all in TranscriptScreen)
- But T011-T020 can overlap with T021-T024 (different files)
- T025-T029 testing can run in parallel

**Phase 4 (US3)**: All verification tasks can run in parallel:
- T030 || T031 || T032 || T033

**Phase 5 (Polish)**: Most tasks can run in parallel:
- T034 || T035 || T036 || T037 (independent checks)
- T038, T039, T040 should run sequentially (comprehensive testing)

## Task Summary

- **Total Tasks**: 40
- **Setup**: 2 tasks (T001-T002)
- **User Story 2**: 8 tasks (T003-T010) - Visual indication
- **User Story 1**: 19 tasks (T011-T029) - Navigation
- **User Story 3**: 4 tasks (T030-T033) - Return to original
- **Polish**: 7 tasks (T034-T040) - Edge cases and QA

**Parallel Opportunities**: 17 tasks marked [P] can run in parallel with other tasks

**Independent Testing**: Each user story has its own "Independent Test" criteria, enabling incremental delivery and testing.
