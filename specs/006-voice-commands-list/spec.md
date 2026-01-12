# Feature Specification: Voice Commands Help List in Settings

**Feature Branch**: `006-voice-commands-list`
**Created**: 2026-01-11
**Status**: Draft
**Input**: User description: "Add the list of the audio commands under settings for both ios and android"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - View Available Voice Commands (Priority: P1)

Users want to discover what voice commands are available in the app without having to search documentation or guess. They navigate to Settings and find a "Voice Commands" section that displays a clear, organized list of all supported commands with descriptions of what each command does.

**Why this priority**: This is essential for discoverability. Voice interfaces lack visual affordances, so users need an easily accessible reference to learn what commands they can use. Without this, users may not discover the voice navigation features implemented in feature 004.

**Independent Test**: Can be fully tested by opening Settings on both platforms and verifying that a "Voice Commands" section appears with a complete list of available commands and their descriptions.

**Acceptance Scenarios**:

1. **Given** the user opens Settings on iOS, **When** they scroll to the Voice Commands section, **Then** they see a list displaying all available voice commands with descriptions
2. **Given** the user opens Settings on Android, **When** they scroll to the Voice Commands section, **Then** they see a list displaying all available voice commands with descriptions
3. **Given** the voice commands list is displayed, **When** the user reads the list, **Then** each command shows the voice phrase(s) to say and what action it performs
4. **Given** the user is viewing the voice commands list, **When** they tap a command, **Then** they can see expanded details or examples (optional enhancement)

---

### Edge Cases

- What happens when no voice commands are available (e.g., on a platform without voice support)? (Assumption: Both iOS and Android support voice commands, so this won't occur)
- What happens when new voice commands are added in future updates? (Assumption: The list is maintained manually or pulled from a central command registry)
- How does the list appear on very small screens? (The list should be scrollable and use standard platform list UI patterns)
- What happens when the user is in landscape orientation? (List should remain readable and properly formatted)

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: iOS Settings screen MUST display a "Voice Commands" section
- **FR-002**: Android Settings screen MUST display a "Voice Commands" section
- **FR-003**: Voice Commands section MUST list all currently supported voice commands
- **FR-004**: Each voice command entry MUST display the command phrase(s) to speak
- **FR-005**: Each voice command entry MUST display a description of what the command does
- **FR-006**: Voice Commands section MUST include "play reference" / "jump to reference" command
- **FR-007**: Voice Commands section MUST include "play previous" / "jump to previous" command
- **FR-008**: Voice commands list MUST be readable without requiring horizontal scrolling
- **FR-009**: Voice commands list MUST be scrollable if content exceeds screen height
- **FR-010**: Voice commands list MUST use platform-appropriate UI styling (iOS list style vs Android Material Design)

### Key Entities

- **Voice Command Entry**: Represents a single voice command with properties including command phrase(s), description, and optional examples or notes

### Mobile-Specific Requirements

- **Platform Scope**: Both iOS and Android
  - Implementation order: Both platforms simultaneously to maintain feature parity
  - Both platforms must display identical command information
- **Offline Behavior**: No network dependency - voice commands list is static content embedded in the app
- **Permissions Required**: None
- **Background Capabilities**: N/A - Settings screen only visible when app is in foreground
- **Platform Integration**:
  - iOS: Uses SwiftUI List or Form with Section for organizing settings
  - Android: Uses Compose LazyColumn with preference-style UI
  - Both must follow platform-specific design guidelines for settings/preferences screens

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can locate the Voice Commands section in Settings within 10 seconds of opening the Settings screen
- **SC-002**: 100% of supported voice commands are displayed in the list on both platforms
- **SC-003**: Each voice command entry is readable without truncation on standard device screen sizes
- **SC-004**: Visual inspection confirms voice commands list appears identically on both iOS and Android (same commands, descriptions)
- **SC-005**: User testing shows 90% of users can successfully find and read the voice commands list

## Out of Scope

- Interactive voice command testing from within Settings (e.g., "tap to try" buttons)
- Voice command tutorials or walkthroughs
- Command history or frequently used commands tracking
- Customization of voice commands or wake words
- Voice command settings or toggles to enable/disable specific commands
- Animated demonstrations of voice commands
- Search or filtering of voice commands list
- Localization of voice commands for non-English languages

## Assumptions

- The app currently supports English voice commands only (localization may be added separately)
- Voice commands are triggered via Google Assistant (Android) and Siri (iOS) integration
- The list of voice commands is relatively short (2-5 commands initially) and won't require search/filter functionality
- Voice commands are available on all supported device versions
- Users accessing Settings screen are already familiar with basic app navigation

## Voice Commands to Display

Based on feature 004 (voice-reference-navigation), the initial list includes:

1. **"Play reference"** or **"Jump to reference"**
   - Description: "Navigate to the episode referenced in the current transcript segment"

2. **"Play previous"** or **"Jump to previous"**
   - Description: "Return to the previous episode in your listening history"

## Design Considerations

- **Section Placement**: Voice Commands section should appear logically within Settings, grouped with other feature help/information
- **Visual Hierarchy**: Command phrases should be prominent (bold or larger text), descriptions should be secondary
- **Platform Consistency**: While respecting platform design guidelines, information content should be identical
- **Future Extensibility**: Design should accommodate additional commands as voice features expand
