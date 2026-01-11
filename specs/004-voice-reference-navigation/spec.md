# Feature Specification: Voice Navigation for Reference Playback

**Feature Branch**: `004-voice-reference-navigation`
**Created**: 2026-01-11
**Status**: Draft
**Input**: User description: "for 'go to reference' I also want to have voice navigation, in addition to tap navigation. The user should be able to just say 'play reference'/'jump to reference' or 'play previous'/'jump to previous'"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Voice Command for Reference Navigation (Priority: P1)

While listening to a podcast episode with their device nearby, users hear a reference to another episode and want to navigate to it hands-free. They say "play reference" or "jump to reference" and the app immediately loads and plays the referenced episode without requiring them to touch the screen or find the link in the transcript.

**Why this priority**: This is the core hands-free navigation feature that brings Android to full parity with iOS. Users listening while driving, exercising, or cooking need voice control for safe and convenient navigation.

**Independent Test**: Can be fully tested by playing any episode with transcript references, speaking the voice command, and verifying the referenced episode starts playing. Delivers immediate value by enabling hands-free reference navigation.

**Acceptance Scenarios**:

1. **Given** the user is listening to an episode where the current transcript segment contains a reference link, **When** the user says "play reference" or "jump to reference", **Then** the referenced episode loads and begins playing from the start
2. **Given** the current segment has multiple reference links, **When** the user says "play reference", **Then** the app plays the first reference link found in that segment
3. **Given** the user is listening to an episode but the current segment has no reference links, **When** the user says "play reference", **Then** the app responds with audio feedback: "No reference found in current segment"

---

### User Story 2 - Voice Command for Returning to Previous Episode (Priority: P1)

After navigating to a referenced episode via voice command or tap, users want to return to the original episode they were listening to. They say "play previous" or "jump to previous" and the app returns them to where they left off in the original episode.

**Why this priority**: Completes the hands-free navigation loop. Users need a way to return without touching the screen, making the voice control feature fully functional for hands-free use cases.

**Independent Test**: Can be tested by following a reference via voice, then using the voice command to return. Verifies the complete voice navigation cycle works.

**Acceptance Scenarios**:

1. **Given** the user navigated from Episode A to Episode B via voice command, **When** the user says "play previous" or "jump to previous", **Then** Episode A resumes at the position where they left off
2. **Given** the user followed multiple reference links (A → B → C), **When** the user repeatedly says "play previous", **Then** they can navigate back through the chain (C → B → A)
3. **Given** the user is on the first episode in playback history with no previous episode, **When** the user says "play previous", **Then** the app responds with audio feedback: "Already at first episode"

---

### User Story 3 - Voice Command Discovery and Feedback (Priority: P2)

Users need to know what voice commands are available and receive clear feedback when commands succeed or fail. The app provides audio responses to confirm actions or explain why a command couldn't be executed.

**Why this priority**: Voice interfaces require clear feedback since users can't see visual state. However, this is lower priority than the core navigation functionality.

**Independent Test**: Can be tested by issuing various voice commands and verifying appropriate audio feedback is provided.

**Acceptance Scenarios**:

1. **Given** the user successfully executes a voice command, **When** the app processes the command, **Then** the app provides brief audio confirmation before executing the action
2. **Given** the user issues a voice command that cannot be executed, **When** the app detects the error condition, **Then** the app provides clear audio explanation of why the command failed
3. **Given** the user is new to the app, **When** they ask "what can I say" or "help", **Then** the app lists available voice commands via audio response

---

### Edge Cases

- What happens when the user says "play reference" but the current segment has no references?
  - Provide audio feedback: "No reference found in current segment"
- What happens when the current segment has multiple reference links?
  - Play the first reference link found in the segment
  - Alternatively: Provide audio feedback "Found [N] references. Playing first reference: [episode title]"
- What happens when the user says "play reference" while offline and the referenced episode isn't downloaded?
  - Provide audio feedback: "Referenced episode requires internet connection. Download it first to access offline."
- What happens when the referenced episode doesn't exist in the API?
  - Provide audio feedback: "Referenced episode not found"
- What happens when the user says "play previous" with no previous episode in history?
  - Provide audio feedback: "Already at first episode"
- What happens when the app is playing but not currently in a transcript segment (e.g., between segments)?
  - Search the most recent transcript segment for references
- What happens when the user issues voice commands in rapid succession?
  - Queue commands and execute sequentially, or ignore subsequent commands until current action completes
- What happens when the device microphone permissions are denied?
  - Display permission request dialog, explain voice commands won't work without microphone access
