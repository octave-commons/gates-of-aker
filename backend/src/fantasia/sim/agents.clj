(ns fantasia.sim.agents
  (:require [fantasia.sim.facets :as f]
            [fantasia.sim.events :as events]
            [fantasia.sim.spatial :as spatial]
            [fantasia.sim.hex :as hex]))

(defn update-needs
  "Decay warmth/food/sleep relative to cold snap."
  [world agent]
  (let [cold (:cold-snap world)
        warmth (get-in agent [:needs :warmth] 0.6)
        warmth' (f/clamp01 (- warmth (* 0.03 cold)))
        food' (f/clamp01 (- (get-in agent [:needs :food] 0.7) 0.01))
        sleep' (f/clamp01 (- (get-in agent [:needs :sleep] 0.7) 0.008))]
    (assoc-in agent [:needs] {:warmth warmth' :food food' :sleep sleep'})))

(defn choose-packet
  "Convert agent state + local context into a broadcast packet."
  [world agent]
  (let [warmth (get-in agent [:needs :warmth] 0.5)
        base (cond-> [:cold]
               (spatial/at-trees? world (:pos agent)) (conj :trees)
               (< warmth 0.25) (conj :fear)
               (spatial/near-shrine? world (:pos agent)) (conj :fire))
        base (if (= (:role agent) :priest)
               (conj base :judgment)
               base)]
    {:intent (cond
               (< warmth 0.25) :warn
               (= (:role agent) :priest) :convert
               :else :chatter)
     :facets base
     :tone {:awe (if (= (:role agent) :priest) 0.3 0.1)
            :urgency (if (< warmth 0.25) 0.6 0.2)}}))

(defn interactions
  "Generate adjacent agent pairs for conversation (using hex distance)."
  [agents]
  (for [a agents
        b agents
        :when (and (not= (:id a) (:id b))
                   (<= (hex/distance (:pos a) (:pos b)) 1))]
    [a b]))

(defn scaled-edges
  "Apply lever modifiers to frontier spread edges."
  [world]
  (let [base (:edges world)
        icon (get-in world [:levers :iconography] {})
        fire->patron (double (get icon :fire->patron 0.80))
        lightning->storm (double (get icon :lightning->storm 0.75))
        storm->deity (double (get icon :storm->deity 0.85))]
    (-> base
        (assoc [:fire :patron/fire] fire->patron)
        (assoc [:lightning :storm] lightning->storm)
        (assoc [:storm :deity/storm] storm->deity))))

(defn score-claim [frontier claim {:keys [claim-hint speaker]}]
  (let [deity (:deity claim)
        deity-a (if deity (double (get-in frontier [deity :a] 0.0)) 0.0)
        hint-bonus (if (= claim-hint (:id claim)) 0.15 0.0)
        speaker-bonus (cond
                        (= (:role speaker) :priest) 0.03
                        (= (:role speaker) :knight) 0.01
                        :else 0.0)]
    (+ deity-a hint-bonus speaker-bonus (double (:bonus claim 0.0)))))

(defn pick-claim [event-type frontier {:keys [claim-hint speaker]}]
  (let [claims (:claims event-type)
        scored (map (fn [c]
                      [(:id c)
                       (score-claim frontier c {:claim-hint claim-hint
                                                :speaker speaker})])
                    claims)]
    (->> scored (sort-by second >) ffirst)))

(defn recall-and-mentions
  "Update listener recall metrics and emit mention/trace metadata."
  [old-recall frontier packet speaker]
  (let [claim-hint (:claim-hint packet)]
    (reduce
      (fn [{:keys [new-recall mentions traces]} et]
        (let [{:keys [score]} (f/event-recall frontier (:signature et)
                                               {:threshold (:threshold et)})
              old (double (get old-recall (:id et) 0.0))
              new (double score)
              drec (- new old)
              mention? (> drec (double (:mention-delta et)))
              claim (when mention?
                      (pick-claim et frontier {:claim-hint claim-hint
                                               :speaker speaker}))
              weight (when mention?
                       (* drec
                          (cond
                            (= (:role speaker) :priest) 1.25
                            (= (:role speaker) :knight) 1.10
                            :else 1.00)))]
          {:new-recall (assoc new-recall (:id et) new)
           :mentions (cond-> mentions
                        mention?
                        (conj {:event-type (:id et)
                               :claim claim
                               :weight weight
                               :event-instance (:instance-id (:event-token packet))}))
           :traces (cond-> traces
                     mention?
                     (conj {:trace/id (str "t-" (:id et) "-" (:listener-id packet)
                                            "-" (:speaker-id packet) "-"
                                            (System/currentTimeMillis))
                            :tick (:tick packet)
                            :listener (:listener-id packet)
                            :speaker (:speaker-id packet)
                            :packet (dissoc packet :listener-id :speaker-id :tick)
                            :seeded (mapv (fn [f] {:facet f :delta 0.30})
                                          (:facets packet))
                            :spread (take 18 (:spread packet))
                            :event-recall {:event-type (:id et)
                                           :delta drec
                                           :new new}
                            :claim-activation [{:claim claim :delta (* 0.6 drec)}]
                            :mention {:event-type (:id et)
                                      :claim claim
                                      :weight weight
                                      :event-instance (:instance-id (:event-token packet))}}))}))
      {:new-recall old-recall :mentions [] :traces []}
      (events/all-event-types))))

(defn apply-packet-to-listener
  "Apply a packet from speaker→listener, updating frontier/recall."
  [world listener speaker packet]
  (let [edges (scaled-edges world)
        fr0 (f/decay-frontier (:frontier listener) {:decay 0.93})
        fr0 (if (spatial/near-shrine? world (:pos listener))
              (f/bump-facet fr0 :fire 0.08)
              fr0)
        fr1 (f/seed fr0 (:facets packet) {:seed-strength 0.30})
        spread (f/spread-step fr1 edges {:spread-gain 0.50 :max-hops 2})
        fr2 (:frontier spread)
        res (recall-and-mentions (:recall listener)
                                 fr2
                                 (assoc packet
                                        :spread (:deltas spread)
                                        :listener-id (:id listener)
                                        :speaker-id (:id speaker)
                                        :tick (:tick world))
                                 speaker)]
     {:listener (-> listener
                    (assoc :frontier fr2)
                    (assoc :recall (:new-recall res)))
      :mentions (:mentions res)
      :traces (:traces res)}))

