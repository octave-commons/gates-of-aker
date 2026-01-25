(ns fantasia.sim.ecs.systems.movement
  (:require [brute.entity :as be]
            [fantasia.sim.ecs.components :as c]
            [fantasia.sim.hex :as hex]
            [fantasia.dev.logging :as log]))

(defn move-agent-along-path
  "Move agent one step along its path waypoints."
  [ecs-world agent-id]
  (let [path-instance (c/->Path [] 0)
        path-type (be/get-component-type path-instance)
        position-instance (c/->Position 0 0)
        position-type (be/get-component-type position-instance)
        path-component (be/get-component ecs-world agent-id path-type)
        position (be/get-component ecs-world agent-id position-type)
        current-index (:current-index path-component)
        waypoints (:waypoints path-component)]
    (when (< current-index (count waypoints))
      (let [target-pos (nth waypoints current-index)
            ecs-world' (be/add-component ecs-world agent-id target-pos)
            new-index (inc current-index)]
        (log/log-debug "[MOVE:AGENT]" {:agent-id agent-id :from (:q position) (:r position) :to (:q target-pos) (:r target-pos) :method "job-path"})
        (if (= new-index (count waypoints))
          (let [path-inst (c/->Path [] 0)
                path-type (be/get-component-type path-inst)]
            (be/remove-component ecs-world' agent-id path-type))
          (be/add-component ecs-world' agent-id (c/->Path waypoints new-index)))))))

(defn process
  "Process movement for all agents with Path or JobAssignment."
  [ecs-world]
  (let [path-instance (c/->Path [] 0)
        path-type (be/get-component-type path-instance)
        agents-with-path (be/get-all-entities-with-component ecs-world path-type)]
    (reduce (fn [acc agent-id]
              (move-agent-along-path acc agent-id))
            ecs-world
            agents-with-path)))