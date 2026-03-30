// Player module for controlling Spotify playback
const PlayerModule = {
    currentTrack: null,
    isPlaying: false,
    updateTimeout: null,
    deviceId: null,

    // Initialize player module
    init() {
        this.bindEvents();
        this.startUpdateLoop();
        this.updateCurrentTrack();
    },
    
    // Bind player control events
    bindEvents() {
        $('#play-pause-btn').on('click', this.handlePlayPause.bind(this));
        $('#next-btn').on('click', this.handleNext.bind(this));
        $('#prev-btn').on('click', this.handlePrevious.bind(this));
        $('#volume-slider').on('input', this.handleVolumeChange.bind(this));
        
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
        
        // Update track info
        $('#track-image').attr('src', imageUrl).attr('alt', track.name);
        $('#track-name').text(track.name);
        $('#track-artist').text(artists);
        
        // Update progress bar
        $('#track-progress').css('width', `${progressPercent}%`);
        
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
    
    // Update play/pause button state
    updatePlayPauseButton() {
        const $btn = $('#play-pause-btn');
        const $icon = $btn.find('i');
        
        if (this.isPlaying) {
            $icon.removeClass('fa-play').addClass('fa-pause');
            $btn.removeClass('btn-success').addClass('btn-warning');
        } else {
            $icon.removeClass('fa-pause').addClass('fa-play');
            $btn.removeClass('btn-warning').addClass('btn-success');
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
            setTimeout(() => this.updateCurrentTrack(), 500);
            
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
            setTimeout(() => this.updateCurrentTrack(), 500);
            
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
        this.updateCurrentTrack();
    },
    
    // Handle keyboard shortcuts
    handleKeyboardShortcuts(event) {
        // Only handle shortcuts if not typing in an input
        if (event.target.tagName === 'INPUT' || event.target.tagName === 'TEXTAREA') {
            return;
        }
        
        switch (event.code) {
            case 'Space':
                event.preventDefault();
                this.handlePlayPause();
                break;
            case 'ArrowRight':
                if (event.ctrlKey || event.metaKey) {
                    event.preventDefault();
                    this.handleNext();
                }
                break;
            case 'ArrowLeft':
                if (event.ctrlKey || event.metaKey) {
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
        $(document).on('keydown', this.handleKeyboardShortcuts.bind(this));
    },
    
    // Cleanup when module is destroyed
    destroy() {
        this.stopUpdateLoop();
        if (this.volumeTimeout) {
            clearTimeout(this.volumeTimeout);
        }
        $(document).off('keydown', this.handleKeyboardShortcuts);
    }
};

// Export for global access
window.PlayerModule = PlayerModule;
