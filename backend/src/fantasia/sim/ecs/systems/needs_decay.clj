(ns fantasia.sim.ecs.systems.needs-decay
  (:require [brute.entity :as be]
            [fantasia.sim.ecs.components :as c]
            [fantasia.sim.constants :as const]
            [fantasia.dev.logging :as log]))

(defn clamp01 [x]
  (cond
    (< x 0.0) 0.0
    (> x 1.0) 1.0
    :else x))

(defn process
  "Decay warmth/food/sleep for all agents based on cold-snap."
  [ecs-world cold-snap]
  (let [needs-instance (c/->Needs 0.6 0.7 0.7 1.0 0.8 0.6 0.5 0.5 0.5 0.6 0.5 0.5 0.5)
        needs-type (be/get-component-type needs-instance)
        agent-ids (be/get-all-entities-with-component ecs-world needs-type)
        cold-snap (or cold-snap 0.5)]
    (reduce (fn [acc agent-id]
              (let [needs (or (be/get-component acc agent-id needs-type)
                              (c/->Needs 0.6 0.7 0.7 1.0 0.8 0.6 0.5 0.5 0.5 0.6 0.5 0.5 0.5))
                    warmth-val (double (or (:warmth needs) 0.6))
                    food-val (double (or (:food needs) 0.7))
                    sleep-val (double (or (:sleep needs) 0.7))
                    warmth (clamp01 (- warmth-val (+ 0.004 (* 0.012 cold-snap))))
                    food (clamp01 (- food-val 0.002))
                    sleep (clamp01 (- sleep-val 0.008))]
                ;; Log need triggers when they cross critical thresholds
                (when (< food 0.3)
                  (log/log-info "[JOB:NEED-TRIGGER]" {:agent-id agent-id :need-type :food :value food :threshold 0.3}))
                (when (< sleep 0.3)
                  (log/log-info "[JOB:NEED-TRIGGER]" {:agent-id agent-id :need-type :sleep :value sleep :threshold 0.3}))
                (when (< warmth 0.3)
                  (log/log-info "[JOB:NEED-TRIGGER]" {:agent-id agent-id :need-type :warmth :value warmth :threshold 0.3}))
                (be/add-component acc agent-id (c/->Needs warmth food sleep 1.0 0.8 0.6 0.5 0.5 0.5 0.6 0.5 0.5 0.5))))
            ecs-world
            agent-ids)))
