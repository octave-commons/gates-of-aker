(ns fantasia.sim.test-helpers
  "Common test utilities and fixtures for ECS testing."
  (:require [brute.entity :as be]
            [fantasia.sim.ecs.core :as ecs]
            [fantasia.sim.ecs.components :as c]
            [fantasia.sim.constants :as const]))

(defn create-test-world
  "Create minimal test ECS world."
  []
  (ecs/create-ecs-world))

(defn create-test-agent
  "Create test agent with specified role and position.
   Returns [agent-id world]."
  ([world role q r]
   (ecs/create-agent world nil q r role))
  ([world role q r opts]
   (ecs/create-agent world nil q r role opts)))

(defn create-test-tiles
  "Create test tiles at specified positions.
   Each position is [q r terrain biome].
   Returns world."
  [world positions]
  (reduce (fn [w [q r terrain biome]]
            (second (ecs/create-tile w q r terrain biome nil nil)))
          world
          positions))

(defn create-test-building
  "Create test building with JobQueue.
   Returns [building-id world]."
  ([world [q r] structure-type]
   (ecs/create-building world [q r] structure-type))
  ([world [q r] structure-type opts]
   (ecs/create-building world [q r] structure-type opts)))

(defn create-test-world-with-agents
  "Create test world with agents at specified positions.
   Returns [world agent-ids] where agent-ids is map of role->id."
  [world agent-specs]
  (let [spec->world (fn [w [role q r opts]]
                        (let [[aid w'] (create-test-agent w role q r opts)]
                          [w' aid]))
        [world' ids] (reduce (fn [[w ids] [role q r opts]]
                               (let [[w' aid] (spec->world w [role q r opts])]
                                 [w' (assoc ids role aid)]))
                             [world {}]
                             agent-specs)]
    [world' ids]))

(defn get-component-by-type
  "Get component using component type from ECS world."
  [world entity-id component-instance]
  (let [component-type (ecs/component-class component-instance)]
    (be/get-component world entity-id component-type)))

(defn with-needs
  "Add or update Needs component for agent."
  [world agent-id warmth food sleep]
  (let [needs (c/->Needs warmth food sleep 1.0 0.8 0.6 0.5 0.5 0.5 0.6 0.5 0.5 0.5)]
    (be/add-component world agent-id needs)))

(defn with-frontend-facets
  "Add Frontier component with given facets map."
  [world agent-id facets]
  (let [frontier (c/->Frontier facets)]
    (be/add-component world agent-id frontier)))

(defn with-recall-events
  "Add Recall component with given events map."
  [world agent-id events]
  (let [recall (c/->Recall events)]
    (be/add-component world agent-id recall)))

(defn with-death-state
  "Add DeathState component to agent."
  [world agent-id alive? cause-of-death death-tick]
  (let [death-state (c/->DeathState alive? cause-of-death death-tick)]
    (be/add-component world agent-id death-state)))

(defn with-stats
  "Add Stats component to agent."
  [world agent-id strength fortitude charisma intelligence]
  (let [stats (c/->Stats strength fortitude charisma intelligence)]
    (be/add-component world agent-id stats)))

(defn with-job-assignment
  "Add JobAssignment component to agent."
  [world agent-id job-id progress]
  (let [job-assignment (c/->JobAssignment job-id progress)]
    (be/add-component world agent-id job-assignment)))

(defn with-path
  "Add Path component to agent."
  [world agent-id waypoints current-index]
  (let [path (c/->Path waypoints current-index)]
    (be/add-component world agent-id path)))

(defn create-test-stockpile
  "Create test stockpile at position.
   Returns [stockpile-id world]."
  [world q r contents]
  (let [[sid w] (ecs/create-stockpile world q r)]
    [sid (be/add-component w sid (c/->Stockpile contents))]))

(defn create-test-world-item
  "Create test world item at position.
   Returns [item-id world]."
  [world q r resource qty tick]
  (ecs/create-world-item world q r resource qty tick))

(defn setup-simple-tick-world
  "Create a simple test world with tick state.
   Returns world with minimal setup for tick testing."
  []
  (let [world (create-test-world)
        [_ world] (create-test-agent world :priest 0 0)
        [_ world] (create-test-agent world :knight 1 0)
        [_ world] (create-test-agent world :peasant 0 1)]
    world))
