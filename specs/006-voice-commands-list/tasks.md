# Implementation Tasks: Voice Commands Help List in Settings

**Feature**: Voice Commands Help List in Settings
**Branch**: `006-voice-commands-list`
**Estimated Effort**: 2-3 hours
**Tech Stack**: Swift 5.9+ (iOS), Kotlin 1.9+ (Android), SwiftUI, Jetpack Compose + Material3

## Task Overview

This document breaks down the voice commands help list feature into implementable tasks organized by user story. The feature has a single user story (P1) with a prerequisite verification phase.

**Total Tasks**: 8
**Parallel Opportunities**: 2 tasks marked [P]
**MVP Scope**: All tasks (single P1 story)

## Phase 1: Prerequisites & Verification (4 tasks)

**Goal**: Verify iOS voice command support and ensure cross-platform parity before documenting commands

- [ ] T001 Review iOS voice command handler implementation to verify supported command variations
- [ ] T002 Test iOS Siri command recognition for "jump to reference" and "jump to previous" variations
- [ ] T003 Implement "jump" command variations on iOS if not currently supported (conditional)
- [ ] T004 Document final list of commands to display based on verification results

## Phase 2: User Story 1 - Display Voice Commands in Settings (4 tasks)

**Story Goal**: Users can view a list of available voice commands with descriptions in Settings on both platforms

**Why this priority**: Essential for discoverability. Voice interfaces lack visual affordances, so users need an easily accessible reference to learn what commands they can use.

**Independent Test**: Open Settings on both iOS and Android, verify "Voice Commands" section appears with complete list of commands and descriptions

**Acceptance Scenarios**:
1. User opens Settings on iOS → sees Voice Commands section with command list
2. User opens Settings on Android → sees Voice Commands section with command list
3. Voice commands list shows phrase(s) to speak and what action each performs
4. Text is readable without truncation on small devices

**Tasks**:

- [ ] T005 [P] [US1] Add Voice Commands section to iOS Settings in ios/StrollcastApp/Views/SettingsView.swift
- [ ] T006 [P] [US1] Add Voice Commands section to Android Settings in android/app/src/main/java/com/strollcast/app/ui/screens/SettingsScreen.kt
- [ ] T007 [US1] Test iOS voice commands display on iPhone SE and regular iPhone
- [ ] T008 [US1] Test Android voice commands display on small and regular Android devices

## Dependencies

### Story Completion Order

```
Phase 1 (Prerequisites) → Phase 2 (User Story 1)
```

**Critical Path**:
- Phase 1 must complete before Phase 2 (need verified command list)
- T001-T004 must complete sequentially (verification → testing → implementation → documentation)
- T005-T006 can run in parallel (different platforms, no shared files)
- T007-T008 must run after T005-T006 complete

**Parallel Execution Opportunities**:
- T005 and T006 can run simultaneously (iOS and Android are independent)

### File Dependencies

Tasks must respect these file modification sequences:

**SettingsView.swift (iOS)**:
1. T005 (add Voice Commands section)

**SettingsScreen.kt (Android)**:
1. T006 (add Voice Commands section)

**No file conflicts** - each platform modifies its own Settings file independently.

## Implementation Strategy

### MVP (Minimum Viable Product)

**Scope**: All 8 tasks (single user story)
**Timeline**: 2-3 hours
**Value**: Complete voice commands help list on both platforms

**Delivers**:
- ✅ Verified command list (works on both platforms)
- ✅ Voice Commands section in iOS Settings
- ✅ Voice Commands section in Android Settings
- ✅ Platform-native UI styling
- ✅ Tested on small and regular devices

### Implementation Order

**Phase 1: Verification (1-2 hours)**:
1. T001: Review iOS code for voice command handler
2. T002: Test with Siri which commands actually work
3. T003: Implement "jump" support if needed (conditional)
4. T004: Finalize command list for documentation

**Phase 2: Implementation (30-60 minutes)**:
1. T005 & T006: Add sections to both Settings screens (parallel)
2. T007 & T008: Test on devices

## Parallel Execution Example

### Phase 2: Settings Implementation (Run Simultaneously)

```bash
# Terminal 1 (iOS)
Implement T005 (Add section to SettingsView.swift)

# Terminal 2 (Android)
Implement T006 (Add section to SettingsScreen.kt)
```

Then sequential testing:
- T007 (iOS testing - requires T005 complete)
- T008 (Android testing - requires T006 complete)

## Testing Strategy

### Manual Testing Checklist

**User Story 1 - Voice Commands Display**:
- [ ] iOS Settings displays "Voice Commands" section header
- [ ] iOS displays 2 command entries with phrases and descriptions
- [ ] iOS command text is bold, descriptions are secondary style
- [ ] iOS text readable without truncation on iPhone SE
- [ ] iOS section scrolls if needed
- [ ] Android Settings displays "Voice Commands" section header
- [ ] Android displays 2 command entries with phrases and descriptions
- [ ] Android command text uses headline, descriptions use supporting text
- [ ] Android text readable without truncation on small phones
- [ ] Android section scrolls if needed
- [ ] Both platforms show identical command information
- [ ] Landscape orientation displays correctly on both platforms

