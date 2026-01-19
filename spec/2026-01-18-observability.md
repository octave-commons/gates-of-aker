# Observability & Debugging Spec: Job System and Pathing #debugging #logging #testing #jobs #pathing #colony-sim #observability

## Context

Milestone 3 (Colony Job Loop) has code written but **no runtime observability**. Code review confirms job system, pathing, and integration exists, but we cannot verify:
- Agents actually get assigned jobs
- Pathing is called and returns valid paths
- Job progress advances correctly
- Job completion triggers expected world changes
- Agents move toward job targets vs random movement

This spec defines logging requirements to verify the integrated system works.

## Related Documentation
- [[docs/notes/planning/2026-01-15-roadmap.md]] - Milestone 3 definition
- [[docs/notes/world-schema.md]] - World schema reference
- [[AGENTS.md]] - Backend style guide (keep logging to `println` for now)

---

## Problem Statement

**What we have:**
- `jobs.clj` with complete job system (creation, assignment, progress, completion)
- `pathing.clj` with BFS and A* algorithms
- Integration in `tick.clj` calling job auto-generation, assignment, and processing

**What we can't see:**
- Are jobs being generated? (eat, sleep, chop, haul)
- Are jobs being assigned to agents?
- Are agents moving toward job targets using pathing?
- Does pathing find valid routes around walls?
- Do jobs complete and produce expected outcomes (wood, food)?
- Do agents' needs actually trigger job generation?

**Risk:**
Job/pathing code exists but may have bugs preventing execution. Without logs, we can't diagnose.

---

## Logging Requirements

### Phase 1: Job Generation (add to `jobs.clj`)

Log each job created with full details:

```clojure
(println "[JOB:CREATE]"
         {:type (:type job)
          :id (:id job)
          :target (:target job)
          :priority (:priority job)})
```

**Where to add:**
- `create-job` (line 13): Log every job created
- `generate-need-jobs!` (line 266): Log needs-based job triggers
- `generate-haul-jobs-for-items!` (line 249): Log item-driven job creation
- `generate-deliver-food-jobs!` (line 286): Log delivery job creation
- `auto-generate-jobs!` (line 303): Log summary of jobs generated this tick

**Expected output:**
```
[JOB:CREATE] {:type :job/eat, :id #uuid, :target [0 0], :priority 100}
[JOB:AUTO-GEN] Generated 3 jobs this tick (total: 5)
[JOB:NEED-TRIGGER] Agent 0 food=0.25 < 0.3, created eat job
```

---

### Phase 2: Job Assignment (add to `jobs.clj` and `tick.clj`)

Log job assignment events:

```clojure
(println "[JOB:ASSIGN]"
         {:agent-id agent-id
          :job-id (:id job')
          :type (:type job')
          :priority (:priority job')})
```

**Where to add:**
- `claim-next-job!` (line 29): Log when agent claims a job
- `auto-assign-jobs!` (line 41): Log summary of assignments this tick

**Expected output:**
```
[JOB:ASSIGN] {:agent-id 0, :job-id #uuid, :type :job/chop-tree, :priority 60}
[JOB:AUTO-ASSIGN] Assigned 4 jobs (2 idle agents claimed jobs)
```

---

### Phase 3: Job Progress (add to `jobs.clj` and `tick.clj`)

Log job progress updates:

```clojure
(println "[JOB:PROGRESS]"
         {:agent-id agent-id
          :job-id job-id
          :delta delta
          :new-progress new-progress
          :required (:required job)})
```

**Where to add:**
- `advance-job!` (line 205): Log every progress update
- `process-jobs!` (tick.clj line 85): Log if agent is adjacent (eligible for progress)

**Expected output:**
```
[JOB:PROGRESS] {:agent-id 0, :job-id #uuid, :delta 0.2, :new-progress 0.6, :required 1.0}
[JOB:ADJACENT] Agent 0 at [3 4] distance 1 from job target [3 5], eligible
[JOB:NOT-ADJACENT] Agent 1 at [10 10] distance 4 from job target [14 12], not eligible
```

