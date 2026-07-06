# Active Tab Polling Plan

## Objective
Optimize polling efficiency and track-transition UX by:
1. **Pausing polling when tab is inactive** — reduce unnecessary API calls and battery drain
2. **Triggering immediate poll on track completion** — catch next track 2 seconds after current finishes instead of waiting up to 15 seconds

---

## Current State

### Polling Loop (`startUpdateLoop()`)
- **Update interval:** 15 seconds (CONFIG.PLAYER.UPDATE_INTERVAL)
- **Queue update interval:** 30 seconds (CONFIG.PLAYER.QUEUE_UPDATE_INTERVAL)
- **Implementation:** `setTimeout` recursive schedule in `scheduleNextUpdate()`
- **Located in:** [frontend/js/player.js](frontend/js/player.js)

### Track Progress Animation
- **Animation interval:** 100ms local updates (smooth progress bar)
- **Syncs progress with backend:** Every 15 seconds via polling loop

---

## Phase 1: Inactive Tab Detection

### 1.1 Add State Tracking
**Location:** `PlayerModule` object initialization

```javascript
// New properties
isTabActive: true,                    // Track tab visibility state
tabVisibilityHandler: null,          // Store handler for cleanup
lastActiveTime: Date.now(),          // Timestamp of last activity
```

### 1.2 Detect Tab Visibility Changes
**Hook:** Browser `visibilitychange` event

```javascript
// In bindEvents() or new method setupTabVisibilityListener()
document.addEventListener('visibilitychange', () => {
    PlayerModule.isTabActive = !document.hidden;
    
    if (PlayerModule.isTabActive) {
        console.log('Tab is active — resuming polling');
        PlayerModule.lastActiveTime = Date.now();
        // Immediately poll once when tab becomes active
        PlayerModule.updateCurrentTrack();
    } else {
        console.log('Tab is inactive — pausing polling');
    }
});
```

### 1.3 Modify Polling Logic
**Location:** `startUpdateLoop()` → `scheduleNextUpdate()`

**Before:**
```javascript
const scheduleNextUpdate = async () => {
    await this.updateCurrentTrack();
    // ... queue update check ...
    this.updateTimeout = setTimeout(scheduleNextUpdate, CONFIG.PLAYER.UPDATE_INTERVAL);
};
```

**After:**
```javascript
const scheduleNextUpdate = async () => {
    // Only poll if tab is active
    if (this.isTabActive) {
        await this.updateCurrentTrack();
        if (Date.now() - this.lastQueueUpdateAt >= CONFIG.PLAYER.QUEUE_UPDATE_INTERVAL) {
            await this.updateQueue();
        }
    }
    // Always schedule next check (even if tab inactive)
    this.updateTimeout = setTimeout(scheduleNextUpdate, CONFIG.PLAYER.UPDATE_INTERVAL);
};
```

### 1.4 Cleanup on Module Destroy
**New method:** `stopUpdateLoop()`

```javascript
stopUpdateLoop() {
    if (this.updateTimeout) {
        clearTimeout(this.updateTimeout);
        this.updateTimeout = null;
    }
    if (this.tabVisibilityHandler) {
        document.removeEventListener('visibilitychange', this.tabVisibilityHandler);
        this.tabVisibilityHandler = null;
    }
}
```

---

## Phase 2: Track Completion Detection

### 2.1 Add Track Completion State
**Location:** `PlayerModule` object initialization

```javascript
// New properties
trackCompletionTimeout: null,         // Timeout for completion check
estimatedTrackEndTime: null,          // When current track should end
trackCompletionPollScheduled: false,  // Flag to avoid duplicate schedules
```

### 2.2 Calculate Track End Time
**Location:** `displayCurrentTrack(trackData)` method

**Add after progress calculation:**
```javascript
// Calculate when current track will complete
const currentProgressMs = trackData.progress_ms || 0;
const remainingMs = durationMs - currentProgressMs;

if (remainingMs > 0 && remainingMs <= CONFIG.PLAYER.UPDATE_INTERVAL * 1.5) {
    // Track is ending soon; schedule early poll 2 seconds before end
    this.scheduleTrackCompletionPoll(remainingMs);
} else if (remainingMs > CONFIG.PLAYER.UPDATE_INTERVAL * 1.5) {
    // Track is long enough; clear any pending completion poll
    this.clearTrackCompletionPoll();
}
```

