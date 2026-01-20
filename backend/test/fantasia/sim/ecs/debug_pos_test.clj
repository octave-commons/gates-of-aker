(ns fantasia.sim.ecs.debug-pos-test
  (:require [brute.entity :as be]
            [fantasia.sim.ecs.core]
            [fantasia.sim.ecs.components :as c]))

(println "\n=== Debug Position Test ===")

(def ecs-world (be/create-system))

(println "Creating tile at (0,0)...")
(def result (fantasia.sim.ecs.core/create-tile ecs-world 0 0 :ground :plains nil nil))
(def tile-key (first result))
(def tile-id (second result))
(def world' (nth result 2))

(println "Tile key:" tile-key)
(println "Tile ID:" tile-id)

(println "\nGetting all tiles with TileIndex...")
(def tile-idx-instance (c/->TileIndex 0 0))
(def tile-idx-type (be/get-component-type tile-idx-instance))
(println "TileIndex type:" tile-idx-type)

(def tile-entities (be/get-all-entities-with-component world' tile-idx-type))
(println "TileIndex entities:" tile-entities)

(println "\nFiltering for tile at (0,0)...")
(doseq [entity-id tile-entities]
  (let [idx (be/get-component world' entity-id tile-idx-type)]
    (println "Entity" entity-id "has TileIndex:" idx)))
(def filtered (filter #(let [idx (be/get-component world' % tile-idx-type)]
                           (= [(:q idx) (:r idx)] [0 0]))
                      tile-entities))
(println "Filtered entities:" filtered)

(println "\nUsing get-tile-at-pos...")
(def found (fantasia.sim.ecs.core/get-tile-at-pos world' [0 0]))
(println "Found tile:" found)

(println "\n=== Debug complete ===")
