# Deities

This directory contains deity definitions for the Gates of Aker simulation.

## Purpose

Deities represent the gods of the world, each with their own:
- **Facets**: What they represent (e.g., fire, wisdom, war, love)
- **Resources**: What they require from worship (favor, faith, specific items)
- **Powers**: Special abilities they grant to followers
- **Consequences**: Side effects of using their powers (cost, risk, corruption)

## Format

Each deity is a markdown file with front matter followed by lore:

```markdown
---
name: "Apollo"
title: "Lord of Light"
facets: ["sun", "music", "prophecy", "healing"]
resources: ["favor", "faith"]
stats-required: {}
powers:
  - name: "Solar Flare"
    type: "damage"
    effect: "area 8, 20 fire damage"
    cost: "favor 0.05"
    consequences: ["fire_risk", "clears_enemies"]
  - name: "Divine Inspiration"
    type: "mood"
    effect: "all agents gain 0.1 mood for 10 ticks"
    cost: "favor 0.03"
    consequences: []
  - name: "Prophecy"
    type: "knowledge"
    effect: "reveals nearby resources"
    cost: "faith 0.1"
    consequences: []
balance:
  favor-cost-multiplier: 1.0
  corruption-per-use: 0.01
  max-followers: 10
---

# [Lore text here describing the deity]
```

## Front Matter

- `name`: Internal identifier (unique across all deities)
- `title`: Display name for UI
- `facets`: Keywords for thematic matching (used by powers, dieties, events)
- `resources`: What worship provides (favor, faith, items)
- `stats-required`: Minimum stats to access certain powers
- `balance`: Multipliers for costs, corruption caps, follower limits

## Powers

Powers come in types:
- `damage`: Deal direct damage (fire, lightning, physical)
- `mood`: Modify agent emotions temporarily
- `knowledge`: Reveal information, resources
- `summon`: Create entities, effects
- `environment`: Modify terrain, weather
- `transform`: Change entity properties temporarily

## Balance

- `favor-cost-multiplier`: Higher means more expensive
- `corruption-per-use`: Each use adds corruption (0.0 to 1.0)
- `max-followers`: Cap on concurrent worshippers
- `consequences`: Side effects of powers (risks, rewards, terrain changes)

## Integration

Deities are loaded by the simulation and:
- Provide options for favor spending
- Grant powers to agents
- Track corruption and balance
- Generate events based on power use
