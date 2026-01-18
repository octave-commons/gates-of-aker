# Gates of Aker - Current State & Path Forward

**Last Updated:** 2026-01-18
**Status:** Milestone 1-2 Complete | Milestone 3 In Progress | Milestones 4-6 Pending

---

## Completed Work ✅

### Milestone 1 — Hex Map Backbone (DONE)
**Status:** ✅ Complete
**Date:** ~2026-01-17
**Evidence:**
- `backend/src/fantasia/sim/hex.clj` - 4054 bytes, axial coordinate system
- Axial coordinates throughout codebase
- `web/src/components/SimulationCanvas.tsx` - 464 lines, hex rendering
- World schema uses `:map` metadata + sparse `:tiles`

**Acceptance met:**
- Backend uses axial hex coords
- Frontend renders hex tiles and selection correctly
- WS snapshot includes map data to draw

---

### Milestone 2 — Walls, Pathing, and Build Ghosts (DONE)
**Status:** ✅ Complete
**Date:** ~2026-01-17
**Evidence:**
- `backend/src/fantasia/sim/pathing.clj` - 4055 bytes
  - BFS algorithm (line 5-31)
  - A* algorithm (line 33-79)
  - `next-step-toward` (line 81-90)
- Tile passability via `spatial/passable?`
- WS op `place_wall_ghost` in server.clj
- `:job/build-wall` in jobs.clj (line 122-136) converts ghost→wall
- UI build tool in App.tsx (line 404)
- Wall ghosts rendered (dashed yellow) and walls (solid gray)

**Acceptance met:**
- Players can place wall ghosts, see them in UI
- Pawns can route around completed walls (BFS/A* implemented)
- Backend authoritative over passability and ghost/wall state

---

## In Progress 🔄

### Milestone 3 — Colony Job Loop (IN PROGRESS - BLOCKED)
**Status:** 🔄 Code written, integration unverified
**Blocker:** No runtime observability

**Evidence of code:**
- `backend/src/fantasia/sim/jobs.clj` - 11940 bytes (308 lines)
  - Job creation (line 13): `create-job` generates jobs with UUID, priority
  - Job assignment (line 23): `assign-job!` assigns job to agent
  - Auto-assignment (line 41): `auto-assign-jobs!` assigns jobs to idle agents
  - Stockpile system (lines 70-109): create, add, consume, find nearest
  - Job types:
    - `:job/chop-tree` (line 138-145)
    - `:job/haul` (line 147-156)
    - `:job/eat` (line 158-163)
    - `:job/sleep` (line 165-168)
    - `:job/build-wall` (line 122-136)
    - `:job/deliver-food` (line 170-186)
  - Need-driven job generation (line 266-285): `generate-need-jobs!`
  - Item-driven job generation (line 249-265): `generate-haul-jobs-for-items!`
  - Food delivery generation (line 286-301): `generate-deliver-food-jobs!`
  - Auto-generation orchestrator (line 303-307): `auto-generate-jobs!`

- Integration in `tick.clj`:
  - Line 120: `(jobs/auto-generate-jobs!)` - generates jobs each tick
  - Line 121: `(jobs/auto-assign-jobs!)` - assigns jobs to idle agents
  - Line 122: `(process-jobs!)` - advances job progress when adjacent
  - Lines 124-126: `move-agent-with-job` calls `pathing/next-step-toward`

**What's working:**
- Job system code exists and is complete
- Pathing algorithms implemented (BFS + A*)
- Integration calls exist in tick loop

**What's NOT working (unverified):**
- ❌ Jobs are actually being generated
- ❌ Agents are claiming jobs
- ❌ Agents are moving toward job targets using pathing
- ❌ Jobs are advancing when adjacent
- ❌ Jobs are completing with expected outcomes
- ❌ World state changes from completion (wood, food, walls)

**Root cause:**
Zero runtime logging. We can't see:
- Job creation events
- Job assignment events
- Job progress updates
- Job completion events
- Pathfinding requests/results
- Agent position changes

**Next step:**
See GitHub Issue #15 and `spec/2026-01-18-observability.md` for detailed logging requirements.

---

## Pending Work 📋

