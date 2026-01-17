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
Implement the champion lifecycle loop where eras end through the Aker rite ([[docs/notes/design/2026-01-15-aker-the-day-night-hinge-as-the-prestige-clock|Aker Rite]]), prestige tokens (Herald Marks, Solar Seals, Lunar Knots) are distilled, inheritance/baggage are rolled, and a successor candidate slate emerges from the current social graph ([[docs/notes/planning/2026-01-15-roadmap|Roadmap]]).

## Key Outputs
- Distillation routine covering structural deltas (bonds, conflicts, institutions, routes, narratives, taboos, violence) per `docs/notes/design/2026-01-15-aker-the-day-night-hinge-as-the-prestige-clock.md` ([[docs/notes/design/2026-01-15-aker-the-day-night-hinge-as-the-prestige-clock|Aker Rite]]).
- Prestige token economy with spend hooks for boons, world edits, inheritance slots, and baggage guarantees (`docs/notes/design/2026-01-15-aker-the-day-night-hinge-as-the-prestige-clock.md`).
- Candidate generator that ties Calling/Shadow/Bond/Taboo traits directly to recent world changes (`docs/notes/design/2026-01-15-aker-the-day-night-hinge-as-the-prestige-clock.md`).
- UI/UX for the Aker rite (freeze rumors, display spend decisions, select successor) referencing `docs/notes/design/2026-01-15-aker-the-day-night-hinge-as-the-prestige-clock.md`.

## Acceptance Criteria
- Retirement cannot occur without emitting prestige bundle + at least one inheritance and one baggage element.
- Selecting a successor consumes prestige where applicable and instantiates candidate-informed starting perks/scars.
- Telemetry proves prestige spend modifies subsequent era start state (routes, doctrines, boons).

## References
- `docs/notes/design/2026-01-15-aker-the-day-night-hinge-as-the-prestige-clock.md`
- `docs/notes/planning/2026-01-15-roadmap.md`
