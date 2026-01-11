# Feature Specification: Android Feature Parity

**Feature Branch**: `002-android-parity`
**Created**: 2026-01-11
**Status**: Draft
**Input**: User description: "Implement the missing features in the android app as compared with the ios one."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Transcript View with Tap-to-Seek (Priority: P1)

As an Android user listening to podcast episodes, I need to view the transcript alongside playback and tap any line to jump to that point in the audio, so I can quickly navigate to sections of interest and review content while listening.

**Why this priority**: Transcript navigation is a core feature heavily used in the iOS app. Its absence creates a significant feature gap that impacts user experience and competitive parity. This is the most visible missing feature.

**Independent Test**: Can be fully tested by playing an episode, opening the transcript view, tapping any transcript line, and verifying playback jumps to the corresponding timestamp.

**Acceptance Scenarios**:

1. **Given** an episode is playing, **When** user opens transcript view, **Then** transcript is displayed with synchronized highlighting of current line
2. **Given** transcript is visible, **When** user taps any transcript line, **Then** playback jumps to that timestamp and highlighting updates
3. **Given** transcript is visible, **When** user scrolls, **Then** playback continues and auto-scroll can be toggled on/off
4. **Given** no internet connection, **When** user opens transcript, **Then** cached transcript is displayed if previously loaded

---

### User Story 2 - Inline Notes on Transcripts (Priority: P2)

As an Android user studying research papers through podcasts, I need to add notes directly on transcript lines so I can capture thoughts, questions, and insights at specific points in the episode for later review.

**Why this priority**: Note-taking transforms passive listening into active learning. iOS users rely on this for research and study workflows. Essential for academic/research audience.

**Independent Test**: Can be tested by viewing a transcript, adding a note to any line, closing and reopening the app, and verifying the note persists and is displayed at the correct transcript position.

**Acceptance Scenarios**:

1. **Given** transcript is visible, **When** user long-presses a line, **Then** note creation UI appears
2. **Given** note creation UI is open, **When** user enters text and saves, **Then** note is attached to that transcript line with visual indicator
3. **Given** a line has notes, **When** user taps the note indicator, **Then** notes are displayed with edit/delete options
4. **Given** multiple notes exist, **When** user navigates to notes view, **Then** all notes across episodes are listed with context

---

### User Story 3 - Listening History and Played Episodes (Priority: P3)

As an Android user who has completed several episodes, I need to see a dedicated list of played/finished episodes so I can easily revisit content and track my listening progress without scrolling through all episodes.

**Why this priority**: iOS has a separate "Played" tab. Android users currently lack easy access to completed episodes. Lower priority than transcript features but important for user organization.

**Independent Test**: Can be tested by completing an episode, navigating to a "Played" section, and verifying the episode appears with completion status and last played date.

**Acceptance Scenarios**:

1. **Given** user completes an episode (listens to 90%+), **When** viewing played list, **Then** episode appears with completion badge
2. **Given** multiple episodes are completed, **When** viewing played list, **Then** episodes are sorted by most recently completed
3. **Given** played list is visible, **When** user taps an episode, **Then** episode details open with option to replay from start
4. **Given** offline mode, **When** viewing played list, **Then** completion data is available from local database

---

### User Story 4 - Voice Commands for Playback Control (Priority: P4)

As an Android user listening while driving or exercising, I need hands-free voice control to pause, play, skip forward/backward, and navigate episodes so I can safely control playback without touching my device.

**Why this priority**: iOS supports voice commands via VoiceCommandService. Android has system-level voice assistant integration opportunities. Lower priority as physical controls work, but valuable for accessibility and safety.

**Independent Test**: Can be tested by saying "Hey Google, skip forward 30 seconds in Strollcast" and verifying the app responds correctly while screen is off.

**Acceptance Scenarios**:

1. **Given** episode is playing, **When** user says voice command "pause", **Then** playback pauses
2. **Given** episode is paused, **When** user says "play", **Then** playback resumes
3. **Given** episode is playing, **When** user says "skip forward 15 seconds", **Then** playback advances 15 seconds
4. **Given** app supports Assistant integration, **When** user asks "what's playing", **Then** Assistant reports current episode title and progress

---

### Edge Cases

