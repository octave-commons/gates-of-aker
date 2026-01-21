# Fruit Reduction Plan

## Problem
Too much fruit is accumulating in the simulation, causing imbalance in food economy.

## Files to Modify
- `backend/src/fantasia/sim/tick/initial.clj:155` - `scatter-fruit!` initial fruit density (currently 0.006)
- `backend/src/fantasia/sim/tick/trees.clj:174-175` - Fruit drop check intervals (currently min 18, max 45)
- `backend/src/fantasia/sim/tick/trees.clj:185-186` - Fruit drop scheduling (currently min 180, max 450)

## Changes Required

### Phase 1: Initial World Setup (tick/initial.clj)
**Line 155**: Change fruit scatter density from 0.006 (0.6%) to 0.0005 (0.05%)
```clojure
desired (max 6 (long (Math/ceil (* total-tiles 0.0005))))
```

### Phase 2: Tree Fruit Drop Logic (tick/trees.clj)
**Lines 174-175**: Increase minimum fruit drop check interval from 18 to 60 ticks
```clojure
min-interval 60
max-interval 120
```

**Lines 185-186**: Increase fruit drop scheduling interval from 180-450 to 600-1200 ticks
```clojure
min-interval 600
max-interval 1200
```

## Definition of Done
- [x] Initial fruit scatter reduced to ~0.05% of map tiles (from 0.6%)
- [x] Tree fruit drop checks happen less frequently (every 60-120 ticks vs 18-45)
- [x] Fruit drops less often overall (600-1200 tick intervals vs 180-450)
- [x] Simulation runs without errors
- [x] Fruit accumulation is visibly reduced during gameplay

## Testing
Reset simulation and observe:
1. Initial world spawn has ~10x fewer scattered fruit items
2. Fruit accumulates much more slowly over time
3. Agents still have access to some food, but economy is less flooded
