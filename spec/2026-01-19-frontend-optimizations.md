# Frontend Optimization Specifications

This document details optimization opportunities identified in the frontend codebase, organized by category with implementation guidance.

---

## Performance Optimizations

### 1. Fix WSClient Memoization

**Priority**: High  
**Story Points**: 3

**Problem**: The `WSClient` is created in a `useMemo` with empty dependencies (App.tsx:77), but the message handler is a closure that captures state. This causes the client to be recreated on every render, breaking WebSocket connections.

**Files Affected**:
- `web/src/App.tsx:77-157`

**Solution**:
1. Extract message handler logic to a `useCallback` with proper dependencies
2. Alternatively, use `useRef` for the client instance to avoid recreation
3. Ensure status handler is also properly memoized

**Definition of Done**:
- [ ] WSClient is only created once on mount
- [ ] WebSocket connections remain stable across re-renders
- [ ] No duplicate WebSocket connections in browser DevTools
- [ ] All message handlers correctly access current state via refs or callbacks
- [ ] Tests verify WebSocket stability after state updates

**Acceptance Criteria**:
- Opening DevTools Network tab shows exactly one WebSocket connection
- Rapidly adjusting levers/ticks does not cause WebSocket disconnects
- Message handlers correctly process incoming messages after multiple renders

---

### 2. Memoize Computed Values

**Priority**: Medium  
**Story Points**: 2

**Problem**: Multiple computed values in App.tsx run on every render despite only depending on specific pieces of state. This causes unnecessary calculations.

**Files Affected**:
- `web/src/App.tsx:300-318`

**Solution**:
1. Wrap `selectedTile`, `selectedAgent`, `selectedTileAgents`, `mouthpieceId`, `agents`, `jobs` in `useMemo`
2. Add appropriate dependency arrays
3. Benchmark performance difference with 100+ agents

**Definition of Done**:
- [ ] All derived state uses `useMemo` with correct dependencies
- [ ] Render profiler shows reduced render time when only UI state changes
- [ ] No stale data issues after memoization
- [ ] Types remain strict (no `any` introduced)

**Acceptance Criteria**:
- Adjusting sliders (e.g., fireToPatron) does not cause recalculation of agent lists
- React DevTools Profiler shows reduced render work for state changes unrelated to derived values

---

### 3. Optimize Animation Loop

**Priority**: Medium  
**Story Points**: 2

**Problem**: The camera movement animation loop (SimulationCanvas.tsx:73-100) runs continuously via `requestAnimationFrame`, even when no keys are pressed. This wastes CPU cycles.

**Files Affected**:
- `web/src/components/SimulationCanvas.tsx:73-100`

**Solution**:
1. Check if any keys are pressed before scheduling next frame
2. Only run `requestAnimationFrame` when active movement is needed
3. Stop loop when no movement is occurring

**Definition of Done**:
- [ ] Animation loop only runs when WASD keys are pressed
- [ ] CPU usage decreases when camera is idle
- [ ] Smooth camera movement is preserved
- [ ] No memory leaks from cancelled animation frames

**Acceptance Criteria**:
- Chrome DevTools Performance profiler shows no continuous work when no keys pressed
- Camera movement remains smooth when multiple keys pressed
- Animation frame properly cancelled on component unmount

---

### 4. Optimize Array Operations for Traces/Events

**Priority**: High  
**Story Points**: 3

**Problem**: Traces and events arrays use spread + slice pattern (`[...prev, incoming].slice(Math.max(0, next.length - 250))`) which creates new arrays on every message. With high-frequency updates, this causes GC pressure.

**Files Affected**:
- `web/src/App.tsx:96-108`

**Solution**:
1. Implement a circular buffer utility for fixed-size arrays
2. Or prepend and truncate efficiently without full spread
3. Consider using immutable.js or similar if more operations needed

