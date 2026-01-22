(ns fantasia.sim.tick
  (:require [fantasia.sim.tick.initial :as initial]
            [fantasia.sim.tick.core :as core]
            [fantasia.sim.tick.actions :as actions]
            [fantasia.sim.tick.movement :as movement]
            [fantasia.sim.tick.trees :as trees]))

(def rng initial/rng)
(def rand-int* initial/rand-int*)
(def ->agent initial/->agent)
(def initial-world initial/initial-world)
(def ^:dynamic tick-once core/tick-once)
(def ^:dynamic *state core/*state)
(def get-state core/get-state)
(def reset-world! core/reset-world!)
(def set-levers! core/set-levers!)
(def set-facet-limit! core/set-facet-limit!)
(def set-vision-radius! core/set-vision-radius!)
(def place-shrine! core/place-shrine!)
(def appoint-mouthpiece! core/appoint-mouthpiece!)
(def tick! core/tick!)

(def move-agent-with-job movement/move-agent-with-job)
(def assign-build-wall-job! actions/assign-build-wall-job!)
(def place-wall-ghost! actions/place-wall-ghost!)
(def place-stockpile! actions/place-stockpile!)
(def place-warehouse! actions/place-warehouse!)
(def place-campfire! actions/place-campfire!)
(def place-statue-dog! actions/place-statue-dog!)
(def place-tree! actions/place-tree!)
(def place-wolf! actions/place-wolf!)
(def place-deer! actions/place-deer!)
(def place-bear! actions/place-bear!)
(def queue-build-job! actions/queue-build-job!)
(def spawn-initial-trees! trees/spawn-initial-trees!)
(def spread-trees! trees/spread-trees!)
(def drop-tree-fruits! trees/drop-tree-fruits!)

(defn get-agent-path!
  "Calculate path from agent's current position to job target.
   Returns full path from current pos to target, or empty array if no path."
  [agent-id]
  (let [world (get-state)
        agent (get-in world [:agents agent-id])]
    (when agent
      (if-let [job (fantasia.sim.jobs/get-agent-job world agent-id)]
        (let [current-pos (:pos agent)
              job-target (fantasia.sim.jobs/job-target-pos world job)]
          (when job-target
            (if (not= current-pos job-target)
              (fantasia.sim.pathing/a-star-path world current-pos job-target)
              [])))))))
