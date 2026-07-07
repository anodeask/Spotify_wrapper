# Skill: Repository Architecture

## Goal
Quickly orient to code ownership and where to implement a requested change.

## Key Paths
- Backend root: `backend/src/main/java/com/spotify/wrapper`
- Controllers: `backend/src/main/java/com/spotify/wrapper/controller`
- Services: `backend/src/main/java/com/spotify/wrapper/service`
- DTOs: `backend/src/main/java/com/spotify/wrapper/dto`
- Frontend modules: `frontend/js`
- Frontend templates: `frontend/index.html`

## Mapping Requests to Files
- API behavior/contract: controller + service + DTO.
- Spotify API request/response translation: service + DTO.
- UI behavior/state: frontend module under `frontend/js`.
- Rendering/layout markup: Handlebars templates in `frontend/index.html`.
- Shared UI helpers: `frontend/js/config.js` (`Utils`).

## Done Criteria
- Change is implemented in the correct layer(s).
- No unrelated module refactor in same patch.
- Any contract change is mirrored in frontend/backend.
