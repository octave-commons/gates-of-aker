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

## Scribe Job + Books (Ollama Integration)
- New job type `:job/scribe` (priority 40) added
- Library generates scribe jobs via `jobs/providers:generate-provider-scribe-jobs!`
- Scribe completion via `scribes:complete-scribe-job!`:
  - Selects up to 3 relevant traces from culture
  - Generates book title via Ollama prompt (short, evocative title)
  - Generates book text via Ollama prompt (3-5 sentences, mythological story)
  - Creates book record with `:book/id`, `:trace-ids`, `:title`, `:text`, `:facets`, `:created-at`, `:created-by`, `:read-count`
  - Stores book in world state `:books` map and `:books-list` vector
- Ollama client via `clj-http` to `http://localhost:11434/api/generate` (model: `llama2:7b`)
- Fallback text provided if Ollama unavailable

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
- `backend/src/fantasia/sim/scribes.clj` - new file for Ollama integration and book generation
- `backend/src/fantasia/sim/tick/initial.clj` - books in initial world state
- `backend/src/fantasia/sim/tick/core.clj` - books in tick output
- `backend/src/fantasia/server.clj` - books broadcast
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
