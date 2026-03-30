// Spotify API wrapper module
const SpotifyAPI = {
    userId: null,
    
    // Initialize with user ID
    init(userId) {
        this.userId = userId;
    },
    
    // Generic API request handler
    async makeRequest(endpoint, options = {}) {
        const defaultOptions = {
            headers: {
                'Content-Type': 'application/json',
                'X-User-ID': this.userId
            }
        };
        
        const requestOptions = {
            ...defaultOptions,
            ...options,
            headers: {
                ...defaultOptions.headers,
                ...options.headers
            }
        };
        
        try {
            const response = await $.ajax({
                url: `${CONFIG.API_BASE_URL}${endpoint}`,
                ...requestOptions
            });
            
            return response;
        } catch (error) {
            console.error('API request failed:', error);
            
            if (error.status === 401) {
                throw new Error(CONFIG.ERRORS.UNAUTHORIZED);
            } else if (error.status === 0) {
                throw new Error(CONFIG.ERRORS.NETWORK);
            } else {
                throw new Error(error.responseJSON?.message || CONFIG.ERRORS.GENERIC);
            }
        }
    },
    
    // Search for tracks, artists, albums, or playlists
    async search(query, type = 'track', limit = CONFIG.SEARCH.DEFAULT_LIMIT) {
        const params = new URLSearchParams({
            userId: this.userId,
            query: query,
            type: type,
            limit: limit.toString()
        });
        
        return await this.makeRequest(`${CONFIG.ENDPOINTS.SPOTIFY.SEARCH}?${params}`);
    },
    
    // Get user's available devices
    async getDevices() {
        const params = new URLSearchParams({
            userId: this.userId
        });
        return await this.makeRequest(`${CONFIG.ENDPOINTS.SPOTIFY.DEVICES}?${params}`);
    },
    currentTrackRequest:undefined,
    // Get currently playing track
    async getCurrentTrack() {
		// abort previous request if still pending
		if (this.currentTrackRequest) {
			// this.currentTrackRequest.abort();
            this.currentTrackRequest = undefined;
		}
        const params = new URLSearchParams({
            userId: this.userId
        });
        this.currentTrackRequest = this.makeRequest(`${CONFIG.ENDPOINTS.SPOTIFY.CURRENT_TRACK}?${params}`);
		// Wait for the request to complete
		var response = await this.currentTrackRequest;
		this.currentTrackRequest = undefined; // Clear the request reference
		return response;
    },

    // Play music
    async play(deviceId = null) {
        const params = new URLSearchParams({
            userId: this.userId
        });
        if (deviceId) {
            params.append('deviceId', deviceId);
        }
        
        return await this.makeRequest(`${CONFIG.ENDPOINTS.SPOTIFY.PLAY}?${params}`, {
            method: 'POST'
        });
    },

    // Pause playback
    async pause(deviceId = null) {
        const params = new URLSearchParams({
            userId: this.userId
        });
        if (deviceId) {
            params.append('deviceId', deviceId);
        }
        
        return await this.makeRequest(`${CONFIG.ENDPOINTS.SPOTIFY.PAUSE}?${params}`, {
            method: 'POST'
        });
    },

    // Skip to next track
    async next(deviceId = null) {
        const params = new URLSearchParams({
            userId: this.userId
        });
        if (deviceId) {
            params.append('deviceId', deviceId);
        }
        
        return await this.makeRequest(`${CONFIG.ENDPOINTS.SPOTIFY.NEXT}?${params}`, {
            method: 'POST'
        });
    },

    // Skip to previous track
    async previous(deviceId = null) {
        const params = new URLSearchParams({
            userId: this.userId
        });
        if (deviceId) {
            params.append('deviceId', deviceId);
        }
        
        return await this.makeRequest(`${CONFIG.ENDPOINTS.SPOTIFY.PREVIOUS}?${params}`, {
            method: 'POST'
        });
    },

    // Transfer playback to a specific device
    async transferPlayback(deviceId) {
        const params = new URLSearchParams({
            userId: this.userId,
            deviceId: deviceId
        });
        
        return await this.makeRequest(`${CONFIG.ENDPOINTS.SPOTIFY.TRANSFER}?${params}`, {
            method: 'POST'
        });
    },

    // Set volume
    async setVolume(volume, deviceId = null) {
        const params = new URLSearchParams({
            userId: this.userId,
            volumePercent: Math.round(volume)
        });
        if (deviceId) {
            params.append('deviceId', deviceId);
        }
        
        return await this.makeRequest(`${CONFIG.ENDPOINTS.SPOTIFY.VOLUME}?${params}`, {
            method: 'POST'
        });
    },
    
    // Play a specific track
    async playTrack(trackUri, deviceId = null) {
        const params = new URLSearchParams({
            userId: this.userId,
            trackUri: trackUri
        });
        
        if (deviceId) {
            params.append('deviceId', deviceId);
        }
        
        return await this.makeRequest(`${CONFIG.ENDPOINTS.SPOTIFY.PLAY}?${params}`, {
            method: 'POST'
        });
    },
    
    // Play a playlist or album (context)
    async playPlaylist(contextUri, deviceId = null) {
        const params = new URLSearchParams({
            userId: this.userId,
            contextUri: contextUri
        });
        
        if (deviceId) {
            params.append('deviceId', deviceId);
        }
        
        return await this.makeRequest(`${CONFIG.ENDPOINTS.SPOTIFY.PLAY_PLAYLIST}?${params}`, {
            method: 'POST'
        });
    },
    
    // Get user's playlists
    async getMyPlaylists(limit = 20, offset = 0) {
        const params = new URLSearchParams({
            userId: this.userId,
            limit: limit.toString(),
            offset: offset.toString()
        });
        
        return await this.makeRequest(`${CONFIG.ENDPOINTS.SPOTIFY.MY_PLAYLISTS}?${params}`);
    },
    
    // Get user's liked songs
    async getLikedSongs(limit = 20, offset = 0) {
        const params = new URLSearchParams({
            userId: this.userId,
            limit: limit.toString(),
            offset: offset.toString()
        });
        
        return await this.makeRequest(`${CONFIG.ENDPOINTS.SPOTIFY.LIKED_SONGS}?${params}`);
    },
    
    // Get recently played tracks
    async getRecentlyPlayed(limit = 50, before = null) {
        const params = new URLSearchParams({
            userId: this.userId,
            limit: limit.toString()
        });
        if (before) {
            params.append('before', before);
        }
        
        return await this.makeRequest(`${CONFIG.ENDPOINTS.SPOTIFY.RECENTLY_PLAYED}?${params}`);
    }
};

// Export for global access
window.SpotifyAPI = SpotifyAPI;