### Milestone 4 — Champion Control & Day/Night Gate
**GitHub Issue:** #5
**Status:** 📋 Pending
**Tasks from issue:**
- Define champion entity/state (`:champion/id`, `:champion/asleep?`, `:world/phase`)
- Add WS ops for champion move/action and sleep/wake transitions
- Enforce rule: night abilities only tick when champion is asleep
- Implement frontend champion controls (selection, move intents) and sleep button
- Display Day/Night indicator and transition panel in UI

**Recommendation:** Create detailed spec document before starting.

---

### Milestone 5 — Six Neighbor Factions
**GitHub Issue:** #6
**Status:** 📋 Pending
**Tasks from issue:**
- Place seven settlements (player center + six neighbors in each hex direction at fixed ring distance)
- Define faction data (`:faction/id`, settlement position, attitude, intents)
- Implement basic behaviors: scout, trade, raid probe (even if crude)
- Surface faction activity/intel in UI (markers + log entries)

**Recommendation:** Create detailed spec document before starting.

---

### Milestone 6 — Decks, Signs, and Collisions
**GitHub Issue:** #7
**Status:** 📋 Pending
**Tasks from issue:**
- Implement deck/hand/discard per faction with shared card schema (phase, cost, keys, tags, effect)
- Add AI play policy (0–1 card per Aker boundary) and collision resolution (winner + complication)
- Ensure cards emit Signs + Modifiers that lead to event candidates; avoid direct raid spawning
- Define initial event families (ultimatum/raid/feud, trial/verdict, trade/scarcity/blackmarket) wired into myth/event bus
- Frontend night panel shows player hand, signs feed, event chain viewer

**Recommendation:** Create detailed spec document before starting.

---

### Bridge Myth Engine to World Event Bus
**GitHub Issue:** #8
**Status:** 📋 Pending (Depends on Milestone 3 completion)
**Tasks from issue:**
- Add `:world/event-bus` that collects structured world events each tick
- Feed myth ledger via real events (wall built, caravan, ultimatum, raid, trial, etc.) instead of synthetic RNG-only events
- Ensure cards/factions/jobs append events that myth can convert into mentions/claims
- Document event schema so future systems can publish consistently

**Blocking:** Needs real events from job system to bridge properly.

---

## Immediate Priority (Do First)

### Priority 1: Observability for Milestone 3 🚨
**GitHub Issue:** #15
**Spec:** `docs/spec/2026-01-18-observability.md`
**Story Points:** 6 SP

**Tasks:**
1. Add job creation logs to `jobs.clj` (8 functions)
2. Add job assignment logs to `jobs.clj` (2 functions)
3. Add job progress logs to `jobs.clj` (1 function)
4. Add job completion logs to `jobs.clj` (5 functions)
5. Add pathing logs to `pathing.clj` (3 functions)
6. Add movement logs to `tick.clj` (2 functions)
7. Run simulation and verify logs appear
8. Test verification scenarios (chop tree, build wall, eat food, path around wall)

**Why first:**
Without observability, we can't confirm anything works. All other milestones depend on knowing Milestone 3 actually works.

---

### Priority 2: Update Roadmap Documentation
**GitHub Issue:** #14
**Status:** Partially complete
**Remaining:**
- [ ] Add completion date to Milestone 1 (2026-01-17?)
- [ ] Add completion date to Milestone 2 (2026-01-17?)
- [ ] Mark Milestone 3 as "In Progress - Blocked on Observability"

**Why second:**
Keep roadmap accurate prevents confusion about what's actually done.

---

## Documentation Created

### ✅ World Schema
**File:** `docs/notes/world-schema.md`
**Content:**
- Axial coordinate conventions
- Neighbor directions
- Map structure (`:map` metadata, bounds)
- Tile structure (`:terrain`, `:structure`, `:resource`, `:biome`)
- Snapshot payload format
- WebSocket operation examples
- Migration notes from square grid
- Implementation module references

