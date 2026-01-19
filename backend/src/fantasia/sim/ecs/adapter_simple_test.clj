(ns fantasia.sim.ecs.adapter-simple-test
  (:require [brute.entity :as be]
            [fantasia.sim.ecs.core]
            [fantasia.sim.ecs.adapter])
  (:import [java.util UUID]))

(println "\n=== Simple Adapter Test ===")

(def ecs-world (be/create-system))

(println "Creating agent...")
(def result (fantasia.sim.ecs.core/create-agent ecs-world (UUID/randomUUID) 0 0 :priest))
(def agent-id (first result))
(def world' (second result))

(println "Created agent ID:" agent-id)

(println "Testing get-comp helper...")
(def pos (fantasia.sim.ecs.adapter/get-comp world' agent-id :position))
(println "Position:" pos)

(def role (fantasia.sim.ecs.adapter/get-comp world' agent-id :role))
(println "Role:" role)

(def needs (fantasia.sim.ecs.adapter/get-comp world' agent-id :needs))
(println "Needs:" needs)

(println "Testing ecs->agent-map...")
(def agent-map (fantasia.sim.ecs.adapter/ecs->agent-map world' agent-id))
(println "Agent map:" agent-map)

(println "\n=== All tests passed! ===")
