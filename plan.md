# Plan: Push Spotify Wrapper to GitHub

## Implementation Update (May 2026)

### Liked Tracks Contract and UX Update (June 2026)
- Confirmed runtime path for liked-track save/remove now uses Spotify `PUT/DELETE /v1/me/library?uris=...` (not deprecated `/v1/me/tracks`).
- Added explicit backend JSON responses for liked-track mutation endpoints:
   - `PUT /api/spotify/me/tracks`
   - `DELETE /api/spotify/me/tracks`
- Response payload now consistently includes:
   - `result` (`success` or `failure`)
   - `status` (HTTP status code)
   - `message` (user-facing confirmation or error text)
   - `ids` (requested track ids)
- Frontend updated to consume and surface backend mutation messages in:
   - `frontend/js/spotify.js`
   - `frontend/js/library.js`
   - `frontend/js/search.js`
   - `frontend/js/detail.js`
- Global alert fallback behavior in `frontend/js/config.js` ensures confirmations are visible even if local alert containers are missing.

#### Incidents (June 2026)
- **Runtime drift incident:** Source code pointed to `/v1/me/library` but a stale backend process was still calling `/v1/me/tracks`, causing recurring 403 responses.
- **Contract visibility incident:** Backend returned empty success responses for liked-track mutations, so users did not receive explicit UI confirmation.

#### Learnings (June 2026)
- Always verify active runtime behavior from logs/process state, not just source code, when API behavior and observed results diverge.
- Mutation endpoints should return structured response bodies for both success and failure to keep frontend feedback deterministic.
- Frontend success messaging should prefer server-provided messages and keep a global fallback path when local alert containers are absent.

### OAuth/Token Stability and Secret Hygiene (June 2026)
- Fixed backend token refresh flow to handle Spotify error payloads safely and prevent null-cast 500 failures.
- Fixed OAuth callback token exchange parsing to handle invalid grant/client responses without `NullPointerException`.
- Standardized auth/token failures to meaningful 4xx responses with actionable error messages.
- Contained credential exposure by untracking root `application.properties`, introducing sanitized `application.properties.template`, and strengthening `.gitignore`.
- Added `.gitignore` entries for runtime PID files: `frontend.pid` and `backend.pid`.

#### Incidents (June 2026)
- **Login crash incident:** Callback path returned 500 when Spotify token error payload omitted `expires_in`.
- **Token refresh crash incident:** Refresh path returned 500 for the same null-cast assumption during background API calls.
- **Credential exposure incident:** Spotify client credentials were present in a tracked root properties file.

#### Learnings (June 2026)
- OAuth and refresh token parsing must validate response shape for both success and error payloads.
- Return explicit auth/token failure messages to accelerate debugging and avoid opaque frontend failures.
- Secret exposure response should be immediate and procedural: rotate, untrack, ignore, template, then history cleanup if pushed.

### UI Guardrail Update (May 2026)
- Search, library, and related result-card play/add actions are standardized to **icon-only mode**.
- Developer instruction added: warn the developer before planning any change that modifies icon-only mode behavior.
- Rationale: preserves UI consistency, avoids accidental regressions to icon+text labels, and keeps accessibility review explicit.

### Delivered
- Added album detail view to display album items (tracks) in a modal.
- Added **Tracks** action on album cards in search results.
- Added pagination controls in detail modal for album tracks.
- Added quick actions in detail modal: **Play**, **Add to Queue**, **Play All**.
- Added player-style hover tooltips for album action buttons, with native title fallback.
- Added frontend module integration:
   - `frontend/js/detail.js`
   - detail templates/modal in `frontend/index.html`
   - detail styles in `frontend/css/styles.css`
   - module initialization in `frontend/js/auth.js`

### Scope Decision
- Playlist detail-track fetch implementation was intentionally rolled back.
- Current supported detail view scope: **Albums only**.

### Issues Faced During Process
- **Detail module wiring bug**: event handlers were not firing until the module was correctly exported and initialized.
- **Tooltip behavior mismatch**: album buttons initially used smaller text buttons and then icon-only buttons without reliable hover labels; fixed by using the same floating tooltip style as the player controls and adding a native fallback.
- **Spotify playlist track access (403)**: repeated forbidden responses for playlist-track APIs complicated multi-source detail support.
- **Token/consent troubleshooting overhead**: OAuth scope/consent behavior made playlist-track debugging non-deterministic.
- **Debugging user context mismatch**: API tests with non-persisted user IDs caused misleading user-not-found/internal errors.
- **Repeated backend restarts while testing**: required multiple restart/verification cycles before stabilizing behavior.
- **Live browser validation**: tooltip hover behavior was verified in the running VS Code browser tab after the fix.

### Follow-Up (Optional)
- Add a small in-app info message clarifying that detail view is currently available for albums only.

## Prerequisites
- [ ] GitHub account
- [ ] Git installed on your machine
- [ ] GitHub CLI (optional, but helpful)

---

## Step 1: Create a GitHub Repository

