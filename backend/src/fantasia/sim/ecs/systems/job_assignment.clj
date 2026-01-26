(ns fantasia.sim.ecs.systems.job_assignment
   (:require [brute.entity :as be]
             [fantasia.sim.ecs.components :as c]))

(defn alive-agent?
  "Check if agent is alive."
  [status]
  (:alive? status))

(defn find-best-job
  "Find the best available job for an agent."
  [ecs-world agent-id pending-jobs]
  (let [role-type (be/get-component-type (c/->Role :priest))
        role (be/get-component ecs-world agent-id role-type)]
    (first (sort-by :priority
                   (filter #(= (:role %) role)
                           (filter #(not (:blocked %)) pending-jobs))))))

(defn claim-job!
   "Assign a job to an agent."
   [ecs-world agent-id job-id job-type job-queue]
   (let [role-type (be/get-component-type (c/->Role :priest))
          role (be/get-component ecs-world agent-id role-type)
          status-type (be/get-component-type (c/->AgentStatus true false false nil))
          job-assignment-type (be/get-component-type (c/->JobAssignment nil 0.0))
          ecs-world' (be/add-component ecs-world agent-id (c/->JobAssignment job-id 0.0))
          ecs-world'' (if job-type
                         (let [queue-type (be/get-component-type job-type)
                               queue (be/get-component ecs-world job-id queue-type)]
                           (-> ecs-world'
                               (be/add-component job-id (assoc queue :pending-jobs (conj (:pending-jobs queue) agent-id)))
                               (be/add-component job-id (assoc queue :claimed-count (inc (or (:claimed-count queue) 0))))
                               (be/add-component job-id (assoc queue :state :claimed))))
                         ecs-world')
          ecs-world''' (be/add-component ecs-world'' agent-id status-type)
          job (be/get-component ecs-world''' job-id job-type)
          target-pos (:target job)]
      (-> ecs-world'''
          (be/add-component agent-id (assoc (be/get-component ecs-world''' agent-id status-type) :idle? false))
          (be/add-component job-id (assoc job :worker-id agent-id :state :claimed))
          (fantasia.sim.ecs.core/set-agent-path agent-id [target-pos]))))

(defn mark-agent-idle!
  "Mark an agent as idle."
  [ecs-world agent-id]
  (be/add-component ecs-world agent-id
                     (assoc (be/get-component ecs-world agent-id
                                                   (be/get-component-type (c/->AgentStatus true false false nil)))
                            :idle? true :alive? true)))

(defn process
  "Process job assignment for all agents with idle status."
  [ecs-world global-state]
  (let [role-type (be/get-component-type (c/->Role :priest))
        all-agents (be/get-all-entities-with-component ecs-world role-type)
        status-type (be/get-component-type (c/->AgentStatus true false false nil))
        idle-agents (filter (fn [agent-id]
                             (let [status (be/get-component ecs-world agent-id status-type)]
                               (and (:idle? status) (alive-agent? status))))
                           all-agents)
        job-queue-type (be/get-component-type (c/->JobQueue [] {} {}))
        buildings-with-queues (be/get-all-entities-with-component ecs-world job-queue-type)]
    (reduce (fn [acc agent-id]
              (let [building-id (first (filter (fn [bid]
                                                  (let [job-queue (be/get-component ecs-world bid job-queue-type)]
                                                    (or (some #(= % agent-id) (:pending-jobs job-queue))
                                                        (some #(= % agent-id) (:assigned-jobs job-queue)))))
                                                  buildings-with-queues))
                     best-job (when building-id
                                   (let [job-queue (be/get-component ecs-world building-id job-queue-type)
                                         jobs (:jobs job-queue [])]
                                     (find-best-job acc agent-id jobs)))]
                 (if best-job
                   (claim-job! acc agent-id (:job-id best-job) (:job-type best-job) job-queue-type)
                   (mark-agent-idle! acc agent-id))))
            ecs-world
            idle-agents)))
