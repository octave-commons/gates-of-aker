(ns fantasia.sim.jobs.eat)

(defn create-eat-job [pos]
   {:id (random-uuid)
    :type :job/eat
    :target pos
    :priority 100
    :progress 0.0
    :required 1.0
    :state :pending})

(defn complete-eat! [world job agent-id]
   (let [[q r] (:target job)
         tile-key (vector q r)
         pos (:target job)
         stockpiles (:stockpiles world)
         items (:items world)
         sp (get stockpiles tile-key)
         has-stockpile-food? (and sp
                                (= (:resource sp) :food)
                                (pos? (:current-qty sp)))
         has-food-item? (get-in items [tile-key :food])
         has-fruit-item? (get-in items [tile-key :fruit])]
    (if has-stockpile-food?
      (let [new-qty (dec (:current-qty sp))
            world1 (assoc-in world [:stockpiles tile-key :current-qty] new-qty)
            world2 (assoc-in world1 [:agents agent-id :needs :food] 1.0)
            world3 (if (zero? new-qty)
                     (update world2 :stockpiles dissoc tile-key)
                     world2)]
        (println "[JOB:COMPLETE]" {:type :job/eat :agent-id agent-id :target pos :source "stockpile"})
        [world3 true])
      (if has-food-item?
        (let [items-at-tile (get items tile-key)
              food-qty (get items-at-tile :food 0)
              new-items (cond
                         (> food-qty 1) (assoc items-at-tile :food (dec food-qty))
                         (= food-qty 1) (dissoc items-at-tile :food)
                         :else items-at-tile)
              world1 (assoc-in world [:items tile-key] new-items)
              world2 (assoc-in world1 [:agents agent-id :needs :food] 1.0)]
          (println "[JOB:COMPLETE]" {:type :job/eat :agent-id agent-id :target pos :source "food-item"})
          [world2 true])
        (if has-fruit-item?
          (let [items-at-tile (get items tile-key)
                fruit-qty (get items-at-tile :fruit 0)
                new-items (cond
                           (> fruit-qty 1) (assoc items-at-tile :fruit (dec fruit-qty))
                           (= fruit-qty 1) (dissoc items-at-tile :fruit)
                           :else items-at-tile)
                world1 (assoc-in world [:items tile-key] new-items)
                world2 (assoc-in world1 [:agents agent-id :needs :food] 1.0)]
            (println "[JOB:COMPLETE]" {:type :job/eat :agent-id agent-id :target pos :source "fruit-item"})
            [world2 true])
          (do
            (println "[JOB:COMPLETE]" {:type :job/eat :agent-id agent-id :target pos :source "none-found"})
            [world false]))))))

(defn progress-eat! [job delta]
  (let [new-progress (min (+ (:progress job) delta) (:required job))]
    (assoc job :progress new-progress)))

(defn job-complete? [job]
  (>= (:progress job 0.0) (:required job 1.0)))
