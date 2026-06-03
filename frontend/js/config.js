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
            QUEUE: '/api/spotify/queue',
            ADD_TO_QUEUE: '/api/spotify/queue/add',
            MY_PLAYLISTS: '/api/spotify/me/playlists',
            LIKED_SONGS: '/api/spotify/me/tracks',
            SAVED_EPISODES: '/api/spotify/me/episodes',
            SAVED_SHOWS: '/api/spotify/me/shows',
            RECENTLY_PLAYED: '/api/spotify/me/recently-played',
            ALBUM_TRACKS: '/api/spotify/albums/tracks',
            PODCAST_SEARCH: '/api/spotify/podcasts/search',
            PODCAST_DETAILS: '/api/spotify/podcasts'
        }
    },
    
    // Search configuration
    SEARCH: {
        TYPES: ['all', 'track', 'artist', 'album', 'playlist', 'show'],
        DEFAULT_LIMIT: 10
    },
    
    // Player configuration
    PLAYER: {
        UPDATE_INTERVAL: 15000, // 15 seconds
        QUEUE_UPDATE_INTERVAL: 30000, // 30 seconds
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
        RATE_LIMIT: 'Too many requests. Please slow down and try again in a moment.',
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

    // Shared formatter: m:ss by default, h:mm:ss when duration exceeds 60 minutes.
    formatDurationFromSeconds: (seconds) => {
        if (!seconds) return '0:00';

        const totalSeconds = Math.floor(seconds);
        const hours = Math.floor(totalSeconds / 3600);
        const minutes = Math.floor((totalSeconds % 3600) / 60);
        const remainingSeconds = totalSeconds % 60;

        if (totalSeconds > 3600) {
            return `${hours}:${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}`;
        }

        const totalMinutes = Math.floor(totalSeconds / 60);
        return `${totalMinutes}:${remainingSeconds.toString().padStart(2, '0')}`;
    },

    // Format time in ms via the shared duration formatter.
    formatTime: (ms) => {
        if (!ms) return '0:00';
        return Utils.formatDurationFromSeconds(ms / 1000);
    },

    // Format duration from seconds via the shared duration formatter.
    formatDuration: (seconds) => {
        return Utils.formatDurationFromSeconds(seconds);
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

        if ($container.length === 0) {
            // Fallback to global floating alert when a local container is not present.
            if (window.App && typeof App.showGlobalError === 'function') {
                App.showGlobalError(message);
                return;
            }

            let $globalError = $('#global-error');
            if ($globalError.length === 0) {
                $globalError = $('<div id="global-error" class="position-fixed top-0 start-50 translate-middle-x" style="z-index: 9999; margin-top: 1rem;"></div>');
                $('body').append($globalError);
            }

            $globalError.html(`
                <div class="alert alert-danger alert-dismissible fade show shadow global-floating-alert" role="alert">
                    <i class="fas fa-exclamation-triangle me-2"></i>
                    ${message}
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            `);
            setTimeout(() => {
                $globalError.find('.alert').alert('close');
            }, 5000);
            return;
        }

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

        if ($container.length === 0) {
            // Fallback to global floating alert when a local container is not present.
            if (window.App && typeof App.showGlobalSuccess === 'function') {
                App.showGlobalSuccess(message);
                return;
            }

            let $globalSuccess = $('#global-success');
            if ($globalSuccess.length === 0) {
                $globalSuccess = $('<div id="global-success" class="position-fixed top-0 start-50 translate-middle-x" style="z-index: 9999; margin-top: 1rem;"></div>');
                $('body').append($globalSuccess);
            }

            $globalSuccess.html(`
                <div class="alert alert-success alert-dismissible fade show shadow global-floating-alert" role="alert">
                    <i class="fas fa-check-circle me-2"></i>
                    ${message}
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            `);
            setTimeout(() => {
                $globalSuccess.find('.alert').alert('close');
            }, 3000);
            return;
        }

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
