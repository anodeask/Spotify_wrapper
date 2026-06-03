// DetailView module — shows track list for albums in a modal
const DetailView = {
    templates: {},
    currentSourceId: null,
    currentSourceType: 'album',
    currentOffset: 0,
    pageSize: 20,
    totalTracks: 0,
    isLoading: false,
    modalInstance: null,

    init() {
        this.compileTemplates();
        this.bindEvents();
        console.log('DetailView module initialized');
    },

    compileTemplates() {
        const trackRowTpl = document.getElementById('detail-track-row-template');
        if (trackRowTpl) {
            this.templates.trackRow = Handlebars.compile(trackRowTpl.innerHTML);
        }
    },

    bindEvents() {
        // Play button inside the modal (delegated)
        $(document).on('click', '#detail-track-list .play-track-btn', this.handlePlayTrack.bind(this));
        $(document).on('click', '#detail-track-list .save-liked-btn', this.handleSaveLikedSong.bind(this));
        $(document).on('click', '#detail-track-list .add-queue-btn', this.handleAddToQueue.bind(this));

        // Pagination
        $(document).on('click', '#detail-prev-btn', () => {
            if (this.currentOffset > 0) {
                this.loadTracks(this.currentSourceType, this.currentSourceId, this.currentOffset - this.pageSize);
            }
        });
        $(document).on('click', '#detail-next-btn', () => {
            if (this.currentOffset + this.pageSize < this.totalTracks) {
                this.loadTracks(this.currentSourceType, this.currentSourceId, this.currentOffset + this.pageSize);
            }
        });

        // Play all button
        $(document).on('click', '#detail-play-all-btn', this.handlePlayAll.bind(this));

        // Open listener on album cards (delegated from document)
        $(document).on('click', '.view-tracks-btn', (e) => {
            const $btn = $(e.currentTarget);
            const id = $btn.data('id');
            const type = $btn.data('type');
            const name = $btn.data('name');
            const imageUrl = $btn.data('image') || '';
            const uri = $btn.data('uri') || '';
            this.open(type, id, name, imageUrl, uri);
        });
    },

    open(type, id, name, imageUrl, uri) {
        if (!id || !type) return;

        $('#detail-modal-title').text(name || 'Tracks');
        $('#detail-modal-cover').attr('src', imageUrl || 'https://via.placeholder.com/80?text=No+Image').attr('alt', name);
        $('#detail-modal-subtitle').text(type === 'album' ? 'Album' : 'Item');
        $('#detail-play-all-btn').data('uri', uri).data('type', type);

        this.currentSourceId = id;
        this.currentSourceType = type;
        this.currentOffset = 0;
        this.totalTracks = 0;

        const modalEl = document.getElementById('detail-modal');
        if (modalEl) {
            this.modalInstance = bootstrap.Modal.getOrCreateInstance(modalEl);
            this.modalInstance.show();
        }

        this.loadTracks(type, id, 0);
    },

    async loadTracks(type, id, offset) {
        if (this.isLoading) return;
        this.isLoading = true;
        this.currentOffset = offset;

        const $list = $('#detail-track-list');
        $list.html(`
            <div class="text-center text-muted py-4">
                <div class="spinner-border spinner-border-sm text-success me-2" role="status"></div>
                Loading tracks…
            </div>
        `);
        this.updatePaginationButtons(false, false);

        try {
            let response;
            if (type === 'album') {
                response = await SpotifyAPI.getAlbumTracks(id, this.pageSize, offset);
            } else {
                throw new Error('Unsupported track source type: ' + type);
            }

            this.renderTracks(response.items || []);
            this.totalTracks = response.total || 0;
            this.updatePaginationButtons(
                offset > 0,
                offset + this.pageSize < this.totalTracks
            );
        } catch (error) {
            console.error('Failed to load tracks:', error);
            Utils.showErrorMessage('#detail-track-list', `Failed to load tracks: ${error.message || 'Unknown error'}`);
        } finally {
            this.isLoading = false;
        }
    },

    renderTracks(tracks) {
        const $list = $('#detail-track-list');
        $list.empty();

        if (!tracks || tracks.length === 0) {
            Utils.showEmptyMessage($list, 'No tracks found');
            return;
        }

        tracks.forEach((track, index) => {
            const trackNum = this.currentOffset + index + 1;
            const html = this.templates.trackRow({
                number: trackNum,
                id: track.id,
                name: track.name,
                artists: track.artists && track.artists.length > 0
                    ? track.artists.map(a => a.name).join(', ')
                    : 'Unknown Artist',
                duration: this.formatDuration(track.duration_ms),
                imageUrl: track.album && track.album.images && track.album.images[0]
                    ? track.album.images[0].url
                    : 'https://via.placeholder.com/40?text=No+Image',
                uri: track.uri
            });
            $list.append(html);
        });

        $('#detail-page-info').text(
            `${Math.min(this.currentOffset + this.pageSize, this.totalTracks)} / ${this.totalTracks}`
        );
    },

    updatePaginationButtons(showPrev, showNext) {
        $('#detail-prev-btn').prop('disabled', !showPrev);
        $('#detail-next-btn').prop('disabled', !showNext);
    },

    formatDuration(ms) {
        return Utils.formatTime(ms);
    },

    handlePlayTrack(e) {
        const $btn = $(e.currentTarget);
        const uri = $btn.data('uri');
        const name = $btn.data('name');

        if (!uri) {
            alert('Track URI is not available');
            return;
        }

        window.PlayerModule?.playTrack(uri, name);
    },

    async handleSaveLikedSong(e) {
        const $btn = $(e.currentTarget);
        const trackId = $btn.data('id');
        const name = $btn.data('name');
        const originalHtml = $btn.html();

        if (!trackId) {
            alert('Track id is not available');
            return;
        }

        $btn.prop('disabled', true).html('<i class="fas fa-spinner fa-spin"></i>');

        try {
            const response = await SpotifyAPI.saveLikedSongs(trackId);
            if (window.Utils?.showSuccess) {
                Utils.showSuccess(response?.message || `Saved to Liked Songs: ${name}`);
            }
        } catch (error) {
            console.error('Error saving liked song:', error);
            if (window.Utils?.showError) {
                Utils.showError(error.message || 'Failed to save to liked songs.');
            }
        } finally {
            $btn.prop('disabled', false).html(originalHtml);
        }
    },

    async handleAddToQueue(e) {
        const $btn = $(e.currentTarget);
        const uri = $btn.data('uri');
        const name = $btn.data('name');

        if (!uri) {
            alert('Track URI is not available');
            return;
        }

        $btn.prop('disabled', true).html('<i class="fas fa-spinner fa-spin"></i>');

        try {
            const deviceId = window.PlayerModule?.deviceId || null;
            await SpotifyAPI.addToQueue(uri, deviceId);
            if (window.Utils?.showSuccess) {
                Utils.showSuccess(`Added to queue: ${name}`);
            }
            if (window.PlayerModule) {
                PlayerModule.updateQueue();
            }
        } catch (error) {
            console.error('Error adding to queue:', error);
            if (window.Utils?.showError) {
                Utils.showError(error.message || 'Failed to add to queue. Make sure you have an active Spotify device.');
            }
        } finally {
            $btn.prop('disabled', false).html('<i class="fas fa-list-ul"></i>');
        }
    },

    handlePlayAll(e) {
        const $btn = $(e.currentTarget);
        const uri = $btn.data('uri');
        const type = $btn.data('type');

        if (!uri) {
            alert('Album URI is not available');
            return;
        }

        window.PlayerModule?.playPlaylist(uri, type);
    }
};

window.DetailView = DetailView;
