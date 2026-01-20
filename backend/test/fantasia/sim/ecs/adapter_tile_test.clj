(ns fantasia.sim.ecs.adapter-tile-test
  (:require [brute.entity :as be]
            [fantasia.sim.ecs.core]
            [fantasia.sim.ecs.adapter]
            [clojure.pprint :as pp]))

(println "\n=== Tile Adapter Test ===")

(def ecs-world (be/create-system))

(println "Creating tiles...")
(def result1 (fantasia.sim.ecs.core/create-tile ecs-world 0 0 :ground :plains nil nil))
(def tile-key1 (nth result1 0))
(def tile-id1 (nth result1 1))
(def world1 (nth result1 2))

(def result2 (fantasia.sim.ecs.core/create-tile world1 1 0 :ground :forest nil nil))
(def tile-key2 (nth result2 0))
(def tile-id2 (nth result2 1))
(def world2 (nth result2 2))

(def result3 (fantasia.sim.ecs.core/create-tile world2 0 1 :ground :mountains :mine nil))
(def tile-key3 (nth result3 0))
(def tile-id3 (nth result3 1))
(def final-world (nth result3 2))

(println "Created tiles:" [tile-key1 tile-key2 tile-key3])

(println "\nTesting get-all-tiles...")
(def all-tiles (fantasia.sim.ecs.core/get-all-tiles final-world))
(println "Total tiles:" (count all-tiles))
(println "Tile IDs:" all-tiles)

(println "\nTesting get-tile-at-pos...")
(def tile-at-0-0 (fantasia.sim.ecs.core/get-tile-at-pos final-world [0 0]))
(println "Tile at (0,0):" tile-at-0-0)

(println "\nTesting ecs->tile-map...")
(def first-tile (first all-tiles))
(def tile-map (fantasia.sim.ecs.adapter/ecs->tile-map final-world first-tile))
(println "Tile map:" tile-map)

(println "\nTesting ecs->tiles-map...")
(def tiles-map (fantasia.sim.ecs.adapter/ecs->tiles-map final-world))
(println "Tiles map:")
(pp/pprint tiles-map)

(println "\n=== All tile tests passed! ===")
