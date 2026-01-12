# Research: Voice Navigation for Reference Playback

**Feature**: Voice commands for reference navigation in Android app
**Date**: 2026-01-11
**Status**: Research complete

## Research Question 1: Which Android voice recognition framework should we use?

### Decision: MediaSession + Google Assistant Integration

**Rationale**:
- The app already implements Media3 MediaSession in `PlaybackService.kt`
- MediaSession automatically integrates with Google Assistant for voice commands
- Works in both foreground and background without additional permissions
- Industry standard for media apps with minimal battery impact
- No continuous listening required - system handles wake word detection

**Alternatives Considered**:

1. **Android SpeechRecognizer API**
   - Rejected: Requires RECORD_AUDIO permission, continuous listening, significant battery drain
   - Complex background operation requiring foreground service
   - Not designed for media playback control

2. **App Actions with Built-In Intents**
   - Evaluated: Good for custom episode search queries
   - Can be used as secondary enhancement after MediaSession foundation
   - Primarily launches app activities, not ideal for background commands

3. **VOSK Offline Speech Recognition**
   - Rejected: Requires 50MB model download, CPU-intensive processing
   - Same background challenges as SpeechRecognizer
   - Only needed if offline operation is critical requirement (it's not per spec)

**Implementation Approach**:
- Extend existing `MediaSessionCallback` with `onPlayFromSearch()` method
- Parse voice query strings for commands: "reference", "previous", timestamp patterns
- Communicate from service to UI layer via broadcasts or shared repository
- Update MediaMetadata when episode changes to support "What's playing" queries

## Research Question 2: How do we handle foreground vs background voice commands?

### Decision: Leverage MediaSession's Native Background Support

**Rationale**:
- MediaSession works seamlessly in both foreground and background when active
- Google Assistant delivers commands through MediaSession callbacks regardless of app state
- No additional implementation needed - framework handles state management
- Already implemented via `MediaSessionService` in `PlaybackService.kt`

**Key Insights**:
- Service runs as foreground service during playback (already implemented)
- MediaSession remains active even when UI is backgrounded
- System manages wake word detection ("Hey Google") with minimal battery impact
- Commands route through same callback interface regardless of app visibility

**Background Service Pattern** (already implemented):
```kotlin
override fun onCreate() {
    super.onCreate()
    val notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Strollcast")
        .setSmallIcon(R.drawable.ic_notification)
        .build()

    startForeground(NOTIFICATION_ID, notification)
}
```

## Research Question 3: What are battery optimization strategies?

### Decision: Use MediaSession (System-Managed) + Best Practices

**Battery Impact**:
- **MediaSession approach**: Minimal (<1% per hour) - system manages wake word detection
- **Continuous listening**: High (5-10% per hour) - NOT RECOMMENDED
- **Our implementation**: MediaSession only active during playback

**Best Practices Applied**:
1. No always-on listening - let system handle wake words
2. Service stops when playback stops (already implemented in `onTaskRemoved()`)
3. Efficient audio attributes for speech content (already set: `AUDIO_CONTENT_TYPE_SPEECH`)
4. Respect system battery optimization settings
5. Only request RECORD_AUDIO if adding custom SpeechRecognizer (not needed for MediaSession)

**Modern Android Optimizations** (2026):
- Adaptive Battery AI learns patterns and reduces unnecessary wake-ups
- Low-power NPUs handle hotword detection without main CPU
- Doze mode exemptions for foreground media services
- Battery Saver mode automatically limits background activity

## Research Question 4: Can voice recognition work offline?

### Decision: Hybrid Online/Offline with MediaSession

**Capabilities**:
- **Google Assistant**: Requires online for complex queries
- **Basic commands offline** (Android 12+): Simple media commands work with `EXTRA_PREFER_OFFLINE` flag
- **On-device models**: Limited to basic voice actions, variable by device

**Our Approach**:
- Primary: Google Assistant (requires online) - acceptable per spec assumptions
- Optional: On-device fallback for basic commands if available
- Episode playback offline already supported (feature 003 dependency)

**Offline Scope**:
- Voice recognition: Online preferred, on-device when available
- Referenced episode playback: Offline for downloaded episodes (already implemented)
- Audio feedback (TTS): Works offline via platform TTS engine

**Alternative Not Chosen**:
- VOSK for full offline: Requires 50MB models, CPU-intensive, battery impact
- Only justified if offline voice is critical requirement (not per spec)

## Research Question 5: What permissions are required?

### Decision: No New Permissions for MediaSession Approach

**Current Permissions** (already in AndroidManifest.xml):
- `INTERNET` - For episode streaming
- `FOREGROUND_SERVICE` - For background playback
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK` - Specific media foreground service
- `WAKE_LOCK` - Keep CPU awake during playback

**NOT REQUIRED**:
- `RECORD_AUDIO` - NOT needed for MediaSession + Google Assistant integration
- Only required if implementing custom SpeechRecognizer (we're not)

**Permission Handling**:
- MediaSession voice commands work through Google Assistant
- User grants permissions to Google Assistant app, not Strollcast
- No runtime permission requests needed for this feature

**If Future Enhancement Adds SpeechRecognizer**:
- Would need `RECORD_AUDIO` runtime permission
- Request just-in-time when user enables custom voice input
- Provide clear explanation and handle denial gracefully

## Research Question 6: How do we integrate with existing PlayerViewModel and TranscriptViewModel?

### Decision: Service-to-ViewModel Communication Pattern

**Architecture**:
```
VoiceCommand (Google Assistant)
    ↓
MediaSessionCallback.onPlayFromSearch()
    ↓
VoiceCommandParser (parse query string)
    ↓
Broadcast Intent OR Shared Repository
    ↓
PlayerViewModel / TranscriptViewModel
    ↓
UI Update + Episode Navigation
```

**Communication Patterns**:

1. **Broadcast Receiver** (Recommended for decoupling):
```kotlin
// In PlaybackService
private fun handleVoiceQuery(query: String) {
    when {
        query.contains("reference") -> {
            sendBroadcast(Intent("com.strollcast.app.PLAY_REFERENCE"))
        }
        query.contains("previous") -> {
            sendBroadcast(Intent("com.strollcast.app.PLAY_PREVIOUS"))
        }
    }
}

// In PlayerViewModel
private val voiceCommandReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            "com.strollcast.app.PLAY_REFERENCE" -> navigateToNextReference()
            "com.strollcast.app.PLAY_PREVIOUS" -> playPreviousEpisode()
        }
    }
}
```

2. **Shared Repository** (Alternative):
- VoiceCommandRepository with StateFlow
- Service emits commands
- ViewModels collect and react

**Integration Points**:
- `PlayerViewModel.navigateToReferencedEpisode()` - Already exists from feature 003
- `TranscriptViewModel.getCurrentSegment()` - Needed to find current references
- `PlaybackHistoryManager` - Already handles "play previous" navigation

## Technology Stack Summary

**Core Technologies** (resolves "NEEDS CLARIFICATION"):
- **Voice Recognition**: MediaSession + Google Assistant (native Android framework)
- **Text-to-Speech**: Android platform TTS engine
- **Service Communication**: BroadcastReceiver pattern
- **Audio Feedback**: TextToSpeech API

**Updated Technical Context**:
- **Primary Dependencies**: Jetpack Compose, Material3, Media3, MediaSession, Hilt, Room, TextToSpeech API
- **Voice Framework**: MediaSession with Google Assistant integration (no additional dependencies)
- **Background Service**: Already implemented via `MediaSessionService`
- **Permissions**: No new permissions required (MediaSession approach)

## Implementation Complexity Assessment

**Low Complexity**:
- Extend existing MediaSessionCallback (single method)
- Parse voice query strings (regex patterns)
- Send broadcast intents to UI layer

**Medium Complexity**:
- Audio feedback manager with TTS
- Finding references in current transcript segment
- Optional voice command UI button

**High Complexity** (Not Needed):
- Custom voice recognition engine (using MediaSession instead)
- Always-on listening (system handles via Google Assistant)
- Offline speech models (not required per spec)

**Estimated Development Effort**: 3-5 days
- Day 1: MediaSession callback implementation
- Day 2: Voice command parser + audio feedback
- Day 3: Integration with ViewModels
- Day 4-5: Testing and polish

## References

- Android Media3 MediaSession API Documentation
- Google Assistant Integration for Media Apps
- Android Background Service Optimization Guidelines
- MediaSession Callback Reference
- App Actions and Built-In Intents
- Android TextToSpeech API
