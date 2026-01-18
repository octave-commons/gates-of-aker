(ns fantasia.sim.jobs
  (:require [fantasia.sim.hex :as hex]
            [fantasia.sim.spatial :as spatial]
            [clojure.string :as str]))

(def job-priorities
  {:job/eat 100
   :job/sleep 90
   :job/chop-tree 60
   :job/haul 50
   :job/build-wall 40})

(defn create-job
  [job-type target]
  {:id (random-uuid)
   :type job-type
   :target target
   :worker-id nil
   :progress 0.0
   :required 1.0
   :state :pending
   :priority (get job-priorities job-type 50)})

(defn assign-job!
  [world job agent-id]
  (let [job' (assoc job :worker-id agent-id :state :claimed)
        agent (get-in world [:agents agent-id])]
    (-> world
        (update-in [:agents agent-id] assoc :current-job (:id job'))
        (update :jobs conj job'))))

(defn claim-next-job!
  [world agent-id]
  (let [agent (get-in world [:agents agent-id])]
        pending-jobs (->> (:jobs world)
                           (filter #(= (:state %) :pending))
                           (sort-by (fn [j] [(:priority j) (hex/distance (:pos agent) (:target j))]))
                           (vec))]
    (if-let [job (first pending-jobs)]
      (assign-job! world job agent-id)
      world)))

(defn auto-assign-jobs!
  [world]
  (let [idle-agents (->> (:agents world)
                        (filter #(nil? (:current-job %)))
                        (map :id)
                        (vec)]
    (reduce claim-next-job! world idle-agents)))

(defn add-item!
  [world pos resource qty]
  (let [tile-key (str (first pos) "," (second pos))]
    (if (zero? qty)
      world
      (update-in world [:items tile-key resource] (fnil + 0) qty))))

(defn consume-items!
  [world pos resource qty]
  (let [tile-key (str (first pos) "," (second pos))]
        available (get-in world [:items tile-key resource] 0)
        consumed (min available qty)]
    (if (zero? consumed)
      [world 0]
      [(let [new-qty (- available qty)
             w1 (if (zero? new-qty)
                     (update-in world [:items tile-key] dissoc resource)
                     (assoc-in world [:items tile-key resource] new-qty])]
         (if (empty? (get-in w1 [:items tile-key]))
           (update w1 :items dissoc tile-key)
           w1))
       consumed])))

(defn create-stockpile!
  [world pos resource max-qty]
  (let [tile-key (str (first pos) "," (second pos))]
    (if (get-in world [:stockpiles tile-key])
      world
      (assoc-in world [:stockpiles tile-key]
                {:resource resource :max-qty max-qty :current-qty 0}))))

(defn add-to-stockpile!
  [world pos resource qty]
  (let [tile-key (str (first pos) "," (second pos))]
        stockpile (get-in world [:stockpiles tile-key])]
    (if (and stockpile (= (:resource stockpile) resource))
      (let [space (- (:max-qty stockpile) (:current-qty stockpile))
            to-add (min space qty)]
        (if (> to-add 0)
          (update-in world [:stockpiles tile-key :current-qty] + to-add)
          world))
      world)))

(defn take-from-stockpile!
  [world pos resource qty]
  (let [tile-key (str (first pos) "," (second pos))]
        stockpile (get-in world [:stockpiles tile-key])]
    (if (and stockpile (= (:resource stockpile) resource))
      (let [available (:current-qty stockpile)
            to-take (min available qty)]
        (if (> to-take 0)
          [(update-in world [:stockpiles tile-key :current-qty] - to-take) to-take]
          [world 0]))
      [world 0])))

(defn stockpile-has-space?
  [world pos]
  (let [tile-key (str (first pos) "," (second pos))]
        stockpile (job/get-in world [:stockpiles tile-key])]
    (and stockpile (< (:current-qty stockpile) (:max-qty stockpile)))))

(defn stockpile-space-remaining
  [world pos]
  (let [tile-key (str (first pos) "," (second pos))]
        stockpile (get-in world [:stockpiles tile-key])]
    (if stockpile
      (- (:max-qty stockpile) (:current-qty stockpile))
      0)))

(defn find-nearest-stockpile-with-space
  [world pos resource]
  (let [stockpile-entries (->> (:stockpiles world)
                               (filter (fn [[_ sp]] (= (:resource sp) resource)))
                               (filter (fn [[_ sp]] (< (:current-qty sp) (:max-qty sp)))
                               vec)]
    (if (seq stockpile-entries)
      (let [stockpiles (map (fn [[k v]] 
                                       (let [parts (str/split k #",")]
                                             [(read-string (first parts)) (read-string (second parts))]
                                             v]))
                                   stockpile-entries)]
            (first (apply min-key (fn [[sp-pos _]] (hex/distance pos sp-pos)) stockpiles)))
      nil)))

(defn complete-build-wall!
  [world job]
  (let [target (:target job)
        [q r] target
        tile-key (str q "," r)]
    (if-let [tile (get-in world [:tiles tile-key])]
      (when (= (:structure tile) :wall-ghost)
        (let [wood-pos tile-key
              wood-qty (get-in world [:items wood-pos :wood] 0)
              wood-required 1]
          (if (>= wood-qty wood-required)
            (-> world
                (assoc-in [:tiles tile-key :structure] :wall)
                (update-in [:items wood-pos :wood] - wood-required)
                (cond-> (zero? (- wood-qty wood-required))
                  (update :items dissoc wood-pos)))
            world)))
      world)))

(defn complete-chop-tree!
  [world job]
  (let [target (:target job)
        [q r] target
        tile-key (str q "," r)
        tile (get-in world [:tiles tile-key])]
    (when (and tile (= (:resource tile) :tree))
      (-> world
          (assoc-in [:tiles tile-key :resource] nil)
          (add-item! target :wood 5)))))

(defn complete-haul!
  [world job agent-id]
  (let [agent (get-in world [:agents agent-id])]
        to-pos (:target job)
        inventory (:inventory agent {:wood 0 :food 0})]
    (reduce (fn [w [resource qty]]
               (if (> qty 0)
                 (add-item! w to-pos resource qty)
                 w))
            world
            (seq inventory))))

(defn complete-eat!
  [world job agent-id]
  (let [target (:target job)]
    (let [[world' consumed] (consume-items! world target :food 1)]
      (if (> consumed 0)
        (update-in world' [:agents agent-id :needs :food] (constantly 1.0))
        world')))

(defn complete-sleep!
  [world agent-id]
  (-> world
      (update-in [:agents agent-id] assoc :asleep? false)
      (update-in [:agents agent-id :needs :sleep] (constantly 1.0))))

(defn complete-job!
  [world agent-id]
  (if-let [job-id (get-in world [:agents agent-id :current-job])]
    (let [job-idx (first (keep-indexed (fn [i j] (when (= (:id j) job-id) i)) (:jobs world)))
          job (get-in world [:jobs job-idx])]
      (if job
        (let [job-type (:type job)
              world' (-> world
                          (update-in [:agents agent-id] dissoc :current-job)
                          (update :jobs (fn [job-list] (vec (remove #(= (:id %) job-id) job-list))))]
              complete-fn (case job-type
                                 :job/build-wall complete-build-wall!
                                 :job/chop-tree complete-chop-tree!
                                 :job/haul (fn [w _] (complete-haul! w job agent-id))
                                 :job/eat (fn [w _] (complete-eat! w job agent-id))
                                 :job/sleep (fn [w _] (complete-sleep! w agent-id))
                                 (fn [w _] w))]
          (complete-fn world' job))
        world))
    world)))

(defn advance-job!
  [world agent-id delta]
  (if-let [job-id (get-in world [:agents agent-id :current-job])]
    (let [job-idx (first (keep-indexed (fn [i j] (when (= (:id j) job-id) i)) (:jobs world)))
          job (get-in world [:jobs job-idx])]
      (if job
        (let [new-state (if (= (:state job) :claimed)
                          :in-progress
                          (:state job))
              new-progress (min (+ (:progress job) delta) (:required job))
              job' (assoc job :state new-state :progress new-progress)
              world' (assoc-in world [:jobs job-idx] job')]
          (if (>= new-progress (:required job))
            (complete-job! world' agent-id)
            world'))
        world))
    world)))

(defn get-agent-job
  [world agent-id]
  (if-let [job-id (get-in world [:agents agent-id :current-job])]
    (first (filter #(= (:id %) job-id) (:jobs world)))
    nil)))

(defn job-complete?
  [job]
  (>= (:progress job 0.0) (:required job 1.0)))

(defn adjacent-to-job?
  [world agent-id]
  (if-let [job (get-agent-job world agent-id)]
    (let [agent-pos (get-in world [:agents agent-id :pos])]
          job-target (:target job)]
      (<= (hex/distance agent-pos job-target) 1))
    false))

(defn pickup-items!
  [world agent-id pos resource qty]
  (let [[world' consumed] (consume-items! world pos resource qty)]
    (if (> consumed 0)
      (update-in world' [:agents agent-id :inventory resource] (fnil + 0) consumed)
      world')))

(defn drop-items!
  [world agent-id]
  (let [agent (get-in world [:agents agent-id])]
        pos (:pos agent)
        inventory (:inventory agent {})]
    (reduce (fn [w [resource qty]]
               (add-item! w pos resource qty))
            (-> world
                (update-in [:agents agent-id] dissoc :inventory))
            (seq inventory))))