### 2.3 Track Completion Poll Handler
**New methods:**

```javascript
scheduleTrackCompletionPoll(remainingMs) {
    // Clear any existing completion timeout
    this.clearTrackCompletionPoll();
    
    // Schedule poll 2 seconds after track ends
    const delayMs = Math.max(0, remainingMs - 2000);
    
    this.trackCompletionTimeout = setTimeout(() => {
        console.log('Track completion poll triggered');
        if (this.isTabActive) {
            this.updateCurrentTrack();
        }
        this.trackCompletionPollScheduled = false;
    }, delayMs);
    
    this.trackCompletionPollScheduled = true;
},

clearTrackCompletionPoll() {
    if (this.trackCompletionTimeout) {
        clearTimeout(this.trackCompletionTimeout);
        this.trackCompletionTimeout = null;
    }
    this.trackCompletionPollScheduled = false;
}
```

### 2.4 Update stopUpdateLoop
**Add cleanup:**

```javascript
stopUpdateLoop() {
    if (this.updateTimeout) {
        clearTimeout(this.updateTimeout);
        this.updateTimeout = null;
    }
    this.clearTrackCompletionPoll();  // NEW
    if (this.tabVisibilityHandler) {
        document.removeEventListener('visibilitychange', this.tabVisibilityHandler);
        this.tabVisibilityHandler = null;
    }
}
```

---

## Integration Timeline

### Step 1: Tab Visibility (Phase 1)
- Add state properties
- Add `visibilitychange` listener in `bindEvents()`
- Modify polling condition in `scheduleNextUpdate()`
- Update `stopUpdateLoop()` to cleanup

### Step 2: Track Completion (Phase 2)
- Add completion state properties
- Add logic to `displayCurrentTrack()` to detect near-end tracks
- Implement `scheduleTrackCompletionPoll()` and `clearTrackCompletionPoll()`
- Test edge cases (skips, seeks, track transitions)

---

## Testing Checklist

### Phase 1 Tests
- [ ] Open app, switch to another tab → no polling requests in DevTools
- [ ] Switch back to Spotify tab → immediate update + resume 15s polling
- [ ] Track progresses smoothly (100ms animation continues even when tab inactive)
- [ ] Close DevTools Network tab before tab goes inactive to verify no requests

### Phase 2 Tests
- [ ] Play a short track (< 20 seconds) → poll triggers 2s after end
- [ ] Seek forward near end → completion poll recalculates correctly
- [ ] Skip to next track → completion poll cancelled, next track's completion poll scheduled
- [ ] Pause near end → completion poll triggers when resumed if time recalculates

---

## Edge Cases & Considerations

| Case | Behavior |
|------|----------|
| Tab becomes inactive while track playing | Polling pauses, progress animation continues locally |
| Tab becomes active before track completes | Immediate poll + resume 15s loop, completion poll still fires if scheduled |
| User seeks forward past end | Completion poll clears if remaining time drops below threshold |
| User pauses playback | Completion poll should clear (since track won't complete) |
| User skips track | Completion poll clears, next track's completion time calculated |
| Network error during completion poll | Logged; next 15s poll will retry |
| Multiple rapid tab visibility changes | Handler is idempotent (checks `isTabActive` guard on each poll) |

---

## Configuration Assumptions

```javascript
// From CONFIG.PLAYER (verify in frontend/js/config.js)
CONFIG.PLAYER.UPDATE_INTERVAL = 15000;        // 15 seconds
CONFIG.PLAYER.QUEUE_UPDATE_INTERVAL = 30000;  // 30 seconds
CONFIG.PLAYER.VOLUME_STEP = 5;                // Volume increment
```

---

## Success Metrics

1. **Polling efficiency:** Baseline API call count → ~0 when tab inactive
2. **Track transition UX:** 2-3 second delay to display next track (vs ~15 seconds currently)
3. **Battery savings:** Reduced background polling on inactive tabs (especially mobile)
4. **No regression:** Local progress animation smooth even when polling paused
