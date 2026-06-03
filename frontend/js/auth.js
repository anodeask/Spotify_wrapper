// Authentication module for Spotify Wrapper
const Auth = {
    currentUser: null,
    
    // Initialize authentication
    init() {
        this.bindEvents();
        this.checkExistingAuth();
    },
    
    // Bind authentication events
    bindEvents() {
        $('#login-btn').on('click', this.handleLogin.bind(this));
        $('#logout-btn').on('click', this.handleLogout.bind(this));
        
        // Check for OAuth callback
        this.handleCallback();
    },
    
    // Check if user is already authenticated
    async checkExistingAuth() {
        const userId = localStorage.getItem(CONFIG.STORAGE_KEYS.USER_ID);
        
        if (userId) {
            try {
                const response = await $.get(`${CONFIG.API_BASE_URL}${CONFIG.ENDPOINTS.AUTH.USER}/${userId}`);
                
                if (response) {
                    this.currentUser = response;
                    this.showDashboard();
                    return;
                }
            } catch (error) {
                console.error('Failed to verify existing auth:', error);
                this.clearAuthData();
            }
        }
        
        this.showLogin();
    },
    
    // Handle OAuth callback from Spotify
    handleCallback() {
        const urlParams = new URLSearchParams(window.location.search);
        const code = urlParams.get('code');
        const state = urlParams.get('state');
        const error = urlParams.get('error');
        
        if (error) {
            this.showError('Authorization failed: ' + error);
            return;
        }
        
        if (code && state) {
            this.processCallback(code, state);
        }
    },
    
    // Process OAuth callback
    async processCallback(code, state) {
        console.log('Processing OAuth callback with code:', code.substring(0, 10) + '...', 'state:', state);
        this.showLoading(true);
        $('#login-error').addClass('d-none');
        
        try {
            const response = await $.ajax({
                url: `${CONFIG.API_BASE_URL}${CONFIG.ENDPOINTS.AUTH.CALLBACK}`,
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                data: `code=${encodeURIComponent(code)}&state=${encodeURIComponent(state)}`
            });
            
            console.log('Callback response:', response);
            
            if (response && response.userId) {
                this.currentUser = response;
                localStorage.setItem(CONFIG.STORAGE_KEYS.USER_ID, response.userId);
                localStorage.setItem(CONFIG.STORAGE_KEYS.USER_DATA, JSON.stringify(response));
                
                // Clear URL parameters
                window.history.replaceState({}, document.title, window.location.pathname);
                
                this.showDashboard();
            } else {
                throw new Error('Invalid response from server');
            }
        } catch (error) {
            console.error('Callback processing failed:', error);
            this.showError('Login failed: ' + (error.responseText || error.message));
        } finally {
            this.showLoading(false);
        }
    },
    
    // Handle login button click
    async handleLogin() {
        console.log('Initiating login process...');
        this.showLoading(true);
        $('#login-error').addClass('d-none');
        
        try {
            const response = await $.get(`${CONFIG.API_BASE_URL}${CONFIG.ENDPOINTS.AUTH.LOGIN}`);
            console.log('Login URL received:', response);
            
            if (response) {
                // Redirect to Spotify authorization
                console.log('Redirecting to Spotify...');
                window.location.href = response;
            } else {
                throw new Error('No authorization URL received');
            }
        } catch (error) {
            console.error('Login initiation failed:', error);
            this.showError('Failed to start login process: ' + (error.responseText || error.message));
            this.showLoading(false);
        }
    },
    
    // Handle logout
    async handleLogout() {
        try {
            if (this.currentUser && this.currentUser.userId) {
                await $.ajax({
                    url: `${CONFIG.API_BASE_URL}${CONFIG.ENDPOINTS.AUTH.LOGOUT}`,
                    method: 'POST',
                    data: { userId: this.currentUser.userId }
                });
            }
        } catch (error) {
            console.error('Logout request failed:', error);
        } finally {
            this.clearAuthData();
            this.showLogin();
        }
    },
    
    // Clear authentication data
    clearAuthData() {
        this.currentUser = null;
        localStorage.removeItem(CONFIG.STORAGE_KEYS.USER_ID);
        localStorage.removeItem(CONFIG.STORAGE_KEYS.USER_DATA);
        localStorage.removeItem(CONFIG.STORAGE_KEYS.ACCESS_TOKEN);
    },
    
    // Show login page
    showLogin() {
        $('#loading').addClass('d-none');
        $('#login-page').removeClass('d-none');
        $('#dashboard-page').addClass('d-none');
    },
    
    // Show dashboard
    showDashboard() {
        $('#loading').addClass('d-none');
        $('#login-page').addClass('d-none');
        $('#dashboard-page').removeClass('d-none');
        
        // Update user info in navbar
        if (this.currentUser) {
            $('#user-name').text(this.currentUser.displayName || 'User');
        }
        
        // Initialize alert templates
        Utils.initAlertTemplates();
        
        // Initialize other modules
        if (window.SpotifyAPI) {
            window.SpotifyAPI.init(this.currentUser.userId);
        }
        if (window.SearchModule) {
            window.SearchModule.init();
        }
        if (window.DevicesModule) {
            window.DevicesModule.init();
        }
        if (window.PlayerModule) {
            window.PlayerModule.init();
        }
        if (window.DetailView) {
            window.DetailView.init();
        }
    },
    
    // Show loading state
    showLoading(show) {
        const $loading = $('#login-loading');
        const $button = $('#login-btn');
        
        if (show) {
            $loading.removeClass('d-none');
            $button.prop('disabled', true);
        } else {
            $loading.addClass('d-none');
            $button.prop('disabled', false);
        }
    },
    
    // Show error message
    showError(message) {
        $('#login-error-message').text(message);
        $('#login-error').removeClass('d-none');
    },
    
    // Get current user
    getCurrentUser() {
        return this.currentUser;
    },
    
    // Get current user ID
    getUserId() {
        if (this.currentUser && this.currentUser.userId) {
            return this.currentUser.userId;
        }
        // Fallback to localStorage
        return localStorage.getItem(CONFIG.STORAGE_KEYS.USER_ID);
    },
    
    // Check if user is authenticated
    isAuthenticated() {
        return this.currentUser !== null;
    }
};

// Export for global access
window.Auth = Auth;
