# Data Model: Voice Commands Help List

**Feature**: `006-voice-commands-list`
**Purpose**: Define the structure for representing voice commands in the Settings help UI

## Entities

### VoiceCommand

Represents a single voice command with alternative phrases and description.

**Properties**:
- `phrases`: Array of strings - Alternative ways to invoke the command (e.g., ["play reference", "jump to reference"])
- `description`: String - Human-readable explanation of what the command does
- `example`: String (optional) - Usage example or additional context (reserved for future use)

**Relationships**: None - This is a display-only entity with no persistence or relations

**Validation Rules**:
- `phrases` array must contain at least one non-empty string
- `description` must be non-empty
- Each phrase should be lowercase for consistency
- Description should be a complete sentence explaining the action

**Lifecycle**: Static/hardcoded - No CRUD operations. Commands are defined in UI code and displayed read-only.

## Implementation Notes

### iOS (Swift)
```swift
struct VoiceCommand {
    let phrases: [String]
    let description: String
    let example: String? = nil
}

let voiceCommands: [VoiceCommand] = [
    VoiceCommand(
        phrases: ["play reference", "jump to reference"],
        description: "Navigate to the episode referenced in the current transcript segment"
    ),
    VoiceCommand(
        phrases: ["play previous", "jump to previous"],
        description: "Return to the previous episode in your listening history"
    )
]
```

### Android (Kotlin)
```kotlin
data class VoiceCommand(
    val phrases: List<String>,
    val description: String,
    val example: String? = null
)

val voiceCommands = listOf(
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

## Storage

**Persistence**: None - Commands are defined in code, not stored in database or fetched from API.

**Future Considerations**:
- If command list grows significantly (>10 commands), consider moving to separate resource files (JSON, XML, plist)
- If commands become configurable or plugin-based, would need persistent storage
- If localization is added, commands would be in string resource files per language

## Sample Data

Current voice commands (from feature 004):

**NOTE**: "jump" variations require verification on iOS before documenting. See plan.md "Prerequisites & Voice Command Verification" section.

1. **Play Reference**
   - Phrases: "play reference" (verified), "jump to reference" (iOS support needs verification)
   - Description: "Navigate to the episode referenced in the current transcript segment"

2. **Play Previous**
   - Phrases: "play previous" (verified), "jump to previous" (iOS support needs verification)
   - Description: "Return to the previous episode in your listening history"

**Implementation Note**: Only document command variations that work on BOTH platforms. If iOS doesn't support "jump", either implement it first or document only "play" variations.

## Constraints

- Commands are read-only - users cannot add, edit, or remove commands
- Command phrases must match exactly what Google Assistant/Siri recognize (per feature 004 implementation)
- Descriptions should be concise (1-2 sentences max) to fit on screen without excessive scrolling
- No internationalization in initial version - English only
