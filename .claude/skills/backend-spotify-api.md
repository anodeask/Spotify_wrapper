# Skill: Backend Spotify API Changes

## Goal
Implement or adjust Spotify-backed endpoints safely in Spring Boot.

## Procedure
1. Locate endpoint in `SpotifyController`.
2. Locate operation in `SpotifyService`.
3. Verify DTO shape in `backend/src/main/java/com/spotify/wrapper/dto`.
4. Add robust status handling for Spotify responses:
   - handle `204` no-content where applicable
   - parse and propagate Spotify error payloads
   - avoid null-cast assumptions
5. Keep user-facing messages meaningful via `SpotifyApiException` + global handler.

## Error Handling Rules
- Treat valid empty states (for example no devices) as successful empty responses.
- Throw `SpotifyApiException` for real Spotify/API failures.
- Include status and message in response payloads where mutations are involved.

## Validation
- Run: `mvn -f backend/pom.xml clean compile`
- Run tests when feasible: `mvn -f backend/pom.xml test`
- Verify endpoint manually against running app when behavior affects UX.
