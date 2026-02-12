# Job Namespace Refactoring

## Summary

Refactored the job system from a single monolithic `jobs.clj` namespace into separate namespaces for each job type. Each job now has its own file with:
- `create-*job` function to instantiate the job
- `complete-*!` function to handle job completion
- `progress-*!` function to update job progress
- `job-complete?` function to check if job is finished

## New Job Namespace Files

1. **`fantasia.sim.jobs.eat`** - `backend/src/fantasia/sim/jobs/eat.clj`
   - Priority: 100
   - Sources: stockpiles with food, food items on ground, fruit items on ground (in priority order)
   - Restores agent food need to 1.0

2. **`fantasia.sim.jobs.chop`** - `backend/src/fantasia/sim/jobs/chop.clj`
   - Priority: 60
   - Target: trees
   - Produces 5 wood at tree location

3. **`fantasia.sim.jobs.haul`** - `backend/src/fantasia/sim/jobs/haul.clj`
   - Priority: 80
   - Transports resources from one location to a stockpile
   - Stages: pickup → delivery

4. **`fantasia.sim.jobs.sleep`** - `backend/src/fantasia/sim/jobs/sleep.clj`
   - Priority: 90
   - Restores agent sleep need to 1.0

5. **`fantasia.sim.jobs.deliver-food`** - `backend/src/fantasia/sim/jobs/deliver_food.clj`
   - Priority: 75
   - Transports food items to food stockpiles

## Job Priority Order (Descending)

1. `:job/eat` - 100 (highest - agents must survive)
2. `:job/sleep` - 90 (agents need rest)
3. `:job/haul` - 80 (moving resources)
4. `:job/deliver-food` - 75 (delivering food to stockpiles)
5. `:job/chop-tree` - 60 (gathering resources)

## Current State

### Fixed Bugs

1. **Bug #8: Eat jobs consume wrong resource**
   - Fixed in `jobs/eat.clj` - now checks for food in stockpiles, food items, and fruit items

2. **Bug #9: Eat jobs target wrong food sources**
   - Fixed in `jobs.clj` generate-need-jobs! - now prioritizes: stockpiles > food items > fruit items

3. **Bug #10: Initial food in wrong location**
   - Fixed in `tick/initial.clj:98` - food now placed in stockpiles instead of on ground

4. **Bug #11: complete-eat! return value**
   - Fixed in `jobs/eat.clj` - returns `[world consumed?]` vector

5. **Bug #12: complete-eat! logic**
   - Fixed in `jobs/eat.clj` - properly structured with nested if statements

6. **Bug #13: Multiple agents can't create eat jobs for same target**
   - Fixed in `jobs.clj` generate-need-jobs! - changed to check for unassigned eat jobs

7. **Bug #14: Agents with jobs can't create eat jobs**
   - Fixed in `jobs.clj` generate-need-jobs! - removed `already-has-job?` check for eat jobs

### Test Results

After 60 ticks:
- ✅ All 12 agents now generate eat jobs when hungry
- ✅ 12 eat jobs created (one per agent)
- ✅ All agents target same food source at [0,118] (first fruit on ground)
- ✅ Eat jobs have priority 100 (higher than chop jobs at 60)

### Next Steps

1. Assign workers to eat jobs and verify agents travel to food sources
2. Verify `complete-eat!` consumes food and restores agent food need
3. Consider adding logic to distribute agents across multiple food sources
4. Update `jobs.clj` to use functions from new job namespaces
5. Write tests for each job namespace

## File Structure

```
backend/src/fantasia/sim/
├── jobs.clj                    # Main job system (dispatchers, generation, assignment)
└── jobs/
    ├── eat.clj                 # Eat job logic
    ├── chop.clj                # Chop tree job logic
    ├── haul.clj                # Haul resource job logic
    ├── sleep.clj               # Sleep job logic
    └── deliver_food.clj        # Deliver food job logic
```

## Migration Path

To complete the migration:
1. Update `jobs.clj` to require each job namespace
2. Replace job-specific functions with calls to job namespace functions
3. Update `complete-job!` to dispatch to appropriate `complete-*!` function
4. Update `advance-job!` to dispatch to appropriate `progress-*!` function
5. Keep backward compatibility during transition
6. Remove deprecated functions from `jobs.clj`
7. Run all tests to verify migration
