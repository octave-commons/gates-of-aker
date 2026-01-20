# AGENTS

## Obsidian Primer
[[2026-01-15-roadmap]] 
we use wiki links to connect documents.
We view these documents in obsidian
Documents can have front matter
obsidian explicitly recognizes `tags:` properties in 
front matter for tagging documents.
### Obsidian docs
- https://blacksmithgu.github.io/obsidian-dataview/
## Related Documentation
- [[README.md]] - Project overview
- [[HACK.md]] - High-level vision and inspirations
- [[HOME]] - Obsidian home page
- [[docs/notes/planning/2026-01-15-roadmap.md]] - Sprint and milestone roadmap
- [[docs/notes/planning/2026-01-19-milestone3-3.5-progress-review.md]] - Milestone 3 & 3.5 detailed progress
- [[spec/2026-01-15-core-loop.md]] - Day/night cycle mechanics
- [[spec/2026-01-15-myth-engine.md]] - Myth engine

## Mission & Scope
- Maintain parity between backend simulation services, the React/Vite client, and supporting docs.
- This document governs every directory in the repo; add nested AGENTS.md files for narrower overrides.
- Favor incremental, reviewable changes; avoid sweeping refactors without prior discussion.
- Default to reproducibility: include exact commands, seeds, and script names in PR descriptions.

## Estimation & Story Points
- All planning and task estimates use **story points** instead of days/hours.
- Story points represent complexity, risk, and effort as a unified metric.
- 1 story point ≈ 2-3 hours of focused work for a typical task.
- Use story points for sprint planning, issue sizing, and roadmap coordination.
- When estimating, consider: complexity, dependencies, risk, and testing effort.

## Directory Map
- `/backend`: Clojure server exposing HTTP/WebSocket endpoints plus simulation logic in `fantasia.sim.*`.
  - `/backend/src`: Source code organized by namespace (no test files here).
  - `/backend/test`: All test files, matching namespace structure of `src`.
- `/web`: React 19 + Vite + TypeScript UI for viewing and steering the myth debugger.
- `/docs`: Notes, inbox thoughts, and long-form design commentary (`HACK.md`).
- `/docs/inbox`: Scratchpad ideas; do not assume stability but keep formatting readable.
- `/docs/notes`: Evergreen facts; prefer appending instead of in-place rewrites.
- Root scripts/config: `HACK.md`, this `AGENTS.md`, and future workspace-wide configs.

## Toolchains & Dependencies
- Backend: `clojure` CLI (deps.edn) with http-kit, reitit, cheshire, and project-local namespaces.
- Backend Java requirement: JDK 17+ recommended to match deps.edn features; set `JAVA_HOME` explicitly when using SDKMAN.
- Frontend: Node 20 LTS + npm; Vite handles dev/build, TypeScript is `strict` per tsconfig.
- UI dependencies kept minimal (React, ReactDOM); add new libs only when debug UI truly needs them.
- Package installs happen per-project (`npm install` inside `web`); avoid hoisting to repo root.
- No Cursor (.cursor/rules) or GitHub Copilot instruction files presently exist; encode all rules here.
- Preferred editors: IntelliJ + Cursive for Clojure, VS Code / WebStorm for TS; configure formatting to match sections below.
- Containerized workflows should mount repo root and forward ports 3000 (backend) and 5173 (web dev).

