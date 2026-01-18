(ns fantasia.sim.jobs
  (:require [fantasia.sim.hex :as hex]
            [clojure.string :as str]))

(def job-priorities
  {:job/eat 100
   :job/sleep 90
   :job/chop-tree 60
   :job/haul 50
   :job/build-wall 40})

(defn create-job [job-type target]
  {:id (random-uuid)
   :type job-type
   :target target
   :worker-id nil
   :progress 0.0
   :required 1.0
   :state :pending
   :priority (get job-priorities job-type 50)})

(defn assign-job! [world job agent-id]
  (let [job' (assoc job :worker-id agent-id :state :claimed)]
    (-> world
        (update-in [:agents agent-id] assoc :current-job (:id job'))
        (update :jobs conj job'))))

(defn claim-next-job! [world agent-id]
  (let [agent (get-in world [:agents agent-id])
        pending (filter #(= (:state %) :pending) (:jobs world))
        sorted (sort-by (fn [j]
                          [(- (:priority j 0))
                           (hex/distance (:pos agent) (:target j))])
                        pending)
        job (first sorted)]
    (if job
      (assign-job! world job agent-id)
      world)))

(defn auto-assign-jobs! [world]
  (reduce (fn [w agent]
            (if (nil? (:current-job agent))
              (claim-next-job! w (:id agent))
              w))
        world
        (:agents world)))

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
            (-> world
                (assoc-in [:tiles k :structure] :wall)
                (update-in [:items k :wood] - wood-required)
                (cond-> (zero? (- wood-qty wood-required))
                  (update :items dissoc k)))
            world)))
      world)))

(defn complete-chop-tree! [world job]
  (let [target (:target job)
        k (str (first target) "," (second target))]
    (if (= (get-in world [:tiles k :resource]) :tree)
      (-> world
          (assoc-in [:tiles k :resource] nil)
          (add-item! target :wood 5))
      world)))

(defn complete-haul! [world job agent-id]
  (let [agent (get-in world [:agents agent-id])
        to-pos (:target job)
        inv (:inventory agent {})]
    (reduce (fn [w [res qty]]
              (if (pos? qty)
                (add-item! w to-pos res qty)
                w))
            world
            (seq inv))))

(defn complete-eat! [world job agent-id]
  (let [target (:target job)
        [w' consumed] (consume-items! world target :food 1)]
    (if (pos? consumed)
      (assoc-in w' [:agents agent-id :needs :food] 1.0)
      w')))

(defn complete-sleep! [world agent-id]
  (-> world
      (update-in [:agents agent-id] dissoc :asleep?)
      (assoc-in [:agents agent-id :needs :sleep] 1.0)))

(defn complete-job! [world agent-id]
  (if-let [job-id (get-in world [:agents agent-id :current-job])]
    (let [idx (first (keep-indexed (fn [i j] (when (= (:id j) job-id) i)) (:jobs world)))
          job (get-in world [:jobs idx])
          world (update-in world [:agents agent-id] dissoc :current-job)
          world (update world :jobs (fn [js] (vec (remove #(= (:id %) job-id) js))))]
      (case (:type job)
        :job/build-wall (complete-build-wall! world job)
        :job/chop-tree (complete-chop-tree! world job)
        :job/haul (complete-haul! world job agent-id)
        :job/eat (complete-eat! world job agent-id)
        :job/sleep (complete-sleep! world agent-id)
        world)
      world)
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
      (update-in w' [:agents agent-id :inventory resource] (fnil + 0) consumed)
      w')))

(defn drop-items! [world agent-id]
  (let [agent (get-in world [:agents agent-id])
        pos (:pos agent)
        inv (:inventory agent {})]
    (-> (reduce (fn [w [res qty]] (add-item! w pos res qty))
                world
                (seq inv))
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
                             (assoc (create-job :job/haul sp-pos)
                                    :from-pos pos :to-pos sp-pos :resource res :qty threshold)))))))]
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

(defn auto-generate-jobs! [world]
  (-> world
      (generate-need-jobs!)
      (generate-haul-jobs-for-items! 5)))
