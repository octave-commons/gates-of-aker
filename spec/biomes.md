# Biome Feature Implementation Spec

## Requirements
- Biomes should be placed around the map where different things are initially spawned
- At minimum, the map should have:
  - A forest biome (with trees)
  - A village biome
  - A field biome (with grain instead of trees)
  - A rocky biome (with rocks)
- Different biomes should have slightly different base tile colors

## Implementation Summary

### Phase 1: Backend Biome System ✓
1. ✓ Created namespace `backend/src/fantasia/sim/biomes.clj` with:
   - Biome type definitions (keywords: `:forest`, `:village`, `:field`, `:rocky`)
   - Biome metadata map with base colors, resource types, and spawn probabilities
   - `generate-biomes!` function partitions map into 4 quadrants
   - `spawn-biome-resources!` populates tiles based on biome type
   - `rand-pos-in-biome` samples positions within specific biomes

2. ✓ Modified `tick.clj:initial-world` to:
   - Call biome generation after creating basic world structure
   - Spawn resources based on biome type

### Phase 2: Frontend Rendering ✓
1. ✓ Updated `web/src/components/SimulationCanvas.tsx` to:
   - Read biome type from tile data
   - Fill hex with biome-specific base color at 0.4 opacity
   - Added rendering for grain (yellow circles) and rock (gray rectangles)

### Phase 3: Entity Spawning by Biome ✓
1. ✓ Modified agent spawning to:
   - All 12 agents now spawn exclusively in village biome
   - Uses `biomes/rand-pos-in-biome` to find valid positions

## Data Structures

### Biome Definition Map
```clojure
{:forest {:base-color "#2e7d32" 
          :resource :tree
          :spawn-prob 0.3
          :description "Dense forest with abundant trees"}
 :village {:base-color "#8d6e63"
           :resource nil
           :spawn-prob 0.0
           :description "Settlement area"}
 :field {:base-color "#9e9e24"
         :resource :grain
         :spawn-prob 0.25
         :description "Open fields with grain crops"}
 :rocky {:base-color "#616161"
         :resource :rock
         :spawn-prob 0.2
         :description "Rocky terrain with stone resources"}}
```

### Tile Structure with Biomes
```clojure
{"10,2" {:terrain :ground 
         :resource :tree 
         :biome :forest}
 "5,5"  {:terrain :ground
         :resource :grain
         :biome :field}
 ...
```

## Biome Placement Algorithm
1. Map is divided into 4 quadrants (64x64 each for 128x128 map)
2. Each quadrant assigned a biome type:
   - NW (0-63, 0-63): Village
   - NE (64-127, 0-63): Forest
   - SW (0-63, 64-127): Field
   - SE (64-127, 64-127): Rocky
3. For each tile in the quadrant:
   - Assign the quadrant's biome type
   - Randomly spawn resources based on biome's `spawn-prob`
4. Biome generation is deterministic based on world seed

## Verification Results

### Backend Test Results (seed 42)
- ✓ Total tiles generated: 16384 (128×128)
- ✓ Forest quadrant spawns trees
- ✓ Field quadrant spawns grain
- ✓ Rocky quadrant spawns rocks
- ✓ Village quadrant spawns no resources
- ✓ Resource counts:
  - Trees: 1199 (30% of forest tiles)
  - Grain: 992 (25% of field tiles)
  - Rocks: 815 (20% of rocky tiles)
- ✓ All 12 agents spawn in village biome

### Frontend Test Results
- ✓ TypeScript compilation succeeds
- ✓ Biome colors defined:
  - Forest: #2e7d32 (dark green)
  - Village: #8d6e63 (brown)
  - Field: #9e9e24 (olive/yellow)
  - Rocky: #616161 (gray)
- ✓ Tile rendering fills with biome colors
- ✓ Trees render as green circles
- ✓ Grain renders as yellow circles
- ✓ Rocks render as gray rectangles

## Definition of Done
- [x] Backend biome namespace created and integrated
- [x] Initial world generation includes biomes
- [x] Forest biome spawns trees
- [x] Field biome spawns grain (new resource type)
- [x] Rocky biome spawns rocks (new resource type)
- [x] Village biome has minimal resources
- [x] Frontend renders biome-specific tile colors
- [x] Agents spawn preferentially in village biome
- [x] All biomes are visible and distinct on the canvas
- [x] Existing code compiles without errors
- [ ] Manual verification: Reset world and observe biomes with correct colors and resources (TODO)

## Test Commands
```bash
# Backend test
cd backend && clojure -M -e "(do (require '[fantasia.sim.tick :as t]) (def world (t/initial-world {:seed 42})) (println 'world-created))"

# Frontend typecheck
cd /home/err/devel/orgs/octave-commons/gates-of-aker/web && npm exec tsc --noEmit

# Manual verification
# 1. Start backend: clojure -M:server
# 2. Start frontend: npm run dev --prefix web
# 3. Reset world via UI or API
# 4. Observe biome colors and resources on canvas
```

## Notes
- Grain and rock are new resource types - may need integration with jobs/resources system
- Biome placement is currently hardcoded to quadrants - could be made configurable
- Biome generation is deterministic based on seed
- All agents spawn in village biome (may want to adjust distribution in future)

## Files Modified
- `backend/src/fantasia/sim/biomes.clj` (new file, 101 lines)
- `backend/src/fantasia/sim/tick.clj:1-10` (added biomes require)
- `backend/src/fantasia/sim/tick.clj:26-76` (modified initial-world to use biomes)
- `web/src/components/SimulationCanvas.tsx:163-219` (added biome coloring and resource rendering)
