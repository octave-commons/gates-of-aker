# Milestone 3 & 3.5 Implementation TODO

## Phase 1: Observability & Logging (Completed ✅)

### Backend Logging Infrastructure
- [x] Added `fantasia.dev.logging` requirement to `jobs.clj`
- [x] Added `[JOB:CREATE]` log in `create-job`
- [x] Added `[JOB:ASSIGN]` log in `assign-job!`
- [x] Added `[JOB:AUTO-ASSIGN]` summary in `auto-assign-jobs!`
- [x] Added `[JOB:AUTO-GEN]` summaries in `generate-need-jobs!`, `generate-deliver-food-jobs!`, `generate-haul-jobs-for-items!`
- [x] Added `[JOB:NEED-TRIGGER]` logs for food/warmth/sleep/fire-build needs
- [x] Added `[JOB:COMPLETE]` logs with `job-outcome` helper in `complete-job!`
- [x] Added `[JOB:PROGRESS]` logs in `advance-job!`
- [x] Added `[JOB:ADJACENT]` and `[JOB:NOT-ADJACENT]` logs in `tick/movement.clj`

- [x] Added `fantasia.dev.logging` requirement to `jobs/providers.clj`
- [x] Added `[JOB:AUTO-GEN]` summary in `auto-generate-jobs!`

- [x] Added `fantasia.dev.logging` requirement to `pathing.clj`
- [x] Added `[PATH:REQUEST]` and `[PATH:RESULT]` logs in `bfs-path`
- [x] Added `[PATH:REQUEST]` and `[PATH:RESULT]` logs in `a-star-path`
- [x] Added `[PATH:NEXT-STEP]` logs in `next-step-toward`

- [x] Added `fantasia.dev.logging` requirement to `tick/movement.clj`
- [x] Added `[MOVE:AGENT]` logs for haul, job-path, and random movement
- [x] Added hex import for distance calculations

### Test Suite Hygiene
- [x] Renamed `fire-creation_test.clj` → `fire_creation_test.clj` (underscore naming)
- [x] Fixed `fire_creation_test.clj` syntax error (missing closing parens)
- [x] Rewrote `traces_test.clj` to fix syntax errors and mismatched brackets
- [x] Fixed `spatial_test.clj` to use `move-agent-with-collision-check` instead of non-existent `move-agent`
- [x] Updated `core_test.clj` to count only `:player` faction agents (16) vs all agents including wildlife (56)
- [x] Updated `fire_creation_test.clj` to clear campfire/shrine to force build-fire job generation
- [x] Updated `fire_creation_test.clj` to use `with-redefs` for `rand` and `find-build-fire-site`

### Core Bug Fixes
- [x] Fixed `agents.clj` nil math in `update-needs` - changed `+` with nil-returning `when` forms to `reduce + 0 (keep identity [...])` pattern

### Specification Updates
- [x] Created `spec/2026-01-21-milestone3-3.5-next-steps.md` with phased plan
- [x] Updated progress section with completed work and next steps
- [x] Documented all file changes and logging patterns

---

## Phase 2: Frontend Instrumentation & Memory Visualization (Pending ⏳)

### Frontend Components
- [ ] Extend `AgentCard.tsx` to render need bars (food, sleep, warmth, security, mood, health)
- [ ] Extend `JobQueuePanel.tsx` to show job progress percentages
- [ ] Add hauling inventory badges to agents
- [ ] Highlight corpses, buildings, and resources on canvas
- [ ] Create `MemoryOverlay.tsx` component
- [ ] Create `FacetControls.tsx` component
- [ ] Add `config_facets` WebSocket operation handler
- [ ] Integrate with existing `EventFeed`/`TraceFeed` pattern

### Routing
- [ ] Add React Router to `web/src/App.tsx`
- [ ] Create `/board` route (canvas-only view)
- [ ] Create `/command` route (analytics dashboard with charts)
- [ ] Ensure WebSocket client state is preserved across route transitions

---