**Definition of Done**:
- [ ] Traces array maintains max 250 items without unnecessary array copies
- [ ] Events array maintains max 50 items efficiently
- [ ] GC pauses reduced during high-frequency tick updates
- [ ] Tests verify buffer behavior at limits

**Acceptance Criteria**:
- Profiling with Chrome DevTools Memory tab shows fewer GC allocations during rapid ticks
- Array operations remain O(1) or O(n) where n is buffer size, not growing
- No visual artifacts in UI from array management

---

### 5. Optimize Canvas Rendering

**Priority**: High  
**Story Points**: 4

**Problem**: The canvas drawing effect (SimulationCanvas.tsx:132-395) runs on every state change, including non-visual changes. The effect has too many dependencies (snapshot, mapConfig, selectedCell, selectedAgentId, camera) causing frequent redraws.

**Files Affected**:
- `web/src/components/SimulationCanvas.tsx:132-395`

**Solution**:
1. Separate camera state effects from snapshot drawing effects
2. Use dirty checking or ref-based flags to skip unnecessary redraws
3. Consider using `useRef` for previous state comparison
4. Extract drawing logic to separate effect with minimal dependencies

**Definition of Done**:
- [ ] Canvas only redraws when visual data actually changes
- [ ] Non-visual state changes (e.g., lever values) don't trigger canvas redraw
- [ ] Camera movement doesn't cause full map redraw if data unchanged
- [ ] Frame rate remains stable at 60fps during typical usage

**Acceptance Criteria**:
- React DevTools Profiler shows canvas effect running only on visual changes
- Smooth camera panning with minimal CPU usage
- No visual flickering or missing updates

---

### 6. Add Component Memoization ✅

**Priority**: Low  
**Story Points**: 2
**Status**: Completed 2026-01-19

**Problem**: List items like `AgentCard`, `EventCard`, `TraceCard` are recreated on every render of their parent lists, even when data hasn't changed.

**Files Affected**:
- `web/src/components/AgentCard.tsx`
- `web/src/components/EventCard.tsx` (if exists)
- `web/src/components/TraceFeed.tsx` (trace items)

**Solution**:
1. Wrap list item components in `React.memo`
2. Use `useMemo` for key props that are derived
3. Add custom comparison functions if needed for deep equality

**Definition of Done**:
- [ ] All list item components use `React.memo`
- [ ] Props that change frequently are handled correctly
- [ ] No visual bugs from over-memoization
- [ ] Performance improvement measurable in profiler

**Acceptance Criteria**:
- React DevTools Profiler shows fewer re-renders for list items when parent updates
- Lists scroll smoothly even with 100+ items
- No stale data issues

---

## Code Quality Improvements

### 7. Define Proper TypeScript Types

**Priority**: High  
**Story Points**: 5

**Problem**: Excessive use of `any` type eliminates TypeScript benefits and allows runtime errors to slip through.

**Files Affected**:
- `web/src/types/index.ts` (Trace is `Record<string, any>`)
- `web/src/ws.ts` (all WSMessage data fields are `any`)
- `web/src/App.tsx` (snapshot, events, jobs are `any`)

**Solution**:
1. Define proper interfaces for Trace, WSMessage payloads, Snapshot, Event, Job
2. Update WSClient methods to use typed payloads
3. Add strict type checking to catch backend contract mismatches
4. Consider using code generation from backend spec if available

**Definition of Done**:
- [ ] No `any` types in production code (except test mocks)
- [ ] All WebSocket messages have discriminated union types
- [ ] Snapshot structure fully typed
- [ ] Traces, events, jobs have explicit interfaces
- [ ] Compilation catches type mismatches

**Acceptance Criteria**:
- `npm run build` produces no TypeScript errors
- Changing a backend field name causes TypeScript error in frontend
- VS Code IntelliSense works for all data structures
- Type coverage > 95%

---

### 8. Refactor App.tsx State Management

**Priority**: High  
**Story Points**: 5

**Problem**: App.tsx has 30+ useState hooks, making it difficult to reason about state flow and causing render thrashing.

