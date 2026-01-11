<!--
Sync Impact Report:
Version: 0.0.0 → 1.0.0 (Initial constitution establishment)
Added Principles:
  - I. Cross-Platform Parity
  - II. API Contract Stability
  - III. Offline-First Architecture
  - IV. Platform-Native UI
  - V. Service-Oriented Architecture
  - VI. Feature Flags & Graceful Degradation
Added Sections:
  - Mobile Development Standards
  - Quality & Testing Requirements
  - Governance
Templates requiring updates:
  ✅ plan-template.md - Constitution Check section will reference these principles
  ✅ spec-template.md - Requirements align with cross-platform and offline-first needs
  ✅ tasks-template.md - Task categorization includes platform-specific and shared tasks
Follow-up TODOs:
  - RATIFICATION_DATE set to 2026-01-11 (today) as initial establishment
-->

# Strollcast Mobile Apps Constitution

## Core Principles

### I. Cross-Platform Parity

Both iOS and Android apps MUST provide equivalent functionality to users, though implementation details may differ per platform. When adding features:

- New features MUST be tracked for implementation on both platforms
- Platform-specific features (iOS-only, Android-only) require explicit justification and user communication
- API contracts MUST support both platforms equally without favoring one implementation
- Feature gaps between platforms MUST be documented in each app's README with target parity timeline

**Rationale**: Users should have a consistent experience regardless of their device choice. Currently Android lags behind iOS in features (missing voice commands, detailed transcripts, separate history service), which violates user expectations.

### II. API Contract Stability

The shared API endpoint (`https://api.strollcast.com/episodes`) serves as the contract between backend and mobile clients. All API changes MUST follow these rules:

- Breaking changes require new API version with migration path
- New fields MUST be optional with sensible defaults
- Deprecated fields MUST remain functional for at least 2 app release cycles
- API changes MUST be documented before any client implementation begins
- Response schemas MUST be validated on both platforms before deployment

**Rationale**: With two mobile apps consuming the same API, breaking changes have multiplied blast radius. The API is in the sibling `director/` folder and changes must be coordinated.

### III. Offline-First Architecture

Mobile apps operate in unreliable network conditions. All features MUST be designed for offline capability:

- Core functionality (playback, navigation, viewing saved content) MUST work offline
- Network requests MUST have local caching layer
- User actions (notes, playback position, downloads) MUST persist locally first
- Sync conflicts MUST be resolved with last-write-wins or user choice
- UI MUST indicate network state and cached vs fresh data

**Rationale**: Podcast listening often happens during commutes, flights, and areas with poor connectivity. An online-only app provides poor user experience.

### IV. Platform-Native UI

Each platform MUST use native UI frameworks and follow platform conventions:

- **iOS**: SwiftUI with iOS Human Interface Guidelines, system fonts, native controls
- **Android**: Jetpack Compose with Material Design 3, dynamic colors, platform patterns
- No cross-platform UI frameworks (React Native, Flutter) to avoid "uncanny valley" UX
- Platform-specific interactions (swipe gestures, navigation patterns) MUST match user expectations
- Accessibility features MUST use platform APIs (VoiceOver, TalkBack)

**Rationale**: Users are trained by their platform and expect consistent patterns. Cross-platform UI frameworks often lag in supporting new OS features and feel non-native.

### V. Service-Oriented Architecture

Business logic MUST be encapsulated in service classes separate from UI code:

- **iOS Services pattern**: `PodcastService`, `AudioPlayer`, `DownloadManager`, `ZoteroService`, etc.
- **Android pattern**: Repository layer + ViewModels, background services for media
- Services MUST have clear, single responsibilities
- Services MUST be independently testable without UI
- UI layer MUST only orchestrate services, not contain business logic

**Rationale**: Keeps UI thin and testable. Currently iOS has 7 services while Android has 1 (PlaybackService), showing Android needs better separation of concerns to match iOS architecture.

### VI. Feature Flags & Graceful Degradation

