# Event DSL (macro-based, like `defgroup`) — *event archetypes + miracle hooks*

Below is a **Clojure macro DSL** to *describe* events in a way your sim can:

* match raw sim log entries against event archetypes
* extract **facet signatures**
* attach **claim candidates** (for attribution battles)
* compute **awe/rarity/impact** knobs
* optionally provide **power synthesis hints** when canonized

This is intentionally **data-first**: macros expand into EDN-ish maps, and a small interpreter evaluates match/field expressions at runtime.

---

## `src/fantasia/event_dsl.clj`

```clojure
(ns fantasia.event-dsl
  "Macro DSL for describing events (and event archetypes) that the sim can match.
  Expands into data maps and registers them in a registry."
  (:require [fantasia.dsl :as d]))

;; -----------------------------------------------------------------------------
;; Registries
;; -----------------------------------------------------------------------------

(defonce ^:private *event-types (atom {}))

(defn register-event-type! [et]
  (swap! *event-types assoc (:id et) et)
  et)

(defn event-types [] @*event-types)
(defn event-type [id] (get @*event-types id))

;; -----------------------------------------------------------------------------
;; Expression helpers (extend fantasia.dsl eval with :field)
;; -----------------------------------------------------------------------------

(defmacro field
  "Accessor into the raw sim event context, eg:
  (field :kind) -> [:field :kind]
  (field :target :rank) -> [:field [:target :rank]]"
  ([k] `[:field ~k])
  ([k1 k2] `[:field [~k1 ~k2]])
  ([k1 k2 & ks] `[:field [~k1 ~k2 ~@ks]]))

(defmacro has-facet?
  "Predicate used in match rules (raw sim event already tagged), eg:
  (has-facet? :lightning) -> [:has-facet? :lightning]"
  [facet]
  `[:has-facet? ~facet])

(defmacro near?
  "Spatial predicate placeholder:
  (near? (field :pos) (field :target :pos) 10.0)
  -> [:near? [:field :pos] [:field [:target :pos]] 10.0]"
  [a b r]
  `[:near? ~a ~b ~r])

(defn eval-expr
  "Evaluate DSL expr against a context map.
  Delegates most ops to fantasia.dsl/eval-expr, but extends with :field etc."
  [ctx expr]
  (if (vector? expr)
    (let [[op & xs] expr]
      (case op
        :field
        (let [k (first xs)]
          (if (vector? k)
            (get-in ctx (into [:event] k))
            (get-in ctx [:event k])))

        :has-facet?
        (contains? (set (get-in ctx [:event :facets] [])) (first xs))

        :near?
        ;; placeholder: you can replace with proper spatial math
        ;; expects [x y] tuples
        (let [p (eval-expr ctx (nth xs 0))
              q (eval-expr ctx (nth xs 1))
              r (double (nth xs 2))
              dx (- (double (nth p 0)) (double (nth q 0)))
              dy (- (double (nth p 1)) (double (nth q 1)))
              dist (Math/sqrt (+ (* dx dx) (* dy dy)))]
          (<= dist r))

        ;; fallback to shared evaluator for :and/:or/:stat/:metric/etc
        (d/eval-expr ctx expr)))
    (d/eval-expr ctx expr)))

;; -----------------------------------------------------------------------------
;; Event type builder + merge semantics
;; -----------------------------------------------------------------------------

(defn- deep-merge [a b]
  (merge-with
    (fn [x y]
      (cond
        (and (map? x) (map? y)) (deep-merge x y)
        (and (vector? x) (vector? y)) (into x y)
        :else y))
    a b))

(defn build-event-type [id name parts]
  (deep-merge
    {:id id
     :name name
     :kind :unspecified
     :match true
     ;; facet signature weights for recall aggregation
     :signature {}
     ;; optional: how to derive facets/signature from raw event ctx
     :derive {:facets nil :signature nil}
     ;; awe knobs (can be numbers or expr AST)
     :awe {:rarity 0.0 :impact 0.0 :need-match 0.0 :witness 0.0}
     ;; claim candidates to spawn into the myth arena
     :claims []
     ;; optional evidence spawns (objects, scars, shrines, debris)
     :evidence []
     ;; tradition thresholds for canonization tiers
     :tiers {:minor 10.0 :significant 50.0 :foundational 200.0}
     ;; synthesis hints for power generation when canonized
     :synthesis {:domain-facets [] :outcome-facets [] :affect-facets []}
     :meta {}}
    parts))

