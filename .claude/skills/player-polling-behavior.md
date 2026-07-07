# Skill: Player Polling Behavior

## Goal
Preserve and extend player polling without regressions.

## Required Behavior
- Pause polling while tab is inactive.
- On tab activation, force immediate reload before normal polling cadence continues.
- Schedule a post-completion refresh roughly 2 seconds after expected track end.

## Touch Points
- `frontend/js/player.js`
  - visibility handler
  - update loop
  - completion timeout scheduling and cleanup

## Guardrails
- Do not reintroduce duplicate polling loops.
- Always clear timers/listeners in teardown paths.
- Keep `isUpdating` and queue update cadence behavior intact.

## Manual Checks
- Switch browser tab away/back and verify no hidden-tab polling.
- Verify immediate refresh on return.
- Verify next track appears quickly after track completion.
