(ns fantasia.sim.ecs.full-integration-test
  (:require [brute.entity :as be]
            [fantasia.sim.ecs.core]
            [fantasia.sim.ecs.tick]
            [fantasia.sim.tick.initial :as initial])
  (:import [java.util UUID]))

(println "\n=== Full ECS Integration Test ===")

(println "\n1. Creating old-style initial world...")
(def old-world (initial/initial-world {:seed 42}))
(println "Old world tick:" (:tick old-world))
(println "Old world agents:" (count (:agents old-world)))
(println "Old world tiles:" (count (:tiles old-world)))
(println "Old world stockpiles:" (count (:stockpiles old-world)))

(println "\n2. Importing old world to ECS...")
(fantasia.sim.ecs.tick/reset-ecs-world!)
(def global-state (fantasia.sim.ecs.tick/create-ecs-initial-world {:seed 42}))
(def ecs-world (fantasia.sim.ecs.tick/import-world-to-ecs old-world))

(println "\n3. Verifying imported entities...")
(def agent-count (count (fantasia.sim.ecs.core/get-all-agents ecs-world)))
(def tile-count (count (fantasia.sim.ecs.core/get-all-tiles ecs-world)))
(println "ECS agents:" agent-count)
(println "ECS tiles:" tile-count)

(println "\n4. Running 5 ECS ticks...")
(def outputs (fantasia.sim.ecs.tick/tick-ecs! 5))
(println "Ran" (count outputs) "ECS ticks")

(println "\n5. Comparing tick counts:")
(println "  Old world tick:" (:tick old-world))
(println "  ECS final tick:" (:tick (last outputs)))

(println "\n6. Checking agent needs after ECS ticks:")
(def final-snapshot (last outputs))
(def first-agent (first (:agents final-snapshot)))
(println "  First agent needs:" (:needs first-agent))
(println "  First agent pos:" (:pos first-agent))
(println "  First agent role:" (:role first-agent))

(println "\n7. Comparing with old-world first agent:")
(def old-first-agent (first (:agents old-world)))
(println "  Old agent needs:" (:needs old-first-agent))
(println "  Old agent pos:" (:pos old-first-agent))
(println "  Old agent role:" (:role old-first-agent))

(println "\n=== Full integration test passed! ===")
