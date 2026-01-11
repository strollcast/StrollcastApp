# Implementation Plan: [FEATURE]

**Branch**: `[###-feature-name]` | **Date**: [DATE] | **Spec**: [link]
**Input**: Feature specification from `/specs/[###-feature-name]/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

[Extract from feature spec: primary requirement + technical approach from research]

## Technical Context

<!--
  ACTION REQUIRED: Replace the content in this section with the technical details
  for the project. The structure here is presented in advisory capacity to guide
  the iteration process.
-->

**Language/Version**: [e.g., Python 3.11, Swift 5.9, Rust 1.75 or NEEDS CLARIFICATION]  
**Primary Dependencies**: [e.g., FastAPI, UIKit, LLVM or NEEDS CLARIFICATION]  
**Storage**: [if applicable, e.g., PostgreSQL, CoreData, files or N/A]  
**Testing**: [e.g., pytest, XCTest, cargo test or NEEDS CLARIFICATION]  
**Target Platform**: [e.g., Linux server, iOS 15+, WASM or NEEDS CLARIFICATION]
**Project Type**: [single/web/mobile - determines source structure]  
**Performance Goals**: [domain-specific, e.g., 1000 req/s, 10k lines/sec, 60 fps or NEEDS CLARIFICATION]  
**Constraints**: [domain-specific, e.g., <200ms p95, <100MB memory, offline-capable or NEEDS CLARIFICATION]  
**Scale/Scope**: [domain-specific, e.g., 10k users, 1M LOC, 50 screens or NEEDS CLARIFICATION]

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Verify compliance with principles from `.specify/memory/constitution.md`:

- [ ] **Cross-Platform Parity**: Does this feature require implementation on both iOS and Android? If iOS-only or Android-only, justify why.
- [ ] **API Contract Stability**: Does this feature require API changes? If yes, are they backward compatible? Document migration path.
- [ ] **Offline-First Architecture**: Does this feature work offline? What data is cached? How are sync conflicts handled?
- [ ] **Platform-Native UI**: Does UI follow platform conventions (SwiftUI/iOS HIG for iOS, Compose/Material3 for Android)?
- [ ] **Service-Oriented Architecture**: Is business logic in services (iOS) or repository/ViewModels (Android), separate from UI?
- [ ] **Feature Flags & Graceful Degradation**: If optional dependencies (Zotero, permissions), does feature degrade gracefully?

**Violations requiring justification**: [List any principle violations and why simpler alternatives were rejected]

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
<!--
  ACTION REQUIRED: Replace the placeholder tree below with the concrete layout
  for this feature. Delete unused options and expand the chosen structure with
  real paths. The delivered plan must not include Option labels.
-->

```text
# [REMOVE IF UNUSED] Option 1: iOS App Only
ios/StrollcastApp/
├── Models/              # Data models
├── Services/            # Business logic (PodcastService, AudioPlayer, etc.)
├── Views/               # SwiftUI views
├── Assets.xcassets/     # Images, colors
└── Info.plist

# [REMOVE IF UNUSED] Option 2: Android App Only
android/app/src/main/java/com/strollcast/app/
├── data/                # Database (Room) layer
├── di/                  # Dependency injection (Hilt)
├── models/              # Data models
├── network/             # API client (Retrofit)
├── repository/          # Data repository pattern
├── services/            # Background services (PlaybackService)
├── ui/                  # Jetpack Compose UI
├── viewmodels/          # ViewModels (MVVM)
└── MainActivity.kt

# [REMOVE IF UNUSED] Option 3: Both Platforms (Cross-Platform Feature)
ios/StrollcastApp/
├── [iOS structure as above]
└── [New feature components]

android/app/src/main/java/com/strollcast/app/
├── [Android structure as above]
└── [Equivalent feature components]

# [REMOVE IF UNUSED] Option 4: API Change (Backend in sibling director/ folder)
../../director/
├── modal/src/           # Modal serverless functions
│   └── [API endpoints, generators]
└── public/              # Static content and episode data
```

**Structure Decision**: [Document the selected structure. For cross-platform features (Option 3), specify which platform is being implemented first and create tracking issue for the other platform.]

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |
