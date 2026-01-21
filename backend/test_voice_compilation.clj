(ns test-voice-compilation
  (:require [fantasia.sim.tick.initial :as initial]))

(println "Testing voice compilation...")
(let [agent1 (initial/->agent 1 0 0 :peasant)
      agent2 (initial/->agent 2 0 1 :knight)
      voice1 (:voice agent1)
      voice2 (:voice agent2)]
  (println "Agent 1 voice:" voice1)
  (println "Agent 2 voice:" voice2)
  (if (and voice1 voice2)
    (println "SUCCESS: Both agents have voices!")
    (println "FAILURE: Missing voices!"))
  (if (not= voice1 voice2)
    (println "SUCCESS: Voices are unique!")
    (println "WARNING: Voices might be same")))
