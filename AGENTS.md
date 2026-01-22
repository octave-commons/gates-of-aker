
- [[OBSIDIAN]]  -> `OBSIDIAN.md` - Obsidian Specific Instructions for knowledge graphing and plugins
- [[README]] -> `README.md`- Project overview, basic setup and testing commands, explains how to use it and what it is for.
- [[HACK.md]] - High-level vision and inspirations
- [[HOME]] - Obsidian home page, day to day thinking and note taking. Obsidian data views welcome.
- [[2026-01-15-roadmap]] - Most current roadmap
- [[docs/notes/planning/2026-01-15-roadmap.md]] - Sprint and milestone roadmap
- [[docs/notes/planning/2026-01-19-milestone3-3.5-progress-review.md]] - Milestone 3 & 3.5 detailed progress
- [[spec/2026-01-15-core-loop.md]] - Day/night cycle mechanics
- [[spec/2026-01-15-myth-engine.md]] - Myth engine
- [[MISSION]]
- [[ESTIMATION]]
- [[DIRECTORIES]]
- [[TOOLCHAINS]]
- [[BUILD]]
- [[LINTING]]
- [[LOGGING]]
- [[TESTING]]
- [[orphaned files output]]
### Backend Test Directory Structure
- All test files live in `/backend/test/` and mirror the namespace structure of `/backend/src/`.
- Test namespace naming: `fantasia.some-ns-test` corresponds to `fantasia/some_ns.clj`.
- Test file naming: Use `*_test.clj` for test namespaces (e.g., `agents_test.clj`).
- No test files or helper test directories should exist in `/backend/src/`.
- ECS test namespace: `fantasia.sim.ecs.*-test` for ECS-specific tests in `/backend/test/fantasia/sim/ecs/`.

## Backend (Clojure) Style Guide
- Namespace layout: `fantasia.server` handles IO, `fantasia.sim.*` stays pure and data-focused; keep new IO separate from domain logic.
- Use `ns` requires with grouped lib -> internal ordering; alias standard libs (`clojure.string` as `str`, etc.).
- Pure functions first: functions that manipulate maps/vectors should not mutate atoms; return new state and let callers swap!.
- Shared atoms such as `*clients` and `*runner` should stay at top-level, be private when possible, and mutated only through helper fns.
- Keep data literal-friendly; prefer vectors for ordered collections, sets for membership, keywords for enums.
- Guard external input via helpers like `read-json-body`; always keywordize incoming JSON before use.
- Provide helpful error ops over WebSocket (`{:op "error" :message ...}`) instead of throwing exceptions.
- HTTP handlers must return the `json-resp` structure with explicit `access-control-allow-*` headers for browser compatibility.
- Keep long `case` blocks exhaustive; fall back to sending an error op rather than silently ignoring messages.
- Schedule work using `future` only when necessary; always reset `:running?` flags in `finally` blocks as `start-runner!` does.
- Utilities like `normalize-token`, `store-event-token`, and `rumorize-token` show preferred naming (`verb-noun`), returning updated maps.
- Use `cond->` for data assembly, `reduce` for accumulation, and descriptive local names (e.g., `listener'`) for updated records.
- Numerical helpers (`clamp01`, `rand-*`) should stay in modules where they are needed; avoid scattering duplicates.
- Keep geometry helpers (`in-bounds?`, `neighbors`) pure and side-effect free.
- When writing watchers (see `fantasia.dev.watch`), log lifecycle events and debounce restarts to avoid thrashing.
- Additional namespaces should mirror existing folder depth: e.g., `backend/src/fantasia/sim/claims.clj`.
- Document non-obvious algorithms (belief propagation, rumor spreading) with short block comments above function definitions.
- CLI entry points should live in a `-main` function with `:gen-class`; parse CLI args with `clojure.tools.cli` if they expand.
- For serialization, keep `json/generate-string` usage centralized; avoid building JSON strings manually.
- Validate inbound numeric ranges (0.0–1.0) before storing them in world state; clamp values when they originate from UI levers.
   - Use `let` bindings for clarity, especially before nested threads; avoid one-letter locals unless inside trivial map destructuring.
   - Use `fantasia.dev/logging` functions (log-error, log-warn, log-info, log-debug) instead of direct `println` for controllable output.

### Spatial Facets & Memory System
- Namespace: `fantasia.sim.spatial_facets` - Facet-based query system for agent perception and memory
- Namespace: `fantasia.sim.memories` - Memory creation, decay, and query functions
- Embedding cache: Stores word vectors for semantic similarity computation (currently uses stub vectors)
- Entity facets: M3 entity types (wolf, bear, tree, fruit, stockpile, campfire, warehouse, wall, statue/dog, agent types)
- Cosine similarity: Computes similarity between facet embeddings and query concepts
- Query function: `query-concept-axis!` returns score in [-1, 1] for concept presence/absence
- Memory functions: `create-memory!`, `decay-memories!`, `get-memories-in-range`, `remove-memory!`, `clean-expired-memories!`
- Memory structure: Includes id, type, location, created-at tick, strength, decay-rate, entity-id, and facets
- Facet limit: Default 16 facets per query, configurable via world `:facet-limit`, maximum 64
- Vision radius: Default 10 tiles for facet gathering, configurable per agent
- Logging: Uses `[FACET:QUERY]` and `[MEMORY:*]` tags for observability


