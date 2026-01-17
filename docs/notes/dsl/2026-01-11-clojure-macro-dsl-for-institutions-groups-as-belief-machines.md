# Clojure macro DSL for institutions (groups-as-belief-machines)

This DSL is designed to be **macro-authored** (nice syntax) but compiles into **pure data (EDN-ish AST)** that your sim can interpret: membership gates, vows, rituals, broadcasts, services, relationships, and *sacralization drift*.

You’ll get:

* groups with **facet signatures**
* rules like “requires faith + skill + vows”
* organizations broadcasting **canonical facet bundles**
* secular orgs becoming religious via **sacrality rules**
* typed relationships + roles + ranks

---

## `src/fantasia/dsl.clj`

```clojure
(ns fantasia.dsl
  "Macro DSL for defining groups/institutions as first-class social entities.
  Expands into data maps + registers them in an in-memory registry.
  Predicate expressions compile into an EDN AST (not executed at macroexpansion time)."
  (:refer-clojure :exclude [and or not = < <= > >=]))

;; -----------------------------------------------------------------------------
;; Registry (you'll probably swap this for your DB / ECS later)
;; -----------------------------------------------------------------------------

(defonce ^:private *groups (atom {}))

(defn register-group! [g]
  (swap! *groups assoc (:id g) g)
  g)

(defn groups [] @*groups)
(defn group [id] (get @*groups id))

;; -----------------------------------------------------------------------------
;; Predicate / expression AST helpers
;; -----------------------------------------------------------------------------
;; The goal: keep authored syntax pleasant, but store an AST like:
;; [:and [:>= [:stat :faith :patron/fire] 0.6] [:not [:trait :oathbreaker]]]

(defmacro and [& xs] `[:and ~@xs])
(defmacro or  [& xs] `[:or  ~@xs])
(defmacro not [x]    `[:not ~x])

(defmacro =  [a b] `[:=  ~a ~b])
(defmacro <  [a b] `[:<  ~a ~b])
(defmacro <= [a b] `[:<= ~a ~b])
(defmacro >  [a b] `[:>  ~a ~b])
(defmacro >= [a b] `[:>= ~a ~b])

(defmacro in [x coll] `[:in ~x ~coll])

(defmacro stat
  "General stat accessor.
  (stat :faith :patron/fire) -> [:stat :faith :patron/fire]"
  [k subk]
  `[:stat ~k ~subk])

(defmacro faith [deity-or-claim] `[:stat :faith ~deity-or-claim])
(defmacro morale [] `[:stat :morale nil])
(defmacro alignment [faction-id] `[:stat :alignment ~faction-id])
(defmacro skill [k] `[:stat :skill ~k])

(defmacro trait
  "(trait :oathbreaker) -> [:trait :oathbreaker]"
  [k]
  `[:trait ~k])

(defmacro tag
  "(tag :priest) -> [:tag :priest]"
  [k]
  `[:tag ~k])

(defmacro metric
  "World/group metrics, eg:
  (metric :ritual-count :ember-knights :vigil {:days 14})
  -> [:metric :ritual-count :ember-knights :vigil {:days 14}]"
  ([k a b] `[:metric ~k ~a ~b {}])
  ([k a b opts] `[:metric ~k ~a ~b ~opts]))

(defmacro recently?
  "(recently? :event/miracle-attributed {:event-id ... :days 7})
  -> [:recently? :event/miracle-attributed {:event-id ... :days 7}]"
  [kind opts]
  `[:recently? ~kind ~opts])

;; -----------------------------------------------------------------------------
;; Runtime evaluator (simple interpreter for the AST)
;; You can replace this with compiled fns later for speed.
;; -----------------------------------------------------------------------------

(defn- ctx-stat [ctx k subk]
  (case k
    :faith     (get-in ctx [:agent :faith subk] 0.0)
    :alignment (get-in ctx [:agent :alignment subk] 0.0)
    :skill     (get-in ctx [:agent :skills subk] 0.0)
    :morale    (get-in ctx [:agent :morale] 0.0)
    ;; default
    (get-in ctx [:agent :stats k subk])))

