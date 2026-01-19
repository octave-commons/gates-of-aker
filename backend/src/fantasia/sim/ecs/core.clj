(ns fantasia.sim.ecs.core
  (:require [brute.entity :as be]
            [fantasia.sim.ecs.components :as c]))

(defn component-class [instance]
  "Get the component class/type for a record instance."
  (be/get-component-type instance))

(defn create-ecs-world []
  "Create a new ECS world using Brute."
  (be/create-system))

(defn create-agent [system id q r role]
  "Create an agent entity with standard components."
  (let [entity-id (java.util.UUID/randomUUID)
        system' (-> system
                      (be/add-entity entity-id)
                      (be/add-component entity-id (c/->Agent (str "agent-" id)))
                      (be/add-component entity-id (c/->Position q r))
                      (be/add-component entity-id (c/->Role role))
                      (be/add-component entity-id (c/->Needs 0.6 0.7 0.7))
                      (be/add-component entity-id (c/->Inventory 0 0))
                      (be/add-component entity-id (c/->Frontier {}))
                      (be/add-component entity-id (c/->Recall {})))]
    [entity-id system']))

(defn create-tile [system q r terrain biome structure resource]
  "Create a tile entity."
  (let [entity-id (java.util.UUID/randomUUID)
        tile-key (str q "," r)
        system' (-> system
                      (be/add-entity entity-id)
                      (be/add-component entity-id (c/->Tile terrain biome structure resource))
                      (be/add-component entity-id (c/->TileIndex tile-key))
                      (be/add-component entity-id (c/->Position q r)))]
    [tile-key entity-id system']))

(defn create-stockpile [system tile-key]
  "Create a stockpile at given position."
  (let [entity-id (java.util.UUID/randomUUID)
        system' (-> system
                      (be/add-entity entity-id)
                      (be/add-component entity-id (c/->Stockpile {}))
                      (be/add-component entity-id (c/->TileIndex tile-key)))]
    [entity-id system']))
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

(defn get-tile-at-pos [system q r]
  "Get tile entity ID at hex position."
  (let [tile-key (str q "," r)
        tile-idx-instance (c/->TileIndex tile-key)
        tile-idx-type (component-class tile-idx-instance)
        tile-entities (be/get-all-entities-with-component system tile-idx-type)]
    (first (filter #(let [idx (be/get-component system % tile-idx-type)]
                       (= (:entity-id idx) tile-key))
                  tile-entities))))

(defn assign-job-to-agent [system entity-id job-id]
  "Assign a job to an agent entity."
  (be/add-component system entity-id (c/->JobAssignment job-id 0.0)))

(defn set-agent-path [system entity-id waypoints]
  "Set path for agent movement."
  (be/add-component system entity-id (c/->Path waypoints 0)))

(defn update-agent-needs [system entity-id warmth food sleep]
  "Update needs component for an agent."
  (be/add-component system entity-id (c/->Needs warmth food sleep)))

(defn update-agent-inventory [system entity-id wood food]
  "Update inventory component for an agent."
  (be/add-component system entity-id (c/->Inventory wood food)))

(defn remove-component [system entity-id component-instance]
  "Remove a component from an entity."
  (be/remove-component system entity-id component-instance))
