# Voice Commands Manual Testing Plan

## Overview
This document outlines the manual testing procedure for Phase 5 (User Story 4) - Voice Commands for Playback Control. These tests verify that MediaSession integration enables hands-free control through Google Assistant.

## Prerequisites
- Physical Android device with Google Assistant enabled (API 26+)
- Strollcast app installed with PlaybackService enhancements
- At least one podcast episode available for playback
- Network connection for Google Assistant

## Test Environment Setup
1. Build and install the debug APK on a physical Android device
2. Ensure Google Assistant is configured and responding to "Hey Google" or "OK Google"
3. Grant all necessary permissions (audio, notifications, etc.)
4. Ensure the device is signed in to a Google account

## Test Cases

### T077: Voice Commands with Screen ON

**Objective**: Verify all voice commands work correctly when device screen is on and unlocked

#### Test Procedure

1. **Start Playback**
   - Open Strollcast app
   - Navigate to a podcast episode
   - Start playback
   - **Verify**: Audio plays and player UI shows "Playing" state

2. **Voice Command: Pause**
   - Say: "Hey Google, pause in Strollcast"
   - **Expected**: Playback pauses immediately
   - **Verify**: Player UI shows "Paused" state
   - **Verify**: Assistant confirms "Pausing Strollcast"

3. **Voice Command: Play/Resume**
   - Say: "Hey Google, play in Strollcast"
   - **Expected**: Playback resumes from paused position
   - **Verify**: Player UI shows "Playing" state
   - **Verify**: Audio continues from where it was paused

4. **Voice Command: Skip Forward**
   - Note current playback position
   - Say: "Hey Google, skip forward 15 seconds in Strollcast"
   - **Expected**: Playback position advances by 15 seconds
   - **Verify**: Time indicator in UI reflects the skip
   - **Verify**: Audio plays from new position

5. **Voice Command: Skip Backward**
   - Note current playback position
   - Say: "Hey Google, skip backward 15 seconds in Strollcast"
   - **Expected**: Playback position rewinds by 15 seconds
   - **Verify**: Time indicator in UI reflects the rewind
   - **Verify**: Audio plays from new position

6. **Voice Command: What's Playing**
   - Say: "Hey Google, what's playing in Strollcast?"
   - **Expected**: Assistant reads the episode title and authors
   - **Verify**: Information matches current episode
   - **Verify**: Artwork thumbnail appears in Assistant UI (if configured)

#### Success Criteria
- ✅ All voice commands execute within 2 seconds
- ✅ Commands work reliably (90%+ success rate across 10 attempts)
- ✅ UI state updates reflect voice command actions
- ✅ No app crashes or freezes during voice commands
- ✅ Assistant provides appropriate verbal/visual feedback

---

### T078: Voice Commands with Screen OFF

**Objective**: Verify voice commands work when device screen is locked or off, confirming foreground service operation

#### Test Procedure

1. **Setup**
   - Open Strollcast app
   - Start playback of a podcast episode
   - **Verify**: Foreground notification appears in status bar

2. **Lock Screen (Screen On)**
   - Lock the device (screen stays on but locked)
   - **Verify**: Lock screen shows media controls
   - **Verify**: Episode title and artwork visible on lock screen

3. **Voice Command: Pause (Screen Locked)**
   - Say: "Hey Google, pause in Strollcast"
   - **Expected**: Playback pauses
   - **Verify**: Lock screen controls update to "Paused" state
   - **Verify**: Assistant responds without unlocking device

4. **Voice Command: Play (Screen Locked)**
   - Say: "Hey Google, play in Strollcast"
   - **Expected**: Playback resumes
   - **Verify**: Lock screen controls update to "Playing" state

5. **Screen OFF Test**
   - Turn off device screen completely (press power button)
   - **Verify**: Audio continues playing
   - **Verify**: Foreground notification persists

6. **Voice Command: Skip Forward (Screen OFF)**
   - Say: "Hey Google, skip forward 15 seconds in Strollcast"
   - **Expected**: Playback position advances by 15 seconds
   - **Verify**: Audio reflects the skip
   - **Verify**: Screen remains off during command

7. **Voice Command: Pause (Screen OFF)**
   - Say: "Hey Google, pause in Strollcast"
   - **Expected**: Playback pauses
   - **Verify**: Audio stops
   - **Verify**: Foreground notification updates to "Paused"

8. **Foreground Service Persistence**
   - Leave device with screen off for 5 minutes
   - Say: "Hey Google, play in Strollcast"
   - **Expected**: Playback resumes immediately
   - **Verify**: Service did not get killed by system

