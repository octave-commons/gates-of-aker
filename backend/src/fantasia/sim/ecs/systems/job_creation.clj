(ns fantasia.sim.ecs.systems.job-creation
  (:require [brute.entity :as be]
            [fantasia.dev.logging :as log]
            [fantasia.sim.constants :as const]
            [fantasia.sim.ecs.components :as c]
            [fantasia.sim.hex :as hex]))

(defn- job-queue-type []
  (be/get-component-type (c/->JobQueue {} [] {})))

(defn- tile-type []
  (be/get-component-type (c/->Tile :ground :plains nil nil)))

(defn- tile-index-type []
  (be/get-component-type (c/->TileIndex 0 0)))

(defn- stockpile-type []
  (be/get-component-type (c/->Stockpile {})))

(defn- role-type []
  (be/get-component-type (c/->Role :peasant)))

(defn- status-type []
  (be/get-component-type (c/->AgentStatus true false false nil)))

(defn- job-assignment-type []
  (be/get-component-type (c/->JobAssignment nil 0.0)))

(defn- position-type []
  (be/get-component-type (c/->Position 0 0)))

(defn- needs-type []
  (be/get-component-type (c/->Needs 0.6 0.7 0.7 1.0 0.8 0.6 0.5 0.5 0.5 0.6 0.5 0.5 0.5)))

(defn- gather-active-jobs
  [ecs-world]
  (let [queue-t (job-queue-type)
        building-ids (be/get-all-entities-with-component ecs-world queue-t)]
    (mapcat (fn [building-id]
              (let [queue (be/get-component ecs-world building-id queue-t)]
                (vals (:jobs queue {}))))
            building-ids)))

(defn- active-job?
  [job]
  (not= :completed (:state job)))

(defn- wood-demand-open-capacity
  [ecs-world]
  (let [stockpile-t (stockpile-type)
        stockpile-ids (be/get-all-entities-with-component ecs-world stockpile-t)]
    (reduce (fn [acc stockpile-id]
              (let [stockpile (be/get-component ecs-world stockpile-id stockpile-t)
                    contents (:contents stockpile {})
                    accepts-wood? (or (contains? contents :log)
                                      (contains? contents :wood))
                    current-qty (+ (long (get contents :log 0))
                                   (long (get contents :wood 0)))
                    open-capacity (max 0 (- const/default-stockpile-max-qty current-qty))]
                (if accepts-wood?
                  (+ acc open-capacity)
                  acc)))
            0
            stockpile-ids)))

(defn- target-pos
  [job]
  (or (:target-pos job) (:target job)))

(defn- tree-targets
  [ecs-world reserved-targets]
  (let [tile-t (tile-type)
        tile-index-t (tile-index-type)
        tile-ids (be/get-all-entities-with-component ecs-world tile-t)]
    (->> tile-ids
         (keep (fn [tile-id]
                 (let [tile (be/get-component ecs-world tile-id tile-t)
                       index (be/get-component ecs-world tile-id tile-index-t)
                       pos [(:q index) (:r index)]]
                   (when (and index
                              (= :tree (:resource tile))
                              (not (contains? reserved-targets pos)))
                     pos))))
         vec)))

(defn- add-job-to-queue
  [ecs-world queue-id job]
  (let [queue-t (job-queue-type)
        queue (or (be/get-component ecs-world queue-id queue-t)
                  (c/->JobQueue {} [] {}))
        updated-queue (c/->JobQueue (assoc (:jobs queue {}) (:id job) job)
                                    (conj (vec (:pending-jobs queue [])) (:id job))
                                    (:assigned-jobs queue {}))]
    (be/add-component ecs-world queue-id updated-queue)))

