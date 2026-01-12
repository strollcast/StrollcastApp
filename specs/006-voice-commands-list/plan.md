# Implementation Plan: Voice Commands Help List in Settings

**Branch**: `006-voice-commands-list` | **Date**: 2026-01-11 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/006-voice-commands-list/spec.md`

## Summary

Add a "Voice Commands" section to the Settings screen on both iOS and Android that displays a list of available voice commands with descriptions. This improves discoverability of the voice navigation features implemented in feature 004, helping users learn what commands they can speak for hands-free navigation.

**Technical Approach**: Add static UI section to existing Settings screens using platform-native list/section components. No data persistence, API calls, or complex logic required - purely informational help content.

## Technical Context

**Language/Version**: Swift 5.9+ (iOS), Kotlin 1.9+ (Android)
**Primary Dependencies**:
- iOS: SwiftUI (native)
- Android: Jetpack Compose + Material3 (already in use)
**Storage**: N/A (static content only)
**Testing**: Manual UI testing on both platforms
**Target Platform**: iOS 15+, Android SDK 26+
**Project Type**: Mobile (dual platform - iOS and Android)
**Performance Goals**: Instant UI rendering (<16ms), no network latency
**Constraints**:
- Must fit on screen without horizontal scrolling
- Must be readable on small devices (iPhone SE, small Android phones)
- Must use platform-appropriate styling
**Scale/Scope**:
- 2 voice commands initially (can grow)
- Single Settings screen modification per platform
- ~50-100 lines of UI code per platform

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Verify compliance with principles from `.specify/memory/constitution.md`:

- [x] **Cross-Platform Parity**: YES - Feature requires implementation on both iOS and Android simultaneously. Both platforms will display identical command information using platform-native UI patterns.
- [x] **API Contract Stability**: N/A - No API changes required. Feature uses static content only.
- [x] **Offline-First Architecture**: COMPLIANT - Feature works 100% offline. No network requests. Content is embedded in the app bundle.
- [x] **Platform-Native UI**: COMPLIANT - iOS uses SwiftUI `List` with `Section`, Android uses Compose `LazyColumn` with Material3 styling. Both follow platform design guidelines.
- [x] **Service-Oriented Architecture**: N/A - No business logic required. This is pure UI/presentation layer. No services needed for displaying static help text.
- [x] **Feature Flags & Graceful Degradation**: N/A - No optional dependencies or external services. Feature is always available.

**Violations requiring justification**: None

## Project Structure

### Documentation (this feature)

```text
specs/006-voice-commands-list/
├── plan.md              # This file
├── data-model.md        # Minimal - just VoiceCommand data structure
├── quickstart.md        # Integration guide for Settings screens
└── tasks.md             # Task breakdown (created by /speckit.tasks)
```

### Source Code (repository root)

```text
# Both Platforms (Cross-Platform Feature)
ios/StrollcastApp/
└── Views/
    └── SettingsView.swift    # Modify: Add Voice Commands section

android/app/src/main/java/com/strollcast/app/
└── ui/screens/
    └── SettingsScreen.kt     # Modify: Add Voice Commands section
