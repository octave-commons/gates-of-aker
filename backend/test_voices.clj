(ns test-voices
  (:require [fantasia.sim.tick.core :as core])
  (:require [fantasia.sim.tick.initial :as initial]))

(println "Testing voice generation...")
(let [agent1 (initial/->agent 1 0 0 :peasant)
      agent2 (initial/->agent 2 0 1 :peasant)
      agent3 (initial/->agent 3 0 2 :knight)]
  (println "Agent 1 voice:" (agent1 :voice))
  (println "Agent 2 voice:" (agent2 :voice))
  (println "Agent 3 voice:" (agent3 :voice))
  (when (not= (agent1 :voice) (agent2 :voice))
    (println "SUCCESS: Each agent has unique voice!")))