;; -----------------------------------------------------------------------------
;; Subforms (macros return fragments)
;; -----------------------------------------------------------------------------

(defmacro kind [k] `{:kind ~k})

(defmacro match
  "Match predicate (AST). Use fantasia.dsl ops + (field ...) + (has-facet?)"
  [pred]
  `{:match ~pred})

(defmacro signature
  "Static facet signature weights (used for event recall aggregation).
  (signature {:winter 0.6 :fire 1.0 :judgment 0.7})"
  [m]
  `{:signature ~m})

(defmacro derive
  "Optional derivation expressions for facets/signature from raw ctx.
  Keep these as AST (or plain data) so sim can run them.
  Example:
  (derive :signature {:fire (if (has-facet? :forest) 1.0 0.6) ...})
  This DSL keeps it flexible: store whatever you want, interpret later."
  [k v]
  `{:derive {~k ~v}})

(defmacro awe
  "Awe components; can be constants or AST expressions.
  (awe {:rarity 0.9 :impact (field :impact) :witness (field :witness-score)})"
  [m]
  `{:awe ~m})

(defmacro tiers
  "Canonization thresholds (tradition units) for the myth ledger."
  [m]
  `{:tiers ~m})

(defmacro claim
  "Define a claim candidate attached to this event type.
  Suggested fields:
  {:id :claim/fire-judgment
   :deity :patron/fire
   :canonical {:facets {...}} ; canonical facet bundle the clergy would repeat
   :hooks {:prayer-facet :lightning-prayer :icon-facet :flame-sigil}
   :rebuttals [...]}"
  [m]
  `{:claims [~m]})

(defmacro evidence
  "Evidence spawns / persistent traces.
  Each entry is just data for your world sim to interpret."
  [& entries]
  `{:evidence [~@entries]})

(defmacro synthesis
  "Hints for power synthesis upon canonization.
  (synthesis {:domain-facets [:fire :storm]
              :outcome-facets [:enemy-rout :no-casualties]
              :affect-facets [:awe :judgment]})"
  [m]
  `{:synthesis ~m})

(defmacro meta [m] `{:meta ~m})

;; -----------------------------------------------------------------------------
;; Top-level macro: defeventtype
;; -----------------------------------------------------------------------------

