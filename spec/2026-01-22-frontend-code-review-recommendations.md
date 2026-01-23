# Frontend Code Review - Cataloged Recommendations

**Date:** 2026-01-22
**Source:** Web code review conducted 2026-01-22

---
Type: review
Component: frontend
Priority: high
Status: proposed
Related-Issues: []
Milestone: 3.5
Estimated-Effort: 120-150 hours
---

## Summary

This document catalogs all frontend improvement recommendations from the code review, mapping them to existing spec files where applicable, and creating new specs where needed.

---

## Critical Issues (Immediate Action Required)

### CRIT-1: Build Failure - TypeScript Compilation Errors
**Status:** 🔴 BLOCKING
**Files:** `web/src/App.tsx`, multiple component files
**Issues:**
- Missing React types (resolved by `npm install`)
- Implicit `any` type errors: `App.tsx:327, 353, 403, 457, 502, 952, 960, 968, 976, 989, 998, 1010, 1037`
- JSX type errors throughout App.tsx and components
- Multiple `Parameter 'x' implicitly has an 'any' type` errors

**Related Specs:**
- `spec/2026-01-19-frontend-optimizations.md` - Item #7: Define Proper TypeScript Types

**Definition of Done:**
- [ ] `npm run build` completes without errors
- [ ] All implicit `any` types replaced with explicit types
- [ ] JSX types resolve correctly
- [ ] `@types/react` and related packages are properly installed

---

### CRIT-2: Tests Failing - Missing Dependencies
**Status:** 🔴 BLOCKING
**Files:** `web/`
**Issues:**
- `jsdom` dependency missing from node_modules
- Test suite cannot run

**Related Specs:**
- `spec/2026-01-15-frontend-tests.md`

**Definition of Done:**
- [ ] All dependencies installed (`npm install`)
- [ ] `npm run test` executes successfully
- [ ] Test harness properly configured

---

## Architecture Issues

### ARCH-1: Monolithic Components
**Status:** 🟡 HIGH PRIORITY
**Files:** `web/src/App.tsx` (1099 lines), `web/src/components/SimulationCanvas.tsx` (1068 lines)
**Issues:**
- App.tsx has 30+ useState hooks
- SimulationCanvas has 770+ line useEffect for rendering
- No clear separation of concerns
- Difficult to test and maintain

**Related Specs:**
- `spec/2026-01-15-app-componentization.md`
- `spec/2026-01-19-frontend-optimizations.md` - Items #8, #11, #12, #13

**Definition of Done:**
- [ ] App.tsx split into logical modules
- [ ] State grouped into `useReducer` or custom hooks
- [ ] Canvas rendering logic extracted
- [ ] WebSocket logic extracted to hook
- [ ] Each component under 200 lines

---

### ARCH-2: Type Safety
**Status:** 🟡 HIGH PRIORITY
**Files:** `web/src/types/index.ts`, `web/src/ws.ts`, `web/src/App.tsx`
**Issues:**
- `Trace = Record<string, any>` (types/index.ts:1)
- All WSMessage data fields are `any` (ws.ts:2-16)
- Extensive use of `any` throughout App.tsx
- No discriminated union types for WebSocket messages
- Type mismatches allowed by compiler

**Related Specs:**
- `spec/2026-01-19-frontend-optimizations.md` - Item #7: Define Proper TypeScript Types

**Definition of Done:**
- [ ] No `any` types in production code
- [ ] All WebSocket messages have discriminated union types
- [ ] Snapshot structure fully typed
- [ ] Traces, events, jobs have explicit interfaces
- [ ] Compilation catches type mismatches
- [ ] Type coverage > 95%

---

### ARCH-3: WebSocket Client Issues
**Status:** 🟡 HIGH PRIORITY
**Files:** `web/src/ws.ts`, `web/src/App.tsx`
**Issues:**
- No reconnection logic
- Silent error handling (ws.ts:47-49)
- WSClient recreated on every render (memoization bug)
- No retry mechanism on disconnect
- No connection health monitoring

**Related Specs:**
- `spec/2026-01-19-frontend-optimizations.md` - Items #1, #11

**Definition of Done:**
- [ ] WSClient stable across re-renders
- [ ] Automatic reconnection implemented
- [ ] Exponential backoff for retries
- [ ] Connection status monitoring
- [ ] Error handling with user feedback

---

## Code Quality Issues

