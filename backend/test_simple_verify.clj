(ns test-simple
  (:require [fantasia.sim.ecs.core :as ecs]))
  
(println "Testing basic agent creation...")
(let [[eid _ world1] (ecs/create-agent (ecs/create-ecs-world) nil 0 0 :priest)]
  (println "✓ Created agent:" eid)
  (let [[eid2 _ world2] (ecs/create-agent world1 nil 1 0 :knight)]
    (println "✓ Created agent:" eid2)
    (let [[eid3 _ world3] (ecs/create-agent world2 nil 2 0 :peasant)]
      (println "✓ Created agent:" eid3))
    (println "✓ All basic tests passed!"))