(defmacro defeventtype
  "Define an event archetype + register it.
  This is NOT an event instance; it’s a matcher/extractor for sim events."
  [sym name & forms]
  (let [id (keyword sym)]
    `(let [parts# (apply deep-merge ~@forms)
           et# (build-event-type ~id ~name parts#)]
       (def ~sym et#)
       (register-event-type! et#))))

;; -----------------------------------------------------------------------------
;; Runtime helpers: matching + instantiation
;; -----------------------------------------------------------------------------

(defn matches?
  "Does this event type match a raw sim event ctx?"
  [event-type ctx]
  (true? (eval-expr ctx (:match event-type))))

(defn awe-score
  "Compute awe score W = rarity * impact * need-match * witness.
  Components may be numbers or AST expressions."
  [event-type ctx]
  (let [{:keys [rarity impact need-match witness]} (:awe event-type)
        v (fn [x]
            (cond
              (number? x) (double x)
              (vector? x) (double (eval-expr ctx x))
              :else 0.0))
        r (max 0.0 (v rarity))
        i (max 0.0 (v impact))
        n (max 0.0 (v need-match))
        w (max 0.0 (v witness))]
    (* r i n w)))

(defn instantiate
  "Create a candidate miracle record from a matched raw sim event.
  This does not canonize anything; it just emits a normalized record your myth ledger can track."
  [event-type ctx]
  (let [ev (get ctx :event)
        base {:event-type-id (:id event-type)
              :name (:name event-type)
              :kind (:kind event-type)
              :time (:time ev)
              :place (:place ev)
              :actors (:actors ev)
              :raw-id (:id ev)
              :facets (or (:facets ev) (keys (:signature event-type)))
              :signature (or (:signature ev) (:signature event-type))
              :awe (awe-score event-type ctx)
              :claims (:claims event-type)
              :evidence (:evidence event-type)
              :tiers (:tiers event-type)
              :synthesis (:synthesis event-type)
              :meta (:meta event-type)}]
    base))
```

---

## `src/fantasia/example/events.clj`

A few event archetypes matching the miracles you’ve been using as anchors.

```clojure
(ns fantasia.example.events
  (:require [fantasia.event-dsl :as e]
            [fantasia.dsl :as d]))

;; -----------------------------------------------------------------------------
;; Lightning kills enemy commander at decisive moment
;; -----------------------------------------------------------------------------

(e/defeventtype lightning-commander "Lightning Strikes the Commander"
  (e/kind :battle)

  ;; Match raw sim events that already have basic tagging
  (e/match
    (d/and
      (d/= (e/field :kind) :lightning-strike)
      (d/= (e/field :target :side) :enemy)
      (d/in (e/field :target :rank) #{:commander :captain :warlord})
      (d/>= (e/field :battle :stakes) 0.7)))

  ;; Signature used by recall aggregation (facets -> event activation)
  (e/signature
    {:storm 0.7
     :lightning 1.0
     :battle 0.6
     :enemy-leader 1.0
     :judgment 0.8
     :awe 0.9})

  ;; Awe components can mix constants and event fields
  (e/awe
    {:rarity 0.9
     :impact (e/field :impact)          ;; assume sim computes a 0..1 swing
     :need-match (e/field :allies :fear) ;; 0..1 fear/uncertainty
     :witness (e/field :witness-score)}) ;; 0..1 weighted witness presence

  (e/claim
    {:id :claim/fire-judgment
     :deity :patron/fire
     :canonical {:facets {:lightning 1.0 :judgment 0.8 :storm 0.6 :oath 0.5}}
     :hooks {:prayer-facet :prayer/smite-with-lightning
             :icon-facet :icon/flame-sigil}
     :rebuttals
     [{:id :rebuttal/natural-weather
       :canonical {:facets {:storm 1.0 :chance 0.6}}}]})

  (e/evidence
    {:type :corpse :tags #{:charred :leader} :persist-days 10}
    {:type :scar :tags #{:burnt-earth} :persist-days 30})

  (e/synthesis
    {:domain-facets [:storm :lightning]
     :outcome-facets [:enemy-leader-dead :victory-swing]
     :affect-facets [:awe :judgment]})

  (e/tiers {:minor 8.0 :significant 45.0 :foundational 180.0}))


;; -----------------------------------------------------------------------------
;; River flood during drought saves harvest
;; -----------------------------------------------------------------------------

(e/defeventtype mercy-flood "The Mercy Flood"
  (e/kind :nature)

  (e/match
    (d/and
      (d/= (e/field :kind) :flood)
      (d/>= (e/field :weather :drought-days) 10)
      (d/>= (e/field :impact) 0.6)))

  (e/signature
    {:drought 0.8
     :river 1.0
     :flood 0.9
     :harvest 1.0
     :mercy 0.8
     :relief 0.6})

  (e/awe
    {:rarity 0.7
     :impact (e/field :impact)
     :need-match (e/field :colony :hunger) ;; 0..1
     :witness (e/field :witness-score)})

  ;; This one is attribution-contested by design
  (e/claim
    {:id :claim/fire-mercy
     :deity :patron/fire
     :canonical {:facets {:mercy 1.0 :river 0.7 :relief 0.8}}
     :hooks {:icon-facet :icon/flame-sigil}
     :rebuttals
     [{:id :rebuttal/river-god
       :canonical {:facets {:river 1.0 :flood 0.9}}
       :rival-deity :deity/river}]})

  (e/evidence
    {:type :terrain-change :tags #{:silted-fields :fresh-water} :persist-days 60}
    {:type :shrine-site :tags #{:flood-mark} : reminding-facet :river})

  (e/synthesis
    {:domain-facets [:river :water]           ;; even if patron is fire, myth drift can expand domains
     :outcome-facets [:harvest-saved :drought-broken]
     :affect-facets [:mercy :relief]}))


;; -----------------------------------------------------------------------------
;; Winter forest fire victory (your cold+trees -> fire -> god -> event chain)
;; -----------------------------------------------------------------------------

(e/defeventtype winter-pyre "The Winter Pyre"
  (e/kind :battle)

  (e/match
    (d/and
      (d/= (e/field :kind) :forest-fire)
      (d/>= (e/field :weather :cold) 0.7)
      (d/>= (e/field :impact) 0.7)
      (d/= (e/field :target :side) :enemy)))

  (e/signature
    {:winter 0.8
     :cold 0.6
     :trees 0.7
     :fire 1.0
     :smoke 0.5
     :enemy-rout 0.9
     :no-casualties 0.8
     :awe 0.8
     :judgment 0.7})

  (e/awe
    {:rarity 0.85
     :impact (e/field :impact)
     :need-match (e/field :allies :fear)
     :witness (e/field :witness-score)})

  (e/claim
    {:id :claim/winter-judgment-flame
     :deity :patron/fire
     :canonical {:facets {:fire 1.0 :winter 0.8 :judgment 0.7 :awe 0.6}}
     :hooks {:icon-facet :icon/flame-sigil}
     :rebuttals
     [{:id :rebuttal/arson
       :canonical {:facets {:fire 1.0 :human-agency 0.7}}
       :notes "Some will claim it was intentional sabotage."}]})

  (e/evidence
    {:type :burn-scar :tags #{:charred-grove} :persist-days 45}
    {:type :relic :tags #{:ember-stone} :spawn-chance 0.2})

  (e/synthesis
    {:domain-facets [:fire]
     :outcome-facets [:enemy-rout :battlefield-denial :no-casualties]
     :affect-facets [:awe :judgment]}))


;; -----------------------------------------------------------------------------
;; Clergy medicine interpreted as miracle (institution-driven)
;; -----------------------------------------------------------------------------

(e/defeventtype healing-rush "Hands That Healed"
  (e/kind :service)

  (e/match
    (d/and
      (d/= (e/field :kind) :mass-treatment)
      (d/>= (e/field :impact) 0.6)
      (d/>= (e/field :service :patients-saved) 5)))

  (e/signature
    {:medicine 1.0
     :mercy 0.8
     :cleanliness 0.6
     :survival 0.9
     :wonder 0.5})

  (e/awe
    {:rarity 0.4                        ;; lower “acts of nature” awe, higher social interpretation
     :impact (e/field :impact)
     :need-match (e/field :colony :injury-crisis) ;; 0..1
     :witness (e/field :witness-score)})

  (e/claim
    {:id :claim/patron-guided-hands
     :deity :patron/fire
     :canonical {:facets {:medicine 1.0 :mercy 0.8 :wonder 0.6}}
     :hooks {:institution :healers-guild}
     :rebuttals
     [{:id :rebuttal/its-just-skill
       :canonical {:facets {:medicine 1.0 :human-agency 0.8}}}]})

  (e/evidence
    {:type :text :tags #{:case-record} :copyable true :persist-days 120}
    {:type :place-tag :tags #{:clinic} :reminding-facet :medicine})

  (e/synthesis
    {:domain-facets [:mercy :medicine]
     :outcome-facets [:healing :survival]
     :affect-facets [:wonder :gratitude]}))
```

---

## How this fits your existing mention/facet system

* The sim emits raw events like:

  * `{:id ... :kind :forest-fire :impact 0.8 :weather {:cold 0.9} ...}`
* You run each `event-type`’s `matches?`
* For matches, `instantiate` produces a **candidate miracle record** with:

  * facet signature weights (for recall aggregation)
  * claim candidates (for attribution battles)
  * evidence spawns (for physical triggers)
  * synthesis hints (for “miracle → power” shaping)

This keeps events “physics-first” and lets religion be a **second-order social phenomenon**.

---

## Next continuation (if you want it)

I can add a sibling DSL for **event *instances*** (scenario scripting / authored world seeds) that uses the same vocabulary, e.g.:

* `(defevent winter-pyre-0007 ...)` to drop a historical miracle into world gen
* and a `compile->facet-signature` helper so any raw sim event can be normalized even without a matched archetype

#game-design #fantasia #clojure #dsl #events #miracles #macro #metaprogramming #myth-engine #narrative #storytelling