(defn- generate-demand-driven-chop-jobs
  [ecs-world global-state]
  (let [queue-t (job-queue-type)
        queue-ids (vec (be/get-all-entities-with-component ecs-world queue-t))
        queue-id (first queue-ids)
        tick (:tick global-state 0)
        active-jobs (filter active-job? (gather-active-jobs ecs-world))
        active-chop-jobs (filter #(= :job/chop-tree (:type %)) active-jobs)
        reserved-targets (set (map target-pos active-chop-jobs))
        open-capacity (wood-demand-open-capacity ecs-world)
        required-jobs (if (pos? open-capacity)
                        (long (Math/ceil (/ (double open-capacity) (double const/log-drop-count))))
                        0)
        jobs-to-create (max 0 (- required-jobs (count active-chop-jobs)))
        available-targets (tree-targets ecs-world reserved-targets)
        targets-to-create (take jobs-to-create available-targets)]
    (if (or (nil? queue-id) (empty? targets-to-create))
      ecs-world
      (reduce (fn [world [idx [q r]]]
                (let [job-id (str "job-chop-" tick "-" idx)
                      job {:id job-id
                           :type :job/chop-tree
                           :target-pos [q r]
                           :target [q r]
                           :resource :log
                           :priority 60
                           :created-at tick
                           :state :pending}]
                  (log/log-info "[JOB:CREATE]"
                                {:job-id job-id
                                 :type :job/chop-tree
                                 :target [q r]
                                 :priority 60
                                 :source :wood-order-demand})
                  (add-job-to-queue world queue-id job)))
              ecs-world
              (map-indexed vector targets-to-create)))))

(defn- build-target-for-agent
  [ecs-world agent-pos]
  (let [tile-t (tile-type)
        tile-index-t (tile-index-type)
        tile-ids (be/get-all-entities-with-component ecs-world tile-t)
        structure-by-pos (reduce (fn [acc tile-id]
                                   (let [tile (be/get-component ecs-world tile-id tile-t)
                                         index (be/get-component ecs-world tile-id tile-index-t)]
                                     (if index
                                       (assoc acc [(:q index) (:r index)] (:structure tile))
                                       acc)))
                                 {}
                                 tile-ids)
        candidates (concat (hex/neighbors agent-pos) [agent-pos])]
    (first (filter #(nil? (get structure-by-pos %)) candidates))))

(defn- find-building-positions
  [ecs-world]
  (let [tile-t (tile-type)
        tile-index-t (tile-index-type)]
    (reduce (fn [acc tile-id]
              (let [tile (be/get-component ecs-world tile-id tile-t)
                    index (be/get-component ecs-world tile-id tile-index-t)
                    structure (:structure tile)]
                (if (and index structure)
                  (assoc acc [(:q index) (:r index)] structure)
                  acc)))
            {}
            (be/get-all-entities-with-component ecs-world tile-t))))

(defn- structure-exists?
  [ecs-world structure]
  (boolean (some #(= structure %) (vals (find-building-positions ecs-world)))))

(defn- campfire-pos
  [ecs-world]
  (some (fn [[pos structure]]
          (when (= :campfire structure) pos))
        (find-building-positions ecs-world)))

(defn- demand-open-capacity-for
  [ecs-world resource]
  (let [stockpile-t (stockpile-type)
        stockpile-ids (be/get-all-entities-with-component ecs-world stockpile-t)]
    (reduce (fn [acc stockpile-id]
              (let [stockpile (be/get-component ecs-world stockpile-id stockpile-t)
                    contents (:contents stockpile {})
                    tracks-resource? (contains? contents resource)
                    current-qty (long (get contents resource 0))
                    open-capacity (max 0 (- const/default-stockpile-max-qty current-qty))]
                (if tracks-resource?
                  (+ acc open-capacity)
                  acc)))
            0
            stockpile-ids)))

(defn- has-active-job-for-structure?
  [ecs-world structure]
  (boolean
   (some (fn [job]
           (and (active-job? job)
                (= :job/build-structure (:type job))
                (= structure (:structure job))))
         (gather-active-jobs ecs-world))))

(defn- build-target-near
  [ecs-world origin-pos]
  (let [structure-by-pos (find-building-positions ecs-world)
        candidates (concat (hex/neighbors origin-pos) [origin-pos])]
    (first (filter #(nil? (get structure-by-pos %)) candidates))))

(defn- generate-harvest-autobuild-jobs
  [ecs-world global-state]
  (let [queue-id (first (be/get-all-entities-with-component ecs-world (job-queue-type)))
        tick (:tick global-state 0)
        origin (campfire-pos ecs-world)
        demand-structures [[:log :lumberyard]
                           [:fruit :orchard]
                           [:grain :granary]
                           [:stone :quarry]]]
    (if (or (nil? queue-id) (nil? origin))
      ecs-world
      (reduce (fn [world [resource structure]]
                (let [open-capacity (demand-open-capacity-for world resource)
                      should-create? (and (pos? open-capacity)
                                          (not (structure-exists? world structure))
                                          (not (has-active-job-for-structure? world structure)))]
                  (if should-create?
                    (if-let [target (build-target-near world origin)]
                      (let [job-id (str "job-autobuild-" (name structure) "-" tick)
                            job {:id job-id
                                 :type :job/build-structure
                                 :structure structure
                                 :target-pos target
                                 :target target
                                 :priority 35
                                 :created-at tick
                                 :state :pending}]
                        (log/log-info "[JOB:CREATE]"
                                      {:job-id job-id
                                       :type :job/build-structure
                                       :structure structure
                                       :target target
                                       :priority 35
                                       :source :harvest-autobuild})
                        (add-job-to-queue world queue-id job))
                      world)
                    world)))
              ecs-world
              demand-structures))))

(defn- generate-idle-build-job
  [ecs-world global-state]
  (let [queue-t (job-queue-type)
        queue-id (first (be/get-all-entities-with-component ecs-world queue-t))
        active-jobs (filter active-job? (gather-active-jobs ecs-world))
        role-t (role-type)
        status-t (status-type)
        assignment-t (job-assignment-type)
        pos-t (position-type)
        tick (:tick global-state 0)
        candidates (when (empty? active-jobs)
                     (->> (be/get-all-entities-with-component ecs-world role-t)
                          (filter (fn [agent-id]
                                    (let [status (be/get-component ecs-world agent-id status-t)
                                          assignment (be/get-component ecs-world agent-id assignment-t)]
                                      (and (or (nil? status) (:alive? status true))
                                           (nil? assignment)
                                           (:idle? (or status (c/->AgentStatus true false true nil)) true)))))))]
    (if (or (nil? queue-id) (empty? candidates))
      ecs-world
      (let [agent-id (first candidates)
            position (be/get-component ecs-world agent-id pos-t)
            target (when position
                     (build-target-for-agent ecs-world [(:q position) (:r position)]))]
        (if (nil? target)
          ecs-world
          (let [agent-id-str (str agent-id)
                agent-id-prefix (subs agent-id-str 0 (min 8 (count agent-id-str)))
                job-id (str "job-idle-build-" tick "-" agent-id-prefix)
                job {:id job-id
                     :type :job/build-structure
                     :structure :stockpile
                     :target-pos target
                     :target target
                     :priority 15
                     :created-at tick
                     :state :pending}]
            (log/log-info "[JOB:CREATE]"
                          {:job-id job-id
                           :type :job/build-structure
                           :target target
                           :priority 15
                           :source :idle-build})
            (add-job-to-queue ecs-world queue-id job)))))))

(defn- distance
  [a b]
  (hex/distance a b))

(defn- find-fruit-tile-target
  [ecs-world agent-pos]
  (let [tile-t (tile-type)
        tile-index-t (tile-index-type)
        tile-ids (be/get-all-entities-with-component ecs-world tile-t)]
    (->> tile-ids
         (keep (fn [tile-id]
                 (let [tile (be/get-component ecs-world tile-id tile-t)
                       index (be/get-component ecs-world tile-id tile-index-t)]
                   (when (and index (= :fruit (:resource tile)))
                     [(:q index) (:r index)]))))
         (sort-by #(distance agent-pos %))
         first)))

(defn- find-fruit-stockpile-target
  [ecs-world agent-pos]
  (let [stockpile-t (stockpile-type)
        tile-index-t (tile-index-type)
        stockpile-ids (be/get-all-entities-with-component ecs-world stockpile-t)]
    (->> stockpile-ids
         (keep (fn [stockpile-id]
                 (let [stockpile (be/get-component ecs-world stockpile-id stockpile-t)
                       index (be/get-component ecs-world stockpile-id tile-index-t)
                       fruit-qty (long (get (:contents stockpile {}) :fruit 0))]
                   (when (and index (pos? fruit-qty))
                     [(:q index) (:r index)]))))
         (sort-by #(distance agent-pos %))
         first)))

(defn- maybe-create-eat-job
  [world tick agent-id agent-pos]
  (when-let [target (or (find-fruit-tile-target world agent-pos)
                        (find-fruit-stockpile-target world agent-pos))]
    (let [job-id (str "need-food-" tick "-" agent-id)
          job {:id job-id
               :type :job/eat
               :priority 90
               :target-pos target
               :target target
               :resource :fruit
               :created-at tick
               :state :pending}]
      (log/log-info "[NEED:JOB-CREATED]"
                    {:agent-id agent-id
                     :need :food
                     :target target
                     :value nil
                     :job-id job-id
                     :job-type :job/eat})
      job)))

(defn- tile-entity-by-pos
  [world pos]
  (let [index-t (tile-index-type)
        tile-t (tile-type)]
    (some (fn [entity-id]
            (let [index (be/get-component world entity-id index-t)
                  tile (be/get-component world entity-id tile-t)]
              (when (and tile index (= [(:q index) (:r index)] pos))
                entity-id)))
          (be/get-all-entities world))))

(defn- maybe-drop-fruit-near-tree
  [world tree-pos]
  (let [tile-t (tile-type)
        candidate-pos (concat (hex/neighbors tree-pos) [tree-pos])
        drop-pos (some (fn [[q r]]
                         (when-let [tile-eid (tile-entity-by-pos world [q r])]
                           (let [tile (be/get-component world tile-eid tile-t)]
                             (when (and tile (nil? (:resource tile)))
                               [tile-eid [q r]]))))
                       candidate-pos)]
    (if drop-pos
      (let [[tile-eid _] drop-pos
            tile (be/get-component world tile-eid tile-t)]
        (be/add-component world tile-eid (assoc tile :resource :fruit)))
      world)))

(defn- enqueue-need-job
  [world job]
  (if-let [queue-id (first (be/get-all-entities-with-component world (job-queue-type)))]
    (add-job-to-queue world queue-id job)
    world))

(defn- generate-tree-fruit-drops
  [ecs-world global-state]
  (let [tile-t (tile-type)
        tile-index-t (tile-index-type)
        tick (:tick global-state 0)
        should-drop? (zero? (mod tick 3))
        tree-tiles (when should-drop?
                     (->> (be/get-all-entities-with-component ecs-world tile-t)
                          (keep (fn [tile-id]
                                  (let [tile (be/get-component ecs-world tile-id tile-t)
                                        index (be/get-component ecs-world tile-id tile-index-t)]
                                    (when (and index (= :tree (:resource tile)))
                                      [(:q index) (:r index)]))))
                          (take 3)))]
    (if (or (not should-drop?) (empty? tree-tiles))
      ecs-world
      (reduce (fn [world tree-pos]
                (maybe-drop-fruit-near-tree world tree-pos))
              ecs-world
              tree-tiles))))

(defn generate-basic-jobs
  "Generate basic jobs from buildings with JobQueue components."
  [ecs-world global-state]
  (let [queue-t (job-queue-type)
        buildings-with-queues (be/get-all-entities-with-component ecs-world queue-t)
        tile-t (tile-type)
        tick (:tick global-state 0)]
    (reduce (fn [acc building-id]
              (let [job-queue (be/get-component ecs-world building-id queue-t)
                    current-jobs (:jobs job-queue {})
                    pos-t (position-type)
                    position (be/get-component ecs-world building-id pos-t)
                    q (:q position)
                    r (:r position)
                    tile (be/get-component acc building-id tile-t)
                    structure (:structure tile)
                    ;; Determine job type based on building structure
                     job-type (case structure
                                :campfire :job/build-fire
                                :stockpile :job/deliver-food
                                :lumberyard :job/harvest-wood
                                :farm :job/farm
                                :orchard :job/harvest-fruit
                                :granary :job/harvest-grain
                                :quarry :job/harvest-stone
                                :house :job/improve
                                :warehouse :job/haul
                                :job/gather-wood)
                    ;; Keep deliver-food bounded at one per stockpile.
                    max-jobs (if (= structure :stockpile) 1 2)
                    existing-job-count (count current-jobs)]
                ;; Add jobs if building has fewer than max jobs
                (if (< existing-job-count max-jobs)
                  (let [job-id (str "job-" tick "-" building-id "-" existing-job-count)
                        new-job {:id job-id
                                :type job-type
                                 :priority (case job-type
                                            :job/build-fire 70
                                            :job/harvest-wood 58
                                            :job/harvest-fruit 58
                                             :job/harvest-grain 58
                                             :job/harvest-stone 58
                                             :job/farm 58
                                             :job/deliver-food 50
                                             :job/haul 50
                                             :job/improve 52
                                            50)
                                :target-pos [q r]
                                :created-at tick
                                :state :pending}]
                    (log/log-info "[JOB:CREATE]"
                                  {:job-id job-id
                                   :type job-type
                                   :building-id building-id
                                   :target [q r]
                                   :priority (:priority new-job)
                                   :source :building-queue})
                    (be/add-component acc building-id (c/->JobQueue (assoc current-jobs job-id new-job) [] {})))
                  acc)))
            ecs-world
            buildings-with-queues)))

(defn generate-need-jobs
  "Generate jobs based on agent needs."
  [ecs-world global-state]
  (let [needs-t (needs-type)
        agent-ids (be/get-all-entities-with-component ecs-world needs-t)
        tick (:tick global-state 0)]
    (reduce (fn [acc agent-id]
              (let [needs (be/get-component acc agent-id needs-t)
                    pos-t (position-type)
                    pos (be/get-component acc agent-id pos-t)]
                (if (and needs pos)
                  (let [acc-after-food
                        (if (< (:food needs) 0.3)
                          (if-let [eat-job (maybe-create-eat-job acc tick agent-id [(:q pos) (:r pos)])]
                            (enqueue-need-job acc eat-job)
                            (let [job-id (str "need-food-" tick "-" agent-id)
                                  food-job {:id job-id
                                            :type :job/gather-food
                                            :priority 90
                                            :target-pos [(:q pos) (:r pos)]
                                            :target [(:q pos) (:r pos)]
                                            :created-at tick
                                            :state :pending}]
                              (log/log-info "[NEED:JOB-CREATED]"
                                            {:agent-id agent-id
                                             :need :food
                                             :value (:food needs)
                                             :job-id job-id
                                             :job-type :job/gather-food})
                              (enqueue-need-job acc food-job)))
                          acc)
                        acc-after-sleep
                        (if (< (:sleep needs) 0.3)
                          (let [job-id (str "need-sleep-" tick "-" agent-id)
                                sleep-job {:id job-id
                                           :type :job/rest
                                           :priority 95
                                           :target-pos [(:q pos) (:r pos)]
                                           :target [(:q pos) (:r pos)]
                                           :created-at tick
                                           :state :pending}]
                            (log/log-info "[NEED:JOB-CREATED]"
                                          {:agent-id agent-id
                                           :need :sleep
                                           :value (:sleep needs)
                                           :job-id job-id})
                            (enqueue-need-job acc-after-food sleep-job))
                          acc-after-food)
                        acc-after-warmth
                        (if (< (:warmth needs) 0.3)
                          (let [job-id (str "need-warmth-" tick "-" agent-id)
                                warmth-job {:id job-id
                                            :type :job/build-fire
                                            :priority 85
                                            :target-pos [(:q pos) (:r pos)]
                                            :target [(:q pos) (:r pos)]
                                            :created-at tick
                                            :state :pending}]
                            (log/log-info "[NEED:JOB-CREATED]"
                                          {:agent-id agent-id
                                           :need :warmth
                                           :value (:warmth needs)
                                           :job-id job-id})
                            (enqueue-need-job acc-after-sleep warmth-job))
                          acc-after-sleep)]
                    acc-after-warmth)
                  acc)))
            ecs-world
            agent-ids)))

(defn process
  "Process job creation system."
  [ecs-world global-state]
  (let [world-after-basic (generate-basic-jobs ecs-world global-state)
        world-after-needs (generate-need-jobs world-after-basic global-state)
        world-after-fruit (generate-tree-fruit-drops world-after-needs global-state)
        world-after-autobuild (generate-harvest-autobuild-jobs world-after-fruit global-state)
        world-after-demand (generate-demand-driven-chop-jobs world-after-autobuild global-state)
        world-after-idle (generate-idle-build-job world-after-demand global-state)
        queue-t (job-queue-type)
        queue-entities (be/get-all-entities-with-component world-after-idle queue-t)
        total-jobs (reduce (fn [n entity-id]
                             (let [queue (be/get-component world-after-idle entity-id queue-t)]
                               (+ n (count (:jobs queue {})))))
                           0
                           queue-entities)]
    (log/log-debug "[JOB:AUTO-GEN]"
                    {:tick (:tick global-state 0)
                     :queue-count (count queue-entities)
                     :total-jobs total-jobs})
    world-after-idle))
