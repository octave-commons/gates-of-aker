# Packet → Facet seeding → Mentions → Tradition tiers (the full chain)

Below is a continuation that turns your “always talking” world into a **computable belief engine** without losing the vibe.

---

## 1) Emission: how an agent decides what to transmit

At any “social contact” moment (bump, near-miss, campfire, work chatter), the speaker chooses:

### A) Intent

Intent is driven by state + role:

* **warn** (danger, scarcity)
* **boast** (status, recruit)
* **soothe** (reduce panic)
* **coordinate** (work, logistics)
* **convert** (religious spread)
* **accuse** (politics, blame)
* **remember** (trauma rumination: “I can’t stop thinking about…”)

A simple selector:

$$
\text{Intent} = \arg\max_i \big(\text{NeedPressure} + \text{RoleBias} + \text{EmotionBias} + \text{Context}\big)
$$

### B) Topic nucleus

The speaker picks a nucleus from their active set:

* an **event** they’re recalling
* a **claim** they believe
* a **concept** that’s currently salient (“cold”, “hunger”, “fire”, “safety”)

Pick the highest “speak pressure”:

$$
\text{SpeakPressure}(n) = a_n \cdot s_n \cdot \text{emotionGain}(n) \cdot \text{socialValue}(n)
$$

### C) Facet bundle

They don’t transmit everything—just a bundle of facets that supports their intent.

Example:

* warn → (cold, wolves, near-river)
* boast → (enemy commander, lightning, no casualties)
* convert → (judgment, flame sigil, “answered prayer”)

Bundle choice is basically: **facets connected to nucleus that best serve intent**.

---

## 2) The “semantic packet” format (what moves through the colony)

You can run most of the system on this:

* `intent`
* `tone` (valence/arousal/fear/awe/urgency)
* `salience` (0..1)
* `topic_vec` (embedding)
* `facet_ids` (optional; explicit anchors if speaker is consciously referencing known facets)
* `claim_hint` (optional)
* `speaker_rep`
* `speaker_trust_profile` (how the listener tends to treat them: priest/leader/rival/etc.)

**Low fidelity:** no text needed
**High fidelity (optional):** generate one line *from this packet* when you want flavor

---

## 3) Attention + noise: why not everyone “hears” the same thing

This is essential for divergence.

### Listener attention gate

Listener has attention (A \in [0,1]):

$$
A = \text{clamp}\big(A_0 - \text{TaskLoad} - \text{Stress} + \text{Relationship} + \text{Novelty}\big)
$$

* TaskLoad: hauling stone, fighting, crafting → low attention
* Stress: fear/pain/hunger → tunnel vision
* Relationship: friend/leader/priest → higher attention
* Novelty: “holy shit” moments grab attention

### Environmental noise

Noise (N \in [0,1]) from wind, crowding, distance, chaos.

### Effective uptake probability

$$
P(\text{uptake}) = A \cdot (1-N) \cdot \text{salience} \cdot \text{speakerRepFactor}
$$

### Partial reception (important)

If uptake is low-but-nonzero, the listener may receive **only some facets** (or they receive tone but not content). This creates myth mutation naturally.

---

## 4) Listener association: packet → facets → graph activation

You already have the key rule: it’s a mention only if association happens.

### Step A — seed candidate nodes

Two routes:

#### Route 1: explicit facets (fast, crisp)

If packet includes `facet_ids`, seed those directly.

#### Route 2: vector search (flexible)

Use `topic_vec` to find top-k facet nodes by cosine similarity, then seed those.

### Step B — apply state bias (your “cold makes fire more likely” effect)

Agent state biases facet activation:

$$
a_f \leftarrow a_f + \beta \cdot \langle \text{StateVec}, \text{FacetAffinity}(f)\rangle
$$

So freezing boosts “cold”, which then spreads to “fire” via graph edges.

### Step C — spread activation along edges (sparse!)

Only propagate from the current active frontier (top-N nodes), not the entire graph:

$$
a_i \leftarrow \text{clamp}\Big(\lambda a_i + input_i + \sum_{j \in \mathcal{F}} a_j w_{ji},\ 0,\ 1\Big)
$$

(\mathcal{F}) = current frontier (e.g., 64 nodes)

---

## 5) Event recall from facets (facet aggregation rule)

Instead of matching event embeddings, you recall events when enough signature facets activate.

$$
a(E) = \sigma\Big(\sum_{f \in \text{Facets}(E)} w_{E,f}, a(f) - \theta_E\Big)
$$

* (w_{E,f}): facet importance (signature weight)
* (\theta_E): recall threshold (some events are easier to remember)

---

## 6) What is a “mention” (formal + weighted)

### Event mention

A mention occurs when the packet causes recall:

