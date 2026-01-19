(ns fantasia.sim.ecs.adapter-comprehensive-test
  (:require [brute.entity :as be]
            [fantasia.sim.ecs.core]
            [fantasia.sim.ecs.adapter]
            [clojure.pprint :as pp])
  (:import [java.util UUID]))

(println "\n=== Comprehensive Adapter Test ===")

(def ecs-world (be/create-system))

(println "\n1. Creating tiles...")
(def world1 (nth (fantasia.sim.ecs.core/create-tile ecs-world 0 0 :ground :plains nil nil) 2))
(def world2 (nth (fantasia.sim.ecs.core/create-tile world1 1 0 :ground :forest nil nil) 2))
(def world3 (nth (fantasia.sim.ecs.core/create-tile world2 0 1 :ground :mountains :mine nil) 2))
(def world4 (nth (fantasia.sim.ecs.core/create-tile world3 2 0 :ground :plains :warehouse nil) 2))

(println "4 tiles created")

(println "\n2. Creating agents...")
(def result1 (fantasia.sim.ecs.core/create-agent world4 1 0 0 :priest))
(def agent1-id (first result1))
(def world5 (second result1))

(def result2 (fantasia.sim.ecs.core/create-agent world5 2 1 0 :worker))
(def agent2-id (first result2))
(def world6 (second result2))

(println "2 agents created:" agent1-id agent2-id)

(println "\n3. Creating stockpiles...")
(def result3 (fantasia.sim.ecs.core/create-stockpile world6 "2,0"))
(def stock1-id (first result3))
(def world7 (second result3))

(println "1 stockpile created")

(println "\n4. Testing ecs->agent-map...")
(def agent1-map (fantasia.sim.ecs.adapter/ecs->agent-map world7 agent1-id))
(def agent2-map (fantasia.sim.ecs.adapter/ecs->agent-map world7 agent2-id))
(println "Agent1:" agent1-map)
(println "Agent2:" agent2-map)

(println "\n5. Testing ecs->agent-list...")
(def agents-list (fantasia.sim.ecs.adapter/ecs->agent-list world7))
(println "Agents list:" agents-list)

(println "\n6. Testing ecs->tiles-map...")
(def tiles-map (fantasia.sim.ecs.adapter/ecs->tiles-map world7))
(println "Tiles map:")
(pp/pprint tiles-map)

(println "\n7. Testing ecs->stockpiles-map...")
(def stockpiles-map (fantasia.sim.ecs.adapter/ecs->stockpiles-map world7))
(println "Stockpiles map:" stockpiles-map)

(println "\n8. Testing ecs->snapshot...")
(def global-state {:tick 1
                   :shrine nil
                   :levers {:cold-snap 0.5}
                   :map {:size 10}
                   :recent-events []
                   :attribution {}
                   :jobs {}
                   :items {}
                   :ledger {}})
(def snapshot (fantasia.sim.ecs.adapter/ecs->snapshot world7 global-state))
(println "Snapshot:")
(pp/pprint snapshot)

(println "\n=== All comprehensive tests passed! ===")
