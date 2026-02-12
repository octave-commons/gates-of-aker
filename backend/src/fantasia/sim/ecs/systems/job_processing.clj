(ns fantasia.sim.ecs.systems.job-processing
  (:require [brute.entity :as be]
            [fantasia.dev.logging :as log]
            [fantasia.sim.constants :as const]
            [fantasia.sim.ecs.components :as c]
            [fantasia.sim.hex :as hex]))

(defn- job-assignment-type []
  (be/get-component-type (c/->JobAssignment nil 0.0)))

(defn- job-queue-type []
  (be/get-component-type (c/->JobQueue {} [] {})))

(defn- status-type []
  (be/get-component-type (c/->AgentStatus true false false nil)))

(defn- position-type []
  (be/get-component-type (c/->Position 0 0)))

(defn- tile-type []
  (be/get-component-type (c/->Tile :ground :plains nil nil)))

(defn- stockpile-type []
  (be/get-component-type (c/->Stockpile {})))

(defn- needs-type []
  (be/get-component-type (c/->Needs 0.6 0.7 0.7 1.0 0.8 0.6 0.5 0.5 0.5 0.6 0.5 0.5 0.5)))

(defn- clamp01
  [v]
  (-> v (max 0.0) (min 1.0)))

(defn- tile-at
  [ecs-world pos]
  (let [tile-index-t (be/get-component-type (c/->TileIndex 0 0))
        tile-t (tile-type)
        tile-id (some (fn [entity-id]
                        (let [index (be/get-component ecs-world entity-id tile-index-t)
                              tile (be/get-component ecs-world entity-id tile-t)]
                          (when (and index tile (= [(:q index) (:r index)] pos))
                            entity-id)))
                      (be/get-all-entities ecs-world))]
    (when tile-id
      {:id tile-id
       :tile (be/get-component ecs-world tile-id (tile-type))})))

(defn- update-tile-resource
  [ecs-world pos resource]
  (if-let [{:keys [id tile]} (tile-at ecs-world pos)]
    (be/add-component ecs-world id (assoc tile :resource resource))
    ecs-world))

(defn- update-tile-structure
  [ecs-world pos structure]
  (if-let [{:keys [id tile]} (tile-at ecs-world pos)]
    (be/add-component ecs-world id (assoc tile :structure structure))
    ecs-world))

(defn- add-resource-to-stockpile
  [ecs-world pos resource qty]
  (if-let [{:keys [id]} (tile-at ecs-world pos)]
    (let [stockpile-t (stockpile-type)
          stockpile (be/get-component ecs-world id stockpile-t)
          contents (:contents stockpile {})
          updated (assoc contents resource (+ (long (get contents resource 0)) (long qty)))]
      (be/add-component ecs-world id (c/->Stockpile updated)))
    ecs-world))