## Phase 3: Lifecycle Completeness & Documentation (Pending ⏳)

### Advanced Lifecycle Features
- [ ] Implement agent stats system (strength, dexterity, fortitude, charisma) in `agents.clj`
- [ ] Implement multi-inventory slots (`:personal`, `:hauling`, `:equipment`)
- [ ] Add building/resource archetypes (town centers, fences, tents, stockpiles, water nodes, chickens)
- [ ] Implement water collection jobs from water sources
- [ ] Implement reproduction logic based on need thresholds and facet scores
- [ ] Add `[INVENTORY:*]` logs for inventory changes

### Facet/Memory Integration
- [ ] Ensure `query-need-axis!` influences job priority heuristics
- [ ] Make reproduction consider facet scores before spawning
- [ ] Document facet-based behavior heuristics in `/docs/notes/milestone3-lifecycle.md`

### Documentation
- [ ] Document manual verification steps in `/docs/notes/planning`
- [ ] Add log command examples for each verification scenario
- [ ] Document frontend route names and component purposes
- [ ] Link documentation to spec files

### Final Verification
- [ ] Run full backend test suite (`clojure -X:test`) with extended timeout
- [ ] Run frontend tests (`npm test`)
- [ ] Execute four observability scenarios:
  - [ ] Chop Tree → Wood
  - [ ] Build Wall
  - [ ] Eat Food
  - [ ] Pathing Around Walls
- [ ] Capture log traces for each scenario in docs

---

## Quick Reference: Key Files

### Backend Source
| File | Purpose |
|------|---------|
| `backend/src/fantasia/sim/jobs.clj` | Job lifecycle + logging hooks |
| `backend/src/fantasia/sim/jobs/providers.clj` | Job generation + auto-gen logging |
| `backend/src/fantasia/sim/pathing.clj` | Pathfinding + request/result logging |
| `backend/src/fantasia/sim/tick/movement.clj` | Agent movement + logging |
| `backend/src/fantasia/sim/agents.clj` | Need decay + nil-safe mood math |
| `backend/src/fantasia/dev/logging.clj` | Logging utility (already exists) |

### Backend Tests
| File | Status |
|------|--------|
| `backend/test/fantasia/sim/fire_creation_test.clj` | Fixed and passing |
| `backend/test/fantasia/sim/traces_test.clj` | Rewritten, syntax-clean |
| `backend/test/fantasia/sim/core_test.clj` | Updated for wildlife counts |
| `backend/test/fantasia/sim/spatial_test.clj` | Fixed API reference |

### Specifications
| File | Purpose |
|------|---------|
| `spec/2026-01-18-observability.md` | Logging requirements (reference) |
| `spec/2026-01-19-milestone3-lifecycle.md` | Lifecycle requirements (reference) |
| `spec/2026-01-21-milestone3-3.5-next-steps.md` | Current plan + progress |
| `TODO.md` | This file |

### Frontend (Pending)
| File | Purpose |
|------|---------|
| `web/src/components/AgentCard.tsx` | Needs visualization |
| `web/src/components/JobQueuePanel.tsx` | Job progress |
| `web/src/components/MemoryOverlay.tsx` | Memory/facet visualization (new) |
| `web/src/components/FacetControls.tsx` | Facet configuration (new) |
| `web/src/App.tsx` | Route configuration |

---

## Verification Commands

```bash
# Run backend tests with extended timeout
cd backend
clojure -X:test

# Run with logging visible
LOG_LEVEL=info clojure -M:server

# Run specific test namespace
clojure -X:test :focus fantasia.sim.fire-creation-test

# Frontend
cd web
npm test
```

---

## Open Issues & Blockers

1. **Backend test timeout**: Long-running simulation tests (`50-tick` test in `core_test.clj`) exceed 120s timeout. Run with `--timeout 300` or split suite.

2. **Frontend visualization**: No MemoryOverlay, FacetControls, or `/board`/`/command` routes exist yet.

3. **Lifecycle features**: Stats, multi-inventory, reproduction, water jobs not implemented.