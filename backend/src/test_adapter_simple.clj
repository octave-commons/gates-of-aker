(ns test-adapter-simple
  (:require [brute.entity :as be]
            [fantasia.sim.ecs.core]
            [fantasia.sim.ecs.components]))

(println "All namespaces loaded!")

(defn test-convert [world eid]
  (let [position (be/get-component world eid fantasia.sim.ecs.components/->Position 0 0)
        role (be/get-component world eid fantasia.sim.ecs.components/->Role :priest))]
    {:pos [(:q position) (:r position)] :role (:type role)}))

(def world (be/create-system))
(def result (fantasia.sim.ecs.core/create-agent world 1 0 0 :priest))
(def eid (first result))
(def world' (second result))
(println "Test result:" (test-convert world' eid))