(ns fantasia.sim.ecs.test
  (:require [brute.entity :as be]
            [fantasia.sim.ecs.core]))

(println "ECS test loaded!")
(def world (be/create-system))
(def result (fantasia.sim.ecs.core/create-agent world 1 0 0 :priest))
(def eid (first result))
(def world' (second result))
(println "Created agent:" eid)
(def agents (fantasia.sim.ecs.core/get-all-agents world'))
(println "Total agents:" (count agents))
(println "ECS test passed!")