(ns fantasia.sim.ecs.systems.simple-jobs
  (:require [brute.entity :as be]
            [fantasia.sim.ecs.components :as c]))

(defn process-simple-jobs
  "Process jobs for testing - assign jobs to idle agents."
  [ecs-world]
  (let [role-type (be/get-component-type (c/->Role :priest))
        role (be/get-component ecs-world role-type)
        status-type (be/get-component-type (c/->AgentStatus true false false nil))
        all-agents (be/get-all-entities-with-component ecs-world role-type)
        idle-agents (filter (fn [agent-id]
                             (let [status (be/get-component ecs-world agent-id status-type)]
                               (:idle? status)))
                          all-agents)]
    (println "Simple jobs: Found" (count idle-agents) "idle agents out of" (count all-agents))
    ecs-world))
