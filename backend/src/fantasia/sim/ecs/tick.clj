(ns fantasia.sim.ecs.tick
(:require [brute.entity :as be]
              [fantasia.sim.ecs.core]
              [fantasia.sim.ecs.components :as c]
            [fantasia.sim.ecs.systems.needs-decay]
              ;; [fantasia.sim.ecs.systems.job-creation]
              [fantasia.sim.ecs.systems.movement]
              [fantasia.sim.ecs.systems.job-assignment]
              [fantasia.sim.ecs.systems.job-processing]
              [fantasia.sim.ecs.systems.agent-interaction]
             [fantasia.sim.ecs.adapter]
             [fantasia.sim.time :as time]
             [fantasia.sim.biomes :as biomes]))

(def ^:dynamic *ecs-world (atom (fantasia.sim.ecs.core/create-ecs-world)))
(def ^:dynamic *global-state (atom {}))

(defn get-ecs-world []
  @*ecs-world)

(defn reset-ecs-world! []
  (clojure.core/reset! *ecs-world (fantasia.sim.ecs.core/create-ecs-world)))

(defn run-systems [ecs-world global-state]
  "Run all ECS systems in sequence.
   Returns updated ECS world."
  (let [levers (:levers global-state {})
        cold-snap (or (:cold-snap levers) 0.4)]
    (-> ecs-world
        (fantasia.sim.ecs.systems.needs-decay/process cold-snap)
        ;; (fantasia.sim.ecs.systems.needs/process global-state)
        ;; (fantasia.sim.ecs.systems.job-creation/process global-state)
        ;; Temporarily disable assignment/processing until stable
        ;; (fantasia.sim.ecs.systems.job_assignment/process global-state)
        ;; (fantasia.sim.ecs.systems.job_processing/process global-state)
        (fantasia.sim.ecs.systems.movement/process)
        ;; (fantasia.sim.ecs.systems.agent-interaction/process)
        )))

