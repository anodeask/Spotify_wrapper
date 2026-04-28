// Player module for controlling Spotify playback
const PlayerModule = {
    currentTrack: null,
    isPlaying: false,
    updateTimeout: null,
    isUpdating: false,
    isQueueUpdating: false,
    deviceId: null,
    keyboardHandler: null,
    keyboardShortcutsInitialized: false,

    // Initialize player module
    init() {
        this.bindEvents();
        this.initKeyboardShortcuts();
        this.startUpdateLoop();
    },
    
    // Bind player control events
    bindEvents() {
        $('#play-pause-btn').on('click', this.handlePlayPause.bind(this));
        $('#next-btn').on('click', this.handleNext.bind(this));
        $('#prev-btn').on('click', this.handlePrevious.bind(this));
        $('#volume-slider').on('input', this.handleVolumeChange.bind(this));
        $('#volume-slider').on('input mousemove', this.handleVolumeTooltip.bind(this));
        $('#volume-slider').on('mouseleave', () => $('#player-tooltip').hide());
        $('#progress-container').on('click', this.handleSeek.bind(this));
        $('#progress-container').on('mousemove', this.handleSeekTooltip.bind(this));
        $('#progress-container').on('mouseleave', () => $('#player-tooltip').hide());
        $('#progress-container').on('keydown', this.handleSeekKeyboard.bind(this));
        $('.seek-btn').on('click', this.handleSeekBySeconds.bind(this));
        $('#volume-up-btn').on('click', () => {
            const val = Math.min(100, parseInt($('#volume-slider').val()) + CONFIG.PLAYER.VOLUME_STEP);
            $('#volume-slider').val(val).trigger('input');
        });
        $('#volume-down-btn').on('click', () => {
            const val = Math.max(0, parseInt($('#volume-slider').val()) - CONFIG.PLAYER.VOLUME_STEP);
            $('#volume-slider').val(val).trigger('input');
        });
        $('#refresh-queue-btn').on('click', this.updateQueue.bind(this));
        
        // Stop event propagation on slider to prevent issues
        $('#volume-slider').on('mousedown touchstart', (e) => {
            e.stopPropagation();
        });
    },
    
    // Start the update loop for current track info
    startUpdateLoop() {
        if (this.updateTimeout) {
            clearTimeout(this.updateTimeout);
        }
        
        const scheduleNextUpdate = async () => {
            await this.updateCurrentTrack();
            await this.updateQueue();
            this.updateTimeout = setTimeout(scheduleNextUpdate, CONFIG.PLAYER.UPDATE_INTERVAL);
        };
        
        scheduleNextUpdate();
    },
    
    // Stop the update loop
    stopUpdateLoop() {
        if (this.updateTimeout) {
            clearTimeout(this.updateTimeout);
            this.updateTimeout = null;
        }
    },
    
    // Update current track information
    async updateCurrentTrack() {
        if (this.isUpdating) return;
        this.isUpdating = true;
        try {
            const response = await SpotifyAPI.getCurrentTrack();
            
            if (response && response.item) {
                this.currentTrack = response.item;
                this.isPlaying = response.is_playing;
                this.deviceId = response.device?.id || null;
                this.displayCurrentTrack(response);
            } else {
                this.currentTrack = null;
                this.isPlaying = false;
                this.displayNoTrack();
            }
            
            this.updatePlayPauseButton();
            
        } catch (error) {
            console.error('Failed to get current track:', error);
            // Don't show error for this as it's called frequently
            if (error.message.includes('401')) {
                // Token expired, might need to re-authenticate
                this.displayNoTrack();
            }
        } finally {
            this.isUpdating = false;
        }
    },
    
    // Display current track information
    displayCurrentTrack(trackData) {
        const track = trackData.item;
        const progressMs = trackData.progress_ms || 0;
        const durationMs = track.duration_ms;
        const progressPercent = (progressMs / durationMs) * 100;
        
        const imageUrl = Utils.getImageUrl(track.album?.images, 300);
        const artists = track.artists.map(artist => artist.name).join(', ');
        const artistLinks = Utils.formatArtistLinks(track.artists);
        
        // Update track info
        $('#track-image').attr('src', imageUrl).attr('alt', track.name);
        $('#track-name').text(track.name);
        $('#track-artist').html(artistLinks);
        
        // Update progress bar and time labels
        $('#track-progress').css('width', `${progressPercent}%`);
        $('#progress-container').attr('aria-valuenow', Math.round(progressPercent));
        $('#track-current-time').text(this.formatTime(progressMs));
        $('#track-duration').text(this.formatTime(durationMs));
        this.currentDurationMs = durationMs;
        this.currentProgressMs = progressMs;
        
        // Show track info, hide no-track message
        $('#track-info').removeClass('d-none');
        $('#no-track').addClass('d-none');
        
        // Update page title
        document.title = `${track.name} - ${artists} | Spotify Wrapper`;
    },
    
    // Display no track playing message
    displayNoTrack() {
        $('#track-info').addClass('d-none');
        $('#no-track').removeClass('d-none');
        
        // Reset page title
        document.title = 'Spotify Wrapper';
    },

    async updateQueue() {
        if (this.isQueueUpdating) return;
        this.isQueueUpdating = true;

        try {
            const response = await SpotifyAPI.getQueue();
            this.displayQueue(response);
        } catch (error) {
            console.error('Failed to get queue:', error);
            $('#queue-list').html(`
                <div class="list-group-item text-muted text-center">
                    Queue unavailable. Start playback on an active device.
                </div>
            `);
        } finally {
            this.isQueueUpdating = false;
        }
    },

    displayQueue(queueData) {
        const $queueList = $('#queue-list');
        const queue = queueData?.queue || [];

        if (!queue.length) {
            $queueList.html(`
                <div class="list-group-item text-muted text-center">
                    Queue is empty.
                </div>
            `);
            return;
        }

        const html = queue.slice(0, 10).map((track, index) => {
            const name = Utils.escapeHtml(track?.name || 'Unknown Track');
            const artistLinks = Utils.formatArtistLinks(track?.artists || []);
            return `
                <div class="list-group-item">
                    <div class="fw-semibold">${index + 1}. ${name}</div>
                    <div class="small text-muted">${artistLinks}</div>
                </div>
            `;
        }).join('');

        $queueList.html(html);
    },
    
    // Update play/pause button state
    updatePlayPauseButton() {
        const $btn = $('#play-pause-btn');
        const $icon = $btn.find('i');
        
        if (this.isPlaying) {
            $icon.removeClass('fa-play').addClass('fa-pause');
            $btn.removeClass('btn-success').addClass('btn-warning');
            $btn.attr('aria-label', 'Pause');
        } else {
            $icon.removeClass('fa-pause').addClass('fa-play');
            $btn.removeClass('btn-warning').addClass('btn-success');
            $btn.attr('aria-label', 'Play');
        }
       
    },
    
    // Handle play/pause button click
    async handlePlayPause() {
        const $btn = $('#play-pause-btn');
        const $icon = $btn.find('i');
        
        // Disable button and show loading
        $btn.prop('disabled', true);
        $icon.removeClass('fa-play fa-pause').addClass('fa-spinner fa-spin');
        
        try {
            if (this.isPlaying) {
                await SpotifyAPI.pause(this.deviceId);
                this.isPlaying = false;
            } else {
                await SpotifyAPI.play(this.deviceId);
                this.isPlaying = true;
            }
            $icon.removeClass('fa-spin');
            $icon.removeClass('fa-spinner');
            
            // Update button immediately
            this.updatePlayPauseButton();
            
            // Force update current track info
            this.updateCurrentTrack();
            this.updateQueue();
            
        } catch (error) {
            console.error('Failed to toggle playback:', error);
            Utils.showError(error.message || CONFIG.ERRORS.PLAYBACK_FAILED);
            
            // Reset button state
            $icon.removeClass('fa-spinner fa-spin');
            this.updatePlayPauseButton();
        } finally {
            $btn.prop('disabled', false);
        }
    },
    
    // Handle next track button
    async handleNext() {
        const $btn = $('#next-btn');
        const $icon = $btn.find('i');
        
        $btn.prop('disabled', true);
        $icon.removeClass('fa-step-forward').addClass('fa-spinner fa-spin');
        
        try {
            await SpotifyAPI.next(this.deviceId);
            
            // Force immediate update
            setTimeout(() => {
                this.updateCurrentTrack();
                this.updateQueue();
            }, 500);
            
        } catch (error) {
            console.error('Failed to skip to next track:', error);
            Utils.showError(error.message || CONFIG.ERRORS.PLAYBACK_FAILED);
        } finally {
            $btn.prop('disabled', false);
            $icon.removeClass('fa-spinner fa-spin').addClass('fa-step-forward');
        }
    },
    
    // Handle previous track button
    async handlePrevious() {
        const $btn = $('#prev-btn');
        const $icon = $btn.find('i');
        
        $btn.prop('disabled', true);
        $icon.removeClass('fa-step-backward').addClass('fa-spinner fa-spin');
        
        try {
            await SpotifyAPI.previous(this.deviceId);
            
            // Force immediate update
            setTimeout(() => {
                this.updateCurrentTrack();
                this.updateQueue();
            }, 500);
            
        } catch (error) {
            console.error('Failed to skip to previous track:', error);
            Utils.showError(error.message || CONFIG.ERRORS.PLAYBACK_FAILED);
        } finally {
            $btn.prop('disabled', false);
            $icon.removeClass('fa-spinner fa-spin').addClass('fa-step-backward');
        }
    },
    
    // Handle volume change
    handleVolumeChange() {
        const volume = $('#volume-slider').val();
        
        // Debounce volume changes to avoid too many API calls
        if (this.volumeTimeout) {
            clearTimeout(this.volumeTimeout);
        }
        
        this.volumeTimeout = setTimeout(() => {
            this.setVolume(volume);
        }, 300);
    },
    
    // Set volume level
    async setVolume(volume) {
        try {
            await SpotifyAPI.setVolume(parseInt(volume));
        } catch (error) {
            console.error('Failed to set volume:', error);
            // Don't show error for volume changes as they're frequent
        }
    },
    
    // Format milliseconds to m:ss
    formatTime(ms) {
        const totalSeconds = Math.floor(ms / 1000);
        const minutes = Math.floor(totalSeconds / 60);
        const seconds = totalSeconds % 60;
        return `${minutes}:${seconds.toString().padStart(2, '0')}`;
    },

    // Show tooltip with time at mouse position on progress bar
    handleSeekTooltip(event) {
        if (!this.currentTrack || !this.currentDurationMs) {
            $('#player-tooltip').hide();
            return;
        }
        const $bar = $('#progress-container');
        const offsetX = event.pageX - $bar.offset().left;
        const barWidth = $bar.width();
        const percent = Math.max(0, Math.min(1, offsetX / barWidth));
        const positionMs = Math.round(percent * this.currentDurationMs);
        $('#player-tooltip').text(this.formatTime(positionMs)).css({ left: event.pageX + 'px', top: (event.pageY - 30) + 'px' }).show();
    },

    // Show tooltip with volume percentage at cursor position
    handleVolumeTooltip(event) {
        if (!event.pageX || !event.pageY) {
            $('#player-tooltip').hide();
            return;
        }
        const $wrapper = $('#volume-wrapper');
        const offsetX = event.pageX - $wrapper.offset().left;
        const wrapperWidth = $wrapper.width();
        const percent = Math.max(0, Math.min(100, Math.round((offsetX / wrapperWidth) * 100)));
        $('#player-tooltip').text(percent + '%').css({ left: event.pageX + 'px', top: (event.pageY - 30) + 'px' }).show();
    },

    // Seek current track by the provided offset in milliseconds.
    async seekByOffsetMs(offsetMs) {
        if (!this.currentTrack || !this.currentDurationMs) return;

        const currentMs = this.currentProgressMs || 0;
        const positionMs = Math.max(0, Math.min(currentMs + offsetMs, this.currentDurationMs));
        const percent = (positionMs / this.currentDurationMs) * 100;

        $('#track-progress').css('width', `${percent}%`);
        $('#progress-container').attr('aria-valuenow', Math.round(percent));
        $('#track-current-time').text(this.formatTime(positionMs));

        try {
            await SpotifyAPI.seek(positionMs, this.deviceId);
            this.startUpdateLoop();
        } catch (error) {
            console.error('Failed to seek:', error);    
        }
    },

    // Handle keyboard seek on progress bar (Left/Right arrows)
    async handleSeekKeyboard(event) {
        if (!this.currentTrack || !this.currentDurationMs) return;
        
        let offsetMs = 0;
        if (event.key === 'ArrowRight') {
            offsetMs = 5000;
        } else if (event.key === 'ArrowLeft') {
            offsetMs = -5000;
        } else {
            return;
        }
        event.preventDefault();

        await this.seekByOffsetMs(offsetMs);
    },

    // Handle seek by +/- seconds button click
    async handleSeekBySeconds(event) {
        if (!this.currentTrack) return;
        
        const seconds = parseInt($(event.currentTarget).data('seek'));
        const offsetMs = seconds * 1000;
        const currentMs = this.currentProgressMs || 0;
        const positionMs = Math.max(0, Math.min(currentMs + offsetMs, this.currentDurationMs || 0));
        
        // Update UI immediately
        const percent = this.currentDurationMs ? (positionMs / this.currentDurationMs) * 100 : 0;
        $('#track-progress').css('width', `${percent}%`);
        $('#track-current-time').text(this.formatTime(positionMs));
        
        try {
            await SpotifyAPI.seek(positionMs, this.deviceId);
            this.startUpdateLoop();
        } catch (error) {
            console.error('Failed to seek:', error);
            Utils.showError(error.message || CONFIG.ERRORS.PLAYBACK_FAILED);
        }
    },

    // Handle seek on progress bar click
    async handleSeek(event) {
        if (!this.currentTrack || !this.currentDurationMs) return;
        
        const $bar = $('#progress-container');
        const clickX = event.pageX - $bar.offset().left;
        const barWidth = $bar.width();
        const seekPercent = Math.max(0, Math.min(1, clickX / barWidth));
        const positionMs = Math.round(seekPercent * this.currentDurationMs);
        
        // Update UI immediately
        $('#track-progress').css('width', `${seekPercent * 100}%`);
        $('#track-current-time').text(this.formatTime(positionMs));
        
        try {
            await SpotifyAPI.seek(positionMs, this.deviceId);
            this.startUpdateLoop();
        } catch (error) {
            console.error('Failed to seek:', error);
            Utils.showError(error.message || CONFIG.ERRORS.PLAYBACK_FAILED);
        }
    },

    // Get current track info
    getCurrentTrack() {
        return this.currentTrack;
    },
    
    // Check if currently playing
    getIsPlaying() {
        return this.isPlaying;
    },
    
    // Force refresh current track
    refresh() {
        this.startUpdateLoop();
        this.updateQueue();
    },
    
    // Handle keyboard shortcuts
    handleKeyboardShortcuts(event) {
        // Handle keyboard controls only when player tab is active.
        if (!window.App || App.getCurrentTab() !== 'player') {
            return;
        }

        // Only handle shortcuts if not typing in an input
        const targetTag = event.target.tagName;
        if (targetTag === 'INPUT' || targetTag === 'TEXTAREA' || event.target.isContentEditable) {
            return;
        }
        
        switch (event.code) {
            case 'Space':
                event.preventDefault();
                this.handlePlayPause();
                break;
            case 'ArrowRight':
                if (event.shiftKey) {
                    event.preventDefault();
                    this.seekByOffsetMs(5000);
                } else if (event.ctrlKey || event.metaKey) {
                    event.preventDefault();
                    this.handleNext();
                }
                break;
            case 'ArrowLeft':
                if (event.shiftKey) {
                    event.preventDefault();
                    this.seekByOffsetMs(-5000);
                } else if (event.ctrlKey || event.metaKey) {
                    event.preventDefault();
                    this.handlePrevious();
                }
                break;
            case 'ArrowUp':
                if (event.ctrlKey || event.metaKey) {
                    event.preventDefault();
                    const currentVolume = parseInt($('#volume-slider').val());
                    const newVolume = Math.min(100, currentVolume + CONFIG.PLAYER.VOLUME_STEP);
                    $('#volume-slider').val(newVolume).trigger('input');
                }
                break;
            case 'ArrowDown':
                if (event.ctrlKey || event.metaKey) {
                    event.preventDefault();
                    const currentVolume = parseInt($('#volume-slider').val());
                    const newVolume = Math.max(0, currentVolume - CONFIG.PLAYER.VOLUME_STEP);
                    $('#volume-slider').val(newVolume).trigger('input');
                }
                break;
        }
    },
    
    // Initialize keyboard shortcuts
    initKeyboardShortcuts() {
        if (this.keyboardShortcutsInitialized) {
            return;
        }

        if (!this.keyboardHandler) {
            this.keyboardHandler = this.handleKeyboardShortcuts.bind(this);
        }

        $(document).off('keydown.playerShortcuts').on('keydown.playerShortcuts', this.keyboardHandler);
        this.keyboardShortcutsInitialized = true;
    },
    
    // Cleanup when module is destroyed
    destroy() {
        this.stopUpdateLoop();
        if (this.volumeTimeout) {
            clearTimeout(this.volumeTimeout);
        }
        $(document).off('keydown.playerShortcuts');
        this.keyboardShortcutsInitialized = false;
    }
};

// Bind shortcuts as soon as the document is ready so key handling is available
// even before auth-triggered module initialization finishes.
$(document).ready(() => {
    PlayerModule.initKeyboardShortcuts();
});

// Export for global access
window.PlayerModule = PlayerModule;
