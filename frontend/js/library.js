// Library module for My Playlists, Liked Songs, and Recently Played
const Library = {
    templates: {},
    currentPlaylistsPage: 0,
    currentLikedSongsPage: 0,
    currentPodcastsPage: 0,
    currentSavedEpisodesPage: 0,
    podcastsLimit: 20,
    savedEpisodesLimit: 20,
    playlistsLimit: 20,
    likedSongsLimit: 20,
    recentlyPlayedLimit: 20,
    savedEpisodeIds: new Set(),
    savedEpisodesIndexLoaded: false,
    savedEpisodesIndexLoading: null,
    podcastModal: null,
    podcastModalState: {
        showId: null,
        showName: '',
        imageUrl: '',
        offset: 0,
        total: 0,
        limit: 20
    },
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
        recentlyPlayed: false,
        podcasts: false,
        savedEpisodes: false
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
        const podcastTemplate = document.getElementById('podcast-template');
        const savedPodcastCardTemplate = document.getElementById('saved-podcast-card-template');
        const savedEpisodeCardTemplate = document.getElementById('saved-episode-card-template');
        const podcastEpisodeRowTemplate = document.getElementById('podcast-episode-row-template');

        if (playlistTemplate) {
            this.templates.playlist = Handlebars.compile(playlistTemplate.innerHTML);
        }
        if (likedSongTemplate) {
            this.templates.likedSong = Handlebars.compile(likedSongTemplate.innerHTML);
        }
        if (recentlyPlayedTemplate) {
            this.templates.recentlyPlayed = Handlebars.compile(recentlyPlayedTemplate.innerHTML);
        }
        if (podcastTemplate) {
            this.templates.podcast = Handlebars.compile(podcastTemplate.innerHTML);
        }
        if (savedPodcastCardTemplate) {
            this.templates.savedPodcastCard = Handlebars.compile(savedPodcastCardTemplate.innerHTML);
        }
        if (savedEpisodeCardTemplate) {
            this.templates.savedEpisodeCard = Handlebars.compile(savedEpisodeCardTemplate.innerHTML);
        }
        if (podcastEpisodeRowTemplate) {
            this.templates.podcastEpisodeRow = Handlebars.compile(podcastEpisodeRowTemplate.innerHTML);
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

        $('#podcasts-tab-btn').on('shown.bs.tab', () => {
            if (!this.isLoaded.podcasts) {
                this.loadSavedPodcasts();
            }
        });

        $('#saved-episodes-tab-btn').on('shown.bs.tab', () => {
            if (!this.isLoaded.savedEpisodes) {
                this.loadSavedEpisodes();
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
        $('#playlists-tab-btn, #liked-songs-tab-btn, #podcasts-tab-btn, #saved-episodes-tab-btn').on('shown.bs.tab', () => {
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
        $(document).on('click', '#liked-songs-list .remove-liked-btn', this.handleRemoveLikedSong.bind(this));
        $(document).on('click', '#recently-played-list .save-liked-btn', this.handleSaveLikedSong.bind(this));
        $(document).on('click', '#liked-songs-list .add-queue-btn', this.handleAddToQueue.bind(this));
        $(document).on('click', '#recently-played-list .add-queue-btn', this.handleAddToQueue.bind(this));
        $(document).on('click', '.save-show-btn', this.handleSaveShow.bind(this));
        $(document).on('click', '.remove-show-btn', this.handleRemoveShow.bind(this));
        $(document).on('click', '.view-show-episodes-btn', this.handleViewShowEpisodes.bind(this));
        $(document).on('click', '.play-episode-btn', this.handlePlayEpisode.bind(this));
        $(document).on('click', '.save-episode-btn', this.handleSaveEpisode.bind(this));
        $(document).on('click', '.remove-episode-btn', this.handleRemoveEpisode.bind(this));

        $('#podcast-prev-btn').on('click', () => this.loadPodcastEpisodesPage(this.podcastModalState.offset - this.podcastModalState.limit));
        $('#podcast-next-btn').on('click', () => this.loadPodcastEpisodesPage(this.podcastModalState.offset + this.podcastModalState.limit));

        const podcastModalElement = document.getElementById('podcast-episodes-modal');
        if (podcastModalElement) {
            this.podcastModal = new bootstrap.Modal(podcastModalElement);
        }
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
        } else if ($('#podcasts-tab-btn').hasClass('active')) {
            if (!this.isLoaded.podcasts) {
                this.loadSavedPodcasts();
            }
        } else if ($('#saved-episodes-tab-btn').hasClass('active')) {
            if (!this.isLoaded.savedEpisodes) {
                this.loadSavedEpisodes();
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
            Utils.showFullLoading($container, 'Loading your playlists...');
            $pagination.empty();
            this.playlistsNextOffset = null;
            $('#load-more-playlists-wrapper').remove();
        } else {
            Utils.showLoadMoreLoading($container, 'load-more-playlists-wrapper', 'Loading more...');
        }

        try {
            const response = await SpotifyAPI.getMyPlaylists(this.playlistsLimit, offset);

            this.displayPlaylists(response, $container, isLoadMore);
            this.isLoaded.playlists = true;

        } catch (error) {
            console.error('Error loading playlists:', error);
            $('#load-more-playlists-wrapper').remove();
            if (!isLoadMore) {
                Utils.showSimpleError($container, 'Failed to load playlists. Please try again.');
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
            Utils.showLoadMoreButton($container, 'load-more-playlists', 'load-more-playlists-wrapper', 'Load More');
        } else {
            Utils.showLoadComplete($container, 'load-more-playlists-wrapper', 'All playlists loaded');
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

    async loadSavedPodcasts(offset = 0) {
        if (!SpotifyAPI.userId) return;

        const $container = $('#podcasts-list');
        const $pagination = $('#podcasts-pagination');

        Utils.showFullLoading($container, 'Loading your podcasts...');

        try {
            const response = await SpotifyAPI.getSavedShows(this.podcastsLimit, offset);

            this.currentPodcastsPage = Math.floor(offset / this.podcastsLimit);
            this.displaySavedPodcasts(response, $container, $pagination);
            this.isLoaded.podcasts = true;
        } catch (error) {
            console.error('Error loading podcasts:', error);
            Utils.showSimpleError($container, 'Failed to load podcasts. Please try again.');
        }
    },

    displaySavedPodcasts(data, $container, $pagination) {
        $container.empty();
        $pagination.empty();

        if (!data.items || data.items.length === 0) {
            $container.html(`
                <div class="col-12 text-center text-muted">
                    <i class="fas fa-microphone display-4 mb-3"></i>
                    <p>No podcasts saved yet.</p>
                </div>
            `);
            return;
        }

        data.items.forEach((show) => {
            const imageUrl = show.images && show.images.length > 0
                ? show.images[0].url
                : 'https://via.placeholder.com/64?text=Podcast';
            const totalEpisodes = show.totalEpisodes ?? show.total_episodes ?? 0;
            const publisher = show.publisher || show.name || 'Podcast';

            const templateData = {
                id: show.id || '',
                name: show.name || 'Untitled Podcast',
                publisher,
                totalEpisodes,
                imageUrl: imageUrl
            };

            $container.append(this.templates.savedPodcastCard(templateData));
        });

        this.renderPagination($pagination, data.total || 0, data.offset || 0, this.podcastsLimit, 'podcasts');
    },

    async loadSavedEpisodes(offset = 0) {
        if (!SpotifyAPI.userId) return;

        const $container = $('#saved-episodes-list');
        const $pagination = $('#saved-episodes-pagination');

        Utils.showFullLoading($container, 'Loading your saved episodes...');

        try {
            const response = await SpotifyAPI.getSavedEpisodes(this.savedEpisodesLimit, offset);
            this.currentSavedEpisodesPage = Math.floor(offset / this.savedEpisodesLimit);

            (response.items || []).forEach((episode) => {
                if (episode?.id) {
                    this.savedEpisodeIds.add(episode.id);
                }
            });

            this.displaySavedEpisodes(response, $container, $pagination);
            this.isLoaded.savedEpisodes = true;
        } catch (error) {
            console.error('Error loading saved episodes:', error);
            Utils.showSimpleError($container, 'Failed to load saved episodes. Please try again.');
        }
    },

    async ensureSavedEpisodeIdsLoaded() {
        if (this.savedEpisodesIndexLoaded || !SpotifyAPI.userId) {
            return;
        }

        if (this.savedEpisodesIndexLoading) {
            return this.savedEpisodesIndexLoading;
        }

        this.savedEpisodesIndexLoading = (async () => {
            const pageSize = 50;
            let offset = 0;
            let total = null;

            try {
                do {
                    const response = await SpotifyAPI.getSavedEpisodes(pageSize, offset);
                    total = response.total || 0;

                    (response.items || []).forEach((episode) => {
                        if (episode?.id) {
                            this.savedEpisodeIds.add(episode.id);
                        }
                    });

                    offset += (response.items || []).length;
                } while (total !== null && offset < total);

                this.savedEpisodesIndexLoaded = true;
            } finally {
                this.savedEpisodesIndexLoading = null;
            }
        })();

        return this.savedEpisodesIndexLoading;
    },

    displaySavedEpisodes(data, $container, $pagination) {
        $container.empty();
        $pagination.empty();

        if (!data.items || data.items.length === 0) {
            Utils.showEmptyMessage($container, 'No saved episodes yet.');
            return;
        }

        data.items.forEach((episode) => {
            const imageUrl = this.getEpisodeImageUrl(episode, 'https://via.placeholder.com/64?text=Ep');
            const progressData = this.getEpisodeProgressData(episode);
            const showName = episode.show?.name || 'Podcast';
            const publisher = episode.show?.publisher || showName;
            const releaseDate = this.formatReleaseDate(episode.releaseDate || episode.release_date || '');

            const templateData = {
                id: episode.id || '',
                uri: episode.uri || '',
                episodeName: episode.name || 'Untitled Episode',
                showId: episode.show?.id || '',
                showName,
                publisher,
                releaseDate,
                addedAtLabel: episode.addedAt ? this.formatPlayedAt(episode.addedAt) : '',
                imageUrl: imageUrl,
                showProgress: progressData.showProgress,
                progressPercent: progressData.progressPercent,
                progressLabel: progressData.progressLabel
            };

            $container.append(this.templates.savedEpisodeCard(templateData));
        });

        this.applyEpisodeProgressWidths($container);

        this.renderPagination($pagination, data.total || 0, data.offset || 0, this.savedEpisodesLimit, 'savedEpisodes');
    },

    async handleSaveShow(event) {
        const $button = $(event.currentTarget);
        const showId = $button.data('id');
        const name = $button.data('name');
        const originalHtml = $button.html();

        if (!showId) return;

        $button.prop('disabled', true).html('<i class="fas fa-spinner fa-spin"></i>');

        try {
            const response = await SpotifyAPI.saveShows(showId);
            Utils.showSuccess(response?.message || `Saved to My Podcasts: ${name}`);
            if ($('#podcasts-content').hasClass('active')) {
                await this.loadSavedPodcasts(this.currentPodcastsPage * this.podcastsLimit);
            } else {
                this.isLoaded.podcasts = false;
            }
        } catch (error) {
            console.error('Error saving podcast:', error);
            Utils.showError(error.message || 'Failed to save podcast.');
        } finally {
            $button.prop('disabled', false).html(originalHtml);
        }
    },

    async handleRemoveShow(event) {
        const $button = $(event.currentTarget);
        const showId = $button.data('id');
        const name = $button.data('name');
        const originalHtml = $button.html();

        if (!showId) return;

        $button.prop('disabled', true).html('<i class="fas fa-spinner fa-spin"></i>');

        try {
            const response = await SpotifyAPI.removeShows(showId);
            Utils.showSuccess(response?.message || `Removed from My Podcasts: ${name}`);
            await this.loadSavedPodcasts(this.currentPodcastsPage * this.podcastsLimit);
        } catch (error) {
            console.error('Error removing podcast:', error);
            Utils.showError(error.message || 'Failed to remove podcast.');
        } finally {
            $button.prop('disabled', false).html(originalHtml);
        }
    },

    async handleViewShowEpisodes(event) {
        const $button = $(event.currentTarget);
        const showId = $button.data('id');
        const showName = $button.data('name') || 'Podcast';
        const imageUrl = $button.data('image') || '';

        if (!showId || !this.podcastModal) return;

        this.podcastModalState = {
            showId,
            showName,
            imageUrl,
            offset: 0,
            total: 0,
            limit: 20
        };

        $('#podcast-episodes-modal-label').text(showName);
        $('#podcast-modal-subtitle').text('Loading episodes...');
        $('#podcast-modal-cover').attr('src', imageUrl || 'https://via.placeholder.com/64?text=Podcast');
        Utils.showLoadingMessage('#podcast-episodes-list', 'Loading episodes...');
        this.podcastModal.show();

        await this.loadPodcastEpisodesPage(0);
    },

    async loadPodcastEpisodesPage(offset) {
        const state = this.podcastModalState;
        if (!state.showId) return;

        const safeOffset = Math.max(0, offset);
        state.offset = safeOffset;

        try {
            await this.ensureSavedEpisodeIdsLoaded();
            const response = await SpotifyAPI.getPodcastEpisodes(state.showId, state.limit, safeOffset);
            state.total = response.total || 0;
            state.offset = response.offset || safeOffset;

            this.renderPodcastEpisodes(response.items || []);
            this.updatePodcastModalPagination();
            $('#podcast-modal-subtitle').text(`${state.total} episodes`);
        } catch (error) {
            console.error('Error loading podcast episodes:', error);
            Utils.showErrorMessage('#podcast-episodes-list', 'Failed to load episodes.');
            $('#podcast-page-info').text('');
            $('#podcast-prev-btn, #podcast-next-btn').prop('disabled', true);
        }
    },

    renderPodcastEpisodes(items) {
        const $list = $('#podcast-episodes-list');
        $list.empty();

        if (!items.length) {
            Utils.showEmptyMessage($list, 'No episodes found.');
            return;
        }

        items.forEach((episode, index) => {
            const imageUrl = this.getEpisodeImageUrl(episode, this.podcastModalState.imageUrl || 'https://via.placeholder.com/48?text=Ep');
            const duration = this.formatDuration(episode.durationMs || episode.duration_ms || 0);
            const progressData = this.getEpisodeProgressData(episode);
            const releaseDate = this.formatReleaseDate(episode.releaseDate || episode.release_date || '');

            const templateData = {
                displayIndex: this.podcastModalState.offset + index + 1,
                id: episode.id || '',
                uri: episode.uri || '',
                episodeName: episode.name || 'Untitled Episode',
                imageUrl: imageUrl,
                duration: duration,
                releaseDate,
                isSaved: this.savedEpisodeIds.has(episode.id),
                showProgress: progressData.showProgress,
                progressPercent: progressData.progressPercent,
                progressLabel: progressData.progressLabel
            };

            $list.append(this.templates.podcastEpisodeRow(templateData));
        });

        this.applyEpisodeProgressWidths($list);
    },

    updatePodcastModalPagination() {
        const state = this.podcastModalState;
        const from = state.total === 0 ? 0 : state.offset + 1;
        const to = Math.min(state.offset + state.limit, state.total);

        $('#podcast-page-info').text(state.total === 0 ? 'No episodes' : `Showing ${from}-${to} of ${state.total}`);
        $('#podcast-prev-btn').prop('disabled', state.offset <= 0);
        $('#podcast-next-btn').prop('disabled', state.offset + state.limit >= state.total);
    },

    // Load liked songs
    async loadLikedSongs(offset = 0) {
        if (!SpotifyAPI.userId) return;

        const $container = $('#liked-songs-list');
        const $pagination = $('#liked-songs-pagination');

        Utils.showFullLoading($container, 'Loading your liked songs...');

        try {
            const response = await SpotifyAPI.getLikedSongs(this.likedSongsLimit, offset);

            this.currentLikedSongsPage = Math.floor(offset / this.likedSongsLimit);
            this.displayLikedSongs(response, $container, $pagination);
            this.isLoaded.likedSongs = true;

        } catch (error) {
            console.error('Error loading liked songs:', error);
            Utils.showSimpleError($container, 'Failed to load liked songs. Please try again.');
        }
    },

    displayLikedSongs(data, $container, $pagination) {
        $container.empty();
        $pagination.empty();

        if (!data.items || data.items.length === 0) {
            Utils.showEmptyMessage($container, 'No liked songs yet. Start liking songs in Spotify!');
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
                artistLinks: Utils.formatArtistLinks(track.artists),
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
            Utils.showFullLoading($container, 'Loading recently played...');
            this.recentlyPlayedCursor = null;
            $('#load-more-recently-played-wrapper').remove();
        } else {
            // Show a small loading indicator at the bottom
            Utils.showLoadMoreLoading($container, 'load-more-recently-played-wrapper', 'Loading more...');
        }

        try {
            const response = await SpotifyAPI.getRecentlyPlayed(this.recentlyPlayedLimit, before);

            this.displayRecentlyPlayed(response, $container, isLoadMore);
            this.isLoaded.recentlyPlayed = true;

        } catch (error) {
            console.error('Error loading recently played:', error);
            $('#load-more-recently-played-wrapper').remove();
            if (!isLoadMore) {
                Utils.showSimpleError($container, 'Failed to load recently played. Please try again.');
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
                Utils.showEmptyMessage($container, 'No recently played tracks. Start listening to music!');
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
                artistLinks: Utils.formatArtistLinks(track.artists),
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
            Utils.showLoadComplete($container, 'load-more-recently-played-wrapper', 'All caught up!');
        }
    },

    // Render pagination controls
    renderPagination($container, total, currentOffset, limit, type) {
        const totalPages = Math.ceil(total / limit);
        const currentPage = Math.floor(currentOffset / limit);

        if (totalPages <= 1) return;

        let html = '<div class="pagination-row d-flex flex-wrap justify-content-center align-items-center gap-2">';
        html += '<nav class="m-0"><ul class="pagination mb-0">';

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
        html += '</div>';

        $container.html(html);
        
        const start = currentOffset + 1;
        const end = Math.min(currentOffset + limit, total);
        Utils.showPaginationInfo($container.find('.pagination').after('<div class="pagination-info"></div>').next(), start, end, total);

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
            } else if (pageType === 'podcasts') {
                this.loadSavedPodcasts(offset);
            } else if (pageType === 'savedEpisodes') {
                this.loadSavedEpisodes(offset);
            }
        });
    },

    // Handle play playlist
    async handlePlayPlaylist(e) {
        const $btn = $(e.currentTarget);
        const uri = $btn.data('uri');
        const name = $btn.data('name');
        const originalHtml = $btn.html();

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
            Utils.showError(error.message || 'Failed to play playlist. Make sure you have an active Spotify device.');
        } finally {
            $btn.prop('disabled', false).html(originalHtml);
        }
    },

    // Handle play track
    async handlePlayTrack(e) {
        const $btn = $(e.currentTarget);
        const uri = $btn.data('uri');
        const name = $btn.data('name');
        const originalHtml = $btn.html();

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
            Utils.showError(error.message || 'Failed to play track. Make sure you have an active Spotify device.');
        } finally {
            $btn.prop('disabled', false).html(originalHtml);
        }
    },

    async handleAddToQueue(event) {
        const $button = $(event.currentTarget);
        const uri = $button.data('uri');
        const name = $button.data('name');
        const originalHtml = $button.html();

        if (!uri) {
            console.error('No URI provided for Add to Queue');
            return;
        }

        $button.prop('disabled', true).html('<i class="fas fa-spinner fa-spin"></i>');

        try {
            const deviceId = window.PlayerModule?.deviceId || null;
            await SpotifyAPI.addToQueue(uri, deviceId);
            Utils.showSuccess(`Added to queue: ${name}`);
            if (window.PlayerModule) {
                PlayerModule.updateQueue();
            }
        } catch (error) {
            console.error('Error adding to queue:', error);
            Utils.showError(error.message || 'Failed to add to queue.');
        } finally {
            $button.prop('disabled', false).html(originalHtml);
        }
    },

    async handleSaveLikedSong(event) {
        const $button = $(event.currentTarget);
        const trackId = $button.data('id');
        const name = $button.data('name');
        const originalHtml = $button.html();

        if (!trackId) {
            return;
        }

        $button.prop('disabled', true).html('<i class="fas fa-spinner fa-spin"></i>');

        try {
            const response = await SpotifyAPI.saveLikedSongs(trackId);
            Utils.showSuccess(response?.message || `Saved to Liked Songs: ${name}`);
        } catch (error) {
            console.error('Error saving liked song:', error);
            Utils.showError(error.message || 'Failed to save to liked songs.');
        } finally {
            $button.prop('disabled', false).html(originalHtml);
        }
    },

    async handleRemoveLikedSong(event) {
        const $button = $(event.currentTarget);
        const trackId = $button.data('id');
        const name = $button.data('name');
        const originalHtml = $button.html();

        if (!trackId) {
            return;
        }

        $button.prop('disabled', true).html('<i class="fas fa-spinner fa-spin"></i>');

        try {
            const response = await SpotifyAPI.removeLikedSongs(trackId);
            Utils.showSuccess(response?.message || `Removed from Liked Songs: ${name}`);
            await this.loadLikedSongs(this.currentLikedSongsPage * this.likedSongsLimit);
        } catch (error) {
            console.error('Error removing liked song:', error);
            Utils.showError(error.message || 'Failed to remove from liked songs.');
            $button.prop('disabled', false).html(originalHtml);
        }
    },

    async handlePlayEpisode(event) {
        const $button = $(event.currentTarget);
        const uri = $button.data('uri');
        const name = $button.data('name');
        const originalHtml = $button.html();

        if (!uri) {
            return;
        }

        $button.prop('disabled', true).html('<i class="fas fa-spinner fa-spin"></i>');

        try {
            const deviceId = PlayerModule.deviceId || null;
            await SpotifyAPI.playTrack(uri, deviceId);
            Utils.showSuccess(`Playing episode: ${name}`);
            App.switchTab('player');
        } catch (error) {
            console.error('Error playing episode:', error);
            Utils.showError(error.message || 'Failed to play episode. Make sure you have an active Spotify device.');
        } finally {
            $button.prop('disabled', false).html(originalHtml);
        }
    },

    async handleSaveEpisode(event) {
        const $button = $(event.currentTarget);
        const episodeId = $button.data('id');
        const name = $button.data('name');
        const originalHtml = $button.html();

        if (!episodeId) {
            return;
        }

        $button.prop('disabled', true).html('<i class="fas fa-spinner fa-spin"></i>');

        try {
            const response = await SpotifyAPI.saveEpisodes(episodeId);
            this.savedEpisodeIds.add(episodeId);
            this.isLoaded.savedEpisodes = false;
            Utils.showSuccess(response?.message || `Saved episode: ${name}`);

            if ($('#saved-episodes-content').hasClass('active')) {
                await this.loadSavedEpisodes(this.currentSavedEpisodesPage * this.savedEpisodesLimit);
            }
            if ($('#podcast-episodes-modal').hasClass('show')) {
                await this.loadPodcastEpisodesPage(this.podcastModalState.offset);
            }
        } catch (error) {
            console.error('Error saving episode:', error);
            Utils.showError(error.message || 'Failed to save episode.');
        } finally {
            $button.prop('disabled', false).html(originalHtml);
        }
    },

    async handleRemoveEpisode(event) {
        const $button = $(event.currentTarget);
        const episodeId = $button.data('id');
        const name = $button.data('name');
        const originalHtml = $button.html();

        if (!episodeId) {
            return;
        }

        $button.prop('disabled', true).html('<i class="fas fa-spinner fa-spin"></i>');

        try {
            const response = await SpotifyAPI.removeEpisodes(episodeId);
            this.savedEpisodeIds.delete(episodeId);
            this.isLoaded.savedEpisodes = false;
            Utils.showSuccess(response?.message || `Removed saved episode: ${name}`);

            if ($('#saved-episodes-content').hasClass('active')) {
                await this.loadSavedEpisodes(this.currentSavedEpisodesPage * this.savedEpisodesLimit);
            }
            if ($('#podcast-episodes-modal').hasClass('show')) {
                await this.loadPodcastEpisodesPage(this.podcastModalState.offset);
            }
        } catch (error) {
            console.error('Error removing episode:', error);
            Utils.showError(error.message || 'Failed to remove saved episode.');
        } finally {
            $button.prop('disabled', false).html(originalHtml);
        }
    },

    getEpisodeImageUrl(episode, fallback = 'https://via.placeholder.com/48?text=Ep') {
        const image = episode?.images && episode.images.length > 0
            ? episode.images[0].url
            : episode?.show?.images && episode.show.images.length > 0
                ? episode.show.images[0].url
                : fallback;
        return image;
    },

    getEpisodeProgressData(episode) {
        const durationMs = episode.durationMs || episode.duration_ms || 0;
        const resumePositionMs = episode.resumePoint?.resumePositionMs || episode.resume_point?.resume_position_ms || 0;
        const fullyPlayed = Boolean(episode.resumePoint?.fullyPlayed || episode.resume_point?.fully_played);

        if (!durationMs || (!resumePositionMs && !fullyPlayed)) {
            return {
                showProgress: false,
                progressPercent: 0,
                progressLabel: ''
            };
        }

        const progressPercent = fullyPlayed
            ? 100
            : Math.max(0, Math.min(100, Math.round((resumePositionMs / durationMs) * 100)));
        const progressLabel = fullyPlayed
            ? 'Finished'
            : `${this.formatDuration(resumePositionMs)} of ${this.formatDuration(durationMs)}`;

        return {
            showProgress: true,
            progressPercent,
            progressLabel
        };
    },

    applyEpisodeProgressWidths($scope) {
        $scope.find('.js-episode-progress-bar').each((_, element) => {
            const value = Number($(element).data('progress'));
            const progress = Number.isFinite(value) ? Math.max(0, Math.min(100, value)) : 0;
            element.style.width = `${progress}%`;
        });
    },

    // Delegate to shared Utils duration formatter.
    formatDuration(ms) {
        return Utils.formatTime(ms);
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

    // Format episode release date for UI labels.
    formatReleaseDate(value) {
        if (!value) return '';

        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return String(value);
        }

        return date.toLocaleDateString(undefined, {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        });
    },

    // Reset loaded state (called on logout)
    reset() {
        this.isLoaded = {
            playlists: false,
            likedSongs: false,
            recentlyPlayed: false,
            podcasts: false,
            savedEpisodes: false
        };
        this.currentPlaylistsPage = 0;
        this.currentLikedSongsPage = 0;
        this.currentPodcastsPage = 0;
        this.currentSavedEpisodesPage = 0;
        this.playlistsNextOffset = null;
        this.playlistsLoading = false;
        this.recentlyPlayedCursor = null;
        this.recentlyPlayedLoading = false;
        this.savedEpisodeIds = new Set();
        this.savedEpisodesIndexLoaded = false;
        this.savedEpisodesIndexLoading = null;
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
