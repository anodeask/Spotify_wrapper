// Main application module
const App = {
    currentTab: 'player',
    
    // Initialize the application
    init() {
        this.bindEvents();
        this.initModules();
        this.setupErrorHandling();
        
        // Initialize authentication
        Auth.init();
    },
    
    // Bind global application events
    bindEvents() {
        // Tab navigation
        $('[data-tab]').on('click', this.handleTabSwitch.bind(this));
        
        // Global error handling for AJAX requests
        $(document).ajaxError(this.handleAjaxError.bind(this));
        
        // Handle browser back/forward buttons
        window.addEventListener('popstate', this.handlePopState.bind(this));
        
        // Handle window focus/blur for pausing updates
        $(window).on('focus', this.handleWindowFocus.bind(this));
        $(window).on('blur', this.handleWindowBlur.bind(this));
        
        // Handle page visibility changes
        if (document.visibilityState !== undefined) {
            document.addEventListener('visibilitychange', this.handleVisibilityChange.bind(this));
        }
    },
    
    // Initialize application modules
    initModules() {
        // Modules will be initialized when dashboard is shown
        console.log('App initialized');
    },
    
    // Setup global error handling
    setupErrorHandling() {
        // Global unhandled promise rejection handler
        window.addEventListener('unhandledrejection', (event) => {
            console.error('Unhandled promise rejection:', event.reason);
            
            // Prevent the default browser behavior
            event.preventDefault();
            
            // Show user-friendly error message
            this.showGlobalError('An unexpected error occurred. Please try again.');
        });
        
        // Global error handler
        window.addEventListener('error', (event) => {
            console.error('Global error:', event.error);
            
            // Don't show error for script loading failures or network errors
            if (event.filename && event.filename.includes('chrome-extension://')) {
                return;
            }
            
            this.showGlobalError('A technical error occurred. Please refresh the page.');
        });
    },
    
    // Handle tab switching
    handleTabSwitch(e) {
        e.preventDefault();
        
        const $link = $(e.currentTarget);
        const tab = $link.data('tab');
        
        if (tab && tab !== this.currentTab) {
            this.switchTab(tab);
        }
    },
    
    // Switch to a specific tab
    switchTab(tab) {
        // Handle deactivation of previous tab
        this.handleTabDeactivation(this.currentTab);
        
        // Update active link
        $('[data-tab]').removeClass('active');
        $(`[data-tab="${tab}"]`).addClass('active');
        
        // Hide all elements with type="lhs_option"

        $('[type="lhs_option"]').addClass('d-none');
        
        // Show selected tab content
        $(`#${tab}-tab`).removeClass('d-none');
        
        // Update current tab
        this.currentTab = tab;
        
        // Update URL without causing page reload
        const url = new URL(window.location);
        url.searchParams.set('tab', tab);
        window.history.pushState({ tab }, '', url);
        
        // Handle tab-specific logic
        this.handleTabActivation(tab);
    },
    
    // Handle tab deactivation logic
    handleTabDeactivation(tab) {
        switch (tab) {
            case 'library':
                // Stop auto-refresh when leaving library tab
                if (window.Library) {
                    Library.onTabDeactivated();
                }
                break;
        }
    },
    
    // Handle tab activation logic
    handleTabActivation(tab) {
        switch (tab) {
            case 'search':
                // Focus on search input
                setTimeout(() => $('#search-input').focus(), 100);
                break;
            case 'library':
                // Load library content when tab is activated
                
                Library.onTabActivated();
                
                break;
            case 'devices':
                // Refresh devices when tab is activated
                if (window.DevicesModule) {
                    DevicesModule.loadDevices();
                }
                break;
            case 'player':
                // Refresh player info when tab is activated
                if (window.PlayerModule) {
                    PlayerModule.refresh();
                }
                break;
        }
    },
    
    // Handle browser back/forward buttons
    handlePopState(event) {
        const urlParams = new URLSearchParams(window.location.search);
        const tab = urlParams.get('tab') || 'player';
        
        if (tab !== this.currentTab) {
            this.switchTab(tab);
        }
    },
    
    // Handle window focus
    handleWindowFocus() {
        // Resume updates when window gains focus
        if (window.PlayerModule && Auth.isAuthenticated()) {
            PlayerModule.startUpdateLoop();
        }
        if (window.DevicesModule && Auth.isAuthenticated()) {
            DevicesModule.startAutoRefresh();
        }
    },
    
    // Handle window blur
    handleWindowBlur() {
        // Reduce update frequency when window loses focus
        // Don't completely stop as user might be using other apps to control Spotify
    },
    
    // Handle page visibility changes
    handleVisibilityChange() {
        if (document.hidden) {
            // Page is hidden, reduce updates
            this.handleWindowBlur();
        } else {
            // Page is visible, resume normal updates
            this.handleWindowFocus();
        }
    },
    
    // Handle AJAX errors globally
    handleAjaxError(event, jqXHR, ajaxSettings, thrownError) {
        console.error('AJAX Error:', {
            status: jqXHR.status,
            statusText: jqXHR.statusText,
            responseText: jqXHR.responseText,
            url: ajaxSettings.url
        });
        
        // Don't show errors for frequent polling requests
        const pollingEndpoints = [
            CONFIG.ENDPOINTS.SPOTIFY.CURRENT_TRACK,
            CONFIG.ENDPOINTS.SPOTIFY.DEVICES
        ];
        
        const isPollingRequest = pollingEndpoints.some(endpoint => 
            ajaxSettings.url.includes(endpoint)
        );
        
        if (isPollingRequest) {
            return; // Silent fail for polling requests
        }
        
        // Handle specific error types
        if (jqXHR.status === 401) {
            // Unauthorized - redirect to login
            Auth.handleLogout();
            return;
        }
        
        if (jqXHR.status === 0) {
            // Network error
            this.showGlobalError(CONFIG.ERRORS.NETWORK);
            return;
        }
        
        // Try to extract error message from response
        let errorMessage = CONFIG.ERRORS.GENERIC;
        try {
            const response = JSON.parse(jqXHR.responseText);
            if (response && response.message) {
                errorMessage = response.message;
            }
        } catch (e) {
            // Use default error message
        }
        
        this.showGlobalError(errorMessage);
    },
    
    // Show global error message
    showGlobalError(message) {
        // Create or update global error container
        let $errorContainer = $('#global-error');
        if ($errorContainer.length === 0) {
            $errorContainer = $('<div id="global-error" class="position-fixed top-0 start-50 translate-middle-x" style="z-index: 9999; margin-top: 1rem;"></div>');
            $('body').append($errorContainer);
        }
        
        $errorContainer.html(`
            <div class="alert alert-danger alert-dismissible fade show shadow" role="alert">
                <i class="fas fa-exclamation-triangle me-2"></i>
                ${message}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        `);
        
        // Auto-dismiss after 5 seconds
        setTimeout(() => {
            $errorContainer.find('.alert').alert('close');
        }, 5000);
    },
    
    // Show global success message
    showGlobalSuccess(message) {
        let $successContainer = $('#global-success');
        if ($successContainer.length === 0) {
            $successContainer = $('<div id="global-success" class="position-fixed top-0 start-50 translate-middle-x" style="z-index: 9999; margin-top: 1rem;"></div>');
            $('body').append($successContainer);
        }
        
        $successContainer.html(`
            <div class="alert alert-success alert-dismissible fade show shadow" role="alert">
                <i class="fas fa-check-circle me-2"></i>
                ${message}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        `);
        
        // Auto-dismiss after 3 seconds
        setTimeout(() => {
            $successContainer.find('.alert').alert('close');
        }, 3000);
    },
    
    // Get current tab
    getCurrentTab() {
        return this.currentTab;
    },
    
    // Check if app is ready
    isReady() {
        return Auth.isAuthenticated();
    },
    
    // Handle app cleanup
    destroy() {
        // Cleanup modules
        if (window.PlayerModule) {
            PlayerModule.destroy();
        }
        if (window.DevicesModule) {
            DevicesModule.destroy();
        }
        
        // Remove global event listeners
        $(document).off('ajaxError');
        $(window).off('focus blur');
        document.removeEventListener('visibilitychange', this.handleVisibilityChange);
        window.removeEventListener('popstate', this.handlePopState);
    }
};

// Initialize app when DOM is ready
$(document).ready(() => {
    App.init();
});

// Handle page unload
$(window).on('beforeunload', () => {
    App.destroy();
});

// Export for global access
window.App = App;