- What happens when voice recognition fails or doesn't understand the command?
  - Provide audio feedback: "Sorry, I didn't understand that. Try 'play reference' or 'play previous'"

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST support voice command "play reference" to navigate to referenced episode in current transcript segment
- **FR-002**: System MUST support voice command "jump to reference" as alternative to "play reference"
- **FR-003**: System MUST support voice command "play previous" to return to previous episode in playback history
- **FR-004**: System MUST support voice command "jump to previous" as alternative to "play previous"
- **FR-005**: System MUST provide audio feedback when voice commands succeed or fail
- **FR-006**: System MUST scan the current transcript segment for reference links when "play reference" command is issued
- **FR-007**: System MUST add the current episode to playback history before navigating to referenced episode via voice command
- **FR-008**: System MUST handle cases where no reference exists in current segment with appropriate audio feedback
- **FR-009**: System MUST handle cases where no previous episode exists with appropriate audio feedback
- **FR-010**: System MUST work both when app is in foreground and when audio is playing in background
- **FR-011**: System MUST request microphone permission if not already granted
- **FR-012**: System MUST support voice commands in English language
- **FR-013**: System MUST play the first reference link when multiple references exist in current segment

### Key Entities

- **Voice Command**: New transient object
  - Contains: command type (play_reference, jump_to_reference, play_previous, jump_to_previous), timestamp, execution status
- **Transcript Segment Context**: Derived from existing TranscriptLineEntity
  - Contains: current segment text, parsed reference links, segment timestamp
- **Playback History**: Already exists via PlaybackHistoryManager
  - Used to enable "play previous" voice command

### Mobile-Specific Requirements

- **Platform Scope**: Android only
  - Justification: iOS already has this feature via App Intents and Siri. Android needs equivalent voice command functionality for hands-free reference navigation.
- **Offline Behavior**:
  - Voice recognition may require internet connection depending on device capabilities
  - Voice commands should work offline if device supports on-device speech recognition
  - Referenced episode navigation follows same offline rules as tap navigation (only downloaded episodes)
- **Permissions Required**:
  - RECORD_AUDIO: Required for voice command recognition
  - Rationale: Essential for hands-free voice control
- **Background Capabilities**:
  - Voice commands must work when app is in background (audio playing in notification)
  - May require foreground service notification for continuous listening
- **Platform Integration**:
  - Integration with platform voice recognition capabilities
  - Media session controls may need voice command integration

### Non-Functional Requirements

- **NFR-001**: Voice command recognition latency MUST be under 1 second from end of speech to command execution
- **NFR-002**: Audio feedback MUST be concise (under 3 seconds) to avoid disrupting listening experience
- **NFR-003**: Voice recognition MUST have accuracy of at least 90% for supported commands in quiet environments
- **NFR-004**: Battery impact of continuous listening MUST be under 5% per hour when app is in background

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can successfully navigate to referenced episodes via voice command with 90% success rate in quiet environments
- **SC-002**: Voice command execution completes within 3 seconds from command utterance to episode playback start (assuming normal network conditions)
- **SC-003**: Users receive audio feedback for all voice commands (success or failure) within 1 second of command recognition
- **SC-004**: Users can return to previous episode via voice command with 100% success rate when previous episode exists in history
- **SC-005**: Voice commands work both in foreground and background with equal reliability (90% success rate)
- **SC-006**: Voice command feature handles all edge cases (no references, no previous, offline) with clear audio feedback instead of silent failures (0% silent failure rate)

## Assumptions *(optional)*

- **A-001**: Android devices have built-in microphone and support voice input
- **A-002**: Most users have granted or will grant microphone permission for voice features
- **A-003**: Voice commands will primarily be used in relatively quiet environments (home, car, office)
- **A-004**: The existing transcript synchronization accurately tracks "current segment" during playback
- **A-005**: Text-to-speech (TTS) is available on Android devices for audio feedback
- **A-006**: Platform-provided voice recognition capabilities are sufficient for command recognition (no need for custom ML model)

## Dependencies *(optional)*

- **D-001**: Requires platform voice recognition capabilities
- **D-002**: Requires platform text-to-speech (TTS) capabilities for audio feedback
- **D-003**: Requires existing reference navigation functionality (feature 003-reference-navigation)
- **D-004**: Requires existing transcript synchronization to determine "current segment"
- **D-005**: Requires existing playback history manager for "play previous" functionality
- **D-006**: Requires microphone permission (RECORD_AUDIO)

## Out of Scope *(optional)*

- Voice commands for other app functions (play/pause, seek, speed control) - focus is only on reference navigation
- Custom wake words ("Hey Strollcast") - users will use system voice assistant or app-specific activation button
- Voice command training or customization
- Multi-language voice command support (English only in initial version)
- Voice command history or analytics
- Contextual voice responses with detailed episode information
- Integration with Google Assistant routines or shortcuts
- Voice-controlled note-taking while listening
- Conversation-style voice interaction ("tell me about this episode")
