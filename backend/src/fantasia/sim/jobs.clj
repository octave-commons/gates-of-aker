(ns fantasia.sim.jobs
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [fantasia.sim.biomes :as biomes]
            [fantasia.sim.hex :as hex]))

(defn tile-key [q r] [q r])
(defn parse-tile-key [[q r]] [q r])
(defn parse-key-pos [[q r]] [q r])

(def job-priorities
  {:job/eat 100
    :job/warm-up 95
    :job/sleep 90
    :job/hunt 75
    :job/chop-tree 60
    :job/mine 60
   :job/harvest-wood 58
   :job/harvest-fruit 58
   :job/harvest-grain 58
   :job/harvest-stone 58
   :job/farm 58
   :job/smelt 57
   :job/build-house 55
   :job/improve 52
   :job/haul 50
   :job/deliver-food 45
    :job/build-wall 40
    :job/builder 38
    :job/build-structure 35})

(def ^:private food-resources
  #{:fruit :berry :raw-meat :cooked-meat :stew :food})

(def ^:private food-item-order
  [:cooked-meat :stew :raw-meat :fruit :berry :food])

(defn- player-agent?
  [agent]
  (= (:faction agent) :player))

(defn- alive-agent?
  [agent]
  (get-in agent [:status :alive?] true))

(def ore-types
  [:ore-iron :ore-copper :ore-tin :ore-gold :ore-silver :ore-aluminum :ore-lead])

(def ore->ingot
  {:ore-iron :ingot-iron
   :ore-copper :ingot-copper
   :ore-tin :ingot-tin
   :ore-gold :ingot-gold
   :ore-silver :ingot-silver
   :ore-aluminum :ingot-aluminum
   :ore-lead :ingot-lead})

(def mineral-types
  (into #{:rock} ore-types))

(def max-structure-level 3)

(def job-provider-config
  {:lumberyard {:job-type :job/harvest-wood :max-jobs 3}
   :orchard {:job-type :job/harvest-fruit :max-jobs 2}
   :granary {:job-type :job/harvest-grain :max-jobs 2}
   :farm {:job-type :job/farm :max-jobs 2}
   :quarry {:job-type :job/mine :max-jobs 3}
   :workshop {:job-type :job/builder :max-jobs 2}
   :improvement-hall {:job-type :job/improve :max-jobs 1}
   :smelter {:job-type :job/smelt :max-jobs 1}})

(def improvable-structures
  #{:lumberyard :orchard :granary :farm :quarry :workshop :smelter :warehouse})

(def build-structure-options
  [:stockpile :lumberyard :orchard :granary :farm :quarry :warehouse :smelter :improvement-hall :workshop])

(def unique-structures
  #{:workshop :smelter :improvement-hall})

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
         job))))

(defn create-hunt-job
  [target-agent-id target-pos]
  (when (and target-agent-id target-pos)
    (assoc (create-job :job/hunt target-pos)
           :target-agent-id target-agent-id)))


(defn- add-job-to-world! [world job]
  (let [job-id (:id job)]
    (-> world
        (update :jobs conj job)
        (assoc-in [:jobs-by-id job-id] job))))

(defn- remove-job-from-world! [world job-id]
  (-> world
      (update :jobs (fn [js] (vec (remove #(= (:id %) job-id) js))))
      (update :jobs-by-id dissoc job-id)))

(defn- update-job-in-world! [world job]
  (let [job-id (:id job)]
    (-> world
        (update :jobs (fn [js] (mapv #(if (= (:id %) job-id) job %) js)))
        (assoc-in [:jobs-by-id job-id] job))))

(defn enqueue-job! [world job]
  (if job
    (add-job-to-world! world job)
    world))

(defn get-job-by-id [world job-id]
  (get-in world [:jobs-by-id job-id]))

(defn assign-job! [world job agent-id]
  (let [job' (assoc job :worker-id agent-id :state :claimed)
        job-id (:id job)]
    (-> world
        (update-in [:agents agent-id]
                   (fn [agent]
                     (-> agent
                         (assoc :current-job job-id)
                         (assoc :idle? false)
                         (assoc-in [:status :idle?] false))))
        (update-job-in-world! job'))))

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
      (let [job' (assoc job :state :claimed)]
        (assign-job! world job' agent-id))
      world)))

(defn auto-assign-jobs! [world]
  (reduce (fn [w agent]
            (let [alive? (get-in agent [:status :alive?] true)]
              (if (and (player-agent? agent) alive? (nil? (:current-job agent)))
                (let [w' (claim-next-job! w (:id agent))]
                  (if (= w w')
                    (mark-agent-idle w (:id agent))
                    w'))
                w)))
          world
          (:agents world)))

(defn add-item! [world pos resource qty]
   (let [[q r] pos
         tile-key (tile-key q r)]
     (if (zero? qty)
       world
       (update-in world [:items tile-key resource] (fnil + 0) qty))))

(defn- stockpile-accepts? [sp resource]
  (let [sp-resource (:resource sp)
        wood? #(contains? #{:wood :log} %)
        food? #(contains? food-resources %)]
    (or (= sp-resource resource)
        (and (wood? sp-resource) (wood? resource))
        (and (food? sp-resource) (food? resource)))))

(defn- stockpile-for-structure [structure]
  (case structure
    :lumberyard :log
    :orchard :fruit
    :granary :grain
    :farm :grain
    :quarry :rock
    nil))

(defn- structure-for-resource [resource]
  (case resource
    :log :lumberyard
    :fruit :orchard
    :berry :orchard
    :grain :farm
    :rock :quarry
    nil))

(defn consume-items! [world pos resource qty]
   (let [[q r] pos
         tile-key (tile-key q r)
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
   (let [[q r] pos
         k (tile-key q r)]
     (if (get-in world [:stockpiles k])
       world
       (assoc-in world [:stockpiles k]
                 {:resource resource :max-qty max-qty :current-qty 0}))))

(defn add-to-stockpile! [world pos resource qty]
   (let [[q r] pos
         k (tile-key q r)
         sp (get-in world [:stockpiles k])]
     (if (and sp (stockpile-accepts? sp resource))
       (let [space (- (:max-qty sp) (:current-qty sp))
             to-add (min space qty)]
         (if (pos? to-add)
           (update-in world [:stockpiles k :current-qty] + to-add)
           world))
       world)))

(defn take-from-stockpile! [world pos resource qty]
   (let [[q r] pos
         k (tile-key q r)
         sp (get-in world [:stockpiles k])]
     (if (and sp (stockpile-accepts? sp resource))
       (let [available (:current-qty sp)
             to-take (min available qty)]
         (if (pos? to-take)
           [(update-in world [:stockpiles k :current-qty] - to-take) to-take]
           [world 0]))
       [world 0])))

(defn stockpile-has-space? [world pos]
   (let [[q r] pos
         k (tile-key q r)
         sp (get-in world [:stockpiles k])]
     (and sp (< (:current-qty sp) (:max-qty sp)))))

(defn stockpile-space-remaining [world pos]
   (let [[q r] pos
         k (tile-key q r)
         sp (get-in world [:stockpiles k])]
     (if sp
       (- (:max-qty sp) (:current-qty sp))
       0)))

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


 (defn find-nearest-stockpile-with-qty [world pos resource]
  (let [entries (filter (fn [[_ sp]] (stockpile-accepts? sp resource)) (:stockpiles world))
        entries (filter (fn [[_ sp]] (pos? (:current-qty sp))) entries)]
    (when (seq entries)
      (let [pairs (map (fn [[k v]] [(parse-key-pos k) v]) entries)]
        (first (first (apply min-key (fn [[p _]] (hex/distance pos p)) pairs)))))))

(defn find-nearest-stockpile-with-space [world pos resource]
  (let [entries (filter (fn [[_ sp]] (stockpile-accepts? sp resource)) (:stockpiles world))
        entries (filter (fn [[_ sp]] (< (:current-qty sp) (:max-qty sp))) entries)]
    (when (seq entries)
      (let [pairs (map (fn [[k v]] [(parse-key-pos k) v]) entries)]
        (first (apply min-key (fn [[p _]] (hex/distance pos p)) pairs))))))

(defn- find-campfire-pos [world]
  (or (:campfire world)
      (first
       (keep (fn [[k tile]]
               (when (= (:structure tile) :campfire)
                 (parse-key-pos k)))
             (:tiles world)))))

(defn- empty-build-site? [world pos]
   (let [[q r] pos
         k (tile-key q r)
         tile (get-in world [:tiles k])]
     (and (hex/in-bounds? (:map world) pos)
          (nil? (:structure tile))
          (nil? (:resource tile)))))

(defn- find-house-site [world campfire-pos]
  (when campfire-pos
    (->> (positions-within-radius campfire-pos 2)
         (filter #(empty-build-site? world %))
         (sort-by (fn [pos] (hex/distance campfire-pos pos)))
         first)))

(defn- find-structure-site [world campfire-pos]
  (when campfire-pos
    (->> (positions-within-radius campfire-pos 3)
         (filter #(empty-build-site? world %))
         (sort-by (fn [pos] (hex/distance campfire-pos pos)))
         first)))

(defn- tiles-with-resource [world resource]
  (->> (:tiles world)
       (keep (fn [[k tile]]
               (when (= (:resource tile) resource)
                 (parse-key-pos k))))))

(defn- items-with-resource [world resource]
  (->> (:items world)
       (keep (fn [[k items]]
               (when (pos? (get items resource 0))
                 (parse-key-pos k))))))

(defn- find-nearest-resource [world pos resource]
  (->> (tiles-with-resource world resource)
       (sort-by (fn [p] (hex/distance pos p)))
       first))


(defn- find-nearest-item [world pos resource]
  (->> (items-with-resource world resource)
       (sort-by (fn [p] (hex/distance pos p)))
       first))

(defn- find-nearest-food-item [world pos]
  (->> food-item-order
       (keep (fn [resource] (find-nearest-item world pos resource)))
       (sort-by (fn [p] (hex/distance pos p)))
       first))

(defn- find-nearest-food-stockpile [world pos]
  (->> food-item-order
       (keep (fn [resource]
               (find-nearest-stockpile-with-qty world pos resource)))
       (sort-by (fn [p] (hex/distance pos p)))
       first))

(defn- find-nearest-food-target [world pos]
  (let [item-pos (find-nearest-food-item world pos)
        stockpile-pos (find-nearest-food-stockpile world pos)]
    (->> [item-pos stockpile-pos]
         (remove nil?)
         (sort-by (fn [p] (hex/distance pos p)))
         first)))

(defn- find-nearest-agent-with-role [world pos role]
  (->> (:agents world)
       (filter alive-agent?)
       (filter #(= (:role %) role))
       (sort-by (fn [agent] (hex/distance pos (:pos agent))))
       first))


(defn- stockpiles-with-space-for [world resource]
  (->> (:stockpiles world)
       (keep (fn [[k sp]]
               (when (and (stockpile-accepts? sp resource)
                          (< (:current-qty sp) (:max-qty sp)))
                 [(parse-key-pos k) sp])))))

(defn- harvest-job-type [resource]
  (case resource
    :log :job/harvest-wood
    :fruit :job/harvest-fruit
    :grain :job/harvest-grain
    :rock :job/harvest-stone
    :job/harvest-wood))

(defn- harvest-structure-type [resource]
  (case resource
    :log :lumberyard
    :fruit :orchard
    :grain :granary
    :rock :quarry
    nil))

(defn- harvest-structures [world]
  (->> (:tiles world)
       (keep (fn [[k tile]]
               (let [structure (:structure tile)
                     resource (stockpile-for-structure structure)]
                 (when resource
                   {:pos (parse-key-pos k)
                    :structure structure
                    :resource resource}))))))

(defn- total-item-qty [items resource]
  (reduce (fn [acc [_ item-map]]
            (+ acc (get item-map resource 0)))
          0
          items))

(defn- total-stockpile-space [world resources]
  (reduce (fn [acc [_ sp]]
            (if (some #(stockpile-accepts? sp %) resources)
              (+ acc (max 0 (- (:max-qty sp) (:current-qty sp))))
              acc))
          0
          (:stockpiles world)))

(defn- wood-demand [world]
  (let [resources #{:wood :log}
        space (total-stockpile-space world resources)
        items (:items world)
        available (+ (total-item-qty items :log)
                     (total-item-qty items :wood))]
    (max 0 (- space available))))

(defn- fruit-demand [world]
  (let [space (total-stockpile-space world #{:fruit})
        available (total-item-qty (:items world) :fruit)]
    (max 0 (min space available))))

(defn- consume-resource-global! [world resource qty]
  (loop [entries (seq (:items world))
         w world
         remaining qty]
    (if (or (zero? remaining) (nil? entries))
      [w (- qty remaining)]
      (let [[tile-key items] (first entries)
            available (get items resource 0)
            take (min available remaining)]
        (if (pos? take)
          (let [new-qty (- available take)
                w1 (if (zero? new-qty)
                     (update-in w [:items tile-key] dissoc resource)
                     (assoc-in w [:items tile-key resource] new-qty))
                w2 (if (empty? (get-in w1 [:items tile-key]))
                     (update w1 :items dissoc tile-key)
                     w1)]
            (recur (next entries) w2 (- remaining take)))
          (recur (next entries) w remaining))))))

(defn- house-resources-available? [world required]
  (let [items (:items world)
        log-qty (total-item-qty items :log)
        wood-qty (total-item-qty items :wood)]
    (>= (+ log-qty wood-qty) required)))

(defn- has-house-near? [world pos radius]
   (some (fn [p]
           (= :house (get-in world [:tiles (apply tile-key p) :structure])))
         (positions-within-radius pos radius)))

(defn complete-build-wall! [world job]
   (let [[q r] (:target job)
          k (tile-key q r)
          target (:target job)
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
               world')
              world)))
       world)))

(defn complete-build-house! [world job]
   (let [[q r] (:target job)
         k (tile-key q r)
         target (:target job)
         required 3
         log-qty (total-item-qty (:items world) :log)
         wood-qty (total-item-qty (:items world) :wood)]
    (if (>= (+ log-qty wood-qty) required)
      (let [log-use (min log-qty required)
            wood-use (- required log-use)
            [w1 _] (consume-resource-global! world :log log-use)
            [w2 _] (consume-resource-global! w1 :wood wood-use)
             world' (-> w2
                        (assoc-in [:tiles k :terrain] :ground)
                        (assoc-in [:tiles k :structure] :house)
                        (assoc-in [:tiles k :resource] nil)
                        (assoc-in [:tiles k :level] 1))]
        world')
      world)))

(defn- harvest-resource! [world resource target-pos qty]
  (let [pos (or target-pos (:campfire world))]
     (if pos
       (let [[q r] pos
             k (tile-key q r)
             sp (get-in world [:stockpiles k])]
         (if sp
           (add-to-stockpile! world pos resource qty)
           (add-item! world pos resource qty)))
       world)))

(defn- farm-yield
  [tile]
  (let [fertility (biomes/biome-fertility (:biome tile))]
    (max 1 (long (Math/round (* fertility 3))))))

(defn complete-farm! [world job]
  (let [[q r] (:target job)
        k (tile-key q r)
        tile (get-in world [:tiles k])]
    (if (and tile (= (:structure tile) :farm))
      (harvest-resource! world :grain (:target job) (farm-yield tile))
      world)))

(defn complete-harvest-job! [world job agent-id]
   (let [resource (:resource job)
         target (:target job)
          yield (case resource
                  :fruit 1
                  :grain 2
                  1)
         target-resource (case resource
                           :log :tree
                           :fruit :tree
                           :grain :grain
                           :rock :rock
                           :tree)
         [q r] target
         tile-key (tile-key q r)
         has-resource? (= target-resource (get-in world [:tiles tile-key :resource]))
         world' (if has-resource?
                  (case resource
                    :log (let [w1 (assoc-in world [:tiles tile-key :resource] nil)]
                           (harvest-resource! w1 :log (:building-pos job) yield))
                    :fruit (harvest-resource! world :fruit (:building-pos job) yield)
                    :grain (harvest-resource! world :grain (:building-pos job) yield)
                    :rock (harvest-resource! world :rock (:building-pos job) yield)
                    world)
                  world)]
     world'))

(defn complete-mine! [world job]
   (let [[q r] (:target job)
         tile-key (tile-key q r)
         target (:target job)
         resource (or (:resource job) (get-in world [:tiles tile-key :resource]))
         provider-pos (:provider-pos job)
         has-resource? (contains? mineral-types (get-in world [:tiles tile-key :resource]))
         world' (if has-resource?
                  (assoc-in world [:tiles tile-key :resource] nil)
                  world)
         world'' (if (and has-resource? resource provider-pos)
                   (add-item! world' provider-pos resource 1)
                   world')]
     world''))

(defn- structure-level [tile]
  (long (or (:level tile) 1)))

(defn complete-improve! [world job]
   (let [[q r] (:target job)
         k (tile-key q r)
         target (:target job)
         tile (get-in world [:tiles k])
         structure (:structure tile)
         level (structure-level tile)]
     (if (and (contains? improvable-structures structure)
              (< level max-structure-level))
       (let [new-level (inc level)
             world' (assoc-in world [:tiles k :level] new-level)]
         world')
       world)))

(defn complete-smelt! [world job]
  (let [resource (:resource job)
        output (:output job)
        target (:target job)
        [w' consumed] (consume-resource-global! world resource 1)]
    (if (and (pos? consumed) output)
      (let [world' (add-item! w' target output 1)]
        world')
      w')))

(defn complete-build-structure! [world job]
   (let [[q r] (:target job)
         k (tile-key q r)
         target (:target job)
        structure (:structure job)
        stockpile-config (:stockpile job)]
    (case structure
      :stockpile
      (let [{:keys [resource max-qty]} stockpile-config
            resource (or resource :log)
            max-qty (or max-qty 120)
            world' (create-stockpile! world target resource max-qty)]
        world')

       (:lumberyard :orchard :granary :farm :quarry :warehouse :smelter :improvement-hall :workshop)
       (let [stockpile-resource (stockpile-for-structure structure)
             resource (or stockpile-resource (:resource stockpile-config))
             max-qty (or (:max-qty stockpile-config) 120)
             world' (assoc-in world [:tiles k]
                              {:terrain :ground :structure structure :resource nil :level 1})
             world' (if resource
                      (create-stockpile! world' target resource max-qty)
                      world')]
        world')

       :campfire
       (let [world' (-> world
                        (assoc :campfire target)
                        (assoc-in [:tiles k] {:terrain :ground :structure :campfire :resource nil :level 1}))]
        world')

       :statue/dog
       (let [world' (assoc-in world [:tiles k] {:terrain :ground :structure :statue/dog :resource nil :level 1})]

        world')

       :wall
       (let [world' (assoc-in world [:tiles k] {:terrain :ground :structure :wall :resource nil :level 1})]

        world')

      world)))

(defn complete-chop-tree! [world job]
   (let [[q r] (:target job)
          k (tile-key q r)
          target (:target job)
          log-targets (->> (concat [target] (hex/neighbors target))
                           (filter #(hex/in-bounds? (:map world) %))
                           distinct
                           (take 3))]
     (if (= (get-in world [:tiles k :resource]) :tree)
       (let [world' (-> world
                        (assoc-in [:tiles k :resource] nil)
                        (assoc-in [:tiles k :last-log-drop] (:tick world)))
             world'' (reduce (fn [w pos] (add-item! w pos :log 1)) world' log-targets)]

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
          [q r] to-pos
          tile-key (tile-key q r)
          stockpile (get-in world [:stockpiles tile-key])
         add-fn (fn [w pos res qty]
                  (if (and stockpile (= res (:resource stockpile)))
                    (add-to-stockpile! w pos res qty)
                    (add-item! w pos res qty)))]


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
        [w' resource consumed]
        (loop [w1 world
               resources food-item-order]
          (if-let [resource (first resources)]
            (let [[w2 taken] (consume-items! w1 target resource 1)]
              (if (pos? taken)
                [w2 resource taken]
                (recur w2 (rest resources))))
            [w1 nil 0]))
        [w'' stocked-resource stocked]
        (if (pos? consumed)
          [w' nil 0]
          (loop [w1 w'
                 resources food-item-order]
            (if-let [resource (first resources)]
              (let [[w2 taken] (take-from-stockpile! w1 target resource 1)]
                (if (pos? taken)
                  [w2 resource taken]
                  (recur w2 (rest resources))))
              [w1 nil 0])))
        total (max consumed stocked)]
    (if (pos? total)
      (assoc-in w'' [:agents agent-id :needs :food] 1.0)
      w'')))

(defn complete-warm-up! [world job agent-id]
  (let [target (:target job)
        world' (assoc-in world [:agents agent-id :needs :warmth] 1.0)]
    world'))

(defn complete-sleep! [world agent-id]
   (let [world' (-> world
                   (assoc-in [:agents agent-id :status :asleep?] false)
                   (assoc-in [:agents agent-id :needs :sleep] 1.0))]

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
      (let [job (get-job-by-id world job-id)
            world (update-in world [:agents agent-id] dissoc :current-job)
            world (remove-job-from-world! world job-id)
            world (case (:type job)
                    :job/build-wall (complete-build-wall! world job)
                    :job/build-house (complete-build-house! world job)
                    :job/build-structure (complete-build-structure! world job)
                    :job/builder (complete-build-structure! world job)
                    :job/improve (complete-improve! world job)
                    :job/mine (complete-mine! world job)
                     :job/smelt (complete-smelt! world job)
                     :job/chop-tree (complete-chop-tree! world job)
                     :job/farm (complete-farm! world job)
                     :job/harvest-wood (complete-harvest-job! world job agent-id)
                    :job/harvest-fruit (complete-harvest-job! world job agent-id)
                    :job/harvest-grain (complete-harvest-job! world job agent-id)
                    :job/harvest-stone (complete-harvest-job! world job agent-id)
                    :job/haul (complete-haul! world job agent-id)
                    :job/eat (complete-eat! world job agent-id)
                    :job/warm-up (complete-warm-up! world job agent-id)
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
    (let [job (get-job-by-id world job-id)]
       (if job
         (let [new-state (if (= (:state job) :claimed) :in-progress (:state job))
               new-progress (min (+ (:progress job) delta) (:required job))
               job' (assoc job :state new-state :progress new-progress)
               world' (update-job-in-world! world job')
               world' (if (= (:type job) :job/sleep)
                        (assoc-in world' [:agents agent-id :status :asleep?] true)
                        world')]

           (if (>= new-progress (:required job))
             (complete-job! world' agent-id)
             world'))
         world))
    world))

(defn job-target-pos
  [world job]
  (if (= (:type job) :job/hunt)
    (when-let [target-id (:target-agent-id job)]
      (:pos (get-in world [:agents target-id])))
    (:target job)))

(defn get-agent-job [world agent-id]
   (when-let [job-id (get-in world [:agents agent-id :current-job])]
     (get-job-by-id world job-id)))

(defn cleanup-hunt-jobs!
  [world]
  (reduce
   (fn [w job]
     (if (= (:type job) :job/hunt)
       (let [target-id (:target-agent-id job)
             target (when target-id (get-in w [:agents target-id]))
             alive? (alive-agent? target)]
         (if alive?
           w
           (let [w' (remove-job-from-world! w (:id job))
                 worker-id (:worker-id job)]
             (if (and worker-id (= (get-in w' [:agents worker-id :current-job]) (:id job)))
               (mark-agent-idle w' worker-id)
               w'))))
       w))
   world
   (:jobs world)))

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
        threshold-for (if (map? threshold)
                        (fn [res] (get threshold res (get threshold :default 5)))
                        (fn [_] threshold))
        existing-keys (->> (:jobs world)
                           (filter #(= (:type %) :job/haul))
                           (keep (fn [job]
                                   (when (and (:from-pos job) (:to-pos job) (:resource job))
                                     [(:from-pos job) (:to-pos job) (:resource job)])))
                           set)
        jobs-to-add
        (->> entries
             (mapcat (fn [[tile-key items]]
                       (let [pos (parse-key-pos tile-key)]
                         (for [[res qty] items
                               :let [min-qty (threshold-for res)]
                               :when (>= qty min-qty)
                               :let [nearest (find-nearest-stockpile-with-space world pos res)]
                               :when nearest
                               :let [[sp-pos _] nearest
                                     job-key [pos sp-pos res]]
                               :when (not (contains? existing-keys job-key))]
                           (assoc (create-job :job/haul pos)
                                  :from-pos pos :to-pos sp-pos :resource res :qty min-qty
                                  :state :pickup :stage :pickup))))))]
    (when (seq jobs-to-add)
      )
    (reduce (fn [w job] (update w :jobs conj job))
            world
            jobs-to-add)))

(defn generate-need-jobs! [world]
  (reduce (fn [w agent]
            (if (player-agent? agent)
              (let [needs (:needs agent)
                    thresholds (or (:need-thresholds agent) {})
                    daylight (double (or (:daylight w) 0.7))
                    night? (< daylight 0.35)
                    food (get needs :food 1.0)
                    warmth (get needs :warmth 1.0)
                    sleep (get needs :sleep 1.0)
                    food-hungry (get thresholds :food-hungry 0.3)
                    warmth-cold (get thresholds :warmth-cold 0.3)
                    sleep-tired (get thresholds :sleep-tired 0.3)
                    sleep-threshold (if night? (max sleep-tired 0.6) sleep-tired)
                    pos (:pos agent)
                    agent-id (:id agent)
                    campfire-pos (find-campfire-pos w)
                    food-target (find-nearest-food-target w pos)
                    deer-target (find-nearest-agent-with-role w pos :deer)
                    has-eat-job? (and food-target
                                      (some #(and (= (:type %) :job/eat)
                                                 (= (:target %) food-target))
                                            (:jobs w)))
                    has-hunt-job? (some #(and (= (:type %) :job/hunt)
                                              (= (:target-agent-id %) (:id deer-target)))
                                        (:jobs w))
                    has-warm-job? (some #(and (= (:type %) :job/warm-up)
                                              (= (:target %) campfire-pos))
                                        (:jobs w))
                    has-sleep-job? (some #(and (= (:type %) :job/sleep)
                                               (= (:target %) pos))
                                         (:jobs w))
                    already-has-job? (some #(= (:worker-id %) agent-id) (:jobs w))]
                (cond-> w
                  (and (< food food-hungry) food-target (not has-eat-job?) (not already-has-job?))
                  (add-job-to-world! (create-job :job/eat food-target))
                  (and (< food food-hungry) (nil? food-target) deer-target (not has-hunt-job?) (not already-has-job?))
                  (add-job-to-world! (create-hunt-job (:id deer-target) (:pos deer-target)))
                  (and campfire-pos (< warmth warmth-cold) (not has-warm-job?) (not already-has-job?))
                  (add-job-to-world! (create-job :job/warm-up campfire-pos))
                  (and (< sleep sleep-threshold) (not has-sleep-job?) (not already-has-job?))
                  (add-job-to-world! (create-job :job/sleep pos))))
              w))
          world
          (:agents world)))

(defn generate-house-jobs! [world]
  (let [campfire-pos (find-campfire-pos world)
        target (find-house-site world campfire-pos)
        required 3
        needs-house?
        (some (fn [agent]
                (let [needs (:needs agent)
                      thresholds (or (:need-thresholds agent) {})
                      warmth (get needs :warmth 1.0)
                      sleep (get needs :sleep 1.0)
                      warmth-cold (get thresholds :warmth-cold 0.3)
                      sleep-tired (get thresholds :sleep-tired 0.3)]
                  (or (< warmth warmth-cold)
                      (< sleep sleep-tired))))
              (:agents world))
        has-job? (some #(= (:type %) :job/build-house) (:jobs world))
        has-house? (and campfire-pos (has-house-near? world campfire-pos 2))]
    (if (and campfire-pos target needs-house? (not has-job?) (not has-house?)
             (house-resources-available? world required))
      (add-job-to-world! world (create-job :job/build-house target))
      world)))

(defn generate-harvest-building-jobs! [world]
  (let [campfire-pos (find-campfire-pos world)
        target (find-structure-site world campfire-pos)
        existing-structures (->> (:tiles world)
                                 (keep (fn [[_ tile]] (:structure tile)))
                                 set)
        existing-jobs (->> (:jobs world)
                           (filter #(= (:type %) :job/build-structure))
                           (map :structure)
                           set)
        resources [:log :fruit :grain :rock]
        build-requests
        (for [resource resources
              :let [structure (harvest-structure-type resource)
                    resource-set (if (= resource :log) #{:log :wood} #{resource})
                    space (total-stockpile-space world resource-set)]
              :when (and structure (pos? space))
              :when (not (contains? existing-structures structure))
              :when (not (contains? existing-jobs structure))]
          structure)]
    (if (and target (seq build-requests))
      (update world :jobs conj
              (assoc (create-job :job/build-structure target)
                     :structure (first build-requests)
                     :stockpile {:resource (stockpile-for-structure (first build-requests))
                                 :max-qty 120}))
      world)))

(defn generate-harvest-jobs! [world]
  (let [existing-keys (->> (:jobs world)
                           (filter (fn [j]
                                     (contains? #{:job/harvest-wood :job/harvest-fruit
                                                  :job/harvest-grain :job/harvest-stone}
                                                (:type j))))
                           (keep (fn [job]
                                   (when (and (:building-pos job) (:resource job))
                                     [(:building-pos job) (:resource job)])))
                           set)
         jobs-to-add
         (for [{:keys [pos resource]} (harvest-structures world)
               :let [[q r] pos
                     stockpile-key (tile-key q r)
                     stockpile (get-in world [:stockpiles stockpile-key])
                    space (when stockpile (- (:max-qty stockpile) (:current-qty stockpile)))
                    target (find-nearest-resource world pos (case resource
                                                               :log :tree
                                                               :fruit :tree
                                                               :grain :grain
                                                               :rock :rock))
                    job-key [pos resource]]
              :when (and target (pos? (or space 0)))
              :when (not (contains? existing-keys job-key))]
          (assoc (create-job (harvest-job-type resource) target)
                 :resource resource
                 :building-pos pos))]
    (reduce (fn [w job] (update w :jobs conj job))
            world
            jobs-to-add)))

(defn generate-idle-structure-jobs! [world]
  (let [idle-agents (filter #(nil? (:current-job %)) (:agents world))
        pending? (some #(= (:state %) :pending) (:jobs world))
        has-job? (some #(= (:type %) :job/build-structure) (:jobs world))
        campfire-pos (find-campfire-pos world)
        target (find-structure-site world campfire-pos)
        r (java.util.Random. (long (+ (:seed world) (* 17 (:tick world)))))
        structure-options (cond-> [:stockpile :statue/dog :wall]
                            (nil? campfire-pos) (conj :campfire))
        roll (.nextDouble r)
        structure (cond
                    (< roll 0.45) :stockpile
                    (< roll 0.70) :statue/dog
                    (< roll 0.90) :wall
                    :else (first structure-options))]
    (if (and (seq idle-agents) (not pending?) (not has-job?) target)
      (update world :jobs conj
              (assoc (create-job :job/build-structure target)
                     :structure structure
                     :stockpile {:resource :log :max-qty 120}))
      world)))

(defn generate-deliver-food-jobs! [world]
  (let [existing-to-pos (->> (:jobs world)
                             (filter #(= (:type %) :job/deliver-food))
                             (map :to-pos)
                             set)
        stockpiles (stockpiles-with-space-for world :fruit)
        jobs-to-add (for [[sp-pos _] stockpiles
                          :when (not (contains? existing-to-pos sp-pos))
                          :let [nearest (find-nearest-item world sp-pos :fruit)]
                          :when nearest]
                      (assoc (create-job :job/deliver-food sp-pos)
                             :from-pos nearest :to-pos sp-pos :resource :fruit :qty 1))]
    (reduce (fn [w job] (update w :jobs conj job))
            world
            jobs-to-add)))

(defn generate-chop-jobs! [world]
   (let [demand (wood-demand world)
         log-yield 3
         desired (long (Math/ceil (/ (double demand) log-yield)))
         existing-targets (->> (:jobs world)
                               (filter #(= (:type %) :job/chop-tree))
                               (map :target)
                               set)
         tree-tiles (keep (fn [[k tile]]
                            (when (= (:resource tile) :tree)
                              [(parse-key-pos k) tile]))
                          (:tiles world))
         jobs-to-add (->> tree-tiles
                          (map first)
                          (remove existing-targets)
                          (take desired)
                          (map (fn [pos] (create-job :job/chop-tree pos))))]
     (reduce (fn [w job] (update w :jobs conj job))
             world
             jobs-to-add)))
