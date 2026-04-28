# Spotify Web API Wrapper

A full-stack web application that wraps the Spotify Web API, allowing users to search for music, manage playlists, control playback, and view their library.

## Features

- 🎵 **Search**: Search for songs, artists, albums, and playlists
- 📚 **Library**: View your playlists, liked songs, and recently played tracks
- 🎮 **Playback Control**: Play, pause, skip tracks, seek with clickable progress bar, +/- 5/10/15s buttons, volume step up/down, and hover tooltips on any connected device
- 🧾 **Queue Management**: Add tracks to queue from Search/Library and view current queue in Player tab
- 📱 **Device Management**: View and switch between available Spotify devices
- 🔐 **OAuth Integration**: Secure authentication with Spotify
- 💾 **Database Storage**: Persistent user data and token management
- 🔄 **Auto-Refresh**: Recently played tracks refresh automatically every minute
- ♿ **WCAG Level A Accessible**: Full keyboard navigation, ARIA labels, screen reader support, semantic HTML structure

## Tech Stack

### Backend
- Java 17
- Spring Boot 3.2.3
- Spring Security
- Spring Data JPA
- MySQL 8.0
- Apache HttpClient (with connection pooling)
- Maven

### Frontend
- jQuery 3.7.1
- Bootstrap 5.3.2
- Handlebars.js 4.7.8
- Font Awesome 6.5.1
- Live Server (development)

## Prerequisites

Before running this application, make sure you have:

- Java 17 or higher
- Node.js 16 or higher
- MySQL 8.0 or higher
- Maven 3.6 or higher
- A Spotify Developer account

## Setup Instructions

### 1. Spotify App Setup

