---
name: "artemis"
title: "Goddess of the Hunt"
facets: ["moon", "hunting", "wilderness", "nature"]
resources: ["favor", "faith"]
stats-required: {}
powers:
  - name: "Moonlight Ambush"
    type: "damage"
    effect: "single target 40 damage"
    cost: 0.03
    cooldown: 4
    consequences: ["prey_vulnerable", "hunting_success"]
  - name: "Wild Grace"
    type: "mood"
    effect: "all agents near wilderness gain 0.02 mood per tick"
    cost: 0.02
    consequences: []
  - name: "Pack Unity"
    type: "summon"
    effect: "spawns 3 wolves at temple, follow for 20 ticks"
    cost: 0.05
    consequences: ["wild_risk", "wolf_summon"]
balance:
  favor-cost-multiplier: 0.8
  corruption-per-use: 0.005
  max-followers: 15

---

Artimis roams through the wilderness under moonlight, goddess of the hunt. Her followers gain advantage in tracking prey, moving silently through forests, and summoning wolf packs for coordinated attacks. She teaches that true hunters work with the land, not against it.

**Powers:**

1. **Moonlight Ambush** - Deals 40 damage to a single target from shadows. High power but requires precision timing and close range.
2. **Wild Grace** - Grants passive mood boost to all agents in wilderness areas, connecting them to the natural world.
3. **Pack Unity** - Summons 3 wolves that follow the highest-will follower for 20 ticks, creating a temporary hunting pack.

**Worship Cost:** Artimis demands favor but is generous with her gifts. Her powers are practical and enhance survival rather than dominate. She costs 0.8 favor (very efficient) but expects her followers to respect the balance of the wild.

**Consequences:** Summoning too many wolves can disrupt the ecosystem (wild_risk), and excessive hunting can drive prey to extinction if not balanced.
