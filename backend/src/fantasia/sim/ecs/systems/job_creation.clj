(ns fantasia.sim.ecs.systems.job_creation
   (:require [brute.entity :as be]
             [fantasia.sim.ecs.components :as c]))

(defn generate-basic-jobs
  "Generate basic jobs from buildings with JobQueue components."
  [ecs-world global-state]
  (let [job-queue-type (be/get-component-type (c/->JobQueue {} [] {}))
        buildings-with-queues (be/get-all-entities-with-component ecs-world job-queue-type)
        tick (:tick global-state 0)]
    (reduce (fn [acc building-id]
              (let [job-queue (be/get-component ecs-world building-id job-queue-type)
                    current-jobs (:jobs job-queue {})
                    position-type (be/get-component-type (c/->Position 0 0))
                    position (be/get-component ecs-world building-id position-type)
                    q (:q position)
                    r (:r position)]
                ;; Add a basic gather job if building has no jobs
                (if (empty? current-jobs)
                   (let [job-id (str "job-" tick "-" building-id)
                         new-job {:id job-id
                                 :type :job/gather-wood
                                 :priority 50
                                 :target-pos [q r]
                                 :created-at tick}]
                     (be/add-component acc building-id 
                                        (c/->JobQueue 
                                        (assoc current-jobs job-id new-job)  ; :jobs field (map)
                                        (conj (:pending-jobs job-queue []) job-id)  ; :pending-jobs field (vector)
                                        (:assigned-jobs job-queue {}))))          ; :assigned-jobs field (map)
                   acc)))
             ecs-world
             buildings-with-queues)))

(defn process
  "Process job creation system."
  [ecs-world global-state]
  (generate-basic-jobs ecs-world global-state))