### CODE-1: Magic Numbers and Constants
**Status:** 🟢 LOW PRIORITY
**Files:** `web/src/App.tsx`, `web/src/components/SimulationCanvas.tsx`, `web/src/audio.ts`
**Issues:**
- `0.11`, `0.05`, `3000`, `1500` hardcoded in App.tsx
- Hardcoded colors repeated throughout
- Some constants in CONFIG but not consistently used

**Related Specs:**
- `spec/2026-01-19-frontend-optimizations.md` - Item #16: Centralize Configuration ✅ COMPLETED

**Definition of Done:**
- [ ] All magic numbers replaced with named constants
- [ ] Constants documented with usage
- [ ] Some constants environment-configurable

---

### CODE-2: Console Logging in Production
**Status:** 🟢 LOW PRIORITY
**Files:** `web/src/App.tsx`, `web/src/utils.ts`, `web/src/components/SimulationCanvas.tsx`
**Issues:**
- Debug statements at App.tsx:434, 437, 677, 818-819, 998-1010
- Verbose console.log in utils.ts:140-143
- Console.log in SimulationCanvas.tsx:299, 309
- No controlled logging utility

**Related Specs:**
- `spec/2026-01-19-frontend-optimizations.md` - Item #9: Remove Production Console Logs ✅ COMPLETED

**Definition of Done:**
- [ ] No console.log in production builds
- [ ] Logging utility respects `VITE_LOG_LEVEL`
- [ ] Important errors still logged in development

---

### CODE-3: Empty/Unused Code
**Status:** 🟢 LOW PRIORITY
**Files:** `web/src/App.tsx`
**Issues:**
- Empty useEffect at App.tsx:170-171
- Unused imports possible

**Related Specs:**
- New spec needed or add to existing

**Definition of Done:**
- [ ] Remove empty useEffect
- [ ] Remove unused imports
- [ ] Run linter to catch dead code

---

### CODE-4: Duplicate Code
**Status:** 🟡 MEDIUM PRIORITY
**Files:** `web/src/App.tsx`, `web/src/utils.ts`
**Issues:**
- Tone sequence logic duplicated (lines 49-102)
- `colorForRole` function duplicated between files
- Repeated visibility state checks in SimulationCanvas

**Related Specs:**
- `spec/2026-01-19-frontend-optimizations.md` - Item #10: Extract Shared Utilities ✅ COMPLETED

**Definition of Done:**
- [ ] No duplicate function definitions
- [ ] Utilities have proper TypeScript types
- [ ] Unit tests cover all utility functions

---

## Performance Issues

### PERF-1: Canvas Rendering Optimization
**Status:** 🟡 HIGH PRIORITY
**Files:** `web/src/components/SimulationCanvas.tsx`
**Issues:**
- Canvas redraws entire map on every state change
- No dirty checking or ref-based flags
- Too many dependencies in useEffect
- No render optimization for non-visual changes

**Related Specs:**
- `spec/2026-01-19-frontend-optimizations.md` - Item #5: Optimize Canvas Rendering

**Definition of Done:**
- [ ] Canvas only redraws when visual data changes
- [ ] Non-visual state changes don't trigger redraw
- [ ] Camera movement doesn't cause full map redraw
- [ ] Frame rate stable at 60fps during typical usage

---

### PERF-2: Continuous Animation Loop
**Status:** 🟡 MEDIUM PRIORITY
**Files:** `web/src/components/SimulationCanvas.tsx:144-172`
**Issues:**
- Animation loop runs continuously via requestAnimationFrame
- Runs even when no keys are pressed
- Wastes CPU cycles

**Related Specs:**
- `spec/2026-01-19-frontend-optimizations.md` - Item #3: Optimize Animation Loop

**Definition of Done:**
- [ ] Animation loop only runs when WASD keys pressed
- [ ] CPU usage decreases when camera is idle
- [ ] Smooth camera movement preserved
- [ ] No memory leaks from cancelled animation frames

---

### PERF-3: Array Operations for Traces/Events
**Status:** 🟡 HIGH PRIORITY
**Files:** `web/src/App.tsx`
**Issues:**
- Uses spread + slice pattern `[...prev, incoming].slice(Math.max(0, next.length - 250))`
- Creates new arrays on every message
- GC pressure during high-frequency updates

**Related Specs:**
- `spec/2026-01-19-frontend-optimizations.md` - Item #4: Optimize Array Operations

