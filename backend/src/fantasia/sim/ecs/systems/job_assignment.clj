(ns fantasia.sim.ecs.systems.job-assignment
  (:require [brute.entity :as be]
            [fantasia.dev.logging :as log]
            [fantasia.sim.ecs.components :as c]
            [fantasia.sim.ecs.core :as ecs-core]))

(defn- status-type []
  (be/get-component-type (c/->AgentStatus true false false nil)))

(defn- death-type []
  (be/get-component-type (c/->DeathState true nil nil)))

(defn- role-type []
  (be/get-component-type (c/->Role :priest)))

(defn- job-assignment-type []
  (be/get-component-type (c/->JobAssignment nil 0.0)))

(defn- job-queue-type []
  (be/get-component-type (c/->JobQueue {} [] {})))

(defn- alive-agent?
  [ecs-world agent-id status]
  (let [death-state (be/get-component ecs-world agent-id (death-type))]
    (and status
         (not= false (:alive? status true))
         (not= false (:alive? death-state true)))))

(defn- idle-agent?
  [ecs-world agent-id status]
  (and status (:idle? status false) (alive-agent? ecs-world agent-id status)))

(defn- job-pending?
  [job]
  (let [state (:state job :pending)]
    (and (not (:blocked job false))
         (not (:worker-id job))
         (contains? #{:pending :queued} state))))

(defn- gather-pending-jobs
  [ecs-world]
  (let [queue-t (job-queue-type)
        building-ids (be/get-all-entities-with-component ecs-world queue-t)]
    (mapcat (fn [building-id]
              (let [queue (be/get-component ecs-world building-id queue-t)
                    jobs (:jobs queue {})]
                (->> jobs
                     (filter (fn [[_ job]] (job-pending? job)))
                     (map (fn [[job-id job]]
                            {:building-id building-id
                             :job-id job-id
                             :job job})))))
            building-ids)))

(defn- find-best-job
  [ecs-world _agent-id]
  (->> (gather-pending-jobs ecs-world)
       (sort-by (fn [{:keys [job]}]
                  (- (long (:priority job 0)))))
       first))

(defn- update-job-in-queue
  [ecs-world building-id job-id f]
  (let [queue-t (job-queue-type)
        queue (be/get-component ecs-world building-id queue-t)
        jobs (:jobs queue {})
        updated-job (f (get jobs job-id))
        updated-jobs (assoc jobs job-id updated-job)
        assigned-jobs (assoc (:assigned-jobs queue {}) job-id (:worker-id updated-job))
        pending-jobs (vec (remove #(= % job-id) (:pending-jobs queue [])))
        updated-queue (c/->JobQueue updated-jobs pending-jobs assigned-jobs)]
    (be/add-component ecs-world building-id updated-queue)))

(defn- mark-agent-idle
  [ecs-world agent-id]
  (let [status-t (status-type)
        current-status (or (be/get-component ecs-world agent-id status-t)
                           (c/->AgentStatus true false true nil))
        updated-status (assoc current-status :idle? true :alive? (:alive? current-status true))]
    (be/add-component ecs-world agent-id updated-status)))

(defn- mark-agent-active
  [ecs-world agent-id]
  (let [status-t (status-type)
        current-status (or (be/get-component ecs-world agent-id status-t)
                           (c/->AgentStatus true false false nil))
        updated-status (assoc current-status :idle? false :alive? (:alive? current-status true))]
    (be/add-component ecs-world agent-id updated-status)))

(defn- target-pos
  [job]
  (or (:target-pos job) (:target job)))

(defn- claim-job!
  [ecs-world agent-id {:keys [building-id job-id job]}]
  (let [claimed-job (assoc job :worker-id agent-id :state :claimed)
        destination (target-pos claimed-job)
        job-assignment (assoc (c/->JobAssignment job-id 0.0) :target-pos destination)
        world-with-job (update-job-in-queue ecs-world building-id job-id (constantly claimed-job))
        world-with-assignment (be/add-component world-with-job agent-id job-assignment)
        world-with-status (mark-agent-active world-with-assignment agent-id)]
    (log/log-info "[JOB:ASSIGN]"
                  {:agent-id agent-id
                   :building-id building-id
                   :job-id job-id
                   :job-type (:type claimed-job)
                   :priority (:priority claimed-job)
                   :target destination})
    (if (and (vector? destination) (= 2 (count destination)))
      (do
        (log/log-info "[PATH:REQUEST]"
                      {:agent-id agent-id
                       :job-id job-id
                       :start nil
                       :goal destination
                       :source :job-assignment})
        (let [world-with-path (ecs-core/set-agent-path world-with-status agent-id [destination])]
          (log/log-info "[PATH:RESULT]"
                        {:agent-id agent-id
                         :job-id job-id
                         :status :ok
                         :waypoints 1
                         :goal destination})
          world-with-path))
      world-with-status)))

(defn process
  "Assign pending jobs to idle agents."
  [ecs-world _global-state]
  (let [role-t (role-type)
        status-t (status-type)
        assignment-t (job-assignment-type)
        agent-ids (be/get-all-entities-with-component ecs-world role-t)
        idle-agent-ids (filter (fn [agent-id]
                                  (let [status (be/get-component ecs-world agent-id status-t)
                                        active-assignment (be/get-component ecs-world agent-id assignment-t)]
                                    (and (idle-agent? ecs-world agent-id status)
                                         (nil? active-assignment))))
                                agent-ids)]
    (reduce (fn [world agent-id]
              (if-let [job-candidate (find-best-job world agent-id)]
                (claim-job! world agent-id job-candidate)
                (mark-agent-idle world agent-id)))
            ecs-world
            idle-agent-ids)))