## Frontend (TypeScript + React) Style Guide
- Modules use ES modules with explicit extensions: third-party imports first, internal absolute/relative imports second.
- Keep React components as functions; prefer hooks (`useState`, `useEffect`, `useMemo`, `useRef`) over class patterns.
- Derive helpers locally (e.g., `fmt`, `clamp01`, `hasPos`) and keep them type-safe; export only what other modules need.
- Always type app state; use tuple types (`[number, number]`) and discriminated unions for WS messages, as in `ws.ts`.
- Favor `const` for bindings and arrow functions for event handlers; keep handlers defined near usage to ease reading.
- Use inline styles only for debug UI; if/when styling grows, switch to CSS modules or Tailwind but document the choice here.
- Restrict direct DOM access to refs; check for `null` before using canvas contexts or bounding rects.
- When updating state derived from previous values, pass an updater function to `setState` to avoid stale closures.
- Maintain deterministic render order: sort arrays before `.map` if visual stability is required.
- Keep canvas drawing imperative but pure with respect to React state (no external mutable globals).
- Manage WebSocket lifecycle through the `WSClient` helper; never instantiate raw `WebSocket` objects in components.
- All outbound messages should follow backend op names exactly (`snake_case` for payload keys even in TS).
- Guard optional values (e.g., `snapshot?.agents ?? []`) before iteration to avoid runtime crashes in strict mode.
- Avoid `any`; if data is dynamic, wrap it in `Record<string, unknown>` and downcast safely.
- Keep TypeScript strict mode enabled; do not relax compiler options without agreement.
- Derive memos (`useMemo`) only when expensive; do not over-memoize trivial computations.
- Prefer early returns in handlers for readability (see `placeShrineAtSelected`).
- Use template literals for user-facing strings; keep debug text short and high-signal.
- All arrays rendered in JSX must have stable keys (IDs, not indexes) unless data is static.
- Snapshots displayed in `<pre>` should be run through `JSON.stringify` with spacing for readability; limit size when logs get large.
- Keep UI state in a single component (`App`) until reuse pressure justifies extracting children; favour plain props over context for now.
- Manage slider/range inputs as controlled components, storing numbers not strings; clamp values before sending to backend.
- File naming: PascalCase for components (`App.tsx`), camelCase for utilities (`ws.ts`).

## Shared Conventions & Error Handling
- Naming: kebab-case for files (Clojure namespaces), camelCase for TS variables, SCREAMING_SNAKE_CASE reserved for constants.
- Imports: absolute first (stdlib), third-party libs, then relative paths; leave a blank line between groups.
- Error handling: catch exceptions at IO boundaries, return informative JSON payloads, and surface user-friendly messages in UI banners.
- Logging: use `fantasia.dev.logging` on backend (log-error, log-warn, log-info, log-debug), controlled via `LOG_LEVEL` env var.
- Frontend logging: console methods are automatically controlled during tests via `VITE_LOG_LEVEL` env var; no changes needed in application code.
- Configuration: prefer env vars or CLI flags; do not hardcode secrets or ports beyond the documented 3000/5173 defaults.
- Data contracts: keep server + UI payloads in sync; document field names when adding new ops to `/docs/notes`.
- Concurrency: guard shared atoms with `swap!`; avoid manual locking.
- Serialization: always encode WS payloads via `cheshire` and decode them once before branching on `:op`/`op`.
- Comments: short, high-signal, describing "why" rather than "what"; remove stale comments promptly.
- Git hygiene: no generated assets in commits (`web/dist`, `web/node_modules`), no direct pushes to `main` without review.
- Dependency changes: update lockfiles (`package-lock.json`) and mention new libs in PR bodies.
- Documentation updates: whenever behavior changes, add a line to `/docs/notes` summarizing the impact.
- Security: treat WS inputs as user-controlled; never trust UI-sent numbers without validation.
- Performance: profile long-running `sim/tick!` operations before optimizing; measure first.
- Internationalization: not required yet; keep strings central for future extraction.

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

## Docs & Knowledge Base

- `HACK.md` captures the creative vision; skim it before implementing new features to preserve thematic intent.
- `docs/inbox` is volatile; annotate entries with timestamps if you add exploratory notes.
- `docs/notes` should read like a changelog of facts; include API contracts, schemas, and non-obvious algorithms there.
- Keep AGENTS.md roughly ~150 lines; update it whenever commands or norms change.
- When adding future AGENTS files in subdirectories, explicitly reference this file to clarify inheritance.

## Cursor / Copilot Rules
- No Cursor or Copilot instruction files exist; any future ones must be reflected here verbatim.
- Until then, consider this AGENTS.md the single source of truth for agent behavior.
