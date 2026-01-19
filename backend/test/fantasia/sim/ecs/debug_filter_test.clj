(ns fantasia.sim.ecs.debug-filter-test
  (:require [brute.entity :as be]
            [fantasia.sim.ecs.core]
            [fantasia.sim.ecs.components :as c]))

(println "\n=== Debug Filter Test ===")

(def ecs-world (be/create-system))

(println "Creating tile at (0,0)...")
(def result (fantasia.sim.ecs.core/create-tile ecs-world 0 0 :ground :plains nil nil))
(def tile-key (nth result 0))
(def tile-id (nth result 1))
(def world' (nth result 2))

(println "Tile key:" tile-key)
(println "Tile key class:" (class tile-key))

(def tile-idx-instance (c/->TileIndex "0,0"))
(def tile-idx-type (be/get-component-type tile-idx-instance))

(def idx (be/get-component world' tile-id tile-idx-type))
(println "TileIndex component:" idx)
(println "TileIndex entity-id:" (:entity-id idx))
(println "TileIndex entity-id class:" (class (:entity-id idx)))

(def compare1 (= (:entity-id idx) tile-key))
(println "Compare (:entity-id idx) == tile-key:" compare1)

(def compare2 (= (:entity-id idx) "0,0"))
(println "Compare (:entity-id idx) == '0,0':" compare2)

(println "tile-key value:" (str tile-key))
(println "tile-key = '0,0'?" (= tile-key "0,0"))

(println "\n=== Debug complete ===")
