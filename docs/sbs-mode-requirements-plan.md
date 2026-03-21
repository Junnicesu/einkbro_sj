# SBS Mode Requirements Plan

This document defines requirements and an implementation plan for Side-by-Side (SBS) modes used by the application (browser viewing in Google Cardboard and fullscreen video playback for both normal 2D and HBS sources).

## 1. Purpose

Provide precise behavior and acceptance criteria for two SBS-related modes:

- `BrowserSbsMirror`: duplicate browser content for left and right eye panes (Cardboard-style).
- `FullscreenVideoSbs`: fullscreen video rendering mode that handles both Normal 2D and HBS (half-side-by-side) sources.

## 2. Definitions

- `BrowserSbsMirror`: Browser content is duplicated to left and right eye panes.
- `FullscreenVideoSbs`: Fullscreen video rendering mode.
- `Normal2D`: A standard non-SBS video source (single full-frame image for both eyes).
- `HBS`: Half-SBS — a frame that contains left and right eye channels side-by-side (left half + right half).

## 3. Requirements

### 3.1 Browser SBS (Cardboard usability)

1. In browser SBS mode, render duplicated browser content for both eyes.
2. Apply inward safe margins so important content is kept away from the lens-edge blur.
3. Use one shared interaction layer (touch/cursor logic remains a single source).
4. No per-eye independent browser controls are required in phase 1.

### 3.2 Fullscreen video behavior

1. On fullscreen enter, switch from `BrowserSbsMirror` to `FullscreenVideoSbs`.
2. Detect source layout and apply the appropriate render policy:
   - If source is `Normal2D`: send the full frame to both eyes.
   - If source is `HBS`: split the frame by half width and map each half to its corresponding eye.
3. In `HBS`, do not mirror the whole frame to both eyes.
4. On fullscreen exit, always return to `BrowserSbsMirror` with inward margins restored.

### 3.3 Detection rules (initial)

1. Primary heuristic: aspect ratio near 2:1 indicates an HBS candidate.
2. Allow manual override in settings if the heuristic is wrong.
3. Keep detection logic isolated for future metadata-based improvements.

## 4. Non-Goals (phase 1)

- Lens distortion correction pipeline
- Independent per-eye interactive browser UI
- Advanced head tracking or stereoscopic scene rendering

## 5. Acceptance Criteria

1. Browser SBS keeps readable content in a centered safe area for both eyes.
2. Fullscreen normal videos appear identical in both eyes.
3. Fullscreen HBS videos show correct left/right channel separation (one channel per eye).
4. Exiting fullscreen reliably restores browser SBS mirror mode.
5. No regression to existing EInkBro fullscreen SBS video playback behavior.

## 6. Implementation Plan

### Phase 1 — Mode state machine

1. Add mode enums:
   - `SbsMode`: `BrowserSbsMirror`, `FullscreenVideoSbs`
   - `VideoLayout`: `Normal2D`, `Hbs`
2. Add a central controller to own mode transitions.

### Phase 2 — Browser SBS renderer

1. Implement inward margin viewport math for both eye panes.
2. Keep shared input mapping from screen coordinates to a single web coordinate space.

### Phase 3 — Fullscreen integration

1. Hook into fullscreen lifecycle (show/hide custom view).
2. On enter fullscreen:
   - classify video layout
   - apply render policy (duplicate full frame or split HBS frame)
3. On exit fullscreen:
   - stop fullscreen SBS renderer
   - restore browser SBS mirror renderer

### Phase 4 — Settings and fallback

1. Add tunables:
   - horizontal inset
   - vertical inset
   - HBS manual override
2. Persist settings and apply at runtime.

### Phase 5 — Validation

1. Create a test matrix including:
   - Browser pages (text-heavy, image-heavy)
   - Normal fullscreen videos
   - HBS fullscreen videos
   - Repeated enter/exit fullscreen sequences
2. Verify there is no incorrect double-channel-in-one-lens output for HBS.

## 7. Risks and Mitigations

- False HBS detection — Mitigation: manual override toggle.
- Device/headset variation — Mitigation: configurable inward margins.
- Fullscreen integration regressions — Mitigation: explicit mode transition tests.

## 8. Deliverables

1. SBS mode state machine implementation
2. Browser inward margin rendering
3. Fullscreen video layout switch (Normal2D vs HBS)
4. Settings UI for inset and layout override
5. Test checklist and pass report

---

(End of plan)
