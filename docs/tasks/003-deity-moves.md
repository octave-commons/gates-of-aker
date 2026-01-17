---
title: Multi-Deity Move Deck & Budgets
priority: high
storyPoints: 8
tags:
  - gameplay
  - ai
status: new
---

## Summary
Codify the Herald/Sun/Moon major abilities plus the three lesser deities into executable move cards with resource costs (Favor, Cred, Attention), telegraphs, counterplay, and recency penalties ([[docs/notes/design/2026-01-15-card-format|Card Format]]). Moves must be mythology-agnostic skins and respect Day/Night gating ([[docs/notes/design/2026-01-15-resolution-system|Resolution System]]).

## Key Outputs
- Data schema for move cards (phase, costs, targets, world/belief effects, telegraph hooks, counters) per `docs/notes/design/2026-01-15-card-format.md` ([[docs/notes/design/2026-01-15-card-format|Card Format]]).
- Shared resource pools and recency tracking to discourage spam and mirror "attention heat" mechanics (`docs/notes/design/2026-01-15-resolution-system.md` / [[docs/notes/design/2026-01-15-resolution-system|Resolution System]]).
- Hooks for telegraphs so players observe omens before moves resolve.

## Acceptance Criteria
- Each move validates preconditions (phase + graph state) before execution and emits conflict keys for the resolution engine.
- Counters from rival domains operate through actual state changes (e.g., Sun audits vs Moon veils) rather than abstract RNG toggles.
- Move metadata is easily skinnable (roles vs. deity names) to keep mythology portability promised in the chat.

## References
- `docs/notes/design/2026-01-15-card-format.md`
