(ns fantasia.sim.agents
  (:require [fantasia.sim.facets :as f]
           [fantasia.sim.spatial_facets :as sf]
           [fantasia.sim.events :as events]
           [fantasia.sim.spatial :as spatial]
           [fantasia.sim.hex :as hex]
           [fantasia.sim.constants :as const]))

(defn update-needs
  "Decay warmth/food/sleep relative to cold snap."
  [world agent]
  (let [alive? (get-in agent [:status :alive?] true)]
    (if (not alive?)
      agent
      (let [temperature (double (or (:temperature world) 0.6))
            cold (max 0.0 (- 1.0 temperature))
            asleep? (get-in agent [:status :asleep?] false)
            current-needs (:needs agent)
            warmth (get current-needs :warmth 0.6)
            pos (:pos agent)
            campfire-pos (:campfire world)
            campfire-near? (and pos campfire-pos (<= (hex/distance pos campfire-pos) const/campfire-radius))
            tile-key (when pos (vector (first pos) (second pos)))
            house-near?
            (and pos
                 (or (= :house (get-in world [:tiles tile-key :structure]))
                     (some (fn [n]
                             (= :house (get-in world [:tiles (vector (first n) (second n))] :structure)))
                           (hex/neighbors pos))))
            warmth-bonus (cond
                           campfire-near? const/warmth-bonus-campfire
                           house-near? const/warmth-bonus-house
                           :else 0.0)
            warmth-decay (+ const/base-warmth-decay (* const/cold-warmth-decay-factor cold))
            role (:role agent)
            food-decay (cond
                         (= role :deer) const/deer-food-decay
                         (= role :wolf) const/wolf-food-decay
                         asleep? const/base-food-decay-asleep
                         :else const/base-food-decay-awake)
            sleep-decay (if asleep? 0.0 const/base-sleep-decay)
            rest-change (if asleep?
                          (+ const/base-rest-recovery
                             (if house-near? const/house-rest-bonus 0.0))
                          (- const/base-rest-decay))
            social-decay const/base-social-decay
            starvation-health-decay (if (and (or (= role :deer) (= role :wolf))
                                              (< (get current-needs :food 0.7) 0.15))
                                        const/wildlife-starvation-health-decay
                                        0.0)
            warmth' (f/clamp01 (+ (- warmth warmth-decay) warmth-bonus))
            food' (f/clamp01 (- (get current-needs :food 0.7) food-decay))
            sleep' (f/clamp01 (- (get current-needs :sleep 0.7) sleep-decay))
             rest' (f/clamp01 (+ (get current-needs :rest 0.7) rest-change))
            social' (f/clamp01 (- (get current-needs :social 0.55) social-decay))
            health (get current-needs :health 1.0)
            heat-damage (if (> warmth' const/heat-damage-threshold) const/heat-damage-per-tick 0.0)
            health' (f/clamp01 (- health starvation-health-decay heat-damage))
            nearby-tiles (when pos
                           (concat [pos] (hex/neighbors pos)
                                   (take 6 (mapcat hex/neighbors (hex/neighbors pos)))))
            nearby-structures (set (keep (fn [p]
                                       (get-in world [:tiles (vector (first p) (second p)) :structure]))
                                     nearby-tiles))
            has-house? (contains? nearby-structures :house)
            has-temple? (contains? nearby-structures :temple)
            has-school? (contains? nearby-structures :school)
            has-library? (contains? nearby-structures :library)
            nearby-tree? (some (fn [p]
                               (= :tree (get-in world [:tiles (vector (first p) (second p)) :resource])))
                             nearby-tiles)
            env-mood-bonus (reduce + 0.0
                                   (keep identity
                                         [(when has-house? const/mood-bonus-house)
                                          (when has-temple? const/mood-bonus-temple)
                                          (when has-school? const/mood-bonus-school)
                                          (when has-library? const/mood-bonus-library)
                                          (when nearby-tree? const/mood-bonus-trees)]))
            current-mood (get current-needs :mood 0.5)
             mood-change (+ env-mood-bonus
                          (cond
                             (< warmth' 0.25) -0.015
                             (< warmth' 0.4) -0.008
                             (> warmth' const/heat-damage-threshold) -0.02
                             (> warmth' 0.75) 0.008
                             (> warmth' 0.65) 0.004
                             :else 0.0)
                           (cond
                             (< social' 0.3) -0.01
                             (> social' 0.7) 0.005
                             :else 0.0)
                           (cond
                             (< rest' 0.3) -0.012
                             (> rest' 0.75) 0.006
                             :else 0.0))
            mood' (f/clamp01 (+ current-mood mood-change))
            warmth-thoughts (cond
                              (< warmth' 0.15) ["I can't feel my toes" "It's unbearable cold"]
                              (< warmth' 0.3) ["Need warmth soon" "Shaking from cold"]
                              (< warmth' 0.5) ["The chill bites" "Could use some heat"]
                              (> warmth' const/heat-damage-threshold) ["Too hot! Burning up!" "Get me away from this fire!"]
                              (> warmth' 0.9) ["Sweating profusely" "Way too warm"]
                              (> warmth' 0.75) ["Nice and toasty" "This fire feels good"]
                              (> warmth' 0.6) ["Finally comfortable" "Just right"]
                              :else [])
             mood-thoughts (vec (concat
                               (cond
                                 (> mood' 0.85) ["Feeling bright" "Life feels good" "So happy today"]
                                 (> mood' 0.7) ["Spirits are lifted" "A good moment"]
                                 (< mood' 0.25) ["Everything feels heavy" "Hard to smile"]
                                 (< mood' 0.4) ["A bit down" "Need a little joy"]
                                 :else [])
                               (when has-temple? ["A sense of peace here" "Sacred ground feels good"])
                               (when has-library? ["Wisdom surrounds this place" "Knowledge brings comfort"])
                               (when has-school? ["The future feels bright" "Learning fills the air"])
                               (when (and has-house? (not has-library?)) ["Home is near" "Safe and warm"])
                               (when nearby-tree? ["Nature's beauty" "Trees are calming"])))
            rest-thoughts (cond
                            (> rest' 0.8) ["Rested and ready" "Fully recharged"]
                            (< rest' 0.3) ["So tired" "Need proper rest"]
                            :else [])
            thoughts (vec (concat warmth-thoughts mood-thoughts rest-thoughts))
            random-thought (when (seq thoughts)
                             (rand-nth thoughts))
            updated-needs (assoc current-needs :warmth warmth' :food food' :sleep sleep' :rest rest'
                                 :mood mood' :social social' :health health')]
        (-> agent
            (assoc :needs updated-needs)
            (assoc :last-thought random-thought))))))

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

(defn query-need-axis!
  "Query facet axis for need-based behavior.
   Returns empty map for now - TODO: Implement facet queries."
  [world agent axis concept-words]
  {})
