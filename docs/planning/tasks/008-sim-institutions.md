---
title: Extract fantasia.sim.institutions namespace
priority: medium
storyPoints: 2
tags:
  - refactor
  - backend
status: done
---

## Summary
Move the broadcast scheduling + mouthpiece routing code out of `fantasia.sim.core` so institutions get a clean seam for testing and future extensions (multiple temples, civic councils, etc.).

## Scope
- Extract `institution-broadcasts` and `apply-institution-broadcast` (currently at `backend/src/fantasia/sim/core.clj:217-241`) into `backend/src/fantasia/sim/institutions.clj`.
- Parameterize speakers/mouthpieces to avoid direct dependencies on the global world atom.
- Add fresh tests under `backend/test/fantasia/sim/institutions_test.clj` verifying cadence (every N ticks), facet payloads, and mouthpiece overrides.
- Replace direct calls inside `tick-once` with `fantasia.sim.institutions` functions.

## Acceptance Criteria
- Broadcast logic lives entirely in the new namespace with pure inputs/outputs.
- Tests cover both default broadcasts and mouthpiece substitutions.
- Running `clojure -X:test` exercises the new namespace without regressions in tick output.
