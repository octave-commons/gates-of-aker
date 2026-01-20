(ns run-eat-test
  (:require [fantasia.sim.tick.core :as tick]
            [fantasia.sim.core :as core]
            [fantasia.sim.jobs :as jobs]))

(reset! fantasia.sim.tick.core/*state (core/initial-world 20))
(println "=== Starting simulation ===")
(println "Initial state:")
(doseq [agent (:agents (tick/get-state))]
  (println "  Agent" (:id agent) "at" (:pos agent) "food:" (get-in agent [:needs :food])))

(dotimes [i 50]
  (println "\n=== Tick" (inc i) "===")
  (let [result (tick/tick! 1)
        world (tick/get-state)]
    (doseq [agent (:agents world)]
      (let [food (get-in agent [:needs :food])]
        (when (< food 0.31)
          (println "  Agent" (:id agent) "at" (:pos agent) "food:" food "hungry:" (< food 0.3)))))))

(println "\n=== Final state ===")
(println "Tick:" (:tick (tick/get-state)))
(println "Agents:")
(doseq [agent (:agents (tick/get-state))]
  (println "  Agent" (:id agent) "at" (:pos agent) "food:" (get-in agent [:needs :food])))
(println "Stockpiles:")
(doseq [[k sp] (:stockpiles (tick/get-state))]
  (println "  " k "resource:" (:resource sp) "qty:" (:current-qty sp)))
(println "Jobs count:" (count (:jobs (tick/get-state))))
(shutdown-agents)
