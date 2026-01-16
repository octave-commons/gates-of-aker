Minimal schemas (EDN maps)
--------------------------

```clojure
;; --- World state (core) ---
{:world/seed        1337
 :world/tick        42
 :world/phase       :day            ;; :day | :night
 :champion/asleep?  false           ;; gate for Night moves

;; budgets per deity
 :deity/budget
 {:herald {:favor 8 :cred 6 :attention 3}
  :sun    {:favor 7 :cred 7 :attention 4}
  :moon   {:favor 9 :cred 5 :attention 2}}

;; influence is “who can push where”
 ;; keep it sparse: only store known non-zero edges
 :influence
 {[:herald :route/royal-road] 0.8
  [:sun    :inst/court-1]     0.7
  [:moon   :loc/market]       0.6}

;; belief layer: claims per group (confidence + alignment)
 :belief
 {:group/townfolk
  {:claim/champion-tyrant {:confidence 2 :alignment :moon}
   :claim/court-legit     {:confidence 4 :alignment :sun}}

 :group/merchants
  {:claim/roads-unsafe    {:confidence 3 :alignment :moon}}}

;; edges/nodes can be as simple as you want at first
 :routes {:route/royal-road {:latency 3 :security 2 :visibility 3}}
 :inst   {:inst/court-1     {:legitimacy 4 :stores 2}}
 :loc    {:loc/market       {:security 2 :heat 1}}

;; recency tags to kill “samey”
 :recency {"raid:sabotage" 2
           "trial"         0
           "relief"        5}

;; pending signs/events (player-facing + simulation queue)
 :signs  []
 :queue  []}
```

### Move / Sign / EventCandidate

```clojure
;; Move submitted by a deity or champion
{:move/id        :move-001
 :move/actor     :herald            ;; :sun | :moon | :herald | etc.
 :move/phase     :night             ;; :night | :day | :always
 :move/type      :dream-courier      ;; ability id
 :move/keys      {:primary [:person/alex] :secondary [:route/royal-road]}
 :move/tags      #{"info" "dream" "herald:night"}
 :move/cost      {:favor 2 :cred 0 :attention 1}
 :move/params    {:theme :suspicion :target :person/alex}}

;; Telegraph sign emitted into the world
{:sign/id        :sign-901
 :sign/type      :omen              ;; :omen :gossip :patrol-prep :missing-animals :pamphlet :price-spike ...
 :sign/origin    :loc/market
 :sign/keys      [:loc/market]
 :sign/visibility {:group/townfolk 3 :group/merchants 2} ;; optional
 :sign/meaning   {:hint :something-stirs-at-night}}      ;; optional

;; Candidate event chain (world event, not “god magic”)
{:ev/id          :ev-777
 :ev/family      :ultimatum-raid-feud
 :ev/keys        [:loc/market :route/royal-road]
 :ev/telegraph   [{:sign/type :scouts-seen :sign/origin :route/royal-road}]
 :ev/steps       [:ultimatum :response-window :raid-or-settlement :aftermath]
 :ev/score       {:plausibility 0.82 :pressure 0.55 :novelty 0.70 :telegraph 0.90}
 :ev/params      {:aggressor :group/bandits :motive :loot :target :loc/market}}
```

* * *

Resolution: collisions + deterministic “noise” (Clojure-ish)
------------------------------------------------------------

