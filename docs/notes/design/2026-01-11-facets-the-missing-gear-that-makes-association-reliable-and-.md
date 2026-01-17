# Facets: the missing gear that makes association *reliable* and myth *mutable*

Facets let an agent recall *parts* of an event without needing the whole thing to light up at once. That’s what makes your “cold + trees → fire → patron → miracle” chain feel natural.

---

## 1) Event model: “event node + facet nodes + claim nodes”

### Node types

* **Event**: the objective sim occurrence (“winter forest fire victory”)
* **Facet**: atomic handles (“winter”, “trees”, “fire”, “enemy commander”, “no casualties”, “smoke smell”, “awe”)
* **Claim**: interpretation (“Patron of Fire judged them”)
* **Deity**: iconography cluster (fire god, storm god, etc.)
* **Ritual/Icon**: symbols + practices (flame sigil, brazier rite)

### Edges (typed)

* `event -> facet` (has-facet, weighted)
* `facet <-> facet` (co-occur / causal / practical pairing)
* `event -> claim` (explanations people attach)
* `claim -> deity` (who gets credit)
* `facet -> deity` (symbolic association: fire ↔ fire god)

```mermaid
graph TD
  E[Event: Winter Forest Fire Victory]
  F1((winter))
  F2((cold))
  F3((trees))
  F4((fire))
  F5((enemy rout))
  F6((awe))
  C[Claim: "The Fire Patron judged them"]
  G[Deity: Fire Patron]

  E -->|has-facet| F1
  E -->|has-facet| F3
  E -->|has-facet| F4
  E -->|has-facet| F5
  E -->|has-facet| F6

  F2 --- F4
  F3 --- F4
  F4 -->|symbolic| G
  C -->|attributes to| G
  E -->|explained by| C
```

---

## 2) Facet taxonomy: keep it small but expressive

Use 4 buckets so you don’t explode node count:

1. **Physical**: objects, weather, terrain (fire, river, winter, lightning, smoke)
2. **Social**: people/groups/roles (commander, priests, scouts, enemies)
3. **Outcome**: deltas and consequences (harvest saved, victory swing, injuries avoided)
4. **Affective/Symbolic**: awe, dread, taboo, omen, “judgment”, “mercy”

This matters because:

* physical facets seed from sensory input,
* social facets seed from chatter,
* outcome facets seed from “what changed,”
* affective facets are what glue stories into religion.

---

## 3) How facets solve “association” cleanly

### Packet arrives → seeds facets → spreads → maybe recalls event

Instead of trying to match a packet directly to an event node, you match it to **facets**, then let the event node activate if enough of its facets are active.

### Event recall as facet aggregation

Let event activation be:

$$
a(E) = \sigma\Big(\sum_{f \in \text{Facets}(E)} w_{E,f}, a(f) - \theta_E\Big)
$$

* (a(f)) is facet activation
* (w_{E,f}) is how central that facet is to the event (weighted “signature”)
* (\theta_E) is event recall threshold
* (\sigma) is a squashing function (or just clamp)

**Interpretation:** “cold + trees” doesn’t have to match the event embedding; it just has to light up enough of the event’s facet signature (directly or via graph spread).

---

## 4) “Mention” becomes: *which facets actually got recalled*

A mention should record **what part of the event** was activated, not just “event mentioned.”

### Mention record (recommended)

* `event_id`
* `speaker_id`, `listener_id`
* `time`, `place`
* `facets_recalled`: list of facet IDs with activation deltas
* `claim_selected` (optional: which interpretation was reinforced)
* `weight` (reputation × attention × Δactivation)

This gives you:

* significance based on sustained network activation,
* *and* the ability for myths to drift (different people repeat different facet subsets).

---

## 5) Myth drift: facets make “telephone game” inevitable (in a good way)

When an agent retells an event, they don’t retell it whole—they retell their **active facets**, plus a claim if one is active.

### Retelling rule

An agent’s outgoing packet is built from:

* top-N active facets connected to the event/claim they’re thinking about
* their emotional tone
* their intent (warn/boast/convert/etc.)

So two agents can “talk about the same miracle” but emphasize different facets:

* soldier: “enemy commander died, we didn’t even fight”
* farmer: “winter fire saved the woods and scared off raiders”
* priest: “judgment flame, patron sign, kneel”

All of those reinforce the same attribution edge *if the culture is set up for it*, otherwise attribution fragments.

---

## 6) Controlling graph explosion (this is the big practical pitfall)

Facets are powerful, but you must prevent infinite “new nodes forever.”

### A) Canonical facet vocabulary (with controlled novelty)

Have two layers:

* **Canonical facets**: a bounded “ontology” (a few thousand max)
* **Ephemeral facets**: temporary, local phrases that decay quickly unless promoted

Promotion happens when:

* an ephemeral facet is repeatedly used across many agents,
* and it clusters near an existing canonical facet or deserves a new one.

### B) Merge by embedding + co-activation

If two facets:

* have high embedding similarity,
* and co-activate often,
* and share neighbors…

merge them (or alias them) into one canonical facet.

### C) Limit per-agent active set

Per agent, keep only:

* top (N) active facets (e.g., 64–256)
* plus a small set of “pinned” identity nodes (deity, doctrine, key traumas)

Everything else can exist in long-term memory at low strength, but not in the “thinking frontier.”

---

## 7) Attribution becomes facet-driven (and therefore steerable)

If your patron is a fire god, you want the culture to have strong edges:

* `fire -> Fire Patron`
* `judgment -> Fire Patron`
* `winter survival -> Fire Patron` (if you want that)

Then when “cold” activates “fire” via experience edges, it cascades into deity + claim.

### Reinforcement (social learning)

When an event is recalled and a claim is selected, reinforce:

* event↔claim
* claim↔deity
* key facets↔deity
* key facets↔claim

That’s how repeated storytelling literally strengthens the “interpretation attractor.”

---

## 8) Minimal viable facet set for your miracle system

For each event, you don’t need 100 facets. You need **a signature**:

* 3–6 **physical** facets
* 1–3 **outcome** facets
* 1–2 **affective/symbolic** facets
* optional 1–2 **social** facets

Example “winter forest fire victory” signature:

* physical: winter, cold, trees, fire, smoke
* outcome: enemy rout, no casualties
* affective: awe, judgment
* social: enemy commander (optional)

This is enough for rich association without bloat.

---

## 9) Where to go next (no questions, just the next concrete piece)

Next, you’ll want the **packet→facet seeding** rules, because that controls how “always talking” stays meaningful.

I can draft:

* how packets choose a “facet bundle” to transmit,
* how listener attention/noise gates association,
* and how you compute mention weight + tradition formation *from facet-level mentions*.

If you want the continuation in the same style, I’ll start from the packet format and walk it through to “tradition tier unlocks” using facet mentions as the atomic unit.

#game-design #fantasia #facets #knowledge-graph #miracles
