1) `docs/notes/world-schema.md` (MVP authoritative world shape)
===============================================================

```md
# Gates of Aker — World Schema (MVP) #gates-of-aker #schema #world

This document describes the **authoritative backend world map** we will converge toward while building:
- hex map + walls
- colony jobs/items
- champion day control + sleep/night gate
- 6 neighbor factions with 6 decks of cards (one per direction)

## Principles

- Backend is authoritative for:
  - map passability
  - jobs + inventories
  - faction actions
  - card resolution + event chain progression
- Frontend can do previews, but must not “decide truth.”
- Powers do not spawn raids directly. They emit:
  - **Signs** (telegraphs)
  - **Modifiers** (pressure/constraints)
  - which generate **Event Candidates** (causal world events)

---

## Top-level World Map

We keep existing keys where possible, and add new ones.

```clojure
{:seed 1
 :tick 0

;; Aker loop
 :phase :day                   ;; :day | :night
 :champion {:id 100            ;; entity id (often also in :agents)
            :asleep? false
            :fatigue 0.0
            :intent nil}       ;; input intent from player

;; Hex map
 :map {:kind :hex
       :layout :pointy
       :bounds {:shape :radius :r 18}}   ;; start radius-disk for clean 6 neighbors

;; Sparse tiles (store only non-default tiles)
 ;; coord = [q r] axial
 :tiles
 { [0 0]  {:terrain :ground}
   [2 -1] {:terrain :ground :resource :tree}
   [1 0]  {:terrain :ground :structure :wall}
   [0 1]  {:terrain :ground :items {:wood 12}} }

;; Entities (initially you can keep your :agents vector)
 :agents
 [{:id 0 :pos [0 0] :role :priest ...}
  {:id 1 :pos [1 0] :role :knight ...}
  {:id 100 :pos [0 0] :role :champion :player-controlled? true ...}]

;; Colony systems
 :jobs {:queue []              ;; ordered work items
        :claims {}             ;; job-id -> entity-id (reservation)
        :cooldowns {}}         ;; optional

:zones {:stockpiles []}       ;; optional early

;; Factions (player + 6 neighbors)
 :factions
 {:faction/player {:id :faction/player
                   :name "Home"
                   :settlement-pos [0 0]
                   :attitude {}    ;; toward others
                   :deity {:id :deity/player :budget {:favor 8 :cred 6 :attention 0}}
                   :deck {:draw [] :hand [] :discard [] :cooldowns {}}}

 :faction/north  {...}
  :faction/ne     {...}
  :faction/se     {...}
  :faction/south  {...}
  :faction/sw     {...}
  :faction/nw     {...}}

;; Card + event engine
 :cards {}                    ;; card-id -> card data
 :signs []                     ;; recent telegraphs (short-lived)
 :modifiers []                 ;; timed pressures/constraints
 :event {:candidates []        ;; generated at boundaries
         :queue []             ;; chosen chains to progress
         :log []}              ;; resolved events (for UI + myth)

;; Existing myth layer (keep!)
 :edges {...}
 :ledger {...}
 :recent-events [...]
 :traces [...]}
```

* * *

Tile schema (MVP)
-----------------

A tile is mostly passability + resources + items.

```c
{:terrain   :ground            ;; later: :water :marsh :rock etc
 :structure :wall              ;; nil | :wall | :door | :floor ...
 :resource  :tree              ;; nil | :tree | :ore ...
 :items     {:wood 12 :food 3} ;; item stacks (simple map)
 :flags     #{:shrine}         ;; optional markers
}
```

**Passability rule (MVP):**

*   blocked if `:structure == :wall`

*   trees are passable early (or make them block if you want “chop to clear”)


* * *

Cards (deity powers) — data-only schema
---------------------------------------

```clojure
{:card/id    :herald/whisper-network
 :card/name  "Whisper Network"
 :card/phase :always             ;; :day | :night | :always
 :card/cost  {:favor 1 :cred 0 :attention 1}

;; collision keys: anything can be a key
 ;; use tuples to avoid ambiguity
 :card/keys  {:primary   [[:route :route/main]]
              :secondary [[:settlement :faction/north]]}

:card/tags  #{:herald :info :route}

;; effect is data to interpret:
 ;; - emits signs (telegraphs)
 ;; - sets modifiers (timed constraints/boosts)
 :card/effect
 {:emit/signs
  [{:sign/type :gossip
    :sign/keys [[:route :route/main]]
    :sign/hint :messages-move-faster}]

 :apply/mods
  [{:mod/type :route/latency
    :mod/keys [[:route :route/main]]
    :mod/delta -1
    :mod/ttl 2}]}}
