(ns fantasia.sim.spatial
  (:require [fantasia.sim.hex :as hex]))

(defn in-bounds?
  "Check whether a coordinate lies inside world bounds (using hex map)."
  [world pos]
  (hex/in-bounds? (:map world) pos))

(defn manhattan
  "Compute Manhattan distance between two coordinates (legacy helper)."
  [[ax ay] [bx by]]
  (+ (Math/abs (long (- ax bx)))
     (Math/abs (long (- ay by)))))

(defn neighbors
  "Return orthogonal neighbor coordinates for a given position (legacy helper)."
  [[x y]]
  [[(inc x) y]
    [(dec x) y]
    [x (inc y)]
    [x (dec y)]])

(defn at-trees?
  "True when the world has a tree at the given position."
  [world pos]
  (let [tile-key (vector (first pos) (second pos))
        tile (get-in world [:tiles tile-key])]
    (= :tree (:resource tile))))

(defn passable?
  "True when a position can be walked through (no wall structures)."
  [world pos]
  (let [tile-key (vector (first pos) (second pos))
        tile (get-in world [:tiles tile-key])
        structure (:structure tile)]
    (not (= :wall structure))))

(defn near-shrine?
  "True when a position is within 3 steps of the world shrine (using hex distance)."
  [world pos]
  (when-let [s (:shrine world)]
    (<= (hex/distance pos s) 3)))

(defn agents-at-position
   "Return all agents at a specific position."
   [world pos]
   (let [pos-key (vector (first pos) (second pos))]
     (filter #(= pos-key (vector (first (:pos %)) (second (:pos %))))
             (:agents world))))

(defn occupied-by-same-faction?
   "Check if position is occupied by another agent of the same faction."
   [world pos faction]
   (let [others (agents-at-position world pos)]
     (some #(and (not= (:id %) (:id world)) ;; Exclude self check if needed
                     (= (:faction %) faction))
           others)))

(defn move-agent-with-collision-check
   "Move an agent one step, avoiding collisions with same-faction agents.
    If all neighbors are occupied by same-faction, agent stays in place."
   [world agent]
   (let [options (->> (hex/neighbors (:pos agent))
                      (filter #(in-bounds? world %))
                      (filter #(passable? world %))
                      vec)
         available-options (filter #(not (occupied-by-same-faction? world % (:faction agent)))
                           options)]
     (if (seq available-options)
       (assoc agent :pos (nth available-options (mod (+ (:tick world)
                                                           (:id agent)
                                                           (:seed world))
                                                        (count available-options))))
       (if (seq options)
         (assoc agent :pos (nth options (mod (+ (:tick world)
                                                (:id agent)
                                                (:seed world))
                                             (count options))))
         agent))))
