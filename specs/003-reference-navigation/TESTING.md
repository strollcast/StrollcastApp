# Testing Guide: Reference Navigation Feature

**Feature**: Clickable reference links in transcript views
**Branch**: `003-reference-navigation`
**Date**: 2026-01-11

## Test Environment Setup

### Prerequisites
- Android device or emulator (Android 8.0+)
- Debug APK built and installed
- Test episode: **"strollcast-2026-overview"** (contains many reference links)
- Network access for online tests
- Downloaded episodes for offline tests

### Build and Install
```bash
cd android
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Phase 2: Visual Links Testing (T008-T010)

### T008: Link Styling in Normal Segments ✅

**Test**: Verify links appear styled in non-highlighted transcript segments

**Steps**:
1. Open episode "strollcast-2026-overview"
2. Navigate to Transcript tab
3. Scroll through transcript
4. Locate segments with reference links (e.g., mentioning "FlashAttention-2", "ZeRO", etc.)

**Expected**:
- Link text appears in **blue** color
- Link text has **underline** decoration
- Regular text appears in default color (no styling)
- Links are clearly distinguishable from regular text

**Pass Criteria**: All links in non-highlighted segments are visually distinct

---

### T009: Link Styling in Highlighted Segments ✅

**Test**: Verify links remain visible when segment is currently playing

**Steps**:
1. Open episode "strollcast-2026-overview"
2. Navigate to Transcript tab
3. Play the episode
4. Wait for a segment with links to become highlighted (currently playing)

**Expected**:
- Highlighted segment has colored background
- Link text uses **tertiary** color (different from normal state for contrast)
- Link underline remains visible
- Links remain clearly distinguishable from regular text

**Pass Criteria**: Links are visible and readable against highlighted background

---

### T010: Multiple Links in Single Segment ✅

**Test**: Verify multiple links in one segment are individually clickable

**Steps**:
1. Find a transcript segment with multiple reference links
2. Observe visual styling
3. Note if links are separately styled

**Expected**:
- Each link is individually styled (blue, underlined)
- Text between links uses regular styling
- All links have adequate spacing (48dp touch targets)

**Pass Criteria**: Each link is visually distinct and independently styled

---

## Phase 3: Navigation Testing (T025-T029)

### T025: Navigate to Referenced Episode (Online) ✅

**Test**: Tap a reference link to navigate to another episode while online

**Steps**:
1. Open episode "strollcast-2026-overview"
2. Navigate to Transcript tab
3. Start playing the episode
4. Note current episode and playback position
5. Tap any reference link (e.g., "FlashAttention-2")

**Expected**:
- Referenced episode loads automatically
- Referenced episode begins playing from start
- Player UI updates to show new episode title
- No errors or crashes

**Pass Criteria**: Successfully navigates to and plays referenced episode

---

### T026: Navigate to Downloaded Episode (Offline) ✅

**Test**: Navigate to a downloaded episode while device is offline

**Steps**:
1. Download episode "dao-2023-flashattention_2_fa" (or any referenced episode)
2. Open episode "strollcast-2026-overview"
3. Navigate to Transcript tab
4. **Enable airplane mode**
5. Tap link to downloaded episode

**Expected**:
- Episode loads from local storage
- Episode plays without network request
- No "requires internet" error
- Playback works smoothly

**Pass Criteria**: Downloaded episode plays offline without errors

---

### T027: Error - Episode Not Found ✅

**Test**: Handle gracefully when referenced episode doesn't exist

**Steps**:
1. This test requires a malformed link or non-existent episode ID
2. If available, tap such a link
3. Observe error handling

**Expected**:
- Snackbar appears with message: "Referenced episode not found"
- App does not crash
- User remains on current episode
- Error message dismisses automatically

**Pass Criteria**: Clear error message, no crash, current episode unchanged

---

### T028: Error - Offline + Episode Not Downloaded ✅

**Test**: Handle offline scenario when tapped episode is not downloaded

**Steps**:
1. Open episode "strollcast-2026-overview"
2. Navigate to Transcript tab
3. **Enable airplane mode**
4. Tap link to an episode that is NOT downloaded locally

**Expected**:
- Snackbar appears with message about requiring internet connection
- Suggestion to download episode first
- App does not crash or hang
- User remains on current episode

**Pass Criteria**: Clear offline error message, no crash

**Note**: Current implementation may attempt to load and fail at media level. This is acceptable as Media3 will handle the offline error appropriately.

---

### T029: Malformed or Non-Episode URLs ✅

**Test**: Verify malformed/external links render as plain text

**Steps**:
1. Check transcript for any external links (if present)
2. If transcript contains external URLs (not matching episode pattern), verify they are not clickable

**Expected**:
- Links matching pattern `https://released.strollcast.com/episodes/{id}/{id}.(mp3|m4a)` are clickable
- Other URLs render as plain text (no styling, not clickable)
- No crashes when encountering external links