(defn tick-ecs-once [global-state]
  "Run one ECS tick with all systems."
  (let [ecs-world (get-ecs-world)
        ecs-world' (run-systems ecs-world global-state)
        new-tick (inc (:tick global-state))
        seed (:seed global-state)
        global-state' (-> global-state
                           (assoc :tick new-tick)
                           (assoc :temperature (time/temperature-at seed new-tick))
                           (assoc :daylight (time/daylight-at seed new-tick)))
        snapshot (fantasia.sim.ecs.adapter/ecs->snapshot ecs-world' global-state')]
    (clojure.core/reset! *ecs-world ecs-world')
    (clojure.core/reset! *global-state global-state')
    snapshot))

(defn tick-ecs! [n]
  "Run N ECS ticks."
  (loop [i 0
         outs []]
    (if (>= i n)
      (reverse outs)
      (let [snapshot (tick-ecs-once @*global-state)
            outs' (conj outs snapshot)]
        (recur (inc i) outs')))))

(defn spawn-initial-agents! [ecs-world bounds]
  "Spawn initial agents near map center."
  (let [center-q (if (= (:shape bounds) :rect)
                   (+ (:origin-q bounds 0) (quot (:w bounds) 2))
                   (:origin-q bounds 0))
        center-r (if (= (:shape bounds) :rect)
                   (+ (:origin-r bounds 0) (quot (:h bounds) 2))
                   (:origin-r bounds 0))
        spawn-radius 10
        rng #(rand-int spawn-radius)]
    (println "[ECS] Spawning initial agents near center:" center-q center-r)
    (let [[_ ecs-world1] (fantasia.sim.ecs.core/create-agent ecs-world nil center-q center-r :priest)
          [_ ecs-world2] (fantasia.sim.ecs.core/create-agent ecs-world1 nil (+ center-q (rng)) (+ center-r (rng)) :knight)
          [_ ecs-world3] (fantasia.sim.ecs.core/create-agent ecs-world2 nil (+ center-q (rng)) (+ center-r (rng)) :peasant)
          [_ ecs-world4] (fantasia.sim.ecs.core/create-agent ecs-world3 nil (+ center-q (rng)) (+ center-r (rng)) :peasant)
          [_ ecs-world5] (fantasia.sim.ecs.core/create-agent ecs-world4 nil (+ center-q (rng)) (+ center-r (rng)) :peasant)]
      (println "[ECS] Spawned 5 initial agents")
       ecs-world5)))

(defn spawn-initial-buildings! [ecs-world bounds]
  "Spawn initial buildings with job queues near map center."
  (let [center-q (if (= (:shape bounds) :rect)
                   (+ (:origin-q bounds 0) (quot (:w bounds) 2))
                   (:origin-q bounds 0))
        center-r (if (= (:shape bounds) :rect)
                   (+ (:origin-r bounds 0) (quot (:h bounds) 2))
                   (:origin-r bounds 0))
        ;; Place buildings slightly offset from center
        campfire-pos [(- center-q 2) center-r]
        stockpile-pos [(+ center-q 2) center-r]]
    (println "[ECS] Spawning initial buildings")
    (let [[_ ecs-world1] (fantasia.sim.ecs.core/create-building ecs-world campfire-pos :campfire)
          [_ ecs-world2] (fantasia.sim.ecs.core/create-building ecs-world1 stockpile-pos :stockpile)]
      (println "[ECS] Spawned 2 initial buildings")
      ecs-world2)))

(defn create-ecs-initial-world [opts]
  "Create initial ECS world from scratch."
  (reset-ecs-world!)
  (let [seed (or (:seed opts) 1)
        bounds (or (:bounds opts) {:shape :rect :w 128 :h 128})
        tree-density (or (:tree-density opts) (:tree_density opts) 0.05)
        ecs-world (get-ecs-world)
        ecs-world-with-agents (spawn-initial-agents! ecs-world bounds)
        ecs-world-with-buildings (spawn-initial-buildings! ecs-world-with-agents bounds)
        global-state {:seed seed
                    :tree-density tree-density
                    :bounds bounds
                    :tick 0
                    :temperature (time/temperature-at seed 0)
                    :daylight (time/daylight-at seed 0)
                    :map {:kind :hex :layout :pointy :bounds bounds}
                    :shrine nil
                    :levers {:cold-snap 0.4 :iconography {:fire->patron 0.80}}
                    :jobs {}
                    :items {}
                    :stockpiles {}
                    :ledger {}
                    :recent-events []
                    :recent-max 30
                    :traces []
                    :trace-max 250
                    :tile-visibility {}
                    :calendar {:day 1 :season :spring :year 1 :tick 0 :day-progress 0.0 :hour 0.0 :time-of-day :night :temperature (time/temperature-at seed 0) :daylight (time/daylight-at seed 0) :cold-snap 0.4}
                    :institutions {:temple {:id :temple
                                          :broadcast-every 6
                                          :canonical {:facets [:fire :judgment :winter]}}}}]
    (clojure.core/reset! *ecs-world ecs-world-with-buildings)
    (clojure.core/reset! *global-state global-state)
    (println "[ECS] Created initial world with bounds:" bounds "tree-density:" tree-density)
    global-state))

(defn import-tile [ecs-world tile-key tile-data]
   "Import old-style tile map into ECS."
   (let [tile-key' (if (keyword? tile-key) (name tile-key) tile-key)]
     (cond
      ;; Handle vector keys directly
      (and tile-key' (sequential? tile-key') (= 2 (count tile-key')))
      (try
        (let [q (first tile-key')
              r (second tile-key')
              terrain (or (:terrain tile-data) :ground)
              biome (or (:biome tile-data) :plains)
              structure (:structure tile-data)
              resource (:resource tile-data)]
          (let [[_ _ world'] (fantasia.sim.ecs.core/create-tile ecs-world q r terrain biome structure resource)]
            world'))
        (catch Exception e
          (println "[ECS] Warning: Failed to import tile at" tile-key ":" (.getMessage e))
          ecs-world))
      ;; Handle string keys (backward compatibility)
      (and tile-key' (string? tile-key') (clojure.string/includes? tile-key' ","))
      (try
        (let [parts (clojure.string/split tile-key' #",")
              q (Integer/parseInt (first parts))
              r (Integer/parseInt (second parts))
              terrain (or (:terrain tile-data) :ground)
              biome (or (:biome tile-data) :plains)
              structure (:structure tile-data)
              resource (:resource tile-data)]
          (let [[_ _ world'] (fantasia.sim.ecs.core/create-tile ecs-world q r terrain biome structure resource)]
            world'))
        (catch Exception e
          (println "[ECS] Warning: Failed to import tile at" tile-key ":" (.getMessage e))
          ecs-world))
      :else
      ecs-world)))

;; Legacy compatibility functions for fantasia.sim.core
(defn get-state []
  "Get the current global state."
  @*global-state)

(defn reset-world! 
  ([] (reset-world! {}))
  ([opts]
   "Reset both ECS world and global state with optional parameters."
   (let [seed (or (:seed opts) 1)
         tree-density (or (:tree_density opts) (:tree-density opts) 0.05)
         bounds (or (:bounds opts) {:shape :rect :w 128 :h 128})
         world-opts (assoc opts :seed seed :tree-density tree-density :bounds bounds)]
     (println "[ECS] Resetting world with seed:" seed "tree-density:" tree-density "bounds:" bounds)
     (reset-ecs-world!)
     (let [global-state (create-ecs-initial-world world-opts)]
       ;; Apply biome generation to create tiles
       (let [world-with-biomes (fantasia.sim.biomes/generate-biomes! global-state)
             world-with-resources (fantasia.sim.biomes/spawn-biome-resources! world-with-biomes)]
         (clojure.core/reset! *global-state world-with-resources)
         world-with-resources)))))

(defn tick! [n]
  "Run N ECS ticks - alias for tick-ecs!."
  (tick-ecs! n))

;; Stub functions for legacy compatibility - these need to be implemented
(defn set-levers! [levers]
  (println "[ECS] Warning: set-levers! not implemented yet")
  (swap! *global-state assoc :levers levers))

(defn set-facet-limit! [limit]
  (println "[ECS] Setting facet limit to" limit)
  (swap! *global-state assoc :facet-limit limit)
  limit)

(defn set-vision-radius! [radius]
  (println "[ECS] Setting vision radius to" radius)
  (swap! *global-state assoc :vision-radius radius)
  radius)

(defn place-shrine! [q r]
  (println "[ECS] Warning: place-shrine! not implemented yet")
  nil)

(defn appoint-mouthpiece! [agent-id]
  (println "[ECS] Warning: appoint-mouthpiece! not implemented yet")
  nil)

(defn place-wall-ghost! [q r]
  (println "[ECS] Warning: place-wall-ghost! not implemented yet")
  nil)

(defn place-stockpile! [q r]
  (println "[ECS] Warning: place-stockpile! not implemented yet")
  nil)

(defn place-warehouse! [q r]
  (println "[ECS] Warning: place-warehouse! not implemented yet")
  nil)

(defn place-campfire! [q r]
  (println "[ECS] Warning: place-campfire! not implemented yet")
  nil)

(defn place-statue-dog! [q r]
  (println "[ECS] Warning: place-statue-dog! not implemented yet")
  nil)

(defn place-tree! [q r]
  (println "[ECS] Warning: place-tree! not implemented yet")
  nil)

(defn place-deer! [q r]
  (println "[ECS] Warning: place-deer! not implemented yet")
  nil)

(defn place-wolf! [q r]
  (println "[ECS] Warning: place-wolf! not implemented yet")
  nil)

(defn place-bear! [q r]
  (println "[ECS] Warning: place-bear! not implemented yet")
  nil)

(defn queue-build-job! [job-data]
  (println "[ECS] Warning: queue-build-job! not implemented yet")
  nil)

(defn get-agent-path! [agent-id]
  (println "[ECS] Warning: get-agent-path! not implemented yet")
  nil)
