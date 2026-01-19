# Milestone 3 Job System Fix Spec

## Related Documentation
- [[AGENTS.md]] - Coding standards
- [[docs/notes/planning/2026-01-15-roadmap.md]] - Roadmap
- [[spec/2026-01-15-core-loop.md]] - Core loop

## Issues Found

### Critical Bug #1: complete-job! doesn't call completion handlers
**Location**: `backend/src/fantasia/sim/jobs.clj:285-303`

The `complete-job!` function has a case statement that always returns `world` without calling actual completion functions:
```clojure
(world (case (:type job)
         :job/build-wall world        ; Bug: returns world directly
         :job/chop-tree world        ; Bug: returns world directly
         :job/haul world            ; Bug: returns world directly
         :job/eat world             ; Bug: returns world directly
         :job/sleep world           ; Bug: returns world directly
         :job/deliver-food world    ; Bug: returns world directly
         world))
```

**Impact**: No jobs ever complete. World state never changes (no wood produced, no walls built, no food consumed).

**Fix**: Call completion handlers with `(complete-build-wall! world job)` etc.

---

### Critical Bug #2: No chop-tree jobs are generated
**Location**: `backend/src/fantasia/sim/jobs.clj:414-418`

The `auto-generate-jobs!` function generates:
- `generate-need-jobs!` (eat, sleep)
- `generate-haul-jobs-for-items!` (haul existing items to stockpiles)
- `generate-deliver-food-jobs!` (deliver food from inventory to stockpiles)

**Missing**: `generate-chop-jobs!` - trees exist but no jobs to chop them.

**Impact**: Agents never gather wood. Wood is required for wall building. System dead-locks on wood requirements.

**Fix**: Add `generate-chop-jobs!` to iterate over tree tiles and create `:job/chop-tree` jobs.

---

### Critical Bug #3: Haul jobs have wrong target and skip pickup phase
**Location**: `backend/src/fantasia/sim/jobs.clj:360-375`

The `generate-haul-jobs-for-items!` function creates haul jobs with:
```clojure
(let [[sp-pos _] nearest]
  (assoc (create-job :job/haul sp-pos)  ; Target = stockpile position
         :from-pos pos :to-pos sp-pos :resource res :qty threshold))
```

**Issues**:
1. Job target is stockpile (`sp-pos`), not the item location (`pos`)
2. Agent moves directly to stockpile, bypassing item pickup
3. `complete-haul!` expects agent to have items in inventory, but `pickup-items!` is never called

**Impact**: Agents move to stockpile and complete "haul" without actually moving any items.

**Fix Options**:
1. Two-phase haul: create separate `pickup-item` and `deliver-item` jobs
2. Target = item location, then retarget to stockpile after pickup
3. Add pickup step in movement when adjacent to `from-pos`

---

### Issue #4: No tree job generation despite trees existing
**Location**: `backend/src/fantasia/sim/tick/trees.clj`

Trees are spawned via `spawn-initial-trees!` at world init (line 96 of `initial.clj`), but:
- No function generates `:job/chop-tree` jobs from tree tiles
- Completion function `complete-chop-tree!` exists (line 188) but is never called due to Bug #1

**Impact**: Even if Bug #1 is fixed, agents will never have wood to build walls.

---

### Issue #5: Only 2 agents moving to stockpile at 0,0
User observation: Only 2 agents move, targeting stockpile at [0,0].

**Root Cause**: With bugs #1, #2, and #3:
- Agents get assigned `:job/deliver-food` jobs (target = stockpile [0,0])
- They move to stockpile but don't actually have food to deliver
- Job completes (progress fills while adjacent) but nothing happens
- Loop repeats, creating same job

**Fix**: Fix bugs #1, #2, #3 will create proper job flow.

---

## Definition of Done

1. ✅ `complete-job!` calls appropriate completion handlers (`complete-build-wall!`, `complete-chop-tree!`, etc.)
2. ✅ `generate-chop-jobs!` adds chop jobs for all tree tiles
3. ✅ `auto-generate-jobs!` includes chop tree job generation
4. ✅ Haul jobs properly pickup items from source before delivering to stockpile
5. ✅ Logs show job creation, assignment, movement, progress, and completion
6. ✅ Agents gather wood from trees
7. ✅ Agents build walls from wood
8. ✅ Agents eat and sleep to maintain needs

---

## Implementation Plan

### Phase 1: Fix complete-job! (Critical)
- Change case statements to call completion handlers
- Add logging to confirm completion handlers execute

### Phase 2: Add chop tree job generation (Critical)
- Create `generate-chop-jobs!` to iterate over tree tiles
- Add to `auto-generate-jobs!`
- Test: chop jobs appear in job queue, agents move to trees

### Phase 3: Fix haul job flow (Critical)
- Decide on two-phase approach (pickup + deliver)
- Implement pickup logic when adjacent to `from-pos`
- Update haul job targeting logic
- Test: items actually move from source to stockpile

### Phase 4: Add comprehensive logging
- Log all job state changes
- Log movement decisions (job-based vs random)
- Verify through console output

### Phase 5: End-to-end testing
- Verify wood gathering
- Verify wall building
- Verify food/eat/sleep cycles
- Confirm multiple agents working simultaneously
