---
title: Extract fantasia.sim.agents namespace
priority: medium
storyPoints: 5
tags:
  - refactor
  - backend
status: done
---

## Summary
Pull the agent physiology + packet pipeline out of `fantasia.sim.core` so we can test `update-needs`, packet generation, recall, and listener application without the entire tick loop.

## Scope
- Relocate `update-needs`, `choose-packet`, `interactions`, `apply-packet-to-listener`, and `recall-and-mentions` (plus any tiny helpers they need) into `backend/src/fantasia/sim/agents.clj`.
- Introduce a small data interface (map for `packet` + world context) that keeps the new namespace pure.
- Update `core.clj:87-216` to require `fantasia.sim.agents` and delegate through it.
- Expand `backend/test/fantasia/sim/core_test.clj` tests into a dedicated `backend/test/fantasia/sim/agents_test.clj` covering needs decay, packet intents, mentions/traces, and interaction pair generation.

## Acceptance Criteria
- `fantasia.sim.core` no longer hosts packet/recall logic; it only orchestrates calls into `fantasia.sim.agents`.
- New tests exist for agent behavior and run via `clojure -X:test`.
- No changes to packet shape or recall math (verified by existing regression tests).
