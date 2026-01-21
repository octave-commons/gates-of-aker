---
name: "apollo"
title: "Lord of Light"
facets: ["sun", "music", "prophecy", "healing"]
resources: ["favor", "faith"]
stats-required: {}
powers:
  - name: "Solar Flare"
    type: "damage"
    effect: "area 8, 15 fire damage"
    cost: 0.05
    consequences: ["fire_risk", "clears_enemies"]
    cooldown: 5
  - name: "Divine Inspiration"
    type: "mood"
    effect: "all agents gain 0.1 mood for 10 ticks"
    cost: 0.03
    consequences: []
    cooldown: 3
  - name: "Oracular Sight"
    type: "knowledge"
    effect: "reveals resources within 15 tiles for 20 ticks"
    cost: 0.1
    consequences: []
balance:
  favor-cost-multiplier: 1.0
  corruption-per-use: 0.01
  max-followers: 10
---

Apollo stands at the center of the divine pantheon, his golden chariot marking the path of the sun across the sky. As Lord of Light, he grants clarity through music and reveals what lies hidden through prophecy. Those who follow him find their path illuminated and their hearts lifted by divine inspiration.

**Powers:**

1. **Solar Flare** - Calls down fire in a radius of 8, dealing 15 damage to all enemies within. The sacred flame can clear threats but risks spreading to allies.
2. **Divine Inspiration** - A song of hope that raises the spirits of all followers, granting them 0.1 mood boost for 10 ticks.
3. **Oracular Sight** - Temporarily reveals hidden resources and treasures within 15 tiles, allowing mortals to see what was meant to stay hidden.

**Worship Cost:** Apollo requires favor and faith in equal measure. His powers are precise and controlled, but his inspiration is subtle and lasting.

**Consequences:** Excessive use of Solar Flare can cause forest fires (fire_risk), and seeing too much can make mortals dependent on divine vision rather than their own wisdom.
