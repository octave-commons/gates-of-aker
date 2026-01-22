(ns fantasia.sim.jobs.deliver-food
  (:require [fantasia.dev.logging :as log]))

(defn create-deliver-food-job [from-pos to-pos qty]
   {:id (random-uuid)
    :type :job/deliver-food
    :target from-pos
    :priority 75
    :progress 0.0
    :required 1.0
    :state :pending
    :from-pos from-pos
    :to-pos to-pos
    :resource :food
    :qty qty})

(defn complete-deliver-food! [world job agent-id]
   (let [[q r] (:to-pos job)
         tile-key (vector q r)
         pos (:to-pos job)
         qty (:qty job)
         stockpiles (:stockpiles world)
         sp (get stockpiles tile-key)
          current-qty (or (when sp (:current-qty sp)) 0)
          new-qty (+ current-qty qty)]
      (log/log-info "[JOB:COMPLETE]" {:type :job/deliver-food :agent-id agent-id :target pos :qty qty})
      (if sp
       (assoc-in world [:stockpiles tile-key :current-qty] new-qty)
       (assoc-in world [:stockpiles tile-key] {:resource :food :max-qty 200 :current-qty new-qty}))))

(defn progress-deliver-food! [job delta]
  (let [new-progress (min (+ (:progress job) delta) (:required job))]
    (assoc job :progress new-progress)))

(defn job-complete? [job]
  (>= (:progress job 0.0) (:required job 1.0)))