**Files Affected**:
- `web/src/App.tsx:27-62` (all useState declarations)

**Solution**:
1. Group related state into `useReducer`:
   - Simulation state (tick, snapshot, mapConfig)
   - UI state (selectedCell, selectedAgentId, collapsed panels)
   - Lever state (fireToPatron, lightningToStorm, etc.)
   - World config (worldWidth, worldHeight, treeDensity)
2. Or create custom hooks: `useSimulation`, `useUIState`, `useLevers`

**Definition of Done**:
- [ ] App.tsx has ≤ 5 useState hooks
- [ ] Related state is grouped logically
- [ ] State updates are atomic (no intermediate render states)
- [ ] Action creators are well-typed

**Acceptance Criteria**:
- App.tsx is under 300 lines
- State transitions are clear and testable
- No race conditions from multiple setState calls
- TypeScript infers state types correctly

---

### 9. Remove Production Console Logs ✅

**Priority**: Low  
**Story Points**: 1
**Status**: Completed 2026-01-19

**Problem**: Console.log statements in production code (App.tsx:178, 186, 191, 196) bloat console output and could expose sensitive data.

**Files Affected**:
- `web/src/App.tsx:178, 186, 191, 196`

**Solution**:
1. Remove all console.log/warn statements
2. Or use a controlled logging utility that respects log level
3. Add build step that strips console statements in production

**Definition of Done**:
- [ ] No console.log in production builds
- [ ] Logging utility respects `VITE_LOG_LEVEL` environment variable
- [ ] Important errors still logged in development

**Acceptance Criteria**:
- Console is clean during normal operation
- Errors still visible when they occur
- Build production bundle has no console statements

---

### 10. Extract Shared Utilities ✅

**Priority**: Low  
**Story Points**: 1
**Status**: Completed 2026-01-19

**Problem**: Code duplication: `colorForRole` function defined twice in SimulationCanvas.

**Files Affected**:
- `web/src/components/SimulationCanvas.tsx:347-356`
- `web/src/components/SimulationCanvas.tsx:423-432`

**Solution**:
1. Extract shared utility functions to a `utils` module
2. Include: `colorForRole`, `clamp01`, `fmt` from App.tsx
3. Add unit tests for utilities

**Definition of Done**:
- [ ] No duplicate function definitions
- [ ] Utilities have proper TypeScript types
- [ ] Unit tests cover all utility functions
- [ ] All imports use shared module

**Acceptance Criteria**:
- Searching for function definitions finds single source
- Running tests shows 100% coverage for utilities
- No functionality changes after refactoring

---

## Architectural Improvements

### 11. Extract WebSocket Hook

**Priority**: High  
**Story Points**: 4

**Problem**: WebSocket logic is embedded in App.tsx, making testing difficult and the component monolithic.

**Files Affected**:
- `web/src/App.tsx:77-157`
- `web/src/ws.ts`

**Solution**:
1. Create `hooks/useWebSocket.ts` hook
2. Encapsulate WSClient creation, lifecycle, and message handling
3. Return clean interface: `{ send, status, messages, connect, close }`
4. Add error handling and reconnection logic

**Definition of Done**:
- [ ] `useWebSocket` hook exports clean API
- [ ] App.tsx uses hook instead of managing WSClient directly
- [ ] Hook handles connection, reconnection, and error states
- [ ] Messages are typed via generics
- [ ] Unit tests cover hook behavior

**Acceptance Criteria**:
- App.tsx WebSocket-related code reduced by >100 lines
- Hook can be tested independently with mock WebSocket
- Reconnection works automatically on disconnect
- Error boundaries catch WebSocket errors

---

### 12. Extract Simulation State Hook

**Priority**: High  
**Story Points**: 5

**Problem**: Simulation state management is scattered throughout App.tsx with no clear separation of concerns.

**Files Affected**:
- `web/src/App.tsx` (simulation-related state and handlers)

