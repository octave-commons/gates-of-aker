(ns fantasia.sim.social
  (:require [fantasia.sim.hex :as hex]
            [fantasia.sim.facets :as f]
            [fantasia.sim.events :as events]))

(def social-interaction-types
  {:small-talk {:mood-boost 0.05 :transmission-strength 0.3 :name "Small talk"}
   :gossip {:mood-boost 0.10 :transmission-strength 0.5 :name "Gossip"}
   :debate {:mood-boost 0.00 :transmission-strength 0.8 :name "Debate"}
   :ritual {:mood-boost 0.20 :transmission-strength 1.0 :divine-favor 1.0 :name "Ritual"}
   :teaching {:mood-boost 0.07 :transmission-strength 0.7 :name "Teaching"}})

(defn- choose-interaction-type
  "Choose interaction type based on agent moods and randomness."
   [agent1 agent2]
   (let [mood1 (get-in agent1 [:needs :mood] 0.5)
         mood2 (get-in agent2 [:needs :mood] 0.5)
         avg-mood (/ (+ mood1 mood2) 2.0)
         rand (.nextInt (java.util.Random.) 100)]
     (cond
       (< rand 10) :ritual
       (and (< rand 30) (or (= (:role agent1) :priest) (= (:role agent2) :priest))) :debate
       (< rand 50) :teaching
       (< rand 75) :gossip
       :else :small-talk)))

(defn- apply-mood-change
   "Apply mood change to an agent, clamping between 0 and 1."
   [agent delta]
   (let [current-mood (get-in agent [:needs :mood] 0.5)
         new-mood (f/clamp01 (+ current-mood delta))]
     (assoc-in agent [:needs :mood] new-mood)))

(defn trigger-social-interaction!
   "Execute a social interaction between two agents.
    Returns updated agents and interaction record."
   [world agent1 agent2]
   (let [interaction-type (choose-interaction-type agent1 agent2)
         config (get social-interaction-types interaction-type)
         agent1' (apply-mood-change agent1 (:mood-boost config))
         agent2' (apply-mood-change agent2 (:mood-boost config))
         divine-favor-gained (:divine-favor config 0)
         interaction {:interaction-type interaction-type
                    :agent-1-id (:id agent1)
                    :agent-2-id (:id agent2)
                    :mood-change-1 (:mood-boost config)
                    :mood-change-2 (:mood-boost config)
                    :divine-favor-gained divine-favor-gained
                    :transmission-strength (:transmission-strength config)
                    :timestamp (:tick world)
                    :location (:pos agent1)}]
     {:agent-1 agent1'
      :agent-2 agent2'
      :interaction interaction}))

(defn enhance-packet-with-social-context
   "Enhance a facet packet with social interaction context."
   [packet interaction-strength mood1 mood2]
   (let [strength-multiplier interaction-strength
         mood-bonus (* 0.2 (/ (+ mood1 mood2) 2.0))]
     (-> packet
         (update :spread-gain #(* % strength-multiplier))
         (update :seed-strength #(+ % mood-bonus)))))

(defn apply-social-packet-to-listener
   "Apply a socially-enhanced packet to listener agent."
   [world listener speaker packet interaction-strength]
   (let [mood1 (get-in speaker [:needs :mood] 0.5)
         mood2 (get-in listener [:needs :mood] 0.5)
         enhanced-packet (enhance-packet-with-social-context packet interaction-strength mood1 mood2)]
     (enhanced-packet))
