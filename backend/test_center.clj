(ns test-center
  (:require [fantasia.sim.tick.initial :as initial]
            [fantasia.sim.hex :as hex]))

(defn test-center-calculation []
  (let [opts {:seed 42}
        world (initial/initial-world opts)
        {:keys [w h origin]} (:bounds (:map world))
        [oq or] (or origin [0 0])
        expected-center [(+ oq (quot w 2)) (+ or (quot h 2))]
        warehouse-tiles (filter #(= (:structure (val %)) :warehouse) (:tiles world))
        warehouse-key (first (keys warehouse-tiles))
        warehouse-pos (clojure.string/split warehouse-key #",")
        warehouse-q (Long/parseLong (first warehouse-pos))
        warehouse-r (Long/parseLong (second warehouse-pos))]
    (println "Map size: " w "x" h)
    (println "Origin: [" oq "," or "]")
    (println "Expected center:" expected-center)
    (println "Warehouse position:" [warehouse-q warehouse-r])
    (if (= [warehouse-q warehouse-r] expected-center)
      (println "✓ Warehouse is centered correctly!")
      (println "✗ Warehouse is NOT centered. Expected:" expected-center "but got:" [warehouse-q warehouse-r]))
    (println "\nWarehouse tile data:" (get warehouse-tiles warehouse-key))))

(test-center-calculation)
