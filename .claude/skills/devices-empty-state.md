# Skill: Devices Empty-State Handling

## Goal
Handle no-device conditions gracefully and avoid generic error UX.

## Expected UX
- If no Spotify devices are available/active, show empty-state UI.
- Do not show generic "Something went wrong" error for valid no-device state.

## Backend Requirements
- `SpotifyService.getDevices` should treat:
  - `204 No Content`
  - blank response body
  - null `devices` array
  as empty list success.

## Frontend Requirements
- `SpotifyAPI.getDevices` should defensively map known no-active-device errors to `{ devices: [] }`.
- `DevicesModule.displayDevices` should render empty message when list is empty.
- Device cards should be rendered from `device-card-template` in `frontend/index.html`, not inline HTML in `frontend/js/devices.js`.
- Volume progress width should be applied after render from `data-volume` values (for example via `.js-device-volume-bar`), not from templated inline style expressions.
- Pause devices polling while tab is inactive.
- On tab activation, trigger immediate `loadDevices()` refresh before normal cadence continues.

## Implementation Notes (Current Repo)
- `frontend/js/devices.js` compiles templates in `compileTemplates()` and renders cards through `renderDevice()`.
- Backend devices flow now normalizes no-device cases to empty list before returning JSON.
- `frontend/js/devices.js` uses a visibility listener to gate background polling and resume with immediate refresh.

## Done Criteria
- No thrown exception for no-device user state.
- Devices tab consistently shows empty-state guidance.
- Devices card UI continues rendering through Handlebars template path.
