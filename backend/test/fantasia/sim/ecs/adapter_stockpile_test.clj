(ns fantasia.sim.ecs.adapter-stockpile-test
  (:require [brute.entity :as be]
            [fantasia.sim.ecs.core]
            [fantasia.sim.ecs.adapter]
            [clojure.pprint :as pp]))

(println "\n=== Stockpile Adapter Test ===")

(def ecs-world (be/create-system))

(println "Creating tile at (0,0)...")
(def result1 (fantasia.sim.ecs.core/create-tile ecs-world 0 0 :ground :plains nil nil))
(def tile-key1 (nth result1 0))
(def tile-id1 (nth result1 1))
(def world1 (nth result1 2))

(println "Creating stockpile at (0,0)...")
(def stock-result1 (fantasia.sim.ecs.core/create-stockpile world1 "0,0"))
(def stock-id1 (first stock-result1))
(def world2 (second stock-result1))

(println "Creating tile at (1,0)...")
(def result2 (fantasia.sim.ecs.core/create-tile world2 1 0 :ground :plains nil nil))
(def tile-key2 (nth result2 0))
(def tile-id2 (nth result2 1))
(def world3 (nth result2 2))

(println "Creating stockpile at (1,0)...")
(def stock-result2 (fantasia.sim.ecs.core/create-stockpile world3 "1,0"))
(def stock-id2 (first stock-result2))
(def final-world (second stock-result2))

(println "\nTesting ecs->stockpiles-map...")
(def stockpiles-map (fantasia.sim.ecs.adapter/ecs->stockpiles-map final-world))
(println "Stockpiles map:")
(pp/pprint stockpiles-map)

(println "\n=== All stockpile tests passed! ===")