```

* * *

Signs (telegraphs)
------------------

Signs are what make the world feel _logical_ instead of “RimWorld random.”

```clojure
{:sign/type :scouts-seen
 :sign/keys [[:route :route/main] [:settlement :faction/player]]
 :sign/origin [:faction :faction/north]   ;; optional
 :sign/hint :pressure-building
 :sign/tick 123}
```

* * *

Modifiers (pressure/constraints)
--------------------------------

Modifiers are the “physics” of story pacing.

```c
{:mod/type  :veil               ;; or :tomorrow-law, :route/latency, :doubt, :sanction, ...
 :mod/keys  [[:tile [3 -2]]]    ;; scope of effect
 :mod/value true
 :mod/ttl   2
 :mod/source :moon/veilwalk}
```

* * *

Event Chains (world events, not god magic)
------------------------------------------

Event families create sequences with telegraphs → escalation → resolution.

```clojure
{:ev/id       :ev-ult-0007
 :ev/family   :ultimatum-raid-feud
 :ev/keys     [[:settlement :faction/player] [:faction :faction/north]]
 :ev/step     :ultimatum
 :ev/steps    [:telegraph :ultimatum :response :attack-or-settle :aftermath]
 :ev/params   {:aggressor :faction/north
               :motive :loot
               :target :faction/player}
 :ev/created-tick 200}
```

* * *

Myth integration hook (future-proof)
------------------------------------

Add a single bridge:

*   every resolved event emits a compact “world event” map into `:event/log`

*   myth consumes those into `:mentions` / `:ledger` (instead of synthetic RNG)


Example:

```c
{:world.event/type :raid
 :world.event/keys [[:faction :faction/north] [:faction :faction/player]]
 :world.event/tick 207
 :world.event/tags #{:violence :night :loot}
 :world.event/summary "Bandits from the North raided the market gate."}
