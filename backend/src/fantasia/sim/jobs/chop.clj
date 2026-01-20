(ns fantasia.sim.jobs.chop
  (:require [fantasia.sim.jobs :refer [parse-key-pos]]
            [fantasia.sim.pathing]))

(defn create-chop-job [pos]
  {:id (random-uuid)
   :type :job/chop-tree
   :target pos
   :priority 60
   :progress 0.0
   :required 1.0
   :state :pending})

(defn complete-chop! [world job]
  (let [pos (:target job)
        tile-key (str (first pos) "," (second pos))
        tile (get-in world [:tiles tile-key])]
    (when (and tile (= (:resource tile) :tree))
      (println "[JOB:COMPLETE]"
               {:type :job/chop-tree
                :target pos
                :outcome (str "Produced 5 wood at " pos)})
      (-> world
          (update-in [:tiles tile-key] assoc :resource :wood :qty 5)))))

(defn progress-chop! [world job delta]
  (let [new-progress (min (+ (:progress job) delta) (:required job))]
    (assoc job :progress new-progress)))

(defn job-complete? [job]
  (>= (:progress job 0.0) (:required job 1.0)))
