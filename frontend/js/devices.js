// Devices module for managing Spotify devices
const DevicesModule = {
    templates: {},
    devices: [],
    activeDevice: null,
    refreshInterval: null,
    isTabActive: !document.hidden,
    tabVisibilityHandler: null,
    
    // Initialize devices module
    init() {
        this.compileTemplates();
        this.bindVisibilityEvents();
        this.bindEvents();
        this.loadDevices();
        this.startAutoRefresh();
    },

    compileTemplates() {
        const deviceCardTemplate = document.getElementById('device-card-template');
        if (deviceCardTemplate) {
            this.templates.deviceCard = Handlebars.compile(deviceCardTemplate.innerHTML);
        }
    },
    
    // Bind device-related events
    bindEvents() {
        $('#refresh-devices').on('click', this.loadDevices.bind(this));
        
        // Delegate events for dynamically created device cards
        $('#devices-list').on('click', '.device-card', this.handleDeviceSelect.bind(this));
        $('#devices-list').on('click', '.transfer-btn', this.handleTransferPlayback.bind(this));
    },

    bindVisibilityEvents() {
        if (this.tabVisibilityHandler) {
            return;
        }

        this.tabVisibilityHandler = () => {
            this.isTabActive = !document.hidden;

            if (this.isTabActive) {
                this.loadDevices();
            }
        };

        document.addEventListener('visibilitychange', this.tabVisibilityHandler);
    },
    
    // Load available devices
    async loadDevices() {
        this.showLoading(true);
        
        try {
            const response = await SpotifyAPI.getDevices();
            this.devices = response.devices || [];
            this.displayDevices();
        } catch (error) {
            console.error('Failed to load devices:', error);
            this.showError(error.message || 'Failed to load devices');
        } finally {
            this.showLoading(false);
        }
    },
    
    // Display devices list
    displayDevices() {
        const $devicesList = $('#devices-list');
        
        if (this.devices.length === 0) {
            Utils.showEmptyMessage($devicesList, 'No devices found. Make sure Spotify is open on at least one of your devices.');
            return;
        }
        
        let html = '';
        this.devices.forEach(device => {
            html += this.renderDevice(device);
        });
        
        $devicesList.html(html);
        this.applyDeviceVolumeWidths($devicesList);
        
        // Update active device
        this.activeDevice = this.devices.find(d => d.is_active) || null;
    },

    applyDeviceVolumeWidths($scope) {
        $scope.find('.js-device-volume-bar').each((_, element) => {
            const value = Number($(element).data('volume'));
            const volume = Number.isFinite(value) ? Math.max(0, Math.min(100, value)) : 0;
            element.style.width = `${volume}%`;
        });
    },
    
    // Render a single device card
    renderDevice(device) {
        if (!this.templates.deviceCard) {
            return '';
        }

        const deviceIcon = this.getDeviceIcon(device.type);
        const isActive = device.is_active;
        const cardClass = isActive ? 'device-card active border-success' : 'device-card';
        const statusClass = isActive ? 'device-status active' : 'device-status inactive';
        const rawVolume = Number.isFinite(device.volume_percent) ? device.volume_percent : Number(device.volume_percent || 0);
        const volumePercent = Math.max(0, Math.min(100, Number.isFinite(rawVolume) ? rawVolume : 0));

        return this.templates.deviceCard({
            cardClass,
            deviceId: device.id,
            deviceIcon,
            deviceName: Utils.truncateText(device.name, 20),
            statusClass,
            statusLabel: isActive ? 'Active' : 'Available',
            volumePercent,
            isActive
        });
    },
    
    // Get device icon based on type
    getDeviceIcon(deviceType) {
        const icons = {
            'Computer': 'fas fa-desktop',
            'Smartphone': 'fas fa-mobile-alt',
            'Speaker': 'fas fa-volume-up',
            'TV': 'fas fa-tv',
            'AVR': 'fas fa-broadcast-tower',
            'STB': 'fas fa-tv',
            'AudioDongle': 'fas fa-usb',
            'GameConsole': 'fas fa-gamepad',
            'CastVideo': 'fab fa-chromecast',
            'CastAudio': 'fab fa-chromecast',
            'Automobile': 'fas fa-car',
            'Unknown': 'fas fa-question-circle'
        };
        
        return icons[deviceType] || icons['Unknown'];
    },
    
    // Handle device selection
    handleDeviceSelect(e) {
        const $card = $(e.currentTarget);
        const deviceId = $card.data('device-id');
        const device = this.devices.find(d => d.id === deviceId);
        
        if (device) {
            this.showDeviceDetails(device);
        }
    },
    
    // Show device details in a modal or detailed view
    showDeviceDetails(device) {
        // For now, just log device details
        // Could expand to show a modal with more device information
        console.log('Device details:', device);
    },
    
    // Handle transfer playback to device
    async handleTransferPlayback(e) {
        e.stopPropagation(); // Prevent card click event
        
        const $btn = $(e.currentTarget);
        const deviceId = $btn.data('device-id');
        const device = this.devices.find(d => d.id === deviceId);
        
        if (!device) return;
        
        $btn.prop('disabled', true).html('<i class="fas fa-spinner fa-spin me-1"></i>Transferring...');
        
        try {
            await SpotifyAPI.transferPlayback(deviceId, true);
            Utils.showSuccess(`Playback transferred to ${device.name}`);
            
            // Refresh devices to update active status
            setTimeout(() => this.loadDevices(), 1000);
            
        } catch (error) {
            console.error('Failed to transfer playback:', error);
            Utils.showError(error.message || 'Failed to transfer playback');
        } finally {
            $btn.prop('disabled', false).html('<i class="fas fa-exchange-alt me-1"></i>Transfer Playback');
        }
    },
    
    // Start auto-refresh of devices
    startAutoRefresh() {
        if (this.refreshInterval) {
            clearInterval(this.refreshInterval);
        }
        
        this.refreshInterval = setInterval(() => {
            if (this.isTabActive) {
                this.loadDevices();
            }
        }, CONFIG.DEVICE_REFRESH_INTERVAL);
    },
    
    // Stop auto-refresh
    stopAutoRefresh() {
        if (this.refreshInterval) {
            clearInterval(this.refreshInterval);
            this.refreshInterval = null;
        }
    },
    
    // Get active device
    getActiveDevice() {
        return this.activeDevice;
    },
    
    // Check if any device is active
    hasActiveDevice() {
        return this.activeDevice !== null;
    },
    
    // Show loading state
    showLoading(show) {
        if (show) {
            Utils.showFullLoading($('#devices-list'), 'Loading devices...');
        }
    },
    
    // Show error message
    showError(message) {
        const $container = $('#devices-list');
        $container.html(`
            <div class="col-12">
                <div class="alert alert-danger" role="alert">
                    <i class="fas fa-exclamation-triangle me-2"></i>
                    ${message}
                    <button class="btn btn-outline-danger btn-sm ms-2" onclick="DevicesModule.loadDevices()">
                        <i class="fas fa-sync-alt me-1"></i>Retry
                    </button>
                </div>
            </div>
        `);
    },
    
    // Cleanup when module is destroyed
    destroy() {
        this.stopAutoRefresh();

        if (this.tabVisibilityHandler) {
            document.removeEventListener('visibilitychange', this.tabVisibilityHandler);
            this.tabVisibilityHandler = null;
        }
    }
};

// Export for global access
window.DevicesModule = DevicesModule;
