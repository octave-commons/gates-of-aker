---
title: Extract fantasia.sim.tick namespace
priority: high
storyPoints: 8
tags:
  - refactor
  - backend
status: done
---

## Summary
After splitting spatial/agents/events/world helpers, rehouse the orchestration logic (state atom, reset/lever APIs, tick loop) in a focused `fantasia.sim.tick` namespace that only wires the subsystems together.

## Scope
- Create `backend/src/fantasia/sim/tick.clj` containing `initial-world`, state atom (`*state`), `get-state`, `reset-world!`, `set-levers!`, `place-shrine!`, `appoint-mouthpiece!`, `tick-once`, and `tick!`.
- Replace direct helper definitions inside this file with requires to `fantasia.sim.spatial`, `fantasia.sim.agents`, `fantasia.sim.institutions`, `fantasia.sim.events.runtime`, and `fantasia.sim.world`.
- Update downstream consumers (server handlers, REPL helpers) to require `fantasia.sim.tick` instead of `fantasia.sim.core`.
- Move existing tick-related tests into `backend/test/fantasia/sim/tick_test.clj`, keeping regression coverage for `tick-once`, `tick!`, and lever/shrine setters.

## Acceptance Criteria
- `fantasia.sim.core` can be deleted or reduced to a compatibility shim; tick orchestration lives in the new namespace referencing the extracted helpers.
- All references elsewhere in the repo import the new namespace.
- Tests validate that tick state still advances deterministically and lever/shrine APIs mutate the atom as before.
