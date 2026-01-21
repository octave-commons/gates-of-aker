(ns fantasia.sim.tick.movement
   (:require [fantasia.dev.logging :as log]
             [fantasia.sim.constants :as const]
             [fantasia.sim.hex :as hex]
             [fantasia.sim.jobs :as jobs]
             [fantasia.sim.pathing :as pathing]
             [fantasia.sim.spatial :as spatial]))

(defn- agent-dexterity
  [agent]
  (double (get-in agent [:stats :dexterity] 0.4)))

(defn- road?
  [world pos]
  (let [tile-key (vector (first pos) (second pos))]
    (= :road (get-in world [:tiles tile-key :structure]))))

(defn- movement-steps
  [world agent]
  (let [dex (agent-dexterity agent)
        base (+ const/base-move-steps
                (long (Math/floor (* dex const/dex-move-step-multiplier))))
        road-bonus (if (road? world (:pos agent))
                     (+ const/road-move-step-bonus
                        (long (Math/floor (* dex const/road-dex-move-step-multiplier))))
                     0)]
    (max 1 (+ base road-bonus))))

(defn- move-toward-steps
  [world agent target-pos]
  (let [step-count (movement-steps world agent)]
    (loop [agent agent
           steps step-count]
      (if (or (zero? steps) (= (:pos agent) target-pos))
        agent
        (let [next-pos (pathing/next-step-toward world (:pos agent) target-pos)]
          (if (= next-pos (:pos agent))
            agent
            (recur (assoc agent :pos next-pos) (dec steps))))))))

(defn- move-random-steps
  [world agent]
  (let [step-count (movement-steps world agent)]
    (loop [agent agent
           steps step-count]
      (if (zero? steps)
        agent
        (let [agent' (spatial/move-agent-with-collision-check world agent)]
          (if (= (:pos agent') (:pos agent))
            agent
            (recur agent' (dec steps))))))))

(defn- handle-haul-job [world agent job]
  (let [current-pos (:pos agent)
        stage (:stage job :pickup)
        target-pos (if (= stage :pickup)
                     (:from-pos job)
                     (:to-pos job))]
    (if (= current-pos target-pos)
      agent
      (move-toward-steps world agent target-pos))))

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
                     (let [agent' (handle-haul-job world agent job)]
                       (log/log-debug "[MOVE:AGENT]"
                                      {:agent-id (:id agent)
                                       :from current-pos
                                       :to (:pos agent')
                                       :method :haul})
                       [world agent'])

                    (nil? job-target)
                    [world agent]

                    (= current-pos job-target)
                    (do
                      (log/log-debug "[JOB:ADJACENT]"
                                     {:agent-id (:id agent)
                                      :job-id (:id job)
                                      :pos current-pos
                                      :target job-target
                                      :distance 0})
                      (if (= (:type job) :job/hunt)
                        [world agent]
                        (let [world' (jobs/advance-job! world (:id agent) 0.2)]
                          [world' (get-in world' [:agents (:id agent)])])))

                     :else
                     (do
                       (log/log-debug "[JOB:NOT-ADJACENT]"
                                      {:agent-id (:id agent)
                                       :job-id (:id job)
                                       :pos current-pos
                                       :target job-target
                                       :distance (hex/distance current-pos job-target)})
                       (let [agent' (move-toward-steps world agent job-target)]
                         (log/log-debug "[MOVE:AGENT]"
                                        {:agent-id (:id agent)
                                         :from current-pos
                                         :to (:pos agent')
                                         :method :job-path})
                         [world agent']))))
                (let [agent' (move-random-steps world agent)]
                  (log/log-debug "[MOVE:AGENT]"
                                 {:agent-id (:id agent)
                                  :from (:pos agent)
                                  :to (:pos agent')
                                  :method :random})
                  [world agent'])))))))
