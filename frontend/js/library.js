// Library module for My Playlists, Liked Songs, and Recently Played
const Library = {
    templates: {},
    currentPlaylistsPage: 0,
    currentLikedSongsPage: 0,
    playlistsLimit: 20,
    likedSongsLimit: 20,
    recentlyPlayedLimit: 20,
    playlistsNextOffset: null,
    playlistsLoading: false,
    recentlyPlayedCursor: null, // cursor for loading more recently played
    recentlyPlayedLoading: false, // flag to prevent duplicate scroll loads
    recentlyPlayedIntervalId: null,
    scrollHandler: null, // bound scroll handler for cleanup
    RECENTLY_PLAYED_REFRESH_INTERVAL: 60 * 1000, // 1 minute in milliseconds
    isLoaded: {
        playlists: false,
        likedSongs: false,
        recentlyPlayed: false
    },

    init() {
        this.compileTemplates();
        this.bindEvents();
        console.log('Library module initialized');
    },

    compileTemplates() {
        const playlistTemplate = document.getElementById('library-playlist-template');
        const likedSongTemplate = document.getElementById('liked-song-template');
        const recentlyPlayedTemplate = document.getElementById('recently-played-template');

        if (playlistTemplate) {
            this.templates.playlist = Handlebars.compile(playlistTemplate.innerHTML);
        }
        if (likedSongTemplate) {
            this.templates.likedSong = Handlebars.compile(likedSongTemplate.innerHTML);
        }
        if (recentlyPlayedTemplate) {
            this.templates.recentlyPlayed = Handlebars.compile(recentlyPlayedTemplate.innerHTML);
        }

        // Register helpers if not already registered
        if (!Handlebars.helpers.truncate) {
            Handlebars.registerHelper('truncate', function(str, len) {
                if (!str) return '';
                if (str.length <= len) return str;
                return str.substring(0, len) + '...';
            });
        }
    },

    bindEvents() {
        // Tab switch events
        $('#playlists-tab-btn').on('shown.bs.tab', () => {
            if (!this.isLoaded.playlists) {
                this.loadMyPlaylists();
            }
        });

        $('#liked-songs-tab-btn').on('shown.bs.tab', () => {
            if (!this.isLoaded.likedSongs) {
                this.loadLikedSongs();
            }
        });

        $('#recently-played-tab-btn').on('shown.bs.tab', () => {
            if (!this.isLoaded.recentlyPlayed) {
                this.loadRecentlyPlayed();
            }
            this.startRecentlyPlayedAutoRefresh();
            this.bindRecentlyPlayedScroll();
        });

        // Stop auto-refresh and scroll when switching away from recently played tab
        $('#playlists-tab-btn, #liked-songs-tab-btn').on('shown.bs.tab', () => {
            this.stopRecentlyPlayedAutoRefresh();
            this.unbindRecentlyPlayedScroll();
        });

        // Refresh button
        $('#refresh-recently-played').on('click', () => {
            this.isLoaded.recentlyPlayed = false;
            this.recentlyPlayedCursor = null;
            $('#load-more-recently-played-wrapper').remove();
            this.loadRecentlyPlayed();
            // Restart the interval timer after manual refresh
            this.startRecentlyPlayedAutoRefresh();
        });

        // Infinite scroll for recently played
        this.scrollHandler = this.handleRecentlyPlayedScroll.bind(this);

        // Load more playlists
        $(document).on('click', '#load-more-playlists', () => {
            if (this.playlistsNextOffset !== null) {
                this.loadMyPlaylists(this.playlistsNextOffset);
            }
        });

        // Play buttons (delegated)
        $(document).on('click', '#my-playlists-list .play-playlist-btn', this.handlePlayPlaylist.bind(this));
        $(document).on('click', '#liked-songs-list .play-track-btn', this.handlePlayTrack.bind(this));
        $(document).on('click', '#recently-played-list .play-track-btn', this.handlePlayTrack.bind(this));
    },

    // Start auto-refresh for recently played (every 5 minutes)
    startRecentlyPlayedAutoRefresh() {
        // Clear any existing interval first
        this.stopRecentlyPlayedAutoRefresh();
        
        this.recentlyPlayedIntervalId = setInterval(() => {
            console.log('Auto-refreshing recently played...');
            this.loadRecentlyPlayed();
        }, this.RECENTLY_PLAYED_REFRESH_INTERVAL);
        
        console.log('Recently played auto-refresh started (every 5 minutes)');
    },

    // Stop auto-refresh for recently played
    stopRecentlyPlayedAutoRefresh() {
        if (this.recentlyPlayedIntervalId) {
            clearInterval(this.recentlyPlayedIntervalId);
            this.recentlyPlayedIntervalId = null;
            console.log('Recently played auto-refresh stopped');
        }
    },

    // Bind scroll listener for infinite scroll on recently played
    bindRecentlyPlayedScroll() {
        $(window).on('scroll.recentlyPlayed', this.scrollHandler);
    },

    // Unbind scroll listener
    unbindRecentlyPlayedScroll() {
        $(window).off('scroll.recentlyPlayed');
    },

    // Scroll handler — load more when near bottom
    handleRecentlyPlayedScroll() {
        // Only load if: recently played tab is visible, we have a cursor, and not already loading
        if (this.recentlyPlayedLoading || !this.recentlyPlayedCursor) return;
        if (!$('#recently-played-content').hasClass('active')) return;

        const scrollTop = $(window).scrollTop();
        const windowHeight = $(window).height();
        const docHeight = $(document).height();

        // Trigger when within 200px of the bottom
        if (scrollTop + windowHeight >= docHeight - 200) {
            this.loadRecentlyPlayed(this.recentlyPlayedCursor);
        }
    },

    // Called when library tab is activated
    onTabActivated() {
        // Load the active sub-tab content
        if ($('#recently-played-tab-btn').hasClass('active')) {
            if (!this.isLoaded.recentlyPlayed) {
                this.loadRecentlyPlayed();
            }
            this.startRecentlyPlayedAutoRefresh();
            this.bindRecentlyPlayedScroll();
        } else if ($('#liked-songs-tab-btn').hasClass('active')) {
            if (!this.isLoaded.likedSongs) {
                this.loadLikedSongs();
            }
        } else if ($('#playlists-tab-btn').hasClass('active')) {
            if (!this.isLoaded.playlists) {
                this.loadMyPlaylists();
            }
        }
    },

    // Called when library tab is deactivated (user switches to another main tab)
    onTabDeactivated() {
        this.stopRecentlyPlayedAutoRefresh();
        this.unbindRecentlyPlayedScroll();
    },

    // Load user's playlists
    async loadMyPlaylists(offset = 0) {
        if (!SpotifyAPI.userId) return;
        if (this.playlistsLoading) return;

        const $container = $('#my-playlists-list');
        const $pagination = $('#playlists-pagination');
        const isLoadMore = offset > 0;
        this.playlistsLoading = true;

        if (!isLoadMore) {
            // Fresh load — show spinner and reset load-more state.
            $container.html(`
                <div class="col-12 text-center text-muted">
                    <div class="spinner-border text-success" role="status">
                        <span class="visually-hidden">Loading...</span>
                    </div>
                    <p class="mt-2">Loading your playlists...</p>
                </div>
            `);
            $pagination.empty();
            this.playlistsNextOffset = null;
            $('#load-more-playlists-wrapper').remove();
        } else {
            $container.after(`
                <div id="load-more-playlists-wrapper" class="d-flex justify-content-center mt-3 mb-3">
                    <div class="spinner-border spinner-border-sm text-success me-2" role="status"></div>
                    <span class="text-muted">Loading more...</span>
                </div>
            `);
        }

        try {
            const response = await SpotifyAPI.getMyPlaylists(this.playlistsLimit, offset);

            this.displayPlaylists(response, $container, isLoadMore);
            this.isLoaded.playlists = true;

        } catch (error) {
            console.error('Error loading playlists:', error);
            $('#load-more-playlists-wrapper').remove();
            if (!isLoadMore) {
                $container.html(`
                    <div class="col-12">
                        <div class="alert alert-danger">
                            <i class="fas fa-exclamation-triangle me-2"></i>
                            Failed to load playlists. Please try again.
                        </div>
                    </div>
                `);
            }
        } finally {
            this.playlistsLoading = false;
        }
    },

    displayPlaylists(data, $container, isAppend = false) {
        if (!isAppend) {
            $container.empty();
        }

        $('#load-more-playlists-wrapper').remove();

        if (!data.items || data.items.length === 0) {
            if (!isAppend) {
                $container.html(`
                    <div class="col-12 text-center text-muted">
                        <i class="fas fa-list display-4 mb-3"></i>
                        <p>No playlists found. Create some playlists in Spotify!</p>
                    </div>
                `);
            }
            this.playlistsNextOffset = null;
            return;
        }

        data.items.forEach(playlist => {
            const imageUrl = playlist.images && playlist.images.length > 0
                ? playlist.images[0].url
                : 'https://via.placeholder.com/64?text=No+Image';

            const templateData = {
                id: playlist.id,
                name: playlist.name,
                imageUrl: imageUrl,
                totalTracks: playlist.tracks?.total || 0,
                uri: playlist.uri
            };

            $container.append(this.templates.playlist(templateData));
        });

        const nextOffset = this.extractOffsetFromSpotifyUrl(data.next);
        this.playlistsNextOffset = Number.isFinite(nextOffset) ? nextOffset : null;

        if (this.playlistsNextOffset !== null) {
            $container.after(`
                <div id="load-more-playlists-wrapper" class="text-center mt-3 mb-3">
                    <button id="load-more-playlists" class="btn btn-outline-success">
                        <i class="fas fa-plus me-2"></i>Load More
                    </button>
                </div>
            `);
        } else {
            $container.after(`
                <div id="load-more-playlists-wrapper" class="text-center text-muted mt-3 mb-3">
                    <small><i class="fas fa-check-circle me-1"></i>All playlists loaded</small>
                </div>
            `);
        }
    },

    extractOffsetFromSpotifyUrl(url) {
        if (!url) return null;

        try {
            const parsedUrl = new URL(url);
            const offset = parseInt(parsedUrl.searchParams.get('offset'));
            return Number.isNaN(offset) ? null : offset;
        } catch (error) {
            return null;
        }
    },

    // Load liked songs
    async loadLikedSongs(offset = 0) {
        if (!SpotifyAPI.userId) return;

        const $container = $('#liked-songs-list');
        const $pagination = $('#liked-songs-pagination');

        $container.html(`
            <div class="col-12 text-center text-muted">
                <div class="spinner-border text-success" role="status">
                    <span class="visually-hidden">Loading...</span>
                </div>
                <p class="mt-2">Loading your liked songs...</p>
            </div>
        `);

        try {
            const response = await SpotifyAPI.getLikedSongs(this.likedSongsLimit, offset);

            this.currentLikedSongsPage = Math.floor(offset / this.likedSongsLimit);
            this.displayLikedSongs(response, $container, $pagination);
            this.isLoaded.likedSongs = true;

        } catch (error) {
            console.error('Error loading liked songs:', error);
            $container.html(`
                <div class="col-12">
                    <div class="alert alert-danger">
                        <i class="fas fa-exclamation-triangle me-2"></i>
                        Failed to load liked songs. Please try again.
                    </div>
                </div>
            `);
        }
    },

    displayLikedSongs(data, $container, $pagination) {
        $container.empty();
        $pagination.empty();

        if (!data.items || data.items.length === 0) {
            $container.html(`
                <div class="col-12 text-center text-muted">
                    <i class="fas fa-heart display-4 mb-3"></i>
                    <p>No liked songs yet. Start liking songs in Spotify!</p>
                </div>
            `);
            return;
        }

        data.items.forEach(track => {
            const imageUrl = track.album?.images && track.album.images.length > 0
                ? track.album.images[0].url
                : 'https://via.placeholder.com/64?text=No+Image';

            const artists = track.artists?.map(a => a.name).join(', ') || 'Unknown Artist';
            const durationMs = track.durationMs || track.duration_ms || 0;
            const duration = this.formatDuration(durationMs);

            const templateData = {
                id: track.id,
                name: track.name,
                artists: artists,
                albumName: track.album?.name || 'Unknown Album',
                imageUrl: imageUrl,
                uri: track.uri,
                duration: duration,
                durationMs: durationMs
            };

            $container.append(this.templates.likedSong(templateData));
        });

        // Add pagination
        this.renderPagination($pagination, data.total, data.offset, this.likedSongsLimit, 'likedSongs');
    },

    // Load recently played
    async loadRecentlyPlayed(before = null) {
        if (!SpotifyAPI.userId) return;
        if (this.recentlyPlayedLoading) return;

        const $container = $('#recently-played-list');
        const isLoadMore = before !== null;
        this.recentlyPlayedLoading = true;

        if (!isLoadMore) {
            // Fresh load — show spinner
            $container.html(`
                <div class="col-12 text-center text-muted">
                    <div class="spinner-border text-success" role="status">
                        <span class="visually-hidden">Loading...</span>
                    </div>
                    <p class="mt-2">Loading recently played...</p>
                </div>
            `);
            this.recentlyPlayedCursor = null;
            $('#load-more-recently-played-wrapper').remove();
        } else {
            // Show a small loading indicator at the bottom
            $container.after(`
                <div id="load-more-recently-played-wrapper" class="d-flex justify-content-center mt-3 mb-3">
                    <div class="spinner-border spinner-border-sm text-success me-2" role="status"></div>
                    <span class="text-muted">Loading more...</span>
                </div>
            `);
        }

        try {
            const response = await SpotifyAPI.getRecentlyPlayed(this.recentlyPlayedLimit, before);

            this.displayRecentlyPlayed(response, $container, isLoadMore);
            this.isLoaded.recentlyPlayed = true;

        } catch (error) {
            console.error('Error loading recently played:', error);
            $('#load-more-recently-played-wrapper').remove();
            if (!isLoadMore) {
                $container.html(`
                    <div class="col-12">
                        <div class="alert alert-danger">
                            <i class="fas fa-exclamation-triangle me-2"></i>
                            Failed to load recently played. Please try again.
                        </div>
                    </div>
                `);
            }
        } finally {
            this.recentlyPlayedLoading = false;
        }
    },

    displayRecentlyPlayed(data, $container, isAppend = false) {
        if (!isAppend) {
            $container.empty();
        }

        // Remove existing load-more button before appending
        $('#load-more-recently-played-wrapper').remove();

        if (!data.items || data.items.length === 0) {
            if (!isAppend) {
                $container.html(`
                    <div class="col-12 text-center text-muted">
                        <i class="fas fa-history display-4 mb-3"></i>
                        <p>No recently played tracks. Start listening to music!</p>
                    </div>
                `);
            }
            this.recentlyPlayedCursor = null;
            return;
        }

        data.items.forEach(item => {
            const track = item.track;
            if (!track) return;

            const imageUrl = track.album?.images && track.album.images.length > 0
                ? track.album.images[0].url
                : 'https://via.placeholder.com/64?text=No+Image';

            const artists = track.artists?.map(a => a.name).join(', ') || 'Unknown Artist';
            const durationMs = track.durationMs || track.duration_ms || 0;
            const playedAt = this.formatPlayedAt(item.playedAt || item.played_at);

            const templateData = {
                id: track.id,
                name: track.name,
                artists: artists,
                albumName: track.album?.name || 'Unknown Album',
                imageUrl: imageUrl,
                uri: track.uri,
                playedAt: playedAt,
                durationMs: durationMs
            };

            $container.append(this.templates.recentlyPlayed(templateData));
        });

        // Store the cursor for next page (scroll handler will use it)
        const beforeCursor = data.cursors?.before || null;
        this.recentlyPlayedCursor = beforeCursor;

        if (!beforeCursor || data.items.length < this.recentlyPlayedLimit) {
            // No more data — unbind scroll and show end message
            this.recentlyPlayedCursor = null;
            this.unbindRecentlyPlayedScroll();
            $container.after(`
                <div id="load-more-recently-played-wrapper" class="text-center text-muted mt-3 mb-3">
                    <small><i class="fas fa-check-circle me-1"></i>All caught up!</small>
                </div>
            `);
        }
    },

    // Render pagination controls
    renderPagination($container, total, currentOffset, limit, type) {
        const totalPages = Math.ceil(total / limit);
        const currentPage = Math.floor(currentOffset / limit);

        if (totalPages <= 1) return;

        let html = '<nav><ul class="pagination">';

        // Previous button
        html += `<li class="page-item ${currentPage === 0 ? 'disabled' : ''}">
            <a class="page-link" href="#" data-page="${currentPage - 1}" data-type="${type}">Previous</a>
        </li>`;

        // Page numbers (show max 5 pages)
        const startPage = Math.max(0, currentPage - 2);
        const endPage = Math.min(totalPages - 1, startPage + 4);

        for (let i = startPage; i <= endPage; i++) {
            html += `<li class="page-item ${i === currentPage ? 'active' : ''}">
                <a class="page-link" href="#" data-page="${i}" data-type="${type}">${i + 1}</a>
            </li>`;
        }

        // Next button
        html += `<li class="page-item ${currentPage >= totalPages - 1 ? 'disabled' : ''}">
            <a class="page-link" href="#" data-page="${currentPage + 1}" data-type="${type}">Next</a>
        </li>`;

        html += '</ul></nav>';
        html += `<p class="text-muted text-center small">Showing ${currentOffset + 1}-${Math.min(currentOffset + limit, total)} of ${total}</p>`;

        $container.html(html);

        // Bind pagination clicks
        $container.find('.page-link').on('click', (e) => {
            e.preventDefault();
            const $link = $(e.currentTarget);
            if ($link.parent().hasClass('disabled') || $link.parent().hasClass('active')) return;

            const page = parseInt($link.data('page'));
            const pageType = $link.data('type');
            const offset = page * limit;

            if (pageType === 'playlists') {
                this.loadMyPlaylists(offset);
            } else if (pageType === 'likedSongs') {
                this.loadLikedSongs(offset);
            }
        });
    },

    // Handle play playlist
    async handlePlayPlaylist(e) {
        const $btn = $(e.currentTarget);
        const uri = $btn.data('uri');
        const name = $btn.data('name');

        if (!uri) return;

        // Show loading state
        $btn.prop('disabled', true).html('<i class="fas fa-spinner fa-spin"></i>');

        try {
            const deviceId = PlayerModule.deviceId || null;
            await SpotifyAPI.playPlaylist(uri, deviceId);
            console.log('Playing playlist:', name);
            // Switch to player tab
            App.switchTab('player');
        } catch (error) {
            console.error('Error playing playlist:', error);
            alert('Failed to play playlist. Make sure you have an active Spotify device.');
        } finally {
            $btn.prop('disabled', false).html('<i class="fas fa-play me-1"></i>Play');
        }
    },

    // Handle play track
    async handlePlayTrack(e) {
        const $btn = $(e.currentTarget);
        const uri = $btn.data('uri');
        const name = $btn.data('name');

        if (!uri) return;

        // Show loading state
        $btn.prop('disabled', true).html('<i class="fas fa-spinner fa-spin"></i>');

        try {
            const deviceId = PlayerModule.deviceId || null;
            await SpotifyAPI.playTrack(uri, deviceId);
            console.log('Playing track:', name);
            // Switch to player tab
            App.switchTab('player');
        } catch (error) {
            console.error('Error playing track:', error);
            alert('Failed to play track. Make sure you have an active Spotify device.');
        } finally {
            $btn.prop('disabled', false).html('<i class="fas fa-play me-1"></i>Play');
        }
    },

    // Format duration from milliseconds to mm:ss
    formatDuration(ms) {
        if (!ms) return '0:00';
        const minutes = Math.floor(ms / 60000);
        const seconds = Math.floor((ms % 60000) / 1000);
        return `${minutes}:${seconds.toString().padStart(2, '0')}`;
    },

    // Format played_at timestamp to relative time
    formatPlayedAt(timestamp) {
        if (!timestamp) return '';

        const date = new Date(timestamp);
        const now = new Date();
        const diffMs = now - date;
        const diffMins = Math.floor(diffMs / 60000);
        const diffHours = Math.floor(diffMs / 3600000);
        const diffDays = Math.floor(diffMs / 86400000);

        if (diffMins < 1) return 'Just now';
        if (diffMins < 60) return `${diffMins} min ago`;
        if (diffHours < 24) return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
        if (diffDays < 7) return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;

        return date.toLocaleDateString();
    },

    // Reset loaded state (called on logout)
    reset() {
        this.isLoaded = {
            playlists: false,
            likedSongs: false,
            recentlyPlayed: false
        };
        this.currentPlaylistsPage = 0;
        this.currentLikedSongsPage = 0;
        this.playlistsNextOffset = null;
        this.playlistsLoading = false;
        this.recentlyPlayedCursor = null;
        this.recentlyPlayedLoading = false;
        this.stopRecentlyPlayedAutoRefresh();
        this.unbindRecentlyPlayedScroll();
        $('#load-more-playlists-wrapper').remove();
    }
};

// Initialize when document is ready
$(document).ready(() => {
    Library.init();
});

// Export for global access
window.Library = Library;