**Definition of Done:**
- [ ] Traces array maintains max 250 items efficiently
- [ ] Events array maintains max 50 items efficiently
- [ ] GC pauses reduced during high-frequency tick updates
- [ ] Tests verify buffer behavior at limits

---

### PERF-4: Component Memoization
**Status:** 🟢 LOW PRIORITY
**Files:** `web/src/components/`
**Issues:**
- List items recreated on every render
- No React.memo for AgentCard, EventCard, TraceCard
- Unnecessary re-renders when parent updates

**Related Specs:**
- `spec/2026-01-19-frontend-optimizations.md` - Item #6: Add Component Memoization ✅ COMPLETED

**Definition of Done:**
- [ ] All list item components use React.memo
- [ ] Props that change frequently handled correctly
- [ ] No visual bugs from over-memoization

---

### PERF-5: Large Dependency Arrays
**Status:** 🟡 MEDIUM PRIORITY
**Files:** `web/src/components/SimulationCanvas.tsx:970`
**Issues:**
- Drawing effect has 10+ dependencies
- Causes frequent re-renders
- Performance bottleneck

**Related Specs:**
- New spec needed

**Definition of Done:**
- [ ] Split effects by concern
- [ ] Minimize dependency arrays
- [ ] Use refs for values that don't need to trigger re-renders

---

### PERF-6: No Virtual Scrolling
**Status:** 🟢 LOW PRIORITY
**Files:** `web/src/App.tsx:579-629`, `web/src/components/AgentList.tsx`
**Issues:**
- Long lists render all items
- Performance issues with 100+ items
- Memory scales with total items, not visible

**Related Specs:**
- `spec/2026-01-19-frontend-optimizations.md` - Item #14: Add Virtual Scrolling

**Definition of Done:**
- [ ] Traces list uses virtual scrolling
- [ ] Agent list uses virtual scrolling when >50 items
- [ ] Scroll performance smooth with 500+ items

---

## Testing Issues

### TEST-1: Low Test Coverage
**Status:** 🟡 HIGH PRIORITY
**Files:** `web/src/components/`
**Issues:**
- Component tests exist but coverage unknown
- No integration tests
- No E2E tests
- Testing harness exists but not comprehensive

**Related Specs:**
- `spec/2026-01-15-frontend-tests.md`
- `spec/2026-01-19-frontend-optimizations.md` - Item #18: Add Integration Tests

**Definition of Done:**
- [ ] Component coverage > 80%
- [ ] Integration tests for critical flows
- [ ] E2E tests for user journeys
- [ ] Tests run in CI

---

### TEST-2: No Error Boundaries
**Status:** 🟢 MEDIUM PRIORITY
**Files:** `web/src/App.tsx`, `web/src/main.tsx`
**Issues:**
- WebSocket errors can crash entire app
- Rendering errors cause white screen
- No graceful degradation
- No user-friendly error messages

**Related Specs:**
- `spec/2026-01-19-frontend-optimizations.md` - Item #15: Add Error Boundaries

**Definition of Done:**
- [ ] ErrorBoundary component created
- [ ] App wrapped in ErrorBoundary
- [ ] WebSocket errors caught and displayed
- [ ] Retry button for reconnection
- [ ] Error reporting integrated

---

### TEST-3: Canvas Not Testable
**Status:** 🟢 LOW PRIORITY
**Files:** `web/src/components/SimulationCanvas.tsx`
**Issues:**
- Canvas drawing logic mixed with state
- Difficult to unit test rendering
- No headless testing support

**Related Specs:**
- `spec/2026-01-19-frontend-optimizations.md` - Item #13: Extract Canvas Drawing Hook

**Definition of Done:**
- [ ] Drawing logic extracted to hook
- [ ] Hook testable with mock canvas context
- [ ] Drawing functions pure (no side effects)

---

## Accessibility Issues

### A11Y-1: Canvas Not Accessible
**Status:** 🟢 LOW PRIORITY
**Files:** `web/src/components/SimulationCanvas.tsx`
**Issues:**
- No ARIA labels on canvas
- Keyboard users cannot interact with simulation
- No screen reader support
- No alternative text-based view

**Related Specs:**
- `spec/2026-01-19-frontend-optimizations.md` - Item #17: Improve Canvas Accessibility

**Definition of Done:**
- [ ] Canvas has proper ARIA labels
- [ ] Keyboard navigation works for cell selection
- [ ] Important events announced to screen readers
- [ ] Focus management is correct

