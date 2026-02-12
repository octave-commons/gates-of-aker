(ns fantasia.sim.ecs.tick
  (:require [brute.entity :as be]
            [clojure.string :as str]
            [fantasia.dev.logging :as log]
            [fantasia.sim.biomes :as biomes]
            [fantasia.sim.constants :as const]
            [fantasia.sim.ecs.adapter :as adapter]
            [fantasia.sim.ecs.components :as c]
            [fantasia.sim.ecs.core :as ecs-core]
            [fantasia.sim.ecs.systems.combat :as combat]
            [fantasia.sim.ecs.systems.job-assignment :as job-assignment]
            [fantasia.sim.ecs.systems.job-creation :as job-creation]
            [fantasia.sim.ecs.systems.job-processing :as job-processing]
            [fantasia.sim.ecs.systems.mortality :as mortality]
            [fantasia.sim.ecs.systems.movement :as movement]
            [fantasia.sim.ecs.systems.needs :as needs]
            [fantasia.sim.ecs.systems.needs-decay :as needs-decay]
            [fantasia.sim.ecs.systems.reproduction :as reproduction]
            [fantasia.sim.ecs.systems.social :as social]
            [fantasia.sim.hex :as hex]
            [fantasia.sim.time :as time]
            [fantasia.sim.world :as world]))

(def ^:dynamic *ecs-world (atom (ecs-core/create-ecs-world)))
(def ^:dynamic *global-state (atom {}))

(defn get-ecs-world []
  @*ecs-world)

(defn reset-ecs-world! []
  (clojure.core/reset! *ecs-world (ecs-core/create-ecs-world)))

(defn- with-ecs-projections
  [state ecs-world]
  (let [ecs-tiles (adapter/ecs->tiles-map ecs-world)]
  (-> state
      (assoc :tiles (merge (:tiles state {}) ecs-tiles))
      (assoc :agents (vec (adapter/ecs->agent-list ecs-world)))
      (assoc :jobs (adapter/ecs->jobs-map ecs-world))
      (assoc :stockpiles (adapter/ecs->stockpiles-map ecs-world)))))

(defn run-systems
  "Run all ECS systems in sequence.
   Returns world and emitted events."
  [ecs-world global-state]
  (let [levers (:levers global-state {})
        tick (:tick global-state 0)
        cold-snap (or (:cold-snap levers) 0.4)
        world1 (needs-decay/process ecs-world cold-snap)
        world2 (needs/process world1 global-state)
        world3 (job-creation/process world2 global-state)
        world4 (job-assignment/process world3 global-state)
        world5 (job-processing/process world4 global-state)
        world6 (movement/process world5 global-state)
        combat-result (combat/process world6 tick)
        world7 (:world combat-result)
        social-result (social/process world7 tick)
        world8 (:world social-result)
        reproduction-result (reproduction/process-reproduction world8 tick)
        world9 (:world reproduction-result)
        world10 (mortality/process world9)]
    {:ecs-world world10
     :combat-events (:events combat-result)
     :social-interactions (:events social-result)
     :reproduction-events (:events reproduction-result)}))

