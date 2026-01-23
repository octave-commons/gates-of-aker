(ns fantasia.sim.ecs.tick
   (:require [brute.entity :as be]
             [fantasia.sim.ecs.core]
             [fantasia.sim.ecs.components :as c]
             [fantasia.sim.ecs.systems.needs-decay]
             [fantasia.sim.ecs.systems.movement]
             [fantasia.sim.ecs.systems.job_processing]
             [fantasia.sim.ecs.adapter]))

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
        (fantasia.sim.ecs.systems.movement/process))))

(defn tick-ecs-once [global-state]
  "Run one ECS tick with all systems.
   Returns updated global-state with ECS data converted via adapter."
  (let [ecs-world (get-ecs-world)
        ecs-world' (run-systems ecs-world global-state)
        new-tick (inc (:tick global-state))
        global-state' (assoc global-state :tick new-tick)
        snapshot (fantasia.sim.ecs.adapter/ecs->snapshot ecs-world' global-state')]
    (clojure.core/reset! *ecs-world ecs-world')
    (clojure.core/reset! *global-state global-state')
    snapshot))

(defn tick-ecs! [n]
  "Run N ECS ticks.
   Returns vector of snapshot outputs."
  (loop [i 0
         outs []]
    (if (>= i n)
      outs
      (let [snapshot (tick-ecs-once @*global-state)]
        (recur (inc i) (conj outs snapshot))))))

(defn import-agent [ecs-world agent-data]
  "Import old-style agent map into ECS.
   Returns [entity-id updated-ecs-world]"
  (let [[q r] (:pos agent-data)
        id (:id agent-data)
        role (:role agent-data)
        {:keys [warmth food sleep]} (:needs agent-data)
        {:keys [wood food-item]} (:inventory agent-data)
        result (fantasia.sim.ecs.core/create-agent ecs-world id q r role)
        entity-id (first result)
        world' (second result)]
    [entity-id 
     (-> world'
         (fantasia.sim.ecs.core/update-agent-needs entity-id warmth food sleep)
         (fantasia.sim.ecs.core/update-agent-inventory entity-id wood (or food-item 0))
         (be/add-component entity-id (c/->Frontier (:frontier agent-data)))
         (be/add-component entity-id (c/->Recall (:recall agent-data))))]))

(defn import-tile [ecs-world tile-key tile-data]
    "Import old-style tile map into ECS.
     Returns updated-ecs-world"
    (let [tile-key' (if (keyword? tile-key) (name tile-key) tile-key)]
      (cond
        ;; Handle vector keys directly
        (and (sequential? tile-key') (= 2 (count tile-key')))
        (try
          (let [q (first tile-key')
                r (second tile-key')
                terrain (or (:terrain tile-data) :ground)
                biome (or (:biome tile-data) :plains)
                structure (:structure tile-data)
                resource (:resource tile-data)
                [_ _ world'] (fantasia.sim.ecs.core/create-tile ecs-world q r terrain biome structure resource)]
            world')
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
                resource (:resource tile-data)
                [_ _ world'] (fantasia.sim.ecs.core/create-tile ecs-world q r terrain biome structure resource)]
            world')
          (catch Exception e
            (println "[ECS] Warning: Failed to import tile at" tile-key ":" (.getMessage e))
            ecs-world))
        :else
        ecs-world)))

(defn import-stockpile [ecs-world tile-key contents]
    "Import old-style stockpile into ECS.
     Returns updated-ecs-world"
    (cond
      ;; Handle vector keys directly
      (and (sequential? tile-key) (= 2 (count tile-key)))
      (try
        (let [q (first tile-key)
              r (second tile-key)
              result (fantasia.sim.ecs.core/create-stockpile ecs-world q r)
              world' (nth result 2)]
          world')
         (catch Exception e
           (println "[ECS] Warning: Failed to import stockpile at" tile-key ":" (.getMessage e))
           ecs-world))
      ;; Handle string keys (backward compatibility)
      (and (string? tile-key) (clojure.string/includes? tile-key ","))
      (try
        (let [parts (clojure.string/split tile-key #",")
              q (Integer/parseInt (first parts))
              r (Integer/parseInt (second parts))
              result (fantasia.sim.ecs.core/create-stockpile ecs-world q r)
              world' (nth result 2)]
          world')
         (catch Exception e
           (println "[ECS] Warning: Failed to import stockpile at" tile-key ":" (.getMessage e))
           ecs-world))
      :else
      ecs-world))

(defn import-world-to-ecs [old-world]
  "Convert entire old-style world to ECS.
   Returns updated ECS world."
  (println "[ECS] Importing world to ECS...")
  (let [ecs-world (get-ecs-world)
        
        agents (get old-world :agents [])
        ecs-world' (reduce (fn [ecs agent]
                              (second (import-agent ecs agent)))
                            ecs-world
                            agents)
        
        tiles (get old-world :tiles {})
        ecs-world'' (reduce-kv (fn [ecs tile-key tile-data]
                                 (import-tile ecs tile-key tile-data))
                               ecs-world'
                               tiles)
        
        stockpiles (get old-world :stockpiles {})
        ecs-world''' (reduce-kv (fn [ecs tile-key contents]
                                   (import-stockpile ecs tile-key contents))
                                 ecs-world''
                                 stockpiles)]
    (clojure.core/reset! *ecs-world ecs-world''')
    (println "[ECS] Imported" (count agents) "agents," (count tiles) "tiles," (count stockpiles) "stockpiles")
    ecs-world'''))

(defn create-ecs-initial-world [opts]
   "Create initial ECS world from scratch.
    Returns global-state with ECS data."
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