---

## Audio Issues

### AUDIO-1: Hardcoded Audio Parameters
**Status:** 🟢 LOW PRIORITY
**Files:** `web/src/audio.ts`
**Issues:**
- Note durations hardcoded (0.12, 0.04, 0.11, 0.05)
- No central audio configuration
- May suspend audio context without handling

**Related Specs:**
- `spec/2026-01-20-musical-sim-audio.md`
- `spec/2025-01-20-speech-bubbles-and-social-sounds.md`
- `spec/2025-01-21-unique-agent-voices.md`

**Definition of Done:**
- [ ] All audio constants in CONFIG
- [ ] Audio context management robust
- [ ] Proper cleanup on unmount

---

## Dashboard/Integration Issues

### INT-1: Frontend Components Not Integrated
**Status:** 🔴 BLOCKING
**Files:** `web/src/components/MemoryOverlay.tsx`, `web/src/components/FacetControls.tsx`
**Issues:**
- Components created but cannot be imported
- LSP import errors blocking integration
- WebSocket handler for facet configuration missing
- State management partially implemented

**Related Specs:**
- `spec/2026-01-22-milestone3.5-frontend-status.md`

**Definition of Done:**
- [ ] Import errors resolved
- [ ] Components rendered in App.tsx
- [ ] WebSocket handler for config_facets implemented
- [ ] End-to-end flow tested

---

## Cross-Reference Matrix

| Issue | Priority | Spec File | Item # | Status |
|-------|----------|------------|---------|--------|
| Build Failure | 🔴 CRITICAL | 2026-01-19-frontend-optimizations.md | #7 | BLOCKING |
| Tests Failing | 🔴 CRITICAL | 2026-01-15-frontend-tests.md | - | BLOCKING |
| Monolithic Components | 🟡 HIGH | 2026-01-15-app-componentization.md | - | TODO |
| Monolithic Components | 🟡 HIGH | 2026-01-19-frontend-optimizations.md | #8, #11, #12, #13 | TODO |
| Type Safety | 🟡 HIGH | 2026-01-19-frontend-optimizations.md | #7 | TODO |
| WebSocket Issues | 🟡 HIGH | 2026-01-19-frontend-optimizations.md | #1, #11 | TODO |
| Magic Numbers | 🟢 LOW | 2026-01-19-frontend-optimizations.md | #16 | ✅ DONE |
| Console Logging | 🟢 LOW | 2026-01-19-frontend-optimizations.md | #9 | ✅ DONE |
| Duplicate Code | 🟡 MEDIUM | 2026-01-19-frontend-optimizations.md | #10 | ✅ DONE |
| Canvas Rendering | 🟡 HIGH | 2026-01-19-frontend-optimizations.md | #5 | TODO |
| Animation Loop | 🟡 MEDIUM | 2026-01-19-frontend-optimizations.md | #3 | TODO |
| Array Operations | 🟡 HIGH | 2026-01-19-frontend-optimizations.md | #4 | TODO |
| Component Memoization | 🟢 LOW | 2026-01-19-frontend-optimizations.md | #6 | ✅ DONE |
| Virtual Scrolling | 🟢 LOW | 2026-01-19-frontend-optimizations.md | #14 | TODO |
| Test Coverage | 🟡 HIGH | 2026-01-15-frontend-tests.md | - | TODO |
| Error Boundaries | 🟢 MEDIUM | 2026-01-19-frontend-optimizations.md | #15 | TODO |
| Canvas Testing | 🟢 LOW | 2026-01-19-frontend-optimizations.md | #13 | TODO |
| Accessibility | 🟢 LOW | 2026-01-19-frontend-optimizations.md | #17 | TODO |
| Integration Issues | 🔴 CRITICAL | 2026-01-22-milestone3.5-frontend-status.md | - | BLOCKING |

---

## Implementation Roadmap

### Phase 1: Unblockers (Week 1)
**Estimated Effort:** 3-4 hours

**Priority:** 🔴 CRITICAL

1. **CRIT-1:** Fix TypeScript compilation errors
   - Add explicit types to all implicit any parameters
   - Fix JSX type resolution
   - Verify build passes

2. **CRIT-2:** Install test dependencies
   - Run `npm install` to restore jsdom
   - Verify test suite runs

3. **INT-1:** Resolve frontend integration
   - Fix LSP import errors for MemoryOverlay/FacetControls
   - Create WebSocket handler for config_facets
   - Integrate components into App.tsx