---

### Phase 4: Job Completion (add to `jobs.clj`)

Log successful job completion with outcomes:

```clojure
(println "[JOB:COMPLETE]"
         {:agent-id agent-id
          :type (:type job)
          :target (:target job)
          :outcome "...description..."})
```

**Where to add:**
- `complete-build-wall!` (line 122): Log wall built, ghost→wall conversion
- `complete-chop-tree!` (line 138): Log tree chopped, wood produced
- `complete-haul!` (line 147): Log items hauled to destination
- `complete-eat!` (line 158): Log food consumed, need restored
- `complete-sleep!` (line 165): Log sleep completed, need restored
- `complete-deliver-food!` (line 170): Log food stored in stockpile

**Expected output:**
```
[JOB:COMPLETE] {:agent-id 0, :type :job/chop-tree, :target [5 7], :outcome "Produced 5 wood at [5 7]"}
[JOB:COMPLETE] {:agent-id 0, :type :job/eat, :target [0 0], :outcome "Consumed 1 food, need food=1.0"}
```

---

### Phase 5: Pathfinding (add to `pathing.clj` and `tick.clj`)

Log pathfinding requests and results:

```clojure
(println "[PATH:REQUEST]"
         {:agent-id (:id agent)
          :start (:pos agent)
          :goal (:target job)
          :method :bfs})
```

```clojure
(println "[PATH:RESULT]"
         {:start start
          :goal goal
          :found (boolean path)
          :path-length (when path (count path))
          :next-step (when path (second path))})
```

**Where to add:**
- `bfs-path` (line 5): Log path requests, success/failure, path length
- `a-star-path` (line 33): Log if/when A* is used
- `next-step-toward` (line 81): Log result returned to caller

**Expected output:**
```
[PATH:REQUEST] {:agent-id 0, :start [2 2], :goal [5 7], :method :bfs}
[PATH:RESULT] {:start [2 2], :goal [5 7], :found true, :path-length 5, :next-step [3 3]}
[PATH:RESULT] {:start [10 10], :goal [14 12], :found false, :path-length nil, :next-step nil}
```

---

### Phase 6: Agent Movement (add to `tick.clj` and `spatial.clj`)

Log agent position changes:

```clojure
(println "[MOVE:AGENT]"
         {:agent-id (:id agent)
          :from (:pos agent)
          :to (:pos agent')
          :method (if job "job-path" "random")})
```

**Where to add:**
- `move-agent-with-job` (tick.clj line 101): Log job-based movement
- `spatial/move-agent` (spatial.clj): Log random/fallback movement

**Expected output:**
```
[MOVE:AGENT] {:agent-id 0, :from [2 2], :to [3 3], :method job-path}
[MOVE:AGENT] {:agent-id 1, :from [5 5], :to [6 4], :method random}
```

---

## Verification Scenarios

### Test 1: Chop Tree → Wood
**Setup:** Place a tree on a tile, ensure agent needs food/sleep are high.

**Expected logs:**
1. `[JOB:CREATE]` - `:job/chop-tree` created
2. `[JOB:ASSIGN]` - Agent claims chop-tree job
3. `[PATH:REQUEST]` - Path from agent to tree
4. `[PATH:RESULT]` - Valid path found
5. `[MOVE:AGENT]` - Agent moves toward tree
6. Repeat 4-5 until adjacent
7. `[JOB:ADJACENT]` - Agent adjacent, progress begins
8. `[JOB:PROGRESS]` - Progress advances (0.0 → 0.2 → ... → 1.0)
9. `[JOB:COMPLETE]` - Tree removed, wood added at tile

**Success if:** All 9 log types appear in sequence.

---

### Test 2: Build Wall
**Setup:** Place wall ghost via UI, ensure agent has wood.

