# Feature Specification: Separate iOS Documentation

**Feature Branch**: `001-ios-readme`
**Created**: 2026-01-11
**Status**: Draft
**Input**: User description: "Move the ios specific text out of ../README.md and in a ios/README.md file"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - iOS Developer Setup (Priority: P1)

As an iOS developer who wants to build the Strollcast iOS app, I need platform-specific installation and build instructions in the ios/ directory so I can quickly find relevant setup steps without sifting through cross-platform or Android documentation.

**Why this priority**: This is the primary use case. iOS developers currently need to read through the main README which mixes general features with iOS-specific installation steps. Separating this improves developer onboarding experience.

**Independent Test**: Can be fully tested by following the ios/README.md to build the iOS app from scratch and verifying all instructions are complete and accurate.

**Acceptance Scenarios**:

1. **Given** a developer clones the repository, **When** they navigate to ios/ directory, **Then** they find a README.md with complete iOS build instructions
2. **Given** a developer reads the main README.md, **When** they look for iOS installation, **Then** they find a clear reference/link to ios/README.md instead of embedded instructions
3. **Given** an iOS developer reads ios/README.md, **When** they follow the build steps, **Then** they can successfully build and run the app without referring to other documentation

---

### User Story 2 - Project Overview for All Users (Priority: P2)

As any user (iOS, Android, or general) browsing the repository, I need to understand what Strollcast is and what features it offers without being overwhelmed by platform-specific installation details.

**Why this priority**: The main README should serve as a landing page for all audiences. Platform-specific details should be in subdirectories.

**Independent Test**: Can be tested by reviewing the main README and verifying it provides clear project overview, features, and pointers to platform-specific docs without iOS-only installation steps.

**Acceptance Scenarios**:

1. **Given** a user visits the repository, **When** they read README.md, **Then** they see project description, features, and links to both ios/ and android/ documentation
2. **Given** a user wants Android installation, **When** they read the main README, **Then** they are not distracted by iOS-specific build instructions
3. **Given** a user wants to understand Zotero integration, **When** they read the main README, **Then** they see general integration benefits with platform-specific setup referenced in subdirectories

---

### Edge Cases

- What happens when the main README mentions "iOS app" in the description? It should remain since it describes what exists, but installation steps should move.
- What happens to the app icon image reference? It should remain in main README as visual identification.
- What happens to project structure section? It's iOS-specific so should move to ios/README.md.
- What happens to IPA building instructions? They're iOS-specific and should move.
- What happens to Zotero integration section? The setup steps mention "In the ios App" so this needs to either stay general or have platform-specific variants.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST create a new file at `ios/README.md` containing iOS-specific documentation
- **FR-002**: System MUST move the "iOS Installation" section from main README to ios/README.md
- **FR-003**: System MUST move the "Project Structure" section (currently iOS-specific) to ios/README.md
- **FR-004**: System MUST move the "Building the IPA Locally" section to ios/README.md
- **FR-005**: Main README MUST retain project overview, feature list, and general description
- **FR-006**: Main README MUST retain the app icon image for visual identification
- **FR-007**: Main README MUST add a clear reference/link to ios/README.md in an "Installation" section
- **FR-008**: ios/README.md MUST include a brief project introduction or reference to main README
- **FR-009**: Zotero integration section MUST either remain platform-agnostic in main README or be split with iOS-specific steps moved to ios/README.md
- **FR-010**: Main README MUST mention both iOS and Android app availability with links to respective documentation
- **FR-011**: License and Acknowledgments sections MUST remain in main README as they apply to all platforms

### Mobile-Specific Requirements

- **Platform Scope**: iOS only
  - **Justification**: This is a documentation restructuring task specific to iOS docs. Android documentation reorganization would be a separate task.
- **Offline Behavior**: N/A (documentation task)
- **Permissions Required**: N/A (documentation task)
- **Background Capabilities**: N/A (documentation task)
- **Platform Integration**: N/A (documentation task)

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: iOS developers can find all necessary build instructions in ios/README.md without referring to main README
- **SC-002**: Main README is under 100 lines and focuses on project overview, not platform-specific installation
- **SC-003**: New contributors can identify which documentation to read (ios/ vs android/) within 30 seconds of viewing the main README
- **SC-004**: Zero broken links between main README and ios/README.md
- **SC-005**: All iOS-specific sections (installation, project structure, IPA building) are removed from main README and present in ios/README.md

## Assumptions

- The ios/ directory already exists in the repository
- Android documentation (android/README.md) already exists or will be handled separately
- The main README should serve as a universal landing page for all platforms
- Zotero integration setup is similar enough across platforms to keep general in main README, or platform-specific steps are obvious from context
- File paths and links use relative paths that work from both root and ios/ directory contexts
