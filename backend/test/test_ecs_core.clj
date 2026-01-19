(ns test-ecs.core
  (:require [brute.entity :as be]
            [fantasia.sim.ecs.components :as c]))

(defn component-class [instance]
  (be/get-component-type instance))

(defn create-ecs-world []
  (be/create-system))

(defn create-agent [system id q r role]
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
  (let [entity-id (java.util.UUID/randomUUID)
        tile-key (str q "," r)
        system' (-> system
                      (be/add-entity entity-id)
                      (be/add-component entity-id (c/->Tile terrain biome structure resource))
                      (be/add-component entity-id (c/->TileIndex tile-key))
                      (be/add-component entity-id (c/->Position q r)))]
    [tile-key entity-id system']))

(defn get-all-agents [system]
  (let [role-instance (c/->Role :priest)
        role-type (component-class role-instance)]
    (be/get-all-entities-with-component system role-type)))

(println "ECS core loaded successfully!")