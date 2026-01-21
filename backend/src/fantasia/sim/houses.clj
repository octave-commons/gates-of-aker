(ns fantasia.sim.houses
  (:require [fantasia.sim.hex :as hex]))

(defn- get-house-tile
  "Get the house tile at a given position."
  [world pos]
  (let [tile-key (vector (first pos) (second pos))]
    (get-in world [:tiles tile-key])))

(defn- is-house?
  "Check if a position contains a house structure."
  [world pos]
  (let [tile (get-house-tile world pos)]
    (= :house (:structure tile))))

(defn- get-house-capacity
  "Get the bed capacity of a house (default 2)."
  [world pos]
  (let [tile (get-house-tile world pos)]
    (if (= :house (:structure tile))
      (get tile :bed-capacity 2)
      nil)))

(defn- get-occupied-beds
  "Get the number of occupied beds in a house."
  [world pos]
  (let [tile (get-house-tile world pos)]
    (if (= :house (:structure tile))
      (get tile :occupied-beds 0)
      0)))

(defn- get-house-residents
  "Get all agent IDs currently assigned to this house."
  [world pos]
  (let [tile (get-house-tile world pos)]
    (if (= :house (:structure tile))
      (get tile :residents [])
      [])))

(defn has-empty-bed?
   "Check if a house has at least one empty bed slot."
   [world pos]
   (let [capacity (get-house-capacity world pos)
         occupied (get-occupied-beds world pos)]
     (and capacity (< occupied capacity))))

(defn assign-agent-to-house
  "Assign an agent to a house, occupying a bed slot."
  [world agent-id pos]
  (let [tile-key (vector (first pos) (second pos))
        capacity (get-house-capacity world pos)
        occupied (get-occupied-beds world pos)]
    (if (and capacity (< occupied capacity))
      (let [residents (get-house-residents world pos)
            new-residents (conj residents agent-id)]
        (-> world
            (assoc-in [:tiles tile-key :occupied-beds] (inc occupied))
            (assoc-in [:tiles tile-key :residents] new-residents)))
      world)))

(defn find-house-with-empty-bed
  "Find any house with an available bed (returns nil if none found)."
  [world]
  (let [tiles (:tiles world)]
    (loop [tile-keys (keys tiles)]
      (if (empty? tile-keys)
        nil
        (let [tile-key (first tile-keys)
              tile (get tiles tile-key)
              pos tile-key]
          (if (and (= :house (:structure tile)) (has-empty-bed? world pos))
            pos
            (recur (rest tile-keys))))))))

(defn find-nearby-house-with-empty-bed
  "Find a house with an available bed near the agent's position (within radius)."
  [world agent-pos radius]
  (loop [r 0]
    (if (> r radius)
      nil
      (let [neighbors (hex/ring agent-pos r)]
        (reduce (fn [found pos]
                  (if (and (nil? found) (has-empty-bed? world pos))
                    (reduced pos)
                    found))
                nil
                neighbors)))))

