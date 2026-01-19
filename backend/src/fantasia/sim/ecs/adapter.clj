(ns fantasia.sim.ecs.adapter
  (:require [brute.entity :as be]
            [fantasia.sim.ecs.core]
            [fantasia.sim.ecs.components]))

(defn ecs->agent-map
  "Convert agent entity to old-style map format."
  [ecs-world agent-id]
  (let [pos-arg (fantasia.sim.ecs.components/->Position [0 0]))
        pos-type (be/get-component-type pos-arg)
        position (be/get-component ecs-world agent-id pos-type)
        role-arg (fantasia.sim.ecs.components/->Role [:priest]))
        role-type (be/get-component-type role-arg)
        role (be/get-component ecs-world agent-id role-type)
        needs-arg (fantasia.sim.ecs.components/->Needs [0.6 0.7 0.7]))
        needs-type (be/get-component-type needs-arg)
        needs (be/get-component ecs-world agent-id needs-type)
        inv-arg (fantasia.sim.ecs.components/->Inventory [0 0]))
        inv-type (be/get-component-type inv-arg)
        inventory (be/get-component ecs-world agent-id inv-type)
        front-arg (fantasia.sim.ecs.components/->Frontier [{}]))
        front-type (be/get-component-type front-arg)
        frontier (be/get-component ecs-world agent-id front-type)
        rec-arg (fantasia.sim.ecs.components/->Recall [{}]))
        rec-type (be/get-component-type rec-arg)
        recall (be/get-component ecs-world agent-id rec-type)
        job-arg (fantasia.sim.ecs.components/->JobAssignment [nil 0.0]))
        job-type (be/get-component-type job-arg)
        job-assignment (be/get-component ecs-world agent-id job-type)
        path-arg (fantasia.sim.ecs.components/->Path [[]]))
        path-type (be/get-component-type path-arg)
        path (be/get-component ecs-world agent-id path-type)]
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
  (let [pos-arg (fantasia.sim.ecs.components/->Position 0 0)
        pos-type (be/get-component-type pos-arg)
        position (be/get-component ecs-world tile-id pos-type)
        tile-arg (fantasia.sim.ecs.components/->Tile :ground nil nil nil))
        tile-type (be/get-component-type tile-arg)
        tile (be/get-component ecs-world tile-id tile-type)]
    {:pos [(:q position) (:r position)]
     :terrain (:terrain tile)
     :biome (:biome tile)
     :resource (:resource tile)
     :structure (:structure tile)}))

(defn ecs->tiles-map
  "Convert all tile entities to old-style map format keyed by position."
  [ecs-world]
  (reduce (fn [acc tile-id]
              (let [pos-arg (fantasia.sim.ecs.components/->Position 0 0)
                    pos-type (be/get-component-type pos-arg)
                    position (be/get-component ecs-world tile-id pos-type)
                    tile-key (str (:q position) "," (:r position))
                    tile-data (ecs->tile-map ecs-world tile-id)]
                (assoc acc tile-key tile-data)))
          {}
          (fantasia.sim.ecs.core/get-all-tiles ecs-world)))

(defn ecs->stockpiles-map
  "Convert stockpile entities to old-style map format."
  [ecs-world]
  (let [stockpile-arg (fantasia.sim.ecs.components/->Stockpile {})
        stockpile-type (be/get-component-type stockpile-arg)]
    (reduce (fn [acc entity-id]
              (let [stockpile (be/get-component ecs-world entity-id stockpile-type)
                    idx-arg (fantasia.sim.ecs.components/->TileIndex "0,0"))
                    idx-type (be/get-component-type idx-arg)
                    index (be/get-component ecs-world entity-id idx-type)]
                (assoc acc (:tile-key index) (:contents stockpile))))
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