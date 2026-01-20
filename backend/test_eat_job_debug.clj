(require '[fantasia.sim.tick.core :as tick]
         '[fantasia.sim.core :as core]
         '[fantasia.sim.jobs :as jobs])

(reset! fantasia.sim.tick.core/*state (core/initial-world 20))
(println "Starting 20 tick simulation...")
(tick/tick! 20)
(def final-world (tick/get-state))
(println "=== FINAL STATE ===")
(println "Tick:" (:tick final-world))
(println "\n=== AGENTS ===")
(doseq [agent (:agents final-world)]
  (println (str "Agent " (:id agent) " at " (:pos agent) " food:" (get-in agent [:needs :food]) " sleep:" (get-in agent [:needs :sleep]))))
(println "\n=== JOBS ===")
(doseq [job (:jobs final-world)]
  (println (str "Job: " (:type job) " target:" (:target job) " worker:" (:worker-id job))))
(println "\n=== STOCKPILES ===")
(doseq [[k sp] (:stockpiles final-world)]
  (println (str k " resource:" (:resource sp) " qty:" (:current-qty sp))))
(println "\nTotal jobs:" (count (:jobs final-world)))
(shutdown-agents)
