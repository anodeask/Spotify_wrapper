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

## Done Criteria
- No thrown exception for no-device user state.
- Devices tab consistently shows empty-state guidance.