```
```less
#tags #gates-of-aker #schema #world #aker
```

* * *

2) `backend/resources/decks/neighbor-decks.edn` (6 decks, starter cards)
========================================================================

This is **data-only EDN**: a shared card pool plus 6 neighbor decks (each deck is a draw pile list; duplicates allowed). It’s intentionally small so you can ship “decks” early.

```edn
{:cards
 {;; -------------------------
  ;; Shared “Herald-ish” cards
  ;; -------------------------
  :herald/whisper-network
  {:card/id :herald/whisper-network
   :card/name "Whisper Network"
   :card/phase :always
   :card/cost {:favor 1 :cred 0 :attention 1}
   :card/keys {:primary [[:route :route/main]]}
   :card/tags #{:herald :info :route}
   :card/effect {:emit/signs [{:sign/type :gossip
                               :sign/keys [[:route :route/main]]
                               :sign/hint :messages-move-faster}]
                :apply/mods  [{:mod/type :route/latency
                               :mod/keys [[:route :route/main]]
                               :mod/delta -1
                               :mod/ttl 2
                               :mod/source :herald/whisper-network}]}}
  :herald/leak-proof
  {:card/id :herald/leak-proof
   :card/name "Leak Proof"
   :card/phase :always
   :card/cost {:favor 1 :cred 1 :attention 2}
   :card/keys {:primary [[:claim :claim/contested]]}
   :card/tags #{:herald :exposure :claim}
   :card/effect {:emit/signs [{:sign/type :evidence-leaked
                               :sign/keys [[:claim :claim/contested]]
                               :sign/hint :someone-has-receipts}]
                :apply/mods  [{:mod/type :claim/confidence
                               :mod/keys [[:claim :claim/contested]]
                               :mod/delta +1
                               :mod/ttl 2
                               :mod/source :herald/leak-proof}]}}
  :herald/market-parley
  {:card/id :herald/market-parley
   :card/name "Market Parley"
   :card/phase :day
   :card/cost {:favor 1 :cred 0 :attention 2}
   :card/keys {:primary [[:tile :loc/market]]}
   :card/tags #{:herald :trade :diplomacy}
   :card/effect {:emit/signs [{:sign/type :parley-called
                               :sign/keys [[:tile :loc/market]]
                               :sign/hint :talk-before-blood}]
                :apply/mods  [{:mod/type :trade/opening
                               :mod/keys [[:tile :loc/market]]
                               :mod/value true
                               :mod/ttl 1
                               :mod/source :herald/market-parley}]}}
  :herald/dream-courier
  {:card/id :herald/dream-courier
   :card/name "Dream Courier"
   :card/phase :night
   :card/cost {:favor 2 :cred 0 :attention 1}
   :card/keys {:primary [[:person :person/target]]}
   :card/tags #{:herald :dream :belief}
   :card/effect {:emit/signs [{:sign/type :omens
                               :sign/keys [[:person :person/target]]
                               :sign/hint :restless-night}]
                :apply/mods  [{:mod/type :rumor/spread
                               :mod/keys [[:person :person/target]]
                               :mod/delta +1
                               :mod/ttl 2
                               :mod/source :herald/dream-courier}]}}

 ;; ----------------------
  ;; Shared “Sun-ish” cards
  ;; ----------------------
  :sun/dawn-decree-curfew
  {:card/id :sun/dawn-decree-curfew
   :card/name "Dawn Decree: Curfew"
   :card/phase :night
   :card/cost {:favor 2 :cred 1 :attention 2}
   :card/keys {:primary [[:settlement :faction/target]]}
   :card/tags #{:sun :law :order}
   :card/effect {:emit/signs [{:sign/type :patrol-prep
                               :sign/keys [[:settlement :faction/target]]
                               :sign/hint :a-law-is-coming}]
                :apply/mods  [{:mod/type :tomorrow-law
                               :mod/keys [[:settlement :faction/target]]
                               :mod/value :curfew
                               :mod/ttl 1
                               :mod/source :sun/dawn-decree-curfew}]}}
  :sun/seal-of-sanction
  {:card/id :sun/seal-of-sanction
   :card/name "Seal of Sanction"
   :card/phase :always
   :card/cost {:favor 1 :cred 1 :attention 0}
   :card/keys {:primary [[:claim :claim/official]]}
   :card/tags #{:sun :legitimacy :canon}
   :card/effect {:emit/signs [{:sign/type :sealed-record
                               :sign/keys [[:claim :claim/official]]
                               :sign/hint :official-version-hardened}]
                :apply/mods  [{:mod/type :claim/stickiness
                               :mod/keys [[:claim :claim/official]]
                               :mod/value true
                               :mod/ttl 3
                               :mod/source :sun/seal-of-sanction}]}}
  :sun/public-trial
  {:card/id :sun/public-trial
   :card/name "Public Trial"
   :card/phase :day
   :card/cost {:favor 2 :cred 0 :attention 3}
   :card/keys {:primary [[:settlement :faction/target] [:claim :claim/contested]]}
   :card/tags #{:sun :trial :public}
   :card/effect {:emit/signs [{:sign/type :witnesses-summoned
                               :sign/keys [[:claim :claim/contested]]
                               :sign/hint :trial-incoming}]
                :apply/mods  [{:mod/type :event/boost
                               :mod/keys [[:event-family :trial-verdict-enforcement]]
                               :mod/delta +1
                               :mod/ttl 2
                               :mod/source :sun/public-trial}]}}
  :sun/public-relief
  {:card/id :sun/public-relief
   :card/name "Public Relief"
   :card/phase :always
   :card/cost {:favor 2 :cred 0 :attention 1}
   :card/keys {:primary [[:settlement :faction/target]]}
   :card/tags #{:sun :relief :stability}
   :card/effect {:emit/signs [{:sign/type :granaries-open
                               :sign/keys [[:settlement :faction/target]]
                               :sign/hint :aid-distributed}]
                :apply/mods  [{:mod/type :settlement/stability
                               :mod/keys [[:settlement :faction/target]]
                               :mod/delta +2
                               :mod/ttl 2
                               :mod/source :sun/public-relief}]}}

 ;; -----------------------
  ;; Shared “Moon-ish” cards
  ;; -----------------------
  :moon/veilwalk
  {:card/id :moon/veilwalk
   :card/name "Veilwalk"
   :card/phase :night
   :card/cost {:favor 2 :cred 0 :attention 1}
   :card/keys {:primary [[:tile :loc/market]]}
   :card/tags #{:moon :stealth :veil}
   :card/effect {:emit/signs [{:sign/type :lanterns-fail
                               :sign/keys [[:tile :loc/market]]
                               :sign/hint :visibility-drops}]
                :apply/mods  [{:mod/type :veil
                               :mod/keys [[:tile :loc/market]]
                               :mod/value true
                               :mod/ttl 2
                               :mod/source :moon/veilwalk}]}}
  :moon/mask-and-doubt
  {:card/id :moon/mask-and-doubt
   :card/name "Mask & Doubt"
   :card/phase :always
   :card/cost {:favor 1 :cred 0 :attention 0}
   :card/keys {:primary [[:claim :claim/contested]]}
   :card/tags #{:moon :misinfo :claim}
   :card/effect {:emit/signs [{:sign/type :pamphlets
                               :sign/keys [[:claim :claim/contested]]
                               :sign/hint :confidence-frays}]
                :apply/mods  [{:mod/type :claim/confidence
                               :mod/keys [[:claim :claim/contested]]
                               :mod/delta -1
                               :mod/ttl 2
                               :mod/source :moon/mask-and-doubt}]}}
  :moon/night-market
  {:card/id :moon/night-market
   :card/name "Night Market"
   :card/phase :night
   :card/cost {:favor 2 :cred 0 :attention 1}
   :card/keys {:primary [[:tile :loc/market]]}
   :card/tags #{:moon :blackmarket :trade}
   :card/effect {:emit/signs [{:sign/type :shadow-stalls
                               :sign/keys [[:tile :loc/market]]
                               :sign/hint :goods-move-in-the-dark}]
                :apply/mods  [{:mod/type :event/boost
                               :mod/keys [[:event-family :trade-scarcity-blackmarket]]
                               :mod/delta +1
                               :mod/ttl 2
                               :mod/source :moon/night-market}]}}
  :moon/false-flag
  {:card/id :moon/false-flag
   :card/name "False Flag"
   :card/phase :day
   :card/cost {:favor 2 :cred 0 :attention 3}
   :card/keys {:primary [[:faction :faction/target] [:faction :faction/third]]}
   :card/tags #{:moon :deception :war}
   :card/effect {:emit/signs [{:sign/type :contradictory-reports
                               :sign/keys [[:faction :faction/target]]
                               :sign/hint :blame-shifts}]
                :apply/mods  [{:mod/type :faction/hostility
                               :mod/keys [[:faction :faction/target] [:faction :faction/third]]
                               :mod/delta +1
                               :mod/ttl 3
                               :mod/source :moon/false-flag}]}}

 ;; -----------------------
  ;; Shared “Wild/Taboo” card
  ;; -----------------------
  :wild/wardens-reckoning
  {:card/id :wild/wardens-reckoning
   :card/name "Warden’s Reckoning"
   :card/phase :always
   :card/cost {:favor 0 :cred 0 :attention 0}
   :card/keys {:primary [[:region :region/wild]]}
   :card/tags #{:wild :taboo :consequence}
   :card/effect {:emit/signs [{:sign/type :animals-vanish
                               :sign/keys [[:region :region/wild]]
                               :sign/hint :the-wild-watches}]
                :apply/mods  [{:mod/type :route/attrition
                               :mod/keys [[:route :route/main]]
                               :mod/delta +1
                               :mod/ttl 3
                               :mod/source :wild/wardens-reckoning}]}}}

:decks
 {;; Each neighbor faction = one deck.
  ;; Start small, bias each deck by theme.
  ;; Draw piles can include duplicates to shape identity.

 :faction/north
  {:faction/name "Tribunal of Dawn"
   :theme #{:sun :law}
   :draw [:sun/dawn-decree-curfew
          :sun/seal-of-sanction
          :sun/public-trial
          :sun/seal-of-sanction
          :herald/leak-proof
          :herald/whisper-network
          :moon/mask-and-doubt
          :sun/public-relief]}

 :faction/ne
  {:faction/name "Caravan Chorus"
   :theme #{:herald :trade}
   :draw [:herald/whisper-network
          :herald/whisper-network
          :herald/market-parley
          :herald/leak-proof
          :moon/night-market
          :sun/seal-of-sanction
          :sun/public-relief
          :moon/mask-and-doubt]}

 :faction/se
  {:faction/name "Syndicate of the Veil"
   :theme #{:moon :stealth}
   :draw [:moon/veilwalk
          :moon/night-market
          :moon/mask-and-doubt
          :moon/false-flag
          :herald/leak-proof
          :herald/dream-courier
          :sun/seal-of-sanction
          :wild/wardens-reckoning]}

 :faction/south
  {:faction/name "Hearth Compact"
   :theme #{:sun :relief}
   :draw [:sun/public-relief
          :sun/public-relief
          :herald/market-parley
          :herald/whisper-network
          :sun/seal-of-sanction
          :moon/mask-and-doubt
          :wild/wardens-reckoning
          :herald/leak-proof]}

 :faction/sw
  {:faction/name "Circle of the Warden"
   :theme #{:wild :taboo}
   :draw [:wild/wardens-reckoning
          :wild/wardens-reckoning
          :moon/veilwalk
          :moon/mask-and-doubt
          :herald/dream-courier
          :herald/whisper-network
          :sun/public-relief
          :sun/seal-of-sanction]}

 :faction/nw
  {:faction/name "Iron Oath Pact"
   :theme #{:moon :sun :war}
   :draw [:moon/false-flag
          :sun/dawn-decree-curfew
          :herald/leak-proof
          :moon/mask-and-doubt
          :herald/whisper-network
          :moon/veilwalk
          :sun/seal-of-sanction
          :sun/public-trial]}}}
