(ns fantasia.sim.ecs.systems.movement
  (:require [brute.entity :as be]
             [fantasia.dev.logging :as log]
            [fantasia.sim.ecs.components :as c]
            [fantasia.sim.hex :as hex]))

(defn- waypoint->coords
  [waypoint]
  (cond
    (and (vector? waypoint) (= 2 (count waypoint))) waypoint
     (and (map? waypoint) (contains? waypoint :q) (contains? waypoint :r)) [(:q waypoint) (:r waypoint)]
     :else nil))

(defn- status-type []
  (be/get-component-type (c/->AgentStatus true false true nil)))

(defn- death-type []
  (be/get-component-type (c/->DeathState true nil nil)))

(defn- alive-agent?
  [ecs-world agent-id]
  (let [status (be/get-component ecs-world agent-id (status-type))
        death-state (be/get-component ecs-world agent-id (death-type))]
    (and (not= false (:alive? status true))
         (not= false (:alive? death-state true)))))

(defn move-agent-along-path
  "Move agent one step along its path waypoints."
  [ecs-world agent-id]
  (let [path-instance (c/->Path [] 0)
        path-type (be/get-component-type path-instance)
        position-instance (c/->Position 0 0)
        position-type (be/get-component-type position-instance)
        path-component (be/get-component ecs-world agent-id path-type)
        position (be/get-component ecs-world agent-id position-type)
        current-index (or (:current-index path-component) 0)
        waypoints (or (:waypoints path-component) [])]
    (if (or (nil? path-component)
            (nil? position)
            (not (alive-agent? ecs-world agent-id)))
      ecs-world
      (if (>= current-index (count waypoints))
        (be/remove-component ecs-world agent-id path-instance)
      (let [target-waypoint (nth waypoints current-index)
            [target-q target-r] (waypoint->coords target-waypoint)]
        (if (or (nil? target-q) (nil? target-r))
          (be/remove-component ecs-world agent-id path-instance)
          (let [next-pos (c/->Position target-q target-r)
                ecs-world' (be/add-component ecs-world agent-id next-pos)
                new-index (inc current-index)]
            (log/log-debug "[MOVE:AGENT]"
                           {:agent-id agent-id
                            :from [(:q position) (:r position)]
                            :to [target-q target-r]
                            :method "job-path"})
            (if (>= new-index (count waypoints))
              (be/remove-component ecs-world' agent-id path-instance)
              (be/add-component ecs-world' agent-id (c/->Path waypoints new-index))))))))))

(defn process
  "Process movement for all agents with Path or JobAssignment."
  ([ecs-world]
   (process ecs-world nil))
  ([ecs-world global-state]
   (let [path-instance (c/->Path [] 0)
         path-type (be/get-component-type path-instance)
         position-type (be/get-component-type (c/->Position 0 0))
         role-type (be/get-component-type (c/->Role :peasant))
         tick-value (when (and (map? global-state) (number? (:tick global-state)))
                      (long (:tick global-state)))
         agents-with-path (be/get-all-entities-with-component ecs-world path-type)
         world-after-path (reduce (fn [acc agent-id]
                                     (move-agent-along-path acc agent-id))
                                   ecs-world
                                   agents-with-path)
         all-agents (be/get-all-entities-with-component world-after-path role-type)]
     (reduce (fn [acc agent-id]
               (let [alive? (alive-agent? acc agent-id)
                     has-path? (some? (be/get-component acc agent-id path-type))
                      position (be/get-component acc agent-id position-type)
                      base-hash (Math/abs (hash (str agent-id)))
                      wander-step? (if (some? tick-value)
                                     (zero? (mod (+ tick-value base-hash) 7))
                                     true)]
                 (if (or (not alive?) has-path? (nil? position) (not wander-step?))
                    acc
                    (let [neighbors (vec (hex/neighbors [(:q position) (:r position)]))
                          idx (mod (if (some? tick-value)
                                    (+ tick-value (Math/abs (hash (str agent-id "-wander"))))
                                    (Math/abs (hash (str agent-id ":" (:q position) ":" (:r position)))))
                                  (count neighbors))
                         [next-q next-r] (nth neighbors idx)]
                     (log/log-debug "[MOVE:WANDER]"
                                    {:agent-id agent-id
                                     :from [(:q position) (:r position)]
                                     :to [next-q next-r]})
                     (be/add-component acc agent-id (c/->Position next-q next-r))))))
             world-after-path
             all-agents))))
