---
title: Aker Day/Night Scheduler & Sleep Tradeoffs
priority: medium
storyPoints: 5
tags:
  - ui
  - systems
status: new
---

## Summary
Deliver the Aker phase loop where day belongs to the champion, night belongs to sleeping deities, and staying awake trades deity power for fatigue and risk ([[docs/notes/design/2026-01-15-aker-the-day-night-hinge-as-the-prestige-clock|Aker Rite]]). This scheduler also hosts the prestige rite UI and telegraphs for impending moves ([[docs/notes/planning/2026-01-15-roadmap|Roadmap]]).

## Key Outputs
- Phase clock implementation with Dawn/Dusk transitions and champion sleep choice gating Night abilities (`docs/notes/design/2026-01-15-aker-the-day-night-hinge-as-the-prestige-clock.md` / [[docs/notes/design/2026-01-15-aker-the-day-night-hinge-as-the-prestige-clock|Aker Rite]]).
- Fatigue + recovery modeling when the champion refuses to sleep, including knock-on effects for day actions.
- Hooks for nightly telegraphs (owls, beacons, crowds) and for freezing rumor propagation during the rite.
- UI overlay showing upcoming deity move opportunities and rival probabilities (per `docs/notes/design/2026-01-15-gates-of-aker-main-zip.md` / [[docs/notes/design/2026-01-15-gates-of-aker-main-zip|Gates of Aker Main Zip]]).

## Acceptance Criteria
- Champion sleep state explicitly toggles availability of "Night" moves and logs consequences of staying awake.
- Prestige rite screen pauses world resolution until spend + successor selection complete.
- Scheduler exposes metrics the Director can use (e.g., next allowable Dawn Decree) without duplicating logic elsewhere.

## References
- `docs/notes/design/2026-01-15-aker-the-day-night-hinge-as-the-prestige-clock.md` ([[docs/notes/design/2026-01-15-aker-the-day-night-hinge-as-the-prestige-clock|Aker Rite]])
- `docs/notes/planning/2026-01-15-roadmap.md` ([[docs/notes/planning/2026-01-15-roadmap|Roadmap]])