(defn- ctx-trait? [ctx t] (boolean (get-in ctx [:agent :traits t])))
(defn- ctx-tag?   [ctx t] (boolean (get-in ctx [:agent :tags t])))

(defn- ctx-metric [ctx k a b opts]
  ;; You decide where these live. Keeping it flexible:
  ;; ctx could include [:metrics k a b] or a function.
  (if-let [f (:metric-fn ctx)]
    (f k a b opts)
    (get-in ctx [:metrics k a b] 0.0)))

(defn eval-expr
  "Evaluate an expression AST against a context map.
  ctx shape example:
  {:agent {:faith {...} :skills {...} :traits {...} :morale 0.3}
   :metrics {...}
   :metric-fn (fn [k a b opts] ...)}"
  [ctx expr]
  (cond
    (nil? expr) true
    (boolean? expr) expr
    (number? expr) expr
    (keyword? expr) expr
    (string? expr) expr
    (map? expr) expr
    (vector? expr)
    (let [[op & xs] expr]
      (case op
        :and (every? true? (map #(eval-expr ctx %) xs))
        :or  (some true? (map #(eval-expr ctx %) xs))
        :not (not (true? (eval-expr ctx (first xs))))

        :=  (=  (eval-expr ctx (nth xs 0)) (eval-expr ctx (nth xs 1)))
        :<  (<  (double (eval-expr ctx (nth xs 0))) (double (eval-expr ctx (nth xs 1))))
        :<= (<= (double (eval-expr ctx (nth xs 0))) (double (eval-expr ctx (nth xs 1))))
        :>  (>  (double (eval-expr ctx (nth xs 0))) (double (eval-expr ctx (nth xs 1))))
        :>= (>= (double (eval-expr ctx (nth xs 0))) (double (eval-expr ctx (nth xs 1))))

        :in (contains? (set (eval-expr ctx (nth xs 1))) (eval-expr ctx (nth xs 0)))

        :stat (ctx-stat ctx (nth xs 0) (nth xs 1))
        :trait (ctx-trait? ctx (nth xs 0))
        :tag   (ctx-tag? ctx (nth xs 0))

        :metric (ctx-metric ctx (nth xs 0) (nth xs 1) (nth xs 2) (nth xs 3))

        :recently?
        ;; placeholder: you can implement via ctx event log
        (boolean (get-in ctx [:recent (nth xs 0) (nth xs 1)] false))

        ;; unknown op => treat as false (or throw)
        false))
    :else
    expr))

;; -----------------------------------------------------------------------------
;; Group DSL builders (forms compile to a map)
;; -----------------------------------------------------------------------------

(defn- merge-facets
  "Facets are a weighted map: {:fire 1.0 :discipline 0.7 ...}"
  [a b]
  (merge-with + a b))

(defn build-group [id name {:keys [kind facets charter roles relations broadcasts rituals services sacrality meta] :as m}]
  {:id id
   :name name
   :kind kind
   :facets (or facets {})
   :charter (or charter {})
   :roles (or roles {})
   :relations (or relations [])
   :broadcasts (or broadcasts [])
   :rituals (or rituals [])
   :services (or services [])
   :sacrality (or sacrality {})
   :meta (or meta {})})

;; -----------------------------------------------------------------------------
;; Subform macros (each returns a tagged map fragment)
;; -----------------------------------------------------------------------------

(defmacro kind [k] `{:kind ~k})

(defmacro facets
  "Define weighted facets: (facets {:defense 1.0 :honor 0.7})"
  [m] `{:facets ~m})

(defmacro role
  "(role :knight {:rank 1 :facets {...} :can-broadcast? true})"
  [id m]
  `{:roles {~id ~m}})

(defmacro relation
  "(relation :ally :city-council {:strength 0.6})"
  [rel other-id opts]
  `{:relations [{:type ~rel :other ~other-id :opts ~opts}]})

(defmacro requires
  "Membership requirements: store as AST.
  (requires (and (>= (faith :patron/fire) 0.6) (>= (skill :melee) 2)))"
  [& preds]
  `{:requires (and ~@preds)})

(defmacro forbids
  "Constraints / taboos expressed as predicates that must be false."
  [& preds]
  `{:forbids (or ~@preds)})

(defmacro vow
  "(vow :no-theft {:breaks-when (trait :thief) :penalty {...}})"
  [id m]
  `{:vows {~id ~m}})

(defmacro promotion
  "Promotion rule per role/rank."
  [role-id m]
  `{:promotion {~role-id ~m}})

(defmacro discipline
  "Discipline / punishment policies."
  [m] `{:discipline ~m})

(defmacro charter
  "Wrap charter parts.
  (charter
    (requires ...)
    (forbids ...)
    (vow ...)
    (promotion ...)
    (discipline ...))"
  [& parts]
  `(let [merged# (apply merge-with
                        (fn [a# b#]
                          (cond
                            (and (map? a#) (map? b#)) (merge a# b#)
                            (and (vector? a#) (vector? b#)) (into a# b#)
                            :else b#))
                        ~@parts)]
     {:charter merged#}))

(defmacro ritual
  "(ritual :vigil {:trigger {:time :night} :bundle {...} :effects {...}})"
  [id m]
  `{:rituals [{:id ~id :spec ~m}]})

(defmacro broadcast
  "(broadcast :sermon {...})"
  [id m]
  `{:broadcasts [{:id ~id :spec ~m}]})

(defmacro service
  "(service :medicine {...})"
  [id m]
  `{:services [{:id ~id :spec ~m}]})

(defmacro sacrality
  "Define sacrality ties + growth/decay rules per deity/claim.
  Example:
  (sacrality
    (tie :patron/fire {:initial 0.2 :cap 0.95})
    (grows :patron/fire (and (>= (metric :ritual-count :ember-knights :vigil {:days 14}) 6)
                             (>= (metric :service-success :ember-knights :defense {:days 14}) 0.7))
           {:rate 0.02})
    (decays :patron/fire (>= (metric :scandal-count :ember-knights :any {:days 30}) 1)
            {:rate 0.05}))"
  [& parts]
  `(let [merged# (apply merge-with
                        (fn [a# b#]
                          (cond
                            (and (map? a#) (map? b#)) (merge a# b#)
                            (and (vector? a#) (vector? b#)) (into a# b#)
                            :else b#))
                        ~@parts)]
     {:sacrality merged#}))

(defmacro tie [deity-or-claim m]
  `{:ties {~deity-or-claim ~m}})

(defmacro grows [deity-or-claim pred opts]
  `{:grows [{:target ~deity-or-claim :when ~pred :opts ~opts}]})

(defmacro decays [deity-or-claim pred opts]
  `{:decays [{:target ~deity-or-claim :when ~pred :opts ~opts}]})

(defmacro meta [m] `{:meta ~m})

;; -----------------------------------------------------------------------------
;; Top-level macro: defgroup
;; -----------------------------------------------------------------------------

(defmacro defgroup
  "Define and register a group.
  Usage:
  (defgroup ember-knights \"Ember Knights\"
    (kind :order)
    (facets {...})
    (charter ...)
    (role ...)
    (ritual ...)
    (broadcast ...)
    (service ...)
    (sacrality ...))"
  [sym name & forms]
  (let [id (keyword sym)]
    `(let [parts# (apply merge-with
                         (fn [a# b#]
                           (cond
                             (and (map? a#) (map? b#)) (merge a# b#)
                             (and (vector? a#) (vector? b#)) (into a# b#)
                             :else b#))
                         ~@forms)
           g# (build-group ~id ~name parts#)]
       (def ~sym g#)
       (register-group! g#))))
```

---

## `src/fantasia/example/groups.clj`

This file shows:

* a **religious knight order**
* a **government council** that can become religious
* a **clinic/medic guild** that can become sacralized by outcomes + member framing

```clojure
(ns fantasia.example.groups
  (:require [fantasia.dsl :as dsl]))

;; ---------------------------------------------------------------------------
;; Ember Knights — explicitly religious military order
;; ---------------------------------------------------------------------------

(dsl/defgroup ember-knights "Order of the Ember Knights"
  (dsl/kind :order)

  (dsl/facets
    {:defense 1.0
     :law 0.6
     :discipline 0.9
     :honor 0.8
     :fire 1.0
     :judgment 0.7
     :ritual 0.6})

  (dsl/charter
    (dsl/requires
      (dsl/>= (dsl/faith :patron/fire) 0.65)
      (dsl/>= (dsl/skill :melee) 2)
      (dsl/not (dsl/trait :oathbreaker)))
    (dsl/forbids
      (dsl/trait :thief)
      (dsl/trait :coward))
    (dsl/vow :no-theft
      {:breaks-when (dsl/trait :thief)
       :penalty {:morale -0.2
                 :trust-institution -0.3
                 :status :probation}})
    (dsl/vow :stand-vigil
      {:breaks-when (dsl/trait :deserter)
       :penalty {:status :exiled}})
    (dsl/promotion :knight
      {:when (dsl/and
               (dsl/>= (dsl/skill :melee) 3)
               (dsl/>= (dsl/faith :patron/fire) 0.75)
               (dsl/>= (dsl/metric :service-success :ember-knights :defense {:days 30}) 0.7))
       :to-rank 2})
    (dsl/discipline
      {:punishments
       [{:id :penance
         :when (dsl/or (dsl/trait :thief) (dsl/trait :brawler))
         :effects {:faith :patron/fire -0.05
                   :status :probation}}]}))

  (dsl/role :knight
    {:rank 1
     :facets {:defense 0.8 :law 0.5 :fire 0.4}
     :can-broadcast? true
     :can-draft? true})

  (dsl/ritual :vigil
    {:trigger {:time :night
               :min-members 3}
     :bundle {:intent :convert
              :tone {:awe 0.7 :urgency 0.3}
              :facets {:fire 1.0 :judgment 0.6 :honor 0.5}
              :claim-hint :claim/fire-judgment}
     :effects {:faith :patron/fire +0.02
               :discipline +0.01}})

  (dsl/broadcast :sermon-of-embers
    {:channel {:type :oral :radius 18 :noise-sensitivity 0.7}
     :bundle {:intent :convert
              :tone {:awe 0.6 :valence 0.4}
              :facets {:fire 1.0 :judgment 0.8 :oath 0.6}
              :claim-hint :claim/fire-judgment}
     :audience {:prefers [:soldiers :citizens]}
     :entropy 0.2}) ;; low entropy => consistent canonical bundle

  (dsl/service :border-defense
    {:outputs {:safety +1}
     :reputation {:competence +0.02 :legitimacy +0.01}
     :notes "Routine patrols, escort, response to raids."})

  (dsl/sacrality
    (dsl/tie :patron/fire {:initial 0.6 :cap 0.98})
    (dsl/grows :patron/fire
      (dsl/and
        (dsl/>= (dsl/metric :ritual-count :ember-knights :vigil {:days 14}) 6)
        (dsl/>= (dsl/metric :service-success :ember-knights :border-defense {:days 14}) 0.6))
      {:rate 0.02})
    (dsl/decays :patron/fire
      (dsl/>= (dsl/metric :scandal-count :ember-knights :any {:days 30}) 1)
      {:rate 0.06})))


;; ---------------------------------------------------------------------------
;; City Council — secular government that can become religious (sacralize)
;; ---------------------------------------------------------------------------

(dsl/defgroup city-council "City Council of Ashgate"
  (dsl/kind :government)

  (dsl/facets
    {:law 1.0
     :tax 0.7
     :infrastructure 0.8
     :order 0.6
     :public-good 0.5})

  (dsl/charter
    (dsl/requires
      (dsl/>= (dsl/alignment :faction/ashgate) 0.6))
    (dsl/forbids
      (dsl/trait :outlaw)))

  (dsl/role :councilor
    {:rank 1
     :facets {:law 0.7 :order 0.4}
     :can-broadcast? true})

  (dsl/broadcast :decree
    {:channel {:type :posted :radius :citywide :copyable true}
     :bundle {:intent :coordinate
              :tone {:urgency 0.4}
              :facets {:law 1.0 :order 0.6}}
     :entropy 0.1})

  ;; Here’s the “members make it religious” hook:
  ;; If enough councilors are faithful + they repeatedly invoke the patron in decrees
  ;; and the city experiences successful outcomes, the council sacralizes.
  (dsl/sacrality
    (dsl/tie :patron/fire {:initial 0.05 :cap 0.65})
    (dsl/grows :patron/fire
      (dsl/and
        (dsl/>= (dsl/metric :broadcast-count :city-council :decree {:days 14}) 5)
        (dsl/>= (dsl/metric :invocation-rate :city-council :patron/fire {:days 14}) 0.4)
        (dsl/>= (dsl/metric :public-safety :ashgate :city {:days 14}) 0.6))
      {:rate 0.015})
    (dsl/decays :patron/fire
      (dsl/>= (dsl/metric :public-backlash :ashgate :religion-in-government {:days 30}) 0.5)
      {:rate 0.03})))


;; ---------------------------------------------------------------------------
;; Healers' Guild — not founded religious, becomes sacralized by outcomes + framing
;; ---------------------------------------------------------------------------

(dsl/defgroup healers-guild "Healers' Guild"
  (dsl/kind :guild)

  (dsl/facets
    {:medicine 1.0
     :education 0.6
     :mercy 0.7
     :hygiene 0.8})

  (dsl/charter
    (dsl/requires
      (dsl/>= (dsl/skill :medicine) 2))
    (dsl/forbids
      (dsl/trait :butcher))) ;; sloppy/violent trait, example

  (dsl/role :physicker
    {:rank 1
     :facets {:medicine 1.0 :mercy 0.5}
     :can-broadcast? false})

  (dsl/service :clinic
    {:outputs {:health +1}
     :reputation {:competence +0.03}
     :notes "Care, triage, sanitation education."})

  ;; “Medicine credited to god” route:
  ;; If outcomes are impressive AND members repeatedly frame them religiously,
  ;; sacrality climbs (even if guild wasn't founded religious).
  (dsl/sacrality
    (dsl/tie :patron/fire {:initial 0.02 :cap 0.55})
    (dsl/grows :patron/fire
      (dsl/and
        (dsl/>= (dsl/metric :service-success :healers-guild :clinic {:days 21}) 0.8)
        (dsl/>= (dsl/metric :attribution-rate :healers-guild :patron/fire {:days 21}) 0.35))
      {:rate 0.02})
    (dsl/decays :patron/fire
      (dsl/>= (dsl/metric :scandal-count :healers-guild :any {:days 60}) 1)
      {:rate 0.04})))
```

---

## How this plugs into your “mentions” system

### Where “invocation-rate” / “attribution-rate” come from

These metrics are fed by your mention ledger:

* when a listener **recalls event facets + deity facets** in the same packet chain → that’s an attribution reinforcement
* institutions amplify these rates via consistent broadcasts (low entropy)

So a secular org can sacralize because:

* members keep injecting deity-linked facet bundles into org comms,
* those bundles get associated and repeated,
* and outcomes (services) provide credible “miracle-adjacent” anchors.

---

## Next continuation (if you want it)

I can extend the DSL with one more layer that’ll matter soon:

* `defsect` / `defcult` helpers for schisms and splinters
* `verification-council` blocks (how a religion decides miracles are canon)
* a tiny “Charter DSL” for **membership gates** like “must be of the faith *and* pass a secular competency test *and* hold citizenship”
* and a `compile-group` step that turns predicates into fast fns (while keeping authored macros)

#game-design #fantasia #clojure #dsl #institutions
