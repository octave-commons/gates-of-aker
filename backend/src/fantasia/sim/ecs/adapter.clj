(ns fantasia.sim.ecs.adapter
  (:require [brute.entity :as be]
            [clojure.string :as str]
            [fantasia.sim.ecs.components :as c]
            [fantasia.sim.ecs.core :as ecs-core]
            [fantasia.sim.time :as time]))

(def component-types
  "Lazy-loaded map of component types for efficient queries."
  (delay
    {:position (be/get-component-type (fantasia.sim.ecs.components/->Position 0 0))
     :needs (be/get-component-type (fantasia.sim.ecs.components/->Needs 0.6 0.7 0.7 1.0 0.8 0.6 0.5 0.5 0.5 0.6 0.5 0.5 0.5))
     :inventory (be/get-component-type (fantasia.sim.ecs.components/->Inventory 0 0))
     :role (be/get-component-type (fantasia.sim.ecs.components/->Role :priest))
     :agent-info (be/get-component-type (fantasia.sim.ecs.components/->AgentInfo nil nil))
     :frontier (be/get-component-type (fantasia.sim.ecs.components/->Frontier {}))
     :recall (be/get-component-type (fantasia.sim.ecs.components/->Recall {}))
     :status (be/get-component-type (fantasia.sim.ecs.components/->AgentStatus true false true nil))
     :job-assignment (be/get-component-type (fantasia.sim.ecs.components/->JobAssignment nil 0.0))
     :path (be/get-component-type (fantasia.sim.ecs.components/->Path [] 0))
     :tile (be/get-component-type (fantasia.sim.ecs.components/->Tile :ground nil nil nil))
     :stockpile (be/get-component-type (fantasia.sim.ecs.components/->Stockpile {}))
     :wall-ghost (be/get-component-type (fantasia.sim.ecs.components/->WallGhost nil))
     :agent (be/get-component-type (fantasia.sim.ecs.components/->Agent "test"))
     :tile-index (be/get-component-type (fantasia.sim.ecs.components/->TileIndex 0 0))}))

(defn get-comp
  "Helper to get component from entity by type key."
  [world entity-id type-key]
  (let [comp-type (get @component-types type-key)]
    (if comp-type
      (ecs-core/get-component-safe world entity-id comp-type)
      nil)))

