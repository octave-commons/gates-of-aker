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
   (let [alive? (get-in agent [:status :alive?] true)]
     (if (not alive?)
       [world agent]
       (if-let [job (jobs/get-agent-job world (:id agent))]
         (if (= (:type job) :job/haul)
           (handle-haul-job world agent job)
           (let [current-pos (:pos agent)
                 job-target (:target job)]
             (if (= current-pos job-target)
               (let [world' (jobs/advance-job! world (:id agent) 0.2)]
                 [world' (get-in world' [:agents (:id agent)])])
                (let [next-pos (pathing/next-step-toward world current-pos job-target)]
                 (if (not= next-pos current-pos)
                   (do [world (assoc agent :pos next-pos)])
                   [world agent])))))
         (let [agent' (spatial/move-agent world agent)]
           [world agent'])))))