### Option A: Using GitHub Web Interface
1. Go to [github.com](https://github.com)
2. Click the **+** icon in the top right → **New repository**
3. Fill in the details:
   - **Repository name:** `spotify-wrapper`
   - **Description:** `A Spotify API wrapper with Spring Boot backend and jQuery frontend`
   - **Visibility:** Public or Private (your choice)
   - **DO NOT** initialize with README, .gitignore, or license (we already have files)
4. Click **Create repository**

### Option B: Using GitHub CLI
```bash
gh repo create spotify-wrapper --public --description "A Spotify API wrapper with Spring Boot backend and jQuery frontend"
```

---

## Step 2: Initialize Local Git Repository

```bash
# Navigate to project directory
cd /Users/anudeep-6653/Documents/project/spotify

# Initialize git repository (if not already done)
git init

# Check current status
git status
```

---

## Step 3: Create .gitignore File

Create a `.gitignore` file to exclude unnecessary files:

```bash
# This will be created automatically - see Step 4
```

**Important files to ignore:**
- `backend/target/` - compiled Java files
- `backend/logs/` - log files
- `node_modules/` - if any
- `.idea/` or `.vscode/` - IDE settings
- `*.class` - compiled classes
- `application.properties` - contains secrets (use template instead)

---

## Step 4: Secure Sensitive Information

⚠️ **IMPORTANT:** Before pushing, ensure your Spotify credentials are NOT exposed!

1. **Keep the template file** (`application.properties.template`) in git
2. **Add actual config to .gitignore:**
   ```
   backend/src/main/resources/application.properties
   backend/target/classes/application.properties
   ```

---

## Step 5: Stage and Commit Files

```bash
# Add all files
git add .

# Review what will be committed
git status

# Create initial commit
git commit -m "Initial commit: Spotify Wrapper with Spring Boot backend and jQuery frontend

Features:
- Spotify OAuth authentication
- Search for tracks, artists, albums, playlists
- Library: My Playlists, Liked Songs, Recently Played
- Device management and playback control
- Volume controls auto-disable on devices without volume support
- Smooth progress animation: progress bar & time updates every 100ms (local), syncs with backend every 15s
- 429 rate-limit error handling with user-friendly frontend message
- Playback error mapping: 404 when no active device, with user-friendly frontend message
- Playback polling every 15 seconds; queue polling every 30 seconds
- stop.sh script to check and stop backend/frontend servers
- Auto-refresh for Recently Played (every 1 minute)"
```

---

## Step 6: Connect to GitHub and Push

```bash
# Add remote repository (replace YOUR_USERNAME with your GitHub username)
git remote add origin https://github.com/YOUR_USERNAME/spotify-wrapper.git

# Verify remote was added
git remote -v

# Push to GitHub (main branch)
git branch -M main
git push -u origin main
```

---

## Step 7: Verify on GitHub

1. Go to `https://github.com/YOUR_USERNAME/spotify-wrapper`
2. Verify all files are uploaded
3. Check that `application.properties` with secrets is NOT visible
4. Update the README if needed

---

## Quick Command Summary

```bash
# All commands in sequence
cd /Users/anudeep-6653/Documents/project/spotify
git init
git add .
git commit -m "Initial commit: Spotify Wrapper"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/spotify-wrapper.git
git push -u origin main
```

---

## Troubleshooting

### If you accidentally committed secrets:
```bash
# Remove file from git history (keeps local copy)
git rm --cached backend/src/main/resources/application.properties
git commit -m "Remove sensitive config file"
git push
```

### If remote already exists:
```bash
git remote remove origin
git remote add origin https://github.com/YOUR_USERNAME/spotify-wrapper.git
```

### If push is rejected:
```bash
git pull origin main --rebase
git push origin main
```

---

## Next Steps After Push

1. Add collaborators (Settings → Collaborators)
2. Set up branch protection rules
3. Create GitHub Actions for CI/CD (optional)
4. Add badges to README (build status, etc.)

---

## UX & Error Handling Improvements

| Area | Change |
|------|--------|
| Browser `alert()` removed | `handlePlayPlaylist`, `handlePlayTrack`, `handleAddToQueue` in `library.js` now call `Utils.showError()` instead of `alert()` |
| Global alert contrast | Error/success banners use solid opaque dark-red / dark-green backgrounds with white text — visible on the dark app background |
| Spotify API errors | `SpotifyApiException` + `GlobalExceptionHandler` propagate original Spotify status code and message to client; no more generic 500 responses |

---

## Pre-Push Checklist: Polling & Rate Limit Validation

Before pushing any change that touches polling or API call frequency, verify:

- [ ] No `setInterval` / `UPDATE_INTERVAL` is set below **15 000 ms**
- [ ] `QUEUE_UPDATE_INTERVAL` (30 s) remains ≥ `UPDATE_INTERVAL` (15 s)
- [ ] No new polling loop duplicates an endpoint already covered by an existing loop
- [ ] Every polling method is guarded by an `isUpdating`-style flag to prevent overlapping requests
- [ ] Any new polling endpoint is added to `pollingEndpoints` in `app.js → handleAjaxError` for silent failure
- [ ] Backend logs (`backend/logs/spotify-wrapper.log`) show no `429` responses during a normal session
- [ ] Browser DevTools Network tab confirms each `/api/spotify/` endpoint fires at most once per its configured interval