Optional features (Zotero integration, voice commands) MUST degrade gracefully when unavailable:

- Features requiring external services MUST have feature flags or capability checks
- Missing capabilities MUST be hidden or disabled in UI, not crash
- Error states MUST provide actionable user guidance
- Settings MUST validate configurations (API keys, permissions) with clear feedback

**Rationale**: Not all users will configure Zotero, grant microphone permissions, or have network access. The app must remain functional without optional features.

## Mobile Development Standards

### Platform Requirements

**iOS**:
- Minimum iOS 15+ (determined by SwiftUI features used)
- Xcode 15+ for builds
- Swift 5.9+
- Dependencies via Swift Package Manager (preferred) or CocoaPods

**Android**:
- Minimum SDK 26 (Android 8.0)
- Target SDK 36 (latest)
- Kotlin 1.9+
- Jetpack Compose with Material3
- Dependencies via Gradle with version catalogs

### Dependency Management

- Keep third-party dependencies minimal
- Prefer platform-provided frameworks over third-party (AVPlayer over custom, Media3 over alternatives)
- Document dependency rationale in app README
- Security updates MUST be applied within 1 week of disclosure
- Major version updates require testing on both platforms

### Data Privacy & Security

- User data (Zotero keys, playback history, notes) MUST be stored securely
  - iOS: Keychain for secrets, encrypted UserDefaults or CoreData for sensitive data
  - Android: EncryptedSharedPreferences, Room with encryption
- API keys MUST NOT be committed to source code
- Crash reports MUST NOT include PII or authentication tokens
- Analytics MUST be opt-in or anonymous only

## Quality & Testing Requirements

### Testing Strategy

Testing is recommended but not mandatory unless specified in feature requirements:

- **Unit tests**: Service/business logic tests encouraged
- **Integration tests**: API contract tests recommended when API changes
- **UI tests**: Optional, focus on critical user flows if implemented
- **Manual testing**: Required on both platforms before release

### Release Process

- GitHub Actions builds unsigned IPA for iOS on version tags (`v*`)
- Android builds require local signing with keystore
- Version numbers MUST follow semantic versioning: `MAJOR.MINOR.PATCH`
- Release notes MUST document features, fixes, and known platform gaps
- Both platforms should target synchronized version numbers when at parity

### Performance Standards

- App launch: < 2 seconds to usable state
- Audio playback: < 1 second from tap to audio start (cached)
- Streaming: < 3 seconds buffering on 4G connection
- Download progress: Real-time feedback, pauseable/resumable
- UI: 60 fps scrolling, no jank during playback

## Governance

### Constitution Authority

This constitution defines the architectural principles and quality standards for the Strollcast mobile apps (iOS and Android). All feature specifications, implementation plans, and code reviews MUST verify compliance with these principles.

### Amendment Process

1. Proposed changes MUST be documented with rationale
2. Impact on existing features MUST be assessed
3. Version bump follows semantic versioning:
   - **MAJOR**: Principle removal or incompatible change
   - **MINOR**: New principle or section added
   - **PATCH**: Clarifications, wording improvements
4. All dependent templates (plan, spec, tasks) MUST be updated
5. Amendment date and version MUST be recorded

### Compliance & Reviews

- Feature specs MUST include "Constitution Check" section referencing relevant principles
- Implementation plans MUST document principle violations with justification
- Code reviews MUST verify architecture matches service-oriented principles
- Cross-platform features MUST have tracking issues for both iOS and Android
- Complexity (violations) MUST be justified: simpler alternatives rejected because [reason]

### Runtime Guidance

For day-to-day development guidance, refer to:
- `/Users/mseritan/dev/strollcast/CLAUDE.md` - Repository-wide context and commands
- `StrollcastApp/README.md` - iOS app structure and setup
- `StrollcastApp/android/README.md` - Android app structure and setup
- `.specify/templates/` - Feature planning templates

**Version**: 1.0.0 | **Ratified**: 2026-01-11 | **Last Amended**: 2026-01-11
