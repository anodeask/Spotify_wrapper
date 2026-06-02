# Podcast Feature Implementation Plan

> **Last Updated:** June 2, 2026

## 📋 Overview

Add comprehensive podcast support to the Spotify Wrapper, including search, discovery, library management, and episode playback integration.

---

## 🎯 Feature Scope

### Phase 1: Core Podcast Discovery (MVP)
- [x] Podcast search endpoint
- [x] Podcast details & episodes retrieval
- [x] Save/unsave podcasts to user library
- [x] Fetch user's saved podcasts
- [x] Frontend search integration
- [x] Frontend library tab for podcasts

### Phase 2: Episode Management (Enhancement)
- [ ] Save/unsave individual episodes
- [ ] Get user's saved episodes
- [ ] Episode playback controls
- [ ] Episode progress tracking

### Phase 3: Advanced Features (Future)
- [ ] Podcast recommendations
- [ ] New episodes notification
- [ ] Podcast follow notifications
- [ ] Episode history/resume

---

## 🔧 Backend Implementation

### 1. Data Transfer Objects (DTOs)

#### `PodcastDto.java`
```java
package com.spotify.wrapper.dto;

import java.util.List;
import java.util.Map;

public class PodcastDto {
    public String id;
    public String name;
    public String description;
    public String imageUrl;
    public int totalEpisodes;
    public List<String> languages;
    public String publisher;
    public String mediaType;
    public boolean explicit;
    public List<Map<String, Object>> externalUrls;
    public Map<String, Object> href;
    public Map<String, Object> uri;
}
```

#### `EpisodeDto.java`
```java
package com.spotify.wrapper.dto;

import java.util.Map;

public class EpisodeDto {
    public String id;
    public String name;
    public String description;
    public String imageUrl;
    public long durationMs;
    public String releaseDate;
    public boolean explicit;
    public String language;
    public String showId;
    public String showName;
    public int episodeNumber;
    public Map<String, Object> externalUrls;
    public Map<String, Object> href;
    public Map<String, Object> uri;
}
```

#### `SavedPodcastDto.java`
```java
package com.spotify.wrapper.dto;

import java.util.List;

public class SavedPodcastDto {
    public List<PodcastDto> items;
    public int total;
    public int limit;
    public int offset;
    public String next;
    public String previous;
}
```

### 2. SpotifyService Methods

#### Search Podcasts
```java
public SearchResultDto searchPodcasts(String userId, String query, int limit, int offset) throws IOException
```
- Calls: `GET https://api.spotify.com/v1/search?q={query}&type=show&limit={limit}&offset={offset}`
- Returns: SearchResultDto with `shows` array

#### Get Podcast Details
```java
public PodcastDto getPodcastDetails(String userId, String podcastId) throws IOException
```
- Calls: `GET https://api.spotify.com/v1/shows/{id}`
- Returns: PodcastDto with full metadata

#### Get Podcast Episodes
```java
public Map<String, Object> getPodcastEpisodes(String userId, String podcastId, int limit, int offset) throws IOException
```
- Calls: `GET https://api.spotify.com/v1/shows/{id}/episodes?limit={limit}&offset={offset}`
- Returns: Map with `items` (episodes), `total`, `next`, `previous`

#### Get User's Saved Podcasts
```java
public SavedPodcastDto getUserSavedPodcasts(String userId, int limit, int offset) throws IOException
```
- Calls: `GET https://api.spotify.com/v1/me/shows?limit={limit}&offset={offset}`
- Returns: SavedPodcastDto with user's saved shows

#### Save Podcast
```java
public void savePodcast(String userId, String podcastId) throws IOException
```
- Calls: `PUT https://api.spotify.com/v1/me/shows?ids={podcastId}`
- Returns: Success/failure structured response

#### Remove Podcast
```java
public void removePodcast(String userId, String podcastId) throws IOException
```
- Calls: `DELETE https://api.spotify.com/v1/me/shows?ids={podcastId}`
- Returns: Success/failure structured response

#### Check if Podcast is Saved
```java
public boolean isPodcastSaved(String userId, String podcastId) throws IOException
```
- Calls: `GET https://api.spotify.com/v1/me/shows/contains?ids={podcastId}`
- Returns: boolean array

### 3. Controller Endpoints

#### `PodcastController.java`

