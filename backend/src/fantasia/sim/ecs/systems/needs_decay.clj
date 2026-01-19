(ns fantasia.sim.ecs.systems.needs-decay
  (:require [brute.entity :as be]
            [fantasia.sim.ecs.components :as c]
            [fantasia.sim.facets :as f]))

(defn clamp01 [x]
  (cond
    (< x 0.0) 0.0
    (> x 1.0) 1.0
    :else x))

(defn process
  "Decay warmth/food/sleep for all agents based on cold-snap."
  [ecs-world cold-snap]
  (let [needs-instance (c/->Needs 0.6 0.7 0.7)
        needs-type (be/get-component-type needs-instance)
        agent-ids (be/get-all-entities-with-component ecs-world needs-type)
        cold-snap (or cold-snap 0.5)]
    (reduce (fn [acc agent-id]
              (let [needs (or (be/get-component acc agent-id needs-type)
                              (c/->Needs 0.6 0.7 0.7))
                    warmth-val (double (or (:warmth needs) 0.6))
                    food-val (double (or (:food needs) 0.7))
                    sleep-val (double (or (:sleep needs) 0.7))
                    warmth (clamp01 (- warmth-val (* 0.03 cold-snap)))
                    food (clamp01 (- food-val 0.01))
                    sleep (clamp01 (- sleep-val 0.008))]
                (be/add-component acc agent-id (c/->Needs warmth food sleep))))
            ecs-world
            agent-ids)))
