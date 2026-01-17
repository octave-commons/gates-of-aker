(ns fantasia.sim.hex)

(def pointy-dirs
  "Axial direction vectors for pointy-top hexes (q, r)."
  [[1 0]
   [1 -1]
   [0 -1]
   [-1 0]
   [-1 1]
   [0 1]])

(defn add
  "Add two axial coordinates together."
  [[aq ar] [bq br]]
  [(+ aq bq) (+ ar br)])

(defn neighbors
  "Return the six axial neighbors around `pos`."
  [pos]
  (mapv #(add pos %) pointy-dirs))

(defn distance
  "Hex (axial) distance between points a and b."
  [[aq ar] [bq br]]
  (let [dq (Math/abs (long (- aq bq)))
        dr (Math/abs (long (- ar br)))
        ;; s = -q - r for axial coords
        as (- (+ aq ar))
        bs (- (+ bq br))
        ds (Math/abs (long (- as bs)))]
    (max dq dr ds)))

(defn- ensure-origin
  "Ensure bounds maps carry an :origin (default [0 0])."
  [bounds]
  (if (contains? bounds :origin)
    bounds
    (assoc bounds :origin [0 0])))

(defn normalize-bounds
  "Normalize user-supplied bounds into {:shape :rect ... :origin [0 0]}.
   Supported inputs:
   - {:shape :rect :w 20 :h 20}
   - {:shape :radius :r 10}
   - {:width 20 :height 20}, {:w 20 :h 20}
   - [w h]
   Returns {:shape :rect ...} for now; :radius bounds are passed through."
  [input default]
  (let [input (or input default)
        from-vec (fn [[w h]] {:shape :rect :w w :h h})
        bounds (cond
                 (map? input)
                 (cond
                   (:shape input) input
                   (and (:width input) (:height input))
                   {:shape :rect :w (:width input) :h (:height input)}
                   (and (:w input) (:h input))
                   {:shape :rect :w (:w input) :h (:h input)}
                   :else input)
                 (and (sequential? input) (>= (count input) 2))
                 (from-vec [(nth input 0) (nth input 1)])
                 :else default)]
    (ensure-origin bounds)))

(defn- rect-in-bounds?
  [{:keys [origin w h]} [q r]]
  (let [[oq or] (or origin [0 0])
        q-max (+ oq (dec (long w)))
        r-max (+ or (dec (long h)))]
    (and (<= oq q)
         (<= q q-max)
         (<= or r)
         (<= r r-max))))

(defn- radius-in-bounds?
  [{:keys [origin r]} pos]
  (let [origin (or origin [0 0])]
    (<= (distance origin pos) (long r))))

(defn in-bounds?
  "True when the given position lies within the provided map metadata.
   `hex-map` expects {:bounds {:shape ...}} as stored in world state."
  [hex-map pos]
  (let [{:keys [bounds]} hex-map
        {:keys [shape]} bounds
        bounds (ensure-origin bounds)]
    (case shape
      :radius (radius-in-bounds? bounds pos)
      ;; Default to :rect when unspecified
      (rect-in-bounds? bounds pos))))

(defn rand-pos
  "Sample a random axial coordinate inside the provided `hex-map`.
   Accepts a java.util.Random instance for determinism."
  [^java.util.Random rng hex-map]
  (let [{:keys [bounds]} hex-map
        {:keys [shape]} bounds
        bounds (ensure-origin bounds)
        [oq or] (:origin bounds [0 0])]
    (case shape
      :radius
      (let [radius (long (:r bounds 1))
            span (inc (* 2 radius))]
        (loop []
          (let [q (+ (- oq radius) (.nextInt rng (int (max 1 span))))
                r (+ (- or radius) (.nextInt rng (int (max 1 span))))
                pos [q r]]
            (if (in-bounds? {:bounds bounds} pos)
              pos
              (recur)))))
      ;; :rect (fallback)
      (let [w (long (:w bounds 1))
            h (long (:h bounds 1))]
        [(+ oq (.nextInt rng (int (max 1 w))))
         (+ or (.nextInt rng (int (max 1 h))))]))))

(defn rect-width
  "Return the width of the bounds if :rect, otherwise nil."
  [hex-map]
  (let [{:keys [bounds]} hex-map]
    (when (= :rect (:shape bounds))
      (long (:w bounds)))))

(defn rect-height
  "Return the height of the bounds if :rect, otherwise nil."
  [hex-map]
  (let [{:keys [bounds]} hex-map]
    (when (= :rect (:shape bounds))
      (long (:h bounds)))))