| Method | Endpoint | Parameters | Returns |
|--------|----------|------------|---------|
| GET | `/api/spotify/podcasts/search` | `userId`, `query`, `limit`, `offset` | SearchResultDto (shows array) |
| GET | `/api/spotify/podcasts/{id}` | `userId`, `podcastId` | PodcastDto |
| GET | `/api/spotify/podcasts/{id}/episodes` | `userId`, `podcastId`, `limit`, `offset` | Episodes list + pagination |
| GET | `/api/spotify/me/shows` | `userId`, `limit`, `offset` | SavedPodcastDto |
| PUT | `/api/spotify/me/shows` | `userId`, `ids` (comma-separated) | `{result, status, message, ids}` |
| DELETE | `/api/spotify/me/shows` | `userId`, `ids` (comma-separated) | `{result, status, message, ids}` |
| GET | `/api/spotify/me/shows/contains` | `userId`, `ids` (comma-separated) | Array of boolean |

---

## 🖥️ Frontend Implementation

### 1. API Wrapper Methods (`spotify.js`)

```javascript
// Search
async searchPodcasts(userId, query, limit = 20, offset = 0)

// Fetch podcast details
async getPodcastDetails(userId, podcastId)

// Fetch episodes for a podcast
async getPodcastEpisodes(userId, podcastId, limit = 20, offset = 0)

// User's saved podcasts
async getSavedPodcasts(userId, limit = 20, offset = 0)

// Save podcast
async savePodcast(userId, podcastId)

// Remove podcast
async removePodcast(userId, podcastId)

// Check if saved
async isPodcastSaved(userId, podcastIds)
```

### 2. Frontend Module (`podcasts.js`)

#### Responsibilities
- Load and display user's saved podcasts with pagination
- Handle save/unsave actions
- Render podcast detail modal with episodes
- Integrate with search results

#### Key Functions
```javascript
const PodcastsModule = {
    currentOffset: 0,
    limit: 20,
    podcastsData: null,

    init() // Initialize module

    loadUserPodcasts(offset = 0) // Fetch and render user's saved podcasts

    renderPodcastList(podcasts) // Render podcast cards

    renderPodcasts(podcasts) // Render podcast item

    handleSavePodcast(event) // Save podcast action

    handleRemovePodcast(event) // Remove podcast action

    showPodcastDetail(podcastId) // Open modal with episodes

    renderPodcastEpisodes(episodes) // Render episodes in modal
}
```

### 3. HTML/Templates

#### Library Tab Addition
```html
<li class="nav-item" role="presentation">
    <button class="nav-link" id="podcasts-tab-btn" data-bs-toggle="tab" 
            data-bs-target="#podcasts-content" type="button" role="tab">
        <i class="fas fa-microphone me-1"></i>My Podcasts
    </button>
</li>

<div class="tab-pane fade" id="podcasts-content" role="tabpanel" 
     aria-labelledby="podcasts-tab-btn">
    <div id="podcasts-list" class="row">
        <!-- Podcast cards rendered here -->
    </div>
    <div id="podcasts-pagination" class="d-flex justify-content-center mt-4"></div>
</div>
```

#### Podcast Card Template (Handlebars)
```handlebars
<div class="col-md-6 col-lg-4 mb-4">
    <div class="card search-result-card">
        <img src="{{imageUrl}}" class="card-img-top search-result-image" alt="{{name}}">
        <div class="card-body">
            <h5 class="card-title">{{name}}</h5>
            <p class="card-text text-muted small">By {{publisher}}</p>
            <p class="card-text text-muted small">{{totalEpisodes}} episodes</p>
            <div class="track-actions">
                <button class="btn btn-outline-success btn-sm save-podcast-btn" 
                        data-id="{{id}}" data-name="{{name}}" title="Add to Library">
                    <i class="fas fa-plus"></i>
                </button>
                <button class="btn btn-outline-info btn-sm view-episodes-btn" 
                        data-id="{{id}}" data-name="{{name}}" title="View Episodes">
                    <i class="fas fa-list"></i>
                </button>
            </div>
        </div>
    </div>
</div>
```