```

* * *

3) `backend/src/fantasia/sim/drama.clj` (3 event families + candidate generation)
=================================================================================

This is the “bridge” layer: **modifiers + signs** → **event candidates** → **chain progression**. It’s intentionally Clojure-ish pseudocode that matches your current `sim/core.clj` style.

```c
(ns fantasia.sim.drama
  (:require [clojure.set :as set]))

;; -------------------------
;; Helpers
;; -------------------------

(defn clamp01 [x] (-> x (max 0.0) (min 1.0)))

(defn has-mod?
  "Return true if any active modifier matches a predicate."
  [world pred]
  (boolean (some pred (get world :modifiers))))

(defn mod-strength
  "Sum deltas of matching mods (useful for event boosts)."
  [world pred]
  (reduce
    (fn [acc m] (+ acc (double (or (:mod/delta m) 0.0))))
    0.0
    (filter pred (get world :modifiers))))

(defn novelty
  "Very simple anti-samey: if tag was recent, novelty drops.
   Store recency however you like; this matches your existing pattern."
  [world tag]
  (let [r (get-in world [:recency tag] 0)]
    (clamp01 (/ (- 5 r) 5.0))))

(defn candidate
  [{:keys [id family keys steps step params telegraph]} score]
  {:ev/id id
   :ev/family family
   :ev/keys keys
   :ev/steps steps
   :ev/step step
   :ev/params params
   :ev/telegraph telegraph
   :ev/score score
   :ev/created-tick (:tick score)})