## Build / Run / Watch Commands
- Backend deps: `clojure -P` inside `/backend` to download dependencies.
- Backend dev server: `clojure -M:server` from `/backend` (prints http://localhost:3000).
- Backend hot reload loop: `clojure -X:watch-server` for filesystem watch + restart.
- Backend REPL: `clojure -M:server -r` to expose the same ns plus an interactive prompt.
- Backend packaging: no uberjar task yet; if needed, add `io.github.clojure/tools.build` scripts under `/backend/build.clj`.
- Frontend install: `npm install --prefix web` to pull dependencies (locks via package-lock.json).
- Frontend dev server: `npm run dev --prefix web` (Vite default port 5173, proxies manually via browser dev tools to backend).
- Frontend production build: `npm run build --prefix web` (runs `tsc -b` then `vite build`).
- Frontend preview: `npm run preview --prefix web` serves the dist bundle locally.
- Type-check only: `npm exec --prefix web tsc --noEmit` for quick verification without bundling.
- Clean artifacts: remove `web/node_modules` or `web/dist`; backend currently generates no build artifacts.
 - Watch canvas assets: use browser dev tools + React strict mode double invocations to catch side effects early.
- PM2 orchestration: Servers are managed by PM2 and automatically restart/recompile on code changes. Wait for `/healthz` to be available before testing changes.
- Backend and frontend are always running with hot reload - backend uses file watchers, frontend uses Vite HMR.

## Linting & Static Analysis
- Backend linting: use `clj-kondo --lint src` (install globally or via npx-like `clojure -M:clj-kondo` once alias is added).
- Static type checks rely on runtime invariants; prefer `clojure.spec`, `malli`, or custom predicate helpers in new code.
- Frontend linting currently manual; align with ESLint defaults if introduced later.
- For now, rely on TypeScript strict mode plus Vite build errors; fix `tsc` issues before committing.
- Keep imports ordered: built-ins (React, std libs) -> internal modules -> relative files.
- Enforce prettier-like style manually: two-space indentation, double quotes, trailing semicolons, single blank line between blocks.
- Add `.editorconfig` if formatting drift becomes a problem; otherwise cite this section in reviews.
- Document any temporary style deviations inline so future automation can encode them.

## Logging Control
- Backend uses `fantasia.dev.logging` with environment-based log levels via `LOG_LEVEL` env var.
- Frontend uses controlled console logging via `VITE_LOG_LEVEL` env var, automatically applied during tests.
- Available log levels (both stacks): `error`, `warn`, `info`, `debug` (default: `warn`).
- Setting log level:
  - Backend: `LOG_LEVEL=debug clojure -X:test` or export before running
  - Frontend: `VITE_LOG_LEVEL=debug npm test` or set in `.env` file
- Coverage reporting is configured to show summaries by default, not verbose file-by-file output.
- Frontend test scripts:
  - `npm test` - run tests with default logging (warn+)
  - `npm run test:quiet` - run tests with minimal output
  - `npm run test:debug` - run tests with full debug logging
  - `npm run test:coverage` - run tests with summary coverage report
  - `npm run test:coverage:verbose` - run tests with detailed coverage output

## Testing & Single-Test Guidance
- Backend test harness: `clojure -X:test` for full suite or use test runner patterns.
- Backend full run example: `clojure -X:test :patterns '[:all]'`.
- Backend single test example: `clojure -X:test :only 'fantasia.sim.core-test/tick-advances-world'`.
- Backend coverage report: `clojure -X:coverage` generates LCOV report in `backend/target/coverage/lcov.info`.
- Frontend test scripts: `npm test`, `npm run test:watch`, `npm run test:coverage`.
- Frontend single test example: `npm test -- App.test.tsx`.
- Use deterministic seeds where possible (`sim/reset` accepts `:seed`) to make manual repro scripts reliable.
- Record reproduction steps in `/docs/notes` when bugs require multi-stage setups.
- Do not skip adding tests once the harness exists; include at least one targeted test for every defect fix.
- For temporary manual verification, capture expected WS payloads via browser network tab and link them in PR notes.
- Keep CI scripts (once created) in `.github/workflows`; mirror commands listed here exactly.

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

## Docs & Knowledge Base

- `HACK.md` captures the creative vision; skim it before implementing new features to preserve thematic intent.
- `docs/inbox` is volatile; annotate entries with timestamps if you add exploratory notes.
- `docs/notes` should read like a changelog of facts; include API contracts, schemas, and non-obvious algorithms there.
- Keep AGENTS.md roughly ~150 lines; update it whenever commands or norms change.
- When adding future AGENTS files in subdirectories, explicitly reference this file to clarify inheritance.

## Cursor / Copilot Rules
- No Cursor or Copilot instruction files exist; any future ones must be reflected here verbatim.
- Until then, consider this AGENTS.md the single source of truth for agent behavior.
