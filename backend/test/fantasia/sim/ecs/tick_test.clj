(ns fantasia.sim.ecs.tick-test
  (:require [brute.entity :as be]
            [fantasia.sim.ecs.core]
            [fantasia.sim.ecs.tick])
  (:import [java.util UUID]))

(println "\n=== ECS Tick Module Test ===")

(println "\n1. Creating initial ECS world...")
(def initial-state (fantasia.sim.ecs.tick/create-ecs-initial-world {:seed 1}))
(println "Initial state:" (keys initial-state))

(println "\n2. Creating test agent...")
(def ecs-world (fantasia.sim.ecs.tick/get-ecs-world))
(def agent-result (fantasia.sim.ecs.core/create-agent ecs-world 1 0 0 :priest))
(def agent-id (first agent-result))
(def world1 (second agent-result))
(clojure.core/reset! fantasia.sim.ecs.tick/*ecs-world world1)
(println "Created agent:" agent-id)

(println "\n3. Running first ECS tick...")
(def snapshot1 (fantasia.sim.ecs.tick/tick-ecs-once @fantasia.sim.ecs.tick/*global-state))
(println "Snapshot tick:" (:tick snapshot1))
(println "Agents count:" (count (:agents snapshot1)))

(println "\n4. Running second ECS tick (needs should decay)...")
(def snapshot2 (fantasia.sim.ecs.tick/tick-ecs-once @fantasia.sim.ecs.tick/*global-state))
(println "Snapshot tick:" (:tick snapshot2))
(def agent1 (first (:agents snapshot2)))
(println "Agent needs after 2 ticks:" (:needs agent1))

(println "\n5. Testing import-agent...")
(def old-agent {:id (UUID/randomUUID)
                :pos [2 0]
                :role :worker
                :needs {:warmth 0.8 :food 0.5 :sleep 0.9}
                :inventory {:wood 5 :food 3}
                :frontier {:fire 0.5}
                :recall {:winter 0.3}})
(def import-result (fantasia.sim.ecs.tick/import-agent (fantasia.sim.ecs.tick/get-ecs-world) old-agent))
(println "Imported agent ID:" (first import-result))

(println "\n6. Testing import-tile...")
(def ecs-world' (fantasia.sim.ecs.tick/get-ecs-world))
(def ecs-world'' (fantasia.sim.ecs.tick/import-tile ecs-world' "3,0" {:terrain :ground :biome :plains :structure nil :resource nil}))
(println "Tile imported")

(println "\n7. Running tick-ecs! (3 ticks)...")
(def outputs (fantasia.sim.ecs.tick/tick-ecs! 3))
(println "Ran" (count outputs) "ticks")
(println "Final tick:" (:tick (last outputs)))

(println "\n=== All ECS tick tests passed! ===")