```clojure
(defn clamp01 [x] (-> x (max 0.0) (min 1.0)))

(defn seeded-noise
  "Deterministic-ish noise in [-0.25, +0.25] from seed+tick+key.
   Pseudocode: replace hash with something stable in your actual impl."
  [seed tick conflict-key]
  (let [h (hash [seed tick conflict-key])
        u (-> (mod (Math/abs (long h)) 10000) (/ 9999.0))] ; 0..1
    (- (* 0.5 u) 0.25)))                                  ; -0.25..+0.25

(defn influence [world actor target]
  (get-in world [:influence [actor target]] 0.0))

(defn recency-penalty [world tags]
  ;; super simple: if any tag has recency>0, penalize
  (let [r (reduce max 0 (for [t tags] (get-in world [:recency t] 0)))]
    (clamp01 (/ r 5.0))))

(defn resistance
  "Optional: derive resistance from Sun seals, Moon veils, etc."
  [world move]
  0.0)

(defn move-strength [world move]
  (let [{:keys [move/actor move/keys move/cost move/tags]} move
        target (first (:primary move/keys))
        I (influence world move/actor target)                ; 0..1
        F (clamp01 (/ (get move/cost :favor 0) 6.0))
        A (clamp01 (/ (get move/cost :attention 0) 6.0))
        C (clamp01 (/ (get-in world [:deity/budget actor :cred] 0) 10.0))
        R (recency-penalty world move/tags)
        D (resistance world move)]
    (+ (* 2.0 I) (* 1.5 F) (- (* 1.0 A)) (* 1.0 C) (- (* 1.2 R)) (- (* 1.5 D)))))

(defn resolve-conflict [world conflict-key moves]
  (let [seed (:world/seed world)
        tick (:world/tick world)
        scored (for [m moves
                     :let [base (move-strength world m)
                           eps  (seeded-noise seed tick conflict-key)]]
                 {:move m :score (+ base eps)})]
    (let [[win runner & _] (sort-by :score > scored)]
      {:winner       (:move win)
       :complication (:move runner)
       :margin       (if runner (- (:score win) (:score runner)) 999)})))

(defn conflict-keys [move]
  ;; collisions happen on primary keys
  (set (get-in move [:move/keys :primary])))

(defn group-by-conflicts [moves]
  ;; map conflict-key -> [moves...]
  (reduce (fn [m mv]
            (reduce (fn [m2 k] (update m2 k (fnil conj []) mv))
                    m
                    (conflict-keys mv)))
          {}
          moves))
```

* * *

Phase gating (Night requires champion asleep)
---------------------------------------------

```clojure
(defn move-allowed? [world move]
  (let [phase (:world/phase world)
        asleep? (:champion/asleep? world)
        mphase (:move/phase move)]
    (cond
      (= mphase :always) true
      (= mphase :day)    (= phase :day)
      (= mphase :night)  (and (= phase :night) asleep?)
      :else false)))
```

* * *

Applying a move emits _signs_ + _state modifiers_ (no “spawn raid”)
-------------------------------------------------------------------

Here’s a tiny example for three move types (enough to demonstrate shape):

```clojure
(defn spend-budget [world actor {:keys [favor cred attention]}]
  (-> world
      (update-in [:deity/budget actor :favor] - (or favor 0))
      (update-in [:deity/budget actor :cred] - (or cred 0))
      (update-in [:deity/budget actor :attention] + (or attention 0))))

(defn emit-sign [world sign]
  (update world :signs conj sign))

(defn apply-move [world move]
  (let [{:keys [move/actor move/type move/cost move/params]} move
        world (spend-budget world move/actor move/cost)]
    (case move/type
      :whisper-network
      (-> world
          (update-in [:routes (:route move/params) :latency] dec)
          (emit-sign {:sign/id (keyword (str "sign-" (hash move)))
                      :sign/type :gossip
                      :sign/origin :route/royal-road
                      :sign/keys [:route/royal-road]
                      :sign/meaning {:hint :messages-move-faster}}))

     :dawn-decree
      (-> world
          (assoc-in [:world/modifiers :tomorrow-law] (:law move/params))
          (emit-sign {:sign/id (keyword (str "sign-" (hash move)))
                      :sign/type :patrol-prep
                      :sign/origin (:inst move/params)
                      :sign/keys [(:inst move/params)]
                      :sign/meaning {:hint :a-law-is-coming}}))

     :veilwalk
      (-> world
          (assoc-in [:world/modifiers :veil (:target move/params)] true)
          (emit-sign {:sign/id (keyword (str "sign-" (hash move)))
                      :sign/type :omen
                      :sign/origin (:target move/params)
                      :sign/keys [(:target move/params)]
                      :sign/meaning {:hint :lanterns-fail}}))

     world)))
```

* * *

Event-family candidate generators (6 cores)
-------------------------------------------

Each generator returns **0..n** candidates. Keep them small and parameterized.

### Helpers: novelty + plausibility

```clojure
(defn novelty [world tag]
  ;; higher is better (less recent)
  (let [r (get-in world [:recency tag] 0)]
    (clamp01 (/ (- 5 r) 5.0))))

(defn has-mod? [world k]
  (get-in world [:world/modifiers k]))

(defn score->candidate [base {:keys [plausibility pressure telegraph novelty]}]
  (assoc base :ev/score {:plausibility plausibility
                         :pressure pressure
                         :telegraph telegraph
                         :novelty novelty}))
```

### A) Ultimatum → Raid → Feud

