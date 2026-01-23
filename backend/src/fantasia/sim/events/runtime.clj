(ns fantasia.sim.events.runtime
  (:require [fantasia.sim.events :as events]
            [fantasia.sim.agents :as agents]
            [fantasia.sim.facets :as f]))

(defn generate
  "Sample a world event candidate from current world + agent list.
   Returns nil or an event map with :id/:type/:pos/:witnesses/etc.
   Optional rng overrides random sampling for deterministic tests."
  ([world agents]
   (generate world agents nil))
   ([world agents rng]
        (let [t (or (:tick world) 0)
              seed (or (:seed world) 1)
              r (if rng 
                    rng 
                    (java.util.Random. (long (+ seed (* 7919 t)))))
           p (.nextDouble r)
          bounds (get-in world [:map :bounds] {:w 32 :h 32})
          [w h] (if (= (:shape bounds) :rect) [(:w bounds) (:h bounds)] [32 32])
          pos [(.nextInt r (int (max 1 w))) (.nextInt r (int (max 1 h)))]
          witnesses (->> agents
                         (filter (fn [a]
                                   (<= (+ (Math/abs (long (- (first (:pos a)) (first pos))))
                                          (Math/abs (long (- (second (:pos a)) (second pos))))) 4)))
                         (mapv :id))
          witness-score (min 1.0 (/ (count witnesses) 6.0))
           cold (or (:cold-snap world) 0.0)
           fear (if (empty? agents)
                   0.0
                   (->> agents
                     (map #(get-in % [:needs :warmth] 0.6))
                     (map (fn [w] (- 1.0 (double w))))
                     (reduce + 0.0)
                     (/ (count agents))
                     (min 1.0)))
          ev (cond
               (< p (+ 0.015 (* 0.015 cold) (* 0.01 fear)))
               {:type :winter-pyre
                :pos pos
                :impact (+ 0.6 (* 0.4 (.nextDouble r)))
                :witness-score witness-score
                :witnesses witnesses}
               (< p (+ 0.004 (* 0.01 cold)))
               {:type :lightning-commander
                :pos pos
                :impact (+ 0.7 (* 0.3 (.nextDouble r)))
                :witness-score (min 1.0 (+ witness-score 0.2))
                :witnesses witnesses}
               :else nil)]
      (when ev
        (assoc ev :id (str "e-" (name (:type ev)) "-" t "-" (.nextInt r 100000))
                  :tick t)))))



(defn apply-to-witness
  "Apply a world event instance to a single agent, returning
   {:agent updated-agent :mentions [...] :traces [...]}"
  [world agent event-instance]
  (let [et (events/get-event-type (:type event-instance))
        impact (double (:impact event-instance))
        fr0 (f/decay-frontier (:frontier agent) {:decay 0.96})
        fr1 (reduce (fn [fr [facet w]]
                      (f/bump-facet fr facet (* impact 0.22 (double w))))
                    fr0
                    (:signature et))
        spread (f/spread-step fr1 (agents/scaled-edges world)
                              {:spread-gain 0.55 :max-hops 2})
        fr2 (:frontier spread)
        speaker {:id :world :role :world}
        packet {:intent :witness
                :facets (->> (:signature et) keys (take 4) vec)
                :tone {:awe (double (:witness-score event-instance))
                       :urgency 0.4}
                :claim-hint nil
                :event-token {:type (:type event-instance)
                              :instance-id (keyword (:id event-instance))
                              :event-type (:type event-instance)
                              :tick (:tick event-instance)}}
        res (agents/recall-and-mentions (:recall agent)
                                        fr2
                                        (assoc packet
                                               :spread (:deltas spread)
                                               :listener-id (:id agent)
                                               :speaker-id :world
                                               :tick (:tick world))
                                        speaker)]
    {:agent (-> agent (assoc :frontier fr2) (assoc :recall (:new-recall res)))
     :mentions (:mentions res)
     :traces (:traces res)}))
