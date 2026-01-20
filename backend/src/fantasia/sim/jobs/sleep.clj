(ns fantasia.sim.jobs.sleep)

(defn create-sleep-job [pos]
  {:id (random-uuid)
   :type :job/sleep
   :target pos
   :priority 90
   :progress 0.0
   :required 1.0
   :state :pending})

(defn complete-sleep! [world agent-id]
  (println "[JOB:COMPLETE]" {:type :job/sleep :agent-id agent-id})
  (assoc-in world [:agents agent-id :needs :sleep] 1.0))

(defn progress-sleep! [job delta]
  (let [new-progress (min (+ (:progress job) delta) (:required job))]
    (assoc job :progress new-progress)))

(defn job-complete? [job]
  (>= (:progress job 0.0) (:required job 1.0)))
