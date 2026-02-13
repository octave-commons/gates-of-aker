---
title: Extract fantasia.sim.spatial namespace
priority: medium
storyPoints: 3
tags:
  - refactor
  - backend
status: done
---

## Summary
Split the geometry/movement helpers out of `fantasia.sim.core` into a new pure namespace (`fantasia.sim.spatial`) so other modules and tests can reuse them without dragging the full world map.

## Scope
- Move `in-bounds?`, `manhattan`, `neighbors`, `in-bounds?`, `move-agent`, `at-trees?`, and `near-shrine?` from `backend/src/fantasia/sim/core.clj:61-85` into `backend/src/fantasia/sim/spatial.clj`.
- Ensure all callers (`tick-once`, packet builders, event runtime) require the new namespace.
- Add focused unit tests for these helpers (bounds, neighbor ordering, deterministic movement) under `backend/test/fantasia/sim/spatial_test.clj`.

## Acceptance Criteria
- `fantasia.sim.core` no longer defines any geometry helpers; it only requires `fantasia.sim.spatial`.
- Tests demonstrate that movement stays in-bounds and tree/shrine checks remain accurate.
- No behavioral changes to existing tick/broadcast logic beyond namespace requires.
