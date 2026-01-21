(ns fantasia.sim.tick.movement
  (:require [fantasia.sim.spatial :as spatial]
            [fantasia.sim.jobs :as jobs]
            [fantasia.sim.pathing :as pathing]))

(defn- handle-haul-job [world agent job]
  (let [current-pos (:pos agent)
        stage (:stage job :pickup)
        target-pos (if (= stage :pickup)
                    (:from-pos job)
                    (:to-pos job))]
    (if (= current-pos target-pos)
      agent
      (let [next-pos (pathing/next-step-toward world current-pos target-pos)]
        (if (not= next-pos current-pos)
          (do (assoc agent :pos next-pos))
          agent)))))

(defn move-agent-with-job
   "Move an agent one step. If agent has job, use pathing toward target.
    Otherwise, use random movement."
   [world agent]
   (let [alive? (get-in agent [:status :alive?] true)
         carrying-child? (not (nil? (:carrying-child agent)))
         child-stage (:child-stage agent)
         is-infant? (= child-stage :infant)]
     (if (not alive?)
       [world agent]
          (if carrying-child?
            (if-let [job (jobs/get-agent-job world (:id agent))]
              (let [world' (jobs/remove-job-from-world! world (:id job))]
                [world' agent])
              [world agent])
           (if is-infant?
             [world agent]
             (if-let [job (jobs/get-agent-job world (:id agent))]
               (let [current-pos (:pos agent)
                     job-target (jobs/job-target-pos world job)]
                 (cond
                   (= (:type job) :job/haul)
                   (handle-haul-job world agent job)

                   (nil? job-target)
                   [world agent]

                   (= current-pos job-target)
                   (if (= (:type job) :job/hunt)
                     [world agent]
                     (let [world' (jobs/advance-job! world (:id agent) 0.2)]
                       [world' (get-in world' [:agents (:id agent)])]))

                   :else
                    (let [next-pos (pathing/next-step-toward world current-pos job-target)]
                      (if (not= next-pos current-pos)
                        [world (assoc agent :pos next-pos)]
                        [world agent]))))
                (let [agent' (spatial/move-agent-with-collision-check world agent)]
                  [world agent'])))))))
