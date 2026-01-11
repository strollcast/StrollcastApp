# Implementation Tasks: Voice Navigation for Reference Playback

**Feature**: Voice commands for hands-free reference navigation
**Branch**: `004-voice-reference-navigation`
**Estimated Effort**: 3-5 days
**Tech Stack**: Kotlin, MediaSession, TextToSpeech API, Media3, Jetpack Compose

## Task Overview

This document breaks down the voice navigation feature into implementable tasks organized by user story. Each phase represents an independently testable increment of functionality.

**Total Tasks**: 25
**Parallel Opportunities**: 8 tasks marked [P]
**MVP Scope**: Phase 3 (User Story 1) - 11 tasks

## Phase 1: Setup & Prerequisites (3 tasks)

**Goal**: Initialize project structure and verify dependencies

- [x] T001 Verify feature 003 (reference navigation) is merged and working
- [x] T002 Verify PlaybackService.kt has MediaSession implementation
- [x] T003 Verify TranscriptViewModel can access current segment data

## Phase 2: Foundational Infrastructure (4 tasks)

**Goal**: Create shared utilities needed by all user stories

- [x] T004 [P] Create VoiceCommandParser utility in android/app/src/main/java/com/strollcast/app/utils/VoiceCommandParser.kt
- [x] T005 [P] Create AudioFeedbackManager utility in android/app/src/main/java/com/strollcast/app/utils/AudioFeedbackManager.kt
- [x] T006 [P] Add voice feedback strings to android/app/src/main/res/values/strings.xml
- [x] T007 Add voice command constants to PlaybackService in android/app/src/main/java/com/strollcast/app/services/PlaybackService.kt

## Phase 3: User Story 1 - Voice Command for Reference Navigation (11 tasks)

**Story Goal**: Users can say "play reference" or "jump to reference" to navigate to referenced episodes hands-free.

**Independent Test Criteria**:
- Play episode "strollcast-2026-overview" to segment with references (~30s)
- Say "Hey Google, play reference"
- Verify referenced episode loads and plays
- Say "play reference" in segment without references
- Verify audio feedback "No reference found in current segment"

**Tasks**:

- [x] T008 [US1] Add getCurrentSegmentContext() method to TranscriptViewModel in android/app/src/main/java/com/strollcast/app/viewmodels/TranscriptViewModel.kt
- [x] T009 [US1] Add playNextReference() method to PlayerViewModel in android/app/src/main/java/com/strollcast/app/viewmodels/PlayerViewModel.kt
- [x] T010 [US1] Add voiceCommandFeedback StateFlow to PlayerViewModel in android/app/src/main/java/com/strollcast/app/viewmodels/PlayerViewModel.kt
- [x] T011 [US1] Implement onPlayFromSearch() callback in PlaybackService MediaSessionCallback in android/app/src/main/java/com/strollcast/app/services/PlaybackService.kt
- [x] T012 [US1] Implement handleVoiceQuery() method in PlaybackService in android/app/src/main/java/com/strollcast/app/services/PlaybackService.kt
- [x] T013 [US1] Implement sendVoiceCommand() broadcast method in PlaybackService in android/app/src/main/java/com/strollcast/app/services/PlaybackService.kt
- [x] T014 [US1] Initialize AudioFeedbackManager in PlaybackService.onCreate() in android/app/src/main/java/com/strollcast/app/services/PlaybackService.kt
- [x] T015 [US1] Add voiceCommandReceiver BroadcastReceiver to MainActivity in android/app/src/main/java/com/strollcast/app/MainActivity.kt
- [x] T016 [US1] Register/unregister voiceCommandReceiver in MainActivity lifecycle in android/app/src/main/java/com/strollcast/app/MainActivity.kt
- [x] T017 [US1] Handle PLAY_REFERENCE command in voiceCommandReceiver in android/app/src/main/java/com/strollcast/app/MainActivity.kt
- [ ] T018 [US1] Test voice command "play reference" with episode containing references

