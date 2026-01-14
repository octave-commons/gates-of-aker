---
title: Prestige Retirement & Successor Draft
priority: high
storyPoints: 8
tags:
  - systems
  - progression
status: new
---

## Summary
Implement the champion lifecycle loop where eras end through the Aker rite, prestige tokens (Herald Marks, Solar Seals, Lunar Knots) are distilled, inheritance/baggage are rolled, and a successor candidate slate emerges from the current social graph.

## Key Outputs
- Distillation routine covering structural deltas (bonds, conflicts, institutions, routes, narratives, taboos, violence) per `docs/chats/...:1172-1259`.
- Prestige token economy with spend hooks for boons, world edits, inheritance slots, and baggage guarantees (`docs/chats/...:1207-1290`).
- Candidate generator that ties Calling/Shadow/Bond/Taboo traits directly to recent world changes (`docs/chats/...:1271-1309`).
- UI/UX for the Aker rite (freeze rumors, display spend decisions, select successor) referencing `docs/chats/...:1315-1339`.

## Acceptance Criteria
- Retirement cannot occur without emitting prestige bundle + at least one inheritance and one baggage element.
- Selecting a successor consumes prestige where applicable and instantiates candidate-informed starting perks/scars.
- Telemetry proves prestige spend modifies subsequent era start state (routes, doctrines, boons).

## References
- `docs/chats/2026-01-13_16-03-38_ChatGPT_A model that matches what you liked in RimWorld, without the....md:1170-1364`
- `docs/chats/2026-01-13_16-03-38_ChatGPT_A model that matches what you liked in RimWorld, without the....md:1315-1364`
