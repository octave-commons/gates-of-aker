(ns fantasia.sim.los
  "Line of sight and vision system for agent perception."
  (:require [fantasia.sim.hex :as hex]
            [fantasia.sim.constants :as const]
            [clojure.string :as str]))

(defn get-vision-radius
  "Return vision radius for an agent based on role."
  [agent]
  (case (:role agent)
    :wolf const/wolf-vision-radius
    :bear const/bear-vision-radius
    :deer const/deer-vision-radius
    const/player-vision-radius))

(defn positions-in-vision
  "Return all hex positions within vision radius of center."
  [center radius]
  (let [result (transient [])]
    (doseq [q (range (- (first center) radius) (+ (first center) radius 1))]
      (doseq [r (range (- (second center) radius) (+ (second center) radius 1))]
        (when (<= (hex/distance center [q r]) radius)
          (conj! result [q r]))))
    (persistent! result)))

(defn normalize-tile-key
   "Normalize tile key from vector [q r] or string representation to 'q,r' string format."
   [tile-key]
   (cond
     (string? tile-key)
     (-> tile-key
         (str/replace #"[^\d,-]" ""))
     (vector? tile-key)
     (str (first tile-key) "," (second tile-key))
     :else tile-key))

(defn string-to-vector-key
   "Convert string tile key 'q,r' back to vector [q r] for tile map lookups."
   [tile-key]
   (when (string? tile-key)
     (let [parts (str/split tile-key #",")]
       (when (= 2 (count parts))
         [(read-string (first parts)) (read-string (second parts))]))))

(defn agent-can-see?
   "True if viewer at viewer-pos can see target at target-pos with given vision radius."
   [viewer-pos target-pos vision-radius]
   (<= (hex/distance viewer-pos target-pos) vision-radius))

(defn filter-visible-agents
  "Return agents visible to a viewer at given position with vision radius."
  [world viewer-pos vision-radius]
  (->> (:agents world)
       (filter #(and (get-in % [:status :alive?] true)
                     (agent-can-see? viewer-pos (:pos %) vision-radius)))
       vec))

(defn visible-agent-ids
  "Return IDs of agents visible from viewer position."
  [world viewer-pos vision-radius]
  (->> (:agents world)
       (filter #(and (get-in % [:status :alive?] true)
                     (agent-can-see? viewer-pos (:pos %) vision-radius)))
       (map :id)
       set))

(defn visible-tiles
   "Return tile keys visible from viewer position within vision radius."
   [world viewer-pos vision-radius]
   (let [visible-pos (positions-in-vision viewer-pos vision-radius)]
     (->> visible-pos
          (map #(vector (first %) (second %)))
          (filter #(get-in world [:tiles %]))
          (map normalize-tile-key)
          set)))

(defn visible-items
  "Return item positions visible from viewer position."
  [world viewer-pos vision-radius]
  (->> (:items world)
         keys
         (filter #(agent-can-see? viewer-pos (if (string? %) (clojure.string/split % #",") %) vision-radius))
         set))

(defn visible-stockpiles
  "Return stockpile positions visible from viewer position."
  [world viewer-pos vision-radius]
  (->> (:stockpiles world)
         keys
         (filter #(agent-can-see? viewer-pos (if (string? %) (clojure.string/split % #",") %) vision-radius))
         set))

(defn compute-visibility
  "Compute full visibility map for a viewer position."
  [world viewer-pos vision-radius]
  {:visible-agent-ids (visible-agent-ids world viewer-pos vision-radius)
   :visible-tiles (visible-tiles world viewer-pos vision-radius)
   :visible-items (visible-items world viewer-pos vision-radius)
   :visible-stockpiles (visible-stockpiles world viewer-pos vision-radius)})

(defn all-player-visible-tiles
   "Get set of all tiles visible by any player agent."
   [world]
   (let [player-agents (filter #(or (= (:faction %) :player) (= (:faction %) "player")) (:agents world))]
     (->> player-agents
           (mapcat (fn [agent]
                     (let [radius (get-vision-radius agent)]
                       (positions-in-vision (:pos agent) radius))))
           (map str)
           (map #(clojure.string/replace % "[" ""))
            (map #(clojure.string/replace % "]" ""))
            (map #(clojure.string/replace % "\\(" ""))
            (map #(clojure.string/replace % "\\)" ""))
            (map #(clojure.string/replace % " " ","))
           set)))

(defn normalize-tile-key
   "Normalize tile key from vector [q r] or string representation to 'q,r' string format."
   [tile-key]
   (cond
     (string? tile-key)
     (str/replace tile-key #"[\[\]\(\)]" "")
     (vector? tile-key)
     (str (first tile-key) "," (second tile-key))
     :else tile-key))

(defn string-to-vector-key
   "Convert string tile key to vector [q r] for tile map lookups."
   [tile-key]
   (when (string? tile-key)
     (let [cleaned (-> tile-key
                        (str/replace "[" "")
                        (str/replace "]" "")
                        (str/replace "(" "")
                        (str/replace ")" "")
                        (str/replace " ", ","))
           parts (str/split cleaned #",")]
       (when (and (= 2 (count parts))
                    (every? #(try (Integer/parseInt %) (catch Exception _ false)) parts))
         [(Integer/parseInt (first parts)) (Integer/parseInt (second parts))]))))

(defn update-tile-visibility!
   "Update tile visibility state and save snapshots for newly revealed tiles.
    Returns map with :tile-visibility and :revealed-tiles-snapshot."
    [world]
    (let [current-visible-tiles (all-player-visible-tiles world)
          old-visibility (:tile-visibility world {})
          old-snapshots (:revealed-tiles-snapshot world {})
          all-tiles (keys (:tiles world))]
     (let [new-visibility (reduce
                               (fn [vis-map tile-key]
                                 (let [old-state (get old-visibility tile-key)
                                       currently-visible? (contains? current-visible-tiles tile-key)]
                                   (cond
                                     (nil? old-state)
                                     (if currently-visible?
                                       (assoc vis-map tile-key :visible)
                                       vis-map)
                                     (= old-state :visible)
                                     (if currently-visible?
                                       vis-map
                                       (assoc vis-map tile-key :revealed))
                                     (= old-state :revealed)
                                     (if currently-visible?
                                       (assoc vis-map tile-key :visible)
                                       vis-map)
                                     (= old-state :hidden)
                                     (if currently-visible?
                                       (assoc vis-map tile-key :visible)
                                       vis-map))))
                               {}
                               all-tiles)
             tiles-to-snapshot (concat
                                 (filter #(not (contains? old-visibility %)) current-visible-tiles)
                                 (filter #(= (get old-visibility %) :visible) current-visible-tiles)
                                 (filter #(and (= (get new-visibility %) :revealed)
                                              (= (get old-visibility %) :visible)
                                              (not (contains? old-snapshots %))) all-tiles))
            new-snapshots (reduce
                               (fn [snapshots tile-key]
                                 (if (contains? old-snapshots tile-key)
                                   snapshots
                                   (when-let [tile (get-in world [:tiles (string-to-vector-key tile-key)])]
                                     (assoc snapshots tile-key tile))))
                               old-snapshots
                               tiles-to-snapshot)]
       {:tile-visibility new-visibility
        :revealed-tiles-snapshot new-snapshots})))
