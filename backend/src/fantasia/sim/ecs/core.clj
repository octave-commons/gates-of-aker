(ns fantasia.sim.ecs.core
  (:require [brute.entity :as be]
            [fantasia.sim.constants :as const]
            [fantasia.sim.ecs.components :as c]))

(defn component-class
  "Get the component class/type for a record instance."
  [instance]
  (be/get-component-type instance))

(defn create-ecs-world
  "Create a new ECS world using Brute."
  []
  (be/create-system))

(defn tile-key [[q r]] [q r])
(defn parse-tile-key [s] s)

(defn- matching-component-type
  [system component-type]
  (let [target-name (.getName ^Class component-type)
        component-map (:entity-components system {})]
    (or (when (contains? component-map component-type)
          component-type)
        (some (fn [k]
                (when (and (instance? Class k)
                           (= target-name (.getName ^Class k)))
                  k))
              (keys component-map)))))

(defn get-component-safe
  "Read a component for an entity by component instance/type, with class-name fallback."
  [system entity-id component-instance]
  (let [component-type (if (instance? Class component-instance)
                         component-instance
                         (be/get-component-type component-instance))
        exact (be/get-component system entity-id component-type)]
    (if (some? exact)
      exact
      (when-let [resolved-type (matching-component-type system component-type)]
        (be/get-component system entity-id resolved-type)))))

(defn get-all-entities-with-component-safe
  "Get entity IDs that contain component instance/type, with class-name fallback."
  [system component-instance]
  (let [component-type (if (instance? Class component-instance)
                         component-instance
                         (be/get-component-type component-instance))
        direct (be/get-all-entities-with-component system component-type)]
    (if (seq direct)
      direct
      (if-let [resolved-type (matching-component-type system component-type)]
        (be/get-all-entities-with-component system resolved-type)
        []))))

(defn create-agent
   "Create an agent entity with standard components."
   ([system id q r role]
    (create-agent system id q r role {}))
   ([system id q r role opts]
       (let [system (or system (create-ecs-world))  ; Guard against nil system
             entity-id (or id (java.util.UUID/randomUUID))
             {:keys [warmth food sleep wood needs status inventory frontier recall path job-id]} opts
             needs' (or needs (c/->Needs (or warmth 0.8) (or food 0.7) (or sleep 0.6) 1.0 0.8 0.6 0.5 0.5 0.5 0.6 0.5 0.5 0.5))
             status' (or status (c/->AgentStatus true false true nil))
             inventory' (or inventory (c/->PersonalInventory (or wood 0) (or food 0) {}))
             frontier' (or frontier (c/->Frontier {}))
             recall' (or recall (c/->Recall {}))
             base-system (-> system
                            (be/add-entity entity-id)
                            (be/add-component entity-id (c/->AgentInfo entity-id (str "agent-" entity-id)))
                            (be/add-component entity-id (c/->Position q r))
                            (be/add-component entity-id (c/->Role role))
                            (be/add-component entity-id needs')
                            (be/add-component entity-id inventory')
                            (be/add-component entity-id status')
                            (be/add-component entity-id frontier')
                            (be/add-component entity-id recall'))]
        [entity-id
         (cond-> base-system
           job-id (be/add-component entity-id (c/->JobAssignment job-id 0.0))
           path (be/add-component entity-id (c/->Path path 0)))])))

(defn get-all-agents
  "Get all agent entities from the ECS world."
  [system]
  (get-all-entities-with-component-safe system (c/->Role :peasant)))

(defn get-all-tiles
  "Get all tile entities from the ECS world."
  [system]
  (get-all-entities-with-component-safe system (c/->TileIndex 0 0)))

(defn get-tile-at-pos
  "Get tile entity ID at specific position."
  [system pos]
  (let [tile-index-instance (c/->TileIndex 0 0)
        tile-entities (get-all-entities-with-component-safe system tile-index-instance)]
    (some (fn [entity-id]
            (when-let [tile-index (get-component-safe system entity-id tile-index-instance)]
              (when (= [(:q tile-index) (:r tile-index)] pos)
                entity-id)))
          tile-entities)))

(defn has-component?
  "Check if entity has specific component type."
  [system entity-id component-type]
  (some? (be/get-component system entity-id component-type)))

(defn remove-component
  "Remove a component from an entity."
  [system entity-id component-instance]
  (be/remove-component (or system (create-ecs-world)) entity-id component-instance))

(defn assign-job-to-agent
  "Assign a job to an agent entity."
  [system entity-id job-id]
  (be/add-component (or system (create-ecs-world)) entity-id (c/->JobAssignment job-id 0.0)))

(defn set-agent-path
  "Set path for agent movement."
  [system entity-id waypoints]
  (be/add-component (or system (create-ecs-world)) entity-id (c/->Path waypoints 0)))

(defn update-agent-needs
  "Update needs component for an agent."
  [system entity-id warmth food sleep]
  (be/add-component (or system (create-ecs-world)) entity-id (c/->Needs warmth food sleep 1.0 0.8 0.6 0.5 0.5 0.5 0.6 0.5 0.5 0.5)))

(defn update-agent-inventory
  "Update inventory component for an agent."
  [system entity-id wood food]
  (be/add-component (or system (create-ecs-world)) entity-id (c/->PersonalInventory wood food {})))

(defn create-tile
   "Create a tile entity with optional components."
   ([system q r terrain biome structure resource]
     (create-tile system q r terrain biome structure resource {}))
   ([system q r terrain biome structure resource opts]
       (let [system (or system (create-ecs-world))  ; Guard against nil system
             entity-id (java.util.UUID/randomUUID)
            {:keys [tile-resources structure-state]} opts
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
  (get-all-entities-with-component-safe system (c/->JobQueue {} [] {})))

(defn get-all-world-items
  "Get all WorldItem entities."
  [system]
  (get-all-entities-with-component-safe system (c/->WorldItem :log 1 [0 0] 0)))

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
                        (be/add-component entity-id (c/->JobQueue {} [] {})))
           system'' (cond-> system'
                      stockpile-config (be/add-component entity-id
                                                         (c/->Stockpile {(:resource stockpile-config) 0})))]
       [entity-id system''])))
