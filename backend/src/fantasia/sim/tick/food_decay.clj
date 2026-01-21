(ns fantasia.sim.tick.food-decay
  "Food decay system for items on the ground.
   Fruits and other food items slowly decay over time.")

(def ^:const food-decay-interval 50)
(def ^:const food-decay-chance 0.02)
(def ^:const food-types #{:fruit :berry :grain :raw-meat :cooked-meat :stew :food})

(defn- rng [seed] (java.util.Random. (long seed)))
(defn- rand-float [^java.util.Random r] (.nextFloat r))

(defn- decay-items-at-tick
  "Decay food items on the ground at a specific tick.
   Each food type has a small chance to decay per tick when interval elapses."
  [world current-tick]
  (let [r (rng (:seed world))
        should-decay? (zero? (mod current-tick food-decay-interval))]
    (if (not should-decay?)
      world
      (reduce-kv
       (fn [w tile-key items]
         (reduce-kv
          (fn [w' resource qty]
            (if (and (contains? food-types resource)
                     (> qty 0)
                     (< (rand-float r) food-decay-chance))
              (let [new-qty (max 0 (dec qty))]
                (if (zero? new-qty)
                  (if (> (count items) 1)
                    (update-in w' [:items tile-key] dissoc resource)
                    (update w' :items dissoc tile-key))
                  (assoc-in w' [:items tile-key resource] new-qty)))
              w'))
          w
          items))
       world
       (:items world)))))

(defn decay-food!
  "Process food decay for all items on the ground."
  [world]
  (decay-items-at-tick world (:tick world)))
