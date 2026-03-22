# SBS Mode Requirements Plan

This document defines requirements and implementation steps for Side-by-Side (SBS) modes used by the app:

- Browser SBS copy mode for Google Cardboard browsing.
- Fullscreen video SBS mode for Normal2D and HBS sources.

## 1. Purpose

Provide a low-latency SBS browsing experience by rendering browser content once and duplicating it in the same draw pass, while preserving existing fullscreen video behavior.

## 2. Definitions

- `BrowserSbsCopy`: browser UI is interactive in the left pane; right pane is a non-interactive identical copy (not flipped).
- `FullscreenVideoSbs`:
  - `FULLSCREEN_VIDEO_NORMAL2D`: duplicate left-frame content to right pane.
  - `FULLSCREEN_VIDEO_HBS`: do not duplicate; let the player show native half-SBS channels.
- `inwardMarginPx`: black center-safe margins near the pane split to improve lens-edge readability.

## 3. Requirements

### 3.1 Browser SBS (Cardboard usability)

1. Browser SBS uses a custom container (`SbsDualRenderContainer`) that draws browser content twice in one render pass.
2. Left pane is interactive and rendered from the real browser view tree.
3. Right pane is a visual-only copy; right-side touches are consumed.
4. Copy is identical and non-flipped.
5. Symmetric black inward margins are applied around the center split.
6. Browser copy must not use `PixelCopy` or bitmap capture loops.
7. No independent right-pane controls are required.

### 3.2 Fullscreen video behavior

1. Entering fullscreen from `BrowserSbsCopy` transitions to fullscreen SBS state.
2. Normal2D: keep the existing fullscreen `PixelCopy` overlay path.
3. HBS: hide fullscreen copy overlay; rely on source channel layout.
4. Runtime toggle between Normal2D and HBS remains available.
5. Exiting fullscreen restores `BrowserSbsCopy` and re-enables container dual-render.

### 3.3 Detection rules (initial)

1. `config.videoCompressedMode == true` acts as the initial HBS heuristic.
2. Manual override via fullscreen SBS/HBS toggle button remains required.
3. Detection logic remains isolated for future metadata-driven detection.

## 4. Non-Goals (phase 1)

- Lens distortion correction.
- Independent per-eye browser interaction.
- Head-tracking stereo scene rendering.

## 5. Acceptance Criteria

1. In `BrowserSbsCopy`, left pane is fully usable and right pane follows with no noticeable copy latency.
2. Right pane is non-flipped and non-interactive.
3. Center inward margins appear black on both panes.
4. Fullscreen Normal2D still duplicates to both panes.
5. Fullscreen HBS still shows proper per-eye channels (no full-frame duplication).
6. Enter/exit fullscreen preserves SBS state transitions without stale overlays/listeners.
7. Exiting browser SBS restores normal full-width browsing.

## 6. Implementation Plan (v3)

### Phase 1 - Browser renderer migration

1. Add `SbsDualRenderContainer` (`app/src/main/java/info/plateaukao/einkbro/view/SbsDualRenderContainer.kt`).
2. Host `ActivityMainBinding.root` inside this container in `BrowserActivity.onCreate`.
3. Implement dual-draw logic:
   - draw child once in left pane,
   - draw center black margins,
   - draw identical copy in right pane.
4. Consume right-pane touch events so only left pane is operable.

### Phase 2 - Browser mode transitions

1. `enterBrowserSbsMode()` enables container SBS copy with configured inward margin.
2. `exitBrowserSbsMode()` disables container SBS copy.
3. Remove browser-mode dependency on `PixelCopy` refresh loops.

### Phase 3 - Fullscreen mode continuity

1. Keep fullscreen Normal2D copy path (`SbsCopyOverlayView` + `PixelCopy`).
2. Keep fullscreen HBS overlay-off path.
3. Keep runtime fullscreen toggle and transition handlers:
   - `handleSbsOnShowCustomView()`
   - `handleSbsOnHideCustomView()`
   - `toggleSbsVideoHbsMode()`

### Phase 4 - Validation

1. Browser pages: verify full usability and low-latency right copy.
2. Cursor/mouse: verify right copy tracks left without capture lag.
3. Fullscreen Normal2D/HBS: verify no regression.
4. Pause/resume and repeated fullscreen enter/exit: verify no leaked refresh tasks/overlays.

## 7. Risks and Mitigations

- Extra overdraw from dual-dispatch: acceptable tradeoff for latency reduction; bounded by e-ink refresh constraints.
- Fullscreen still uses `PixelCopy`: intentionally scoped to video mode where container dual-draw is not guaranteed for surface-backed content.
- Layout/input regressions: right-pane input is explicitly swallowed by container.

## 8. Deliverables

1. `SbsDualRenderContainer` in app view layer.
2. `BrowserActivity` integration using container-hosted content.
3. Browser SBS state transitions updated for container mode.
4. Fullscreen SBS/HBS path preserved.
5. Validation checklist execution.

---

(End of plan — v3, updated after initial implementation review)
