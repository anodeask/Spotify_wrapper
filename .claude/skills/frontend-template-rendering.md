# Skill: Frontend Template-First Rendering

## Goal
Implement UI changes without building HTML strings inside feature JS modules.

## Rules
- Prefer Handlebars templates in `frontend/index.html`.
- Render through compiled templates in module `compileTemplates()` methods.
- Reuse shared `Utils` helpers in `frontend/js/config.js` for loading/error/empty/pagination states.

## Typical Steps
1. Add or update template in `frontend/index.html`.
2. Compile template in corresponding module.
3. Pass normalized data from module to template renderer.
4. Keep behavior and event delegation in JS, not markup string assembly.

## Project-Specific Reference
- Devices cards are rendered via `device-card-template` in `frontend/index.html` and compiled in `frontend/js/devices.js`.
- Keep this pattern when changing device UI: update template + data mapping, not string HTML construction.
- For progress-like values in templates, prefer `data-*` attributes and apply styles in JS after render when templated inline styles cause tooling/diagnostic issues.

## Anti-Patterns
- Massive inline `$.html(` blocks in `frontend/js/*.js`.
- Duplicated loading/error markup across modules.

## Done Criteria
- Rendering comes from templates.
- Data mapping is explicit and resilient to missing fields.
- Existing CSS classes and UX semantics are preserved.
- Device UI changes preserve delegated events (`.device-card`, `.transfer-btn`) and do not regress transfer playback actions.