**Solution**:
1. Create `hooks/useSimulation.ts` hook
2. Encapsulate: tick, snapshot, mapConfig, traces, events, agents, jobs
3. Provide actions: `tick()`, `reset()`, `placeShrine()`, etc.
4. Handle initialization logic from App.tsx:160-218

**Definition of Done**:
- [ ] `useSimulation` manages all simulation state
- [ ] App.tsx uses hook with clean interface
- [ ] Initialization logic (fetch/fallback) is in hook
- [ ] Actions are well-typed and documented
- [ ] Tests cover state transitions

**Acceptance Criteria**:
- App.tsx simulation code reduced by >150 lines
- Hook can be tested with mocked backend
- Initialization works correctly for both existing and new states
- State updates are atomic

---

### 13. Extract Canvas Drawing Hook

**Priority**: Medium  
**Story Points**: 3

**Problem**: Canvas drawing logic is mixed with state management and event handling in SimulationCanvas component.

**Files Affected**:
- `web/src/components/SimulationCanvas.tsx:132-395`

**Solution**:
1. Create `hooks/useCanvasRenderer.ts` hook
2. Encapsulate all drawing logic
3. Return `render` function and configuration
4. Separate concerns: camera, zoom, drawing primitives

**Definition of Done**:
- [ ] Drawing logic extracted to hook
- [ ] SimulationCanvas focuses on event handling only
- [ ] Hook is testable with mock canvas context
- [ ] Drawing functions are pure (no side effects)

**Acceptance Criteria**:
- SimulationCanvas code reduced by >100 lines
- Drawing logic can be unit tested
- No functionality changes after extraction
- Performance maintained

---

### 14. Add Virtual Scrolling

**Priority**: Medium  
**Story Points**: 4

**Problem**: Long lists (traces, events, agents) render all items, causing performance issues with 100+ items.

**Files Affected**:
- `web/src/App.tsx:579-629` (traces list)
- `web/src/components/AgentList.tsx`
- `web/src/components/EventFeed.tsx` (if exists)

**Solution**:
1. Add `react-window` or `react-virtualized` dependency
2. Implement virtual scrolling for traces list
3. Implement virtual scrolling for agent list (50+ agents)
4. Keep item height stable for performance

**Definition of Done**:
- [ ] Traces list uses virtual scrolling
- [ ] Agent list uses virtual scrolling when >50 items
- [ ] Scroll performance is smooth with 500+ items
- [ ] Keyboard navigation works correctly

**Acceptance Criteria**:
- Lists scroll smoothly with 1000+ items
- Initial render time under 100ms
- Memory usage scales with visible items, not total
- No visual artifacts during scroll

---

### 15. Add Error Boundaries

**Priority**: Medium  
**Story Points**: 2

**Problem**: WebSocket errors or rendering errors can crash the entire app with no graceful degradation.

**Files Affected**:
- `web/src/App.tsx` (root component)
- `web/src/main.tsx` (entry point)

**Solution**:
1. Create `components/ErrorBoundary.tsx`
2. Wrap App component in ErrorBoundary
3. Add retry mechanism for WebSocket errors
4. Display user-friendly error messages

**Definition of Done**:
- [ ] ErrorBoundary component created
- [ ] App wrapped in ErrorBoundary
- [ ] WebSocket errors caught and displayed
- [ ] Retry button allows reconnection
- [ ] Error reporting (console/Sentry) integrated

**Acceptance Criteria**:
- Network errors show friendly UI instead of blank screen
- Recoverable errors allow user to continue
- Crash logging captures error details
- No white screen of death

---

### 16. Centralize Configuration ✅

**Priority**: Low  
**Story Points**: 1
**Status**: Completed 2026-01-19

**Problem**: Magic numbers scattered throughout code (HEX_SIZE, timeouts, limits).

**Files Affected**:
- `web/src/components/SimulationCanvas.tsx:23-29`
- `web/src/App.tsx` (various constants)

