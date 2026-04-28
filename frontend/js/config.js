// Configuration for the Spotify Wrapper application
const CONFIG = {
    // API base URL - points to our Spring Boot backend
    API_BASE_URL: 'http://127.0.0.1:9090',
    
    // Local storage keys
    STORAGE_KEYS: {
        USER_ID: 'spotifyUserId',
        ACCESS_TOKEN: 'spotifyAccessToken',
        USER_DATA: 'spotifyUserData'
    },
    
    // API endpoints
    ENDPOINTS: {
        AUTH: {
            LOGIN: '/auth/login',
            CALLBACK: '/auth/callback',
            USER: '/auth/user',
            LOGOUT: '/auth/logout'
        },
        SPOTIFY: {
            SEARCH: '/api/spotify/search',
            DEVICES: '/api/spotify/devices',
            CURRENT_TRACK: '/api/spotify/current-track',
            PLAY: '/api/spotify/play',
            PAUSE: '/api/spotify/pause',
            NEXT: '/api/spotify/next',
            PREVIOUS: '/api/spotify/previous',
            VOLUME: '/api/spotify/volume',
            PLAY_TRACK: '/api/spotify/play-track',
            PLAY_PLAYLIST: '/api/spotify/play-playlist',
            TRANSFER: '/api/spotify/transfer',
            SEEK: '/api/spotify/seek',
            MY_PLAYLISTS: '/api/spotify/me/playlists',
            LIKED_SONGS: '/api/spotify/me/tracks',
            RECENTLY_PLAYED: '/api/spotify/me/recently-played'
        }
    },
    
    // Search configuration
    SEARCH: {
        TYPES: ['all', 'track', 'artist', 'album', 'playlist'],
        DEFAULT_LIMIT: 10
    },
    
    // Player configuration
    PLAYER: {
        UPDATE_INTERVAL: 10000, // 10 seconds
        VOLUME_STEP: 2
    },
    
    // Device refresh interval
    DEVICE_REFRESH_INTERVAL: 30000, // 30 seconds

    // Error messages
    ERRORS: {
        NETWORK: 'Network error. Please check your connection.',
        UNAUTHORIZED: 'Please login to Spotify first.',
        PLAYBACK_FAILED: 'Failed to control playback. Make sure Spotify is active on a device.',
        SEARCH_FAILED: 'Search failed. Please try again.',
        GENERIC: 'Something went wrong. Please try again.'
    }
};

// Utility functions
const Utils = {
    escapeHtml: (value) => {
        if (value == null) return '';
        return String(value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    },

    buildSpotifyArtistUrl: (artist) => {
        if (!artist) return '';
        if (artist.external_urls?.spotify) return artist.external_urls.spotify;
        if (artist.externalUrls?.spotify) return artist.externalUrls.spotify;
        if (artist.id) return `https://open.spotify.com/artist/${encodeURIComponent(artist.id)}`;
        return '';
    },

    formatArtistLinks: (artists) => {
        if (!Array.isArray(artists) || artists.length === 0) {
            return 'Unknown Artist';
        }

        return artists.map((artist) => {
            const name = Utils.escapeHtml(artist?.name || 'Unknown Artist');
            const url = Utils.buildSpotifyArtistUrl(artist);

            if (!url) {
                return name;
            }

            return `<a href="${Utils.escapeHtml(url)}" target="_blank" rel="noopener noreferrer" class="link-secondary artist-link">${name}</a>`;
        }).join(', ');
    },

    // Format time in ms to mm:ss
    formatTime: (ms) => {
        const minutes = Math.floor(ms / 60000);
        const seconds = ((ms % 60000) / 1000).toFixed(0);
        return minutes + ":" + (seconds < 10 ? '0' : '') + seconds;
    },
    
    // Format duration from seconds to readable format
    formatDuration: (seconds) => {
        const hours = Math.floor(seconds / 3600);
        const minutes = Math.floor((seconds % 3600) / 60);
        const remainingSeconds = seconds % 60;
        
        if (hours > 0) {
            return `${hours}:${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}`;
        } else {
            return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`;
        }
    },
    
    // Debounce function for search
    debounce: (func, wait) => {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    },
    
    // Show/hide loading state
    showLoading: (element, show = true) => {
        const $element = $(element);
        if (show) {
            $element.removeClass('d-none');
        } else {
            $element.addClass('d-none');
        }
    },
    
    // Show error message
    showError: (message, container = '#error-container') => {
        const $container = $(container);
        $container.html(`
            <div class="alert alert-danger alert-dismissible fade show" role="alert">
                <i class="fas fa-exclamation-triangle me-2"></i>
                ${message}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        `);
        $container.removeClass('d-none');
    },
    
    // Show success message
    showSuccess: (message, container = '#success-container') => {
        const $container = $(container);
        $container.html(`
            <div class="alert alert-success alert-dismissible fade show" role="alert">
                <i class="fas fa-check-circle me-2"></i>
                ${message}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        `);
        $container.removeClass('d-none');
    },
    
    // Truncate text
    truncateText: (text, maxLength = 50) => {
        if (text.length <= maxLength) return text;
        return text.substring(0, maxLength) + '...';
    },
    
    // Get image URL with fallback
    getImageUrl: (images, preferredSize = 300) => {
        if (!images || images.length === 0) {
            return '/images/placeholder.png';
        }
        
        // Find closest size to preferred
        let bestImage = images[0];
        let bestDiff = Math.abs(bestImage.width - preferredSize);
        
        for (const image of images) {
            const diff = Math.abs(image.width - preferredSize);
            if (diff < bestDiff) {
                bestImage = image;
                bestDiff = diff;
            }
        }
        
        return bestImage.url;
    }
};

// Export for use in other files
window.CONFIG = CONFIG;
window.Utils = Utils;
