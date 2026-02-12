# Phase 3: Colonist Names - Partially Completed

## Summary
Partially implemented colonist names system with Ollama integration and file persistence.

## Changes Made

### 1. Created Name Generation System
- **File**: `backend/src/fantasia/sim/names.clj` (NEW)
- **Features**:
  - Default names for each role type:
    - Priests: Roman/Latin names (Caius, Octavia, Livia, Marcus, etc.)
    - Knights: Medieval European names (Gareth, Isolde, Roland, Elaine, etc.)
    - Peasants: Simple common names (Alden, Brea, Colm, Dara, etc.)
  - Name persistence to `data/names.edn` file
  - Ollama integration to generate new names on each world start:
    - Small model: phi3:mini
    - Generates 5 names per role type
    - Parses response and adds to name list
    - Falls back to default names if Ollama unavailable
  - Functions:
    - `ensure-names-file!` - Creates default names file if missing
    - `load-names` - Loads names from file with error handling
    - `save-names!` - Saves names map to EDN file
    - `generate-names-with-ollama!` - Calls Ollama to generate new names
    - `pick-name [rng role]` - Randomly picks name from role's name list
    - `generate-names-for-world! [agent-count rng]` - Generates agent names with Ollama

### 2. Added clj-http Dependency
- **File**: `backend/deps.edn`
- **Change**: Added `clj-http/clj-http {:mvn/version "3.12.3"}` dependency
- **Reason**: Required for Ollama HTTP API calls

### 3. Updated AgentCard Display
- **File**: `web/src/components/AgentCard.tsx`
- **Change**: Updated display to show agent name prominently
- **Before**: `#{agent.id} {agent.role ?? "unknown"}`
- **After**: `{agent.name || <span>#{agent.id} <span style={{ fontWeight: "normal", color: "#666" }}>{agent.role ?? "unknown"}</span></span>}`
- **Effect**: Agent names now appear at the top of each agent card, with ID shown in smaller font if no name available

### 4. AgentList Integration
- **File**: `web/src/components/AgentList.tsx`
- **Status**: Already using `AgentCard` component for display
- **Effect**: Names automatically display in agent list since it uses `AgentCard`

## Known Issues

### File Corruption
- `backend/src/fantasia/sim/tick/initial.clj` experienced file corruption during edits
- Multiple bracket mismatches and typos were introduced
- File was restored from git but `agent-names` integration not completed
- Names namespace was added but full integration incomplete due to editing issues

### LSP Errors (Pre-existing)
These errors existed before Phase 3 work:
- `backend/src/fantasia/sim/jobs/deliver_food.clj` - parse-key-pos is private
- `backend/src/fantasia/sim/jobs/haul.clj` - parse-key-pos is private  
- `backend/src/fantasia/sim/events.clj` - unresolved symbols: winter-pyre, lightning-commander
- `backend/src/fantasia/sim/jobs/chop.clj` - parse-keypos is private
- `backend/find_warehouse.clj` - mismatched brackets

## Definition of Done
✅ Name generation system created (`fantasia.sim.names`)
✅ Default names defined for each role type (priest, knight, peasant)
✅ Name persistence to `data/names.edn` file
✅ Ollama integration for dynamic name generation
✅ Fallback to default names when Ollama unavailable
✅ clj-http dependency added for HTTP calls
✅ AgentCard displays agent name prominently
✅ AgentList displays agent names (via AgentCard)
❌ Agent creation in initial.clj not updated to assign names (file corruption issues)
❌ Names not generated on world initialization (integration incomplete)

## Next Steps
1. Fix file corruption issues in `backend/src/fantasia/sim/tick/initial.clj`
2. Complete integration of `generate-names-for-world!` in `initial-world` function
3. Ensure agents are assigned unique names on world creation
4. Resolve pre-existing LSP errors in jobs modules
5. Test name generation with Ollama running
6. Test name persistence across multiple world restarts

## Testing
Run: `cd backend && clojure -X:test` to verify backend tests still pass

## Data Flow
```
1. World starts → generate-names-for-world! called
2. Calls ensure-names-file! → creates data/names.edn if missing
3. Calls add-generated-names! → generates new names via Ollama (if available)
4. Agent creation → pick-name called with role → returns random name
5. Names persisted to file → accumulated over time
6. Frontend displays agent.name in AgentCard and AgentList
```
