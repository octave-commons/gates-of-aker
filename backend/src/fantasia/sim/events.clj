(ns fantasia.sim.events
  "Event archetypes (NOT instances). Pure data + a tiny macro DSL.

  Event types drive:
  - facet signatures used for recall scoring
  - claim candidates for attribution battles
  - thresholds for when recall becomes a 'mention'
  "
  (:require [clojure.string :as str]))

(defonce ^:private *event-types (atom {}))

(defn register! [et]
  (swap! *event-types assoc (:id et) et)
  et)

(defn all-event-types []
  (->> @*event-types vals (sort-by :id) vec))

(defn get-event-type [id]
  (get @*event-types id))

(defn- deep-merge-pairs [a b]
  (merge-with
    (fn [x y]
      (cond
        (and (map? x) (map? y)) (deep-merge-pairs x y)
        (and (vector? x) (vector? y)) (into x y)
        :else y))
    a b))

(defn- merge-fragments [& frags]
  (reduce deep-merge-pairs {} frags))

(defmacro kind [k] `{:kind ~k})

(defmacro signature
  "Weighted facet signature for recall scoring."
  [m]
  `{:signature ~m})

(defmacro threshold
  "Recall score threshold beyond which it's considered strongly recalled."
  [x]
  `{:threshold (double ~x)})

(defmacro mention-delta
  "Minimum delta in recall score to count as a 'mention-worthy' activation."
  [x]
  `{:mention-delta (double ~x)})

(defmacro claim
  "Add a claim candidate.
  (claim :claim/foo :deity :patron/fire :bonus 0.05)

  :deity is a facet-key that can exist in the frontier (ex: :patron/fire, :deity/storm)
  :bonus is a base bias for this claim."
  [id & {:keys [deity bonus] :or {bonus 0.0}}]
  `{:claims [{:id ~id :deity ~deity :bonus (double ~bonus)}]})

(defmacro tiers
  "Optional canonization tiers; not used heavily yet but wired for future."
  [m]
  `{:tiers ~m})

(defmacro defeventtype [sym name & forms]
  (let [id (keyword sym)]
    `(let [parts# (merge-fragments ~@forms)
           et# (merge-fragments
                 {:id ~id
                  :name ~name
                  :kind :unspecified
                  :signature {}
                  :threshold 0.75
                  :mention-delta 0.18
                  :claims []
                  :tiers {:minor 10.0 :significant 50.0 :foundational 200.0}}
                 parts#)]
       (def ~sym et#)
       (register! et#))))

;; -----------------------------------------------------------------------------
;; Built-in event archetypes for Prototype 0/1
;; -----------------------------------------------------------------------------

(defeventtype winter-pyre "The Winter Pyre"
  (kind :battle)
  (signature
    {:winter 0.8
     :cold 0.6
     :trees 0.7
     :fire 1.0
     :smoke 0.35
     :enemy-rout 0.7
     :awe 0.6
     :judgment 0.6
     :patron/fire 0.7
     :deity/storm 0.35})
  (threshold 0.78)
  (mention-delta 0.17)
  (claim :claim/winter-judgment-flame :deity :patron/fire :bonus 0.02)
  (claim :claim/storm-wrath :deity :deity/storm :bonus 0.01)
  (claim :rebuttal/natural-chance :deity nil :bonus 0.00))

(defeventtype lightning-commander "Lightning Strikes the Commander"
  (kind :battle)
  (signature
    {:storm 0.8
     :lightning 1.0
     :battle 0.55
     :enemy-leader 1.0
     :awe 0.8
     :judgment 0.35
     :patron/fire 0.25
     :deity/storm 0.85})
  (threshold 0.76)
  (mention-delta 0.18)
  (claim :claim/storm-spear :deity :deity/storm :bonus 0.03)
  (claim :claim/patron-smiting :deity :patron/fire :bonus 0.01)
  (claim :rebuttal/weather-happens :deity nil :bonus 0.00))
