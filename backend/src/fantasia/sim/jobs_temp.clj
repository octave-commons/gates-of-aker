(ns fantasia.sim.jobs-temp
  (:require [fantasia.sim.hex :as hex]
            [clojure.string :as str]))

(def job-priorities
   {:job/eat 100
    :job/sleep 90
    :job/chop-tree 60
    :job/haul 50
    :job/deliver-food 45
    :job/build-wall 40})

(defn create-job [job-type target]
   (let [target-pos (cond
                     (sequential? target) target
                     (number? target) [target 0]
                     :else [0 0])]
     (when target-pos
       (let [job {:id (random-uuid)
                  :type job-type
                  :target target-pos
                  :worker-id nil
                  :progress 0.0
                  :required 1.0
                  :state :pending
                  :priority (get job-priorities job-type 50)}]
         (println "[JOB:CREATE]"
                  {:type (:type job)
                   :id (:id job)
                   :target (:target job)
                   :priority (:priority job)})
         job))))

(defn assign-job! [world job agent-id]
  (let [job' (assoc job :worker-id agent-id :state :claimed)]
    (-> world
        (update-in [:agents agent-id] assoc :current-job (:id job'))
        (update :jobs conj job')))

(defn claim-next-job! [world agent-id]
  (let [agent (get-in world [:agents agent-id])
        pending (filter #(= (:state %) :pending) (:jobs world))
        sorted (sort-by (fn [j]
                          [(- (:priority j 0))
                           (hex/distance (:pos agent) (:target j))])
                        pending)
        job (first sorted)]
    (if job
      (do (println "[JOB:ASSIGN]"
                   {:agent-id agent-id
                    :job-id (:id job)
                    :type (:type job)
                    :priority (:priority job)})
          (assign-job! world job agent-id))
      world)))

(defn auto-assign-jobs! [world]
  (let [result (reduce (fn [w agent]
                        (if (nil? (:current-job agent))
                          (claim-next-job! w (:id agent))
                          w))
                      world
                      (:agents world))]
    (when (not= world result)
      (println "[JOB:AUTO-ASSIGN]"
               {:assigned (->> (:agents result)
                              (filter #(not= (:current-job %) (get-in world [:agents (:id %) :current-job])))
                              count)
                :tick (:tick result)}))
    result))

(defn get-agent-job [world agent-id]
  (when-let [job-id (get-in world [:agents agent-id] :current-job])]
    (first (filter #(= (:id %) job-id) (:jobs world)))))

(defn complete-job! [world agent-id]
  (if-let [job-id (get-in world [:agents agent-id] :current-job])]
    (let [idx (first (keep-indexed (fn [i j] (when (= (:id j) job-id) i)) (:jobs world)))
          job (get-in world [:jobs idx])
          world (update-in world [:agents agent-id] dissoc :current-job)
          world (update world :jobs (fn [js] (vec (remove #(= (:id %) job-id) js)))]
      (case (:type job)
        :job/build-wall (do world)
        :job/chop-tree (do world)
        :job/haul (do world)
        :job/eat (do world)
        :job/sleep (do world)
        :job/deliver-food (do world)
        (do world))
      world)
    world))

(defn advance-job! [world agent-id delta]
  (if-let [job-id (get-in world [:agents agent-id] :current-job])]
    (let [idx (first (keep-indexed (fn [i j] (when (= (:id j) job-id) i)) (:jobs world)))
          job (get-in world [:jobs idx])]
      (if job
        (let [new-state (if (= (:state job) :claimed) :in-progress (:state job))
              new-progress (min (+ (:progress job) delta) (:required job))
              job' (assoc job :state new-state :progress new-progress)
              world' (assoc-in world [:jobs idx] job')]
          (println "[JOB:PROGRESS]"
                   {:agent-id agent-id
                    :job-id job-id
                    :delta delta
                    :new-progress new-progress
                    :required (:required job)})
          (if (>= new-progress (:required job))
            (complete-job! world' agent-id)
            world'))
        world))
    world))

(defn adjacent-to-job? [world agent-id]
  (if-let [job (get-agent-job world agent-id)]
    (let [apos (get-in world [:agents agent-id] :pos)
          t (:target job)]
      (<= (hex/distance apos t) 1))
    false))