**Solution**:
1. Create `config/constants.ts` file
2. Export all constants with documentation
3. Replace magic numbers with named constants
4. Consider making some configurable via environment

**Definition of Done**:
- [ ] All magic numbers replaced with named constants
- [ ] Constants are documented with usage
- [ ] Some constants are environment-configurable
- [ ] No duplicate constant values

**Acceptance Criteria**:
- Searching for literal numbers finds only comments/tests
- Constants file is under 100 lines
- Changing a constant in one place affects all uses
- Documentation explains each constant's purpose

---

## Accessibility & Testing

### 17. Improve Canvas Accessibility

**Priority**: Low  
**Story Points**: 2

**Problem**: Canvas lacks ARIA labels and keyboard users cannot interact with the simulation.

**Files Affected**:
- `web/src/components/SimulationCanvas.tsx`

**Solution**:
1. Add ARIA labels to canvas element
2. Implement keyboard navigation for cell selection
3. Add screen reader announcements for important events
4. Consider alternative text-based view for screen readers

**Definition of Done**:
- [ ] Canvas has proper ARIA labels
- [ ] Keyboard navigation works (arrow keys for movement, Enter for selection)
- [ ] Important events announced to screen readers
- [ ] Focus management is correct

**Acceptance Criteria**:
- NVDA/VoiceOver announces selected cell coordinates
- Keyboard users can navigate entire map
- Tab order is logical
- WAVE or axe DevTools shows no critical issues

---

### 18. Add Integration Tests

**Priority**: Medium  
**Story Points**: 6

**Problem**: No end-to-end tests verify the full workflow from WebSocket to UI.

**Files Affected**:
- `web/` (new test files)

**Solution**:
1. Add Playwright or Cypress for E2E tests
2. Test critical flows: tick, place shrine, adjust levers, select agents
3. Test error conditions: WebSocket disconnect, network errors
4. Add Visual Regression testing for canvas

**Definition of Done**:
- [ ] E2E test framework set up
- [ ] Core user flows have tests
- [ ] WebSocket mocking for tests works
- [ ] Canvas snapshots for visual regression
- [ ] Tests run in CI

**Acceptance Criteria**:
- E2E tests cover 80% of critical user paths
- Tests complete in <5 minutes
- Flaky tests < 5%
- CI pipeline runs E2E tests

---

## Migration Strategy

**Phase 1 (Week 1-2): Quick Wins**
- Items 9, 10, 16, 6 - Low hanging fruit
- Story points: 6
- Goal: Clean up code, prepare for larger changes

**Phase 2 (Week 3-4): Performance**
- Items 1, 2, 3, 4, 5 - Core performance issues
- Story points: 14
- Goal: Eliminate performance bottlenecks

**Phase 3 (Week 5-7): Architecture**
- Items 11, 12, 13, 15, 14 - Major refactoring
- Story points: 18
- Goal: Improve maintainability and testability

**Phase 4 (Week 8-9): Quality**
- Items 7, 8, 17, 18 - Type safety and testing
- Story points: 18
- Goal: Production readiness

**Total Estimated Effort**: 56 story points (~11-13 weeks)

---

## Testing Checklist

Before deploying any optimization:
- [ ] All existing tests pass
- [ ] New tests added for changed code
- [ ] Performance benchmarks run before/after
- [ ] Manual smoke test passes (tick, place shrine, adjust levers)
- [ ] Console is clean (no errors/warnings in production)
- [ ] TypeScript compilation succeeds
- [ ] Bundle size analyzed (no significant increases)

---

## Success Metrics

Track these metrics throughout the optimization process:

- **First Contentful Paint**: Target < 1s
- **Time to Interactive**: Target < 2s
- **Frame Rate**: Target 60fps during normal operation
- **Bundle Size**: Target < 200KB gzipped
- **TypeScript Coverage**: Target > 95%
- **Test Coverage**: Target > 80%
- **Lighthouse Performance**: Target > 90
