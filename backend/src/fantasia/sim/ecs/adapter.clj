(ns fantasia.sim.ecs.adapter
  (:require [brute.entity :as be]
            [fantasia.sim.ecs.core]
            [fantasia.sim.ecs.components]))

(def component-types
  "Lazy-loaded map of component types for efficient queries."
  (delay
    {:position (be/get-component-type (fantasia.sim.ecs.components/->Position 0 0))
     :needs (be/get-component-type (fantasia.sim.ecs.components/->Needs 0.6 0.7 0.7 1.0 0.8 0.6 0.5 0.5 0.5 0.6 0.5 0.5 0.5))
     :inventory (be/get-component-type (fantasia.sim.ecs.components/->Inventory 0 0))
     :role (be/get-component-type (fantasia.sim.ecs.components/->Role :priest))
     :frontier (be/get-component-type (fantasia.sim.ecs.components/->Frontier {}))
     :recall (be/get-component-type (fantasia.sim.ecs.components/->Recall {}))
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
    (be/get-component world entity-id comp-type)))

(defn ecs->agent-map
  "Convert agent entity to old-style map format."
  [ecs-world agent-id]
  (let [position (get-comp ecs-world agent-id :position)
        role (get-comp ecs-world agent-id :role)
        needs (get-comp ecs-world agent-id :needs)
        inventory (get-comp ecs-world agent-id :inventory)
        frontier (get-comp ecs-world agent-id :frontier)
        recall (get-comp ecs-world agent-id :recall)
        job-assignment (get-comp ecs-world agent-id :job-assignment)
        path (get-comp ecs-world agent-id :path)]
    {:id agent-id
     :pos [(:q position) (:r position)]
     :role (:type role)
     :needs {:warmth (:warmth needs) :food (:food needs) :sleep (:sleep needs)}
     :inventory {:wood (:wood inventory) :food (:food inventory)}
     :frontier (:facets frontier)
     :recall (:events recall)
     :current-job (:job-id job-assignment)
     :current-path (:waypoints path)}))

(defn ecs->agent-list
  "Convert all agent entities to old-style list format."
  [ecs-world]
  (map (partial ecs->agent-map ecs-world)
        (fantasia.sim.ecs.core/get-all-agents ecs-world)))

(defn ecs->tile-map
  "Convert tile entity to old-style map format."
  [ecs-world tile-id]
  (let [position (get-comp ecs-world tile-id :position)
        tile (get-comp ecs-world tile-id :tile)]
    {:pos [(:q position) (:r position)]
     :terrain (:terrain tile)
     :biome (:biome tile)
     :resource (:resource tile)
     :structure (:structure tile)}))

(defn ecs->tiles-map
   "Convert all tile entities to old-style map format keyed by position."
   [ecs-world]
   (reduce (fn [acc tile-id]
               (let [position (get-comp ecs-world tile-id :position)
                     tile-key (str (:q position) "," (:r position))
                     tile-data (ecs->tile-map ecs-world tile-id)]
                 (assoc acc tile-key tile-data)))
           {}
           (fantasia.sim.ecs.core/get-all-tiles ecs-world)))

(defn ecs->stockpiles-map
  "Convert stockpile entities to old-style map format."
  [ecs-world]
  (let [stockpile-type (get @component-types :stockpile)]
    (reduce (fn [acc entity-id]
              (let [stockpile (get-comp ecs-world entity-id :stockpile)
                    index (get-comp ecs-world entity-id :tile-index)]
                (assoc acc (str (:q index) "," (:r index)) (:contents stockpile))))
            {}
            (be/get-all-entities-with-component ecs-world stockpile-type))))

(defn ecs->snapshot
  "Convert ECS world to old-style snapshot for WebSocket broadcast."
  [ecs-world global-state]
  {:tick (:tick global-state)
   :shrine (:shrine global-state)
   :levers (:levers global-state)
   :map (:map global-state)
   :tiles (ecs->tiles-map ecs-world)
   :recent-events (:recent-events global-state)
   :attribution (:attribution global-state)
   :jobs (:jobs global-state)
   :items (:items global-state)
   :stockpiles (ecs->stockpiles-map ecs-world)
   :agents (ecs->agent-list ecs-world)
   :ledger (:ledger global-state)})
