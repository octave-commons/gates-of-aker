## Backend Test Directory Structure
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