(ns fantasia.sim.ecs.debug-filter-test
  (:require [brute.entity :as be]
            [fantasia.sim.ecs.core]
            [fantasia.sim.ecs.components :as c]))

(println "\n=== Debug Filter Test ===")

(def ecs-world (be/create-system))

(println "Creating tile at (0,0)...")
(def result (fantasia.sim.ecs.core/create-tile ecs-world 0 0 :ground :plains nil nil))
(def tile-key (first result))
(def tile-id (second result))
(def world' (nth result 2))

(println "Tile key:" tile-key)
(println "Tile key class:" (class tile-key))

(def tile-idx-instance (c/->TileIndex 0 0))
(def tile-idx-type (be/get-component-type tile-idx-instance))

(def idx (be/get-component world' tile-id tile-idx-type))
(println "TileIndex component:" idx)
(println "TileIndex q:" (:q idx))
(println "TileIndex r:" (:r idx))

(def compare1 (= [(:q idx) (:r idx)] tile-key))
(println "Compare [:q :r] == tile-key:" compare1)

(println "tile-key value:" (pr-str tile-key))
(println "tile-key = [0 0]?" (= tile-key [0 0]))

(println "\n=== Debug complete ===")
