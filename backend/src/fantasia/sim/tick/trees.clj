(ns fantasia.sim.tick.trees
  "Tree lifecycle helpers: spawn, spread, and fruit-drop logic.
  Split into small, testable helpers to simplify reasoning and maintenance."
  (:require [fantasia.sim.hex :as hex]
            [fantasia.sim.jobs :as jobs]
            [clojure.string :as str]))

;; RNG helpers
(defn- rng [seed] (java.util.Random. (long seed)))
(defn- rand-int* [^java.util.Random r n] (.nextInt r (int n)))

;; Tile key helpers
(defn- parse-tile-key
  "Parse a tile key string 'q,r' into [q r] coordinate vector."
  [k]
  (let [parts (str/split k #",")]
    [(Integer/parseInt (first parts)) (Integer/parseInt (second parts))]))

(defn- tile-key-for
  "String key for a position vector [q r]."
  [[q r]]
  (str q "," r))

(defn- make-tree-tile
  "Construct a fresh tree tile map for the given tick and rng.
  `tick` is used to seed scheduled values so they are deterministic per world tick." 
  [tick rng]
  {:terrain :ground
   :resource :tree
   :last-fruit-drop 0
   :next-spread-tick (+ tick 20 (rand-int* rng 141))})

;; --- Spawn initial trees -----------------------------------------------------
(defn- try-place-tree
   "Place a tree at pos in world when a randomized roll succeeds.
   Returns updated world." 
   [world pos rng spawn-chance]
    (let [tile-key (tile-key-for pos)
          existing-tile (get-in world [:tiles tile-key])]
      (if (< (rand-int* rng 1000) (int (* spawn-chance 1000)))
        (assoc-in world [:tiles tile-key] (if existing-tile
                                         (merge existing-tile (make-tree-tile (:tick world) rng))
                                         (make-tree-tile (:tick world) rng)))
        world)))

(defn spawn-initial-trees!
  "Spawn initial trees randomly throughout the map. Approximately `tree-density`
  fraction of tiles get trees by default. Keeps original arities for callers."
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
             (fn [w'' r]
               (try-place-tree w'' [(+ (first origin) q) (+ (second origin) r)] rng spawn-chance))
             w'
             (range h)))
          world
          (range w)))
       world))))

;; --- Spread trees -----------------------------------------------------------
(defn- next-spread-tick
  "Compute a next-spread tick using rng between min and max intervals."
  [current min-interval max-interval rng]
  (+ current min-interval (rand-int* rng (- max-interval min-interval))))

(defn- valid-empty-neighbors
  "Return neighbor positions that are in-bounds and have no tile yet."
  [world pos]
  (filter (fn [n]
            (and (hex/in-bounds? (:map world) n)
                 (nil? (get-in world [:tiles (tile-key-for n)]))))
          (hex/neighbors pos)))

(defn- spread-from-tile
  "Attempt to spread a tree from tile-key in world. Returns updated world.
  Uses rng and probability to decide whether to spawn a neighboring tree." 
  [world tile-key r current-tick min-interval max-interval spread-probability]
  (let [tile (get-in world [:tiles tile-key])
        next-spread (or (:next-spread-tick tile) (next-spread-tick current-tick min-interval max-interval r))
        should-spread? (>= current-tick next-spread)
        actual-spread? (and should-spread? (< (rand-int* r 1000) (int (* spread-probability 1000))))]
    (cond
      actual-spread?
      (let [pos (parse-tile-key tile-key)
            neighbors (valid-empty-neighbors world pos)]
        (if (seq neighbors)
          (let [new-pos (rand-nth neighbors)
                new-key (tile-key-for new-pos)]
            (-> world
                (assoc-in [:tiles new-key] (merge (get-in world [:tiles new-key]) (make-tree-tile current-tick r)))
                (assoc-in [:tiles tile-key :next-spread-tick] (next-spread-tick current-tick min-interval max-interval r))))
          (assoc-in world [:tiles tile-key :next-spread-tick] (next-spread-tick current-tick min-interval max-interval r))))

      should-spread?
      (assoc-in world [:tiles tile-key :next-spread-tick] (next-spread-tick current-tick min-interval max-interval r))

      :else
      world)))

(defn spread-trees!
  "Spread trees to adjacent empty tiles. Each tree has a chance to spawn a new tree
  in an adjacent empty tile when its spread timer elapses." 
  [world]
  (let [current-tick (:tick world)
        r (rng (:seed world))
        min-interval 20
        max-interval 160
        spread-probability 0.30]
    (reduce-kv
     (fn [w tile-key tile]
       (if (= (:resource tile) :tree)
         (spread-from-tile w tile-key r current-tick min-interval max-interval spread-probability)
         w))
     world
     (:tiles world))))

;; --- Drop fruit -------------------------------------------------------------
(defn- should-drop-fruit?
  "Return true when the tree should drop fruit on this tick." 
  [tile current-tick rng]
  (let [last-drop (or (:last-fruit-drop tile) 0)
        turns-since (- current-tick last-drop)
        min-interval 5
        max-interval 20
        interval-range (+ 1 (- max-interval min-interval))
        drop-at (+ last-drop min-interval (mod (rand-int* rng 1000000) interval-range))]
    (and (>= turns-since min-interval)
         (>= turns-since (or (:next-fruit-drop tile) drop-at)))))

(defn- perform-drop
  "Add one fruit at a nearby tile and update tile timings." 
  [w tile-key tile current-tick rng]
  (let [pos (parse-tile-key tile-key)
        neighbors (->> (hex/neighbors pos)
                       (filter #(hex/in-bounds? (:map w) %))
                       vec)
        drop-pos (if (seq neighbors)
                   (nth neighbors (rand-int* rng (count neighbors)))
                   pos)]
    (-> w
        (jobs/add-item! drop-pos :fruit 1)
        (assoc-in [:tiles tile-key :last-fruit-drop] current-tick)
        (assoc-in [:tiles tile-key :next-fruit-drop] (+ current-tick 5 (mod (rand-int* rng 1000000) 16))))))

(defn drop-tree-fruits!
  "Process fruit dropping for all trees. Fruits accumulate at tree positions in :items." 
  [world]
  (let [current-tick (:tick world)
        r (rng (:seed world))]
    (reduce-kv
     (fn [w tile-key tile]
       (if (= (:resource tile) :tree)
         (if (should-drop-fruit? tile current-tick r)
           (perform-drop w tile-key tile current-tick r)
           w)
         w))
     world
     (:tiles world))))
