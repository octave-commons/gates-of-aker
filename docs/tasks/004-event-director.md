---
title: Causal Event Director & Adversity Ledger
priority: high
storyPoints: 13
tags:
  - simulation
  - pacing
status: new
---

## Summary
Replace random RimWorld-style incidents with a causal event director that assembles candidates from faction goals, rumors, logistics, and seasonal constraints, then applies the Adversity Ledger ([[docs/notes/brainstorming/2026-01-15-you-re-describing-the-exact-gap-between-a-story-optimizer-an|Causal Director]]) to adjust pressure/relief while keeping everything diegetic.

## Key Outputs
- Event candidate builder scoring plausibility, pressure, novelty, telegraph strength, and world cost per `docs/notes/brainstorming/2026-01-15-you-re-describing-the-exact-gap-between-a-story-optimizer-an.md` ([[docs/notes/brainstorming/2026-01-15-you-re-describing-the-exact-gap-between-a-story-optimizer-an|Causal Director]]).
- Adversity Ledger tracking predicted vs observed damage and ensuring relief/escalation stay causal (`docs/notes/brainstorming/2026-01-15-you-re-describing-the-exact-gap-between-a-story-optimizer-an.md`).
- Director that nudges by accelerating/amplifying existing candidates instead of conjuring new events (`docs/notes/brainstorming/2026-01-15-you-re-describing-the-exact-gap-between-a-story-optimizer-an.md`).
- Collision-aware resolution pipeline for deity moves (conflict keys, deterministic strength with controlled noise, aftermath hooks) referencing `docs/notes/design/2026-01-15-resolution-system.md` ([[docs/notes/design/2026-01-15-resolution-system|Resolution System]]).
- Recency-based suppression per motive/method to kill "samey" loops (`docs/notes/brainstorming/2026-01-15-you-re-describing-the-exact-gap-between-a-story-optimizer-an.md`).

## Acceptance Criteria
- Each triggered event lists the faction/deity/constraint chain that caused it (audit trail).
- Relief events are always explainable via allies, markets, institutions, or spirits rather than "freebies".
- Ledger misjudge values demonstrably bias future sampling toward helpful or brutal outcomes consistent with chat description.

## References
- `docs/notes/brainstorming/2026-01-15-you-re-describing-the-exact-gap-between-a-story-optimizer-an.md`
- `docs/notes/design/2026-01-15-resolution-system.md`