```clojure
(defn gen-ultimatum-raid-feud [world]
  (let [veil? (has-mod? world [:veil :loc/market])
        roads-fast? (<= (get-in world [:routes :route/royal-road :latency] 3) 2)
        plaus (cond-> 0.5 veil? (+ 0.2) roads-fast? (+ 0.15))
        nov  (novelty world "raid:sabotage")]
    (cond-> []
      (> plaus 0.65)
      (conj (score->candidate
             {:ev/id :ev-ult-1
              :ev/family :ultimatum-raid-feud
              :ev/keys [:loc/market :route/royal-road]
              :ev/telegraph [{:sign/type :scouts-seen :sign/origin :route/royal-road}]
              :ev/steps [:ultimatum :response-window :attack-or-settle :aftermath]
              :ev/params {:aggressor :group/bandits :motive (if veil? :kidnap :loot) :target :loc/market}}
             {:plausibility plaus :pressure 0.55 :telegraph 0.9 :novelty nov})))))
```

### B) Trial → Verdict → Enforcement (Sun loves this)

```clojure
(defn gen-trial-verdict [world]
  (let [court-legit (get-in world [:inst :inst/court-1 :legitimacy] 0)
        claim? (get-in world [:belief :group/townfolk :claim/champion-tyrant] nil)
        plaus (cond-> 0.4 (>= court-legit 4) (+ 0.25) claim? (+ 0.2))
        nov  (novelty world "trial")]
    (cond-> []
      (> plaus 0.7)
      (conj (score->candidate
             {:ev/id :ev-trial-1
              :ev/family :trial-verdict-enforcement
              :ev/keys [:inst/court-1]
              :ev/telegraph [{:sign/type :witnesses-summoned :sign/origin :inst/court-1}]
              :ev/steps [:summons :testimony :verdict :enforcement :aftermath]
              :ev/params {:court :inst/court-1 :issue :claim/champion-tyrant}}
             {:plausibility plaus :pressure 0.45 :telegraph 0.95 :novelty nov})))))
```

### C) Schism → Cult formation

```clojure
(defn gen-schism [world]
  (let [doubt? (some (fn [[_ c]] (<= (get c :confidence 0) 2))
                     (get-in world [:belief :group/townfolk] {}))
        plaus (cond-> 0.45 doubt? (+ 0.25))
        nov   (novelty world "schism")]
    (cond-> []
      (> plaus 0.65)
      (conj (score->candidate
             {:ev/id :ev-schism-1
              :ev/family :schism-cult
              :ev/keys [:loc/market]
              :ev/telegraph [{:sign/type :pamphlets :sign/origin :loc/market}]
              :ev/steps [:symbols :recruitment :conflict :purge-or-merge :aftermath]
              :ev/params {:seed-claim :claim/champion-tyrant}}
             {:plausibility plaus :pressure 0.5 :telegraph 0.85 :novelty nov})))))
```

### D) Trade boom → Scarcity → Black market

```clojure
(defn gen-trade-scarcity [world]
  (let [lat (get-in world [:routes :route/royal-road :latency] 3)
        plaus (cond-> 0.35 (<= lat 2) (+ 0.25) (has-mod? world :tomorrow-law) (+ 0.15))
        nov (novelty world "trade")]
    (cond-> []
      (> plaus 0.6)
      (conj (score->candidate
             {:ev/id :ev-trade-1
              :ev/family :trade-scarcity-blackmarket
              :ev/keys [:route/royal-road :loc/market]
              :ev/telegraph [{:sign/type :price-spike :sign/origin :loc/market}]
              :ev/steps [:boom :rationing :smuggling :charter-or-riot :aftermath]
              :ev/params {:good :grain}}
             {:plausibility plaus :pressure 0.4 :telegraph 0.9 :novelty nov})))))
```

### E) Wild reckoning → adaptation → tradition

```clojure
(defn gen-wild-reckoning [world]
  (let [taboo-breach? (get-in world [:world/flags :taboo/broken?] false)
        plaus (cond-> 0.3 taboo-breach? (+ 0.5))
        nov (novelty world "wild")]
    (cond-> []
      (> plaus 0.7)
      (conj (score->candidate
             {:ev/id :ev-wild-1
              :ev/family :wild-reckoning
              :ev/keys [:route/royal-road]
              :ev/telegraph [{:sign/type :animals-vanish :sign/origin :route/royal-road}]
              :ev/steps [:omen :constraint :crisis :new-law-or-rite :aftermath]
              :ev/params {:cause :taboo/breach}}
             {:plausibility plaus :pressure 0.6 :telegraph 0.8 :novelty nov})))))
```

