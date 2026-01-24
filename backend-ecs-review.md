# Backend Code Review: Competing Implementations and Legacy Code

## Executive Summary

This review identifies significant architectural duplication in the Gates of Aker backend, with two parallel implementations of core game systems:

1. **Legacy System**: Traditional Clojure maps and atoms in `fantasia.sim.tick.*`
2. **ECS System**: Brute ECS framework implementation in `fantasia.sim.ecs.*`

Both systems implement identical game logic (combat, movement, reproduction, etc.) with no clear migration path or integration strategy.

## Key Findings

### 1. Dual Architecture Implementation

#### Legacy Tick System (`/src/fantasia/sim/tick/`)
- **Entry Point**: `tick/core.clj:65` - `tick-once` function
- **State Management**: Uses atoms with nested maps (`*state` atom)
- **Data Structure**: Traditional Clojure maps with `:agents`, `:tiles`, `:jobs` keys
- **Core Files**:
  - `tick/movement.clj` - Agent movement logic
  - `tick/combat.clj` - Combat system
  - `tick/reproduction.clj` - Agent reproduction
  - `tick/mortality.clj` - Death processing
  - `tick/social.clj` - Social interactions

#### Brute ECS System (`/src/fantasia/sim/ecs/`)
- **Entry Point**: `ecs/tick.clj:62` - `tick-ecs-once` function  
- **State Management**: Uses `*ecs-world` atom with Brute entity system
- **Data Structure**: Component-based entities with `Position`, `Needs`, `Role`, etc.
- **Core Files**:
  - `ecs/systems/movement.clj` - Agent movement logic
  - `ecs/systems/combat.clj` - Combat system
  - `ecs/systems/reproduction.clj` - Agent reproduction
  - `ecs/systems/mortality.clj` - Death processing
  - `ecs/systems/social.clj` - Social interactions

### 2. Competing System Implementations

#### Movement Systems
- **Legacy**: `tick/movement.clj:63` - `move-agent-with-job`
- **ECS**: `ecs/systems/movement.clj:27` - `process`
- **Issue**: Both implement pathfinding and collision detection separately

#### Combat Systems  
- **Legacy**: `tick/combat.clj:117` - `process-combat!`
- **ECS**: `ecs/systems/combat.clj:186` - `process`
- **Issue**: Identical damage calculations, targeting logic, and death handling duplicated

#### Reproduction Systems
- **Legacy**: `tick/reproduction.clj:5` - `process-reproduction-step!`
- **ECS**: `ecs/systems/reproduction.clj:136` - `process-reproduction`
- **Issue**: Pregnancy, growth, and relationship mechanics implemented twice

#### Other Duplicated Systems
- **Mortality**: `tick/mortality.clj:83` vs `ecs/systems/mortality.clj:99`
- **Social**: `tick/social.clj:6` vs `ecs/systems/social.clj:132`
- **Jobs**: Legacy jobs system vs `ecs/systems/job_*.clj`

### 3. Legacy Code Not Using ECS Framework

#### Core Game Logic
- `fantasia.sim.agents.clj:9` - `update-needs` function uses legacy map structure
- `fantasia.sim.jobs.clj` - Entire job system built on legacy data structures
- `fantasia.sim.world.clj` - World state management using traditional maps

#### Spatial and Utility Systems
- `fantasia.sim.spatial.clj` - Spatial queries on legacy tile system
- `fantasia.sim.hex.clj` - Hex grid calculations (shared between systems)
- `fantasia.sim.pathing.clj` - Pathfinding for legacy system

#### Event and Memory Systems
- `fantasia.sim.events.clj` - Event generation for legacy tick
- `fantasia.sim.memories.clj` - Agent memory management
- `fantasia.sim.institutions.clj` - Social institution mechanics

### 4. Integration Challenges

#### Adapter Pattern (`ecs/adapter.clj`)
- **Purpose**: Bridge between ECS entities and legacy map format
- **Problem**: Adds complexity and performance overhead
- **Usage**: `ecs/adapter.clj:29` - `ecs->agent-map` conversion function

#### Dual Entry Points
- Server can run either legacy or ECS ticks but not both
- No clear migration strategy from legacy to ECS
- Test suites split between both systems

### 5. Testing Duplication

#### Legacy Tests (`/test/fantasia/sim/`)
- `tick_test.clj` - Legacy tick system tests
- `core_test.clj` - Core simulation tests
- `agents_test.clj` - Agent behavior tests

#### ECS Tests (`/test/fantasia/sim/ecs/`)
- `ecs_test.clj` - ECS framework tests  
- `comprehensive_test.clj` - Full ECS integration tests
- `adapter_test.clj` - Adapter pattern tests

## Recommendations

### Immediate Actions (High Priority)

1. **Choose Primary Architecture**
   - Decide between legacy vs ECS as the primary system
   - ECS is likely the better choice for scalability and maintainability

2. **Create Migration Plan**
   - Document step-by-step migration from legacy to ECS
   - Identify which systems should migrate first
   - Plan for backward compatibility during transition

3. **Consolidate Entry Points**
   - Unify `tick-once` and `tick-ecs-once` into single interface
   - Use adapter pattern temporarily during migration
   - Remove dual configuration options

### Medium-term Actions

1. **Migrate Core Systems**
   - Start with independent systems (movement, combat)
   - Progress to interconnected systems (jobs, social)
   - Finally migrate complex systems (reproduction, institutions)

2. **Unify Data Access**
   - Standardize on component-based entity access
   - Remove legacy map-based data structures
   - Update all utility functions to use ECS

3. **Consolidate Tests**
   - Migrate test coverage to primary architecture
   - Remove duplicate test suites
   - Ensure integration tests cover migrated systems

### Long-term Actions

1. **Remove Legacy Code**
   - Delete entire `fantasia.sim.tick.*` namespace after migration
   - Remove adapter pattern once no longer needed
   - Clean up any remaining map-based data structures

2. **Optimize ECS Implementation**
   - Profile and optimize ECS system performance
   - Consider ECS-specific optimizations (batching, queries)
   - Implement proper component pooling if needed

## Risk Assessment

### High Risks
- **Data Corruption**: Simultaneous use of both systems could lead to inconsistent state
- **Performance Impact**: Adapter pattern adds conversion overhead
- **Maintenance Burden**: Duplicated code doubles maintenance effort

### Medium Risks  
- **Migration Complexity**: Large codebase with complex interdependencies
- **Test Coverage Gaps**: May miss edge cases during migration
- **Feature Divergence**: Systems may evolve differently, creating compatibility issues

## Conclusion

The current dual architecture presents significant technical debt and maintenance challenges. The Brute ECS implementation appears more modern and scalable, but requires careful migration planning to avoid data loss and ensure feature parity. Immediate action is needed to choose a primary architecture and create a detailed migration strategy.