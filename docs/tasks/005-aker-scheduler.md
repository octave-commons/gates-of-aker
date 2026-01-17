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
Deliver the Aker phase loop where day belongs to the champion, night belongs to sleeping deities, and staying awake trades deity power for fatigue and risk. This scheduler also hosts the prestige rite UI and telegraphs for impending moves.

## Key Outputs
- Phase clock implementation with Dawn/Dusk transitions and champion sleep choice gating Night abilities (`docs/notes/design/2026-01-15-aker-the-day-night-hinge-as-the-prestige-clock.md`).
- Fatigue + recovery modeling when the champion refuses to sleep, including knock-on effects for day actions.
- Hooks for nightly telegraphs (owls, beacons, crowds) and for freezing rumor propagation during the rite.
- UI overlay showing upcoming deity move opportunities and rival probabilities (per `docs/notes/design/2026-01-15-gates-of-aker-main-zip.md`).

## Acceptance Criteria
- Champion sleep state explicitly toggles availability of "Night" moves and logs consequences of staying awake.
- Prestige rite screen pauses world resolution until spend + successor selection complete.
- Scheduler exposes metrics the Director can use (e.g., next allowable Dawn Decree) without duplicating logic elsewhere.

## References
- `docs/chats/2026-01-13_16-03-38_ChatGPT_A model that matches what you liked in RimWorld, without the....md:1315-1339`
- `docs/chats/2026-01-13_16-03-38_ChatGPT_A model that matches what you liked in RimWorld, without the....md:1818-2146`
