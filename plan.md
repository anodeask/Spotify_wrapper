# Plan: Push Spotify Wrapper to GitHub

## Prerequisites
- [ ] GitHub account
- [ ] Git installed on your machine
- [ ] GitHub CLI (optional, but helpful)

---

## Step 1: Create a GitHub Repository

### Option A: Using GitHub Web Interface
1. Go to [github.com](https://github.com)
2. Click the **+** icon in the top right → **New repository**
3. Fill in the details:
   - **Repository name:** `spotify-wrapper`
   - **Description:** `A Spotify API wrapper with Spring Boot backend and jQuery frontend`
   - **Visibility:** Public or Private (your choice)
   - **DO NOT** initialize with README, .gitignore, or license (we already have files)
4. Click **Create repository**

### Option B: Using GitHub CLI
```bash
gh repo create spotify-wrapper --public --description "A Spotify API wrapper with Spring Boot backend and jQuery frontend"
```

---

## Step 2: Initialize Local Git Repository

```bash
# Navigate to project directory
cd /Users/anudeep-6653/Documents/project/spotify

# Initialize git repository (if not already done)
git init

# Check current status
git status
```

---

## Step 3: Create .gitignore File

Create a `.gitignore` file to exclude unnecessary files:

```bash
# This will be created automatically - see Step 4
```

**Important files to ignore:**
- `backend/target/` - compiled Java files
- `backend/logs/` - log files
- `node_modules/` - if any
- `.idea/` or `.vscode/` - IDE settings
- `*.class` - compiled classes
- `application.properties` - contains secrets (use template instead)

---

## Step 4: Secure Sensitive Information

⚠️ **IMPORTANT:** Before pushing, ensure your Spotify credentials are NOT exposed!

1. **Keep the template file** (`application.properties.template`) in git
2. **Add actual config to .gitignore:**
   ```
   backend/src/main/resources/application.properties
   backend/target/classes/application.properties
   ```

---

## Step 5: Stage and Commit Files

```bash
# Add all files
git add .

# Review what will be committed
git status

# Create initial commit
git commit -m "Initial commit: Spotify Wrapper with Spring Boot backend and jQuery frontend

Features:
- Spotify OAuth authentication
- Search for tracks, artists, albums, playlists
- Library: My Playlists, Liked Songs, Recently Played
- Device management and playback control
- Volume controls auto-disable on devices without volume support
- Auto-refresh for Recently Played (every 1 minute)"
```

---

## Step 6: Connect to GitHub and Push

```bash
# Add remote repository (replace YOUR_USERNAME with your GitHub username)
git remote add origin https://github.com/YOUR_USERNAME/spotify-wrapper.git

# Verify remote was added
git remote -v

# Push to GitHub (main branch)
git branch -M main
git push -u origin main
```

---

## Step 7: Verify on GitHub

1. Go to `https://github.com/YOUR_USERNAME/spotify-wrapper`
2. Verify all files are uploaded
3. Check that `application.properties` with secrets is NOT visible
4. Update the README if needed

---

## Quick Command Summary

```bash
# All commands in sequence
cd /Users/anudeep-6653/Documents/project/spotify
git init
git add .
git commit -m "Initial commit: Spotify Wrapper"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/spotify-wrapper.git
git push -u origin main
```

---

## Troubleshooting

### If you accidentally committed secrets:
```bash
# Remove file from git history (keeps local copy)
git rm --cached backend/src/main/resources/application.properties
git commit -m "Remove sensitive config file"
git push
```

### If remote already exists:
```bash
git remote remove origin
git remote add origin https://github.com/YOUR_USERNAME/spotify-wrapper.git
```

### If push is rejected:
```bash
git pull origin main --rebase
git push origin main
```

---

## Next Steps After Push

1. Add collaborators (Settings → Collaborators)
2. Set up branch protection rules
3. Create GitHub Actions for CI/CD (optional)
4. Add badges to README (build status, etc.)