#### Episodes Detail Modal
```html
<div class="modal fade" id="podccastDetailModal" tabindex="-1">
    <div class="modal-dialog modal-lg">
        <div class="modal-content">
            <div class="modal-header">
                <img id="detail-podcast-image" src="" alt="Podcast" 
                     style="max-width: 60px; margin-right: 1rem;">
                <div>
                    <h5 id="detail-podcast-name" class="modal-title"></h5>
                    <p id="detail-podcast-publisher" class="text-muted small"></p>
                </div>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
                <div id="detail-episodes-list" class="list-group">
                    <!-- Episodes rendered here -->
                </div>
            </div>
        </div>
    </div>
</div>
```

### 4. Search Integration

Update `search.js` to include podcasts in All mode:
- Add podcasts to grouped search results
- Use same card template/styling as albums
- Integrate save/unsave actions

---

## 🔐 OAuth Scopes

Verify these scopes are in `application.properties`:
```
user-library-read       # Read saved shows/podcasts
user-library-modify     # Save/remove shows/podcasts
```

If not already present, add to:
```properties
spotify.scope=user-read-private user-read-email playlist-read-private playlist-read-collaborative user-read-playback-state user-modify-playback-state user-library-read user-library-modify user-read-recently-played streaming
```

---

## 📊 Database (Optional)

Currently no DB schema change needed. User podcast saves are managed by Spotify, not locally.

If future requirement arises for tracking podcast preferences:
- Add `podcast_id` and `favorite_flag` to a user-podcast relationship table
- Cache user preference in local DB for quick filtering

---

## 🧪 Testing Checklist

### Backend
- [ ] `mvn test` passes all tests
- [ ] Search returns valid podcast list
- [ ] Save/unsave returns structured response
- [ ] Token refresh handled correctly during API calls
- [ ] Error responses propagated meaningfully

### Frontend
- [ ] Podcasts tab loads and displays user's saved podcasts
- [ ] Pagination controls work for saved podcasts list
- [ ] Save/unsave buttons toggle correctly with server feedback
- [ ] Detail modal shows episodes with pagination
- [ ] Search "All" mode includes podcasts
- [ ] Responsive on mobile/tablet

### Integration
- [ ] Backend and frontend services running
- [ ] No CORS errors
- [ ] Token refresh doesn't break podcast operations
- [ ] Rate limiting respected for podcast search

---

## 🚀 Rollout Plan

### Week 1
- [ ] Implement backend DTOs and SpotifyService methods
- [ ] Create PodcastController endpoints
- [ ] Compile and run `mvn test`

### Week 2
- [ ] Create `podcasts.js` frontend module
- [ ] Add podcast tab to Library UI
- [ ] Integrate save/unsave handlers

### Week 3
- [ ] Integrate podcasts into Search "All" mode
- [ ] Test end-to-end workflows
- [ ] Verify pagination and error handling

### Week 4
- [ ] Manual browser validation
- [ ] Performance checks
- [ ] Documentation updates (README, CONTEXT)
- [ ] Deploy and monitor

---

## 📝 Documentation Updates

After implementation, update:
- [README.md](README.md) — Add podcast features to feature list
- [CONTEXT.md](CONTEXT.md) — Document new endpoints and DTOs
- [AGENTS.md](AGENTS.md) — Add podcast endpoints to code map
- [QUICK_START.md](QUICK_START.md) — Add podcast usage example (if applicable)

---

## 🔗 Spotify API References

- [Podcasts (Shows) Endpoint](https://developer.spotify.com/documentation/web-api/reference/search)
- [Get Show Episodes](https://developer.spotify.com/documentation/web-api/reference/get-show-episodes)
- [Save Shows to Library](https://developer.spotify.com/documentation/web-api/reference/save-shows-user)
- [Get User's Saved Shows](https://developer.spotify.com/documentation/web-api/reference/get-saved-shows)

---

## 📌 Known Constraints

- **Episode Playback**: Spotify Web API does not provide direct episode playback URIs. Episodes are queued via show context only.
- **Live Feeds**: Podcast follow notifications require webhook infrastructure (not in scope for now).
- **Offline**: Episodes cannot be downloaded for offline listening via standard Web API.
- **New Episode Detection**: Requires polling `/me/shows` to detect new episodes; no push notification support.

---

## ✅ Success Criteria

- Users can search for podcasts
- Users can save/unsave podcasts to their library
- Users can view their saved podcasts with pagination
- Users can see episodes for any podcast
- Save/unsave actions return clear success/failure messages
- No regressions in existing features (search, library, playback)
- All endpoints follow established error-handling patterns
- Documentation is current and accurate
