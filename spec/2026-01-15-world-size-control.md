# Spec: Configurable World Size Control

## Prompt
Add a control in the React/Vite debugger that lets operators change the simulation world dimensions. The backend must accept the requested size and propagate it through snapshots so the canvas and logic stay in sync.

## Code References
- `backend/src/fantasia/sim/tick.clj:19-58` — `initial-world` currently hard-codes `:size [20 20]`, tree placement ranges, and agent spawn coordinates.
- `backend/src/fantasia/server.clj:68-137` — WebSocket `reset` op plus `/sim/reset` HTTP handler that calls `sim/reset-world!`.
- `backend/src/fantasia/sim/world.clj:5-30` — Snapshot payload sent to the UI (needs to expose `:size`).
- `web/src/App.tsx:70-180` — Canvas rendering and pointer math assume a fixed 20×20 grid; reset button does not convey size.
- `web/src/ws.ts:1-44` — Client-side WS helper that serializes outbound messages.

## Existing Issues / PRs
- None referenced; no open items mentioning world-size controls were found locally.

## Definition of Done
1. Backend `reset` pathways accept an optional `size` vector, validate it, and build the world with those dimensions (default remains 20×20 when unspecified).
2. Snapshot payloads include the resolved size so the frontend can render/convert clicks accurately.
3. The React UI presents inputs for width/height and sends them during reset; canvas drawing plus hit-testing adapts to the live `snapshot.size` value.
4. Manual verification instructions (or automated coverage if added later) describe how to reset with multiple sizes and observe the canvas resizing correctly.

## Requirements & Notes
- Keep the size bounded (e.g., clamp to a reasonable min/max) to preserve performance and ensure placement logic stays valid.
- Agent spawn ranges, shrine placement rules, and procedural trees must respect the requested bounds.
- Preserve backwards compatibility for existing clients that only send a seed.
- Document the new control/params so future contributors know how to request specific world dimensions.
