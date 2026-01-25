(ns fantasia.sim.ecs-test
  (:require [brute.entity :as be]
            [fantasia.sim.ecs.core]
            [fantasia.sim.ecs.components :as c]))

(defn test-needs-decay
  []
  (println "Testing needs-decay system...")
(let [world (fantasia.sim.ecs.core/create-ecs-world)
         ecs-world (fantasia.sim.ecs.systems.needs-decay/process world 0.5)]
    (println "✓ Needs decay system processed")
     world))

(defn test-movement
  []
  (println "Testing movement system...")
  (let [world (fantasia.sim.ecs.core/create-ecs-world)
        ecs-world (fantasia.sim.ecs.systems.movement/process world)]
    (println "✓ Movement system processed")
    world))

(defn -main
  []
  (println "=== ECS Systems Test Suite ===")
  (test-needs-decay)
  (test-movement)
  (println "=== All tests passed! ==="))
