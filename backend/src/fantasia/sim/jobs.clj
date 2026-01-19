(ns fantasia.sim.jobs
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
  (let [job' (assoc job :worker-id agent-id :state :claimed)
        job-id (:id job)
        idx (first (keep-indexed (fn [i j] (when (= (:id j) job-id) i)) (:jobs world)))]
    (-> world
        (update-in [:agents agent-id]
                   (fn [agent]
                     (-> agent
                         (assoc :current-job job-id)
                         (assoc :idle? false)
                         (assoc-in [:status :idle?] false))))
        (update :jobs (fn [jobs]
                        (if (some? idx)
                          (assoc jobs idx job')
                          (conj jobs job')))))))

(defn mark-agent-idle [world agent-id]
  (update-in world [:agents agent-id]
             (fn [agent]
               (-> agent
                   (dissoc :current-job)
                   (assoc :idle? true)
                   (assoc-in [:status :idle?] true)))))

(defn claim-next-job! [world agent-id]
  (let [agent (get-in world [:agents agent-id])
        pending (filter #(= (:state %) :pending) (:jobs world))
        sorted (sort-by (fn [j]
                          [(- (:priority j 0))
                           (hex/distance (:pos agent) (:target j))])
                        pending)
        job (first sorted)]
    (if job
      (let [idx (first (keep-indexed (fn [i j] (when (= (:id j) (:id job)) i)) (:jobs world)))
            job (assoc job :state :claimed)]
        (println "[JOB:ASSIGN]"
                 {:agent-id agent-id
                  :job-id (:id job)
                  :type (:type job)
                  :priority (:priority job)})
        (assign-job! (if (some? idx) (assoc-in world [:jobs idx] job) world) job agent-id))
      world)))

(defn auto-assign-jobs! [world]
  (let [result (reduce (fn [w agent]
                        (if (nil? (:current-job agent))
                          (let [w' (claim-next-job! w (:id agent))]
                            (if (= w w')
                              (mark-agent-idle w (:id agent))
                              w'))
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

(defn add-item! [world pos resource qty]
  (let [tile-key (str (first pos) "," (second pos))]
    (if (zero? qty)
      world
      (update-in world [:items tile-key resource] (fnil + 0) qty))))

(defn consume-items! [world pos resource qty]
  (let [tile-key (str (first pos) "," (second pos))
        available (get-in world [:items tile-key resource] 0)
        take (min available qty)]
    (if (zero? take)
      [world 0]
      (let [new-qty (- available take)
            w1 (if (zero? new-qty)
                 (update-in world [:items tile-key] dissoc resource)
                 (assoc-in world [:items tile-key resource] new-qty))
            w2 (if (empty? (get-in w1 [:items tile-key]))
                 (update w1 :items dissoc tile-key)
                 w1)]
        [w2 take]))))

(defn create-stockpile! [world pos resource max-qty]
  (let [k (str (first pos) "," (second pos))]
    (if (get-in world [:stockpiles k])
      world
      (assoc-in world [:stockpiles k]
                {:resource resource :max-qty max-qty :current-qty 0}))))

(defn add-to-stockpile! [world pos resource qty]
  (let [k (str (first pos) "," (second pos))
        sp (get-in world [:stockpiles k])]
    (if (and sp (= (:resource sp) resource))
      (let [space (- (:max-qty sp) (:current-qty sp))
            to-add (min space qty)]
        (if (pos? to-add)
          (update-in world [:stockpiles k :current-qty] + to-add)
          world))
      world)))

(defn take-from-stockpile! [world pos resource qty]
  (let [k (str (first pos) "," (second pos))
        sp (get-in world [:stockpiles k])]
    (if (and sp (= (:resource sp) resource))
      (let [available (:current-qty sp)
            to-take (min available qty)]
        (if (pos? to-take)
          [(update-in world [:stockpiles k :current-qty] - to-take) to-take]
          [world 0]))
      [world 0])))

(defn stockpile-has-space? [world pos]
  (let [k (str (first pos) "," (second pos))
        sp (get-in world [:stockpiles k])]
    (and sp (< (:current-qty sp) (:max-qty sp)))))

(defn stockpile-space-remaining [world pos]
  (let [k (str (first pos) "," (second pos))
        sp (get-in world [:stockpiles k])]
    (if sp
      (- (:max-qty sp) (:current-qty sp))
      0)))

(defn- parse-key-pos [k]
  (let [parts (str/split k #",")]
    [(Integer/parseInt (first parts)) (Integer/parseInt (second parts))]))

(defn find-nearest-stockpile-with-space [world pos resource]
  (let [entries (filter (fn [[_ sp]] (= (:resource sp) resource)) (:stockpiles world))
        entries (filter (fn [[_ sp]] (< (:current-qty sp) (:max-qty sp))) entries)]
    (when (seq entries)
      (let [pairs (map (fn [[k v]] [(parse-key-pos k) v]) entries)]
        (first (apply min-key (fn [[p _]] (hex/distance pos p)) pairs))))))

(defn complete-build-wall! [world job]
   (let [target (:target job)
         k (str (first target) "," (second target))
         wood-required 1]
     (if-let [tile (get-in world [:tiles k])]
       (when (= (:structure tile) :wall-ghost)
         (let [wood-qty (get-in world [:items k :wood] 0)]
           (if (>= wood-qty wood-required)
             (let [world' (-> world
                            (assoc-in [:tiles k :structure] :wall)
                            (update-in [:items k :wood] - wood-required)
                            (cond-> (zero? (- wood-qty wood-required))
                              (update :items dissoc k)))]
               (println "[JOB:COMPLETE]"
                        {:type :job/build-wall
                         :target target
                         :outcome (format "Wall built at %s, consumed 1 wood" (pr-str target))})
               world')
             world)))
       world)))

(defn complete-chop-tree! [world job]
   (let [target (:target job)
         k (str (first target) "," (second target))]
     (if (= (get-in world [:tiles k :resource]) :tree)
       (let [world' (-> world
                        (assoc-in [:tiles k :resource] nil)
                        (add-item! target :wood 5))]
         (println "[JOB:COMPLETE]"
                  {:type :job/chop-tree
                   :target target
                   :outcome (format "Produced 5 wood at %s" (pr-str target))})
         world')
       world)))

(defn complete-haul! [world job agent-id]
   (let [agent (get-in world [:agents agent-id])
         to-pos (or (:to-pos job) (:target job))
         hauling (get-in agent [:inventories :hauling] {})
         inv (:inventory agent {})
          merged (if (seq hauling)
                   hauling
                   inv)

         items-hauled (vec (filter (comp pos? second) merged))
         tile-key (str (first to-pos) "," (second to-pos))
         stockpile (get-in world [:stockpiles tile-key])
         add-fn (fn [w pos res qty]
                  (if (and stockpile (= res (:resource stockpile)))
                    (add-to-stockpile! w pos res qty)
                    (add-item! w pos res qty)))]


     (when (seq items-hauled)
       (println "[JOB:COMPLETE]"
                {:type :job/haul
                 :target to-pos
                 :outcome (format "Hauled %d item types to %s" (count items-hauled) (pr-str to-pos))}))
      (-> (reduce (fn [w [res qty]]
                    (if (pos? qty)
                      (add-fn w to-pos res qty)
                      w))
                  world
                  (seq merged))
          (assoc-in [:agents agent-id :inventories :hauling] {})
          (update-in [:agents agent-id] dissoc :inventory))))


(defn complete-eat! [world job agent-id]
   (let [target (:target job)
         [w' consumed] (consume-items! world target :food 1)]
     (if (pos? consumed)
       (let [world' (assoc-in w' [:agents agent-id :needs :food] 1.0)]
         (println "[JOB:COMPLETE]"
                  {:type :job/eat
                   :target target
                   :outcome (format "Consumed 1 food, need food=1.0")})
         world')
       w')))

(defn complete-sleep! [world agent-id]
   (let [world' (-> world
                   (update-in [:agents agent-id] dissoc :asleep?)
                   (assoc-in [:agents agent-id :needs :sleep] 1.0))]
     (println "[JOB:COMPLETE]"
              {:type :job/sleep
               :agent-id agent-id
               :outcome "Agent woke up, need sleep=1.0"})
     world'))

(defn complete-deliver-food! [world job agent-id]
   (let [agent (get-in world [:agents agent-id])
         target (:target job)
         food-in-inventory (get-in agent [:inventory :food] 0)]
     (if (pos? food-in-inventory)
        (let [[w' delivered] (take-from-stockpile! world target :food 100)
              space-remaining (stockpile-space-remaining w' target)
              to-store (min food-in-inventory space-remaining)
              w'' (if (pos? to-store)
                    (add-to-stockpile! w' target :food to-store)
                    w')
              new-inventory (- food-in-inventory to-store)]
          (when (pos? to-store)
            (println "[JOB:COMPLETE]"
                     {:type :job/deliver-food
                      :target target
                      :outcome (format "Delivered %d food to stockpile at %s" to-store (pr-str target))}))
          (let [w-final (-> w''
                             (assoc-in [:agents agent-id :inventories :hauling :food] new-inventory)
                             (assoc-in [:agents agent-id :inventory :food] new-inventory))]
            (if (zero? new-inventory)
              (-> w-final
                  (update-in [:agents agent-id :inventories :hauling] dissoc :food)
                  (update-in [:agents agent-id :inventory] dissoc :food))
              w-final)))

       world)))

(defn complete-job! [world agent-id]
   (if-let [job-id (get-in world [:agents agent-id :current-job])]
      (let [idx (first (keep-indexed (fn [i j] (when (= (:id j) job-id) i)) (:jobs world)))
            job (get-in world [:jobs idx])
            world (update-in world [:agents agent-id] dissoc :current-job)
            world (update world :jobs (fn [js] (vec (remove #(= (:id %) job-id) js))))
            world (case (:type job)
                    :job/build-wall (complete-build-wall! world job)
                    :job/chop-tree (complete-chop-tree! world job)
                    :job/haul (complete-haul! world job agent-id)
                    :job/eat (complete-eat! world job agent-id)
                    :job/sleep (complete-sleep! world agent-id)
                    :job/deliver-food (complete-deliver-food! world job agent-id)
                    world)
            reassigned (claim-next-job! world agent-id)]
        (if (= reassigned world)
          (mark-agent-idle world agent-id)
          reassigned))
      world))

(defn advance-job! [world agent-id delta]
  (if-let [job-id (get-in world [:agents agent-id :current-job])]
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

(defn get-agent-job [world agent-id]
  (when-let [job-id (get-in world [:agents agent-id :current-job])]
    (first (filter #(= (:id %) job-id) (:jobs world)))))

(defn job-complete? [job]
  (>= (:progress job 0.0) (:required job 1.0)))

(defn adjacent-to-job? [world agent-id]
  (if-let [job (get-agent-job world agent-id)]
    (let [apos (get-in world [:agents agent-id :pos])
          t (:target job)]
      (<= (hex/distance apos t) 1))
    false))

(defn pickup-items! [world agent-id pos resource qty]
  (let [[w' consumed] (consume-items! world pos resource qty)]
    (if (pos? consumed)
      (-> w'
          (update-in [:agents agent-id :inventories :hauling resource] (fnil + 0) consumed)
          (update-in [:agents agent-id :inventory resource] (fnil + 0) consumed))
      w')))

(defn drop-items! [world agent-id]
  (let [agent (get-in world [:agents agent-id])
        pos (:pos agent)
        hauling (get-in agent [:inventories :hauling] {})
        inv (:inventory agent {})
        merged (merge-with + hauling inv)]
    (-> (reduce (fn [w [res qty]] (add-item! w pos res qty))
                world
                (seq merged))
        (assoc-in [:agents agent-id :inventories :hauling] {})
        (update-in [:agents agent-id] dissoc :inventory))))

(defn generate-haul-jobs-for-items! [world threshold]
  (let [entries (:items world)
        jobs-to-add
        (->> entries
             (mapcat (fn [[tile-key items]]
                       (let [pos (parse-key-pos tile-key)]
                         (for [[res qty] items
                               :when (>= qty threshold)
                               :let [nearest (find-nearest-stockpile-with-space world pos res)]
                               :when nearest]
                           (let [[sp-pos _] nearest]
                             (assoc (create-job :job/haul pos)
                                    :from-pos pos :to-pos sp-pos :resource res :qty threshold
                                    :state :pickup :stage :pickup)))))))]
    (when (seq jobs-to-add)
      (println "[JOB:GENERATE]"
               {:type :job/haul
                :count (count jobs-to-add)
                :tick (:tick world)}))
    (reduce (fn [w job] (update w :jobs conj job))
            world
            jobs-to-add)))

(defn generate-need-jobs! [world]
  (reduce (fn [w agent]
            (let [needs (:needs agent)
                  thresholds (or (:need-thresholds agent) {})
                  food (get needs :food 1.0)
                  sleep (get needs :sleep 1.0)
                  food-hungry (get thresholds :food-hungry 0.3)
                  sleep-tired (get thresholds :sleep-tired 0.3)
                  pos (:pos agent)
                  agent-id (:id agent)
                  food-pos (or (first (keep (fn [[k items]] (when (get items :fruit) (parse-key-pos k))) (:items w))) pos)
                  has-eat-job? (some #(and (= (:type %) :job/eat) (= (:target %) food-pos)) (:jobs w))
                  has-sleep-job? (some #(and (= (:type %) :job/sleep) (= (:target %) pos)) (:jobs w))
                  already-has-job? (some #(= (:worker-id %) agent-id) (:jobs w))]
              (cond-> w
                (and (< food food-hungry) (not has-eat-job?) (not already-has-job?)) (update :jobs conj (create-job :job/eat food-pos))
                (and (< sleep sleep-tired) (not has-sleep-job?) (not already-has-job?)) (update :jobs conj (create-job :job/sleep pos)))))
        world
        (:agents world)))

(defn generate-deliver-food-jobs! [world]
  (let [entries (:items world)
        jobs-to-add
        (->> entries
             (mapcat (fn [[tile-key items]]
                       (let [pos (parse-key-pos tile-key)]
                         (for [[res qty] items
                               :when (and (= res :food) (pos? qty))
                               :let [nearest (find-nearest-stockpile-with-space world pos :food)]
                               :when nearest]
                           (let [[sp-pos _] nearest]
                             (assoc (create-job :job/deliver-food sp-pos)
                                    :from-pos pos :to-pos sp-pos :resource :food :qty qty)))))))]
    (reduce (fn [w job] (update w :jobs conj job))
            world
            jobs-to-add)))

(defn generate-chop-jobs! [world]
  (let [tree-tiles (keep (fn [[k tile]]
                           (when (= (:resource tile) :tree)
                             [(parse-key-pos k) tile]))
                         (:tiles world))
        jobs-to-add (map (fn [[pos _]]
                           (create-job :job/chop-tree pos))
                         tree-tiles)]
    (when (seq jobs-to-add)
      (println "[JOB:GENERATE]"
               {:type :job/chop-tree
                :count (count jobs-to-add)
                :tick (:tick world)}))
    (reduce (fn [w job] (update w :jobs conj job))
            world
            jobs-to-add)))

(defn auto-generate-jobs! [world]
  (-> world
      (generate-need-jobs!)
      (generate-chop-jobs!)
      (generate-haul-jobs-for-items! 5)
      (generate-deliver-food-jobs!)))