(defn tick-ecs-once
   "Run one ECS tick with all systems."
   [global-state]
   (let [ecs-world (get-ecs-world)
         {:keys [ecs-world combat-events social-interactions reproduction-events]} (run-systems ecs-world global-state)
         new-tick (inc (:tick global-state))
         seed (:seed global-state)
         old-global-state @*global-state
         global-state' (-> global-state
                             (assoc :tick new-tick)
                             (assoc :temperature (time/temperature-at seed new-tick))
                             (assoc :daylight (time/daylight-at seed new-tick))
                             (with-ecs-projections ecs-world))
         snapshot (adapter/ecs->snapshot ecs-world global-state')
         delta-snapshot (world/delta-snapshot old-global-state global-state' nil)]
     (clojure.core/reset! *ecs-world ecs-world)
     (clojure.core/reset! *global-state global-state')
     {:snapshot snapshot
       :tick new-tick
       :attribution nil
       :delta-snapshot delta-snapshot
       :combat-events combat-events
       :social-interactions social-interactions
       :reproduction-events reproduction-events}))

(defn tick-ecs!
  "Run N ECS ticks."
  [n]
  (loop [i 0
         outs []]
    (if (>= i n)
      (reverse outs)
      (let [snapshot (tick-ecs-once @*global-state)
            outs' (conj outs snapshot)]
        (recur (inc i) outs')))))

(defn spawn-initial-agents!
  "Spawn initial agents near map center."
  [ecs-world bounds]
  (let [center-q (if (= (:shape bounds) :rect)
                   (+ (:origin-q bounds 0) (quot (:w bounds) 2))
                   (:origin-q bounds 0))
        center-r (if (= (:shape bounds) :rect)
                   (+ (:origin-r bounds 0) (quot (:h bounds) 2))
                   (:origin-r bounds 0))
        spawn-radius 10
        rng #(rand-int spawn-radius)]
    (println "[ECS] Spawning initial agents near center:" center-q center-r)
    (let [[_ ecs-world1] (ecs-core/create-agent ecs-world nil center-q center-r :priest)
          [_ ecs-world2] (ecs-core/create-agent ecs-world1 nil (+ center-q (rng)) (+ center-r (rng)) :knight)
          [_ ecs-world3] (ecs-core/create-agent ecs-world2 nil (+ center-q (rng)) (+ center-r (rng)) :peasant)
          [_ ecs-world4] (ecs-core/create-agent ecs-world3 nil (+ center-q (rng)) (+ center-r (rng)) :peasant)
          [_ ecs-world5] (ecs-core/create-agent ecs-world4 nil (+ center-q (rng)) (+ center-r (rng)) :peasant)]
      (println "[ECS] Spawned 5 initial agents")
       ecs-world5)))

(defn spawn-initial-buildings!
  "Spawn initial buildings with job queues near map center.
   Includes workshop for builder jobs, campfire for warmth, and stockpile for storage."
  [ecs-world bounds]
  (let [center-q (if (= (:shape bounds) :rect)
                   (+ (:origin-q bounds 0) (quot (:w bounds) 2))
                   (:origin-q bounds 0))
         center-r (if (= (:shape bounds) :rect)
                   (+ (:origin-r bounds 0) (quot (:h bounds) 2))
                   (:origin-r bounds 0))
        ;; Place buildings in a circle around center
        campfire-pos [(- center-q 2) center-r]
        stockpile-pos [(+ center-q 2) center-r]
        farm-pos [center-q (- center-r 3)]
        orchard-pos [(- center-q 3) (- center-r 2)]
        house-pos [(+ center-q 3) (- center-r 2)]
        warehouse-pos [(+ center-q 1) (+ center-r 3)]]
    (println "[ECS] Spawning initial buildings")
    (let [[_ ecs-world1] (ecs-core/create-building ecs-world campfire-pos :campfire)
          [_ ecs-world2] (ecs-core/create-building ecs-world1 stockpile-pos :stockpile
                                                   {:stockpile-config {:resource :fruit}})
          [_ ecs-world3] (ecs-core/create-building ecs-world2 farm-pos :farm)
          [_ ecs-world4] (ecs-core/create-building ecs-world3 orchard-pos :orchard)
          [_ ecs-world5] (ecs-core/create-building ecs-world4 house-pos :house)
          [_ ecs-world6] (ecs-core/create-building ecs-world5 warehouse-pos :warehouse
                                                            {:stockpile-config {:resource :log}})]
      (println "[ECS] Spawned 6 initial buildings")
      ecs-world6)))

(defn create-ecs-initial-world
  "Create initial ECS world from scratch."
  [opts]
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

(defn- parse-tile-pos
  [tile-key]
  (cond
    (and (vector? tile-key) (= 2 (count tile-key))) tile-key
    (string? tile-key)
    (let [parts (str/split tile-key #",")]
      (when (= 2 (count parts))
        [(Long/parseLong (nth parts 0)) (Long/parseLong (nth parts 1))]))
    :else nil))

(defn- scatter-initial-fruit
  [state]
  (let [tiles (:tiles state {})
        tree-positions (->> tiles
                            (keep (fn [[k v]]
                                    (when (= :tree (:resource v))
                                      (parse-tile-pos k))))
                            (take 40))
        placements (->> tree-positions
                        (mapcat hex/neighbors)
                        (distinct)
                        (filter (fn [pos]
                                  (let [tile (get tiles (str (first pos) "," (second pos)))]
                                    (and tile (nil? (:resource tile))))))
                        (take 30))]
    (reduce (fn [acc [q r]]
              (assoc-in acc [:tiles (str q "," r) :resource] :fruit))
            state
            placements)))

(defn import-tile
   "Import old-style tile map into ECS."
   [ecs-world tile-key tile-data]
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
          (let [[_ _ world'] (ecs-core/create-tile ecs-world q r terrain biome structure resource)]
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
          (let [[_ _ world'] (ecs-core/create-tile ecs-world q r terrain biome structure resource)]
            world'))
        (catch Exception e
          (println "[ECS] Warning: Failed to import tile at" tile-key ":" (.getMessage e))
          ecs-world))
      :else
      ecs-world)))

;; Legacy compatibility functions for fantasia.sim.core
(defn get-state
  "Get the current global state."
  []
  @*global-state)

(defn reset-world!
  "Reset both ECS world and global state with optional parameters."
  ([] (reset-world! {}))
  ([opts]
   (let [seed (or (:seed opts) 1)
         tree-density (or (:tree_density opts) (:tree-density opts) 0.05)
         bounds (or (:bounds opts) {:shape :rect :w 128 :h 128})
         world-opts (assoc opts :seed seed :tree-density tree-density :bounds bounds)]
     (println "[ECS] Resetting world with seed:" seed "tree-density:" tree-density "bounds:" bounds)
     (reset-ecs-world!)
     (let [global-state (create-ecs-initial-world world-opts)]
       ;; Apply biome generation to create tiles
         (let [world-with-biomes (biomes/generate-biomes! global-state)
              world-with-resources (biomes/spawn-biome-resources! world-with-biomes)
              world-with-fruit (scatter-initial-fruit world-with-resources)]
           (clojure.core/reset! *global-state world-with-fruit)
           world-with-fruit)))))

(defn tick!
  "Run N ECS ticks - alias for tick-ecs!."
  [n]
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
  (let [ecs-world (get-ecs-world)
        pos [q r]
        tile-id (ecs-core/get-tile-at-pos ecs-world pos)
        tile-type (be/get-component-type (c/->Tile :ground :plains nil nil))
        ecs-world' (if tile-id
                     (let [tile (be/get-component ecs-world tile-id tile-type)
                           terrain (or (:terrain tile) :ground)
                           biome (or (:biome tile) :plains)
                           resource (:resource tile)
                           updated-tile (c/->Tile terrain biome :shrine resource)]
                       (-> ecs-world
                           (be/add-component tile-id updated-tile)
                           (be/add-component tile-id (c/->ShrineState nil))))
                      (let [[_ _ world'] (ecs-core/create-tile ecs-world q r :ground :plains :shrine nil
                                                                           {:shrine-state (c/->ShrineState nil)})]
                        world'))]
    (clojure.core/reset! *ecs-world ecs-world')
    (swap! *global-state
           (fn [state]
             (-> state
                 (assoc :shrine pos)
                 (update :tiles
                         (fn [tiles]
                           (let [tiles' (or tiles {})
                                 tile (get tiles' pos {:terrain :ground :biome :plains :resource nil})]
                             (assoc tiles' pos (assoc tile :structure :shrine))))))))
    pos))

(defn- tile-key [q r]
  [q r])

(defn- tile-component-type []
  (be/get-component-type (c/->Tile :ground :plains nil nil)))

(defn- path-component-type []
  (be/get-component-type (c/->Path [] 0)))

(defn- role-component-type []
  (be/get-component-type (c/->Role :peasant)))

(defn- agent-info-component-type []
  (be/get-component-type (c/->AgentInfo nil nil)))

(defn- job-queue-component-type []
  (be/get-component-type (c/->JobQueue {} [] {})))

(defn- as-uuid
  [v]
  (cond
    (instance? java.util.UUID v) v
    (string? v) (try
                  (java.util.UUID/fromString v)
                  (catch Exception _ nil))
    :else nil))

(defn- locate-agent-entity-id
  [ecs-world agent-id]
  (let [role-t (role-component-type)
        info-t (agent-info-component-type)
        candidates (be/get-all-entities-with-component ecs-world role-t)
        as-uuid-id (as-uuid agent-id)]
    (some (fn [entity-id]
            (let [agent-info (be/get-component ecs-world entity-id info-t)]
              (when (or (= entity-id agent-id)
                        (= entity-id as-uuid-id)
                        (= (:id agent-info) agent-id)
                        (= (:id agent-info) as-uuid-id)
                        (= (str entity-id) (str agent-id))
                        (= (str (:id agent-info)) (str agent-id)))
                entity-id)))
          candidates)))

(defn- ensure-tile-entity
  [ecs-world q r]
  (if-let [existing-id (ecs-core/get-tile-at-pos ecs-world [q r])]
    [existing-id ecs-world]
    (let [[_ tile-id world'] (ecs-core/create-tile ecs-world q r :ground :plains nil nil)]
      [tile-id world'])))

(defn- set-tile-resource
  [ecs-world q r resource]
  (let [[tile-id world'] (ensure-tile-entity ecs-world q r)
        tile-t (tile-component-type)
        tile (or (be/get-component world' tile-id tile-t)
                 (c/->Tile :ground :plains nil nil))
        updated-tile (assoc tile :resource resource)]
    (be/add-component world' tile-id updated-tile)))

(defn- set-tile-structure
  [ecs-world q r structure]
  (let [[tile-id world'] (ensure-tile-entity ecs-world q r)
        tile-t (tile-component-type)
        tile (or (be/get-component world' tile-id tile-t)
                 (c/->Tile :ground :plains nil nil))
        updated-tile (assoc tile :structure structure)]
    (be/add-component world' tile-id updated-tile)))

(defn- upsert-world-tile!
  [q r f]
  (swap! *global-state
         (fn [state]
           (let [k (tile-key q r)
                 current (get-in state [:tiles k] {:terrain :ground :biome :plains :resource nil})]
             (assoc-in state [:tiles k] (f current))))))

(defn- sync-global-projections!
  [ecs-world]
  (swap! *global-state with-ecs-projections ecs-world)
  (clojure.core/reset! *ecs-world ecs-world)
  ecs-world)

(defn- structure-priority
  [structure]
  (case structure
    :wall 75
    :shrine 100
    :campfire 70
    :stockpile 65
    :warehouse 68
    :statue/dog 40
    :tree 30
    50))

(defn- normalize-job-type
  [job-type]
  (cond
    (keyword? job-type) job-type
    (string? job-type)
    (keyword (if (str/includes? job-type "/")
               job-type
               (str "job/" job-type)))
    :else :job/build-structure))

(defn- now-tick []
  (:tick @*global-state 0))

(defn appoint-mouthpiece! [agent-id]
  (let [ecs-world (get-ecs-world)
        shrine-pos (:shrine @*global-state)
        agent-entity-id (locate-agent-entity-id ecs-world agent-id)
        shrine-id (when (vector? shrine-pos)
                    (ecs-core/get-tile-at-pos ecs-world shrine-pos))]
    (when (and shrine-id agent-entity-id)
      (let [updated-world (be/add-component ecs-world shrine-id (c/->ShrineState agent-entity-id))]
        (clojure.core/reset! *ecs-world updated-world)))
    (swap! *global-state assoc-in [:levers :mouthpiece-agent-id] agent-entity-id)
    agent-entity-id))

(defn place-wall-ghost! [q r]
  (let [ecs-world (get-ecs-world)
        [tile-id ecs-world-with-tile] (ensure-tile-entity ecs-world q r)
        ecs-world' (-> ecs-world-with-tile
                       (set-tile-structure q r :wall-ghost)
                       (be/add-component tile-id (c/->WallGhost nil)))]
    (upsert-world-tile! q r #(assoc % :structure :wall-ghost))
    (sync-global-projections! ecs-world')
    [q r]))

(defn place-stockpile! [q r]
  (let [ecs-world (get-ecs-world)
        [tile-id ecs-world-with-tile] (ensure-tile-entity ecs-world q r)
        ecs-world' (-> ecs-world-with-tile
                       (set-tile-structure q r :stockpile)
                       (be/add-component tile-id (c/->Stockpile {:log 0}))
                       (be/add-component tile-id (c/->StructureState 1 100 100 nil)))]
    (upsert-world-tile! q r #(assoc % :structure :stockpile))
    (sync-global-projections! ecs-world')
    [q r]))

(defn place-warehouse! [q r]
  (let [ecs-world (get-ecs-world)
        [tile-id ecs-world-with-tile] (ensure-tile-entity ecs-world q r)
        ecs-world' (-> ecs-world-with-tile
                       (set-tile-structure q r :warehouse)
                       (be/add-component tile-id (c/->Stockpile {:log 0}))
                       (be/add-component tile-id (c/->StructureState 1 100 100 nil))
                       (be/add-component tile-id (c/->JobQueue {} [] {})))]
    (upsert-world-tile! q r #(assoc % :structure :warehouse))
    (sync-global-projections! ecs-world')
    [q r]))

(defn place-campfire! [q r]
  (let [ecs-world (get-ecs-world)
        [tile-id ecs-world-with-tile] (ensure-tile-entity ecs-world q r)
        ecs-world' (-> ecs-world-with-tile
                       (set-tile-structure q r :campfire)
                       (be/add-component tile-id (c/->StructureState 1 100 100 nil))
                       (be/add-component tile-id (c/->CampfireState const/campfire-radius true (now-tick)))
                       (be/add-component tile-id (c/->JobQueue {} [] {})))]
    (upsert-world-tile! q r #(assoc % :structure :campfire))
    (sync-global-projections! ecs-world')
    [q r]))

(defn place-statue-dog! [q r]
  (let [ecs-world (get-ecs-world)
        [tile-id ecs-world-with-tile] (ensure-tile-entity ecs-world q r)
        ecs-world' (-> ecs-world-with-tile
                       (set-tile-structure q r :statue/dog)
                       (be/add-component tile-id (c/->StructureState 1 100 100 nil)))]
    (upsert-world-tile! q r #(assoc % :structure :statue/dog))
    (sync-global-projections! ecs-world')
    [q r]))

(defn place-tree! [q r]
  (let [ecs-world (set-tile-resource (get-ecs-world) q r :tree)]
    (upsert-world-tile! q r #(assoc % :resource :tree))
    (sync-global-projections! ecs-world)
    [q r]))

(defn- spawn-animal! [role q r]
  (let [[entity-id ecs-world'] (ecs-core/create-agent (get-ecs-world) nil q r role)]
    (sync-global-projections! ecs-world')
    entity-id))

(defn place-deer! [q r]
  (spawn-animal! :deer q r))

(defn place-wolf! [q r]
  (spawn-animal! :wolf q r))

(defn place-bear! [q r]
  (spawn-animal! :bear q r))

(defn queue-build-job! [job-data]
  (let [ecs-world (get-ecs-world)
        payload (if (map? job-data) job-data {:structure job-data})
        structure (:structure payload)
        target-pos (or (:pos payload) (:target-pos payload) (:target payload))
        target-pos' (when (and (vector? target-pos) (= 2 (count target-pos)))
                      target-pos)
        tick (now-tick)
        job-id (str "build-" tick "-" (subs (str (java.util.UUID/randomUUID)) 0 8))
        queue-t (job-queue-component-type)
        queue-entity-id (or (:queue-entity-id payload)
                            (first (be/get-all-entities-with-component ecs-world queue-t)))
        job-type (or (:job-type payload)
                     (if structure :job/build-structure :job/manual))
        job {:id job-id
             :type (normalize-job-type job-type)
             :structure structure
             :target-pos target-pos'
             :target target-pos'
             :state :pending
             :priority (or (:priority payload) (structure-priority structure))
             :created-at tick}
        agent-id (:agent-id payload)
        agent-entity-id (when agent-id
                          (locate-agent-entity-id ecs-world agent-id))]
    (log/log-info "[JOB:CREATE]"
                  {:job-id job-id
                   :type (:type job)
                   :structure structure
                   :target target-pos'
                   :priority (:priority job)
                   :source :queue-build})
    (when-not queue-entity-id
      (throw (ex-info "No building with JobQueue exists in ECS world" {:job-data payload})))
    (let [ecs-world-base (if (and (= structure :wall) target-pos')
                           (set-tile-structure ecs-world (first target-pos') (second target-pos') :wall-ghost)
                           ecs-world)
          queue (or (be/get-component ecs-world-base queue-entity-id queue-t)
                    (c/->JobQueue {} [] {}))
          jobs (assoc (:jobs queue {}) job-id job)
          pending-jobs (conj (vec (:pending-jobs queue [])) job-id)
          assigned-jobs (:assigned-jobs queue {})
          queue' (c/->JobQueue jobs pending-jobs assigned-jobs)
          ecs-world-with-queue (be/add-component ecs-world-base queue-entity-id queue')
          ecs-world' (if agent-entity-id
                       (let [world-with-assignment (be/add-component ecs-world-with-queue agent-entity-id (c/->JobAssignment job-id 0.0))]
                         (if target-pos'
                           (do
                             (log/log-info "[PATH:REQUEST]"
                                           {:agent-id agent-entity-id
                                            :job-id job-id
                                            :start nil
                                            :goal target-pos'
                                            :source :queue-build})
                             (let [world-with-path (ecs-core/set-agent-path world-with-assignment agent-entity-id [target-pos'])]
                               (log/log-info "[PATH:RESULT]"
                                             {:agent-id agent-entity-id
                                              :job-id job-id
                                              :status :ok
                                              :waypoints 1
                                              :goal target-pos'})
                               world-with-path))
                           world-with-assignment))
                       ecs-world-with-queue)]
      (when (and (= structure :wall) target-pos')
        (upsert-world-tile! (first target-pos') (second target-pos') #(assoc % :structure :wall-ghost)))
      (sync-global-projections! ecs-world')
      job)))

(defn get-agent-path! [agent-id]
  (let [ecs-world (get-ecs-world)
        entity-id (locate-agent-entity-id ecs-world agent-id)
        path (when entity-id
               (be/get-component ecs-world entity-id (path-component-type)))]
    (when path
      (->> (:waypoints path)
           (map (fn [waypoint]
                  (cond
                    (and (vector? waypoint) (= 2 (count waypoint))) waypoint
                    (map? waypoint) [(:q waypoint) (:r waypoint)]
                    :else nil)))
           (remove nil?)
           vec))))
