# Agent Movement and Camera Input Issues

## Problem Statement
1. Agents are not moving visually in simulation
2. Camera is not responding to keyboard (WASD) or mouse inputs properly

## Investigation Results

### Agent Movement Analysis

#### Backend Flow
1. **Initial World Creation** (`backend/src/fantasia/sim/tick/initial.clj:74-79`)
   - 12 agents are created with positions in village biome
   - Uses `biomes/rand-pos-in-biome` to position agents
   - All agents should have `:pos` field set

2. **Tick Processing** (`backend/src/fantasia/sim/tick/core.clj:47-59`)
   - `tick-once` calls `movement/move-agent-with-job` for each agent
   - Movement happens before agent interactions

3. **Movement Logic** (`backend/src/fantasia/sim/tick/movement.clj:38-51`)
   - If agent has job → path toward job target
   - If no job → fallback to `spatial/move-agent` (random movement)

4. **Random Movement** (`backend/src/fantasia/sim/spatial.clj:44-56`)
   - `move-agent` selects neighbor using: `(mod (+ (:tick world) (:id agent) (:seed world)) (count options))`
   - Filters neighbors by in-bounds and passable (no walls)
   - Updates agent's `:pos` field

#### Potential Issues
- Agents might be getting moved but frontend not rendering updates
- Tick might not be called at all
- Initial agent positions might be nil or invalid

### Camera Input Analysis

#### Canvas Input Handling (`web/src/components/SimulationCanvas.tsx`)

**Keyboard Input** (lines 46-99)
- Event listeners attached to `window` for keydown/keyup
- Keys tracked in `keysPressed` ref
- Camera movement loop uses RAF to process keys
- Moves camera by `PAN_SPEED / zoom` pixels per frame

**Mouse Input** (lines 403-428)
- Mouse drag (middle mouse button) pans camera
- Mouse wheel zooms centered on cursor

#### Potential Issues
1. **Focus Issue**: Canvas has `tabIndex="0"` but might need explicit focus
2. **Event Propagation**: Key events might be consumed by other elements
3. **State Updates**: `setCamera` in RAF loop might not be triggering re-renders properly

## Root Cause Analysis

### Issue 1: Agent Movement
**Hypothesis**: The canvas rendering effect (line 101-351) has dependency `[snapshot, mapConfig, selectedCell, selectedAgentId]` but NOT `agentPaths`. This means when agent positions update in snapshot, canvas redraws.

**However**, actual problem is likely that:
- Simulation might not be running (no ticks being called)
- OR agents are moving but position changes are not visible

### Issue 2: Camera Not Responding
**Root Cause Identified**: Looking at the camera movement effect (lines 72-99), there's a state update in a RAF loop calling `setCamera`. However, React's batching and the fact that the render effect (lines 101-351) doesn't include `camera` in its dependencies means the canvas won't re-render when camera state changes!

**Critical Bug**: The canvas drawing effect at line 101 has:
```typescript
}, [snapshot, mapConfig, selectedCell, selectedAgentId]);
```

But it SHOULD have:
```typescript
}, [snapshot, mapConfig, selectedCell, selectedAgentId, camera]);
```

Without `camera` in the dependency array, changing camera state does NOT trigger a canvas redraw.

## Solution Plan

### Phase 1: Fix Camera Rendering
**File**: `web/src/components/SimulationCanvas.tsx`

1. **Add camera to render dependencies** (line 351)
   - Change dependency array from `[snapshot, mapConfig, selectedCell, selectedAgentId]` 
   - To: `[snapshot, mapConfig, selectedCell, selectedAgentId, camera]`

2. **Test camera controls**
   - WASD keys should pan the view
   - Mouse wheel should zoom
   - Middle-click drag should pan

**Expected Outcome**: Camera controls become immediately responsive

### Phase 2: Debug Agent Movement
**Investigation Steps**:
1. Check if simulation is running (isRunning state)
2. Check if tick is advancing (tick counter increasing)
3. Check if agents have valid positions in snapshot
4. Verify agent positions are changing between ticks

