(ns fantasia.sim.jobs.providers
   (:require [clojure.set :as set]
             [clojure.string :as str]
             [fantasia.sim.hex :as hex]
             [fantasia.sim.jobs :as jobs]
             [fantasia.sim.constants :as const]))

(defn- parse-key-pos [k]
  (let [parts (str/split k #",")]
    [(Integer/parseInt (first parts)) (Integer/parseInt (second parts))]))

(defn- stockpile-accepts? [sp resource]
  (let [sp-resource (:resource sp)
        wood? #(contains? #{:wood :log} %)]
    (or (= sp-resource resource)
        (and (wood? sp-resource) (wood? resource)))))

(defn- stockpile-resource-set [sp]
  (let [sp-resource (:resource sp)]
    (if (contains? #{:wood :log} sp-resource)
      #{:wood :log}
      #{sp-resource})))

(defn- total-item-qty [items resource]
  (reduce (fn [acc [_ item-map]]
            (+ acc (get item-map resource 0)))
          0
          items))

(defn- tiles-with-resources [world resources]
  (let [resource-set (set resources)]
    (->> (:tiles world)
         (keep (fn [[k tile]]
                 (when (contains? resource-set (:resource tile))
                   (parse-key-pos k)))))))

(defn- items-with-resource [world resource]
  (->> (:items world)
       (keep (fn [[k items]]
               (when (pos? (get items resource 0))
                 (parse-key-pos k))))))

(defn- find-nearest-item [world pos resource]
  (->> (items-with-resource world resource)
       (sort-by (fn [p] (hex/distance pos p)))
       first))

(defn- item-targets [world origin resources]
  (let [resource-set (set resources)]
    (->> (:items world)
         (keep (fn [[k items]]
                 (when-let [res (some (fn [[res qty]]
                                        (when (and (contains? resource-set res) (pos? qty)) res))
                                      items)]
                   [(parse-key-pos k) res])))
         (remove (fn [[pos _]] (= pos origin)))
         (sort-by (fn [[pos _]] (hex/distance origin pos))))))

(defn- positions-within-radius [origin radius]
  (loop [frontier #{origin}
         visited #{origin}
         step 0]
    (if (>= step radius)
      visited
      (let [next (->> frontier
                      (mapcat hex/neighbors)
                      set)
            next (set/difference next visited)]
        (recur next (into visited next) (inc step))))))

(defn- empty-build-site? [world pos]
  (let [k (str (first pos) "," (second pos))
        tile (get-in world [:tiles k])]
    (and (hex/in-bounds? (:map world) pos)
         (nil? (:structure tile))
         (nil? (:resource tile)))))

(defn- structure-providers [world]
  (->> (:tiles world)
       (keep (fn [[k tile]]
               (when-let [provider (get jobs/job-provider-config (:structure tile))]
                 (let [pos (parse-key-pos k)]
                   (assoc provider :pos pos :structure (:structure tile))))))))

(defn- job-provider-pos [job]
  (or (:provider-pos job)
      (:building-pos job)
      (:from-pos job)))

(defn- provider-job-count [world provider]
  (count (filter (fn [job]
                   (and (= (:type job) (:job-type provider))
                        (= (job-provider-pos job) (:pos provider))
                        (not= (:state job) :completed)))
                 (:jobs world))))

(defn- provider-open-slots [world provider]
  (max 0 (- (:max-jobs provider 1) (provider-job-count world provider))))

(defn- existing-structures [world]
  (->> (:tiles world)
       (keep (fn [[_ tile]] (:structure tile)))
       set))

(defn- structure-level [tile]
  (long (or (:level tile) 1)))

(defn- pick-build-structure [world rng]
  (let [present (existing-structures world)
        options (filter (fn [structure]
                          (or (not (contains? jobs/unique-structures structure))
                              (not (contains? present structure))))
                        jobs/build-structure-options)
        idx (when (seq options)
              (.nextInt rng (count options)))]
    (when idx
      (nth options idx))))

(defn- build-sites [world origin]
  (->> (positions-within-radius origin 4)
       (filter #(empty-build-site? world %))
       (sort-by (fn [pos] (hex/distance origin pos)))))

(defn- provider-existing-targets [world provider]
  (->> (:jobs world)
       (filter (fn [job]
                 (and (= (:type job) (:job-type provider))
                      (= (job-provider-pos job) (:pos provider)))))
       (map :target)
       set))

(defn- harvest-resource-for-job [job-type]
  (case job-type
    :job/harvest-wood :tree
    :job/harvest-fruit :tree
    :job/harvest-grain :grain
    :job/harvest-stone :rock
    :tree))

(defn- job-resource-for-harvest [job-type]
  (case job-type
    :job/harvest-wood :log
    :job/harvest-fruit :fruit
    :job/harvest-grain :grain
    :job/harvest-stone :rock
    :log))

(defn- provider-resource-targets [world provider resources]
  (->> (tiles-with-resources world resources)
       (remove (provider-existing-targets world provider))
       (sort-by (fn [p] (hex/distance (:pos provider) p)))))

(defn generate-provider-harvest-jobs! [world]
  (reduce
   (fn [w provider]
     (if (contains? #{:job/harvest-wood :job/harvest-fruit :job/harvest-grain :job/harvest-stone}
                    (:job-type provider))
       (let [open (provider-open-slots w provider)
             target-resource (harvest-resource-for-job (:job-type provider))
             stockpile-key (str (first (:pos provider)) "," (second (:pos provider)))
             stockpile (get-in w [:stockpiles stockpile-key])
             space (when stockpile (- (:max-qty stockpile) (:current-qty stockpile)))
             targets (provider-resource-targets w provider [target-resource])
             jobs (if (pos? (or space 0))
                    (->> targets
                         (take open)
                         (map (fn [target]
                                (assoc (jobs/create-job (:job-type provider) target)
                                       :resource (job-resource-for-harvest (:job-type provider))
                                       :provider-pos (:pos provider)
                                       :building-pos (:pos provider)))))
                    [])
             w' (reduce (fn [acc job] (update acc :jobs conj job)) w jobs)]
         w')
       w))
   world
   (structure-providers world)))

(defn generate-provider-mine-jobs! [world]
  (reduce
   (fn [w provider]
     (if (= (:job-type provider) :job/mine)
       (let [open (provider-open-slots w provider)
             targets (provider-resource-targets w provider jobs/mineral-types)
             jobs (->> targets
                       (take open)
                       (map (fn [target]
                              (let [tile-key (str (first target) "," (second target))
                                    resource (get-in w [:tiles tile-key :resource])]
                                (assoc (jobs/create-job :job/mine target)
                                       :resource resource
                                       :provider-pos (:pos provider))))))
             w' (reduce (fn [acc job] (update acc :jobs conj job)) w jobs)]
         w')
       w))
   world
   (structure-providers world)))

(defn generate-provider-builder-jobs! [world]
  (reduce
   (fn [w provider]
     (if (= (:job-type provider) :job/builder)
       (let [open (provider-open-slots w provider)
             r (java.util.Random. (long (+ (:seed w) (* 31 (:tick w)) (hash (:pos provider)))))
             targets (->> (build-sites w (:pos provider))
                          (remove (provider-existing-targets w provider))
                          (take open))
             jobs (->> targets
                       (keep (fn [target]
                               (when-let [structure (pick-build-structure w r)]
                                 (assoc (jobs/create-job :job/builder target)
                                        :structure structure
                                        :provider-pos (:pos provider)
                                        :stockpile (when (= structure :stockpile)
                                                     {:resource :log :max-qty 120}))))))
             w' (reduce (fn [acc job] (update acc :jobs conj job)) w jobs)]
         w')
       w))
   world
   (structure-providers world)))

(defn- improvable-targets [world provider]
  (->> (:tiles world)
       (keep (fn [[k tile]]
               (let [structure (:structure tile)
                     level (structure-level tile)]
                 (when (and (contains? jobs/improvable-structures structure)
                             (< level const/max-structure-level))
                   (parse-key-pos k)))))
       (remove (provider-existing-targets world provider))
       (sort-by (fn [pos] (hex/distance (:pos provider) pos)))))

(defn generate-provider-improve-jobs! [world]
  (reduce
   (fn [w provider]
     (if (= (:job-type provider) :job/improve)
       (let [open (provider-open-slots w provider)
             targets (improvable-targets w provider)
             jobs (->> targets
                       (take open)
                       (map (fn [target]
                              (assoc (jobs/create-job :job/improve target)
                                     :provider-pos (:pos provider)))))
             w' (reduce (fn [acc job] (update acc :jobs conj job)) w jobs)]
         w')
       w))
   world
   (structure-providers world)))

(defn- smeltable-ores [world]
  (filter #(pos? (total-item-qty (:items world) %)) jobs/ore-types))

(defn generate-provider-smelt-jobs! [world]
  (reduce
   (fn [w provider]
     (if (= (:job-type provider) :job/smelt)
       (let [open (provider-open-slots w provider)
             ores (vec (smeltable-ores w))
             jobs (->> (range open)
                       (keep (fn [idx]
                               (when (seq ores)
                                 (let [ore (nth ores (mod idx (count ores)))
                                       from-pos (find-nearest-item w (:pos provider) ore)]
                                   (assoc (jobs/create-job :job/smelt (:pos provider))
                                          :resource ore
                                          :output (get jobs/ore->ingot ore)
                                          :provider-pos (:pos provider)
                                          :from-pos from-pos))))))
             w' (reduce (fn [acc job] (update acc :jobs conj job)) w jobs)]
         w')
       w))
   world
   (structure-providers world)))

(defn generate-stockpile-haul-jobs! [world]
  (let [existing-keys (->> (:jobs world)
                           (filter #(= (:type %) :job/haul))
                           (keep (fn [job]
                                   (when (and (:from-pos job) (:to-pos job) (:resource job))
                                     [(:from-pos job) (:to-pos job) (:resource job)])))
                           set)
        jobs-to-add
        (->> (:stockpiles world)
             (mapcat (fn [[tile-key sp]]
                       (let [pos (parse-key-pos tile-key)
                             open (max 0 (- 2 (count (filter #(and (= (:type %) :job/haul)
                                                                     (= (:to-pos %) pos))
                                                            (:jobs world)))))
                             resource-set (stockpile-resource-set sp)
                             target-items (item-targets world pos resource-set)]
                         (for [[from-pos resource] (take open target-items)
                               :let [job-key [from-pos pos resource]]
                               :when (not (contains? existing-keys job-key))]
                           (assoc (jobs/create-job :job/haul from-pos)
                                  :from-pos from-pos
                                  :to-pos pos
                                  :resource resource
                                  :qty 1
                                  :provider-pos pos
                                  :stage :pickup
                                  :state :pickup))))))]
    (reduce (fn [w job] (update w :jobs conj job))
            world
            jobs-to-add)))

(defn generate-provider-jobs! [world]
  (-> world
      (generate-provider-harvest-jobs!)
      (generate-provider-mine-jobs!)
      (generate-provider-builder-jobs!)
      (generate-provider-improve-jobs!)
      (generate-provider-smelt-jobs!)
      (generate-stockpile-haul-jobs!)))

(defn seed-initial-jobs [world desired-count]
  (let [world' (generate-provider-jobs! world)
        jobs (:jobs world')
        required-types [:job/builder :job/improve :job/mine]
        required (->> required-types
                      (keep (fn [job-type]
                              (first (filter #(= (:type %) job-type) jobs)))))
        required-set (set required)
        remaining (remove required-set jobs)
        ordered (sort-by (fn [job] (- (:priority job 0))) remaining)
        room (max 0 (- desired-count (count required)))
        trimmed (vec (take room ordered))
        world' (assoc world' :jobs (vec (concat required trimmed)))
        providers (vec (filter #(= (:job-type %) :job/builder)
                               (structure-providers world')))
        missing (max 0 (- desired-count (count (:jobs world'))))]
    (loop [w world'
           remaining missing
           idx 0
           attempts 0]
      (if (or (zero? remaining) (empty? providers) (>= attempts (* desired-count 4)))
        w
        (let [provider (nth providers (mod idx (count providers)))
              r (java.util.Random. (long (+ (:seed w) (* 61 idx) (hash (:pos provider)))))
              used-targets (set (map :target (:jobs w)))
              target (->> (build-sites w (:pos provider))
                          (remove used-targets)
                          first)
              structure (when target (pick-build-structure w r))]
          (if (and target structure)
            (recur (update w :jobs conj
                           (assoc (jobs/create-job :job/builder target)
                                  :structure structure
                                  :provider-pos (:pos provider)
                                  :stockpile (when (= structure :stockpile)
                                               {:resource :log :max-qty 120})))
                   (dec remaining)
                   (inc idx)
                   (inc attempts))
            (recur w remaining (inc idx) (inc attempts))))))))

(defn auto-generate-jobs! [world]
  (-> world
      (jobs/generate-need-jobs!)
      (generate-provider-jobs!)))