#### Success Criteria
- ✅ All voice commands work with screen locked
- ✅ All voice commands work with screen completely off
- ✅ Foreground service persists for extended periods (30+ minutes)
- ✅ Lock screen media controls update correctly
- ✅ No wake locks preventing device sleep
- ✅ Battery usage remains reasonable (<5% per hour of playback)

---

### T079: MediaSession Metadata Verification

**Objective**: Verify MediaSession metadata appears correctly in all Android media control surfaces

#### Test Procedure

1. **Lock Screen Media Controls**
   - Start playback
   - Lock device
   - **Verify**: Episode title displayed
   - **Verify**: Authors displayed as "Artist"
   - **Verify**: Podcast artwork thumbnail shown
   - **Verify**: Play/Pause button functional
   - **Verify**: Skip forward/backward buttons present and functional

2. **Quick Settings Media Controls**
   - Swipe down from top to open Quick Settings
   - **Verify**: Strollcast media control card visible
   - **Verify**: Episode title and authors correct
   - **Verify**: Artwork thumbnail present
   - **Verify**: All playback controls work

3. **Google Assistant Response**
   - Say: "Hey Google, what's playing?"
   - **Verify**: Assistant shows Strollcast episode card
   - **Verify**: Episode title spoken correctly
   - **Verify**: Authors mentioned
   - **Verify**: Artwork displayed in Assistant UI

4. **Android Auto Simulation** (if available)
   - Connect device to Android Auto (or use simulator)
   - Navigate to media apps
   - **Verify**: Strollcast appears in media app list
   - **Verify**: Episode metadata displays in car interface
   - **Verify**: Voice commands work through car's assistant

5. **Wear OS Companion** (if available)
   - Pair with Wear OS watch
   - **Verify**: Playback controls appear on watch
   - **Verify**: Episode title visible on watch
   - **Verify**: Play/Pause works from watch

6. **Metadata Update on Episode Change**
   - Start playing Episode A
   - Switch to Episode B
   - **Verify**: All media surfaces update to show Episode B metadata
   - **Verify**: Artwork changes appropriately
   - **Verify**: "What's playing" query returns Episode B info

#### Success Criteria
- ✅ Metadata appears correctly on lock screen
- ✅ Metadata appears correctly in Quick Settings
- ✅ Assistant accurately reports current episode
- ✅ Artwork loads and displays (no broken images)
- ✅ Metadata updates immediately when episode changes
- ✅ All control surfaces remain functional

---

## Known Issues / Limitations

### Current Implementation Notes
1. **Player Architecture**: The current implementation has PlayerScreen creating its own ExoPlayer instance separate from PlaybackService. For full voice command integration, the app should use MediaController to connect to the service's player.

2. **Metadata Updates**: The `updateMetadata()` method in PlaybackService requires manual invocation. Currently, metadata won't update automatically when episodes change until PlayerViewModel is refactored to use the service's player.

3. **Testing Workaround**: To test voice commands, you may need to temporarily bind the UI to the PlaybackService's player, or issue MediaController commands directly to the service.

### Future Enhancements (Post-Phase 5)
- Refactor PlayerViewModel to use MediaController instead of creating its own player
- Automatic metadata updates when episodes change
- Custom notification layout with episode-specific artwork
- Sleep timer integration with voice commands
- Playback speed control via voice

---

## Test Results Template

| Test Case | Date | Device | Android Version | Pass/Fail | Notes |
|-----------|------|--------|-----------------|-----------|-------|
| T077: Screen ON | | | | | |
| T078: Screen OFF | | | | | |
| T079: Metadata | | | | | |

---

## Troubleshooting

### Voice Commands Not Working
1. Verify Google Assistant is enabled and configured
2. Check that PlaybackService is running as foreground service
3. Verify MediaSession is created and active
4. Check LogCat for MediaSession callback invocations
5. Ensure app has FOREGROUND_SERVICE permission

### Metadata Not Appearing
1. Verify `updateMetadata()` is being called
2. Check that artwork URLs are valid and accessible
3. Verify MediaMetadata is set on the MediaItem
4. Check for errors in LogCat related to image loading

### Service Gets Killed
1. Verify `startForeground()` is called in `onCreate()`
2. Check that notification channel is created
3. Ensure foregroundServiceType="mediaPlayback" in manifest
4. Verify notification is persistent (ongoing = true)

---

## Testing Checklist

- [ ] All T077 test cases pass with screen ON
- [ ] All T078 test cases pass with screen OFF
- [ ] All T079 metadata verifications complete
- [ ] Tested on multiple Android versions (26, 30, 34)
- [ ] Tested with different Google Assistant languages (if applicable)
- [ ] Battery usage profiled and acceptable
- [ ] No memory leaks detected during extended testing
- [ ] All known issues documented
- [ ] Test results logged in table above