$$
\text{Mention}(E) \iff \Delta a(E) > \theta_m
$$

### Claim mention (attribution reinforcement)

A claim mention occurs when:

* an event is recalled
* and a claim/deity becomes active as an explanation

Example:

* “fire” → “Fire Patron” → “judgment” claim
* reinforce claim edges

### Mention weight (this replaces raw counts)

$$
W_m = \Delta a(E)\cdot \text{attention}\cdot \text{speakerRep}\cdot \text{listenerConfidence}
$$

Optionally modulate by emotional charge (awe spreads harder):

$$
W_m \leftarrow W_m \cdot (1 + \kappa \cdot \text{awe})
$$

### Mention record should include facets

Store the facet deltas that contributed:

* `event_id`
* `facet_deltas: {facet_id: Δa}`
* `claim_selected` (if any)
* `weight = W_m`

This is how you later compute “tradition” and “myth drift.”

---

## 7) From mentions to “Buzz” vs “Tradition” (your significance tiers)

You want: early chatter counts less; sustained cultural repetition counts more.

Do it with two global accumulators per event (or per event+claim):

### Buzz (B)

* increases fast with mention weight
* decays fast

$$
B_{t+1} = \lambda_b B_t + \sum W_m
$$

### Tradition (T)

* increases slowly
* only grows when the event is mentioned across **diverse social groups** and/or clergy supports it
* decays slowly

A simple diversity factor:

* compute group coverage (D \in [0,1]) (how many distinct groups have mentioned it recently)

Then:

$$
T_{t+1} = \lambda_t T_t + \rho \cdot \log(1+B_t)\cdot D \cdot \text{RitualSupport}
$$

Where `RitualSupport` comes from:

* sermons
* shrines at the site
* copied texts
* festivals
* repeated rites

This makes “later mentions worth more” naturally, because only tradition can sustain itself.

---

## 8) Verification + Attribution as emergent outputs

### Verification score (V)

Think of verification as “tradition strength + credibility”:

$$
V = T \cdot \text{WitnessCred} \cdot \text{ClergyEndorse} - \text{RebuttalPressure}
$$

### Attribution probability (P(g|E))

Attribution is a competition among claims/deities, driven by facet↔deity edges and social reinforcement.

Practical rule:

* whichever deity node gets the most activation when the event is recalled tends to win, unless clergy overrides.

---

## 9) Tier unlocks: how canonization becomes gameplay

Once an event+claim crosses thresholds:

* **Minor miracle**: (B) high, (T) low
  → faith bump, morale shift, maybe small passive buff

* **Significant miracle**: (T \ge T_1) and (P(\text{patron}|E)\ge p_1)
  → unlock/upgrade a power in that deity’s kit

* **Foundational**: (T \ge T_2) and survives rebuttals over time
  → doctrine change, new ritual, iconography expands (new facet↔deity associations)

### Power synthesis from facets (this is the fun bridge)

When a miracle is canonized, derive power “shape” from dominant facets:

* dominant physical facet → domain (fire/water/storm/etc.)
* dominant outcome facet → effect type (defense, routing enemies, fertility, healing)
* dominant affect facet → psychological profile (awe, dread, mercy, judgment)

So “winter forest fire victory” might synthesize:

* **Domain**: Fire
* **Effect**: battlefield denial / routing
* **Tone**: judgment/awe
  → *Judgment Flame*, *Winter Pyre*, *Awebrand*

---

## 10) Full loop diagram (now end-to-end)

```mermaid
flowchart TD
  A[Agent contact] --> B[Speaker selects intent]
  B --> C[Select nucleus: event/claim/concept]
  C --> D[Build facet bundle + packet]
  D --> E{Listener uptake?}
  E -- no --> X[No change]
  E -- partial --> F[Seed some facets + tone]
  E -- yes --> G[Seed facets via ids or vector]
  F --> H[State bias + spread activation]
  G --> H
  H --> I[Event recall via facet aggregation]
  I --> J{Δa(event) > threshold?}
  J -- no --> X
  J -- yes --> K[Record weighted mention + facets]
  K --> L[Update Buzz B]
  K --> M[Update Tradition T (needs diversity + ritual)]
  M --> N{Canonization threshold?}
  N -- no --> X
  N -- yes --> O[Unlock/reshape powers + myth drift]
```

---

## Next continuation (the *immediately useful* piece)

If you want to keep going, the next thing to lock is **facet bundle selection + mutation**, because that determines how myths evolve.

I can write the rules for:

* how a speaker chooses which facets to include (and which to omit)
* how partial hearing drops facets
* how retelling *systematically* swaps facets (telephone-game operators)
* how clergy interventions bias facet selection (“always mention the flame sigil”)

#game-design #fantasia #agents #mentions #facets #knowledge-graph #miracles
