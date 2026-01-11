# Strollcast Android App

Native Android application for listening to ML research papers transformed into audio podcasts, with advanced features for active learning including synchronized transcripts, inline note-taking, and listening history tracking.

## Features

### 🎧 Audio Playback
- High-quality audio streaming optimized for speech content
- Background playback with Media3 ExoPlayer
- Automatic position saving and resume
- 15-second skip forward/backward controls
- Playback speed control (0.5x - 2.0x)

### 📝 Interactive Transcripts
- Real-time synchronized transcript display
- Auto-scroll following playback position
- Tap any line to seek to that moment
- Speaker identification (Eric and Maya)
- 3-tier caching (Memory → Room → Network) for offline access
- Automatic cleanup of old transcripts (30+ days)
- LRU eviction when cache exceeds 10MB

### ✍️ Inline Notes
- Create notes on any transcript line
- Rich text support for highlighting key insights
- Notes persist across sessions in local database
- Edit and delete notes seamlessly
- Visual indicators show which lines have notes
- Export notes for review

### 📊 Listening History
- Automatic episode completion tracking (90% threshold)
- Dedicated "Played" tab showing completed episodes
- Completion badges and percentage indicators
- Relative date formatting ("Today", "Yesterday", "N days ago")
- Replay from start functionality
- Real-time Flow-based updates

### 🎤 Voice Commands (Google Assistant)
- **"Hey Google, pause in Strollcast"** - Pause playback
- **"Hey Google, play in Strollcast"** - Resume playback
- **"Hey Google, skip forward 15 seconds in Strollcast"** - Seek forward
- **"Hey Google, skip backward 15 seconds in Strollcast"** - Seek backward
- **"Hey Google, what's playing in Strollcast"** - Get episode information
- Works with screen off via foreground service
- Lock screen media controls integration

## Architecture

### Tech Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose with Material Design 3
- **Architecture**: MVVM with Repository pattern
- **Dependency Injection**: Hilt
- **Database**: Room for local storage
- **Networking**: Retrofit + OkHttp
- **Audio**: Media3 ExoPlayer with MediaSession
- **Background Tasks**: WorkManager
- **Async**: Kotlin Coroutines + Flow

### Key Components

#### ViewModels
- **PlayerViewModel**: Manages audio playback state, position tracking, and completion detection
- **TranscriptViewModel**: Handles transcript loading, caching, and synchronization with playback
- **NotesViewModel**: Manages CRUD operations for inline notes
- **PlayedViewModel**: Tracks and displays listening history with completion badges
- **PodcastListViewModel**: Fetches and displays available podcast episodes

#### Repositories
- **PodcastRepository**: Episode metadata and API communication
- **TranscriptRepository**: 3-tier caching with error handling, network timeouts, and LRU eviction
- **NoteRepository**: Local note persistence and retrieval
- **HistoryRepository**: Completion tracking with 90% threshold logic

#### Services
- **PlaybackService**: MediaSessionService for background playback and voice control
  - Custom MediaSession.Callback for Assistant integration
  - Foreground service with persistent notification
  - Custom actions for 15-second skip commands

#### Workers
- **TranscriptCleanupWorker**: Daily background job to clean up old transcripts (30+ days)

### Data Flow

```
UI Layer (Compose)
    ↓↑
ViewModels (State Management)
    ↓↑
Repositories (Business Logic)
    ↓↑
Data Sources (Room, Retrofit, ExoPlayer)
```

### Caching Strategy

**Transcripts** (3-tier caching):
1. **Memory Cache**: Active transcript in RAM for instant access
2. **Room Database**: Persistent local storage (10MB limit with LRU eviction)
3. **Network**: Download from VTT URLs with 30-second timeout and fallback to stale cache

**Error Handling**:
- 404 errors: Display "Transcript unavailable" message
- Malformed VTT: Log error, re-download if possible, show user-friendly message
- Network timeouts: Fall back to cached data (even if stale)
- Parsing errors: Wrap in TranscriptException with contextual messages

## Project Structure

```
android/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/strollcast/app/
│   │   │   │   ├── data/             # Room DAOs and database
│   │   │   │   ├── di/               # Hilt modules
│   │   │   │   ├── models/           # Data models
│   │   │   │   ├── repository/       # Data layer
│   │   │   │   ├── services/         # Background services
│   │   │   │   ├── ui/
│   │   │   │   │   ├── components/   # Reusable UI components
│   │   │   │   │   ├── screens/      # Full-screen composables
│   │   │   │   │   └── theme/        # Material Design theme
│   │   │   │   ├── util/             # Utilities (VTT parser, etc.)
│   │   │   │   ├── viewmodels/       # ViewModels
│   │   │   │   ├── workers/          # WorkManager workers
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── StrollcastApp.kt  # Main Compose app
│   │   │   │   └── StrollcastApplication.kt  # Application class
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                     # Unit tests
│   │   └── androidTest/              # Instrumentation tests
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts
├── settings.gradle.kts
├── VOICE_COMMANDS_TEST_PLAN.md      # Manual testing guide
└── README.md                         # This file
```

