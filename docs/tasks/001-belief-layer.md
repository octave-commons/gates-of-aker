---
title: Belief Layer & Narrative Ledger
priority: high
storyPoints: 5
tags:
  - simulation
  - narrative
status: new
---

## Summary
Stand up the belief/narrative layer described in the 2026-01-13 chat so every event has both a factual outcome and a contested "what people think happened" record ([[docs/notes/design/2026-01-15-resolution-system|Resolution System]]). This enables deities to compete via belief density and lets Herald/Sun/Moon abilities meaningfully shift legitimacy instead of toggling raw stats ([[docs/notes/design/2026-01-15-card-format|Card Format]]).

## Key Outputs
- Data model for claims with confidence/alignment per group (per `docs/notes/design/2026-01-15-card-format.md#L440` / [[docs/notes/design/2026-01-15-card-format|Card Format]]).
- APIs to adjust belief weights via deity moves, rumors, audits, and taboos.
- Visible ledger so players can inspect competing versions of events and forecast social fallout.

## Acceptance Criteria
- Every event insert updates at least one claim entry plus associated audiences.
- Belief weights feed deity influence and prestige generation loops.
- Telemetry/logging proves when/how belief flips occur (critical for debugging fairness).

## References
- `docs/notes/brainstorming/2026-01-15-you-re-describing-the-exact-gap-between-a-story-optimizer-an.md#L53`
- `docs/notes/design/2026-01-15-resolution-system.md#L1`
