# Quickstart: Adding Voice Commands Section to Settings

**Feature**: `006-voice-commands-list`
**Target**: iOS and Android Settings screens
**Estimated Time**: 30 minutes per platform

## Prerequisites

- Existing Settings screen functional on both platforms
- Feature 004 (voice-reference-navigation) merged and working
- Familiarity with SwiftUI (iOS) and Jetpack Compose (Android)

**CRITICAL**: Before implementing, verify iOS voice command support for "jump" variations:
- Check iOS voice command handler code
- Test with Siri: "Hey Siri, jump to reference" and "Hey Siri, jump to previous"
- If not supported, either implement "jump" support first OR document only "play" variations
- See plan.md "Prerequisites & Voice Command Verification" for details

## iOS Implementation (SettingsView.swift)

### Step 1: Locate the Settings View

File: `ios/StrollcastApp/Views/SettingsView.swift`

Find the existing `Form` or `List` structure containing Zotero, Storage, and About sections.

### Step 2: Define Voice Commands Data

Add this at the top of the `SettingsView` struct or as a separate file:

```swift
struct VoiceCommand {
    let phrases: [String]
    let description: String
}

// NOTE: Verify "jump" variations work on iOS before documenting them
// If "jump" not supported, use only ["play reference"] and ["play previous"]
private let voiceCommands: [VoiceCommand] = [
    VoiceCommand(
        phrases: ["play reference", "jump to reference"],  // TODO: Verify "jump" works
        description: "Navigate to the episode referenced in the current transcript segment"
    ),
    VoiceCommand(
        phrases: ["play previous", "jump to previous"],    // TODO: Verify "jump" works
        description: "Return to the previous episode in your listening history"
    )
]
```

### Step 3: Add Voice Commands Section

Insert new section after existing sections (e.g., after "About"):

```swift
Section(header: Text("Voice Commands")) {
    ForEach(voiceCommands, id: \.phrases.first) { command in
        VStack(alignment: .leading, spacing: 4) {
            Text(command.phrases.joined(separator: " or "))
                .font(.headline)
            Text(command.description)
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
        .padding(.vertical, 4)
    }
}
```

### Step 4: Test

- Build and run on iOS Simulator
- Navigate to Settings tab
- Scroll to Voice Commands section
- Verify both commands display with phrases and descriptions
- Test on iPhone SE to ensure no truncation

## Android Implementation (SettingsScreen.kt)

### Step 1: Locate the Settings Screen

File: `android/app/src/main/java/com/strollcast/app/ui/screens/SettingsScreen.kt`

Find the `Column` or `LazyColumn` containing settings items.

### Step 2: Define Voice Commands Data

Add this at the top of the file or in a separate data class file:

```kotlin
data class VoiceCommand(
    val phrases: List<String>,
    val description: String
)

private val voiceCommands = listOf(
    VoiceCommand(
        phrases = listOf("play reference", "jump to reference"),
        description = "Navigate to the episode referenced in the current transcript segment"
    ),
    VoiceCommand(
        phrases = listOf("play previous", "jump to previous"),
        description = "Return to the previous episode in your listening history"
    )
)
```

### Step 3: Add Voice Commands Section

Insert new section after existing sections in the `LazyColumn`:

```kotlin
item {
    Text(
        text = "Voice Commands",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

items(voiceCommands) { command ->
    ListItem(
        headlineContent = {
            Text(command.phrases.joinToString(" or "))
        },
        supportingContent = {
            Text(command.description)
        },
        modifier = Modifier.padding(vertical = 4.dp)
    )
}
```

### Step 4: Test

- Build and run on Android Emulator
- Navigate to Settings screen
- Scroll to Voice Commands section
- Verify both commands display with phrases and descriptions
- Test on small screen device to ensure no horizontal scrolling

## Visual Verification Checklist

For both platforms:

- [ ] Section header "Voice Commands" appears
- [ ] Two commands are listed
- [ ] Command phrases appear prominent/bold
- [ ] Descriptions appear secondary/lighter
- [ ] Alternative phrases joined with "or"
- [ ] No horizontal scrolling required
- [ ] Section fits logically with other Settings content
- [ ] Text is readable on smallest supported device

## Common Issues & Solutions

### iOS

**Issue**: Section doesn't appear
- **Solution**: Ensure Section is inside Form or List, not standalone

**Issue**: ForEach warning about identifiers
- **Solution**: Use `id: \.phrases.first` or assign unique IDs to commands

### Android

**Issue**: ListItem not found
- **Solution**: Import `androidx.compose.material3.ListItem`

**Issue**: Spacing looks off
- **Solution**: Adjust `padding` modifiers on Text and ListItem

## Future Enhancements

When adding new voice commands:

1. Add new `VoiceCommand` to the list array
2. Ensure phrases match exactly what voice assistant recognizes
3. Keep descriptions concise (1-2 sentences)
4. Test on both platforms to verify display

When localizing:

1. Move command data to string resources
2. iOS: Use `NSLocalizedString()` or `.strings` files
3. Android: Use string resources in `res/values/strings.xml`
4. Maintain separate translations per language

## Rollback

If issues arise:

1. Remove the Voice Commands section code
2. Revert changes to SettingsView.swift / SettingsScreen.kt
3. Rebuild and verify Settings screen still works

No database migrations or API changes to rollback.