**Success Criteria:**
- [ ] `npm run build` completes without errors
- [ ] `npm run test` executes successfully
- [ ] Frontend components integrated and visible

---

### Phase 2: Architecture Refactoring (Weeks 2-4)
**Estimated Effort:** 40-60 hours

**Priority:** 🟡 HIGH

1. **ARCH-1:** Split App.tsx into logical modules
   - Extract `useWebSocket` hook
   - Extract `useSimulation` hook
   - Extract `useCanvasRenderer` hook
   - Group state into useReducer

2. **ARCH-2:** Improve type safety
   - Define proper interfaces for all WebSocket messages
   - Replace all `any` types
   - Create discriminated union types
   - Achieve >95% type coverage

3. **ARCH-3:** Fix WebSocket client
   - Implement reconnection logic
   - Add exponential backoff
   - Add connection health monitoring
   - Fix memoization bug

**Success Criteria:**
- [ ] App.tsx under 300 lines
- [ ] Zero `any` types in production code
- [ ] WebSocket stable across re-renders
- [ ] Automatic reconnection working

---

### Phase 3: Performance Optimization (Weeks 5-7)
**Estimated Effort:** 30-40 hours

**Priority:** 🟡 MEDIUM

1. **PERF-1:** Optimize canvas rendering
   - Implement dirty checking
   - Separate camera and drawing effects
   - Use refs for state comparison
   - Target 60fps

2. **PERF-2:** Fix animation loop
   - Only run when keys pressed
   - Stop loop when idle
   - Verify reduced CPU usage

3. **PERF-3:** Optimize array operations
   - Implement circular buffer
   - Remove spread + slice pattern
   - Reduce GC pressure

4. **CODE-4:** Remove duplicate code
   - Consolidate tone sequence logic
   - Extract shared utilities
   - Remove duplicate colorForRole

**Success Criteria:**
- [ ] Frame rate stable at 60fps
- [ ] CPU usage reduced when idle
- [ ] GC pauses reduced during ticks
- [ ] No duplicate code patterns

---

### Phase 4: Testing & Quality (Weeks 8-9)
**Estimated Effort:** 20-30 hours

**Priority:** 🟢 LOW

1. **TEST-1:** Increase test coverage
   - Add component tests for all new modules
   - Add integration tests
   - Target >80% coverage

2. **TEST-2:** Add error boundaries
   - Create ErrorBoundary component
   - Wrap App in boundary
   - Add retry mechanism

3. **TEST-3:** Make canvas testable
   - Extract drawing logic to hook
   - Add unit tests for rendering
   - Use mock canvas context

4. **A11Y-1:** Improve accessibility
   - Add ARIA labels
   - Implement keyboard navigation
   - Add screen reader support

**Success Criteria:**
- [ ] Test coverage >80%
- [ ] Error boundaries catch and display errors
- [ ] Canvas rendering unit tested
- [ ] WAVE/axe DevTools shows no critical issues

---

## Metrics to Track

**Code Quality:**
- TypeScript error count: Target 0
- Lines of code per file: Target <200
- `any` type usage: Target 0
- Code duplication: Target <5%

**Performance:**
- First Contentful Paint: Target <1s
- Time to Interactive: Target <2s
- Frame rate: Target 60fps
- Bundle size: Target <200KB gzipped

**Testing:**
- Test coverage: Target >80%
- Integration tests: Target >50 critical paths
- E2E tests: Target >80% user journeys

**Development:**
- Build time: Target <10s
- Test run time: Target <30s
- Type check time: Target <5s

---

## Related Documentation

- `AGENTS.md` - Frontend style guide
- `spec/2026-01-15-app-componentization.md` - Componentization plan
- `spec/2026-01-15-frontend-tests.md` - Testing strategy
- `spec/2026-01-19-frontend-optimizations.md` - Detailed optimization specs
- `spec/2026-01-22-milestone3.5-frontend-status.md` - Integration status

---

## Change Log

### 2026-01-22
- Created comprehensive catalog of frontend code review recommendations
- Mapped all recommendations to existing spec files
- Cross-referenced issues with priorities and status
- Created implementation roadmap with phases
- Added success metrics and tracking

---

**Next Steps:**
1. Begin Phase 1: Resolve critical blockers
2. Update existing specs with any missing recommendations
3. Start execution on highest-priority items
