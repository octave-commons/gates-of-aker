(ns fantasia.sim.agent-visibility
  "Per-agent polygon-based visibility system.
   Each agent has a visibility shape defined by vertices and edges.
   Everything inside the shape is visible to that agent.")

(defn get-vision-radius
  "Return vision radius for an agent based on role."
  [agent]
  (case (:role agent)
    :wolf fantasia.sim.constants/wolf-vision-radius
    :bear fantasia.sim.constants/bear-vision-radius
    :deer fantasia.sim.constants/deer-vision-radius
    fantasia.sim.constants/player-vision-radius))

(defn to-radians
  "Convert degrees to radians."
  [degrees]
  (* degrees (/ Math/PI 180)))

(defn compute-visibility-polygon
  "Compute visibility polygon for an agent at position with given radius.
   Returns a map with :vertices (list of [q,r] coordinates) and :edges (list of vertex pairs)."
  [agent-pos radius]
  (let [center-q (first agent-pos)
        center-r (second agent-pos)
        num-points 24
        vertices (vec (for [i (range num-points)]
                          (let [angle (to-radians (* i (/ 360 num-points)))
                                q (int (+ center-q (* radius (Math/cos angle))))
                                r (int (+ center-r (* radius (Math/sin angle)))]
                            [q r])))
        num-verts (count vertices)
        edges (vec (for [i (range num-verts)]
                         (let [i2 (mod (inc i) num-verts)]
                           [(nth vertices i) (nth vertices i2)])))]
    {:vertices vertices
     :edges edges}))

(defn point-in-polygon?
  "Check if a point is inside a polygon using ray casting algorithm."
  [point vertices]
  (let [[px py] point
        n (count vertices)
        inside (volatile! false)]
    (doseq [i (range n)]
      (let [i2 (mod (inc i) n)
            [vx1 vy1] (nth vertices i)
            [vx2 vy2] (nth vertices i2)
            on-edge1 (or (<= py vy1) (> py vy2))
            on-edge2 (or (<= py vy2) (> py vy1))]
        (when (and on-edge1 on-edge2)
          (let [intersect-x (+ (/ (* (- vx2 vx1) (- py vy1)) (- vy2 vy1)) vx1)]
            (when (< px intersect-x)
              (vreset! inside (not @inside)))))))
    @inside))

(defn compute-agent-visibility
  "Compute which tiles are visible to a specific agent.
   Returns set of tile keys (e.g., '0,1', '2,3')."
  [world agent]
  (let [agent-pos (:pos agent)
        radius (get-vision-radius agent)]
    (when agent-pos
      (let [polygon (compute-visibility-polygon agent-pos radius)
            tiles (:tiles world)]
        (->> tiles
             keys
             (filter (fn [tile-key]
                       (when (string? tile-key)
                         (let [parts (clojure.string/split tile-key #",")]
                               q (when (>= (count parts) 2)
                                     (read-string (first parts)))
                               r (when (>= (count parts) 2)
                                     (read-string (second parts)))]
                           (and q r (point-in-polygon? [q r] (:vertices polygon)))))
             set)))))

(defn compute-all-agents-visibility
  "Compute visibility for all alive agents.
   Returns map of agent-id -> set of visible tile keys."
  [world]
  (->> (:agents world)
       (reduce (fn [vis-map agent]
                 (if (get-in agent [:status :alive?] true)
                   (assoc vis-map (:id agent) (compute-agent-visibility world agent))
                   vis-map))
               {})))

(defn get-visible-tiles-for-agent
  "Get visible tiles for a specific agent, or nil if agent-id is nil."
  [world agent-id]
  (when (and agent-id world)
    (get-in world [:agent-visibility agent-id])))
