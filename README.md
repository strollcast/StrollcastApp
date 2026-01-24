# Strollcast

Native mobile apps for browsing and playing episodes from [strollcast.com](https://strollcast.com) - AI-generated audio summaries of machine learning research papers.

[![Download on the App Store](https://img.shields.io/badge/App_Store-0D96F6?style=for-the-badge&logo=app-store&logoColor=white)](https://apps.apple.com/us/app/strollcast/id6758072315)

![App Icon](ios/StrollcastApp/Assets.xcassets/AppIcon.appiconset/AppIcon.png)

## Features

- Browse podcast episodes with title, authors, year, and duration
- Stream episodes directly or download for offline playback
- Background audio playback with lock screen controls
- Skip forward/backward 15 seconds
- Progress tracking with seek slider
- Transcript view with tap-to-seek and inline notes
- Zotero integration to save papers to your library

## Installation

### iOS

**[Download from the App Store](https://apps.apple.com/us/app/strollcast/id6758072315)**

Or build from source - see the [iOS README](ios/README.md) for instructions.

### Android

See the [Android README](android/README.md) for installation instructions including:
- Building with Android Studio or Gradle
- APK installation
- Android-specific project structure

## Zotero Integration

Link your Zotero account to automatically save papers from podcasts to your library.

### Setup

1. Go to [zotero.org/settings/keys](https://www.zotero.org/settings/keys)
2. Click **"Create new private key"** under the Applications section
3. Give the key a name (e.g., "Strollcast")
4. Under **Permissions**, enable:
   - "Allow library access"
   - "Allow write access" (to add items to your library)
5. Click **"Save Key"** and copy the generated API key

### In the App

Configure the Zotero integration in the app's **Settings** screen:
1. Paste your **API Key**
2. Tap **"Save & Validate"** to verify the connection
3. Your User ID will be automatically fetched
4. A green checkmark indicates successful setup

Platform-specific setup instructions are also available in the [iOS README](ios/README.md) and [Android README](android/README.md).

## Acknowledgments

- **Stefan Seritan** - Zotero integration idea and workflow design

## License

MIT License - see [LICENSE](LICENSE) for details.
