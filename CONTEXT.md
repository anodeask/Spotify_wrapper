# Spotify Wrapper - Project Context

> **Last Updated:** April 28, 2026

This document provides a comprehensive overview of the Spotify Wrapper project, including architecture, recent changes, and development notes.

---

## 📋 Table of Contents

1. [Project Overview](#project-overview)
2. [Tech Stack](#tech-stack)
3. [Project Structure](#project-structure)
4. [Backend API Endpoints](#backend-api-endpoints)
5. [Frontend Modules](#frontend-modules)
6. [Database Schema](#database-schema)
7. [Configuration](#configuration)
8. [Recent Changes](#recent-changes)
9. [Known Issues & Solutions](#known-issues--solutions)
10. [Development Notes](#development-notes)

---

## 🎵 Project Overview

A full-stack web application that wraps the Spotify Web API, allowing users to:
- Search for songs, artists, albums, and playlists
- View their personal library (playlists, liked songs, recently played)
- Control playback across connected Spotify devices
- Manage volume and transfer playback between devices

---

## 🛠 Tech Stack

### Backend
| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 17 | Runtime |
| Spring Boot | 3.2.3 | Application Framework |
| Spring Security | - | OAuth Authentication |
| Spring Data JPA | - | Data Access |
| MySQL | 8.0 | Database |
| HikariCP | - | Connection Pooling |
| Apache HttpClient | - | HTTP Client with Connection Pooling |
| Maven | - | Build Tool |

### Frontend
| Technology | Version | Purpose |
|------------|---------|---------|
| jQuery | 3.7.1 | DOM Manipulation |
| Bootstrap | 5.3.2 | UI Framework |
| Handlebars | 4.7.8 | Client-side Templating |
| Font Awesome | 6.5.1 | Icons |

### Servers
- **Backend:** Port `9090` (Spring Boot)
- **Frontend:** Port `3000` (Python HTTP Server / Node.js)

---

## 📁 Project Structure

```
spotify/
├── CONTEXT.md                    # This file
├── README.md                     # Setup instructions
├── QUICK_START.md                # Quick start guide
├── database_setup.sql            # Database setup script
├── application.properties.template
├── start.sh                      # Startup script
│
├── backend/
│   ├── pom.xml                   # Maven dependencies
│   ├── logs/
│   │   └── spotify-wrapper.log
│   └── src/main/
│       ├── java/com/spotify/wrapper/
│       │   ├── SpotifyWrapperApplication.java
│       │   ├── config/
│       │   │   └── SecurityConfig.java
│       │   ├── controller/
│       │   │   ├── AuthController.java
│       │   │   └── SpotifyController.java
│       │   ├── dto/
│       │   │   ├── DevicesDto.java
│       │   │   ├── PlaybackDto.java
│       │   │   ├── QueueDto.java
│       │   │   └── SearchResultDto.java
│       │   ├── entity/
│       │   │   └── User.java
│       │   ├── repository/
│       │   │   └── UserRepository.java
│       │   └── service/
│       │       └── SpotifyService.java
│       └── resources/
│           └── application.properties
│
└── frontend/
    ├── index.html                # Main HTML (single-page app)
    ├── package.json
    ├── server.js                 # Node.js server
    ├── css/
    │   └── styles.css
    └── js/
        ├── app.js                # Main application logic
        ├── auth.js               # Authentication handling
        ├── config.js             # Configuration
        ├── devices.js            # Device management
        ├── library.js            # Library (playlists, liked songs, recent)
        ├── player.js             # Playback controls
        ├── search.js             # Search functionality
        └── spotify.js            # Spotify API wrapper
```

---

## 🔌 Backend API Endpoints

### Authentication (`/api/auth`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/login` | Initiate Spotify OAuth flow |
| GET | `/callback` | OAuth callback handler |
| GET | `/user/{userId}` | Get user info |

### Spotify API (`/api/spotify`)

#### Search
| Method | Endpoint | Parameters | Description |
|--------|----------|------------|-------------|
| GET | `/search` | `userId`, `query`, `type` | Search tracks, artists, albums, playlists |

#### Library
| Method | Endpoint | Parameters | Description |
|--------|----------|------------|-------------|
| GET | `/me/playlists` | `userId`, `limit`, `offset` | Get user's playlists |
| GET | `/me/tracks` | `userId`, `limit`, `offset` | Get user's liked songs |
| GET | `/me/recently-played` | `userId`, `limit` | Get recently played tracks |

#### Devices
| Method | Endpoint | Parameters | Description |
|--------|----------|------------|-------------|
| GET | `/devices` | `userId` | Get available devices |

#### Playback
| Method | Endpoint | Parameters | Description |
|--------|----------|------------|-------------|
| GET | `/playback` | `userId` | Get current playback state |
| GET | `/current-track` | `userId` | Get currently playing track |
| GET | `/queue` | `userId` | Get current playback queue |
| POST | `/play` | `userId`, `deviceId?`, `trackUri?` | Play a track |
| POST | `/play-playlist` | `userId`, `deviceId?`, `contextUri` | Play playlist/album |
| POST | `/pause` | `userId`, `deviceId?` | Pause playback |
| POST | `/next` | `userId`, `deviceId?` | Skip to next track |
| POST | `/previous` | `userId`, `deviceId?` | Go to previous track |
| POST | `/queue/add` | `userId`, `uri`, `deviceId?` | Add item to queue |
| POST | `/transfer` | `userId`, `deviceId` | Transfer playback to device |
| POST | `/volume` | `userId`, `volumePercent`, `deviceId?` | Set volume (0-100) |
| PUT | `/seek` | `userId`, `positionMs`, `deviceId?` | Seek to position in current track |

---

## 🖥 Frontend Modules

### `app.js`
- Main application initialization
- Tab navigation handling
- UI state management

### `auth.js`
- OAuth flow management
- Token storage (localStorage)
- User session handling

### `search.js`
- Search form handling
- Results rendering with Handlebars templates
- Type filtering (track, artist, album, playlist)
- Supports `All` mode (track + album + playlist grouped sections)
- Add-to-queue actions for track results

### `library.js`
- **Recently Played:** Recent tracks with relative timestamps (default sub-tab)
- **Liked Songs:** Paginated liked songs view
- **My Playlists:** Paginated playlist view
- Lazy loading (loads content only when tab is activated)
- Add-to-queue actions for track cards in Liked Songs and Recently Played

### `devices.js`
- Device list rendering
- Device selection for playback
- Transfer playback functionality

### `player.js`
- Playback controls (play, pause, next, previous)
- Seek to position (clickable progress bar with hover tooltip)
- +/- 5, 10, 15 second seek buttons
- Current time and duration labels
- Volume control with step up/down buttons and hover tooltip
- Shared `#player-tooltip` for seek and volume (fixed position, follows cursor)
- Track progress display (updates every 10 seconds)
- Now playing info
- Duplicate call prevention (`isUpdating` guard)
- Current Queue panel rendering and refresh support

### `spotify.js`
- API wrapper for backend calls
- Centralized API functions

---

## 🗄 Database Schema

### `users` Table
| Column | Type | Description |
|--------|------|-------------|
| id | VARCHAR(255) | Primary key (Spotify user ID) |
| display_name | VARCHAR(255) | User's display name |
| email | VARCHAR(255) | User's email |
| access_token | TEXT | Spotify access token |
| refresh_token | TEXT | Spotify refresh token |
| token_expires_at | DATETIME | Token expiration time |
| created_at | DATETIME | Account creation time |
| updated_at | DATETIME | Last update time |

---

## ⚙️ Configuration

### Backend (`application.properties`)
```properties
# Server
server.port=9090

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/spotify_wrapper
spring.datasource.username=<your_username>
spring.datasource.password=<your_password>
spring.datasource.hikari.maximum-pool-size=10

# Spotify API
spotify.client.id=<your_client_id>
spotify.client.secret=<your_client_secret>
spotify.redirect.uri=http://127.0.0.1:9090/callback
```

### Frontend (`config.js`)
```javascript
const Config = {
    API_BASE_URL: 'http://localhost:9090',
    // ...
};
```

---

## 📝 Recent Changes

### April 28, 2026

#### Queue Management + Artist Link UX
- Added backend queue endpoints:
  - `GET /api/spotify/queue`
  - `POST /api/spotify/queue/add`
- Added `QueueDto` for queue response parsing
- Added frontend queue API methods (`getQueue`, `addToQueue`) in `spotify.js`
- Added **Add to Queue** buttons:
  - Search track cards
  - Library liked songs cards
  - Library recently played cards
- Added **Current Queue** panel in Player tab with manual refresh button
- Player module now refreshes queue alongside playback updates
- Artist names are now clickable links to Spotify artist pages in Player/Search/Library track contexts

#### Operational Notes
- Queue actions require an active Spotify device
- Existing scopes already cover queue operations:
  - `user-read-playback-state`
  - `user-modify-playback-state`
- Backend restart required after queue endpoint additions

### April 16, 2026

#### Combined Search: All Mode
- Added an `All` option to the search type selector in `frontend/index.html`
- The search UI now maps `All` to a single backend request with `type=track,album,playlist`
- Search results are rendered in grouped sections for Songs, Albums, and Playlists using the existing result card templates
- Existing single-type search behavior remains unchanged for track, artist, album, and playlist filters

#### Keyboard Shortcut Enhancement: Shift + Arrow Seek
- Added global keyboard seek shortcuts in `player.js` for the active Player tab:
  - `Shift + ArrowRight` seeks forward by 5 seconds
  - `Shift + ArrowLeft` seeks backward by 5 seconds
- This behavior works even when the progress bar is not focused, improving keyboard accessibility and usability.
- Existing keyboard behavior remains unchanged:
  - `Ctrl/Cmd + ArrowRight` for next track
  - `Ctrl/Cmd + ArrowLeft` for previous track
  - Focused progress bar `ArrowRight/ArrowLeft` for ±5 second seek

### April 15, 2026 (Afternoon Update)

#### WCAG Level A Accessibility Compliance
- **Objective:** Achieve WCAG 2.1 Level A accessibility compliance
- **Implementation Areas:**

  **Sidebar Navigation Tabs**
  - Added `role="tablist"` to sidebar nav container
  - Added `role="tab"` to each nav button
  - Added `aria-selected` attribute (true/false) updated dynamically on tab switch
  - Added `aria-controls` linking each tab button to its content panel ID
  - Tab content panels now have `role="tabpanel"` and `aria-labelledby` linking back to their tabs
  - Updated `switchTab()` in `app.js` to handle all ARIA attribute changes

  **Progress Bar (Seek Control)**
  - Added `role="slider"` to progress bar container
  - Added `aria-label="Progress bar"`
  - Added `aria-valuenow` (current position in ms), updated during track playback
  - Added `aria-valuemin="0"`
  - Added `aria-valuemax` (total duration in ms)
  - Added `tabindex="0"` for keyboard accessibility
  - Implemented `handleSeekKeyboard()` for Left/Right arrow keys (±5 second increments)
  - Updated `aria-valuenow` when seeking via keyboard or click

  **Volume Slider**
  - Added `aria-label="Volume"`

  **Icon-Only Buttons**
  - Added `aria-label` to 6 buttons: Search, Previous, Play/Pause, Next, Volume Down, Volume Up
  - Made Play/Pause `aria-label` dynamic ("Play" when paused, "Pause" when playing)
  - Updated dynamically in `updatePlayPauseButton()` as playback state changes

  **Library Sub-Tabs**
  - All library sub-tab panels (Recently Played, Liked Songs, My Playlists) now have:
    - `role="tabpanel"`
    - `aria-labelledby` linking to their tab button by ID

  **Focus Management**
  - Tab content panels receive keyboard focus when activated
  - Implemented via `tabindex="-1"` and `.focus()` call in `switchTab()`
  - Allows screen reader users to navigate between main sections

- **Files Modified:**
  - `frontend/index.html` - Added ARIA roles, labels, and attributes to HTML elements
  - `frontend/js/app.js` - Enhanced `switchTab()` to manage dynamic ARIA attributes and focus
  - `frontend/js/player.js` - Added `handleSeekKeyboard()` method, dynamic play/pause label updates

- **Compliance:**
  - Estimated 90%+ WCAG 2.1 Level A compliance achieved
  - All major accessibility criteria implemented (semantic HTML, ARIA roles, keyboard navigation, focus management)
  - Remaining minor items: Link contrast ratios, form labels (if applicable)

### April 15, 2026

#### Seek Playback Feature
- **Backend:**
  - Added `seek()` method to `SpotifyService` — calls `PUT /me/player/seek?position_ms={ms}` on Spotify API
  - Added `PUT /api/spotify/seek` endpoint to `SpotifyController`
- **Frontend:**
  - Added `SEEK` endpoint to `config.js`
  - Added `SpotifyAPI.seek()` method to `spotify.js`
  - Made progress bar clickable to seek to any position
  - Added current time and duration labels alongside progress bar
  - Added hover tooltip on progress bar showing time at mouse position
  - Added `handleSeek()`, `handleSeekTooltip()`, and `formatTime()` to `PlayerModule`

#### Added +/- Seek Buttons
- Added buttons for -15s, -10s, -5s (left) and +5s, +10s, +15s (right) around playback controls
- Added `handleSeekBySeconds()` handler in `PlayerModule`

#### UI Reordering
- **Sidebar:** Reordered to Player (default) → Search → Library → Devices
- **Library sub-tabs:** Reordered to Recently Played (default) → Liked Songs → My Playlists
- Updated `app.js` default tab from `'search'` to `'player'`
- Updated tab content visibility classes to match new defaults

#### Bug Fix: Recently Played Stuck on Initial Load
- **Problem:** After reordering Library sub-tabs, Recently Played (now the default) never loaded on first visit
- **Cause:** `onTabActivated()` in `library.js` always loaded playlists (old default) and never triggered `loadRecentlyPlayed()` since Bootstrap `shown.bs.tab` doesn't fire for the already-active tab
- **Solution:** Updated `onTabActivated()` to check which sub-tab is actually active and load the correct content

#### CSS: Cursor Pointer Cleanup
- Removed `cursor: pointer` from `.search-result-card` and `.device-card`
- Hand cursor now only appears on actual clickable `<button>` elements

#### Player Update Interval
- Player seeker update interval set to 10 seconds in `config.js`

#### Volume Step Up/Down Buttons
- Added volume down (`-`) and volume up (`+`) buttons flanking the volume slider
- Each click adjusts by `CONFIG.PLAYER.VOLUME_STEP` (2%)
- Reuses existing debounced `handleVolumeChange()` flow

#### Common Tooltip Refactor
- Replaced separate `#seek-tooltip` and `#volume-tooltip` with a single shared `#player-tooltip`
- Uses `position: fixed` with `pageX`/`pageY` so it works consistently for both controls
- Volume tooltip shows value at cursor position (not current slider value)

#### Fixed Duplicate Player Update Calls
- **Problem:** Switching tabs triggered duplicate `updateCurrentTrack()` API calls
- **Cause:** `init()` called both `startUpdateLoop()` and `updateCurrentTrack()` separately; `refresh()` and seek handlers added extra calls outside the loop
- **Solution:**
  - Removed standalone `updateCurrentTrack()` from `init()`
  - Added `isUpdating` guard to prevent concurrent calls
  - `refresh()` now restarts the loop via `startUpdateLoop()` instead of calling `updateCurrentTrack()` directly
  - Seek handlers restart the loop instead of using `setTimeout`

### March 5, 2026

#### Fixed Missing Spotify Scopes for Library Endpoints
- **Problem:** Recently played API returning 403 Forbidden, Liked Songs returning empty
- **Cause:** Missing OAuth scopes in `application.properties`
- **Solution:** Added required scopes to `spotify.scope`:
  - `user-read-recently-played` - for recently played tracks
  - `user-library-read` - for liked songs
  - `playlist-read-collaborative` - for collaborative playlists
- **Action Required:** Users must re-login to Spotify to grant the new permissions

#### Fixed Duplicate Method in spotify.js
- **Problem:** `SpotifyAPI.getLikedSongs is not a function` error
- **Cause:** Duplicate `transferPlayback` method at end of file caused syntax issues
- **Solution:** Removed duplicate method and fixed trailing comma

#### Fixed Merge Conflict in library.js
- **Problem:** `library.js` had duplicate method definitions and malformed code from failed merge
- **Solution:** Recreated the file with clean, properly structured code
- **File:** `/frontend/js/library.js` - completely rewritten

#### Refactored Library.js to Use SpotifyAPI Module
- **Problem:** `library.js` used direct `$.ajax()` calls inconsistent with other modules
- **Solution:** Refactored to use `SpotifyAPI` module for consistent API handling:
  - Added library endpoints to `config.js`: `MY_PLAYLISTS`, `LIKED_SONGS`, `RECENTLY_PLAYED`
  - Added new methods to `spotify.js`: `getMyPlaylists()`, `getLikedSongs()`, `getRecentlyPlayed()`
  - Rewrote `library.js` load methods to use `SpotifyAPI` instead of direct AJAX
  - Changed auth check from `Auth.getUserId()` to `SpotifyAPI.userId`
  - Updated play handlers to use async/await with `SpotifyAPI.playPlaylist()` and `SpotifyAPI.playTrack()`
  - Changed device reference from `Player.getCurrentDeviceId()` to `PlayerModule.deviceId`

#### Bug Fix: Missing `Auth.getUserId()` Method
- **Problem:** `library.js` called `Auth.getUserId()` which didn't exist in `auth.js`
- **Error:** `Unhandled promise rejection: TypeError: Auth.getUserId is not a function`
- **Solution:** Added `getUserId()` method to `Auth` module in `auth.js`:
  - Returns `currentUser.userId` if available
  - Falls back to `localStorage.getItem(CONFIG.STORAGE_KEYS.USER_ID)`

#### Library Feature Implementation
- **Backend:**
  - Added `getMyPlaylists()` method to `SpotifyService`
  - Added `getLikedSongs()` method with `SavedTracksResponse` DTO
  - Added `getRecentlyPlayed()` method with `RecentlyPlayedResponse` DTO
  - Created inner DTOs: `SavedTracksResponse`, `SavedTrackItem`, `RecentlyPlayedResponse`, `CursorsDto`, `PlayHistoryItem`, `ContextDto`
  - Added controller endpoints: `/me/playlists`, `/me/tracks`, `/me/recently-played`

- **Frontend:**
  - Added "My Library" tab in sidebar navigation
  - Created Library sub-tabs: My Playlists, Liked Songs, Recently Played
  - Created `library.js` module with:
    - Lazy loading for each sub-tab
    - Pagination for playlists and liked songs
    - Relative time formatting for recently played
    - Play button integration
  - Added Handlebars templates: `library-playlist-template`, `liked-song-template`, `recently-played-template`
  - Added CSS styles for library tabs and pagination

#### Previous Session Fixes
- Fixed NullPointerException in search (Spotify API returns null items in arrays)
- Added `TracksInfoDto` with `href` and `total` fields
- Fixed volume endpoint to use query params instead of JSON body
- Added Handlebars templating for search results
- Added `uri` field to `PlaylistDto`
- Fixed HTTP Client connection pool exhaustion with `PoolingHttpClientConnectionManager`
- Added `@JsonIgnoreProperties(ignoreUnknown = true)` to all DTOs
- Fixed `@Transactional` proxy issues in services

---

## 🐛 Known Issues & Solutions

### 1. Connection Pool Exhaustion
**Problem:** HTTP connections not being released properly  
**Solution:** Added `PoolingHttpClientConnectionManager` to `SpotifyService`:
```java
PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
cm.setMaxTotal(100);
cm.setDefaultMaxPerRoute(50);
```

### 2. Unknown JSON Properties
**Problem:** Spotify API returns additional fields not in our DTOs  
**Solution:** Added `@JsonIgnoreProperties(ignoreUnknown = true)` to all DTOs

### 3. Null Items in Search Results
**Problem:** Spotify API sometimes returns `null` items in arrays  
**Solution:** Added null filtering in service methods:
```java
.filter(Objects::nonNull)
.collect(Collectors.toList())
```

### 4. HikariCP Connection Issues
**Problem:** Database connections not being properly managed  
**Solution:** Configured HikariCP pool settings and ensured `@Transactional` is on correct methods

---

## 📌 Development Notes

### Starting the Application

1. **Start MySQL database**
2. **Start Backend:**
   ```bash
   cd backend
   mvn spring-boot:run
   ```
3. **Start Frontend:**
   ```bash
   cd frontend
   python3 -m http.server 3000
   # or
   node server.js
   ```

### Testing Endpoints
```bash
# Search
curl "http://localhost:9090/api/spotify/search?userId=<userId>&query=hello&type=track"

# Get Playlists
curl "http://localhost:9090/api/spotify/me/playlists?userId=<userId>&limit=20&offset=0"

# Get Liked Songs
curl "http://localhost:9090/api/spotify/me/tracks?userId=<userId>&limit=20&offset=0"

# Get Recently Played
curl "http://localhost:9090/api/spotify/me/recently-played?userId=<userId>&limit=20"
```

### Important Files to Watch
- `SpotifyService.java` - Core Spotify API integration
- `SpotifyController.java` - REST endpoints
- `library.js` - Library frontend module
- `app.js` - Main application logic

---

## 🔗 Related Resources

- [Spotify Web API Documentation](https://developer.spotify.com/documentation/web-api)
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)
- [Handlebars.js Documentation](https://handlebarsjs.com/guide/)

---

*This document is automatically updated with each code change to maintain an accurate project context.*