1. Go to the [Spotify Developer Dashboard](https://developer.spotify.com/dashboard)
2. Create a new app
3. Note your `Client ID` and `Client Secret`
4. Add `http://127.0.0.1:3000` to your Redirect URIs

### 2. Database Setup

1. Create a MySQL database:
```sql
CREATE DATABASE spotify_wrapper;
```

2. Create a MySQL user (optional):
```sql
CREATE USER 'spotify_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON spotify_wrapper.* TO 'spotify_user'@'localhost';
FLUSH PRIVILEGES;
```

### 3. Backend Configuration

1. Navigate to the backend directory:
```bash
cd backend
```

2. Copy the template and update `src/main/resources/application.properties`:
```bash
cp ../application.properties.template src/main/resources/application.properties
```

3. Edit `application.properties` with your credentials:
```properties
# Database configuration
spring.datasource.url=jdbc:mysql://localhost:3306/spotify_wrapper
spring.datasource.username=your_db_username
spring.datasource.password=your_db_password

# Spotify API configuration
spotify.client.id=your_spotify_client_id
spotify.client.secret=your_spotify_client_secret
spotify.redirect.uri=http://127.0.0.1:3000
spotify.scope=user-read-private user-read-email playlist-read-private playlist-read-collaborative user-read-playback-state user-modify-playback-state user-library-read user-read-recently-played streaming
```

4. Build and run:
```bash
mvn -f pom.xml org.springframework.boot:spring-boot-maven-plugin:run
```

The backend will start on `http://127.0.0.1:9090`

### 4. Frontend Setup

1. Navigate to the frontend directory:
```bash
cd frontend
```

2. Install dependencies:
```bash
npm install
```

3. Start the development server:
```bash
npm start
```

The frontend will start on `http://127.0.0.1:3000`

### Quick Start (Recommended)

Use the startup script to run both backend and frontend:
```bash
./start.sh
```

## Usage

1. **Login**: Click "Login with Spotify" to authenticate with your Spotify account
2. **Player** (Default Tab): Control playback with play/pause, next, and previous buttons. Seek using:
   - Clickable progress bar (shows time on hover)
   - Arrow buttons for ±5/10/15 second jumps
   - Keyboard: **Shift + Left/Right** for ±5 second increments (from anywhere in Player tab)
   - Keyboard (when progress bar is focused): Left/Right for ±5 second increments
   - Volume: Adjust with +/- buttons or slider (shows volume % on hover)
3. **Search**: Use the search tab to find songs, artists, albums, and playlists. Select **All** to search songs, albums, and playlists together in grouped results.
   - Artist names are clickable and open the Spotify artist page
   - Use **Add to Queue** on tracks to queue without interrupting playback
4. **Library**: Browse recently played tracks, liked songs, and your playlists (Recently Played is the default sub-tab)
   - Liked Songs and Recently Played support **Add to Queue**
5. **Devices**: View and select available Spotify devices
6. **Current Queue**: In Player tab, use **Current Queue** panel to view queued tracks and refresh queue state

### Keyboard Navigation
- **Tab** to navigate between interactive elements
- **Shift + Left/Right** to seek -5/+5 seconds in Player tab
- **Ctrl/Cmd + Left/Right** for previous/next track
- **Ctrl/Cmd + Up/Down** to adjust volume step
- **Arrow keys** to adjust seek/volume when the relevant slider control is focused
- **Enter/Space** to activate buttons

## API Endpoints

### Authentication
- `GET /auth/login` - Get Spotify authorization URL
- `GET /auth/callback` - Handle OAuth callback
- `GET /auth/user/{userId}` - Get user information

### Spotify Integration
- `GET /api/spotify/search` - Search for tracks, artists, albums, and playlists (`limit` supported)
- `GET /api/spotify/devices` - Get available devices
- `GET /api/spotify/playback` - Get current playback state
- `GET /api/spotify/queue` - Get current playback queue
- `POST /api/spotify/play` - Start playback
- `POST /api/spotify/pause` - Pause playback
- `POST /api/spotify/next` - Skip to next track
- `POST /api/spotify/previous` - Skip to previous track
- `POST /api/spotify/transfer` - Transfer playback to device
- `POST /api/spotify/queue/add` - Add track/episode URI to queue
- `PUT /api/spotify/seek` - Seek to position in current track

### Library
- `GET /api/spotify/me/playlists` - Get user's playlists
- `GET /api/spotify/me/tracks` - Get user's liked songs
- `GET /api/spotify/me/recently-played` - Get recently played tracks

## Database Schema

The application uses a single `users` table to store:
- Spotify user ID
- Display name and email
- Access and refresh tokens
- Token expiration timestamp
- Created/updated timestamps

## Development

### Backend Development
- The application uses Spring Boot with HikariCP connection pooling
- Apache HttpClient with connection pooling for Spotify API calls
- Logs are output to console and `logs/spotify-wrapper.log`
- Configure logging levels in `application.properties`

### Frontend Development
- Live-server provides hot reloading during development
- Uses Handlebars.js for templating
- Bootstrap 5 for responsive UI components
- jQuery for DOM manipulation and AJAX calls

## Project Structure

```
spotify/
├── backend/
│   ├── src/main/java/com/spotify/wrapper/
│   │   ├── config/          # Security configuration
│   │   ├── controller/      # REST controllers
│   │   ├── dto/             # Data transfer objects
│   │   ├── entity/          # JPA entities
│   │   ├── repository/      # Data repositories
│   │   └── service/         # Business logic
│   └── src/main/resources/
│       └── application.properties
├── frontend/
│   ├── css/                 # Stylesheets
│   ├── js/                  # JavaScript modules
│   │   ├── app.js          # Main application
│   │   ├── auth.js         # Authentication
│   │   ├── config.js       # API endpoints
│   │   ├── devices.js      # Device management
│   │   ├── library.js      # Library features
│   │   ├── player.js       # Playback controls
│   │   ├── search.js       # Search functionality
│   │   └── spotify.js      # Spotify API wrapper
│   └── index.html          # Main HTML page
├── application.properties.template
├── database_setup.sql
├── start.sh
└── README.md
```

## Security Considerations

- Access tokens are stored securely in the database
- CORS is configured to allow requests from the frontend origin only
- Refresh tokens are automatically used to obtain new access tokens
- All Spotify API calls require valid authentication

## Troubleshooting

### Common Issues

1. **"No devices found"**: Make sure Spotify is open and active on at least one device
2. **"Authentication failed"**: Check your Spotify app credentials and redirect URI
3. **"Database connection error"**: Verify MySQL is running and credentials are correct
4. **"CORS error"**: Ensure the frontend URL is properly configured in CORS settings

### Development Tips

- Check browser console for frontend errors
- Monitor backend logs for API call details
- Use Spotify's Web API documentation for reference
- Test with different device types (desktop, mobile, web player)

## License

This project is for educational purposes. Make sure to comply with Spotify's Terms of Service and API Terms of Use.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

For questions or issues, please create an issue in the repository.
