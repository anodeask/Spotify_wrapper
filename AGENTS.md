# AGENTS.md

## Purpose
Instructions for AI coding agents working in this repository.

## Start Here
- Read [README.md](README.md) for full setup and feature behavior.
- Read [QUICK_START.md](QUICK_START.md) for fast local bootstrapping.
- Read [CONTEXT.md](CONTEXT.md) for architecture and module-level overview.
- Use [plan.md](plan.md) for current project direction and pending work.

## Tech and Runtime
- Backend: Java 17, Spring Boot 3.2.x, Maven, MySQL.
- Frontend: static SPA with jQuery + Bootstrap + Handlebars, served on port 3000.
- Default local ports:
  - Backend: 9090
  - Frontend: 3000

## Common Commands
Run from repository root unless noted.

- Start both services:
  - `./start.sh`
- Check service status / stop services:
  - `./stop.sh status`
  - `./stop.sh stop`
- Backend dev run (from [backend](backend)):
  - `mvn -f pom.xml org.springframework.boot:spring-boot-maven-plugin:run`
- Backend compile/test (from [backend](backend)):
  - `mvn clean compile`
  - `mvn test`
- Frontend dev run (from [frontend](frontend)):
  - `npm install`
  - `npm start`

## Code Map
- Backend source: [backend/src/main/java/com/spotify/wrapper](backend/src/main/java/com/spotify/wrapper)
  - Controllers: [backend/src/main/java/com/spotify/wrapper/controller](backend/src/main/java/com/spotify/wrapper/controller)
  - Services: [backend/src/main/java/com/spotify/wrapper/service](backend/src/main/java/com/spotify/wrapper/service)
  - Global API error mapping: [backend/src/main/java/com/spotify/wrapper/exception](backend/src/main/java/com/spotify/wrapper/exception)
- Frontend source: [frontend/js](frontend/js)
  - API wrapper and constants: [frontend/js/config.js](frontend/js/config.js), [frontend/js/spotify.js](frontend/js/spotify.js)
  - Main feature modules: [frontend/js/player.js](frontend/js/player.js), [frontend/js/search.js](frontend/js/search.js), [frontend/js/library.js](frontend/js/library.js), [frontend/js/devices.js](frontend/js/devices.js)

## Working Conventions
- Keep frontend code modular by feature file under [frontend/js](frontend/js).
- Mandate template-first rendering in frontend: do not construct HTML UI blocks directly in JavaScript files under [frontend/js](frontend/js); define/reuse Handlebars templates in [frontend/index.html](frontend/index.html) and render via shared Utils helpers.
- For player polling changes in [frontend/js/player.js](frontend/js/player.js): preserve active-tab gating (no background polling while tab is inactive) and preserve the post-completion refresh trigger (poll ~2 seconds after expected track end).
- For device-loading paths (`/api/spotify/devices`, [frontend/js/spotify.js](frontend/js/spotify.js), [frontend/js/devices.js](frontend/js/devices.js)): treat no-active-device responses as empty device lists, not errors.
- Keep backend endpoint behavior aligned with DTOs in [backend/src/main/java/com/spotify/wrapper/dto](backend/src/main/java/com/spotify/wrapper/dto).
- Preserve user-facing error quality: backend should propagate meaningful Spotify API status/messages and frontend should surface via in-app alerts (not browser alerts).
- Prefer minimal, targeted patches. Do not refactor unrelated files in the same change.

## Known Pitfalls
- OAuth and redirect/scopes can easily drift. Verify redirect URI and scopes in Spotify app settings and backend properties before debugging feature regressions.
- Playback operations often fail when no active Spotify device exists; treat this as a user-state issue first, not a code bug.
- Devices API may return no-content/empty payloads when no Spotify device is available; this is expected user state and should render the empty device UI, not trigger generic failure alerts.
- Queue and playback interactions are sensitive to token/session freshness. Re-auth can resolve false negatives during debugging.

## Validation Checklist Before Finishing
- For backend changes, run `mvn test` in [backend](backend) when feasible.
- For frontend changes, verify behavior manually at `http://127.0.0.1:3000` against a running backend.
- If changing API contracts, update both backend controller/service flow and frontend API usage.