(defn- normalize-resource
  "Normalize resource values for frontend rendering contracts."
  [resource]
  (cond
    (keyword? resource) (name resource)
    (string? resource) (str/replace resource #"^:" "")
    :else resource))

(defn- normalize-tile-key
  [tile-key]
  (cond
    (string? tile-key) tile-key
    (vector? tile-key) (str (first tile-key) "," (second tile-key))
    :else (str tile-key)))

(defn ecs->agent-map
  "Convert agent entity to old-style map format."
  [ecs-world agent-id]
  (let [position (get-comp ecs-world agent-id :position)
        role (get-comp ecs-world agent-id :role)
        agent-info (get-comp ecs-world agent-id :agent-info)
        needs (get-comp ecs-world agent-id :needs)
        inventory (get-comp ecs-world agent-id :inventory)
        agent-info-type (be/get-component-type (c/->AgentInfo nil nil))
        agent-info (ecs-core/get-component-safe ecs-world agent-id agent-info-type)
        frontier (get-comp ecs-world agent-id :frontier)
        recall (get-comp ecs-world agent-id :recall)
        status (or (get-comp ecs-world agent-id :status)
                   (c/->AgentStatus true false true nil))
        job-assignment (get-comp ecs-world agent-id :job-assignment)
        path (get-comp ecs-world agent-id :path)]
    {:id agent-id
     :name (:name agent-info)
     :pos [(:q position) (:r position)]
     :role (:type role)
     :needs {:warmth (:warmth needs) :food (:food needs) :sleep (:sleep needs)}
     :inventory {:wood (:wood inventory) :food (:food inventory)}
     :frontier (:facets frontier)
     :recall (:events recall)
     :status {:alive? (:alive? status)
              :asleep? (:asleep? status)
              :idle? (:idle? status)
              :cause-of-death (:cause-of-death status)}
     :current-job (:job-id job-assignment)
     :current-path (:waypoints path)}))

(defn ecs->agent-list
  "Convert all agent entities to old-style list format."
  [ecs-world]
  (let [agent-ids (ecs-core/get-all-agents ecs-world)]
    (println "[ECS Adapter] Found agent IDs:" agent-ids "Count:" (count agent-ids))
    (map (partial ecs->agent-map ecs-world) agent-ids)))

(defn ecs->tile-map
  "Convert tile entity to old-style map format."
  [ecs-world tile-id]
  (let [tile-index (get-comp ecs-world tile-id :tile-index)
        tile (get-comp ecs-world tile-id :tile)]
    (when tile-index
      {:pos [(:q tile-index) (:r tile-index)]
        :terrain (:terrain tile)
        :biome (:biome tile)
        :resource (normalize-resource (:resource tile))
        :structure (:structure tile)})))

(defn ecs->tiles-map
  "Convert all tile entities to old-style map format keyed by position."
  [ecs-world]
  (reduce (fn [acc tile-id]
            (let [tile-index (get-comp ecs-world tile-id :tile-index)
                  tile-key (when tile-index (str (:q tile-index) "," (:r tile-index)))
                  tile-data (ecs->tile-map ecs-world tile-id)]
              (if (and tile-key tile-data)
                (assoc acc tile-key tile-data)
                acc)))
          {}
          (ecs-core/get-all-tiles ecs-world)))

(defn ecs->stockpiles-map
  "Convert stockpile entities to old-style map format."
  [ecs-world]
  (let [stockpile-instance (fantasia.sim.ecs.components/->Stockpile {})
        entities (ecs-core/get-all-entities-with-component-safe ecs-world stockpile-instance)]
    (println "[ECS Adapter] Getting stockpiles, found" (count entities) "stockpile entities")
    (reduce (fn [acc entity-id]
              (let [stockpile (get-comp ecs-world entity-id :stockpile)
                    index (get-comp ecs-world entity-id :tile-index)]
                (println "[ECS Adapter] Stockpile entity" entity-id ":" (:contents stockpile))
                (if index
                  (assoc acc (str (:q index) "," (:r index)) (:contents stockpile))
                  acc)))
            {}
            entities))
  )

(defn ecs->jobs-map
  "Extract jobs from ECS JobQueue components."
  [ecs-world]
  (let [buildings-with-queues (ecs-core/get-all-entities-with-component-safe ecs-world (c/->JobQueue {} [] {}))]
    (reduce (fn [acc building-id]
              (let [job-queue (ecs-core/get-component-safe ecs-world building-id (c/->JobQueue {} [] {}))
                    jobs (:jobs job-queue {})]
                (merge acc jobs)))
            {}
            buildings-with-queues)))

(defn ecs->snapshot
  "Convert ECS world to old-style snapshot for WebSocket broadcast."
  [ecs-world global-state]
  (let [tile-visibility (into {}
                              (map (fn [[tile-key vis]]
                                     [(normalize-tile-key tile-key) vis]))
                              (:tile-visibility global-state {}))
        ecs-tiles (ecs->tiles-map ecs-world)
        all-tiles (merge (into {}
                               (map (fn [[tile-key tile-data]]
                                      [(normalize-tile-key tile-key) tile-data]))
                               (:tiles global-state {}))
                         ecs-tiles)
        visible-tiles (if (empty? tile-visibility)
                        all-tiles
                        (into {}
                              (filter (fn [[tile-key _]]
                                        (let [vis (get tile-visibility tile-key :hidden)]
                                          (or (= vis :visible) (= vis :revealed))))
                                      all-tiles)))
        calendar (time/calendar-info global-state)
        agent-list (ecs->agent-list ecs-world)
        memories (mapv (fn [m]
                         {:id (:id m)
                          :type (:type m)
                          :location (:location m)
                          :created-at (:created-at m)
                          :strength (:strength m)
                          :decay-rate (:decay-rate m)
                          :entity-id (:entity-id m)
                          :facets (:facets m)})
                       (vals (:memories global-state)))]
    (println "[ECS Adapter] Creating snapshot with" (count agent-list) "agents" (count memories) "memories")
    {:tick (:tick global-state)
     :shrine (:shrine global-state)
     :temperature (:temperature global-state)
     :daylight (:daylight global-state)
     :calendar calendar
     :cold-snap (get-in global-state [:levers :cold-snap] 0.4)
     :levers (:levers global-state)
     :map (:map global-state)
     :tiles visible-tiles
     :tile-visibility tile-visibility
     :revealed-tiles-snapshot (:revealed-tiles-snapshot global-state {})
     :recent-events (:recent-events global-state)
     :attribution (:attribution global-state)
     :jobs (ecs->jobs-map ecs-world)
     :items (:items global-state)
     :stockpiles (ecs->stockpiles-map ecs-world)
     :agents agent-list
     :memories memories
     :ledger (:ledger global-state)}))
