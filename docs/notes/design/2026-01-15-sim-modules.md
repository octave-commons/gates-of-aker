
1) `backend/src/fantasia/sim/hex.clj` (axial hex utilities)
===========================================================

```clojure
(ns fantasia.sim.hex)

;; Axial coords: [q r]
;; Pointy-top axial direction order (E, NE, NW, W, SW, SE)
(def dirs
  [[+1  0]
   [+1 -1]
   [ 0 -1]
   [-1  0]
   [-1 +1]
   [ 0 +1]])

(defn add [[aq ar] [bq br]] [(+ aq bq) (+ ar br)])
(defn sub [[aq ar] [bq br]] [(- aq bq) (- ar br)])
(defn scale [[dq dr] k] [(* dq k) (* dr k)])

(defn neighbors [pos]
  (mapv #(add pos %) dirs))

;; cube: (x, y, z) with x=q, z=r, y=-x-z
(defn axial->cube [[q r]]
  (let [x q
        z r
        y (- 0 x z)]
    [x y z]))

(defn cube->axial [[x _y z]] [x z])

(defn cube-round [[x y z]]
  (let [rx (Math/round (double x))
        ry (Math/round (double y))
        rz (Math/round (double z))
        dx (Math/abs (- rx x))
        dy (Math/abs (- ry y))
        dz (Math/abs (- rz z))
        [rx ry rz]
        (cond
          (and (> dx dy) (> dx dz)) [(- 0 ry rz) ry rz]
          (> dy dz)                [rx (- 0 rx rz) rz]
          :else                    [rx ry (- 0 rx ry)])]
    [rx ry rz]))

(defn distance [a b]
  (let [[ax ay az] (axial->cube a)
        [bx by bz] (axial->cube b)]
    (long (max (Math/abs (long (- ax bx)))
               (Math/abs (long (- ay by)))
               (Math/abs (long (- az bz)))))))

(defn in-radius? [center r pos]
  (<= (distance center pos) (long r)))

(defn coords-in-radius
  "Return all axial coords in a hex disk of radius r centered at `center`."
  [center r]
  (let [[cq cr] center
        r (long r)]
    (vec
      (for [dq (range (- r) (inc r))
            dr (range (max (- r) (- (- dq) r))
                      (inc (min r (- (- dq) (- r))))) ]
        [(+ cq dq) (+ cr dr)]))))
```

* * *

2) `backend/src/fantasia/sim/factions.clj` (place 6 neighbors + init decks + seed influence)
============================================================================================

This assumes:

*   you have `resources/decks/neighbor-decks.edn`

*   you have `fantasia.sim.cards` (the deck engine)

*   you’re fine with **symbolic keys** at first (route/main, loc/market, etc.)