### Command Verification Testing

- [ ] Test "play reference" on iOS with Siri
- [ ] Test "play previous" on iOS with Siri
- [ ] Test "jump to reference" on iOS with Siri (verify support)
- [ ] Test "jump to previous" on iOS with Siri (verify support)
- [ ] Test "play reference" on Android with Google Assistant
- [ ] Test "play previous" on Android with Google Assistant
- [ ] Test "jump to reference" on Android with Google Assistant
- [ ] Test "jump to previous" on Android with Google Assistant

## Success Criteria

Feature is complete when all acceptance scenarios pass:

**US1 - Display Voice Commands**:
- ✅ iOS Settings displays Voice Commands section with verified commands
- ✅ Android Settings displays Voice Commands section with verified commands
- ✅ Commands show phrases and descriptions clearly
- ✅ Text readable on small devices (iPhone SE, small Android)
- ✅ Both platforms show identical command information
- ✅ Only commands that work on BOTH platforms are documented

## Notes

- **No new files**: Only modifications to existing Settings screens
- **No persistence**: Commands are hardcoded in UI (static content)
- **No API calls**: Feature works 100% offline
- **Cross-platform parity**: Constitution Principle I requires both platforms show same commands
- **Conditional task**: T003 only needed if "jump" variations don't work on iOS
- **Simple scope**: 2 commands initially, can grow as more voice features are added

## Task Details

### T001: Review iOS Voice Command Handler

**Action**: Locate and review the iOS code that handles voice commands (Siri integration, Shortcuts, or custom handler)

**Files to check**:
- ios/StrollcastApp/ (search for Siri, voice command, shortcuts)
- Look for Intent handlers or voice command parsers
- Check what command variations are registered

**Output**: Document which command variations are currently supported

### T002: Test iOS Siri Commands

**Action**: Test actual Siri command recognition on iOS device or simulator

**Commands to test**:
- "Hey Siri, play reference" (expected to work)
- "Hey Siri, play previous" (expected to work)
- "Hey Siri, jump to reference" (needs verification)
- "Hey Siri, jump to previous" (needs verification)
- "Hey Siri, go to reference" (optional)
- "Hey Siri, go to previous" (optional)

**Output**: List of commands that successfully trigger voice navigation

### T003: Implement "jump" Variations (Conditional)

**Action**: If "jump" variations don't work, add support for them

**Files to modify**:
- iOS voice command handler/parser
- Siri Intent definitions or Shortcuts configuration

**Ensure**: "jump to reference" and "jump to previous" trigger same actions as "play" variations

**Test**: Verify with Siri that new variations work

### T004: Document Final Command List

**Action**: Based on T001-T003 results, finalize which commands to document in Settings

**Decision options**:
1. If "jump" works: Document both "play" and "jump" variations
2. If "jump" doesn't work and can't be implemented: Document only "play" variations
3. If platforms differ: Document platform-specific differences

**Output**: Final list of 2 commands with phrase variations for Settings display

### T005: Add iOS Voice Commands Section

**File**: ios/StrollcastApp/Views/SettingsView.swift

**Action**:
1. Define VoiceCommand struct with phrases and description properties
2. Create hardcoded list of 2 commands (from T004)
3. Add new Section with header "Voice Commands" after existing sections
4. Use ForEach to display commands with headline phrases and secondary descriptions
5. Format alternative phrases joined with "or"

**Styling**:
- Use `.font(.headline)` for command phrases
- Use `.font(.subheadline).foregroundColor(.secondary)` for descriptions
- Leverage SwiftUI Section with header

### T006: Add Android Voice Commands Section

**File**: android/app/src/main/java/com/strollcast/app/ui/screens/SettingsScreen.kt

**Action**:
1. Define VoiceCommand data class with phrases and description properties
2. Create hardcoded list of 2 commands (from T004)
3. Add new section with "Voice Commands" header to LazyColumn
4. Use items() block to display commands with ListItem
5. Format alternative phrases joined with "or"

**Styling**:
- Use Material3 ListItem with headlineContent for command phrases
- Use supportingContent for descriptions
- Follow Material3 list styling conventions

### T007: Test iOS Display

**Action**: Build and run iOS app, navigate to Settings

**Test on**:
- iPhone SE (smallest screen)
- Standard iPhone (e.g., iPhone 14)

**Verify**:
- Voice Commands section appears
- 2 commands display with phrases and descriptions
- Text is readable without truncation
- Section scrolls if needed
- Landscape orientation works

### T008: Test Android Display

**Action**: Build and run Android app, navigate to Settings

**Test on**:
- Small Android phone (or emulator with small screen)
- Standard Android phone

**Verify**:
- Voice Commands section appears
- 2 commands display with phrases and descriptions
- Text is readable without truncation or horizontal scrolling
- Section scrolls if needed
- Landscape orientation works