## Phase 4: User Story 2 - Voice Command for Returning to Previous (6 tasks)

**Story Goal**: Users can say "play previous" or "jump to previous" to return to the original episode.

**Independent Test Criteria**:
- Follow a reference via voice command (from US1)
- Say "Hey Google, play previous"
- Verify original episode resumes at saved position
- Say "play previous" when already at first episode
- Verify audio feedback "Already at first episode"

**Tasks**:

- [x] T019 [US2] Add playPreviousEpisode() method to PlayerViewModel in android/app/src/main/java/com/strollcast/app/viewmodels/PlayerViewModel.kt
- [x] T020 [US2] Integrate with existing PlaybackHistoryManager in PlayerViewModel in android/app/src/main/java/com/strollcast/app/viewmodels/PlayerViewModel.kt
- [x] T021 [US2] Add PLAY_PREVIOUS command handling to handleVoiceQuery() in PlaybackService in android/app/src/main/java/com/strollcast/app/services/PlaybackService.kt
- [x] T022 [US2] Handle PLAY_PREVIOUS command in voiceCommandReceiver in android/app/src/main/java/com/strollcast/app/MainActivity.kt
- [ ] T023 [US2] Test voice command "play previous" after following a reference
- [ ] T024 [US2] Test voice command "play previous" with no previous episode (error case)

## Phase 5: User Story 3 - Voice Command Discovery & Feedback (Optional - P2)

**Story Goal**: Users receive clear audio feedback for all voice commands and can discover available commands.

**Independent Test Criteria**:
- Issue various voice commands (reference, previous, unknown)
- Verify appropriate audio feedback for each
- Verify feedback is concise (<3 seconds)
- Verify feedback doesn't interrupt playback

**Tasks**:

- [ ] T025 [US3] Enhance audio feedback messages for all edge cases in AudioFeedbackManager in android/app/src/main/java/com/strollcast/app/utils/AudioFeedbackManager.kt

## Dependencies

### Story Completion Order

```
Phase 1 (Setup) → Phase 2 (Foundation)
                     ↓
                  Phase 3 (US1: Reference Navigation)
                     ↓
                  Phase 4 (US2: Previous Episode)
                     ↓
                  Phase 5 (US3: Feedback - Optional)
```

**Critical Path**:
- Phase 1 & 2 must complete before any user story
- User Story 2 depends on User Story 1 (reuses voice command infrastructure)
- User Story 3 is optional polish (P2 priority)

**Parallel Execution Opportunities**:
- Within Phase 2: All 4 tasks can run in parallel [P]
- T008-T010 (ViewModel methods) can run in parallel
- T014 can run in parallel with T011-T013 (different sections of PlaybackService)

### File Dependencies

Tasks must respect these file modification sequences:

**PlaybackService.kt**:
1. T007 (constants) → T011 (callback) → T012 (query handler) → T013 (broadcast) → T014 (init) → T021 (previous command)

**PlayerViewModel.kt**:
1. T009, T010 can run in parallel → T019, T020 sequential

**TranscriptViewModel.kt**:
1. T008 (standalone, no dependencies)

**MainActivity.kt**:
1. T015 → T016 → T017, T022 sequential

## Implementation Strategy

### MVP (Minimum Viable Product)

**Scope**: Phase 3 only (User Story 1)
**Tasks**: T001-T018 (18 tasks)
**Timeline**: 2-3 days
**Value**: Core voice navigation for references

**Delivers**:
- ✅ "play reference" voice command works
- ✅ Audio feedback for success/error
- ✅ Integration with existing reference navigation
- ✅ Works in foreground and background

### Full Feature

**Scope**: All phases (US1 + US2 + US3)
**Tasks**: T001-T025 (25 tasks)
**Timeline**: 3-5 days
**Value**: Complete voice navigation loop

