(ns fantasia.sim.ecs.adapter-test
  (:require [brute.entity :as be]
            [fantasia.sim.ecs.core]
            [fantasia.sim.ecs.adapter])
  (:import [java.util UUID]))

(println "Adapter test loading!")

(def ecs-world (be/create-system))

(defn run-test []
  (println "\n=== Testing Adapter ===")
  
  (println "\n1. Creating test agent...")
  (let [agent-result (fantasia.sim.ecs.core/create-agent ecs-world 1 0 0 :priest)
        agent-id (first agent-result)
        world' (second result)]
    (println "Created agent:" agent-id)
    
    (println "\n2. Converting agent to map...")
    (let [agent-map (fantasia.sim.ecs.adapter/ecs->agent-map world' agent-id)]
      (println "Agent map:" agent-map)))
  
  (println "\nAdapter test complete!"))

(def result (fantasia.sim.ecs.core/create-agent ecs-world (UUID/randomUUID) 0 0 :priest))
(def agent-id (first result))
(def world' (second result))

(println "\nCreated agent:" agent-id)

(println "\nConverting agent to map...")
(def agent-map (fantasia.sim.ecs.adapter/ecs->agent-map world' agent-id))
(println "Agent map:" agent-map)

(println "\nAdapter test passed!")
