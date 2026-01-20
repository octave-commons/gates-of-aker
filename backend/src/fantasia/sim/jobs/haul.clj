(ns fantasia.sim.jobs.haul
  (:require [fantasia.sim.jobs :refer [parse-key-pos]]))

(defn create-haul-job [from-pos to-pos resource qty]
  {:id (random-uuid)
   :type :job/haul
   :target from-pos
   :priority 80
   :progress 0.0
   :required 1.0
   :state :pending
   :from-pos from-pos
   :to-pos to-pos
   :resource resource
   :qty qty
   :stage :pickup})

(defn complete-haul! [world job agent-id]
  (let [pos (:to-pos job)
        resource (:resource job)
        qty (:qty job)
        tile-key (str (first pos) "," (second pos))
        stockpiles (:stockpiles world)
        sp (get stockpiles tile-key)
        current-qty (or (when sp (:current-qty sp)) 0)
        new-qty (+ current-qty qty)]
    (println "[JOB:COMPLETE]" {:type :job/haul :agent-id agent-id :target pos :resource resource :qty qty})
    (if sp
      (assoc-in world [:stockpiles tile-key :current-qty] new-qty)
      (assoc-in world [:stockpiles tile-key] {:resource resource :max-qty 200 :current-qty new-qty}))))

(defn progress-haul! [job delta]
  (let [new-progress (min (+ (:progress job) delta) (:required job))]
    (assoc job :progress new-progress)))

(defn job-complete? [job]
  (>= (:progress job 0.0) (:required job 1.0)))
