# Strollcast Android App

Android app for Strollcast - transforms ML research papers into audio podcasts.

## Features

- Browse and play podcast episodes
- Background audio playback with MediaSession
- Download episodes for offline listening
- Playback position saving and restoration
- Zotero integration for automatic paper library management
- Material Design 3 UI with dynamic colors

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material Design 3
- **Architecture**: MVVM with Hilt dependency injection
- **Database**: Room for local persistence
- **Networking**: Retrofit with Gson
- **Media Playback**: Media3 (ExoPlayer) with MediaSession
- **Async**: Kotlin Coroutines and Flow

## Project Structure

```
app/src/main/java/com/strollcast/app/
├── data/                    # Database layer
│   ├── StrollcastDatabase.kt
│   ├── PodcastDao.kt
│   ├── PlaybackHistoryDao.kt
│   └── DownloadDao.kt
├── di/                      # Dependency injection modules
│   ├── DatabaseModule.kt
│   └── NetworkModule.kt
├── models/                  # Data models
│   ├── Podcast.kt
│   ├── PlaybackHistoryEntry.kt
│   └── DownloadedEpisode.kt
├── network/                 # API client
│   └── StrollcastApi.kt
├── repository/              # Data repository
│   └── PodcastRepository.kt
├── services/                # Background services
│   └── PlaybackService.kt
├── ui/                      # UI layer
│   ├── screens/
│   │   ├── PodcastListScreen.kt
│   │   ├── PlayerScreen.kt
│   │   └── SettingsScreen.kt
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   └── StrollcastApp.kt
├── viewmodels/              # ViewModels
│   ├── PodcastViewModel.kt
│   ├── PlayerViewModel.kt
│   └── SettingsViewModel.kt
├── MainActivity.kt
└── StrollcastApplication.kt
```

## Building the App

### Requirements

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17 or later
- Android SDK 34
- Minimum SDK 26 (Android 8.0)

### Build Steps

1. Clone the repository:
   ```bash
   cd StrollcastApp/android
   ```

2. Open the project in Android Studio

3. Sync Gradle files

4. Run the app:
   - Click "Run" or press Shift+F10
   - Or use command line:
     ```bash
     ./gradlew installDebug
     ```

### Build Variants

- **debug**: Development build with debugging enabled
- **release**: Production build with ProGuard/R8 optimization

## Key Components

### Playback Service

Background media playback using Media3:
- Foreground service for uninterrupted playback
- MediaSession for system media controls
- Audio focus management
- Position saving and restoration

### Database

Room database with three tables:
- `podcasts`: Episode metadata
- `playback_history`: Last playback position per episode
- `downloaded_episodes`: Downloaded episode file tracking

### Repository Pattern

Single source of truth for data operations:
- Network API calls
- Local database caching
- Download management
- Playback history tracking

## API Endpoint

The app fetches episodes from:
```
https://strollcast.com/api/episodes.json
```

## Zotero Integration

Configure in Settings to automatically add papers to your Zotero library:
- API Key (required)
- User ID (required)
- Collection Key (optional)

## License

See root LICENSE file.
