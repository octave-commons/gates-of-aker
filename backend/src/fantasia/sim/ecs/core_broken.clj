(ns fantasia.sim.ecs.core
  (:require [brute.entity :as be]
            [fantasia.sim.ecs.components :as c]
            [fantasia.sim.constants :as const]))

(defn component-class [instance]
  "Get the component class/type for a record instance."
  (be/get-component-type instance))

(defn create-ecs-world []
  "Create a new ECS world using Brute."
  (be/create-system))

(defn tile-key [[q r]] [q r])
(defn parse-tile-key [s] s)


(defn create-tile
   "Create a tile entity with optional components."
   ([system q r terrain biome structure resource]
    (create-tile system q r terrain biome structure resource {}))
   ([system q r terrain biome structure resource opts]
     (let [entity-id (java.util.UUID/randomUUID)
           {:keys [tile-resources structure-state campfire-state shrine-state]} opts
           base-system (-> system
                          (be/add-entity entity-id)
                          (be/add-component entity-id (c/->Tile terrain biome structure resource))
                          (be/add-component entity-id (c/->TileIndex q r)))
           system' (cond-> base-system
                      tile-resources (be/add-component entity-id tile-resources)
                      structure-state (be/add-component entity-id structure-state)
                      (= structure :campfire) (be/add-component entity-id (c/->CampfireState const/campfire-radius true (:tick system)))
                      (= structure :shrine) (be/add-component entity-id (c/->ShrineState nil)))]
       [(vector q r) entity-id system'])))

(defn create-building
   "Create a building entity (job provider) with JobQueue."
   ([system [q r] structure-type]
     (create-building system [q r] structure-type {}))
   ([system [q r] structure-type opts]
     (let [entity-id (java.util.UUID/randomUUID)
           {:keys [level health owner-id stockpile-config]} opts
           structure-state (c/->StructureState
                            (or level 1)
                            (or health 100)
                            (or health 100)
                            owner-id)
           system' (-> system
                       (be/add-entity entity-id)
                       (be/add-component entity-id (c/->Position q r))
                       (be/add-component entity-id (c/->TileIndex q r))
                       (be/add-component entity-id (c/->Tile :ground :plains structure-type nil))
                       (be/add-component entity-id structure-state)
                       (be/add-component entity-id (c/->JobQueue [] {})))
           system'' (cond-> system'
                      stockpile-config (be/add-component entity-id
                                                         (c/->Stockpile {(:resource stockpile-config) 0})))]
       [entity-id system''])))

(defn create-stockpile
   "Create a stockpile entity at given position."
    [system q r]
    (let [entity-id (java.util.UUID/randomUUID)
           system' (-> system
                       (be/add-entity entity-id)
                       (be/add-component entity-id (c/->TileIndex q r))
                       (be/add-component entity-id (c/->Stockpile {:log 0})))]
       [entity-id (vector q r) system']))

(defn create-world-item
   "Create a dropped item entity."
   [system q r resource qty tick]
   (let [entity-id (java.util.UUID/randomUUID)
          system' (-> system
                        (be/add-entity entity-id)
                        (be/add-component entity-id (c/->Position q r))
                        (be/add-component entity-id (c/->WorldItem resource qty [q r] tick)))]
      [entity-id (vector q r) system']))

(defn get-all-agents [system]
  "Get all entities with Role component (agents)."
  (let [role-instance (c/->Role :priest)
        role-type (component-class role-instance)]
    (be/get-all-entities-with-component system role-type)))

(defn get-all-tiles [system]
  "Get all entities with Tile component."
  (let [tile-instance (c/->Tile :ground nil nil nil)
        tile-type (component-class tile-instance)]
    (be/get-all-entities-with-component system tile-type)))

(defn get-tile-at-pos
  "Get tile entity ID at hex position using vector key."
  [system [q r]]
   (let [index-instance (c/->TileIndex q r)
        index-type (component-class index-instance)
        index-entities (be/get-all-entities-with-component system index-type)]
     (first (filter #(= [q r] [(:q (be/get-component system % index-type))
                               (:r (be/get-component system % index-type))])
                   index-entities))))

(defn get-buildings-with-job-queue
  "Get all building entities with JobQueue component."
  [system]
  (let [job-queue-instance (c/->JobQueue [] {})
        job-queue-type (component-class job-queue-instance)]
        (be/get-all-entities-with-component system job-queue-type)))

(defn get-all-world-items [system]
  "Get all WorldItem entities."
  (let [item-instance (c/->WorldItem :log 1 [0 0] 0)
        item-type (component-class item-instance)]
    (be/get-all-entities-with-component system item-type)))

(defn assign-job-to-agent [system entity-id job-id]
  "Assign a job to an agent entity."
  (be/add-component system entity-id (c/->JobAssignment job-id 0.0)))

(defn set-agent-path [system entity-id waypoints]
  "Set path for agent movement."
  (be/add-component system entity-id (c/->Path waypoints 0)))

(defn update-agent-needs [system entity-id warmth food sleep]
  "Update needs component for an agent."
  (be/add-component system entity-id (c/->Needs warmth food sleep 1.0 0.8 0.6 0.5 0.5 0.5 0.6 0.5 0.5 0.5)))

(defn update-agent-inventory [system entity-id wood food]
  "Update inventory component for an agent."
  (be/add-component system entity-id (c/->PersonalInventory wood food {})))

(defn remove-component [system entity-id component-instance]
  "Remove a component from an entity."
  (be/remove-component system entity-id component-instance))

(defn has-component?
  "Check if entity has a specific component type."
  [system entity-id component-type]
  (some? (be/get-component system entity-id component-type)))
