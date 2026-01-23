(ns fantasia.sim.memories
  (:require [fantasia.sim.hex :as hex]
            [fantasia.dev.logging :as log]))

(defn create-memory!
  "Create a new memory facet from an event."
  [world type location strength entity-id facets]
  (let [memory-id (random-uuid)
        memory {:id memory-id
                :type type
                :location location
                :created-at (:tick world)
                :strength strength
                :decay-rate 0.001
                :entity-id entity-id
                :facets facets}]
    (log/log-info "[MEMORY:CREATE]"
                 {:type type
                  :location location
                  :strength strength
                  :entity-id entity-id
                  :facet-count (count facets)})
    (assoc-in world [:memories memory-id] memory)))

(defn decay-memories!
   "Apply time-based decay to all memory facets."
   [world]
   (let [tick (:tick world)
         decayed-count (->> (:memories world)
                           vals
                           (filter #(< (:strength %) 1.0))
                           count)]
     (when (> decayed-count 0)
       (log/log-debug "[MEMORY:DECAY]"
                     {:decay-count decayed-count}))
     (reduce-kv
       (fn [w' id memory]
         (let [new-strength (max 0.0 (- (:strength memory) (:decay-rate memory)))]
           (assoc-in w' [:memories id] (assoc memory :strength new-strength))))
       world
       (:memories world))))

(defn get-memories-in-range
  "Return all memories within distance bounds, filtered by strength > threshold."
  [world pos max-distance min-strength]
  (->> (:memories world)
        vals
        (filter #(> (:strength %) min-strength))
        (filter #(<= (hex/distance (:location %) pos) max-distance))))

(defn remove-memory!
  "Remove a memory by ID."
  [world memory-id]
  (do
    (log/log-info "[MEMORY:REMOVE]"
                 {:memory-id memory-id})
    (update world :memories dissoc memory-id)))

(defn clean-expired-memories!
  "Remove all memories with strength below threshold."
  [world strength-threshold]
  (let [expired-memories (->> (:memories world)
                                  vals
                                  (filter #(< (:strength %) strength-threshold))
                                  (map :id))
        removed-count (count expired-memories)]
    (when (> removed-count 0)
      (log/log-debug "[MEMORY:CLEAN]"
                   {:removed-count removed-count
                    :threshold strength-threshold}))
    (reduce (fn [w' mem-id]
                (update w' :memories dissoc mem-id))
            world
            expired-memories)))