(defn- nearest-stockpile-pos-with-content
  [ecs-world agent-pos resource]
  (let [stockpile-t (stockpile-type)
        stockpile-ids (be/get-all-entities-with-component ecs-world stockpile-t)]
    (->> stockpile-ids
         (keep (fn [stockpile-id]
                 (let [stockpile (be/get-component ecs-world stockpile-id stockpile-t)
                       index (be/get-component ecs-world stockpile-id (be/get-component-type (c/->TileIndex 0 0)))
                       qty (long (get (:contents stockpile {}) resource 0))]
                   (when (and index (pos? qty))
                     [(:q index) (:r index)]))))
         (sort-by #(hex/distance agent-pos %))
         first)))

(defn- consume-fruit
  [ecs-world agent-id target-pos]
  (let [agent-position (be/get-component ecs-world agent-id (position-type))
        agent-pos [(:q agent-position) (:r agent-position)]
        needs-t (needs-type)
        needs (be/get-component ecs-world agent-id needs-t)
        world-after-tile (if (= :fruit (:resource (:tile (tile-at ecs-world target-pos))))
                           (update-tile-resource ecs-world target-pos nil)
                           ecs-world)
        stockpile-pos (nearest-stockpile-pos-with-content world-after-tile agent-pos :fruit)
        consumed-from-stockpile? (and (= world-after-tile ecs-world) stockpile-pos)
        world-after-stockpile (if consumed-from-stockpile?
                               (add-resource-to-stockpile world-after-tile stockpile-pos :fruit -1)
                               world-after-tile)]
    (if needs
      (be/add-component world-after-stockpile agent-id (assoc needs :food (clamp01 (+ (:food needs 0.0) 0.35))))
      world-after-stockpile)))

(defn- scatter-logs-near
  [ecs-world target-pos]
  (let [candidates (concat (hex/neighbors target-pos) [target-pos])
        chosen (->> candidates
                    (keep (fn [pos]
                            (when-let [{:keys [tile]} (tile-at ecs-world pos)]
                              (when (nil? (:resource tile)) pos))))
                    (take const/log-drop-count))]
    (reduce (fn [world pos]
              (update-tile-resource world pos :log))
            ecs-world
            chosen)))

(defn- harvest-structure-resource
  [structure]
  (case structure
    :lumberyard :log
    :orchard :fruit
    :granary :grain
    :quarry :stone
    :stockpile :fruit
    :warehouse :log
    nil))

(defn- structure-needs-job-queue?
  [structure]
  (contains? #{:campfire :stockpile :lumberyard :farm :orchard :granary :quarry :house :warehouse :workshop}
             structure))

(defn- complete-build-structure
  [ecs-world job target]
  (let [structure (:structure job)
        world-with-tile (update-tile-structure ecs-world target structure)]
    (if-let [{:keys [id]} (tile-at world-with-tile target)]
      (let [world-with-structure-state (be/add-component world-with-tile id (c/->StructureState 1 100 100 nil))
            world-with-queue (if (structure-needs-job-queue? structure)
                               (be/add-component world-with-structure-state id (c/->JobQueue {} [] {}))
                               world-with-structure-state)]
        (if-let [resource (harvest-structure-resource structure)]
          (be/add-component world-with-queue id (c/->Stockpile {resource 0}))
          world-with-queue))
      world-with-tile)))

(defn- apply-rest
  [ecs-world agent-id]
  (let [needs-t (needs-type)
        needs (be/get-component ecs-world agent-id needs-t)]
    (if needs
      (be/add-component ecs-world
                        agent-id
                        (assoc needs :sleep (clamp01 (+ (:sleep needs 0.0) 0.35))))
      ecs-world)))

(defn- apply-job-effects
  [ecs-world agent-id job target]
  (case (:type job)
    :job/chop-tree
    (-> ecs-world
        (update-tile-resource target nil)
        (scatter-logs-near target))

    :job/eat
    (consume-fruit ecs-world agent-id target)

    :job/build-structure
    (complete-build-structure ecs-world job target)

    :job/rest
    (apply-rest ecs-world agent-id)

    ecs-world))

(defn- find-job-by-id
  [ecs-world job-id]
  (let [queue-t (job-queue-type)
        building-ids (be/get-all-entities-with-component ecs-world queue-t)]
    (some (fn [building-id]
            (let [queue (be/get-component ecs-world building-id queue-t)
                  job (get (:jobs queue {}) job-id)]
              (when job
                {:building-id building-id
                 :job job
                 :queue queue})))
          building-ids)))

(defn- update-job
  [ecs-world building-id queue job-id new-job]
  (let [completed? (= :completed (:state new-job))
        updated-jobs (if completed?
                       (dissoc (:jobs queue {}) job-id)
                       (assoc (:jobs queue {}) job-id new-job))
        pending-jobs (vec (remove #(= % job-id) (:pending-jobs queue [])))
        assigned-jobs (if completed?
                        (dissoc (:assigned-jobs queue {}) job-id)
                        (assoc (:assigned-jobs queue {}) job-id (:worker-id new-job)))
        updated-queue (c/->JobQueue updated-jobs pending-jobs assigned-jobs)]
    (be/add-component ecs-world building-id updated-queue)))

(defn- mark-agent-idle
  [ecs-world agent-id]
  (let [status-t (status-type)
        status (or (be/get-component ecs-world agent-id status-t)
                   (c/->AgentStatus true false true nil))]
    (be/add-component ecs-world agent-id (assoc status :idle? true))))

(defn- clear-assignment
  [ecs-world agent-id]
  (be/remove-component ecs-world agent-id (job-assignment-type)))

(defn- target-pos
  [job]
  (or (:target-pos job) (:target job)))

(defn- assignment-target-pos
  [assignment]
  (or (:target-pos assignment) (:target assignment)))

(defn- adjacent-or-on-target?
  [agent-pos target]
  (or (= agent-pos target)
      (some #(= % target) (hex/neighbors agent-pos))))

(defn- process-job-for-agent
  [ecs-world tick agent-id assignment]
  (let [position (be/get-component ecs-world agent-id (position-type))
        job-id (:job-id assignment)
        current-progress (double (:progress assignment 0.0))]
    (if (nil? position)
      (-> ecs-world
          (clear-assignment agent-id)
          (mark-agent-idle agent-id))
      (if-let [{:keys [building-id job queue]} (find-job-by-id ecs-world job-id)]
        (let [target (or (assignment-target-pos assignment)
                         (target-pos job))
              agent-pos [(:q position) (:r position)]]
          (if (and (vector? target)
                   (= 2 (count target))
                   (adjacent-or-on-target? agent-pos target))
            (let [new-progress (min 1.0 (+ current-progress 0.2))
                  world-with-progress (be/add-component ecs-world agent-id (assoc assignment :progress new-progress))]
              (log/log-info "[JOB:PROGRESS]"
                            {:agent-id agent-id
                             :job-id job-id
                             :type (:type job)
                             :target target
                             :progress new-progress})
              (if (>= new-progress 1.0)
                (let [world-with-effects (apply-job-effects world-with-progress agent-id job target)
                      completed-job (assoc job :state :completed :completed-at tick)
                      world-with-job (update-job world-with-effects building-id queue job-id completed-job)]
                  (log/log-info "[JOB:COMPLETE]"
                                {:agent-id agent-id
                                 :job-id job-id
                                 :type (:type completed-job)
                                 :target target
                                 :building-id building-id})
                  (-> world-with-job
                      (clear-assignment agent-id)
                      (mark-agent-idle agent-id)))
                world-with-progress))
            ecs-world))
        (-> ecs-world
            (clear-assignment agent-id)
            (mark-agent-idle agent-id))))))

(defn process
  "Advance progress on claimed jobs for agents."
  [ecs-world global-state]
  (let [assignment-t (job-assignment-type)
        tick (:tick global-state 0)
        agents-with-jobs (be/get-all-entities-with-component ecs-world assignment-t)]
    (reduce (fn [world agent-id]
              (if-let [assignment (be/get-component world agent-id assignment-t)]
                (process-job-for-agent world tick agent-id assignment)
                world))
            ecs-world
            agents-with-jobs)))