**Status:** Complete (GitHub Issue #9 closed)

---

### ✅ Observability Spec
**File:** `spec/2026-01-18-observability.md`
**Content:**
- Problem statement (why we need observability)
- 6 phases of logging requirements:
  1. Job Generation
  2. Job Assignment
  3. Job Progress
  4. Job Completion
  5. Pathfinding
  6. Agent Movement
- Verification scenarios:
  - Test 1: Chop Tree → Wood
  - Test 2: Build Wall
  - Test 3: Eat Food
  - Test 4: Pathing Around Walls
- Implementation priorities (3 phases)
- Future enhancements (toggle-based logging, structured traces)

**Status:** Complete (ready for implementation)

---

## Code Quality Fixes ✅

### ✅ Debug Print Removed
**Issue:** #12
**Fix:** Removed `(print "hi")` from `backend/src/fantasia/server.clj:10`
**Status:** Complete

---

### ✅ Duplicate Function Removed
**Issue:** #10
**Fix:** Removed duplicate `move-agent-with-job` definition from `backend/src/fantasia/sim/tick.clj` (lines 221-234 deleted)
**Status:** Complete

---

### ✅ Duplicate Keyboard Handlers Removed
**Issue:** #11
**Fix:** Removed duplicate useEffect (lines 100-126) from `web/src/components/SimulationCanvas.tsx`
**Status:** Complete

---

## GitHub Issues Summary

### Open Issues (7)
| # | Title | Status | Priority | Blocked By |
|---|--------|----------|-------------|
| #4 | Milestone 3 — Colony Job Loop | 🔴 REOPENED | #15 (Observability) |
| #5 | Milestone 4 — Champion Control & Day/Night Gate | 📋 Pending | #4 |
| #6 | Milestone 5 — Six Neighbor Factions | 📋 Pending | #4 |
| #7 | Milestone 6 — Decks, Signs, and Collisions | 📋 Pending | #4, #8 |
| #8 | Bridge Myth Engine to World Event Bus | 📋 Pending | #4 |
| #14 | Update roadmap docs to reflect completed milestones | 🔄 Partial | None |
| #15 | Add Observability to Job System and Pathing | 📋 Pending | None |

### Closed Issues (7)
| # | Title | Date |
|---|--------|------|
| #2 | Milestone 1 — Hex Map Backbone | 2026-01-18 |
| #3 | Milestone 2 — Walls, Pathing, and Build Ghosts | 2026-01-18 |
| #9 | Document Axial World Schema | 2026-01-18 |
| #10 | Remove duplicate move-agent-with-job function | 2026-01-18 |
| #11 | Remove duplicate keyboard handlers in SimulationCanvas.tsx | 2026-01-18 |
| #12 | Remove debug print statement from server.clj | 2026-01-18 |
| #13 | Update Roadmap Progress Tracking | 2026-01-18 |

---

## Recommended Path Forward

### Sprint 1: Verify Milestone 3
1. **SP 6:** Implement observability logging
   - Add all logging to jobs.clj, pathing.clj, tick.clj
   - Run and verify logs appear
   - Test all 4 verification scenarios

2. **Day 2-3:** Debug and fix any issues found
   - If jobs not generating: debug `generate-need-jobs!`
   - If agents not claiming: debug `claim-next-job!`
   - If pathing failing: debug `bfs-path` with wall test cases

3. **Day 4-5:** Complete verification scenarios
   - Chop tree → produce wood
   - Build wall → ghost becomes solid
   - Eat food → need restored
   - Path around wall → agent navigates correctly

4. **Day 6-7:** Close Milestone 3, start Milestone 4 planning

### Week 2-3: Milestone 4 (Champion)
- Create detailed spec document
- Implement champion entity
- Implement day/night phase system
- Add WS ops
- UI controls

### Week 4+: Milestones 5-6 (Factions, Decks)
- Faction settlements and behaviors
- Card engine and deck management
- Collision resolution
- Event families

---

## Questions & Decisions Needed

1. **Toggle-based logging vs always-on?**
   - Should observability logs be always-on (for now) or behind a toggle?
   - Recommendation: Always-on for debugging Milestone 3, then add toggle later

2. **Champion implementation approach:**
   - Is champion a special agent role (`:role :champion`) or separate entity?
   - Recommendation: Special agent role to reuse existing job/movement systems

3. **Faction AI complexity:**
   - Should faction behaviors be simple random choice or weighted AI?
   - Recommendation: Start simple (random), upgrade to weighted AI later

---

## Metrics

**Codebase size:**
- Backend: ~15 sim files
- Frontend: ~10 React components
- Total: ~25K lines of code

**Progress:**
- Milestones complete: 2/6 (33%)
- Milestone 3 in progress: blocked on observability
- Code quality issues fixed: 3/3 (100%)
- Documentation complete: 2/2 (100%)

**Story Points to Milestone 3 completion:** 6 SP

---

*This document is a living reference. Update it as work progresses.*
