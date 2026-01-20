(ns check-biomes
  (:require [fantasia.sim.tick.initial :as initial]))

(def world (initial/initial-world {:seed 1}))

(println "=== World Structure ===")
(println "Total tiles:" (count (:tiles world)))
(println "Map bounds:" (:bounds (:map world)))

(println "\n=== Sample Tiles with Biomes ===")
(doseq [[k tile] (take 20 (:tiles world))]
  (println (str "Tile " k ": biome=" (:biome tile) ", structure=" (:structure tile))))

(println "\n=== Biome Distribution ===")
(def biome-count (frequencies (map #(-> % second :biome) (:tiles world))))
(doseq [[biome count] (sort-by val > biome-count)]
  (println (str biome ": " count)))

(println "\n=== Looking for warehouse ===")
(def warehouse-tile (first (filter #(= (:structure (val %)) :warehouse) (:tiles world))))
(if warehouse-tile
  (let [[k v] warehouse-tile]
    (println "Warehouse found at:" k)
    (println "Warehouse data:" v))
  (println "No warehouse found!"))

(println "\n=== Campfire position ===")
(println "Campfire:" (:campfire world))
(println "Shrine:" (:shrine world))

(System/exit 0)
