# Plan: Repeat & Shuffle Controls

## Goal
Add repeat (off / context / track) and shuffle (on/off) toggle buttons to the Player tab.

---

## Spotify API

| Action | Method | Endpoint | Params |
|--------|--------|----------|--------|
| Set repeat mode | PUT | `/v1/me/player/repeat` | `state=off\|context\|track`, `device_id?` |
| Set shuffle | PUT | `/v1/me/player/shuffle` | `state=true\|false`, `device_id?` |
| Read current state | — | already in `GET /v1/me/player` response | `repeat_state`, `shuffle_state` |

**Scope required:** `user-modify-playback-state` — already granted.

---

## What Already Exists

- `PlaybackDto.java` already has `repeat_state` and `shuffle_state` fields (lines 9–11).
- `GET /api/spotify/current-track` polling response already carries these values to the frontend.
- Player controls HTML structure is in `frontend/index.html` around `#prev-btn` / `#next-btn`.
- All playback control endpoints follow the same `PUT`/`POST` pattern in `SpotifyController` + `SpotifyService`.

---

## Implementation Steps

### Step 1 — Backend: Add repeat endpoint

**File:** `backend/src/main/java/com/spotify/wrapper/service/SpotifyService.java`

- Add `setRepeat(String userId, String state, String deviceId)` method.
- Call `PUT https://api.spotify.com/v1/me/player/repeat?state={state}&device_id={deviceId}`.
- Accept `state` values: `off`, `context`, `track`.

**File:** `backend/src/main/java/com/spotify/wrapper/controller/SpotifyController.java`

- Add `PUT /api/spotify/repeat` endpoint.
- Request params: `userId` (required), `state` (required), `deviceId` (optional).
- Return `ResponseEntity<Void>` on success (consistent with seek, volume endpoints).

---

### Step 2 — Backend: Add shuffle endpoint

**File:** `SpotifyService.java`

- Add `setShuffle(String userId, boolean state, String deviceId)` method.
- Call `PUT https://api.spotify.com/v1/me/player/shuffle?state={state}&device_id={deviceId}`.

**File:** `SpotifyController.java`

- Add `PUT /api/spotify/shuffle` endpoint.
- Request params: `userId` (required), `state` (required, boolean), `deviceId` (optional).
- Return `ResponseEntity<Void>`.

---

### Step 3 — Frontend: Register endpoints

**File:** `frontend/js/config.js`

Add to `CONFIG.ENDPOINTS`:
```js
REPEAT:  '/api/spotify/repeat',
SHUFFLE: '/api/spotify/shuffle',
```

---

### Step 4 — Frontend: Add API wrapper methods

**File:** `frontend/js/spotify.js`

- Add `SpotifyAPI.setRepeat(state, deviceId)` — `PUT` to `REPEAT` endpoint.
- Add `SpotifyAPI.setShuffle(state, deviceId)` — `PUT` to `SHUFFLE` endpoint.

Pattern: mirror existing `SpotifyAPI.seek()` / `SpotifyAPI.setVolume()`.

---

### Step 5 — Frontend: Add buttons to Player HTML

**File:** `frontend/index.html`

Place the two buttons in the player controls row alongside `#prev-btn` / `#next-btn`:

```
[ shuffle ]  [ -15s ] [ -10s ] [ -5s ]  [ prev ] [ play/pause ] [ next ]  [ +5s ] [ +10s ] [ +15s ]  [ repeat ]
```

Button IDs and initial state:
- `#shuffle-btn` — icon `fa-shuffle`, active class toggles when shuffle is on.
- `#repeat-btn` — icon cycles: `fa-repeat` (off) → `fa-repeat` with badge/active (context) → `fa-redo` or `fa-repeat` with `1` overlay (track).

Both buttons use `btn btn-outline-secondary btn-sm` consistent with seek buttons. Add `aria-label` and `aria-pressed` for accessibility.

Repeat state visual mapping:
| `repeat_state` | Button appearance |
|---------------|------------------|
| `off` | outline, dim |
| `context` | outline-primary / active |
| `track` | outline-primary / active + small `1` badge overlay |

---

### Step 6 — Frontend: PlayerModule — read state from polling

**File:** `frontend/js/player.js`

In `updateCurrentTrack()` (the polling handler), after the existing response parsing:
- Read `response.repeat_state` and `response.shuffle_state`.
- Store as `this.repeatState` and `this.shuffleState`.
- Call new `updateRepeatButton()` and `updateShuffleButton()` methods to sync button appearance.

---

### Step 7 — Frontend: PlayerModule — add handlers

**File:** `frontend/js/player.js`

Add in `init()`:
```js
$('#repeat-btn').on('click', this.handleRepeat.bind(this));
$('#shuffle-btn').on('click', this.handleShuffle.bind(this));
```

**`handleRepeat()`** — cycles through states:
- `off` → `context` → `track` → `off`
- Calls `SpotifyAPI.setRepeat(nextState, this.deviceId)`.
- On success, updates `this.repeatState` and calls `updateRepeatButton()`.

**`handleShuffle()`** — toggles boolean:
- Calls `SpotifyAPI.setShuffle(!this.shuffleState, this.deviceId)`.
- On success, updates `this.shuffleState` and calls `updateShuffleButton()`.

Both handlers follow the same error handling pattern as `handleNext()` / `handlePrevious()`.

---

### Step 8 — Frontend: Button state helpers

**File:** `frontend/js/player.js`

Add two pure render helpers (no template needed — button state is a CSS class swap):

**`updateRepeatButton()`**
```js
// Sets active class and icon based on this.repeatState ('off' | 'context' | 'track')
```

**`updateShuffleButton()`**
```js
// Toggles active class on #shuffle-btn based on this.shuffleState (bool)
```

---

## Files Changed

| File | Change |
|------|--------|
| `SpotifyService.java` | Add `setRepeat()`, `setShuffle()` |
| `SpotifyController.java` | Add `PUT /repeat`, `PUT /shuffle` |
| `frontend/js/config.js` | Add `REPEAT`, `SHUFFLE` endpoints |
| `frontend/js/spotify.js` | Add `setRepeat()`, `setShuffle()` API wrappers |
| `frontend/index.html` | Add `#repeat-btn`, `#shuffle-btn` in player controls |
| `frontend/js/player.js` | Read state from polling, wire handlers, add button helpers |

No new files. No changes to DTOs (`repeat_state` / `shuffle_state` are already mapped in `PlaybackDto`).

---

## Pre-Implementation Checklist

- [ ] Confirm `repeat_state` and `shuffle_state` are returned in the current `/api/spotify/current-track` response (they are in `PlaybackDto`, but verify the service actually passes them through).
- [ ] Icon-only buttons — this adds two new icon-only buttons. Per the UI guardrail: confirm intent before implementing to avoid icon+text regressions.
- [ ] No new polling needed — repeat/shuffle state rides the existing 15s `current-track` poll.

---

## Out of Scope

- Shuffle state for individual tracks (Spotify only supports shuffle at the context level).
- Persisting repeat/shuffle preference locally — Spotify state is the source of truth.
