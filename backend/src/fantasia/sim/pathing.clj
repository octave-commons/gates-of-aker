(ns fantasia.sim.pathing
  (:require [fantasia.sim.hex :as hex]
            [fantasia.sim.spatial :as spatial]
            [fantasia.sim.constants :as const]))

(defn bfs-path
  "Find shortest path from start to goal using BFS.
   Returns sequence of positions from start to goal (inclusive).
   Returns nil if no path exists."
  [world start goal]
  (if (= start goal)
    [start]
    (loop [queue [[start]]
           visited #{start}
           steps 0]
      (let [max-steps const/pathfinding-max-steps]
        (if (>= steps max-steps)
          nil
          (if (empty? queue)
            nil
            (let [path (first queue)
                  current (last path)]
              (if (= current goal)
                path
                (let [neighbors (->> (hex/neighbors current)
                                     (filter #(spatial/in-bounds? world %))
                                     (filter #(spatial/passable? world %))
                                     (remove visited))
                      visited' (reduce conj visited neighbors)
                      new-paths (map #(conj path %) neighbors)
                      queue' (concat (rest queue) new-paths)]
                  (recur queue' visited' (inc steps)))))))))))

(defn- move-cost
  [world pos]
  (let [tile-key (vector (first pos) (second pos))
        structure (get-in world [:tiles tile-key :structure])]
    (if (= :road structure)
      const/road-move-cost
      1.0)))

(defn a-star-path
  "Find shortest path from start to goal using A* algorithm.
   Returns sequence of positions from start to goal (inclusive).
   Returns nil if no path exists."
  [world start goal]
  (if (= start goal)
    [start]
    (let [heuristic (fn [pos] (hex/distance pos goal))
          start-g 0
           start-f (+ start-g (heuristic start))
           open-set {start {:g start-g :f start-f :path [start]}}
           closed-set #{}
           max-steps const/pathfinding-max-steps]
      (loop [open-set open-set
             closed-set closed-set
             steps 0]
        (if (>= steps max-steps)
          nil
          (if (empty? open-set)
            nil
            (let [current (key (apply min-key (fn [[_ v]] (:f v)) open-set))
                  current-data (get open-set current)
                  current-path (:path current-data)]
              (if (= current goal)
                current-path
                (let [neighbors (->> (hex/neighbors current)
                                     (filter #(spatial/in-bounds? world %))
                                     (filter #(spatial/passable? world %))
                                     (remove closed-set))
                       process-neighbor (fn [os neighbor]
                                         (let [tentative-g (+ (:g current-data) (move-cost world neighbor))
                                               neighbor-f (+ tentative-g (heuristic neighbor))]
                                          (if-let [existing (get os neighbor)]
                                            (if (< tentative-g (:g existing))
                                              (assoc os neighbor
                                                     {:g tentative-g
                                                      :f neighbor-f
                                                      :path (conj current-path neighbor)})
                                              os)
                                            (assoc os neighbor
                                                   {:g tentative-g
                                                    :f neighbor-f
                                                    :path (conj current-path neighbor)}))))
                      new-open (reduce process-neighbor
                                    (dissoc open-set current)
                                    neighbors)]
                  (recur new-open (conj closed-set current) (inc steps)))))))))))

(defn next-step-toward
  "Get next position on path toward goal.
    Returns one step along path, or current pos if already there."
  [world start goal]
  (if (= start goal)
    start
    (let [path (a-star-path world start goal)]
      (if (and path (> (count path) 1))
        (second path)
        start))))

(defn reachable?
  "Check if goal is reachable from start."
  [world start goal]
  (boolean (a-star-path world start goal)))