**Potential Fixes** (if agents not moving):
1. Ensure tick is being called
2. Check backend logs for movement errors
3. Verify agents have valid initial positions
4. Add debug logging to movement code

### Phase 3: Verify Agent Visualization
**File**: `web/src/components/SimulationCanvas.tsx`

After Phase 1 is working, verify:
1. Agents render as colored circles (lines 314-348)
2. Agent colors match role (priest=red, knight=blue, peasant=black)
3. Selected agent shows golden outline
4. Agent path shows when agent is selected

## Testing Strategy

### Camera Controls Test
1. Open application
2. Press W/A/S/D keys
3. **Expected**: View pans in corresponding direction
4. Scroll mouse wheel
5. **Expected**: View zooms in/out centered on cursor
6. Middle-click and drag
7. **Expected**: View pans following mouse

### Agent Movement Test
1. Click "Tick" button to advance one tick
2. Observe tick counter increases
3. Check if agents move to new positions
4. Click "Run" to auto-tick
5. Observe agents moving continuously

## Implementation Log

### Phase 1: Fix Camera Rendering ✅ COMPLETED
**File**: `web/src/components/SimulationCanvas.tsx`

**Change Made** (line 351):
```typescript
// Before:
}, [snapshot, mapConfig, selectedCell, selectedAgentId]);

// After:
}, [snapshot, mapConfig, selectedCell, selectedAgentId, camera]);
```

**Why This Works**: 
- Previously, when `camera` state changed via WASD keys or mouse input, the canvas rendering effect would not re-trigger because `camera` wasn't in the dependency array
- React's useEffect would not run, so the canvas would keep displaying the old camera transform
- Adding `camera` to the dependency array ensures the canvas redraws whenever camera state changes

**Build Verification**:
- ✅ TypeScript compilation passes (`npm run build`)
- ✅ Type checking passes (`npm exec tsc --noEmit`)
- ✅ Frontend builds successfully

**GitHub Issue**: #17 created

### Phase 2: Debug Agent Movement ✅ COMPLETED

**Root Cause Found**: Job `:target` field was being set to a number (0) instead of a coordinate vector [0, 0].

**Investigation**:
1. Tested simulation tick - agents had jobs but weren't moving
2. Inspected job structure: `"to-pos": 0, "target": 0` (both numbers, not arrays)
3. Found root cause in `create-job` function - it wasn't validating/normalizing the target input
4. Movement code was calling `hex/distance` with a number (0) instead of a vector [0,0]
5. The `hex/coerce-axial` function converted the number 0 to [0, 0], but this wasn't working correctly in the pathing logic

**Fix Applied**:
- **File**: `backend/src/fantasia/sim/jobs.clj`
  - Modified `create-job` to normalize target input:
    - If sequential (already a vector): use as-is
    - If number: convert to `[number, 0]`
    - Otherwise: default to `[0, 0]`
  - This ensures `:target` is always a valid coordinate vector

**Verification**:
- Agent 0 started at [4, 3]
- After 6 ticks, agent 0 moved to [1, 0] (moving toward [0, 0])
- Agent 10 already at [0, 0] (destination reached)
- Other agents moving via random fallback or toward their own job targets

**Additional Fix**:
- **File**: `backend/src/fantasia/sim/tick/movement.clj`
  - Simplified `move-agent-with-job` to use only `:target` field (removed `:to-pos` fallback)
  - This is cleaner since we now ensure `:target` is always a valid vector

### Phase 3: Verification - IN PROGRESS
Testing both camera controls and agent movement after deployment.

## Definition of Done
- [ ] Camera responds smoothly to WASD keyboard input
- [ ] Camera zooms with mouse wheel centered on cursor
- [ ] Camera pans with middle-click drag
- [ ] Agents are visible on the canvas
- [ ] Agents move when tick advances
- [ ] Agent movement is visible and smooth
- [ ] Selected agent shows path visualization

## Story Points
- Phase 1 (Camera): 2 story points (simple dependency fix) ✅ COMPLETED
- Phase 2 (Agent debug): 3 story points (investigation + potential fixes)
- Phase 3 (Verification): 1 story point (testing)

**Total**: 6 story points