```

**Structure Decision**: Both platforms modified simultaneously. iOS and Android Settings screens each get a new section added inline. No new files required - modifications to existing Settings views only.

## Complexity Tracking

N/A - No constitution violations.

## Phase 0: Research

No research phase needed for this feature. Technical approach is straightforward:
- Add UI section to existing Settings screens
- Use platform-native list components already in use
- Display static text content
- No external dependencies, APIs, or complex patterns required

## Phase 1: Design

### Data Model

Simple data structure for representing voice commands:

```text
VoiceCommand {
  phrases: [String]       # e.g., ["play reference", "jump to reference"]
  description: String     # e.g., "Navigate to referenced episode"
  example: String?        # Optional usage example (future)
}
```

Commands are hardcoded in UI code, not fetched from data source.

### Quickstart Integration

**iOS (SettingsView.swift)**:
1. Locate existing `Form` or `List` in SettingsView
2. Add new `Section(header: Text("Voice Commands"))` after existing sections
3. Inside section, add `ForEach` over hardcoded command list
4. Each item displays command phrases (bold) and description (secondary text)

**Android (SettingsScreen.kt)**:
1. Locate existing `LazyColumn` in SettingsScreen
2. Add new section with header "Voice Commands"
3. Add `items()` block with hardcoded command list
4. Each item uses `ListItem` with headline (command) and supporting text (description)

### API Contracts

N/A - No API changes required.

## Platform-Specific Implementation Notes

### iOS
- Use `Text().font(.headline)` for command phrases
- Use `Text().font(.subheadline).foregroundColor(.secondary)` for descriptions
- Leverage SwiftUI's native `Section` with header
- Alternative commands separated by "or" in same row

### Android
- Use `ListItem` with `headlineContent` for command phrases
- Use `supportingContent` for descriptions
- Follow Material3 list styling conventions
- Alternative commands formatted as "command1 or command2"

### Visual Consistency
While respecting platform differences, both should present:
1. Section header: "Voice Commands"
2. Two items initially:
   - "Play reference" / "Jump to reference" → description
   - "Play previous" / "Jump to previous" → description

## Dependencies

- **Feature 004 (voice-reference-navigation)**: Provides the actual commands being documented
- **Existing Settings screens**: Must integrate into current Settings UI structure

## Prerequisites & Voice Command Verification

**IMPORTANT**: Before documenting voice commands in Settings, we must verify what commands are actually supported on each platform.

### iOS Voice Command Support Verification

**Issue**: The specification documents "jump to reference" and "jump to previous" as alternative commands, but iOS implementation may only support "play" and "go to" variations.

**Verification Required**:
1. Check iOS voice command handler implementation (likely in Shortcuts, Siri integration, or VoiceCommandHandler)
2. Determine which command variations are actually recognized:
   - ✓ "play reference"
   - ✓ "play previous"
   - ? "jump to reference"
   - ? "jump to previous"
   - ? "go to reference"
   - ? "go to previous"

**Action Items** (to be included in tasks.md):
- **Task: Verify iOS voice command support**
  - Review iOS voice command handler code
  - Test actual Siri command recognition
  - Document which variations work

- **Task: Implement "jump" variations on iOS (if needed)**
  - If "jump to reference" and "jump to previous" are not currently supported
  - Add these variations to iOS voice command parser/handler
  - Test with Siri to verify recognition
  - Ensure parity with Android MediaSession commands

- **Task: Update command documentation based on verification**
  - Only document commands that actually work on both platforms
  - If platforms differ, document platform-specific differences in Settings
  - Ensure cross-platform consistency where possible

**Decision**: Do not display "jump" variations in Settings help until verified to work on iOS. If unverified, either:
1. Implement "jump" support on iOS first, OR
2. Document only "play" variations initially, OR
3. Document platform differences explicitly ("iOS: play, Android: play/jump")

## Testing Strategy

Manual testing checklist:
- [ ] iOS: Settings screen displays Voice Commands section
- [ ] iOS: Both commands appear with correct phrases and descriptions
- [ ] iOS: Text is readable without truncation on iPhone SE
- [ ] iOS: Scrolling works if content exceeds screen
- [ ] Android: Settings screen displays Voice Commands section
- [ ] Android: Both commands appear with correct phrases and descriptions
- [ ] Android: Text is readable without truncation on small Android phones
- [ ] Android: Scrolling works if content exceeds screen
- [ ] Both: Visual inspection confirms section placement is logical
- [ ] Both: Landscape orientation displays correctly

## Future Extensibility

This design easily accommodates:
- Additional voice commands as new features are added
- Optional examples or usage notes per command
- Links to help documentation (out of scope for this feature)
- Dynamic command lists pulled from a central registry (future enhancement)

Current implementation keeps it simple with hardcoded list for 2 commands.
