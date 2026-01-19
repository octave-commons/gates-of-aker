(ns test_ecs2.core
  (:require [brute.entity :as be]
            [test_ecs2.components :as c]))

(defn create-ecs-world []
  "Create a new ECS world using Brute."
  (be/create-system))

(defn component-class [instance]
  (be/get-component-type instance))

(defn create-agent
  "Create an agent entity with standard components."
  [system id q r role]
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

(defn create-tile
  "Create a tile entity."
  [system q r terrain biome structure resource]
  (let [entity-id (java.util.UUID/randomUUID)
        tile-key (str q "," r)
        system' (-> system
                      (be/add-entity entity-id)
                      (be/add-component entity-id (c/->Tile terrain biome structure resource))
                      (be/add-component entity-id (c/->TileIndex tile-key))
                      (be/add-component entity-id (c/->Position q r)))]
    [tile-key entity-id system']))

(defn create-stockpile
  "Create a stockpile at given position."
  [system tile-key]
  (let [entity-id (java.util.UUID/randomUUID)
        system' (-> system
                      (be/add-entity entity-id)
                      (be/add-component entity-id (c/->Stockpile {}))
                      (be/add-component entity-id (c/->TileIndex tile-key)))]
    [entity-id system']))

(defn get-all-agents
  "Get all entities with Role component (agents)."
  [system]
  (let [role-instance (c/->Role :priest)
        role-type (component-class role-instance)]
    (be/get-all-entities-with-component system role-type)))

(defn get-all-tiles
  "Get all entities with Tile component."
  [system]
  (let [tile-instance (c/->Tile :ground nil nil nil)
        tile-type (component-class tile-instance)]
    (be/get-all-entities-with-component system tile-type)))

(defn get-tile-at-pos
  "Get tile entity ID at hex position."
  [system q r]
  (let [tile-key (str q "," r)
        tile-idx-instance (c/->TileIndex tile-key)
        tile-idx-type (component-class tile-idx-instance)
        tile-entities (be/get-all-entities-with-component system tile-idx-type)]
    (first (filter #(let [idx (be/get-component system % tile-idx-type)]
                       (= (:tile-key idx) tile-key))
                  tile-entities))))

(defn get-nearest-agent-with-component
  "Find nearest agent with specific component to position."
  [system pos component-cls]
  (let [role-instance (c/->Role :priest)
        role-type (component-class role-instance)
        agents (be/get-all-entities-with-component system role-type)
        pos-instance (c/->Position 0 0)
        pos-type (component-class pos-instance)]
    (->> (map (fn [agent-id]
                  [agent-id (be/get-component system agent-id pos-type)])
                agents)
         (filter #(some? (second %)))
         (sort-by #(let [[agent-id position] %]
                    (let [pos-q (:q position)
                          pos-r (:r position)
                          target-q (first pos)
                          target-r (second pos)]
                      (+ (* (- pos-q target-q) (- pos-q target-r))
                         (* (- pos-r target-r) (- pos-r target-r)))))
         first)))

(defn assign-job-to-agent
  "Assign a job to an agent entity."
  [system entity-id job-id]
  (be/add-component system entity-id (c/->JobAssignment job-id 0.0)))

(defn set-agent-path
  "Set path for agent movement."
  [system entity-id waypoints]
  (be/add-component system entity-id (c/->Path waypoints 0)))

(defn update-agent-needs
  "Update needs component for an agent."
  [system entity-id warmth food sleep]
  (be/add-component system entity-id (c/->Needs warmth food sleep)))

(defn update-agent-inventory
  "Update inventory component for an agent."
  [system entity-id wood food]
  (be/add-component system entity-id (c/->Inventory wood food)))

(defn remove-component
  "Remove a component from an entity."
  [system entity-id component-instance]
  (be/remove-component system entity-id component-instance))