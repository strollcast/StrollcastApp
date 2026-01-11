# Feature Specification: Reference Navigation in Transcripts

**Feature Branch**: `003-reference-navigation`
**Created**: 2026-01-11
**Status**: Draft
**Input**: User description: "The ios application has the ability to 'go to reference' or 'play reference'. Can you implement a similar feature in Android? The links are defined in the transcript file."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Tap Reference Link to Navigate (Priority: P1)

While viewing a transcript during podcast playback, users see certain transcript segments contain blue hyperlinks referencing other episodes. When they tap these links, the app immediately loads and begins playing the referenced episode, allowing seamless navigation between related content.

**Why this priority**: This is the core functionality - enabling users to follow references between episodes while listening. Without this, users cannot explore related content mentioned in episodes.

**Independent Test**: Can be fully tested by loading any episode with transcript references, tapping a link, and verifying the referenced episode starts playing. Delivers immediate value by connecting related episodes.

**Acceptance Scenarios**:

1. **Given** a transcript segment contains a markdown link to another episode, **When** the user taps the link in the transcript view, **Then** the referenced episode loads and begins playing from the start
2. **Given** the user is playing Episode A which references Episode B, **When** the user taps the reference link, **Then** Episode A is added to playback history and Episode B starts playing
3. **Given** a referenced episode is downloaded locally, **When** the user taps the reference link, **Then** the app plays the local copy instead of streaming

---

### User Story 2 - Visual Indication of Reference Links (Priority: P1)

Users viewing transcripts can immediately identify which segments contain references to other episodes through visual styling (blue text, underline, or link icon). This helps users quickly scan for related content without reading every word.

**Why this priority**: Users need to discover that references exist. Without visual differentiation, they won't know links are tappable or that cross-references exist at all.

**Independent Test**: Can be fully tested by viewing any transcript with references and verifying links are visually distinct from regular text. Delivers value by making references discoverable.

**Acceptance Scenarios**:

1. **Given** a transcript segment contains a markdown link, **When** the transcript is displayed, **Then** the link text appears in blue with an underline or distinct styling
2. **Given** multiple links exist in a single transcript segment, **When** viewing that segment, **Then** each link is individually styled and tappable
3. **Given** the user is viewing an active (currently playing) transcript segment with a link, **When** the segment is highlighted, **Then** the link styling remains visible against the highlight color

---

### User Story 3 - Return to Original Episode (Priority: P2)

After navigating to a referenced episode via a transcript link, users can return to the original episode they were listening to by using the "Play Previous" functionality (already exists in Android app via PlaybackHistoryManager).

**Why this priority**: While important for navigation flow, the existing playback history feature already provides this capability. This story just ensures it works correctly with reference navigation.

**Independent Test**: Can be tested by following a reference link, then using existing "previous" controls to verify return to original episode at the previous position.

**Acceptance Scenarios**:

1. **Given** the user navigated from Episode A to Episode B via reference link, **When** the user selects "Play Previous" (or back button in player), **Then** Episode A resumes at the position where they left off
2. **Given** the user followed multiple reference links (A → B → C), **When** the user repeatedly selects "Play Previous", **Then** they can navigate back through the chain (C → B → A)

---

### Edge Cases

- What happens when a reference link points to an episode that doesn't exist in the API?
  - Display error message: "Referenced episode not found" and remain on current episode
