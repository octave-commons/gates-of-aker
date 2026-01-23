
## Verification & Release Discipline
- Before opening a PR, run the applicable backend/frontend commands from the sections above and state the exact results.
- Capture manual repro steps (including lever values and seeds) so reviewers can follow along without guessing.
- When touching WebSocket flows, record one full payload exchange via browser dev tools and link the JSON in `/docs/notes`.
- Smoke-test the canvas UI by ticking, placing a shrine, adjusting levers, and setting a mouthpiece in a single session.
- Exercise `/sim/reset`, `/sim/tick`, and `/sim/run` endpoints after changing simulation code; include curl snippets in PR discussions.
- Prefer feature flags behind env vars when experimenting; remove dead flags before merging.
- Keep release builds reproducible by tagging the backend commit hash inside PR descriptions when shipping the frontend.
 - Coordinate port usage when running locally; backend default 3000 and frontend 5173 should stay open.
 - Document any long-running futures or watchers you add, including how to stop them cleanly.
 - When adding migrations or data backfills, script them and check those scripts into `/docs/notes` or `/scripts` once that dir exists.
 - Backend restarts and recompiles automatically on file changes; no manual restart needed after code changes.