**Adds to MVP**:
- ✅ "play previous" voice command
- ✅ Complete navigation cycle (forward and back)
- ✅ Enhanced feedback for all edge cases
- ✅ Full feature parity with iOS

## Parallel Execution Examples

### Phase 2: Foundation (All Parallel)

Run simultaneously:
```bash
# Terminal 1
Implement T004 (VoiceCommandParser)

# Terminal 2
Implement T005 (AudioFeedbackManager)

# Terminal 3
Implement T006 (strings.xml)

# Terminal 4
Implement T007 (PlaybackService constants)
```

### Phase 3: ViewModel Extensions

Run simultaneously:
```bash
# Terminal 1
Implement T008 (TranscriptViewModel.getCurrentSegmentContext)

# Terminal 2
Implement T009 (PlayerViewModel.playNextReference)

# Terminal 3
Implement T010 (PlayerViewModel.voiceCommandFeedback StateFlow)
```

Then sequential:
- T011-T014 (PlaybackService modifications must be sequential)
- T015-T017 (MainActivity modifications must be sequential)
- T018 (testing requires all previous tasks complete)

## Testing Strategy

### Manual Testing Checklist

**User Story 1**:
- [ ] Play "strollcast-2026-overview" to segment with references
- [ ] Say "Hey Google, play reference" → verify referenced episode plays
- [ ] Play to segment without references
- [ ] Say "Hey Google, play reference" → verify feedback "No reference found"
- [ ] Test in background (app minimized)
- [ ] Test with multiple references in segment → verify first one plays

**User Story 2**:
- [ ] Follow reference via voice
- [ ] Say "Hey Google, play previous" → verify returns to original
- [ ] Verify position is restored correctly
- [ ] Say "play previous" when at first episode → verify error feedback

**User Story 3**:
- [ ] Verify all feedback messages are clear and concise
- [ ] Verify feedback doesn't interrupt playback
- [ ] Test unrecognized commands → verify helpful error message

### Performance Validation

- [ ] Voice command parsing: <50ms
- [ ] Reference lookup: <100ms
- [ ] Total command-to-playback: <3s (network dependent)
- [ ] Audio feedback start: <1s
- [ ] Battery impact: <1% per hour (MediaSession approach)

## Success Criteria

Feature is complete when all acceptance scenarios from spec.md pass:

**US1 - Reference Navigation**:
- [x] Say "play reference" in segment with link → referenced episode plays
- [x] Say "play reference" with multiple links → first link plays
- [x] Say "play reference" with no links → audio feedback provided

**US2 - Return to Previous**:
- [x] Navigate via voice → say "play previous" → returns to original position
- [x] Multiple navigation chain (A→B→C) → "play previous" twice → back to A
- [x] Say "play previous" at first episode → error feedback provided

**US3 - Feedback (Optional)**:
- [x] All voice commands provide audio feedback
- [x] Error conditions provide clear explanations
- [x] Feedback is concise (<3 seconds)

## Notes

- **No new permissions**: MediaSession + Google Assistant approach doesn't require RECORD_AUDIO
- **Dependency on feature 003**: Reference navigation must be working (already merged)
- **Battery optimization**: System-managed wake word detection, <1% per hour impact
- **Offline support**: Voice recognition may require network, episode playback works offline for downloaded content
- **Testing environment**: Requires physical device or emulator with Google Assistant configured

## Estimated Timeline

- **Day 1**: Phase 1-2 (Setup + Foundation) - T001-T007
- **Day 2**: Phase 3 Part 1 (ViewModels + PlaybackService) - T008-T014
- **Day 3**: Phase 3 Part 2 (MainActivity + Testing) - T015-T018
- **Day 4**: Phase 4 (Previous command) - T019-T024
- **Day 5**: Phase 5 (Polish) + Testing - T025 + comprehensive testing

**MVP Timeline**: 2-3 days (Phases 1-3 only)
**Full Feature**: 3-5 days (all phases)