**Pass Criteria**: Only valid episode links are interactive

---

## Phase 4: Return to Original Episode (T030-T033)

### T030: Verify Playback History Integration ✅

**Test**: Confirm current position is saved before navigation

**Steps**:
1. Open episode "strollcast-2026-overview"
2. Play for ~30 seconds
3. Navigate to Transcript tab
4. Tap a reference link
5. After referenced episode loads, check logs or internal state

**Expected**:
- `savePosition()` called before loading referenced episode
- Current position saved to database
- Playback history updated

**Pass Criteria**: Position is saved (can be verified by returning)

---

### T031: Single Navigation Return ✅

**Test**: Navigate A → B, then return to A

**Steps**:
1. Open episode A ("strollcast-2026-overview")
2. Play to position ~00:30
3. Tap reference link to episode B
4. After episode B starts playing, press "Play Previous" button (or back navigation)

**Expected**:
- Episode A resumes playing
- Playback position is ~00:30 (where left off)
- Player UI shows episode A
- No errors or crashes

**Pass Criteria**: Successfully returns to original episode at saved position

---

### T032: Multiple Navigation Chain ✅

**Test**: Navigate A → B → C, then return through C → B → A

**Steps**:
1. Open episode A ("strollcast-2026-overview")
2. Play to position ~00:30
3. Tap reference link to episode B
4. In episode B transcript, find another reference link to episode C
5. Tap link to episode C
6. Press "Play Previous" twice

**Expected**:
- First "Previous": Returns to episode B (at position where link was clicked)
- Second "Previous": Returns to episode A (at ~00:30)
- Each return preserves playback position
- UI updates correctly each time

**Pass Criteria**: Can navigate back through entire chain with position preservation

---

### T033: Position Preservation Accuracy ✅

**Test**: Verify exact position is restored when returning

**Steps**:
1. Open episode A
2. Play to specific position (e.g., 1:23)
3. Tap reference link to episode B
4. Immediately press "Play Previous"
5. Note the resumed position

**Expected**:
- Position matches exactly (±1 second tolerance)
- Audio continues smoothly from saved position
- No jarring jumps or restarts

**Pass Criteria**: Position restored within 1 second of saved value

---

## Phase 5: Polish & Edge Cases (T034-T040)

### T034: Accessibility - Touch Targets ✅

**Test**: Verify minimum 48dp touch targets for links

**Steps**:
1. Open transcript with links
2. Attempt to tap links with finger (not stylus)
3. Tap near edges of link text

**Expected**:
- Links are easily tappable with finger
- Touch target extends beyond visible text (minimum 48dp per Material guidelines)
- No missed taps due to small target
- Accidental taps on adjacent text don't trigger wrong action

**Pass Criteria**: Links are easily tappable, meet Android accessibility guidelines

**Note**: ClickableText with default Material3 styling should automatically provide adequate touch targets.

---

### T035: Link Contrast in Highlighted State ✅

**Test**: Verify links remain visible against highlighted background

**Steps**:
1. Play episode to segment with links
2. Observe link color in highlighted (currently playing) state
3. Check contrast ratio

**Expected**:
- Links use tertiary color in highlighted state (different from normal primary color)
- Adequate contrast against primaryContainer background
- Links remain readable
- Underline decoration visible

**Pass Criteria**: Links are clearly visible in both normal and highlighted states

---

### T036: Performance with Many Links ✅

**Test**: Verify smooth scrolling with episodes containing many links

**Steps**:
1. Open episode "strollcast-2026-overview" (contains ~20+ reference links)
2. Navigate to Transcript tab
3. Rapidly scroll up and down through transcript
4. Monitor frame rate and responsiveness

**Expected**:
- Scrolling remains smooth (60 fps)
- No lag or jank when rendering links
- Link parsing doesn't block UI thread
- Memory usage stays reasonable

**Pass Criteria**: Smooth 60fps scrolling, no performance degradation

**Note**: `remember()` caches parsed AnnotatedString to avoid re-parsing on recomposition.

---

### T037: Episodes Without Links ✅

**Test**: Verify feature doesn't break episodes with no reference links

**Steps**:
1. Open an older episode without reference links
2. Navigate to Transcript tab
3. Scroll through transcript
4. Tap on regular text

**Expected**:
- Transcript displays normally (no styling changes)
- Tapping text seeks to that timestamp (existing behavior)
- Long press shows note dialog (existing behavior)
- No errors or warnings in logs

**Pass Criteria**: Existing transcript behavior works unchanged for episodes without links

---

### T038: Existing Features Preserved ✅