- What happens when transcript file is corrupted or missing? Display error message and allow playback to continue without transcript.
- What happens when user adds a note while offline? Note is saved locally and synced when online (if cloud sync is implemented).
- What happens if played episode data conflicts between devices? Use last-write-wins with most recent timestamp.
- What happens when voice command is ambiguous? Show disambiguation UI or use default action (e.g., skip forward defaults to 15 seconds).
- What happens when transcript is very long (2+ hours)? Implement virtual scrolling/pagination to avoid performance issues.
- What happens when user taps transcript while network is loading? Queue the seek action and execute when transcript fully loads.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST display episode transcripts in a scrollable view alongside playback controls
- **FR-002**: System MUST support tap-to-seek where tapping any transcript line jumps playback to that timestamp
- **FR-003**: System MUST highlight the current transcript line in real-time as audio plays
- **FR-004**: System MUST allow users to toggle auto-scroll (transcript follows playback) on/off
- **FR-005**: System MUST cache transcripts locally for offline access after first load
- **FR-006**: System MUST support creating notes attached to specific transcript lines
- **FR-007**: Notes MUST persist across app restarts in local database (Room)
- **FR-008**: System MUST provide a unified notes view showing all notes across episodes with context
- **FR-009**: System MUST allow editing and deleting existing notes
- **FR-010**: System MUST track episode completion status (considered complete at 90% played)
- **FR-011**: System MUST provide a "Played" or "History" section listing completed episodes
- **FR-012**: Played list MUST display completion date, duration, and allow replay from start
- **FR-013**: System MUST integrate with Android voice assistant for basic playback commands (play, pause, skip)
- **FR-014**: System MUST respond to "skip forward X seconds" and "skip backward X seconds" voice commands
- **FR-015**: All features MUST work offline using cached/local data where applicable

### Key Entities

- **Note**: User-created annotation attached to a specific transcript line; attributes include text content, timestamp, episode reference, creation/modification dates
- **TranscriptLine**: Individual line of episode transcript; attributes include text content, start timestamp, end timestamp, associated notes indicator
- **PlaybackHistory**: Record of user's listening progress; attributes include episode ID, completion percentage, last played timestamp, completion status
- **CompletedEpisode**: Episode marked as fully listened; attributes include episode reference, completion date, total listen time

### Mobile-Specific Requirements

- **Platform Scope**: Android only
  - **Justification**: This feature specifically addresses Android's feature gap compared to iOS. iOS already has these features implemented.
  - **Parity Timeline**: All P1 and P2 features should be implemented to achieve core parity. P3 and P4 can follow in subsequent releases.
- **Offline Behavior**:
  - Transcripts cached after first view
  - Notes saved locally first, sync later if cloud implemented
  - Played history maintained in local database
  - All features function without network except initial transcript download
- **Permissions Required**:
  - Microphone permission (optional, only if voice commands are implemented via app-level speech recognition)
  - No additional permissions for transcript/notes features
- **Background Capabilities**:
  - Media playback continues in background
  - Voice commands work with screen off via MediaSession integration
- **Platform Integration**:
  - Android Assistant integration for voice commands
  - MediaSession for lock screen controls
  - Notification controls remain functional

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Android users can navigate episode transcripts and jump to any point in playback within 2 seconds
- **SC-002**: Users can create and retrieve notes on transcript lines without data loss across app restarts
- **SC-003**: 90% of Android users can discover and use the played episodes list within first week of release
- **SC-004**: Voice commands successfully execute playback actions with 95% accuracy when supported
- **SC-005**: Feature parity reduces user complaints about "Android missing features" by 80% within 3 months
- **SC-006**: Transcript view loads and displays within 3 seconds on 4G connection
- **SC-007**: Android app achieves feature rating of 4.0+ stars (currently may be lower due to feature gaps)

## Assumptions

- Episode transcripts are available via the same API endpoint that iOS uses (VTT format or similar)
- Android app's existing Room database can be extended with new tables for notes and history
- Current PodcastRepository pattern can be extended to handle transcript fetching and caching
- MediaSession in PlaybackService can be enhanced to support voice command intents
- Android app follows MVVM architecture with ViewModels managing UI state for new features
- Jetpack Compose is used for all new UI components (transcript view, notes UI, played list)
- Notes are stored locally only (no cloud sync) unless explicitly specified in implementation phase
- Voice commands leverage Android's existing Assistant/MediaSession integration rather than custom speech recognition

## Feature Comparison Matrix

**Current State**:

| Feature | iOS | Android | Gap |
|---------|-----|---------|-----|
| Transcript View | ✅ | ❌ | High |
| Tap-to-Seek on Transcript | ✅ | ❌ | High |
| Inline Notes | ✅ | ❌ | High |
| Played Episodes List | ✅ | ❌ | Medium |
| Voice Commands | ✅ | ❌ | Medium |
| Background Playback | ✅ | ✅ | ✅ Parity |
| Download Management | ✅ | ✅ | ✅ Parity |
| Zotero Integration | ✅ | ✅ | ✅ Parity |
| Progress Tracking | ✅ | ✅ | ✅ Parity |

**Target State (Post-Implementation)**:

All features should show ✅ for both platforms, maintaining platform-native implementations while providing equivalent user capabilities.
