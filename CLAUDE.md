# StrollcastApp Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-01-11

## Active Technologies
- Kotlin 1.9+ (Android) (003-reference-navigation)
- Room database (existing TranscriptLineEntity already stores text with markdown) (003-reference-navigation)
- Room database for transcript data, SharedPreferences for settings (004-voice-reference-navigation)

- Kotlin 1.9+ (current Android app standard) (002-android-parity)

## Project Structure

```text
src/
tests/
```

## Commands

# Add commands for Kotlin 1.9+ (current Android app standard)

## Code Style

Kotlin 1.9+ (current Android app standard): Follow standard conventions

## Recent Changes
- 004-voice-reference-navigation: Added Kotlin 1.9+
- 003-reference-navigation: Added Kotlin 1.9+ (Android)

- 002-android-parity: Added Kotlin 1.9+ (current Android app standard)

<!-- MANUAL ADDITIONS START -->

## iOS Firebase Setup

The iOS app uses Firebase for crash reporting (Crashlytics) and analytics. Follow these steps to complete the setup:

### 1. Add Firebase SDK via Swift Package Manager

In Xcode:
1. Open `StrollcastApp.xcodeproj`
2. File > Add Package Dependencies
3. Enter URL: `https://github.com/firebase/firebase-ios-sdk`
4. Select these packages:
   - `FirebaseAnalytics`
   - `FirebaseCrashlytics`
5. Add to target: `StrollcastApp`

### 2. Configure Firebase Console

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project or use existing
3. Add iOS app with bundle ID: `com.strollcast.app`
4. Download `GoogleService-Info.plist`
5. Add it to the Xcode project (drag to StrollcastApp folder, ensure "Copy items if needed" is checked)

### 3. Enable Crashlytics

In Firebase Console:
1. Go to Crashlytics
2. Click "Enable Crashlytics"
3. Build and run the app to verify connection

### 4. Upload dSYM Files (for crash symbolication)

Add this build phase in Xcode (Build Phases > New Run Script Phase):
```bash
"${BUILD_DIR%/Build/*}/SourcePackages/checkouts/firebase-ios-sdk/Crashlytics/run"
```

Input files:
```
${DWARF_DSYM_FOLDER_PATH}/${DWARF_DSYM_FILE_NAME}
${DWARF_DSYM_FOLDER_PATH}/${DWARF_DSYM_FILE_NAME}/Contents/Resources/DWARF/${PRODUCT_NAME}
${DWARF_DSYM_FOLDER_PATH}/${DWARF_DSYM_FILE_NAME}/Contents/Info.plist
$(TARGET_BUILD_DIR)/$(UNLOCALIZED_RESOURCES_FOLDER_PATH)/GoogleService-Info.plist
$(TARGET_BUILD_DIR)/$(EXECUTABLE_PATH)
```

### Analytics Events

The app logs these analytics events:
- `app_opened` - When app becomes active
- `podcast_play` - When playback starts
- `podcast_pause` - When playback pauses (includes progress %)
- `podcast_complete` - When episode finishes
- `download_start` - When download begins
- `download_complete` - When download succeeds
- `download_error` - When download fails

### Debug Mode

Analytics and Crashlytics are disabled in DEBUG builds to avoid polluting production data. See `AppDelegate.swift` for configuration.

<!-- MANUAL ADDITIONS END -->
