(ns fantasia.sim.ecs.test2
  (:require [brute.entity :as be]
            [fantasia.sim.ecs.core]
            [fantasia.sim.ecs.components]))

(println "Test adapter loading!")

(def world (be/create-system))
(def result (fantasia.sim.ecs.core/create-agent world 1 0 0 :priest))
(def eid (first result))
(def world' (second result))
(println "Created agent:" eid)

(def agent-maps (fantasia.sim.ecs.adapter/ecs->agent-list world'))
(println "Agent count:" (count agent-maps))

(def first-agent (first agent-maps)
(println "First agent:" first-agent)