#!/usr/bin/env bb

(require '[babashka.fs :as fs])

(defn run-sim []
  (spit "test_sim_output.txt"
        (-> (sh "cd" "/home/err/devel/orgs/octave-commons/gates-of-aker/backend"
                 "timeout" "10" "clojure" "-M:server" "-e"
                 "(require '[fantasia.sim.tick.core :as tick] '[fantasia.sim.core :as core] '[fantasia.sim.jobs :as jobs]) (reset! fantasia.sim.tick.core/*state (core/initial-world 20)) (tick/tick! 20) (def final-world (tick/get-state)) (println \"=== AGENTS ===\") (doseq [agent (:agents final-world)] (println \"Agent\" (:id agent) \"food:\" (get-in agent [:needs :food]) \"sleep:\" (get-in agent [:needs :sleep]))) (println \"=== JOBS ===\") (doseq [job (:jobs final-world)] (println \"Job:\" (:type job) \"target:\" (:target job) \"worker:\" (:worker-id job))) (println \"=== STOCKPILES ===\") (doseq [[k sp] (:stockpiles final-world)] (println k \"resource:\" (:resource sp) \"qty:\" (:current-qty sp)))")
             :out :capture
             :err :out)
        :out))

(defn main []
  (println "Running 20 tick simulation...")
  (let [output (run-sim)]
    (println output)
    (spit "test_sim_output.txt" output)))

(main)
