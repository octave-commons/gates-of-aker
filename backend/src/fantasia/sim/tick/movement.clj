(ns fantasia.sim.tick.movement
  (:require [fantasia.sim.spatial :as spatial]
            [fantasia.sim.jobs :as jobs]
            [fantasia.sim.hex :as hex]))

(defn- bfs-path-simple [world start goal]
  (if (= start goal)
    [start]
    (loop [queue [[start]]
           visited #{start}
           steps 0]
      (let [max-steps 1000]
        (if (>= steps max-steps)
          nil
          (if (empty? queue)
            nil
            (let [path (first queue)
                  current (last path)]
              (if (= current goal)
                path
                (let [neighbors (->> (hex/neighbors current)
                                     (filter #(spatial/in-bounds? world %))
                                     (filter #(spatial/passable? world %))
                                     (remove visited))
                      visited' (reduce conj visited neighbors)
                      new-paths (map #(conj path %) neighbors)
                      queue' (concat (rest queue) new-paths)]
                  (recur queue' visited' (inc steps)))))))))))

(defn- next-step-toward-simple [world start goal]
  (if (= start goal)
    start
    (let [path (bfs-path-simple world start goal)]
      (if (and path (> (count path) 1))
        (second path)
        start))))

(defn- handle-haul-job [world agent job]
  (let [current-pos (:pos agent)
        stage (:stage job :pickup)
        target-pos (if (= stage :pickup)
                    (:from-pos job)
                    (:to-pos job))]
    (if (= current-pos target-pos)
      agent
      (let [next-pos (next-step-toward-simple world current-pos target-pos)]
        (if (not= next-pos current-pos)
          (do (println "[MOVEMENT:AGENT]"
                       {:agent-id (:id agent)
                       :from current-pos
                       :to next-pos
                       :job-type (:type job)
                       :stage stage
                       :job-target target-pos})
              (assoc agent :pos next-pos))
          agent)))))

(defn move-agent-with-job
   "Move an agent one step. If agent has job, use pathing toward target.
    Otherwise, use random movement."
   [world agent]
   (if-let [job (jobs/get-agent-job world (:id agent))]
      (if (= (:type job) :job/haul)
        (handle-haul-job world agent job)
        (let [current-pos (:pos agent)
              job-target (:target job)]
          (if (= current-pos job-target)
            (let [world' (jobs/advance-job! world (:id agent) 0.2)]
              (println "[MOVEMENT:AGENT]"
                       {:agent-id (:id agent)
                        :current-pos current-pos
                        :job-target job-target
                        :moved? false
                        :reason "Working on job"})
              [world' (get-in world' [:agents (:id agent)])])
            (let [next-pos (next-step-toward-simple world current-pos job-target)]
              (if (not= next-pos current-pos)
                (do (println "[MOVEMENT:AGENT]"
                             {:agent-id (:id agent)
                              :from current-pos
                              :to next-pos
                              :job-type (:type job)
                              :job-target job-target})
                     [world (assoc agent :pos next-pos)])
                [world agent])))))
      (let [agent' (spatial/move-agent world agent)]
        (when (not= (:pos agent') (:pos agent))
          (println "[MOVEMENT:RANDOM]"
                   {:agent-id (:id agent)
                    :from (:pos agent)
                    :to (:pos agent')}))
        [world agent'])))