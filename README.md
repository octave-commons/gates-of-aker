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
- Node.js 20 LTS and npm for the frontend.

## Getting Started
1. **Clone** the repo and open a shell at the repo root.
2. **Install backend deps:**
   ```bash
   (cd backend && clojure -P)
   ```
3. **Install frontend deps:**
   ```bash
   npm install --prefix web
   ```

## Running the Stacks
### Backend
- Dev server: `clojure -M:server` (serves HTTP+WS on `http://localhost:3000`).
- Hot reload loop: `clojure -X:watch-server` (restarts when `src/` or `deps.edn` changes).
- REPL + server: `clojure -M:server -r` for interactive tinkering.

### Frontend
- Dev server: `npm run dev --prefix web` (Vite on port 5173, expected to talk to backend on 3000).
- Build: `npm run build --prefix web` (runs `tsc -b` then `vite build`).
- Preview: `npm run preview --prefix web` to serve the production bundle locally.

## Docker Compose
- Prereq: Docker Engine/Desktop 20.10+ with Compose v2.
- Validate syntax via `docker compose config` in the repo root.
- Launch both services with hot reload via:
  ```bash
  docker compose up --build
  ```
  - Backend container (`backend`) runs `clojure -M:server` and binds `localhost:3000`.
  - Frontend container (`web`) runs `npm run dev -- --host 0.0.0.0 --port 5173` and binds `http://localhost:5173`.
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

## Contributing Workflow
1. Branch from `main` and keep changes focused and reviewable.
2. Follow the build/run/test commands above before opening a PR.
3. Record any WS payload captures or manual verification steps in `docs/notes` or the PR body.
4. Update `AGENTS.md` or this README when workflows or conventions change.

Questions or ideas? Capture them in `docs/inbox` or open a discussion in your PR description so future agents can follow the breadcrumbs.
