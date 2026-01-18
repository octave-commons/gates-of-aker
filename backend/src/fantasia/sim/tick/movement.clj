(ns fantasia.sim.tick.movement
  (:require [fantasia.sim.spatial :as spatial]
            [fantasia.sim.pathing :as pathing]
            [fantasia.sim.jobs :as jobs]))

(defn move-agent-with-job
   "Move an agent one step. If agent has job, use pathing toward target.
    Otherwise, use random movement."
   [world agent]
   (if-let [job (jobs/get-agent-job world (:id agent))]
      (let [current-pos (:pos agent)
            job-target (:target job)]
        (if (= current-pos job-target)
          agent
          (let [next-pos (pathing/next-step-toward world current-pos job-target)]
            (if (not= next-pos current-pos)
              (assoc agent :pos next-pos)
              agent))))
      (spatial/move-agent world agent)))
