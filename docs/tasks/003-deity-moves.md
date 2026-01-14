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
Codify the Herald/Sun/Moon major abilities plus the three lesser deities into executable move cards with resource costs (Favor, Cred, Attention), telegraphs, counterplay, and recency penalties. Moves must be mythology-agnostic skins and respect Day/Night gating.

## Key Outputs
- Data schema for move cards (phase, costs, targets, world/belief effects, telegraph hooks, counters) per `docs/chats/...:2152-2595`.
- Implementation of the 12-card starter deck + 9 bonus variants, including Dream Courier, Dawn Decree, Veilwalk, Bind Contract, Public Relief, Warden's Reckoning, etc.
- Shared resource pools and recency tracking to discourage spam and mirror "attention heat" mechanics (`docs/chats/...:1821-1887`, `...:2111-2113`).
- Hooks for telegraphs so players observe omens before moves resolve.

## Acceptance Criteria
- Each move validates preconditions (phase + graph state) before execution and emits conflict keys for the resolution engine.
- Counters from rival domains operate through actual state changes (e.g., Sun audits vs Moon veils) rather than abstract RNG toggles.
- Move metadata is easily skinnable (roles vs. deity names) to keep mythology portability promised in the chat.

## References
- `docs/chats/2026-01-13_16-03-38_ChatGPT_A model that matches what you liked in RimWorld, without the....md:1821-2595`
