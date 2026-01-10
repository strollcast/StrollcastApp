# Firebase Setup Guide

Firebase Crashlytics and Analytics have been integrated into the Strollcast Android app.

## Prerequisites

1. Firebase project created at https://console.firebase.google.com/
2. Android app registered in Firebase with package name: `com.strollcast.app`

## Setup Steps

### 1. Download google-services.json

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Click on the Android app (or add one if not exists)
4. Download `google-services.json`
5. Place it at: `android/app/google-services.json`

**⚠️ IMPORTANT**: Add this to your `.gitignore`:
```
google-services.json
```

### 2. Enable Services in Firebase Console

#### Crashlytics
1. In Firebase Console → Build → Crashlytics
2. Click "Enable Crashlytics"
3. No additional configuration needed

#### Analytics
1. In Firebase Console → Build → Analytics
2. Should be auto-enabled
3. Review data collection settings

### 3. Build the App

After adding `google-services.json`:

```bash
cd android
./gradlew assembleDebug  # Debug build
./gradlew bundleRelease  # Release build
```

### 4. Test Firebase Integration

Run the app and check:

**Crashlytics:**
- Crashes will appear in Firebase Console → Crashlytics
- Test with: Throw an exception and check console after ~5 minutes

**Analytics:**
- Events appear in Firebase Console → Analytics → Events
- Real-time data in DebugView (enable debug mode on device)

## Enable Debug Mode (Testing)

To see real-time analytics:

```bash
adb shell setprop debug.firebase.analytics.app com.strollcast.app
```

To disable:
```bash
adb shell setprop debug.firebase.analytics.app .none.
```

## Usage in Code

The app includes `AnalyticsHelper` for easy tracking:

```kotlin
@Inject
lateinit var analyticsHelper: AnalyticsHelper

// Log custom events
analyticsHelper.logPodcastPlayed(podcastId, title)
analyticsHelper.logPodcastDownloaded(podcastId)

// Log errors
analyticsHelper.logError("Something went wrong", exception)

// Log screen views
analyticsHelper.logScreenView("PodcastList")
```

## Events Tracked

- `podcast_played` - When user plays a podcast
- `podcast_downloaded` - When user downloads a podcast
- `zotero_sync` - When Zotero sync succeeds/fails
- `screen_view` - Screen navigation

## Privacy Considerations

Firebase collects:
- Crash reports with stack traces
- Analytics events (custom + automatic)
- Device info (model, OS version, etc.)

**Update your privacy policy** to include:
- Firebase Crashlytics data collection
- Firebase Analytics data collection
- Link to [Firebase privacy policy](https://firebase.google.com/support/privacy)

## Production Checklist

- [ ] `google-services.json` added and NOT committed to git
- [ ] Privacy policy updated
- [ ] Tested crash reporting (throw test exception)
- [ ] Verified analytics events in Firebase Console
- [ ] ProGuard rules configured (already done)
- [ ] Release build tested with Firebase

## Troubleshooting

### Build fails with "google-services.json not found"
- Ensure file is at `android/app/google-services.json`
- Check file is valid JSON (download again if needed)

### No crashes appearing
- Wait 5-10 minutes after crash
- Check Crashlytics is enabled in Firebase Console
- Verify app is in release mode (debug crashes may not upload)

### No analytics events
- Enable debug mode (see above)
- Check DebugView in Firebase Console
- Ensure internet connection
- Events batch every ~1 hour (debug mode is instant)

## Additional Resources

- [Firebase Crashlytics Docs](https://firebase.google.com/docs/crashlytics)
- [Firebase Analytics Docs](https://firebase.google.com/docs/analytics)
- [ProGuard/R8 Setup](https://firebase.google.com/docs/crashlytics/get-deobfuscated-reports)
