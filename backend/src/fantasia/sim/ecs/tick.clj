(ns fantasia.sim.ecs.tick
   (:require [brute.entity :as be]
             [fantasia.sim.ecs.core]
             [fantasia.sim.ecs.components :as c]
             [fantasia.sim.ecs.systems.needs-decay]
             [fantasia.sim.ecs.systems.movement]
             [fantasia.sim.ecs.systems.agent-interaction]
             [fantasia.sim.ecs.adapter]))

(def ^:dynamic *ecs-world (atom (fantasia.sim.ecs.core/create-ecs-world)))
(def ^:dynamic *global-state (atom {}))

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
          (fantasia.sim.ecs.systems.movement/process)
          ;; (fantasia.sim.ecs.systems.agent-interaction/process))))

(defn tick-ecs-once [global-state]
  "Run one ECS tick with all systems."
  (let [ecs-world (get-ecs-world)
        ecs-world' (run-systems ecs-world global-state)
        new-tick (inc (:tick global-state))
        global-state' (assoc global-state :tick new-tick)
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
        (recur (inc i) outs'))))

(defn create-ecs-initial-world [opts]
  "Create initial ECS world from scratch."
  (reset-ecs-world!)
  (let [seed (or (:seed opts) 1)
         global-state {:seed seed
                         :tick 0
                         :map {:kind :hex :layout :pointy :bounds {:shape :rect :w 128 :h 128}}
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
                         :tile-visibility {}}]
    (clojure.core/reset! *global-state global-state)
    (println "[ECS] Created initial world")
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
             [_ _ world'] (fantasia.sim.ecs.core/create-tile ecs-world q r terrain biome structure resource))
           (catch Exception e
             (println "[ECS] Warning: Failed to import tile at" tile-key ":" (.getMessage e))
             ecs-world))
         ;; Handle string keys (backward compatibility)
         (and tile-key' (string? tile-key') (clojure.string/includes? tile-key' ","))
         (try
           (let [parts (clojure.string/split tile-key' #",")]
                 q (Integer/parseInt (first parts))
                 r (Integer/parseInt (second parts))
                 terrain (or (:terrain tile-data) :ground)
                 biome (or (:biome tile-data) :plains)
                 structure (:structure tile-data)
                 resource (:resource tile-data)]
           [_ _ world'] (fantasia.sim.ecs.core/create-tile ecs-world q r terrain biome structure resource))
           (catch Exception e
             (println "[ECS] Warning: Failed to import tile at" tile-key ":" (.getMessage e))
             ecs-world))
         :else
         ecs-world)))
