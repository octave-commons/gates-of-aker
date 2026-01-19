(ns fantasia.sim.ecs.systems.needs-decay
  (:require [brute.entity :as be]
            [fantasia.sim.ecs.components :as c]
            [fantasia.sim.facets :as f]))

(defn clamp01 [x]
  (cond
    (< x 0.0) 0.0
    (> x 1.0) 1.0
    :else x))

(defn process-needs-decay
  "Decay warmth/food/sleep for all agents based on cold-snap."
  [ecs-world cold-snap]
  (let [agent-ids (be/get-all-entities-with-component ecs-world c/Needs)]
    (reduce (fn [acc agent-id]
              (let [needs (be/get-component acc agent-id c/Needs)
                    warmth (clamp01 (- (:warmth needs) (* 0.03 cold-snap)))
                    food (clamp01 (- (:food needs) 0.01))
                    sleep (clamp01 (- (:sleep needs) 0.008))]
                (be/add-component acc agent-id (c/->Needs warmth food sleep))))
            ecs-world
            agent-ids)))