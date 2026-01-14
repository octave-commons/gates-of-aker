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
Replace random RimWorld-style incidents with a causal event director that assembles candidates from faction goals, rumors, logistics, and seasonal constraints, then applies the Adversity Ledger to adjust pressure/relief while keeping everything diegetic.

## Key Outputs
- Event candidate builder scoring plausibility, pressure, novelty, telegraph strength, and world cost per `docs/chats/...:1463-1580`.
- Adversity Ledger tracking predicted vs observed damage and ensuring relief/escalation stay causal (`docs/chats/...:1489-1506`).
- Director that nudges by accelerating/amplifying existing candidates instead of conjuring new events (`docs/chats/...:1414-1454`).
- Collision-aware resolution pipeline for deity moves (conflict keys, deterministic strength with controlled noise, aftermath hooks) referencing `docs/chats/...:2618-2709`.
- Recency-based suppression per motive/method to kill "samey" loops (`docs/chats/...:1528-1556`).

## Acceptance Criteria
- Each triggered event lists the faction/deity/constraint chain that caused it (audit trail).
- Relief events are always explainable via allies, markets, institutions, or spirits rather than "freebies".
- Ledger misjudge values demonstrably bias future sampling toward helpful or brutal outcomes consistent with chat description.

## References
- `docs/chats/2026-01-13_16-03-38_ChatGPT_A model that matches what you liked in RimWorld, without the....md:1406-1596`
- `docs/chats/2026-01-13_16-03-38_ChatGPT_A model that matches what you liked in RimWorld, without the....md:2604-2709`
