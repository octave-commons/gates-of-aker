(ns fantasia.sim.ecs.systems.job_processing
  (:require [brute.entity :as be]
            [fantasia.sim.ecs.components :as c]
            [fantasia.sim.hex :as hex]))

(defn process-job-for-agent
  "Process a single job for an agent adjacent to target."
  [ecs-world agent-id job-id]
  (let [job-assignment-type (be/get-component-type (c/->JobAssignment nil 0.0))
        job-assignment (be/get-component ecs-world agent-id job-assignment-type)
        position-type (be/get-component-type (c/->Position 0 0))
        position (be/get-component ecs-world agent-id position-type)
        target-pos (:target job-assignment)
        job-type (:type job-assignment)
        current-progress (:progress job-assignment)]
    (when (and target-pos (not (= 1.0 current-progress)))
      (if (hex/neighbors target-pos (position))
        (let [new-progress (min 1.0 (+ current-progress 0.1))]
          (be/add-component ecs-world agent-id (c/->JobAssignment job-id new-progress)))
        (be/remove-component ecs-world agent-id job-assignment-type))))

(defn process
  "Process job progress for all agents with jobs."
  [ecs-world global-state]
  (let [job-assignment-type (be/get-component-type (c/->JobAssignment nil 0.0))
        agents-with-jobs (be/get-all-entities-with-component ecs-world job-assignment-type)]
    (reduce (fn [acc agent-id]
              (let [job-assignment (be/get-component acc agent-id job-assignment-type)]
                (if job-assignment
                  (process-job-for-agent acc agent-id (:job-id job-assignment))
                  acc)))
            ecs-world
            agents-with-jobs))))
