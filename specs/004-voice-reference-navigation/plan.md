# Implementation Plan: Voice Navigation for Reference Playback

**Branch**: `004-voice-reference-navigation` | **Date**: 2026-01-11 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/004-voice-reference-navigation/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Add hands-free voice commands to Android app enabling users to navigate to referenced episodes and return to previous episodes using voice control. Commands include "play reference", "jump to reference", "play previous", and "jump to previous" with audio feedback for command success/failure. This brings Android to full parity with iOS voice navigation capabilities.

## Technical Context

**Language/Version**: Kotlin 1.9+
**Primary Dependencies**: Jetpack Compose, Material3, Media3, MediaSession, Hilt, Room, TextToSpeech API
**Voice Framework**: MediaSession + Google Assistant integration (no additional dependencies)
**Storage**: Room database for transcript data, SharedPreferences for settings
**Testing**: JUnit, Compose UI testing (optional)
**Target Platform**: Android 8.0+ (SDK 26+), target SDK 36
**Project Type**: Mobile (Android app)
**Performance Goals**: <1s voice recognition latency, <3s command-to-playback execution, 60 fps UI
**Constraints**: <1% battery drain per hour (MediaSession approach), offline episode playback, hands-free operation
**Scale/Scope**: Android app with 3 new files (VoiceCommandParser, AudioFeedbackManager, optional VoiceCommandButton), 3 modified files (PlaybackService, PlayerViewModel, TranscriptViewModel)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Verify compliance with principles from `.specify/memory/constitution.md`:

- [x] **Cross-Platform Parity**: Android-only feature that brings Android to parity with iOS voice navigation (iOS already has this via App Intents and Siri). Justified because iOS implementation already exists and this specifically addresses the Android gap.
- [x] **API Contract Stability**: No API changes required. Uses existing episode metadata endpoint and reference navigation functionality (feature 003).
- [x] **Offline-First Architecture**: Voice commands work offline if device supports on-device speech recognition. Referenced episode navigation uses same offline logic as feature 003 (only downloaded episodes). Audio feedback works offline via platform TTS.
- [x] **Platform-Native UI**: Uses Android platform voice recognition APIs, Material3 for any UI components (microphone button), follows Android permission patterns for RECORD_AUDIO.
- [x] **Service-Oriented Architecture**: Voice command handling in VoiceCommandService, transcript context extraction in existing TranscriptViewModel, navigation logic in existing PlayerViewModel. Separation of concerns maintained.
- [x] **Feature Flags & Graceful Degradation**: Requires RECORD_AUDIO permission - feature disabled if denied with clear UI indication. Voice recognition failure provides audio feedback. Works both foreground and background.

**Violations requiring justification**: None - all principles satisfied.

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
android/app/src/main/java/com/strollcast/app/
├── services/
│   └── VoiceCommandService.kt         # NEW: Voice recognition and command handling
├── viewmodels/
│   └── PlayerViewModel.kt             # MODIFIED: Add voice command integration
├── ui/
│   ├── components/
│   │   └── VoiceCommandButton.kt      # NEW: Optional UI trigger for voice input
│   └── screens/
│       └── PlayerScreen.kt            # MODIFIED: Add voice command UI integration
├── utils/
│   ├── VoiceCommandParser.kt          # NEW: Parse voice commands
│   └── AudioFeedbackManager.kt        # NEW: TTS audio feedback
└── AndroidManifest.xml                # MODIFIED: Add RECORD_AUDIO permission

android/app/src/main/res/
└── values/
    └── strings.xml                     # MODIFIED: Voice command strings and feedback messages
```

**Structure Decision**: Android-only implementation. iOS already has equivalent functionality via App Intents and Siri integration. This feature specifically addresses the Android platform gap identified in the constitution's Cross-Platform Parity principle.

## Complexity Tracking

No constitution violations - section not applicable.
