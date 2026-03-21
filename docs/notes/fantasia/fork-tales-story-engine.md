# Fork Tales Story Engine

## Summary

Gates of Aker now has a dedicated Fork Tales continuation path in the backend.

### What changed

- Added `GET /api/fork-tales/status` to report storyteller readiness, chapter count, and latest chapter metadata.
- Added `POST /api/fork-tales/continue` to generate the next Fork Tales chapter from current game state.
- Added `fantasia.story.fork-tales` for chapter loading, prompt construction, chapter numbering, dry-run support, and chapter writing.
- Added `fantasia.llm.openai-compat` for proxy-backed `/v1/chat/completions` requests.
- Added storyteller config defaults in `fantasia.config` with env-backed proxy values:
  - `OPEN_HAX_OPENAI_PROXY_URL`
  - `OPEN_HAX_OPENAI_PROXY_AUTH_TOKEN`
- Initial ECS agents now spawn with Fork Tales names:
  - Duct
  - Null
  - Patch
  - Sei
  - 莉津律宗利都
- ECS snapshot projection now includes agent names so the existing UI can display the Fork Tales cast.
- Written chapters are also appended into `.myth/myths.jsonl` as persistent story memory records.

### Default paths

- Fork Tales narrative directory default: `../../../../vaults/fork_tales/narrative` (relative to `backend/`)
- Myth memory default: `../.myth/myths.jsonl` (relative to `backend/`)

### Frontend control surface

- Added `web/src/components/ForkTalesPanel.tsx` with:
  - storyteller status refresh
  - dry-run toggle
  - operator prompt textarea
  - generate/write action
  - preview/result display with chapter path and text
- Added dedicated route `#/fork-tales` / `/fork-tales` via `ForkTalesPage` and a `Fork Tales` main-menu entry.
- Also embedded the panel inside the simulation route so story tooling remains available in-world.

### History browser

- Added `GET /api/fork-tales/history` for recent chapter summaries.
- Added `GET /api/fork-tales/history/:chapter-number` for full chapter inspection.
- The Fork Tales panel now includes:
  - recent chapter list
  - latest-chapter auto-selection
  - full chapter detail viewer
  - preview/write result card separate from historical browsing

### Deployment/runtime notes

- Backend now respects `PORT` from the environment instead of assuming `3000` only.
- All JSON responses now emit CORS headers so the Vite frontend can talk to a backend on a different local port.
- Current live local deploy was started with:
  - backend: `http://127.0.0.1:3300`
  - frontend: `http://127.0.0.1:5173/fork-tales`
- PM2 process names:
  - `gates-backend`
  - `gates-frontend`

### Verification

- Backend test suite: `cd backend && clojure -X:test`
- Frontend panel test: `npm test --prefix web -- src/components/__tests__/ForkTalesPanel.test.tsx`
- Frontend build: `npm run build --prefix web`
- Manual dry run against real proxy succeeded and resolved the next chapter path under `devel/vaults/fork_tales/narrative/` without writing.
- Browser verification used a dedicated preview on `http://127.0.0.1:4173/fork-tales` with screenshot artifact `/tmp/gates-fork-tales-page-verified.png`.
- Live deploy verification now also succeeded on `http://127.0.0.1:5173/fork-tales` against backend `http://127.0.0.1:3300`, with screenshot artifact `/tmp/gates-fork-tales-live-deploy.png`.
