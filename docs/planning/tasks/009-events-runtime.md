---
title: Extract fantasia.sim.events.runtime namespace
priority: medium
storyPoints: 5
tags:
  - refactor
  - backend
status: done
---

## Summary
Separate the event instantiation pipeline (sampling, witness application, recall impacts) from `fantasia.sim.core` so it can evolve independently of the tick orchestrator and share code with future directors.

## Scope
- Move `gen-event`, `apply-event-to-witness`, and any closely related helper data (`witness-score`, `signature` spreading) into `backend/src/fantasia/sim/events/runtime.clj`.
- Keep dependencies limited to `fantasia.sim.events` (static archetypes), `fantasia.sim.facets`, and the new spatial/agent helpers.
- Provide tests under `backend/test/fantasia/sim/events_runtime_test.clj` that fix the RNG seed and assert event shapes + witness frontier updates.
- Update `tick-once` to require this namespace and delegate event handling through it.

## Acceptance Criteria
- `fantasia.sim.core` does not house event sampling logic anymore.
- Runtime module exposes pure-ish functions that accept world snapshots and RNG seeds.
- Coverage shows new tests hitting witness mention paths without driving full ticks.