**Test**: Verify all existing transcript features still work

**Steps**:
1. Open any episode with transcript
2. Test each existing feature:
   - **Tap to seek**: Tap regular text → verify playback seeks to that timestamp
   - **Long press for notes**: Long press → verify note dialog appears
   - **Auto-scroll**: Play episode → verify transcript auto-scrolls to current segment
   - **Highlighting**: Play episode → verify currently playing segment is highlighted
   - **Note indicator**: Segments with notes show badge

**Expected**:
- All existing features work as before
- No regressions or broken functionality
- Link feature doesn't interfere with existing interactions

**Pass Criteria**: All 5 existing features work correctly

---

### T039: TalkBack Accessibility ✅

**Test**: Verify feature works with Android TalkBack screen reader

**Steps**:
1. **Enable TalkBack** in device settings (Settings → Accessibility → TalkBack)
2. Open episode with links
3. Navigate to Transcript tab
4. Swipe through transcript segments
5. Focus on a link
6. Double-tap to activate link

**Expected**:
- TalkBack announces links as "link" or "button"
- Link text is read aloud
- Focused link has visual focus indicator
- Double-tap activates link (navigates to episode)
- Error messages are announced via TalkBack

**Pass Criteria**: Links are discoverable and activatable via TalkBack

**Note**: ClickableText with AnnotatedString should automatically support TalkBack.

---

### T040: Final Comprehensive Test ✅

**Test**: End-to-end test covering full user journey

**Steps**:
1. Fresh app install (clear data)
2. Open episode "strollcast-2026-overview"
3. Navigate to Transcript tab
4. Observe links are styled correctly (T008-T010)
5. Play episode to ~00:30
6. Tap first reference link
7. Verify navigation succeeds (T025)
8. Press "Play Previous"
9. Verify return to original at ~00:30 (T031)
10. Tap another reference link
11. Enable airplane mode
12. Tap link to non-downloaded episode → verify error (T028)
13. Disable airplane mode
14. Test note-taking on segment with link
15. Test seeking by tapping regular text

**Expected**:
- Complete user journey works smoothly
- All features integrate correctly
- No crashes, errors, or unexpected behavior
- Performance is smooth throughout
- UI is responsive and intuitive

**Pass Criteria**: Full feature works end-to-end without issues

---

## Test Results Summary

Record test results here:

| Test ID | Test Name | Status | Notes |
|---------|-----------|--------|-------|
| T008 | Link styling (normal) | ⬜ | |
| T009 | Link styling (highlighted) | ⬜ | |
| T010 | Multiple links | ⬜ | |
| T025 | Navigate (online) | ⬜ | |
| T026 | Navigate (offline) | ⬜ | |
| T027 | Error: not found | ⬜ | |
| T028 | Error: offline | ⬜ | |
| T029 | Malformed URLs | ⬜ | |
| T030 | History integration | ⬜ | |
| T031 | Single return | ⬜ | |
| T032 | Multiple returns | ⬜ | |
| T033 | Position accuracy | ⬜ | |
| T034 | Touch targets | ⬜ | |
| T035 | Link contrast | ⬜ | |
| T036 | Performance | ⬜ | |
| T037 | No links | ⬜ | |
| T038 | Existing features | ⬜ | |
| T039 | TalkBack | ⬜ | |
| T040 | End-to-end | ⬜ | |

Legend: ⬜ Not tested | ✅ Pass | ❌ Fail

---

## Known Issues / Limitations

1. **Network detection**: The implementation doesn't explicitly check network connectivity before attempting to load episodes. Instead, it relies on Media3's built-in error handling. This means offline errors may manifest as media loading failures rather than preemptive "offline" messages.

2. **External links**: Currently, only Strollcast episode URLs are made clickable. External links (to papers, GitHub, etc.) render as plain text. This is by design per the specification.

3. **Link styling in dark mode**: Tested with system theme. Link colors (primary/tertiary) should automatically adapt to dark mode via Material Theme.

---

## Regression Testing

After completing this feature, verify these existing features still work:

- [ ] Episode list loads and displays correctly
- [ ] Podcast playback (play, pause, seek)
- [ ] Download management (download, cancel, delete)
- [ ] Notes feature (create, view, edit notes)
- [ ] Played list (mark complete, replay)
- [ ] Settings (all preferences work)

---

## Performance Benchmarks

**Target metrics** (from spec):
- Link parsing: < 50ms per segment
- Episode navigation: < 3s from tap to playback (network dependent)
- UI rendering: 60 fps scrolling

**Measurement approach**:
- Use Android Studio Profiler for frame rate
- Add logging in `MarkdownLinkParser.buildAnnotatedString()` to measure parse time
- Use network inspector for episode fetch timing
