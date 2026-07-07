# Skill: Testing and Validation

## Goal
Validate functional changes quickly and safely.

## Backend
- Compile: `mvn -f backend/pom.xml clean compile`
- Test: `mvn -f backend/pom.xml test` (when feasible)

## Frontend
- Run app and verify target flow manually at `http://127.0.0.1:3000`.
- Check browser console for runtime errors after behavior changes.

## Static/Diagnostics
- Resolve lint/syntax diagnostics for edited files.
- Ensure no new errors introduced by patch.

## Change-Specific Validation
- For API contract changes: verify both backend payload and frontend usage.
- For polling changes: verify timer/listener cleanup and inactive-tab behavior.
- For devices changes: verify empty-state render when no active device exists.
