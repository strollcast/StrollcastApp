# Feature Specification: Navigation Label Updates

**Feature Branch**: `005-navigation-labels`
**Created**: 2026-01-11
**Status**: Draft
**Input**: User description: "The labels in the menu at the bottom don't fit. Change both the ios and android applications to use 'Strolls' instead of 'Podcasts'. Remove the 'Settings' label, unless there is a shorter alternative for it."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Rename "Podcasts" to "Strolls" (Priority: P1)

Users see "Strolls" as the label for the podcast list tab in the bottom navigation, creating consistent branding with the app name "Strollcast" while using less horizontal space.

**Why this priority**: This is the core branding change requested and directly addresses the space constraint issue. The term "Strolls" is shorter than "Podcasts" (7 vs 8 characters) and better aligns with the app's unique identity.

**Independent Test**: Can be fully tested by launching the app on both iOS and Android and verifying that the first tab label reads "Strolls" instead of "Podcasts". All functionality remains unchanged, only the visible label changes.

**Acceptance Scenarios**:

1. **Given** the iOS app is launched, **When** user views the bottom tab bar, **Then** the first tab displays "Strolls" with the headphones icon
2. **Given** the Android app is launched, **When** user views the bottom navigation bar, **Then** the first tab displays "Strolls" with the headphones icon
3. **Given** the user taps the "Strolls" tab on either platform, **When** the tab is selected, **Then** the podcast/stroll list screen is displayed
4. **Given** the app is displaying the podcast list, **When** user views the bottom navigation, **Then** the "Strolls" tab appears selected/highlighted

---

### User Story 2 - Remove or Shorten "Settings" Label (Priority: P2)

Users see either an icon-only Settings tab or a shortened label like "Gear" that takes less space in the bottom navigation.

**Why this priority**: This addresses the space constraint but is lower priority because Settings is typically accessed less frequently than content tabs, and industry convention supports icon-only settings tabs.

**Independent Test**: Can be fully tested by launching the app and verifying that the Settings tab either has no label (icon only) or has a shortened label, and that tapping it navigates to the Settings screen.

**Acceptance Scenarios**:

1. **Given** the iOS app is launched, **When** user views the bottom tab bar, **Then** the Settings tab displays only the gear icon without a text label
2. **Given** the Android app is launched, **When** user views the bottom navigation bar, **Then** the Settings tab displays only the gear icon without a text label
3. **Given** the user taps the Settings icon on either platform, **When** the tab is selected, **Then** the Settings screen is displayed
4. **Given** the app is displaying Settings, **When** user views the bottom navigation, **Then** the Settings icon appears selected/highlighted

---

### Edge Cases

- What happens when the device is set to a language with longer translations for "Strolls"? (Assumption: English-only app currently, future localization will need to validate label lengths)
- How does the icon-only Settings tab affect accessibility with screen readers? (System should still announce "Settings" even without visible label)
- What happens on very small screen devices (e.g., iPhone SE)? (Labels should still fit comfortably with reduced character count)
- How does the tab bar appear in landscape orientation? (Labels should remain readable and appropriately spaced)

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: iOS app MUST display "Strolls" as the label for the podcast list tab instead of "Podcasts"
- **FR-002**: Android app MUST display "Strolls" as the label for the podcast list tab instead of "Podcasts"
- **FR-003**: iOS app MUST display the Settings tab with icon only (no text label)
- **FR-004**: Android app MUST display the Settings tab with icon only (no text label)
- **FR-005**: Tapping the "Strolls" tab MUST navigate to the podcast list screen on both platforms
- **FR-006**: Tapping the Settings icon MUST navigate to the Settings screen on both platforms
- **FR-007**: Screen readers MUST announce "Strolls" for the podcast list tab
- **FR-008**: Screen readers MUST announce "Settings" for the settings tab even without visible label
- **FR-009**: Tab selection state MUST be visually indicated for the active tab
- **FR-010**: All other tabs (Played, Notes) MUST remain unchanged

### Key Entities

- **Navigation Tab**: Represents a single tab in the bottom navigation bar with properties including label text, icon, destination screen, and accessibility label

### Mobile-Specific Requirements

- **Platform Scope**: Both iOS and Android
  - Implementation order: Both platforms simultaneously to maintain feature parity
  - Both apps must show identical labels and icon-only settings
- **Offline Behavior**: No network dependency - navigation labels are static UI elements
- **Permissions Required**: None
- **Background Capabilities**: N/A - navigation is only visible when app is in foreground
- **Platform Integration**:
  - iOS: Uses SwiftUI TabView with Label components
  - Android: Uses bottom navigation bar with string resources
  - Both must maintain platform-specific styling (iOS tab bar style vs Android Material Design bottom nav)

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Bottom navigation labels fit comfortably within the available space on all supported device sizes without truncation
- **SC-002**: 100% of tab navigation functionality works identically before and after label changes
- **SC-003**: Screen reader users can successfully identify and navigate to all tabs using accessibility labels
- **SC-004**: Visual inspection confirms "Strolls" appears in place of "Podcasts" on both iOS and Android
- **SC-005**: Visual inspection confirms Settings tab shows icon only on both iOS and Android

## Out of Scope

- Renaming internal code references (class names, variables, file names) from "Podcast" to "Stroll"
- Changing terminology elsewhere in the app (screen titles, API references, data models)
- Updating app store listings or marketing materials
- Localization/internationalization of the new "Strolls" label
- Redesigning tab icons or navigation bar styling
- Adding new tabs or reordering existing tabs

## Assumptions

- The app is currently English-only; if localized, translations will need to be updated separately
- Users are familiar with icon-only Settings tabs (common mobile UI pattern)
- The term "Strolls" is approved branding and doesn't require further validation
- Screen reader accessibility labels can differ from visible labels
- Current tab icons (headphones for Strolls, gear for Settings) remain appropriate