```clojure
(ns fantasia.sim.factions
  (:require [fantasia.sim.hex :as hex]
            [fantasia.sim.cards :as cards]))

(def default-budget {:favor 8 :cred 6 :attention 0})

;; Map your 6 neighbor faction ids to axial directions (clockwise-ish).
;; This matches the EDN deck keys we’ve been using: north, ne, se, south, sw, nw.
(def neighbor-slots
  [[:faction/north (nth hex/dirs 2)]  ;; NW
   [:faction/ne    (nth hex/dirs 1)]  ;; NE
   [:faction/se    (nth hex/dirs 0)]  ;; E
   [:faction/south (nth hex/dirs 5)]  ;; SE
   [:faction/sw    (nth hex/dirs 4)]  ;; SW
   [:faction/nw    (nth hex/dirs 3)]]) ;; W

(defn place-neighbor-settlements
  [{:keys [center dist]}]
  (into {}
        (map (fn [[fid dir]]
               [fid (hex/add center (hex/scale dir dist))])
             neighbor-slots)))

(defn ensure-rules [world]
  (-> world
      (update :rules merge {:attention-cap 12
                            :sign-cap 60})))

(defn ensure-places
  "Introduce shared world entities that cards will reference early.
   These can be symbolic now; later you can map them to real tile coords."
  [world]
  (-> world
      (update :routes merge {:route/main {:latency 3 :security 2 :visibility 2}})
      (update :loci merge {:loc/market {:kind :market}})))

(defn init-faction
  [world {:keys [faction-id name settlement-pos theme draw]}]
  (let [deck (cards/init-deck-state {:world-seed (:seed world)
                                    :faction-id faction-id
                                    :draw draw})]
    (assoc-in world [:factions faction-id]
              {:id faction-id
               :name name
               :theme theme
               :settlement-pos settlement-pos
               :attitude {} ;; start simple
               :deity {:id faction-id :budget default-budget}
               :deck deck})))

(defn bootstrap-factions
  "Create player + 6 neighbors from deck pack.
   deck-pack = {:cards {...} :decks {...}} (from EDN)
   opts = {:center [0 0] :dist 12}"
  [world deck-pack {:keys [center dist] :or {center [0 0] dist 12}}]
  (let [world (-> world
                  ensure-rules
                  ensure-places
                  (assoc :cards (:cards deck-pack)))
        placements (place-neighbor-settlements {:center center :dist dist})

       ;; player faction
        world (assoc-in world [:factions :faction/player]
                        {:id :faction/player
                         :name "Home"
                         :theme #{:player}
                         :settlement-pos center
                         :attitude {}
                         :deity {:id :faction/player :budget default-budget}
                         :deck {:draw [] :hand [] :discard [] :cooldowns {}}})]

   ;; neighbors: require that a deck exists for each slot
    (reduce
      (fn [w [fid pos]]
        (let [deck-meta (get-in deck-pack [:decks fid])]
          (when-not deck-meta
            (throw (ex-info "Missing neighbor deck in EDN" {:faction fid})))
          (init-faction w {:faction-id fid
                           :name (:faction/name deck-meta)
                           :theme (:theme deck-meta)
                           :settlement-pos pos
                           :draw (:draw deck-meta)})))
      world
      placements)))

;; -----------------------------------------------------------------------------
;; Influence seeding (so collisions aren’t all noise)
;; -----------------------------------------------------------------------------

(defn set-influence [world deity-id key v]
  (assoc-in world [:influence [deity-id key]] (double v)))

(defn influence-falloff
  "0..1 falloff inside radius r. Cheap and good enough."
  [d r]
  (let [d (double d)
        r (double r)]
    (-> (- 1.0 (/ d (max 1.0 r))) (max 0.0) (min 1.0))))

(defn seed-local-tile-influence
  "Seed influence for each faction on tile keys near its settlement.
   Even if no cards target [:tile [q r]] yet, this will pay off later for walls/raids."
  [world {:keys [radius] :or {radius 8}}]
  (reduce
    (fn [w [fid f]]
      (let [center (:settlement-pos f)
            coords (hex/coords-in-radius center radius)]
        (reduce
          (fn [w2 p]
            (let [d (hex/distance center p)
                  v (* 0.85 (influence-falloff d radius))]
              (if (pos? v)
                (set-influence w2 fid [:tile p] v)
                w2)))
          w
          coords)))
    world
    (:factions world)))

(defn theme-bias [theme]
  {:route/main   (cond
                   (contains? theme :trade) 0.85
                   (contains? theme :herald) 0.75
                   :else 0.55)
   :loc/market   (cond
                   (contains? theme :trade) 0.85
                   (contains? theme :moon) 0.75
                   :else 0.50)
   :claims       (cond
                   (contains? theme :sun) 0.80
                   (contains? theme :moon) 0.70
                   :else 0.55)})

(defn seed-symbolic-influence
  "Seed influence on the *symbolic* keys that early cards use."
  [world]
  (reduce
    (fn [w [fid f]]
      (let [bias (theme-bias (:theme f))]
        (-> w
            ;; Route & market loci
            (set-influence fid [:route :route/main] (:route/main bias))
            (set-influence fid [:tile :loc/market] (:loc/market bias))

           ;; Claim battlegrounds (until you have real claim graphs)
            (set-influence fid [:claim :claim/contested] (:claims bias))
            (set-influence fid [:claim :claim/official]  (:claims bias))

           ;; Each faction naturally has influence over its own settlement key
            (set-influence fid [:settlement fid] 0.90)

           ;; Neighbors generally care about the player settlement too (competition)
            (set-influence fid [:settlement :faction/player]
                           (if (contains? (:theme f) :relief) 0.55 0.75)))))
    world
    (:factions world)))

(defn seed-influence
  "Call after bootstrap-factions."
  [world]
  (-> world
      (assoc :influence {})
      seed-symbolic-influence
      (seed-local-tile-influence {:radius 8})))
```

* * *

3) `backend/src/fantasia/sim/aker.clj` (Night boundary pipeline)
================================================================

This is the “one function” you call when champion is asleep and it’s night.

```clojure
(ns fantasia.sim.aker
  (:require [fantasia.sim.cards :as cards]
            [fantasia.sim.drama :as drama]))

(defn night-eligible? [world]
  (and (= (:phase world) :night)
       (true? (get-in world [:champion :asleep?]))))

(defn neighbor-faction?
  "Only the 6 neighbors play automatically. Player can be added later."
  [fid]
  (and (keyword? fid)
       (not= fid :faction/player)))

(defn night-boundary
  "Runs ONE night 'boundary':
   1) neighbors play cards (collisions → signs/modifiers)
   2) generate + enqueue best event candidate
   3) advance event queue one step

  Returns updated world."
  [world]
  (if-not (night-eligible? world)
    world
    (let [world (cards/run-night-cards world (:cards world)
                                       {:hand-size 3
                                        :playable-faction? neighbor-faction?})
          world (drama/enqueue-best! world)
          world (drama/tick-event-queue world)]
      world)))
```

* * *

4) The missing piece you _need_ before this feels real: card targeting/instantiation
====================================================================================

Right now, your EDN cards contain placeholders like `:faction/target` (or similar). Before collisions matter, each “play” needs to **instantiate** a template card with concrete keys.

### Minimal rule (good enough for MVP)

*   Neighbors target `:faction/player` by default.

*   “False flag” picks a third party neighbor.

*   Dream courier targets champion (or a random pawn).


If you want, I’ll generate a tiny `fantasia.sim.targeting` module that:

*   walks the card template and replaces `:target/*` placeholders

*   returns a concrete play `{faction-id card-id card}` ready for collision grouping


_(This is the one place where “data-only cards” become “gameplay actions.”)_

* * *

5) How this plugs into your current `sim/core.clj` (minimal integration plan)
=============================================================================

You don’t need to rewrite everything at once. The smallest integration order is:

1.  Add world keys (even if the map is still square for a moment):


```clojure
:phase :day
:champion {:id 0 :asleep? false}
:factions {...}     ;; from bootstrap-factions
:cards {...}        ;; from EDN
:signs []
:modifiers []
:event {:candidates [] :queue [] :log []}
```

2.  On “sleep” (WS op later), do:


```clojure
(-> world
    (assoc :phase :night)
    (assoc-in [:champion :asleep?] true))
```

3.  During ticks while asleep, call:


```clojure
(fantasia.sim.aker/night-boundary world)
```

4.  Then later swap the map to hex + walls + pathing without changing the night pipeline.
