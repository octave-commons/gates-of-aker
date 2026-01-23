(ns fantasia.sim.ecs.systems.job_assignment
  (:require [brute.entity :as be]
            [fantasia.sim.ecs.components :as c]
            [fantasia.sim.hex :as hex]))

(def job-priorities
  {:job/eat 100
   :job/warm-up 95
   :job/build-fire 70
   :job/sleep 90
   :job/hunt 75
   :job/chop-tree 60
   :job/mine 60
   :job/harvest-wood 58
   :job/harvest-fruit 58
   :job/harvest-grain 58
   :job/harvest-stone 58
   :job/farm 58
   :job/smelt 57
   :job/build-house 55
   :job/improve 52
   :job/haul 50
   :job/deliver-food 45
   :job/build-wall 40
   :job/builder 38
   :job/build-structure 35
   :job/scribe 40})

(defn player-agent?
  "Check if agent has player faction."
  [role-component]
  (= (:type role-component) :player))

(defn alive-agent?
  "Check if agent status indicates alive."
  [status-component]
  (:alive? status-component))

(defn get-job-priority
  "Get priority value for job type."
  [job-type]
  (get job-priorities job-type 50))

(defn sort-jobs-by-priority
  "Sort jobs by priority (higher first) then distance."
  [jobs agent-pos]
  (sort-by (fn [job]
             [(- (get-job-priority (:type job)) 0)
              (hex/distance agent-pos (:target job))])
           jobs))

(defn find-best-job
  "Find the best available job for an agent."
  [ecs-world agent-id job-type job-queue pending-jobs]
  (let [position-type (be/get-component-type (c/->Position 0 0))
        position (be/get-component ecs-world agent-id position-type)
        role-type (be/get-component-type (c/->Role :priest))
        role (be/get-component ecs-world agent-id role-type)
        status-type (be/get-component-type (c/->AgentStatus true false false nil))
        status (be/get-component ecs-world agent-id status-type)
        idle? (and (:idle? status) (alive-agent? status))
        player? (player-agent? role)]
          (when (and idle? player?)
        (let [available-jobs (filter (fn [j] (and (= (:state j) :pending)
                                                       (or (nil? (:type-limit j))
                                                           (< (:claimed-count j) (:type-limit j))))
                                    pending-jobs)
              best-job (first (sort-jobs-by-priority available-jobs position))]
          best-job))))

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
                             (be/add-component job-id (assoc queue :pending-jobs (conj (:pending-jobs queue) agent-id))
                             (be/add-component job-id (assoc queue :claimed-count (inc (or (:claimed-count queue) 0)))
                             (be/add-component job-id (assoc queue :state :claimed)))
                       ecs-world'))
        ecs-world''' (be/add-component ecs-world'' agent-id status-type)]
    (-> ecs-world'''
        (be/add-component agent-id (assoc (be/get-component ecs-world''' agent-id status-type) :idle? false))
        (be/add-component job-id (assoc (be/get-component ecs-world''' job-id job-type) :worker-id agent-id :state :claimed)))))

(defn mark-agent-idle!
  "Mark an agent as idle."
  [ecs-world agent-id]
  (let [status-type (be/get-component-type (c/->AgentStatus true false false nil))
        current-status (be/get-component ecs-world agent-id status-type)]
    (when (:idle? current-status)
      (be/add-component ecs-world agent-id (assoc current-status :idle? true :alive? true)))))

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
        job-queue-type (be/get-component-type (c/->JobQueue [] {}))
        buildings-with-queues (be/get-all-entities-with-component ecs-world job-queue-type)]
    (reduce (fn [acc agent-id]
              (let [building-id (first (filter (fn [bid]
                                                 (let [job-queue (be/get-component ecs-world bid job-queue-type)]
                                                   (or (some #(= % agent-id) (:pending-jobs job-queue))
                                                       (some #(= % agent-id) (:assigned-jobs job-queue))))
                                                 buildings-with-queues))
                    best-job (when building-id
                                 (let [job-queue (be/get-component ecs-world building-id job-queue-type)
                                       jobs (:jobs job-queue [])]
                                   (find-best-job acc agent-id job-queue-type jobs)))]
                (if best-job
                  (claim-job! acc agent-id (:job-id best-job) (:job-type best-job) job-queue)
                  (mark-agent-idle! acc agent-id))))
            ecs-world
            idle-agents)))