- What happens if a reference link is malformed or points to external websites?
  - Only parse and handle links matching the Strollcast episode URL format (https://released.strollcast.com/episodes/{id}/{id}.mp3 or .m4a)
  - Ignore non-episode links (leave as non-clickable text)
- What happens when the user taps a reference link while offline and the referenced episode isn't downloaded?
  - Show error message: "Referenced episode requires internet connection. Download it first to access offline."
- What happens when multiple links exist in a single transcript segment?
  - Each link is independently clickable with proper touch target spacing
- What happens when the currently playing segment contains a reference to the currently playing episode?
  - The link should still be clickable but essentially reload the current episode from the start (edge case, but should not crash)

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST parse markdown-style links from transcript text in the format `[link text](url)`
- **FR-002**: System MUST extract the episode ID from reference URLs in the format `https://released.strollcast.com/episodes/{episode-id}/{episode-id}.mp3` or `.m4a`
- **FR-003**: System MUST render reference links with distinct visual styling (blue color and underline) in the transcript view
- **FR-004**: Users MUST be able to tap reference links to navigate to the referenced episode
- **FR-005**: System MUST fetch episode metadata from the API when a reference link is tapped
- **FR-006**: System MUST add the current episode to playback history before loading a referenced episode
- **FR-007**: System MUST prefer local downloaded copies of referenced episodes over streaming when available
- **FR-008**: System MUST display appropriate error messages when referenced episodes cannot be loaded (not found, offline, etc.)
- **FR-009**: System MUST only recognize and make clickable links that match the Strollcast episode URL pattern
- **FR-010**: System MUST maintain proper touch target sizing for reference links (minimum 48dp per Android guidelines)

### Key Entities

- **Transcript Line**: Already exists in Android app as `TranscriptLineEntity`
  - Contains: id, transcriptId, startMs, endMs, speaker, text, lineNumber
  - The `text` field contains the markdown with embedded reference links
- **Episode Reference**: No new entity needed
  - Extracted transiently from transcript text when parsing markdown links
  - Contains: episode ID (extracted from URL), display text (from markdown)
- **Playback History**: Already exists in Android app via PlaybackHistoryManager
  - Used to enable returning to the original episode after following references

### Mobile-Specific Requirements

- **Platform Scope**: Android only
  - Justification: iOS already has this feature implemented via voice commands. This brings Android to feature parity. The iOS implementation uses App Intents for voice control ("Hey Siri, go to reference"), while Android will implement direct tap interaction in the transcript UI.
- **Offline Behavior**:
  - When offline, only allow navigation to downloaded episodes
  - Show error message when attempting to follow reference to non-downloaded episode
  - Reference links should still be visually styled but show error on tap when network unavailable
- **Permissions Required**: None (uses existing network and storage permissions)
- **Background Capabilities**: None required (playback already supported in background)
- **Platform Integration**: None specific to this feature

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can identify reference links in transcripts within 2 seconds of viewing a segment with links (measured by distinct visual styling)
- **SC-002**: Users can tap a reference link and have the referenced episode begin playing within 3 seconds (assuming normal network conditions)
- **SC-003**: The system successfully parses and renders 100% of markdown-formatted episode links in transcript files
- **SC-004**: Users can navigate to referenced episodes offline when those episodes are pre-downloaded (100% success rate for downloaded content)
- **SC-005**: Users can return to their original episode after following references by using existing playback history controls (100% success rate)
- **SC-006**: The feature handles all edge cases (non-existent episodes, malformed links, offline access) with appropriate user-facing error messages instead of crashes (0% crash rate)

## Assumptions *(optional)*

- **A-001**: Transcript VTT files already contain markdown-formatted links in the `text` field (confirmed by examining actual transcript files from the API)
- **A-002**: The Android app's existing `TranscriptLineEntity` and parsing logic preserves markdown formatting in the text field (not stripped during VTT parsing)
- **A-003**: The episode API endpoint (https://api.strollcast.com/episodes) remains available and returns episodes in the same format
- **A-004**: Reference URLs follow the consistent pattern: `https://released.strollcast.com/episodes/{episode-id}/{episode-id}.(mp3|m4a)`
- **A-005**: The existing playback history manager properly handles the reference navigation pattern (adding episodes to history before switching)

## Dependencies *(optional)*

- **D-001**: Requires existing transcript display functionality in Android app (TranscriptScreen.kt)
- **D-002**: Requires existing playback controls (PlayerViewModel)
- **D-003**: Requires existing episode API integration (PodcastRepository)
- **D-004**: Requires existing download manager for offline episode detection
- **D-005**: Requires existing playback history manager for "return to previous episode" functionality

## Out of Scope *(optional)*

- Voice command integration (Android doesn't have iOS's App Intents equivalent; would require Google Assistant integration which is significant effort)
- Visual preview of referenced episode (thumbnail, title) on hover or long-press
- In-transcript indicators showing which episodes have already been listened to
- Smart reference suggestions based on current position or interests
- Reference link analytics (tracking which references users follow most)
- Bidirectional reference graph visualization
- External link support (links to papers, GitHub, etc. - only episode references are in scope)
