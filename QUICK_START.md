# 🚀 Quick Setup Guide

Follow these steps to get your Spotify Wrapper app running:

## 1. Prerequisites Setup

### Install Required Software
- **Java 17+**: Download from [OpenJDK](https://openjdk.org/) or [Oracle](https://www.oracle.com/java/technologies/downloads/)
- **Node.js 16+**: Download from [nodejs.org](https://nodejs.org/)
- **MySQL 8.0+**: Download from [mysql.com](https://dev.mysql.com/downloads/mysql/) or use Homebrew: `brew install mysql`
- **Git**: Download from [git-scm.com](https://git-scm.com/) (if not already installed)

### Start MySQL
```bash
# On macOS with Homebrew
brew services start mysql

# On Linux
sudo systemctl start mysql

# On Windows
net start mysql
```

## 2. Spotify Developer Setup

1. Go to [Spotify Developer Dashboard](https://developer.spotify.com/dashboard)
2. Log in with your Spotify account
3. Click "Create App"
4. Fill in:
   - **App Name**: Spotify Wrapper
   - **App Description**: Personal Spotify API wrapper
   - **Redirect URI**: `http://127.0.0.1:9090/callback`
   - **API Used**: Web API
5. Save your **Client ID** and **Client Secret**

## 3. Database Setup

```bash
# Connect to MySQL
mysql -u root -p

# Create database
CREATE DATABASE spotify_wrapper;

# Optional: Create dedicated user
CREATE USER 'spotify_user'@'localhost' IDENTIFIED BY 'your_secure_password';
GRANT ALL PRIVILEGES ON spotify_wrapper.* TO 'spotify_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

## 4. Configure Application

1. Copy the configuration template:
```bash
cp application.properties.template backend/src/main/resources/application.properties
```

2. Edit `backend/src/main/resources/application.properties`:
   - Replace `YOUR_MYSQL_PASSWORD` with your MySQL password
   - Replace `YOUR_SPOTIFY_CLIENT_ID` with your Spotify Client ID
   - Replace `YOUR_SPOTIFY_CLIENT_SECRET` with your Spotify Client Secret

## 5. Start the Application

### Option A: Use the startup script (Recommended)
```bash
./start.sh
```

### Option B: Start manually

**Terminal 1 - Backend:**
```bash
cd backend
mvn spring-boot:run
```

**Terminal 2 - Frontend:**
```bash
cd frontend
npm install
npm start
```

## 6. Access the Application

- Open your browser to: `http://127.0.0.1:3000`
- Click "Connect with Spotify"
- Authorize the application
- Start searching and playing music!

### New in current build
- Use **Add to Queue** buttons in Search and Library track cards
- Open **Player** tab to view the **Current Queue** panel
- Click artist names to open their Spotify profile pages

## 🔧 Troubleshooting

### Common Issues:

**"Connection refused" or "Cannot connect to database"**
- Make sure MySQL is running
- Check your database credentials in `application.properties`

**"Invalid client" error during Spotify login**
- Verify your Client ID and Secret are correct
- Make sure `http://127.0.0.1:9090/callback` is added to your Spotify app's Redirect URIs

**"No devices found"**
- Open Spotify on your computer, phone, or web browser
- Make sure you're logged into the same Spotify account
- Click "Refresh" in the Devices tab

**"Add to Queue" fails**
- Make sure at least one Spotify device is active
- Re-login if your session was created before playback scopes were updated

**Port already in use**
- Check if another application is using port 9090 or 3000
- Stop other instances: `pkill -f 'spring-boot\\|npm start'`

### Development Tools:

**View logs:**
- Backend logs appear in the terminal where you ran `mvn spring-boot:run`
- Frontend logs appear in your browser's developer console (F12)

**Database inspection:**
```bash
mysql -u root -p
USE spotify_wrapper;
SELECT * FROM users;
```

**Reset everything:**
```bash
# Stop all services
pkill -f 'spring-boot\\|npm start'

# Clear database
mysql -u root -p -e "DROP DATABASE spotify_wrapper; CREATE DATABASE spotify_wrapper;"

# Restart
./start.sh
```

## 🎯 Next Steps

Once everything is working:
- Explore the Search tab to find music
- Try the Player tab to control playback
- Use the Devices tab to switch between your devices
- Check out the code to understand how the Spotify API integration works

Enjoy your new Spotify wrapper! 🎵
