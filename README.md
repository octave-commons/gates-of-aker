# Fantasia

Fantasia is a hybrid colony simulator and myth debugger that pairs a Clojure backend simulation engine with a React/Vite front-end for observing and steering runs. This README orients contributors around the repo layout, local setup, and the conventions summarized in `AGENTS.md`.

## Project Layout
- `backend/`: Clojure server (`fantasia.server`) and simulation logic under `fantasia.sim.*`.
- `web/`: React 19 + Vite UI for viewing ticks, levers, and trace logs.
- `docs/`: Design notes, inbox sketches, and longer references (`HACK.md`, `docs/notes`).

See `AGENTS.md` for the authoritative coding standards, naming conventions, and test expectations.

## Prerequisites
- Java 17+ (set `JAVA_HOME` accordingly) for the Clojure tools.
- Clojure CLI (`clojure` command) with access to Maven Central.
- Bun for the frontend (https://bun.sh). Install via:
  ```bash
  curl -fsSL https://bun.sh/install | bash
  ```

## Getting Started
1. **Clone** the repo and open a shell at the repo root.
2. **Install backend deps:**
   ```bash
   (cd backend && clojure -P)
   ```
3. **Install frontend deps:**
    ```bash
    bun install --prefix web
    ```

## Running the Stacks
### Backend
- Dev server: `clojure -M:server` (serves HTTP+WS on `http://localhost:3000`; also starts nREPL on port 7888).
- Hot reload loop: `clojure -X:watch-server` (restarts when `src/` or `deps.edn` changes).
- Custom nREPL port: Set `NREPL_PORT` environment variable before starting (e.g., `NREPL_PORT=7889 clojure -M:server`).

#### Using nREPL for Dynamic Interaction
The backend includes an nREPL server for interactive development and dynamic simulation manipulation. Connect using your preferred editor (CIDER, Calva, conjure) or CLI:

**CLI connection:**
```bash
# Start the backend server (nREPL runs on port 7888 by default)
clojure -M:server

# In another terminal, connect to the running nREPL
clj -R:nrepl
# or
clojure -R:nrepl
```

**Editor connections:**
- **Emacs/CIDER**: Use `cider-connect` and specify `localhost:7888`
- **VS Code/Calva**: Connect to running REPL on port 7888
- **Neovim/Conjure**: Configure to connect to `localhost:7888`

**Common nREPL operations:**
```clojure
(require '[fantasia.sim.tick :as sim])

; Inspect current simulation state
(sim/get-state)

; Manually advance the simulation
(sim/tick! 1)

; Reset the world with specific seed
(sim/reset-world! {:seed 42 :tree-density 0.1})

; Set simulation levers
(sim/set-levers! {:ollama-model "llama2" :belief-propagation-rate 0.5})

; Place entities
(sim/place-shrine! [10 10])
(sim/place-wolf! [5 5])
(sim/place-tree! [7 8])

; Access shared atoms
(require '[fantasia.server :as server])
@server/*clients
@server/*runner
```

### Frontend
- Dev server: `bun run dev` (from `web/` directory, Vite on port 5173, expected to talk to backend on 3000).
- Build: `bun run build` (runs `tsc -b` then `vite build`).
- Preview: `bun run preview` to serve the production bundle locally.

## Docker Compose
- Prereq: Docker Engine/Desktop 20.10+ with Compose v2.
- Validate syntax via `docker compose config` in the repo root.
- Launch both services with hot reload via:
  ```bash
  docker compose up --build
  ```
  - Backend container (`backend`) runs `clojure -M:server` and binds `localhost:3000`.
  - Frontend container (`web`) runs `bun run dev -- --host 0.0.0.0 --port 5173` and binds `http://localhost:5173`.
- Stop with `Ctrl+C`; add `-d` to detach. Compose mounts the repo plus caches (`~/.m2`, `web/node_modules`) so edits on the host refresh inside containers.
- Override the UI’s API origin (default `http://localhost:3000`) by setting `VITE_BACKEND_ORIGIN` before `docker compose up`; the value propagates into the Vite dev server.

## Coding Standards
- **Backend:** keep IO (`fantasia.server`) separate from pure simulation namespaces, prefer `cond->`, guard all external input, and broadcast helpful WS ops (`{:op "error" ...}`) instead of throwing.
- **Frontend:** React function components only, hooks over classes, strict typing (tuples, discriminated unions), stable keys, and controlled form inputs. WebSocket usage should always flow through `WSClient`.
- Shared guidelines (imports, formatting, logging, serialization, docs) live in `AGENTS.md`; review them before opening PRs.

## Documentation
- `HACK.md` captures the thematic goals—skim before implementing major features to preserve tone.
- `docs/inbox` stores volatile experiments; timestamp any additions.
- `docs/notes` acts as the changelog of facts (API contracts, seeds, payload examples). Append rather than rewrite when possible.

### Key Design Documents
- [[docs/notes/planning/2026-01-15-roadmap.md]] - Sprint and milestone roadmap
- [[docs/notes/planning/2025-01-15-mvp.md]] - MVP definition
- [[docs/notes/design/2026-01-15-world-schema.md]] - World schema (MVP authoritative shape)
- [[spec/2026-01-15-core-loop.md]] - Day/night cycle and champion mechanics
- [[spec/2026-01-15-myth-engine.md]] - Myth engine and miracle pipeline
- [[docs/notes/design/2026-01-15-aker-boundry-control-flow.md]] - Card system and Aker boundary

### Milestone Specifications
- [[spec/2026-01-17-milestone2-walls-pathing-build-ghosts.md]] - Walls, pathing, and build ghosts
- [[spec/2026-01-17-milestone3-colony-core.md]] - Colony job loop

### Task List
- [[docs/tasks/001-belief-layer.md]] - Belief layer implementation
- [[docs/tasks/002-prestige-retirement.md]] - Prestige and retirement system
- [[docs/tasks/003-deity-moves.md]] - Deity moves and powers
- [[docs/tasks/004-event-director.md]] - Event director
- [[docs/tasks/005-aker-scheduler.md]] - Aker day/night scheduler
- [[docs/tasks/006-sim-spatial.md]] - Spatial systems and hex math
- [[docs/tasks/007-sim-agents.md]] - Agent simulation
- [[docs/tasks/008-sim-institutions.md]] - Institutions and groups
- [[docs/tasks/009-events-runtime.md]] - Events runtime
- [[docs/tasks/010-world-snapshot.md]] - World snapshot system
- [[docs/tasks/011-tick-engine.md]] - Tick engine

### Design Notes
- [[docs/notes/design/2026-01-15-minimal-schemas.md]] - Minimal schemas
- [[docs/notes/design/2026-01-15-sim-modules.md]] - Simulation modules
- [[docs/notes/design/2026-01-15-card-format.md]] - Card format spec
- [[docs/notes/design/2026-01-15-resolution-system.md]] - Resolution system
- [[docs/notes/design/2026-01-15-retirement-distillation.md]] - Retirement distillation
- [[docs/notes/design/2026-01-15-aker-the-day-night-hinge-as-the-prestige-clock.md]] - Aker as prestige clock

### Brainstorming
- [[docs/notes/brainstorming/2026-01-11-alright-here-s-a-minimal-working-myth-engine-in-a-box-scaffo.md]]
- [[docs/notes/brainstorming/2026-01-15-oh-that-gives-me-a-great-idea-for-a-game-mechanic-i-was-thin.md]]
- [[docs/notes/brainstorming/2026-01-15-so-they-had-story-tells-we-have-deities-all-of-them-are-comp.md]]
- [[docs/notes/brainstorming/2026-01-15-yea-let-s-do-it-major-deities-get-3-abilities-each-night-abi.md]]
- [[docs/notes/brainstorming/2026-01-15-you-can-keep-the-myth-energy-without-pinning-the-project-to-.md]]
- [[docs/notes/brainstorming/2026-01-15-you-re-describing-the-exact-gap-between-a-story-optimizer-an.md]]
- [[docs/notes/brainstorming/2026-01-15-the-cleanest-third-pillar-names.md]]

## Contributing Workflow
1. Branch from `main` and keep changes focused and reviewable.
2. Follow the build/run/test commands above before opening a PR.
3. Record any WS payload captures or manual verification steps in `docs/notes` or the PR body.
4. Update `AGENTS.md` or this README when workflows or conventions change.

Questions or ideas? Capture them in `docs/inbox` or open a discussion in your PR description so future agents can follow the breadcrumbs.
