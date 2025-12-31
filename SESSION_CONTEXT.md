# Strollcast App - Session Context

## Project Overview
iOS podcast app for browsing and playing AI-generated audio summaries of ML research papers from strollcast.com.

## Recent Work Completed

### 1. Transcript & Notes Integration
- Combined transcript and notes views with inline timestamped comments
- Format: `[MM:SS] comment text`
- Auto-scroll to current transcript cue during playback
- Pause playback when editing notes, resume after save
- Tap on transcript cue to seek playback to that position

### 2. UI Improvements
- Moved download menu to PodcastDetailView navigation bar
- Combined PodcastDetailView and PlayerView into one unified view
- Player controls at bottom of detail view
- Replaced plus button with "Add note" placeholder text
- Faster scroll animations (0.15s)

### 3. Zotero Integration
- **Settings page** with API Key input (User ID auto-fetched)
- **Auto-add papers** to Zotero when playback starts
- **Sync notes** to Zotero as child notes of the paper
- Credentials stored securely in iOS Keychain
- Sync status display in Settings

### 4. Bug Fixes
- Fixed note editing to update existing notes instead of adding duplicates
- Fixed Zotero write token length (must be 5-32 chars)
- Fixed Zotero author parsing (firstName required even if empty)
- Fixed CI to auto-detect Xcode version
- Removed automatic text insertion in notes ("Paused at...", etc.)

## Key Files Modified

### Services
- `ListeningHistoryService.swift` - Notes, playback history, Zotero sync triggers
- `ZoteroService.swift` - Zotero API integration, credentials, item/note creation
- `TranscriptService.swift` - VTT transcript parsing
- `AudioPlayer.swift` - Playback controls

### Views
- `PodcastDetailView.swift` - Main episode view with player, transcript, notes
- `PodcastListView.swift` - Episode list with mini player
- `SettingsView.swift` - Zotero configuration
- `ContentView.swift` - Tab navigation (Podcasts, Played, Notes, Settings)

### Configuration
- `.github/workflows/build.yml` - CI with auto-detected Xcode version
- `README.md` - Updated with Zotero instructions and acknowledgments

## Current State
- All features working
- Latest commit: `2af1e85` - "Add Zotero sync for podcasts and notes"
- Branch: `main`
- Pushed to origin

## Zotero API Details
- Endpoint: `https://api.zotero.org/users/{userId}/items`
- Auth: `Zotero-API-Key` header
- Version: `Zotero-API-Version: 3`
- Write token: 5-32 character string
- Items created as `journalArticle` type
- Notes created as `note` type with `parentItem` reference

## Acknowledgments
- Stefan Seritan - Zotero integration idea and workflow design

## To Resume Session
When resuming, you can reference this file for context. The codebase is in a clean state with all changes committed and pushed.
