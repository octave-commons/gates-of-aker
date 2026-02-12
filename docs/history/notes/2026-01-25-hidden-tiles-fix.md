---
title: Fix for Hidden Tiles Issue
type: fix
component: backend
priority: critical
status: implemented
related-issues: []
estimated-effort: 2 hours
created: 2026-01-25

---

# Fix for Hidden Tiles Issue

## Problem
Frontend was displaying all tiles as "hidden" with visibility status showing "hidden" even though tiles should be visible. All tile properties (biome, terrain, structure, etc.) showed as "None".

## Root Cause
The backend ECS tick system was not computing or broadcasting tile visibility data to the frontend:

1. **tick-ecs-once** in `backend/src/fantasia/sim/ecs/tick.clj` only returned a snapshot, not including delta-snapshot with tile visibility data
2. **hello message** in `backend/src/fantasia/server.clj` was not sending `:tile-visibility` and `:revealed-tiles-snapshot` to frontend on initial connection
3. **tick messages** did not include delta-snapshot data because tick-ecs-once wasn't returning it

Without visibility data, the frontend's `tileVisibility` state was empty, causing:
- SelectedPanel to show "hidden" for all tiles (falls back to "hidden" when key not found)
- SimulationCanvas to apply incorrect visibility logic

## Solution

### 1. Updated tick-ecs-once to return delta-snapshot
**File:** `backend/src/fantasia/sim/ecs/tick.clj`

Changed return value from just `snapshot` to a map containing:
- `:snapshot` - The game snapshot
- `:tick` - Current tick number
- `:attribution` - Attribution data
- `:delta-snapshot` - Full delta with visibility data (including `:tile-visibility` and `:revealed-tiles-snapshot`)

**Key change (lines 44-58):**
```clojure
(defn tick-ecs-once [global-state]
   "Run one ECS tick with all systems."
   (let [ecs-world (get-ecs-world)
         ecs-world' (run-systems ecs-world global-state)
         new-tick (inc (:tick global-state))
         seed (:seed global-state)
         old-global-state @*global-state
         global-state' (-> global-state
                            (assoc :tick new-tick)
                            (assoc :temperature (time/temperature-at seed new-tick))
                            (assoc :daylight (time/daylight-at seed new-tick)))
         snapshot (fantasia.sim.ecs.adapter/ecs->snapshot ecs-world' global-state')
         delta-snapshot (fantasia.sim.world/delta-snapshot old-global-state global-state' nil)]
     (clojure.core/reset! *ecs-world ecs-world')
     (clojure.core/reset! *global-state global-state')
     {:snapshot snapshot
      :tick new-tick
      :attribution nil
      :delta-snapshot delta-snapshot}))
```

### 2. Updated hello message to include tile visibility data
**File:** `backend/src/fantasia/server.clj`

Added `:tile-visibility` and `:revealed-tiles-snapshot` to the select-keys call so they are sent to frontend on initial connection.

**Key change (line 181):**
```clojure
:state (merge (select-keys snapshot [:tick :shrine :levers :map :agents :calendar :temperature :daylight :cold-snap :tile-visibility :revealed-tiles-snapshot])
```

## Data Flow After Fix

### Backend (per tick)
1. ECS tick runs → agent positions updated
2. `tick-ecs-once` captures old-global-state before updates
3. World state updated with new tick, temperature, daylight
4. Snapshot generated via `ecs->snapshot`
5. **NEW:** Delta-snapshot computed with visibility data via `world/delta-snapshot`
6. Returns map with `:snapshot`, `:tick`, `:attribution`, and `:delta-snapshot`

### Backend (per broadcast)
7. Server extracts `:tick`, `:snapshot`, `:attribution` from result and broadcasts as "tick" message
8. Server extracts `:delta-snapshot` from result and broadcasts as "tick_delta" message
9. Frontend receives both messages and updates state

### Backend (initial connection)
10. **NEW:** Hello message now includes `:tile-visibility` and `:revealed-tiles-snapshot`
11. Frontend receives initial visibility state on connection

### Frontend
1. On "hello" message: Set initial `tileVisibility` from state
2. On "tick_delta" message: Update `tileVisibility` with new visibility data
3. SelectedPanel: Correctly looks up tile visibility from map
4. SimulationCanvas: Applies visibility filtering for rendering

## Testing
- Backend code compiles successfully
- Backend should now broadcast tile visibility data in both hello and tick_delta messages
- Frontend should receive and store tile visibility state
- SelectedPanel should display correct visibility status for tiles
- Tiles should not all show as "hidden" anymore

## Notes
- The fog-of-war system was already implemented (see `spec/visibility-system.md`)
- This fix ensures the visibility data is properly transmitted to frontend
- Initial state shows all tiles as visible (tile-visibility empty map, which causes backend to send all tiles)
- As agents explore, tile-visibility will be populated with visible/revealed/hidden states
