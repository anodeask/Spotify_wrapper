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
- For any polling changes (player, devices, or future modules): pause all polling while tab is inactive, and trigger an immediate reload when the tab becomes active before normal polling cadence continues.
- For device-loading paths (`/api/spotify/devices`, [frontend/js/spotify.js](frontend/js/spotify.js), [frontend/js/devices.js](frontend/js/devices.js)): treat no-active-device responses as empty device lists, not errors.
- For current track polling paths (`/api/spotify/current-track`, [frontend/js/spotify.js](frontend/js/spotify.js), [frontend/js/player.js](frontend/js/player.js)): do not surface transient Spotify `/me/player` 5xx as backend 500; prefer currently-playing fallback and return a structured empty playback state when no track is available.
- Theming: dark mode follows the device via `data-bs-theme` (set in the [frontend/index.html](frontend/index.html) `<head>` script). Use Bootstrap CSS variables (`--bs-body-bg`, `--bs-border-color`, `--bs-secondary-color`) instead of hardcoded light colors, and put dark-only overrides under `:root[data-bs-theme="dark"]` in [frontend/css/styles.css](frontend/css/styles.css). Check new UI in both modes.
- Accessibility: keep `<main>` landmark, a single `<h1>`, and sequential heading levels (tab titles `h2`, card titles `h3` styled with `.h6`). New tabs need an id, `aria-controls`, `aria-labelledby` on the panel, and must work with arrow-key navigation in [frontend/js/app.js](frontend/js/app.js). Any hover-revealed control needs a `:focus-within` equivalent. Use black text on Spotify green. Status containers must stay live regions.
- Keep backend endpoint behavior aligned with DTOs in [backend/src/main/java/com/spotify/wrapper/dto](backend/src/main/java/com/spotify/wrapper/dto).
- Preserve user-facing error quality: backend should propagate meaningful Spotify API status/messages and frontend should surface via in-app alerts (not browser alerts).
- Prefer minimal, targeted patches. Do not refactor unrelated files in the same change.

## Known Pitfalls
- OAuth and redirect/scopes can easily drift. Verify redirect URI and scopes in Spotify app settings and backend properties before debugging feature regressions.
- Playback operations often fail when no active Spotify device exists; treat this as a user-state issue first, not a code bug.
- Devices API may return no-content/empty payloads when no Spotify device is available; this is expected user state and should render the empty device UI, not trigger generic failure alerts.
- Spotify `/me/player` can intermittently return upstream 5xx. `current-track` should gracefully degrade via currently-playing fallback or a stable empty playback payload instead of hard failures.
- Bootstrap 5.3.2 does not re-tint `btn-outline-*` or `.link-secondary` for dark mode; they need explicit overrides (already in [frontend/css/styles.css](frontend/css/styles.css)). White text on `#1db954` fails WCAG contrast.
- The a11y audit reports modal dialog checks as "Not decided" unless run in driven mode, and flags hidden spinner `visually-hidden` text and unreadable CDN stylesheets as incomplete; these are not page defects.
- Queue and playback interactions are sensitive to token/session freshness. Re-auth can resolve false negatives during debugging.

## Validation Checklist Before Finishing
- For backend changes, run `mvn test` in [backend](backend) when feasible.
- For frontend changes, verify behavior manually at `http://127.0.0.1:3000` against a running backend.
- If changing API contracts, update both backend controller/service flow and frontend API usage.
