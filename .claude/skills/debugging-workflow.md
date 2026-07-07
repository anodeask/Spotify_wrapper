# Skill: Debugging Workflow

## Goal
Debug issues quickly with low-risk, high-signal steps.

## Workflow
1. Reproduce from UI with minimal steps.
2. Identify failing layer:
   - frontend module
   - API wrapper
   - backend controller/service
3. Inspect logs and response payloads before changing code.
4. Fix smallest scope first, then expand if needed.
5. Validate end-to-end and check for regressions.

## Frequent Pitfalls in This Repo
- OAuth scope drift or stale token/session state.
- No active Spotify device mistaken for code failure.
- Runtime process drift (old backend process still running).
- Duplication drift when template-first convention is not followed.

## Practical Tips
- Restart services if runtime appears out-of-sync with source.
- Prefer meaningful, user-facing backend error messages.
- Keep changes localized and avoid opportunistic refactors.