### F) Succession / retirement arc (your prestige loop)

```clojure
(defn gen-succession [world]
  (let [ready? (get-in world [:champion/ready-to-retire?] false)
        plaus (if ready? 0.9 0.1)
        nov (novelty world "succession")]
    (cond-> []
      (> plaus 0.8)
      (conj (score->candidate
             {:ev/id :ev-succ-1
              :ev/family :succession
              :ev/keys [:champion]
              :ev/telegraph [{:sign/type :omens-gather :sign/origin :champion}]
              :ev/steps [:candidates :maneuvering :aker-rite :inheritance :baggage]
              :ev/params {:pool-size 7}}
             {:plausibility plaus :pressure 0.3 :telegraph 1.0 :novelty nov})))))
```

### Combine generators

```clojure
(defn gen-event-candidates [world]
  (vec (mapcat #(% world)
               [gen-ultimatum-raid-feud
                gen-trial-verdict
                gen-schism
                gen-trade-scarcity
                gen-wild-reckoning
                gen-succession])))
```

* * *

Picking an event (plausibility-first, novelty-aware)
----------------------------------------------------

```clojure
(defn event-score [{:keys [ev/score]}]
  (let [{:keys [plausibility pressure telegraph novelty]} ev/score]
    ;; tweak weights to taste
    (+ (* 2.0 plausibility)
       (* 1.0 novelty)
       (* 0.5 telegraph)
       (* 0.7 pressure))))

(defn choose-event [world candidates]
  (->> candidates
       (sort-by event-score >)
       first)) ;; start deterministic; add seeded sampling later if you want
```

* * *

Example tick: 3 gods submit moves → collide → signs → event candidates
----------------------------------------------------------------------

```clojure
(defn run-aker-boundary [world submitted-moves]
  (let [moves (->> submitted-moves
                   (filter #(move-allowed? world %))
                   vec)
        conflicts (group-by-conflicts moves)
        resolutions (for [[k ms] conflicts]
                      (resolve-conflict world k ms))
        ;; apply winner + complication as “side effect”
        world' (reduce (fn [w {:keys [winner complication]}]
                         (cond-> w
                           winner       (apply-move winner)
                           complication (apply-move (assoc complication :move/cost {:favor 0 :cred 0 :attention 0}))))
                       world
                       resolutions)
        candidates (gen-event-candidates world')
        chosen (choose-event world' candidates)]
    (-> world'
        (update :queue conj chosen)
        (assoc :last/resolutions resolutions))))

;; --- Example usage ---
(def example-moves
  [{:move/id :m1 :move/actor :herald :move/phase :always :move/type :whisper-network
    :move/keys {:primary [:route/royal-road]} :move/tags #{"trade" "herald"}
    :move/cost {:favor 1 :attention 1} :move/params {:route :route/royal-road}}

  {:move/id :m2 :move/actor :sun :move/phase :night :move/type :dawn-decree
    :move/keys {:primary [:inst/court-1]} :move/tags #{"trial" "sun:night"}
    :move/cost {:favor 2 :attention 2} :move/params {:inst :inst/court-1 :law :curfew}}

  {:move/id :m3 :move/actor :moon :move/phase :night :move/type :veilwalk
    :move/keys {:primary [:loc/market]} :move/tags #{"raid:sabotage" "moon:night"}
    :move/cost {:favor 2 :attention 1} :move/params {:target :loc/market}}])

;; Suppose it’s night and the champion chose to sleep:
(def world-night-asleep
  (-> your-world
      (assoc :world/phase :night
             :champion/asleep? true)))

(def world-after (run-aker-boundary world-night-asleep example-moves))
```

What you’ll see in `world-after`:

*   `:signs` populated with **telegraphs**

*   `:world/modifiers` updated (curfew tomorrow, veil on market, faster road)

*   `:queue` gets one **chosen event candidate** (likely an ultimatum/raid chain _or_ a trial chain, depending on your state)


* * *

Next “continue” I’ll do one of these (pick implicitly by continuing)
--------------------------------------------------------------------

*   **(A)** Write a compact “event executor” that walks `:ev/steps` and mutates the graph + beliefs (still Clojure pseudocode)

*   **(B)** Convert this into a **macro-ish DSL** (`defmove`, `defevent-family`, `defsign`) so it reads like myth-scripting rather than raw maps
