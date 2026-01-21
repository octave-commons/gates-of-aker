# 2026-01-20 Civic Growth + Scribes

## Passive Mood Buffs
Agents now receive passive mood boosts from pleasant environments without explicit actions:
- Houses: +0.003 per tick when within 2 tiles
- Temple: +0.006 per tick when within 2 tiles
- School: +0.004 per tick when within 2 tiles
- Library: +0.005 per tick when within 2 tiles
- Trees: +0.002 per tick when within 2 tiles
- Constants added in `backend/src/fantasia/sim/constants.clj`
- Environment sampling logic in `backend/src/fantasia/sim/agents.clj:update-needs`
- Mood feedback thoughts added for temples, libraries, schools

## Building Updates
- Added `:house`, `:temple`, `:school`, `:library` to `jobs/build-structure-options`
- Added civic structures to `jobs/unique-structures` set
- Library added to `jobs/job-provider-config` with `:job/scribe` (max-jobs 1)
- House/temple/school/library completions in `jobs:complete-build-structure!`
- Library initialization creates empty `:books {}` and `:books-list []` in world state

## Scribe Job + Books (Ollama + Meta-Myths Integration)
- New job type `:job/scribe` (priority 40) added
- Library generates scribe jobs via `jobs/providers:generate-provider-scribe-jobs!`
- Scribe completion via `scribes:complete-scribe-job!`:
  - Creates placeholder book immediately to avoid blocking ticks
  - Selects up to 3 relevant traces from culture
  - Generates book title (short, evocative title) from facets
  - Generates book text via async Ollama prompt (3-5 sentences, mythological story)
  - Passes random ancient myths (up to 5) from `myths.jsonl` as context to Ollama
  - Creates book record with `:book/id`, `:trace-ids`, `:title`, `:text`, `:facets`, `:created-at`, `:created-by`, `:read-count`, `:generating?`, `:completed-at`
  - Stores book in world state `:books` map and `:books-list` vector
  - Async future updates world state and saves myth to `myths.jsonl` file when complete
  - Ollama client via `clj-http` to `http://localhost:11434/api/generate` (model: `qwen3:4b`)
  - Fallback text provided if Ollama unavailable
  - Myths persist across world restarts in JSONL format: `{"title": "...", "text": "...", "facets": [...], "created-at": timestamp}`

## Frontend Updates
- Render new structures in `web/src/components/SimulationCanvas.tsx`:
  - Temple: blue with pillars, shrine-like appearance
  - School: wood/brown with roof detail
  - Library: dark brown with book symbol
- Job labels added in `web/src/components/JobQueuePanel.tsx` and `web/src/components/AgentCard.tsx` for `:job/scribe`
- New component `web/src/components/LibraryPanel.tsx`:
  - Lists books sorted by creation date
  - Shows title, tick, source count, read count
  - Displays selected book content with scroll
- Books broadcast to frontend via new `:op "books"` message
- App state handles `books` map and `selectedBookId` selection
- LibraryPanel added to main UI in `web/src/App.tsx` below TraceFeed

## Backend Tick Integration
- Books included in tick output: `:books` field
- Server broadcasts books to clients on tick via `:op "books"`
- Initial world state includes `:books {}` and `:books-list []`

## File Changes
- `backend/src/fantasia/sim/constants.clj` - mood buff constants
- `backend/src/fantasia/sim/jobs.clj` - build options, job priority, structure completions, scribe job handler
- `backend/src/fantasia/sim/jobs/providers.clj` - scribe job provider generation
- `backend/src/fantasia/sim/agents.clj` - passive mood environment sampling
- `backend/src/fantasia/sim/scribes.clj` - new file for Ollama + meta-myths integration:
  - `load-myths!` - loads myths from `myths.jsonl`
  - `save-myth!` - appends new myth to `myths.jsonl`
  - `get-random-myths` - returns up to 5 random ancient myths for Ollama context
  - `build-myths-context` - formats ancient myths as context string
  - `generate-book-text` - includes ancient myths in Ollama prompts
  - `generate-book-content-async!` - async generation + myth persistence
  - `complete-scribe-job!` - creates placeholder book, starts async generation
- `backend/src/fantasia/sim/tick/initial.clj` - books in initial world state
- `backend/src/fantasia/sim/tick/core.clj` - books in tick output
- `backend/src/fantasia/sim/server.clj` - books broadcast
- `myths.jsonl` - new file for persistent meta-myths across world restarts (JSONL format)
- `web/src/components/SimulationCanvas.tsx` - temple/school/library rendering
- `web/src/components/JobQueuePanel.tsx` - scribe job labels
- `web/src/components/AgentCard.tsx` - scribe job labels
- `web/src/components/LibraryPanel.tsx` - new library book viewer component
- `web/src/components/index.tsx` - LibraryPanel export
- `web/src/App.tsx` - books state, selected book, LibraryPanel integration, books message handling
- `spec/2026-01-20-civic-growth-scribes.md` - updated spec file

## Definition of Done
✓ Houses and civic buildings appear via builder jobs during play
✓ Agents receive passive mood buffs when near pleasant structures/terrain
✓ Scribe job creates books via Ollama and stores them in library data
✓ UI shows new structures and library books can be read
✓ Documentation notes describe new mechanics
✓ Every generated book is saved to `myths.jsonl` for persistent meta-myths
✓ New worlds load up to 5 random ancient myths to seed Ollama with "infinite dawn" context
✓ Book generation is fully async - tick loop never blocks on Ollama calls
✓ Default Ollama model: `qwen3:4b`
