(ns fantasia.sim.spatial)

(defn in-bounds?
  "Check whether a coordinate lies inside the world size bounds."
  [[w h] [x y]]
  (and (<= 0 x) (< x w) (<= 0 y) (< y h)))

(defn manhattan
  "Compute Manhattan distance between two coordinates."
  [[ax ay] [bx by]]
  (+ (Math/abs (long (- ax bx)))
     (Math/abs (long (- ay by)))))

(defn neighbors
  "Return orthogonal neighbor coordinates for a given position."
  [[x y]]
  [[(inc x) y]
   [(dec x) y]
   [x (inc y)]
   [x (dec y)]])

(defn at-trees?
  "True when the world has a tree at the given position."
  [world pos]
  (contains? (:trees world) pos))

(defn near-shrine?
  "True when the position is within 3 steps of the world shrine."
  [world pos]
  (when-let [s (:shrine world)]
    (<= (manhattan pos s) 3)))

(defn move-agent
  "Deterministically move an agent one step among in-bounds neighbors."
  [world agent]
  (let [size (:size world)
        options (->> (neighbors (:pos agent))
                     (filter #(in-bounds? size %))
                     vec)]
    (if (seq options)
      (assoc agent :pos (nth options (mod (+ (:tick world)
                                             (:id agent)
                                             (:seed world))
                                          (count options))))
      agent)))
