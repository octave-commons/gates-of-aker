---
title: Extract fantasia.sim.world namespace
priority: medium
storyPoints: 3
tags:
  - refactor
  - backend
status: done
---

## Summary
Give snapshotting and ledger attribution their own namespace so UI serialization and myth ledger math stay decoupled from the tick loop.

## Scope
- Move `snapshot`, ledger decay/update wiring, and attribution assembly (currently `backend/src/fantasia/sim/core.clj:310-398`) into `backend/src/fantasia/sim/world.clj`.
- Ensure `fantasia.sim.world` depends on `fantasia.sim.myth` for ledger math but keeps outputs UI-friendly (tick, agent summaries, ledger map).
- Add tests in `backend/test/fantasia/sim/world_test.clj` confirming snapshot structure (agents trimmed, facets sorted) and ledger attribution formatting.
- Update `tick-once` to call into `fantasia.sim.world` for both ledger updates and `snapshot` creation.

## Acceptance Criteria
- World snapshotting and ledger math live entirely in the new namespace.
- Tests cover ledger decay + attribution wiring (ensuring the existing behavior remains intact).
- The tick orchestrator simply receives a new snapshot map from `fantasia.sim.world/snapshot`.