**Expected logs:**
1. `[JOB:CREATE]` - `:job/build-wall` created (ghost target)
2. `[JOB:ASSIGN]` - Agent claims build-wall job
3. `[PATH:REQUEST]` - Path to wall ghost
4. `[PATH:RESULT]` - Valid path found
5. `[MOVE:AGENT]` - Agent moves adjacent
6. `[JOB:ADJACENT]` - Agent adjacent, progress begins
7. `[JOB:PROGRESS]` - Progress advances
8. `[JOB:COMPLETE]` - Ghost becomes wall, wood consumed
9. `[JOB:COMPLETE]` - Verify tile structure changed from `:wall-ghost` to `:wall`

**Success if:** Wall solidifies and blocks movement in subsequent pathfinding.

---

### Test 3: Eat Food
**Setup:** Place food in warehouse, agent food need drops below threshold.

**Expected logs:**
1. `[JOB:NEED-TRIGGER]` - Agent food < 0.3
2. `[JOB:CREATE]` - `:job/eat` created at food location
3. `[JOB:ASSIGN]` - Agent claims eat job
4. `[PATH:REQUEST]` - Path to food
5. `[MOVE:AGENT]` - Agent moves adjacent
6. `[JOB:ADJACENT]` - Agent adjacent
7. `[JOB:COMPLETE]` - Food consumed, agent need = 1.0

**Success if:** Agent food need resets to 1.0.

---

### Test 4: Pathing Around Walls
**Setup:** Build wall in corridor, place agent on one side, job target on other side.

**Expected logs:**
1. `[PATH:REQUEST]` - Path request from agent to target
2. `[PATH:RESULT]` - Path that goes around wall, not through it
3. `[MOVE:AGENT]` - Agent follows path around wall

**Success if:** Agent reaches target by navigating around wall, not through it.

---

## Implementation Priority

### Priority 1: Job Lifecycle Logs (Quick Win) - 3 SP
Add logs to `jobs.clj`:
- `create-job` (1 line)
- `claim-next-job!` (4-5 lines)
- `advance-job!` (5 lines)
- One completion function per job type (5 functions)

**Impact:** Immediately see if jobs are created, assigned, advanced, and completed.

---

### Priority 2: Pathing Logs (Medium) - 2 SP
Add logs to `pathing.clj`:
- `bfs-path` (10 lines)
- `a-star-path` (optional, 20 lines)
- `next-step-toward` (3 lines)

**Impact:** Verify pathing works and agents actually use job targets for movement.

---

### Priority 3: Movement Logs (Low) - 1 SP
Add logs to `tick.clj`:
- `move-agent-with-job` (2 lines)
- `spatial/move-agent` (add wrapper to call it)

**Impact:** Confirm agent movement correlates with job/pathing.

---

## Definition of Done

Milestone 3 is complete when:

1. **All log phases implemented** (Phases 1-6 above)
2. **Job lifecycle visible** in console output for all job types
3. **Pathing requests logged** with start/goal/result
4. **Agent movement logged** with method (job-path vs random)
5. **At least one verification scenario passes** (Test 1-4 above)
6. **Logs are readable** - structured maps with clear labels

**Story Points:** 6 SP (3+2+1)

---

## Future Enhancements

### Toggle-based Logging (Phase 2)
After Phase 1 logs work, add a `:debug-logging` flag to world state:

```clojure
{:debug-logging true  ;; or false to disable
 :tick 0
 :agents [...]}
```

Wrap all `println` calls in conditional:

```clojure
(when (:debug-logging world)
  (println "[JOB:CREATE]" ...))
```

**Benefit:** Disable logs in production or when debugging is complete.

### Structured Trace Feed (Phase 3)
Emit job/pathing events to the trace system already feeding the frontend:

```clojure
{:trace/id "job-create-123"
 :type :job/create
 :job-id (:id job)
 :agent-id agent-id
 :data job}
```

**Benefit:** Frontend can visualize job flows, pathfinding, and agent behavior in real-time.

---

## Related Issues
- GitHub Issue #4 - Milestone 3 (reopened with this spec as attachment)
- GitHub Issue #8 - Bridge Myth Engine (depends on real events from jobs)
