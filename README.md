# Strollcast

A native  app for browsing and playing episodes from [strollcast.com](https://strollcast.com) - AI-generated audio summaries of machine learning research papers.

![App Icon](StrollcastApp/Assets.xcassets/AppIcon.appiconset/AppIcon.png)

## Features

- Browse podcast episodes with title, authors, year, and duration
- Stream episodes directly or download for offline playback
- Background audio playback with lock screen controls
- Skip forward/backward 15 seconds
- Progress tracking with seek slider
- Transcript view with tap-to-seek and inline notes
- Zotero integration to save papers to your library

## Zotero Integration

Link your Zotero account to save papers from podcasts to your library.

### Setup

1. Go to [zotero.org/settings/keys](https://www.zotero.org/settings/keys)
2. Click **"Create new private key"** under the Applications section
3. Give the key a name (e.g., "Strollcast")
4. Under **Permissions**, enable:
   - "Allow library access"
   - "Allow write access" (to add items to your library)
5. Click **"Save Key"** and copy the generated API key

### In the ios App

1. Open Strollcast and go to the **Settings** tab
2. Paste your **API Key**
3. Tap **"Save & Validate"** to verify the connection
4. Your User ID will be automatically fetched
5. A green checkmark indicates successful setup

## iOS Installation

### Option 1: Build from Source (Recommended)

**Requirements:**
- macOS with Xcode 15+
- Apple ID (free) for simulator, or Apple Developer account for physical device

**Steps:**
1. Clone the repository:
   ```bash
   git clone git@github.com:strollcast/StrollcastApp.git
   cd ios/StrollcastApp
   ```

2. Open in Xcode:
   ```bash
   open StrollcastApp.xcodeproj
   ```

3. Select your team:
   - Open project settings (click on `StrollcastApp` in the navigator)
   - Select the `StrollcastApp` target
   - Go to "Signing & Capabilities"
   - Choose your team from the dropdown

4. Build and run:
   - Select a simulator or your connected device
   - Press `Cmd + R` or click the Play button

### Option 2: Sideload IPA (No Mac Required)

Download the latest unsigned IPA from [Releases](https://github.com/strollcast/StrollcastApp/releases) and install using one of these tools:

| Tool | Platform | Notes |
|------|----------|-------|
| [AltStore](https://altstore.io) | Windows/macOS | Requires AltServer running on computer |
| [Sideloadly](https://sideloadly.io) | Windows/macOS | Simple drag-and-drop |
| [Scarlet](https://usescarlet.com) | iOS | On-device signing |

**Note:** Sideloaded apps signed with a free Apple ID expire after 7 days and need to be re-signed.

## Project Structure

```
StrollcastApp/
├── StrollcastApp.swift              # App entry point
├── ContentView.swift                # Root tab view
├── Info.plist                       # App configuration
├── Models/
│   └── Podcast.swift                # Podcast data model
├── Services/
│   ├── PodcastService.swift         # Fetches podcasts from strollcast.com
│   ├── DownloadManager.swift        # Downloads & caches audio files
│   ├── AudioPlayer.swift            # AVPlayer wrapper with controls
│   ├── ListeningHistoryService.swift # Notes and playback history
│   ├── TranscriptService.swift      # VTT transcript parsing
│   └── ZoteroService.swift          # Zotero API integration
├── Views/
│   ├── PodcastListView.swift        # Main list with mini player
│   ├── PodcastRowView.swift         # List row with download status
│   ├── PodcastDetailView.swift      # Episode details, player & transcript
│   ├── PlayedListView.swift         # Completed episodes
│   ├── NotesListView.swift          # All notes across episodes
│   └── SettingsView.swift           # Zotero configuration
└── Assets.xcassets/                 # App icons and colors
```

## Building the IPA Locally

To create an unsigned IPA for distribution:

```bash
# Build the archive

cd ios

xcodebuild archive \
  -project StrollcastApp.xcodeproj \
  -scheme StrollcastApp \
  -configuration Release \
  -archivePath build/StrollcastApp.xcarchive \
  CODE_SIGNING_ALLOWED=NO

# Export the IPA
mkdir -p build/Payload
cp -r build/StrollcastApp.xcarchive/Products/Applications/StrollcastApp.app build/Payload/
cd build && zip -r StrollcastApp-unsigned.ipa Payload
```

## Acknowledgments

- **Stefan Seritan** - Zotero integration idea and workflow design

## License

MIT License - see [LICENSE](LICENSE) for details.
