(ns fantasia.sim.ecs.systems.job_creation
(:require [brute.entity :as be]
              [fantasia.sim.ecs.components :as c]
              [fantasia.dev.logging :as log]))

(defn generate-basic-jobs
  "Generate basic jobs from buildings with JobQueue components."
  [ecs-world global-state]
  (let [job-queue-type (be/get-component-type (c/->JobQueue {} [] {}))
        buildings-with-queues (be/get-all-entities-with-component ecs-world job-queue-type)
        tile-type (be/get-component-type (c/->Tile :ground :plains nil nil))
        tick (:tick global-state 0)]
    (reduce (fn [acc building-id]
              (let [job-queue (be/get-component ecs-world building-id job-queue-type)
                    current-jobs (:jobs job-queue {})
                    position-type (be/get-component-type (c/->Position 0 0))
                    position (be/get-component ecs-world building-id position-type)
                    q (:q position)
                    r (:r position)
                    tile (be/get-component acc building-id tile-type)
                    structure (:structure tile)
                    ;; Determine job type based on building structure
                    job-type (case structure
                               :campfire :job/build-fire
                               :stockpile :job/haul
                               :farm :job/farm
                               :orchard :job/harvest-fruit
                               :house :job/improve
                               :warehouse :job/haul
                               :job/gather-wood)
                    ;; Create up to 2 jobs per building
                    max-jobs 2
                    existing-job-count (count current-jobs)]
                ;; Add jobs if building has fewer than max jobs
                (if (< existing-job-count max-jobs)
                  (let [job-id (str "job-" tick "-" building-id "-" existing-job-count)
                        new-job {:id job-id
                                :type job-type
                                :priority (case job-type
                                           :job/build-fire 70
                                           :job/harvest-fruit 58
                                           :job/farm 58
                                           :job/haul 50
                                           :job/improve 52
                                           50)
                                :target-pos [q r]
                                :created-at tick
                                :state :pending}]
                    (be/add-component acc building-id (c/->JobQueue (assoc current-jobs job-id new-job) [] {})))
                  acc)))
            ecs-world
            buildings-with-queues)))

(defn generate-need-jobs
  "Generate jobs based on agent needs."
  [ecs-world global-state]
  (let [needs-type (be/get-component-type (c/->Needs 0.6 0.7 0.7 1.0 0.8 0.6 0.5 0.5 0.5 0.6 0.5 0.5 0.5))
        agent-ids (be/get-all-entities-with-component ecs-world needs-type)
        job-queue-type (be/get-component-type (c/->JobQueue {} [] {}))
        tick (:tick global-state 0)]
    (reduce (fn [acc agent-id]
              (let [needs (be/get-component ecs-world agent-id needs-type)
                    pos-type (be/get-component-type (c/->Position 0 0))
                    pos (be/get-component ecs-world agent-id pos-type)]
                (when (and needs pos)
                  ;; Create food-related jobs if food is very low (< 0.3)
                  (when (< (:food needs) 0.3)
                    (let [job-id (str "need-food-" tick "-" agent-id)
                          food-job {:id job-id
                                   :type :job/gather-food
                                   :priority 90  ;; High priority for needs
                                   :target-pos [(:q pos) (:r pos)]
                                   :created-at tick
                                   :state :pending}]
                      (log/log-info "[NEED:JOB-CREATED]" 
                                   {:agent-id agent-id 
                                    :need :food 
                                    :value (:food needs)
                                    :job-id job-id})))
                  ;; Create sleep jobs if sleep is very low (< 0.3)
                  (when (< (:sleep needs) 0.3)
                    (let [job-id (str "need-sleep-" tick "-" agent-id)
                          sleep-job {:id job-id
                                    :type :job/rest
                                    :priority 95  ;; Very high priority for sleep
                                    :target-pos [(:q pos) (:r pos)]
                                    :created-at tick
                                    :state :pending}]
                      (log/log-info "[NEED:JOB-CREATED]" 
                                   {:agent-id agent-id 
                                    :need :sleep 
                                    :value (:sleep needs)
                                    :job-id job-id})))
                  ;; Create warmth jobs if warmth is very low (< 0.3)
                  (when (< (:warmth needs) 0.3)
                    (let [job-id (str "need-warmth-" tick "-" agent-id)
                          warmth-job {:id job-id
                                     :type :job/build-fire
                                     :priority 85  ;; High priority for warmth
                                     :target-pos [(:q pos) (:r pos)]
                                     :created-at tick
                                     :state :pending}]
                      (log/log-info "[NEED:JOB-CREATED]" 
                                   {:agent-id agent-id 
                                    :need :warmth 
                                    :value (:warmth needs)
                                    :job-id job-id}))))
                acc))
            ecs-world
            agent-ids)))

(defn process
  "Process job creation system."
  [ecs-world global-state]
  (-> ecs-world
      (generate-basic-jobs global-state)
      (generate-need-jobs global-state)))
