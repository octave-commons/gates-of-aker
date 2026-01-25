---
type: spec
component: testing
priority: high
status: proposed
related-issues: []
estimated-effort: 13 story points
---

# Test Coverage Improvement Plan

## Current Coverage Analysis

Based on the latest coverage report:
- **Overall Coverage**: 22.20% forms, 31.31% lines
- **Critical Low Coverage Areas** (< 20%):
  - fantasia.agent-visibility: 1.85% forms, 12.31% lines
  - fantasia.sim.delta: 1.46% forms, 7.34% lines
  - fantasia.sim.spatial: 5.02% forms, 19.40% lines
  - fantasia.sim.jobs: 5.19% forms, 11.76% lines
  - fantasia.server: 3.87% forms, 16.14% lines

## Phase 1: Foundational Systems (Priority: Critical)

### 1.1 ECS Core Systems (Target: 80% coverage)
**Files to improve:**
- `fantasia.sim.ecs.core` (63.52% forms, 80.65% lines)
- `fantasia.sim.ecs.spatial` (5.02% forms, 19.40% lines)
- `fantasia.sim.ecs.tick` (32.00% forms, 39.06% lines)

**Test Strategy:**
- Entity creation, component management, system registration
- Spatial queries and neighbor finding
- Tick execution and system coordination
- Error handling for invalid operations

### 1.2 World State Management (Target: 75% coverage)
**Files to improve:**
- `fantasia.sim.world` (54.47% forms, 56.29% lines)
- `fantasia.sim.delta` (1.46% forms, 7.34% lines)

**Test Strategy:**
- World initialization and state transitions
- Delta generation and application
- State validation and consistency checks
- Integration with ECS systems

## Phase 2: Game Logic Systems (Priority: High)

### 2.1 Agent Systems (Target: 70% coverage)
**Files to improve:**
- `fantasia.sim.ecs.systems.movement` (89.17% forms, 89.29% lines) ✓ Already good
- `fantasia.sim.ecs.systems.needs-decay` (68.42% forms, 86.21% lines) ✓ Already good
- `fantasia.sim.ecs.systems.agent-interaction` (2.70% forms, 15.79% lines)
- `fantasia.sim.ecs.systems.social` (1.25% forms, 6.54% lines)
- `fantasia.sim.ecs.systems.mortality` (9.30% forms, 21.13% lines)

### 2.2 Job Systems (Target: 65% coverage)
**Files to improve:**
- `fantasia.sim.jobs` (5.19% forms, 11.76% lines)
- `fantasia.sim.ecs.systems.job_assignment` (11.91% forms, 14.10% lines)
- `fantasia.sim.ecs.systems.job_processing` (1.92% forms, 8.33% lines)
- `fantasia.sim.ecs.systems.simple-jobs` (4.76% forms, 18.18% lines)

## Phase 3: Supporting Systems (Priority: Medium)

### 3.1 Utility and Infrastructure (Target: 60% coverage)
**Files to improve:**
- `fantasia.sim.server` (3.87% forms, 16.14% lines)
- `fantasia.sim.agent-visibility` (1.85% forms, 12.31% lines)
- `fantasia.sim.time` (49.77% forms, 52.08% lines)
- `fantasia.sim.hex` (6.60% forms, 18.75% lines)

### 3.2 Content Systems (Target: 50% coverage)
**Files to improve:**
- `fantasia.sim.biomes` (10.64% forms, 14.50% lines)
- `fantasia.sim.houses` (4.50% forms, 18.52% lines)
- `fantasia.sim.names` (41.71% forms, 28.57% lines)

## Implementation Strategy

### Test Organization Principles
1. **One test file per namespace** following existing naming convention
2. **Use `deftest` with descriptive names** following `component-scenario-expected` pattern
3. **Fix deterministic seeds** using `sim/reset` with `:seed` parameter
4. **Test both success and failure paths** for each public function
5. **Include integration tests** for critical system interactions

### Test Development Workflow
1. **Read existing test patterns** from high-coverage files:
   - `fantasia.sim.myth` (99.39% forms, 100% lines)
   - `fantasia.sim.events` (97.58% forms, 98.15% lines)
   - `fantasia.sim.ecs.components` (100% forms, 100% lines)

2. **Create test helpers** for common setup patterns:
   - World initialization with test data
   - Entity creation with standard components
   - System execution with predictable inputs

3. **Add property-based tests** for pure functions using `clojure.test.check`

### Coverage Verification
- **Run coverage after each phase**: `clojure -X:coverage`
- **Target 70% overall coverage** after Phase 2
- **Target 80% overall coverage** after completion
- **Document gaps** in `/docs/notes` when coverage targets aren't met

## Success Metrics

### Quantitative Goals
- Phase 1: Increase overall coverage from 22.20% to 40%
- Phase 2: Increase overall coverage from 40% to 60%
- Phase 3: Increase overall coverage from 60% to 70%
- Zero regressions in currently well-tested areas

### Qualitative Goals
- All critical game loops have test coverage
- Test suite runs in under 30 seconds
- Tests provide meaningful error messages
- Tests serve as documentation for system behavior

## Next Steps

1. **Review and approve** this plan
2. **Begin Phase 1** with ECS core systems
3. **Set up coverage tracking** in CI pipeline
4. **Document test patterns** for future developers
5. **Regular coverage reviews** during standups