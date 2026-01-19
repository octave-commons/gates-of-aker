(ns fantasia.sim.ecs.systems.movement
  (:require [brute.entity :as be]
            [fantasia.sim.ecs.components :as c]
            [fantasia.sim.hex :as hex]))

(defn move-agent-along-path
  "Move agent one step along its path waypoints."
  [ecs-world agent-id]
  (let [path-component (be/get-component ecs-world agent-id c/Path)
        position (be/get-component ecs-world agent-id c/Position)
        current-index (:current-index path-component)
        waypoints (:waypoints path-component)]
    (when (< current-index (count waypoints))
      (let [[q r] (nth waypoints current-index)
            ecs-world' (be/add-component ecs-world agent-id (c/->Position q r))
            new-index (inc current-index)]
        (if (= new-index (count waypoints))
          (be/remove-component ecs-world' agent-id c/Path)
          (be/add-component ecs-world' agent-id (c/->Path waypoints new-index)))))))

(defn process-movement
  "Process movement for all agents with Path or JobAssignment."
  [ecs-world]
  (let [agents-with-path (be/get-all-entities-with-component ecs-world c/Path)]
    (reduce (fn [acc agent-id]
              (move-agent-along-path acc agent-id))
            ecs-world
            agents-with-path)))