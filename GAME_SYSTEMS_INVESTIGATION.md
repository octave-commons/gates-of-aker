# Game Systems Investigation Report

## Issues Identified

### 1. **Tiles Are Black/Empty**
**Problem**: The `tiles` object in simulation state is empty: `"tiles":{}`
**Root Cause**: Biome generation creates tiles but they're not being properly stored or converted
- `backend/src/fantasia/sim/biomes.clj` generates biomes and stores them in `(:tiles world)`
- `backend/src/fantasia/sim/ecs/tick.clj:164` calls biome generation but the result isn't being merged properly
- The ECS adapter expects tiles to be in global state, but they're getting lost

**Files Involved**:
- `backend/src/fantasia/sim/ecs/tick.clj:163-166` - biome generation call
- `backend/src/fantasia/sim/biomes.clj:85-121` - biome generation logic
- `backend/src/fantasia/sim/ecs/adapter.clj:91-125` - snapshot conversion

### 2. **Entities Not Moving**
**Problem**: Agent needs are all zero: `"needs":{"warmth":0.0,"food":0.0,"sleep":0.0}`
**Root Cause**: Needs components are not being properly initialized with default values
- In `backend/src/fantasia/sim/ecs/core.clj:24`, the `Needs` component is created but many fields are `nil`
- The `needs_decay` system tries to read these nil values and crashes silently
- Movement system only works for agents with Path components, but no job assignment system creates paths

**Files Involved**:
- `backend/src/fantasia/sim/ecs/core.clj:17-42` - agent creation
- `backend/src/fantasia/sim/ecs/systems/needs_decay.clj:13-38` - needs processing
- `backend/src/fantasia/sim/ecs/systems/movement.clj:7-27` - movement logic
- `backend/src/fantasia/sim/ecs/systems/job_assignment.clj:100-126` - job assignment

### 3. **Job Queue Is Empty**
**Problem**: The jobs object is empty: `"jobs":{}`
**Root Cause**: Job creation and assignment systems are not connected
- `job_assignment.clj` looks for buildings with JobQueue components, but none exist
- `job_processing.clj` expects jobs to be assigned, but assignment system is incomplete
- No actual job providers (buildings, resource nodes) are being created

**Files Involved**:
- `backend/src/fantasia/sim/ecs/systems/job_assignment.clj:100-126` - job assignment
- `backend/src/fantasia/sim/ecs/systems/job_processing.clj:22-33` - job processing
- `backend/src/fantasia/sim/ecs/systems/simple_jobs.clj:5-16` - placeholder system

### 4. **Entities Can't Meet Needs**
**Problem**: Needs are all zero, no resources exist, no jobs to fulfill needs
**Root Cause**: Complete lack of resource generation and need fulfillment systems
- Stockpiles are empty: `"stockpiles":{}`
- Items are empty: `"items":{}`
- No resource gathering jobs exist
- No need-based job creation exists

### 5. **ECS System Crashes**
**Problem**: Multiple test failures with NullPointerException in ECS core functions
**Root Cause**: Brute entity system is receiving nil collections
- Tests show `be/add-component` and `be/remove-component` are failing
- This suggests the ECS world is not being properly initialized or passed around

**Files Involved**:
- `backend/src/fantasia/sim/ecs/core.clj` - all ECS operations
- Brute entity system integration

## Core Simulation Loop Analysis

### Main Game Loop Location
**File**: `backend/src/fantasia/sim/ecs/tick.clj:21-30`
**Function**: `run-systems`
**Current Systems**:
1. `needs-decay` - decays needs (broken due to nil values)
2. `movement` - moves agents along paths (no paths exist)
3. `agent-interaction` - commented out

### Missing Systems
1. **Resource Generation** - trees, rocks, food spawning
2. **Job Creation** - automatic job generation based on needs
3. **Path Planning** - agents need pathfinding to reach job targets
4. **Building Creation** - structures that provide jobs
5. **Inventory Management** - picking up/dropping items
6. **Need Satisfaction** - eating, sleeping, warming actions

## Frontend Rendering Analysis

### Tile Rendering
**File**: `web/src/components/SimulationCanvas.tsx:284-598`
**Logic**: 
- Iterates through hex coordinates
- Looks up tiles in `snapshot.tiles["q,r"]`
- Falls back to black fill if no tile data exists

### Entity Rendering
**File**: `web/src/components/SimulationCanvas.tsx:896-1002`
**Logic**:
- Renders agents from `snapshot.agents` array
- Uses `DEBUG_BYPASS_VISIBILITY = true` (line 903) - indicates visibility issues
- Agents exist but have all zero needs and no paths

## Integration Issues

### Backend-Frontend Disconnect
1. **Tile Key Format**: Backend uses vector keys `[q r]`, frontend expects string keys `"q,r"`
2. **Component Conversion**: ECS adapter isn't properly converting component data
3. **State Synchronization**: Global state and ECS world are getting out of sync

### System Integration Failures
1. **Job Pipeline**: Job creation → Assignment → Path Planning → Movement → Processing
2. **Resource Pipeline**: Resource Generation → Gathering → Stockpiling → Usage
3. **Need Pipeline**: Need Decay → Job Creation → Need Satisfaction

## Recommended Fixes

### Phase 1: Core Infrastructure
1. **Fix ECS initialization** - resolve nil collection errors
2. **Fix tile generation** - ensure biomes are properly stored and converted
3. **Fix agent needs** - initialize with proper default values

### Phase 2: Basic Systems
1. **Implement resource spawning** - create initial trees, rocks, food
2. **Implement basic job creation** - create jobs from resources and needs
3. **Implement simple pathfinding** - get agents moving to targets

### Phase 3: Complete Systems
1. **Implement job processing** - agents actually complete jobs
2. **Implement need satisfaction** - agents can eat, sleep, warm up
3. **Implement building system** - create structures that provide ongoing jobs

## Priority Issues to Fix Immediately

1. **Tile rendering** - fix biome generation and storage (highest visual impact)
2. **Agent needs initialization** - fix nil values causing crashes
3. **ECS core errors** - fix fundamental system stability
4. **Basic job creation** - give agents something to do

The simulation has a solid foundation but needs core systems to be properly wired together and initialized with correct default values.
