(ns fantasia.test.ecs.simple
  (:require [brute.entity :as be]
            [fantasia.sim.ecs.core]
            [fantasia.sim.ecs.components :as c]))

(defn test-systems-simple
  []
  (println "=== ECS Systems Test Suite ===")
  (test-needs-decay)
  (test-movement)
  (println "=== All tests passed! ==="))
