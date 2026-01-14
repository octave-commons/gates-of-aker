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
Stand up the belief/narrative layer described in the 2026-01-13 chat so every event has both a factual outcome and a contested "what people think happened" record. This enables deities to compete via belief density and lets Herald/Sun/Moon abilities meaningfully shift legitimacy instead of toggling raw stats.

## Key Outputs
- Data model for claims with confidence/alignment per group (per `docs/chats/...md:1711-1794`).
- APIs to adjust belief weights via deity moves, rumors, audits, and taboos.
- Visible ledger so players can inspect competing versions of events and forecast social fallout.

## Acceptance Criteria
- Every event insert updates at least one claim entry plus associated audiences.
- Belief weights feed deity influence and prestige generation loops.
- Telemetry/logging proves when/how belief flips occur (critical for debugging fairness).

## References
- `docs/chats/2026-01-13_16-03-38_ChatGPT_A model that matches what you liked in RimWorld, without the....md:1711-1794`
- `docs/chats/2026-01-13_16-03-38_ChatGPT_A model that matches what you liked in RimWorld, without the....md:1769-1795`
