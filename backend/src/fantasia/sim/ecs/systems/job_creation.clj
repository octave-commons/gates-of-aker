(ns fantasia.sim.ecs.systems.job_creation
   (:require [brute.entity :as be]
             [fantasia.sim.ecs.components :as c]))

(defn generate-basic-jobs
  "Generate basic jobs from buildings with JobQueue components."
  [ecs-world global-state]
  (let [job-queue-type (be/get-component-type (c/->JobQueue [] {}))
        buildings-with-queues (be/get-all-entities-with-component ecs-world job-queue-type)
        tile-type (be/get-component-type (c/->Tile :ground :plains nil nil))
        tick (:tick global-state 0)]
    (reduce (fn [acc building-id]
              (let [job-queue (be/get-component acc building-id job-queue-type)
                    current-jobs (:jobs job-queue {})
                    position-type (be/get-component-type (c/->Position 0 0))
                    position (be/get-component acc building-id position-type)
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
                    (be/add-component acc building-id (c/->JobQueue [] (assoc current-jobs job-id new-job))))
                  acc)))
            ecs-world
            buildings-with-queues)))

(defn process
  "Process job creation system."
  [ecs-world global-state]
  (generate-basic-jobs ecs-world global-state))