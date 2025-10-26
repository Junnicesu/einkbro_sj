# Media Playback Detection and Bluetooth Control - Implementation Summary

## Problem
The initial implementation only detected fullscreen video (via `onShowCustomView()`), which doesn't work for:
- YouTube Music (background audio)
- Inline video playback (not fullscreen)
- Regular audio elements
- Videos that never go fullscreen

## Solution Overview
Implemented a comprehensive JavaScript-based media detection system that monitors ALL HTML5 video and audio elements on web pages.

## Files Created

### 1. MediaPlaybackDetector.kt
**Location:** `/app/src/main/java/info/plateaukao/einkbro/browser/MediaPlaybackDetector.kt`

**Purpose:** JavaScript interface to detect HTML5 media playback state

**Key Features:**
- Provides JavaScript interface with `@JavascriptInterface` methods
- Monitors `play`, `pause`, and `ended` events on all media elements
- Automatically detects dynamically added media elements using MutationObserver
- Calls back to native code when media state changes

## Files Modified

### 2. BrowserController.kt
**Added:** `onMediaPlaybackStateChanged(isPlaying: Boolean)` interface method

**Purpose:** Allows WebView to notify the activity when media playback state changes

### 3. MediaSessionHelper.kt
**Added:** `setPlaybackState(isPlaying: Boolean)` method

**Purpose:** 
- Updates Android MediaSession playback state
- Activates/deactivates media session based on playback
- Allows Bluetooth media buttons to control playback

### 4. BrowserActivity.kt
**Changes:**
1. Added `isMediaPlaying: Boolean` field to track media state
2. Implemented `onMediaPlaybackStateChanged()` to update MediaSession
3. Updated MediaSession callback to:
   - Check if media is playing first
   - Use JavaScript to toggle media playback (play/pause)
   - Fall back to TTS control if no media is playing

### 5. EBWebView.kt
**Changes:**
- Added MediaPlaybackDetector JavaScript interface during initialization
- Detector automatically notifies BrowserController of media state changes

### 6. NinjaWebViewClient.kt (EBWebViewClient)
**Changes:**
- Injects media detection JavaScript on every page load (`onPageFinished`)
- Ensures monitoring works on all pages including dynamically loaded content

## How It Works

### 1. Page Load
```
Page loads → onPageFinished() → Inject INJECTION_SCRIPT
```

### 2. Media Detection
```javascript
// JavaScript monitors all video/audio elements
document.querySelectorAll('video, audio').forEach(element => {
    element.addEventListener('play', () => MediaPlaybackDetector.onMediaPlay());
    element.addEventListener('pause', () => MediaPlaybackDetector.onMediaPause());
    element.addEventListener('ended', () => MediaPlaybackDetector.onMediaEnded());
});
```

### 3. State Updates
```
Media plays → JavaScript calls onMediaPlay()
           → MediaPlaybackDetector notifies BrowserController
           → BrowserActivity updates isMediaPlaying
           → MediaSessionHelper.setPlaybackState(true)
           → Android activates MediaSession
```

### 4. Bluetooth Control
```
Bluetooth pause pressed → MediaSession callback triggered
                       → Check if isMediaPlaying
                       → If yes: Execute JavaScript to pause media
                       → If no: Control TTS instead
```

## Supported Scenarios

✅ YouTube Music (background audio)
✅ YouTube videos (inline and fullscreen)
✅ Any HTML5 video element
✅ Any HTML5 audio element
✅ Dynamically added media elements (SPA pages)
✅ Multiple media elements on one page
✅ TTS playback (when no media is playing)

## Testing Recommendations

1. **YouTube Music**: Play a song, press Bluetooth pause button
2. **Inline Video**: Play YouTube video without fullscreen, test Bluetooth controls
3. **Multiple Tabs**: Switch between tabs with different media
4. **TTS**: Ensure TTS still works when no media is playing
5. **Dynamic Content**: Test on single-page apps (SPAs) where content loads dynamically

## Key Improvements Over Previous Implementation

| Feature | Old (VideoView-based) | New (JavaScript-based) |
|---------|----------------------|------------------------|
| Inline video | ❌ Not detected | ✅ Detected |
| Audio elements | ❌ Not detected | ✅ Detected |
| Background audio | ❌ Not detected | ✅ Detected |
| YouTube Music | ❌ Not detected | ✅ Detected |
| Dynamic content | ❌ Not detected | ✅ Detected |
| Fullscreen video | ✅ Detected | ✅ Detected |
| Resume after pause | ❌ Session deactivates | ✅ Session stays active |

## Bug Fixes

### Issue 1: Cannot Resume After Pause
**Problem:** When media was paused, `isMediaPlaying` became false, causing the MediaSession to deactivate and Bluetooth controls to stop working.

**Solution:**
1. Added `hasMediaElement` flag to track if media exists (separate from playing state)
2. Keep MediaSession active even when paused: `mediaSession.isActive = true` always
3. Updated JavaScript to find paused media and resume it properly
4. JavaScript search priority:
   - First: Currently playing media
   - Second: Paused media
   - Third: Any video/audio element

This ensures Bluetooth controls remain responsive whether media is playing or paused.

### Issue 2: Cannot Control TTS When Web Media Exists
**Problem:** Once `hasMediaElement` was set to true, it never reset, so TTS could never be controlled even when no media was playing.

**Solution:**
1. Implemented smart priority system based on **actual playback state**:
   - **Priority 1:** Currently playing web media
   - **Priority 2:** Currently playing TTS (PLAYING state, not just paused)
   - **Priority 3:** Paused web media (has higher priority than paused TTS)
   - **Priority 4:** Paused/Preparing TTS
   - **Fallback:** TTS control

2. Reset `hasMediaElement` flag when navigating to new page (`onPageStarted`)

3. Update MediaSession state for TTS when no web media is active

**Code Logic:**
```kotlin
val ttsState = ttsViewModel.readingState.value
val isTtsPlaying = ttsState == TtsReadingState.PLAYING

when {
    isMediaPlaying -> toggleWebMediaPlayback()           // Playing web media
    isTtsPlaying -> ttsViewModel.pauseOrResume()         // Playing TTS
    hasMediaElement -> toggleWebMediaPlayback()          // Paused web media
    ttsViewModel.isReading() -> ttsViewModel.pauseOrResume()  // Paused TTS
    else -> ttsViewModel.pauseOrResume()                 // Fallback
}
```

This ensures:
- ✅ Web media controls work when media is playing
- ✅ Web media can be resumed even when TTS is paused
- ✅ TTS controls work when TTS is actively playing
- ✅ Paused web media has priority over paused TTS
- ✅ Can resume paused web media
- ✅ Flags reset on page navigation
- ✅ Proper priority between different media sources

## MediaSession States

- **STATE_PLAYING**: Active when `isMediaPlaying == true`
- **STATE_PAUSED**: Active when `isMediaPlaying == false` but media element exists
- **Always Active**: MediaSession stays active once media is detected (until page navigation)

This allows proper integration with Android's media notification system and Bluetooth controls.