(defn score
  [world {:keys [plausibility pressure telegraph novelty] :as s}]
  ;; keep it deterministic early; you can add seeded noise later
  (assoc s :tick (:tick world)
           :score (+ (* 2.0 plausibility)
                     (* 1.0 novelty)
                     (* 0.6 telegraph)
                     (* 0.7 pressure))))

(defn choose-event
  "Pick the top candidate deterministically for now."
  [cands]
  (->> cands (sort-by (comp :score :ev/score) >) first))

;; -------------------------
;; Event family: Ultimatum → Raid → Feud
;; -------------------------

(defn gen-ultimatum-raid-feud [world]
  (let [veil? (has-mod? world #(= (:mod/type %) :veil))
        hostility (mod-strength world #(= (:mod/type %) :faction/hostility))
        plaus (-> 0.45
                  (+ (if veil? 0.15 0.0))
                  (+ (clamp01 (/ hostility 3.0))))
        nov (novelty world "ultimatum-raid-feud")
        score' (score world {:plausibility plaus :pressure 0.60 :telegraph 0.85 :novelty nov})]
    (when (> plaus 0.65)
      [(candidate
        {:id :ev/ultimatum-0001
         :family :ultimatum-raid-feud
         :keys [[:settlement :faction/player] [:faction :faction/north]]
         :steps [:telegraph :ultimatum :response :attack-or-settle :aftermath]
         :step :telegraph
         :telegraph [{:sign/type :scouts-seen
                      :sign/keys [[:settlement :faction/player]]
                      :sign/hint :pressure-building}]
         :params {:aggressor :faction/north
                  :target :faction/player
                  :motive (if veil? :kidnap :loot)}}
        score')] )))

;; -------------------------
;; Event family: Trial → Verdict → Enforcement
;; -------------------------

(defn gen-trial-verdict [world]
  (let [boost (mod-strength world #(and (= (:mod/type %) :event/boost)
                                       (= (get-in % [:mod/keys 0 1]) :trial-verdict-enforcement)))
        plaus (-> 0.40
                  (+ (clamp01 (/ boost 2.0))))
        nov (novelty world "trial-verdict-enforcement")
        score' (score world {:plausibility plaus :pressure 0.45 :telegraph 0.95 :novelty nov})]
    (when (> plaus 0.65)
      [(candidate
        {:id :ev/trial-0001
         :family :trial-verdict-enforcement
         :keys [[:settlement :faction/player] [:claim :claim/contested]]
         :steps [:telegraph :summons :testimony :verdict :enforcement :aftermath]
         :step :telegraph
         :telegraph [{:sign/type :witnesses-summoned
                      :sign/keys [[:claim :claim/contested]]
                      :sign/hint :trial-incoming}]
         :params {:court :inst/court-1
                  :issue :claim/contested}}
        score')] )))

;; -------------------------
;; Event family: Trade → Scarcity → Black Market
;; -------------------------

(defn gen-trade-scarcity [world]
  (let [boost (mod-strength world #(and (= (:mod/type %) :event/boost)
                                       (= (get-in % [:mod/keys 0 1]) :trade-scarcity-blackmarket)))
        plaus (-> 0.35
                  (+ (clamp01 (/ boost 2.0))))
        nov (novelty world "trade-scarcity-blackmarket")
        score' (score world {:plausibility plaus :pressure 0.40 :telegraph 0.90 :novelty nov})]
    (when (> plaus 0.60)
      [(candidate
        {:id :ev/trade-0001
         :family :trade-scarcity-blackmarket
         :keys [[:tile :loc/market] [:route :route/main]]
         :steps [:telegraph :boom :rationing :smuggling :charter-or-riot :aftermath]
         :step :telegraph
         :telegraph [{:sign/type :price-spike
                      :sign/keys [[:tile :loc/market]]
                      :sign/hint :scarcity-brewing}]
         :params {:good :grain}}
        score')] )))

;; -------------------------
;; Candidate aggregation
;; -------------------------

(defn gen-candidates [world]
  (->> (concat (or (gen-ultimatum-raid-feud world) [])
               (or (gen-trial-verdict world) [])
               (or (gen-trade-scarcity world) []))
       vec))

(defn enqueue-best! [world]
  (let [cands (gen-candidates world)
        chosen (choose-event cands)]
    (cond-> world
      chosen (update-in [:event :queue] (fnil conj []) chosen)
      true   (assoc-in [:event :candidates] cands))))

;; -------------------------
;; Executing a chain step (very MVP)
;; -------------------------

(defn advance-event
  "Given an event chain, move it to the next step.
   This should emit a world event to :event/log that myth can consume."
  [world ev]
  (let [steps (:ev/steps ev)
        idx (.indexOf steps (:ev/step ev))
        next-step (nth steps (inc idx) nil)]
    (if (nil? next-step)
      (-> world
          (update-in [:event :log] (fnil conj [])
                     {:world.event/type (:ev/family ev)
                      :world.event/keys (:ev/keys ev)
                      :world.event/tick (:tick world)
                      :world.event/summary (str "Resolved " (name (:ev/family ev)))})
          ev) ; returning ev is optional
      (assoc ev :ev/step next-step))))

(defn tick-event-queue
  "Advance 0..N active events (start with 1 per Aker boundary)."
  [world]
  (let [q (get-in world [:event :queue] [])
        [ev & rest] q]
    (if-not ev
      world
      (let [ev' (advance-event world ev)]
        (assoc-in world [:event :queue] (cons ev' rest))))))
```

* * *

Next roadmap slice (so you can aim implementation)
--------------------------------------------------

### Sprint A (Hex + walls)

*   Implement `hex.clj`

*   Convert `initial-world` + movement to axial

*   Add `:tiles` sparse map

*   Add wall ghost op + BFS “next step” pathing


### Sprint B (Colony core)

*   Items, job queue, build-wall job consumes wood

*   Tree harvesting into wood stacks

*   Stockpile hauling


### Sprint C (Champion loop)

*   Champion control on day, sleep toggles night

*   Night triggers: AI factions play a card, apply mods/signs, generate candidates, enqueue event


### Sprint D (6 neighbors + 6 decks)

*   Place neighbor settlements on the 6 ring directions

*   Load EDN decks, initialize each faction deck state

*   One card play per night boundary (0–1), with collision resolution


If you want, I’ll continue by drafting **collision resolution** (winner + complication) in Clojure pseudocode that works directly on the EDN card schema above, and a tiny loader (`edn/read-string` + init deck state).
