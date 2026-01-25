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

(defn create-agent
   "Create an agent entity with standard components."
   ([system id q r role]
    (create-agent system id q r role {}))
   ([system id q r role opts]
       (let [system (or system (create-ecs-world))  ; Guard against nil system
             entity-id (or id (java.util.UUID/randomUUID))
             {:keys [warmth food sleep wood needs status inventory frontier recall path job-id]} opts
             needs' (or needs (c/->Needs (or warmth 0.8) (or food 0.7) (or sleep 0.6) 1.0 0.8 0.6 0.5 0.5 0.5 0.6 0.5 0.5 0.5))
             status' (or status (c/->AgentStatus true false false nil))
             inventory' (or inventory (c/->PersonalInventory (or wood 0) (or food 0) {}))
             frontier' (or frontier (c/->Frontier {}))
             recall' (or recall (c/->Recall {}))
            base-system (-> system
                           (be/add-entity entity-id)
                           (be/add-component entity-id (c/->AgentInfo id (str "agent-" id)))
                           (be/add-component entity-id (c/->Position q r))
                           (be/add-component entity-id (c/->Role role))
                           (be/add-component entity-id needs')
                           (be/add-component entity-id inventory')
                           (be/add-component entity-id status')
                           (be/add-component entity-id frontier')
                           (be/add-component entity-id recall'))]
        (let [system' (cond-> base-system
                       job-id (be/add-component entity-id (c/->JobAssignment job-id 0.0))
                       path (be/add-component entity-id (c/->Path path 0)))]
          [entity-id system']))))

(defn get-all-agents
  "Get all agent entities from the ECS world."
  [system]
  (let [all-components (:entity-components system)
        role-components (get all-components fantasia.sim.ecs.components.Role)
        agent-ids (keys role-components)]
    agent-ids))

(defn get-all-tiles
  "Get all tile entities from the ECS world."
  [system]
  (let [all-entities (be/get-all-entities system)
        tile-entities (filter #(be/get-component system % fantasia.sim.ecs.components.TileIndex) all-entities)]
    tile-entities))

(defn get-tile-at-pos
  "Get tile entity ID at specific position."
  [system pos]
  (let [all-entities (be/get-all-entities system)
        tile-entities (filter #(be/get-component system % fantasia.sim.ecs.components.TileIndex) all-entities)]
    (some #(when-let [tile-index (be/get-component system % fantasia.sim.ecs.components.TileIndex)]
                   (= [(:q tile-index) (:r tile-index)] pos))
           tile-entities)))

(defn has-component?
  "Check if entity has specific component type."
  [system entity-id component-type]
  (some? (be/get-component system entity-id component-type)))

(defn remove-component
  "Remove a component from an entity."
  [system entity-id component-instance]
  (be/remove-component (or system (create-ecs-world)) entity-id component-instance))

(defn assign-job-to-agent [system entity-id job-id]
  "Assign a job to an agent entity."
  (be/add-component (or system (create-ecs-world)) entity-id (c/->JobAssignment job-id 0.0)))

(defn set-agent-path [system entity-id waypoints]
  "Set path for agent movement."
  (be/add-component (or system (create-ecs-world)) entity-id (c/->Path waypoints 0)))

(defn update-agent-needs [system entity-id warmth food sleep]
  "Update needs component for an agent."
  (be/add-component (or system (create-ecs-world)) entity-id (c/->Needs warmth food sleep 1.0 0.8 0.6 0.5 0.5 0.5 0.6 0.5 0.5 0.5)))

(defn update-agent-inventory [system entity-id wood food]
  "Update inventory component for an agent."
  (be/add-component (or system (create-ecs-world)) entity-id (c/->PersonalInventory wood food {})))

(defn create-tile
   "Create a tile entity with optional components."
   ([system q r terrain biome structure resource]
     (create-tile system q r terrain biome structure resource {}))
   ([system q r terrain biome structure resource opts]
      (let [system (or system (create-ecs-world))  ; Guard against nil system
            entity-id (java.util.UUID/randomUUID)
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

(defn get-buildings-with-job-queue
  "Get all building entities with JobQueue component."
  [system]
  (let [job-queue-instance (c/->JobQueue [] {} {})
        job-queue-type (component-class job-queue-instance)]
        (be/get-all-entities-with-component system job-queue-type)))

(defn get-all-world-items [system]
  "Get all WorldItem entities."
  (let [item-instance (c/->WorldItem :log 1 [0 0] 0)
        item-type (component-class item-instance)]
    (be/get-all-entities-with-component system item-type)))

(defn create-building
   "Create a building entity (job provider) with JobQueue."
   ([system [q r] structure-type]
     (create-building system [q r] structure-type {}))
   ([system [q r] structure-type opts]
      (let [system (or system (create-ecs-world))  ; Guard against nil system
            entity-id (java.util.UUID/randomUUID)
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
                        (be/add-component entity-id (c/->JobQueue [] {} {})))
           system'' (cond-> system'
                      stockpile-config (be/add-component entity-id
                                                         (c/->Stockpile {(:resource stockpile-config) 0})))]
       [entity-id system''])))
