(ns fantasia.sim.tick.trees
  (:require [fantasia.sim.hex :as hex]
            [fantasia.sim.jobs :as jobs]
            [clojure.string :as str]))

(defn- rng [seed] (java.util.Random. (long seed)))
(defn- rand-int* [^java.util.Random r n] (.nextInt r (int n)))

(defn- parse-tile-key
   "Parse a tile key string 'q,r' into [q r] coordinate vector."
   [k]
   (let [parts (str/split k #",")]
     [(Integer/parseInt (first parts)) (Integer/parseInt (second parts))]))

(defn spawn-initial-trees!
   "Spawn initial trees randomly throughout the map. Approximately 5% of tiles get trees by default."
  ([world]
   (spawn-initial-trees! world 0.05))
  ([world tree-density]
   (let [rng (rng (:seed world))
         hex-map (:map world)
         bounds (:bounds hex-map)
         spawn-chance tree-density]
    (if (= (:shape bounds) :rect)
      (let [w (long (:w bounds 1))
            h (long (:h bounds 1))
            origin (:origin bounds [0 0])]
        (reduce
          (fn [w' q]
            (reduce
              (fn [w'' r-idx]
                (if (< (rand-int* rng 1000) (int (* spawn-chance 1000)))
                  (let [pos [(+ (first origin) q) (+ (second origin) r-idx)]
                        tile-key (str (first pos) "," (second pos))]
                    (assoc-in w'' [:tiles tile-key]
                              {:terrain :ground :resource :tree :last-fruit-drop 0
                               :next-spread-tick (+ (:tick w'') 20 (rand-int* rng 141))}))
                  w''))
              w'
              (range h)))
          world
          (range w)))
      world)))

(defn spread-trees!
   "Spread trees to adjacent empty tiles. Each tree has a 25% chance to spawn a new tree
     in an adjacent empty tile every 20-160 ticks."
   [world]
   (let [current-tick (:tick world)
          r (rng (:seed world))
          min-interval 20
          max-interval 160
          spread-probability 0.30]
       (reduce-kv
        (fn [w tile-key tile]
          (if (= (:resource tile) :tree)
            (let [next-spread (or (:next-spread-tick tile) (+ current-tick min-interval (rand-int* r (- max-interval min-interval))))
                  should-spread? (>= current-tick next-spread)
                  actual-spread? (and should-spread? (< (rand-int* r 1000) (int (* spread-probability 1000))))]
              (cond
                actual-spread?
                (let [pos (parse-tile-key tile-key)
                      neighbors (hex/neighbors pos)
                      valid-neighbors (filter (fn [n] (and (hex/in-bounds? (:map w) n)
                                                          (nil? (get-in w [:tiles (str (first n) "," (second n))]))))
                                                neighbors)]
                  (if (seq valid-neighbors)
                    (let [new-pos (rand-nth valid-neighbors)
                          new-tile-key (str (first new-pos) "," (second new-pos))]
                      (-> w
                          (assoc-in [:tiles new-tile-key]
                                    {:terrain :ground :resource :tree :last-fruit-drop 0
                                     :next-spread-tick (+ current-tick min-interval (rand-int* r (- max-interval min-interval)))})
                          (assoc-in [:tiles tile-key :next-spread-tick]
                                    (+ current-tick min-interval (rand-int* r (- max-interval min-interval))))))
                    (assoc-in w [:tiles tile-key :next-spread-tick]
                              (+ current-tick min-interval (rand-int* r (- max-interval min-interval))))))
                should-spread?
                (assoc-in w [:tiles tile-key :next-spread-tick]
                          (+ current-tick min-interval (rand-int* r (- max-interval min-interval))))
                :else
                w))
            w))
          world
         (:tiles world))))

(defn drop-tree-fruits!
   "Process fruit dropping for all trees. Each tree drops fruit randomly every 5-20 turns.
    Fruits accumulate at the tree position in the :items map."
   [world]
   (let [current-tick (:tick world)
         rng (rng (:seed world))]
     (reduce-kv
      (fn [w tile-key tile]
        (if (= (:resource tile) :tree)
          (let [last-drop (or (:last-fruit-drop tile) 0)
                turns-since (- current-tick last-drop)
                min-interval 5
                max-interval 20
                interval-range (+ 1 (- max-interval min-interval))
                drop-at (+ last-drop min-interval (mod (rand-int* rng 1000000) interval-range))]
            (if (>= turns-since min-interval)
              (let [should-drop? (>= turns-since (or (:next-fruit-drop tile) drop-at))]
                (cond-> w
                  should-drop?
                  (as-> w'
                    (let [pos (parse-tile-key tile-key)]
                      (-> w'
                          (jobs/add-item! pos :fruit 1)
                          (assoc-in [:tiles tile-key :last-fruit-drop] current-tick)
                          (assoc-in [:tiles tile-key :next-fruit-drop] (+ current-tick min-interval (mod (rand-int* rng 1000000) interval-range))))))))
              w))
          w))
      world
      (:tiles world))))
