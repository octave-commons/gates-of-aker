(ns fantasia.sim.ecs.spatial
  "ECS spatial utilities for tile and world queries."
  (:require [brute.entity :as be]
            [fantasia.sim.ecs.components :as c]
            [fantasia.sim.hex :as hex]))

(def default-world-width 40)
(def default-world-height 40)

(defn get-world-dimensions
  "Get world dimensions from global state or defaults."
  [global-state]
  {:width (or (:width global-state) default-world-width)
   :height (or (:height global-state) default-world-height)})

(defn in-bounds?
  "Check if position is within world bounds."
  [ecs-world pos [width height]]
  (let [[q r] pos]
    (and (>= q 0)
         (< q width)
         (>= r 0)
         (< r height))))

(defn get-tile-component
  "Get Tile component at position."
  [ecs-world pos]
  (let [[q r] pos
        tile-index-type (be/get-component-type (c/->TileIndex q r))
        tile-type (be/get-component-type (c/->Tile :ground :plains nil nil))
        tiles-with-index (be/get-all-entities-with-component ecs-world tile-index-type)
        matching-tiles (filter #(= pos (let [idx (be/get-component ecs-world % tile-index-type)]
                                          [(:q idx) (:r idx)]))
                             tiles-with-index)]
    (when-let [tile-id (first matching-tiles)]
      (be/get-component ecs-world tile-id tile-type))))

(defn passable?
  "Check if position is passable (walkable).
   Tiles are passable unless they are structures that block movement."
  [ecs-world pos]
  (if-let [tile (get-tile-component ecs-world pos)]
    (let [structure (:structure tile)]
      (not (#{:wall :mountain} structure)))
    true))

(defn get-tiles-in-radius
  "Get all tiles within radius of position."
  [ecs-world pos radius]
  (loop [tiles #{}
         dist 0]
    (if (> dist radius)
      tiles
      (recur (into tiles (hex/ring pos dist))
             (inc dist)))))

(defn get-neighboring-tiles
  "Get tiles adjacent to position (hexagonal neighbors)."
  [ecs-world pos]
  (let [neighbors (hex/neighbors pos)]
    (filter #(get-tile-component ecs-world %) neighbors)))

(defn tile-has-structure?
  "Check if tile at position has a specific structure."
  [ecs-world pos structure-type]
  (if-let [tile (get-tile-component ecs-world pos)]
    (= (:structure tile) structure-type)
    false))

(defn get-structures-in-radius
  "Get all positions with structures within radius."
  [ecs-world pos radius structure-type]
  (let [tiles (get-tiles-in-radius ecs-world pos radius)]
    (filter #(tile-has-structure? ecs-world % structure-type) tiles)))

(defn get-structures-of-type
  "Get all positions with specific structure type in world."
  [ecs-world structure-type]
  (let [tile-type (be/get-component-type (c/->Tile :ground :plains nil nil))
        all-tiles (be/get-all-entities-with-component ecs-world tile-type)]
    (reduce (fn [acc tile-id]
              (if-let [tile (be/get-component ecs-world tile-id tile-type)]
                (if (= (:structure tile) structure-type)
                  (conj acc tile-id)
                  acc)
                acc))
            []
            all-tiles)))

(defn get-nearest-structure
  "Find nearest structure of type to position."
  [ecs-world pos structure-type]
  (let [structures (get-structures-of-type ecs-world structure-type)
        tile-type (be/get-component-type (c/->Tile :ground :plains nil nil))
        tile-index-type (be/get-component-type (c/->TileIndex 0 0))]
    (when (seq structures)
      (reduce (fn [[nearest dist] tile-id]
                (if-let [tile (be/get-component ecs-world tile-id tile-type)]
                  (let [tile-pos [(:q (be/get-component ecs-world tile-id tile-index-type))
                                  (:r (be/get-component ecs-world tile-id tile-index-type))]
                        tile-dist (hex/distance pos tile-pos)]
                    (if (< tile-dist dist)
                      [tile-id tile-dist]
                      [nearest dist]))
                  [nearest dist]))
              [nil Integer/MAX_VALUE]
              structures))))
