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
         (filter #(get-in world [:tiles (str (first %) "," (second %))]))
         (map #(str (first %) "," (second %)))
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
  (let [player-agents (filter #(= (:faction %) :player) (:agents world))]
    (println "[VISIBILITY] Found" (count player-agents) "player agents out of" (count (:agents world)) "total agents")
    (doseq [agent player-agents]
      (println "[VISIBILITY] Player agent:" (:name agent) "at" (:pos agent) "with role" (:role agent) "and faction" (:faction agent)))
    (->> player-agents
         (mapcat (fn [agent]
                   (let [radius (get-vision-radius agent)]
                     (positions-in-vision (:pos agent) radius))))
         (map #(str (first %) "," (second %)))
         set)))

(defn update-tile-visibility!
   "Update tile visibility state and save snapshots for newly revealed tiles.
   Returns map with :tile-visibility and :revealed-tiles-snapshot."
   [world]
   (let [current-visible-tiles (all-player-visible-tiles world)
         old-visibility (:tile-visibility world {})
         old-snapshots (:revealed-tiles-snapshot world {})
         all-tiles (keys (:tiles world))]
     (println "[VISIBILITY] Currently visible tiles:" (count current-visible-tiles) "out of" (count all-tiles) "total tiles")
     (let [new-visibility (reduce
                             (fn [vis-map tile-key]
                               (let [old-state (get old-visibility tile-key :hidden)
                                     currently-visible? (contains? current-visible-tiles tile-key)]
                                 (cond
                                   (= old-state :visible)
                                   (if currently-visible?
                                     vis-map
                                     (assoc vis-map tile-key :revealed))

                                   (= old-state :revealed)
                                   (if currently-visible?
                                     (assoc vis-map tile-key :visible)
                                     vis-map)

                                   :else
                                   (if currently-visible?
                                     (assoc vis-map tile-key :visible)
                                     (assoc vis-map tile-key :hidden)))))
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
                                  (when-let [tile (get-in world [:tiles tile-key])]
                                    (assoc snapshots tile-key tile))))
                              old-snapshots
                              tiles-to-snapshot)]
       (println "[VISIBILITY] new-visibility map has" (count new-visibility) "entries"
                (str "(" (count (filter #(= % :revealed) (vals new-visibility))) " revealed, "
                     (count (filter #(= % :visible) (vals new-visibility))) " visible, "
                     (count (filter #(= % :hidden) (vals new-visibility))) " hidden)"))
       {:tile-visibility new-visibility
        :revealed-tiles-snapshot new-snapshots})))
