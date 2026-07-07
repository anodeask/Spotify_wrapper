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

## Anti-Patterns
- Massive inline `$.html(` blocks in `frontend/js/*.js`.
- Duplicated loading/error markup across modules.

## Done Criteria
- Rendering comes from templates.
- Data mapping is explicit and resilient to missing fields.
- Existing CSS classes and UX semantics are preserved.