## Building and Running

### Requirements
- **Android Studio**: Hedgehog (2023.1.1) or later
- **Java**: JDK 17
- **Android SDK**: API 26+ (minimum), API 36 (target)
- **Gradle**: 9.0+ (included via wrapper)

### Build Commands

```bash
# Clean build
./gradlew clean

# Debug build
./gradlew assembleDebug

# Release build (requires signing config)
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run instrumentation tests
./gradlew connectedAndroidTest

# Run specific test class
./gradlew testDebugUnitTest --tests "com.strollcast.app.repository.TranscriptRepositoryTest"
```

### Running on Device/Emulator

1. Connect Android device or start emulator
2. Build and install:
```bash
./gradlew installDebug
```
3. Launch app from launcher or via ADB:
```bash
adb shell am start -n com.strollcast.app/.MainActivity
```

## Configuration

### API Endpoints

The app fetches episode metadata from:
```
https://api.strollcast.com/episodes
```

Transcript VTT files are downloaded from URLs provided in episode metadata.

### Signing Configuration

For release builds, create `strollcast-release.keystore` and set environment variables:
```bash
export KEYSTORE_PASSWORD=your_keystore_password
export KEY_PASSWORD=your_key_password
```

## Testing

### Unit Tests (93 tests total)
- **Repository Tests**: TranscriptRepository, HistoryRepository, NoteRepository, PodcastRepository
- **ViewModel Tests**: PlayerViewModel, TranscriptViewModel, NotesViewModel, PlayedViewModel
- **Parser Tests**: VttParser edge cases and error handling

### Instrumentation Tests
- **UI Tests**: TranscriptScreen, PlayerScreen, NotesDialog, PlayedListScreen
- **Database Tests**: TranscriptDao, CompletedEpisodeDao, NoteDao

### Manual Testing

For voice command testing, see `VOICE_COMMANDS_TEST_PLAN.md` which covers:
- Voice commands with screen ON
- Voice commands with screen OFF
- MediaSession metadata verification
- Lock screen controls
- Google Assistant integration

### Test Coverage

Run tests with coverage:
```bash
./gradlew testDebugUnitTestCoverageVerification
```

## Performance

### Optimizations
- **LazyColumn**: Virtualized scrolling for long transcripts (500+ lines tested)
- **Memory Management**: 10MB cache limit with LRU eviction
- **Background Processing**: WorkManager for periodic cleanup (daily, battery-aware)
- **Network Efficiency**: 3-tier caching minimizes redundant downloads
- **Compose Performance**: Recomposition optimization with `remember` and `derivedStateOf`

### Profiling
- Use Android Studio Profiler to monitor CPU, memory, and network usage
- Layout Inspector to verify LazyColumn performance with 500+ items
- Memory Profiler to check for leaks in ViewModels and coroutines

## Accessibility

### TalkBack Support
All interactive UI elements include `contentDescription` for screen readers:
- Transcript lines: "Eric says: [text]" or "Maya says: [text]"
- Note buttons: "Add note to this line", "Edit note", "Delete note"
- Playback controls: "Play", "Pause", "Skip forward 15 seconds", etc.
- Completion badges: "Completed 95%"

### Testing with TalkBack
1. Enable TalkBack: Settings → Accessibility → TalkBack
2. Navigate app using swipe gestures
3. Verify all UI elements are announced clearly
4. Test all interactive actions (tap, long press)

## Troubleshooting

### Common Issues

**Build fails with "25.0.1" error:**
- This is a known Gradle configuration issue unrelated to app code
- Workaround: Clean project and invalidate caches

**Voice commands not working:**
1. Verify Google Assistant is enabled
2. Check PlaybackService is running as foreground service
3. Ensure FOREGROUND_SERVICE permission granted
4. Check LogCat for MediaSession callback invocations

**Transcripts not loading:**
1. Check internet connection
2. Verify VTT URL is valid in episode metadata
3. Check LogCat for network errors or parsing exceptions
4. Try clearing app cache: Settings → Apps → Strollcast → Clear Cache

**Notes not saving:**
1. Check database permissions
2. Verify NoteDao methods in LogCat
3. Check for ViewModel lifecycle issues (retained across config changes)

## Contributing

### Code Style
- Follow Kotlin coding conventions
- Use Material Design 3 components
- Prefer Jetpack Compose over XML layouts
- Write unit tests for all business logic
- Add KDoc comments for public APIs

### Pull Request Process
1. Create feature branch from `main`
2. Implement changes with tests
3. Update README if adding new features
4. Run all tests: `./gradlew test connectedAndroidTest`
5. Submit PR with descriptive commit messages

## License

See main repository LICENSE file.

## Support

For issues and feature requests, visit:
https://github.com/strollcast/StrollcastApp/issues

---

**Built with Claude Code** - Developed iteratively across 6 phases:
- Phase 1: Setup & Foundation
- Phase 2: Interactive Transcripts (US1)
- Phase 3: Inline Notes (US2)
- Phase 4: Listening History (US3)
- Phase 5: Voice Commands (US4)
- Phase 6: Polish & Performance
