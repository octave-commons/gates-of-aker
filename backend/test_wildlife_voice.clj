(ns test-wildlife-voice
  (:require [fantasia.sim.tick.initial :as initial]))

(println "Testing wildlife agent with voice...")
(let [agent (initial/->wildlife-agent {:agents []} [0 0] :wolf)]
  (println "Wildlife agent:" agent)
  (if (:voice agent)
    (println "SUCCESS: Wildlife agent has voice!")
    (println "FAILURE: Wildlife agent missing voice!")))
