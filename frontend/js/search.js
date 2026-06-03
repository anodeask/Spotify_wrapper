// Search module for Spotify content using Handlebars templates
const SearchModule = {
    currentResults: [],
    templates: {},
    
    // Initialize search module
    init() {
        this.compileTemplates();
        this.registerHelpers();
        this.setupDebouncedSearch();
        this.bindEvents();
    },
    
    // Compile Handlebars templates
    compileTemplates() {
        this.templates = {
            track: Handlebars.compile($('#track-template').html()),
            artist: Handlebars.compile($('#artist-template').html()),
            album: Handlebars.compile($('#album-template').html()),
            playlist: Handlebars.compile($('#playlist-template').html()),
            show: Handlebars.compile($('#podcast-template').html()),
            noResults: Handlebars.compile($('#no-results-template').html()),
            error: Handlebars.compile($('#error-template').html())
        };
    },
    
    // Register Handlebars helpers
    registerHelpers() {
        // Truncate helper
        Handlebars.registerHelper('truncate', function(str, length) {
            if (!str) return '';
            if (str.length <= length) return str;
            return str.substring(0, length) + '...';
        });
        
        // Format time helper using shared Utils formatter
        Handlebars.registerHelper('formatTime', function(ms) {
            return Utils.formatTime(ms);
        });
        
        // Format number helper (for followers count)
        Handlebars.registerHelper('formatNumber', function(num) {
            if (!num) return '0';
            return new Intl.NumberFormat().format(num);
        });
    },
    
    // Bind search events
    bindEvents() {
        $('#search-btn').on('click', this.handleSearch.bind(this));
        $('#search-input').on('keypress', (e) => {
            if (e.which === 13) { // Enter key
                this.handleSearch();
            }
        });
        
        // Auto-search as user types (debounced)
        $('#search-input').on('input', this.debouncedSearch.bind(this));
        
        // Search type change
        $('#search-type').on('change', this.handleSearch.bind(this));
        
        // Delegate events for dynamically created elements
        $('#search-results').on('click', '.play-track-btn', this.handlePlayTrack.bind(this));
        $('#search-results').on('click', '.play-playlist-btn', this.handlePlayPlaylist.bind(this));
        $('#search-results').on('click', '.save-liked-btn', this.handleSaveLikedSong.bind(this));
        $('#search-results').on('click', '.add-queue-btn', this.handleAddToQueue.bind(this));
        $('#search-results').on('mouseenter', '.album-track-actions button', this.showActionTooltip.bind(this));
        $('#search-results').on('mousemove', '.album-track-actions button', this.moveActionTooltip.bind(this));
        $('#search-results').on('mouseleave', '.album-track-actions button', this.hideActionTooltip.bind(this));
        $('#search-results').on('focus', '.album-track-actions button', this.showActionTooltip.bind(this));
        $('#search-results').on('blur', '.album-track-actions button', this.hideActionTooltip.bind(this));
        $('#search-results').on('click', '.album-track-actions button', this.hideActionTooltip.bind(this));
    },
    
    // Setup debounced search
    setupDebouncedSearch() {
        this.debouncedSearch = Utils.debounce(() => {
            const query = $('#search-input').val().trim();
            if (query.length > 2) {
                this.performSearch(query);
            }
        }, 500);
    },
    
    // Handle search button click or enter
    handleSearch() {
        const query = $('#search-input').val().trim();
        if (query) {
            this.performSearch(query);
        }
    },

    // Show player-style floating tooltip for album action icons
    showActionTooltip(event) {
        const text = $(event.currentTarget).data('tooltip') || $(event.currentTarget).attr('aria-label') || '';
        if (!text) {
            $('#player-tooltip').hide();
            return;
        }

        const rect = event.currentTarget.getBoundingClientRect();
        const left = event.pageX || (window.scrollX + rect.left + rect.width / 2);
        const top = event.pageY || (window.scrollY + rect.top);

        $('#player-tooltip')
            .text(text)
            .css({ left: left + 'px', top: (top - 30) + 'px' })
            .show();
    },

    // Keep tooltip following cursor similar to player controls
    moveActionTooltip(event) {
        if (!event.pageX || !event.pageY || !$('#player-tooltip').is(':visible')) {
            return;
        }

        $('#player-tooltip').css({ left: event.pageX + 'px', top: (event.pageY - 30) + 'px' });
    },

    hideActionTooltip() {
        $('#player-tooltip').hide();
    },
    
    // Perform the actual search
    async performSearch(query) {
        const selectedType = $('#search-type').val();
        const apiType = selectedType === 'all' ? 'track,album,playlist,show' : selectedType;
        
        this.showLoading(true);
        this.clearResults();
        
        try {
            const results = await SpotifyAPI.search(query, apiType);
            this.currentResults = results;
            this.displayResults(results, selectedType);
        } catch (error) {
            console.error('Search failed:', error);
            this.showError(error.message || CONFIG.ERRORS.SEARCH_FAILED);
        } finally {
            this.showLoading(false);
        }
    },
    
    // Display search results using Handlebars templates
    displayResults(results, type) {
        const $resultsContainer = $('#search-results');

        if (type === 'all') {
            const html = this.buildAllResultsHtml(results);
            $resultsContainer.html(html || this.templates.noResults());
            return;
        }

        const resultsKey = type + 's';
        
        if (!results || !results[resultsKey] || results[resultsKey].items.length === 0) {
            $resultsContainer.html(this.templates.noResults());
            return;
        }
        
        const items = results[resultsKey].items;
        let html = '';
        
        items.forEach(item => {
            const data = this.prepareTemplateData(item, type);
            if(data){
                html += this.templates[type](data);
            }
        });
        
        $resultsContainer.html(html);
    },

    buildAllResultsHtml(results) {
        if (!results) {
            return '';
        }

        const sections = [
            this.renderResultsSection('Songs', 'track', results.tracks?.items || []),
            this.renderResultsSection('Albums', 'album', results.albums?.items || []),
            this.renderResultsSection('Playlists', 'playlist', results.playlists?.items || []),
            this.renderResultsSection('Podcasts', 'show', results.shows?.items || [])
        ].filter(Boolean);

        return sections.join('');
    },

    renderResultsSection(title, type, items) {
        const validItems = items.filter(item => item != null);
        if (validItems.length === 0) {
            return '';
        }

        let cardsHtml = '';
        validItems.forEach(item => {
            const data = this.prepareTemplateData(item, type);
            if (data) {
                cardsHtml += this.templates[type](data);
            }
        });

        if (!cardsHtml) {
            return '';
        }

        return `
            <div class="col-12 mt-2 mb-3">
                <div class="d-flex align-items-center justify-content-between border-bottom pb-2 mb-3">
                    <h4 class="mb-0">${title}</h4>
                    <span class="text-muted small">${validItems.length} result${validItems.length === 1 ? '' : 's'}</span>
                </div>
                <div class="row">
                    ${cardsHtml}
                </div>
            </div>
        `;
    },
    
    // Prepare data for templates based on type
    prepareTemplateData(item, type) {
        if (!item) return undefined;
        switch (type) {
            case 'track':
                return {
                    id: item.id,
                    name: item.name,
                    uri: item.uri,
                    imageUrl: Utils.getImageUrl(item.album?.images, 100),
                    artists: item.artists.map(a => a.name).join(', '),
                    artistLinks: Utils.formatArtistLinks(item.artists),
                    albumName: item.album?.name || '',
                    durationMs: item.duration_ms,
                    duration: Utils.formatTime(item.duration_ms)
                };
                
            case 'artist':
                return {
                    id: item.id,
                    name: item.name,
                    uri: item.uri,
                    imageUrl: Utils.getImageUrl(item.images, 200),
                    followers: item.followers?.total ? 
                        new Intl.NumberFormat().format(item.followers.total) + ' followers' : '',
                    genres: item.genres?.slice(0, 3) || []
                };
                
            case 'album':
                return {
                    id: item.id,
                    name: item.name,
                    uri: item.uri,
                    imageUrl: Utils.getImageUrl(item.images, 150),
                    artists: item.artists.map(a => a.name).join(', '),
                    artistLinks: Utils.formatArtistLinks(item.artists),
                    releaseYear: item.release_date ? new Date(item.release_date).getFullYear() : '',
                    totalTracks: item.total_tracks || 0
                };
                
            case 'playlist':
                return {
                    id: item.id,
                    name: item.name,
                    uri: item.uri,
                    imageUrl: Utils.getImageUrl(item.images, 150),
                    owner: item.owner?.display_name || 'Unknown',
                    totalTracks: item.tracks?.total || 0
                };

            case 'show':
                return {
                    id: item.id,
                    name: item.name,
                    uri: item.uri,
                    imageUrl: Utils.getImageUrl(item.images, 150),
                    publisher: item.publisher || 'Unknown publisher',
                    totalEpisodes: item.total_episodes || item.totalEpisodes || 0
                };
                
            default:
                return item;
        }
    },
    
    // Handle play track button click
    async handlePlayTrack(e) {
        const $btn = $(e.currentTarget);
        const uri = $btn.data('uri');
        const name = $btn.data('name');
        const originalHtml = $btn.html();
        
        $btn.prop('disabled', true).html('<i class="fas fa-spinner fa-spin"></i>');
        
        try {
            await SpotifyAPI.playTrack(uri);
            Utils.showSuccess(`Now playing: ${name}`);
        } catch (error) {
            console.error('Failed to play track:', error);
            Utils.showError(error.message || CONFIG.ERRORS.PLAYBACK_FAILED);
        } finally {
            $btn.prop('disabled', false).html(originalHtml);
        }
    },
    
    // Handle play playlist button click
    async handlePlayPlaylist(e) {
        const $btn = $(e.currentTarget);
        const uri = $btn.data('uri');
        const name = $btn.data('name');
        const originalHtml = $btn.html();
        
        $btn.prop('disabled', true).html('<i class="fas fa-spinner fa-spin"></i>');
        
        try {
            await SpotifyAPI.playPlaylist(uri);
            Utils.showSuccess(`Now playing: ${name}`);
        } catch (error) {
            console.error('Failed to play playlist:', error);
            Utils.showError(error.message || CONFIG.ERRORS.PLAYBACK_FAILED);
        } finally {
            $btn.prop('disabled', false).html(originalHtml);
        }
    },

    // Handle add-to-queue button click
    async handleAddToQueue(e) {
        const $btn = $(e.currentTarget);
        const uri = $btn.data('uri');
        const name = $btn.data('name');
        const type = $btn.data('type') || 'track';
        const id = $btn.data('id');
        const originalHtml = $btn.html();

        if (!uri && !id) {
            return;
        }

        // Show only a spinner icon while loading
        $btn.prop('disabled', true).html('<i class="fas fa-spinner fa-spin"></i>');

        try {
            if (type === 'playlist') {
                Utils.showError('Add to Queue is not supported for playlists.');
                return;
            }

            const deviceId = window.PlayerModule?.deviceId || null;
            const queueInput = id && type ? { uri, id, type } : uri;

            await SpotifyAPI.addToQueue(queueInput, deviceId);
            Utils.showSuccess(`Added to queue: ${name}`);
            if (window.PlayerModule) {
                PlayerModule.updateQueue();
            }
        } catch (error) {
            console.error('Failed to add to queue:', error);
            Utils.showError(error.message || CONFIG.ERRORS.PLAYBACK_FAILED);
        } finally {
            // Restore the button label/icon as originally rendered
            $btn.prop('disabled', false).html(originalHtml);
        }
    },

    async handleSaveLikedSong(e) {
        const $btn = $(e.currentTarget);
        const trackId = $btn.data('id');
        const name = $btn.data('name');
        const originalHtml = $btn.html();

        if (!trackId) {
            return;
        }

        $btn.prop('disabled', true).html('<i class="fas fa-spinner fa-spin"></i>');

        try {
            const response = await SpotifyAPI.saveLikedSongs(trackId);
            Utils.showSuccess(response?.message || `Saved to Liked Songs: ${name}`);
        } catch (error) {
            console.error('Failed to save liked song:', error);
            Utils.showError(error.message || CONFIG.ERRORS.GENERIC);
        } finally {
            $btn.prop('disabled', false).html(originalHtml);
        }
    },
    
    // Show loading state
    showLoading(show) {
        Utils.showLoading('#search-loading', show);
    },
    
    // Clear search results
    clearResults() {
        $('#search-results').empty();
    },
    
    // Show error message using Handlebars template
    showError(message) {
        $('#search-results').html(this.templates.error({ message }));
    }
};

// Export for global access
window.SearchModule = SearchModule;
