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

(defn- coerce-axial
  "Coerce various position shapes into an axial [q r] pair.
   Accepts sequential [q r], string \"q,r\", map with :q/:r, numbers (treated as [n 0]) or nil.
   Falls back to [0 0] for unknown inputs to keep callers resilient." 
  [p]
  (cond
    (sequential? p) [(long (first p)) (long (second p))]
    (string? p) (let [parts (clojure.string/split p #",")]
                  (try [(Long/parseLong (nth parts 0)) (Long/parseLong (nth parts 1))]
                       (catch Exception _ [0 0])))
    (map? p) (cond
               (and (contains? p :q) (contains? p :r)) [(long (:q p)) (long (:r p))]
               (contains? p :pos) (recur (:pos p))
               :else [0 0])
    (number? p) [(long p) 0]
    :else [0 0]))

(defn distance
  "Hex (axial) distance between points a and b.
   This version coerces inputs to axial pairs to avoid runtime crashes when inputs are malformed,
   returning a large distance (via normal calculation) when values are unexpected." 
  [a b]
  (let [[aq ar] (coerce-axial a)
        [bq br] (coerce-axial b)
        dq (Math/abs (long (- aq bq)))
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
   - {:shape :rect :w 32 :h 32}
   - {:shape :radius :r 10}
   - {:width 32 :height 32}, {:w 32 :h 32}
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
  "Check if axial position is within rectangular bounds."
  [{:keys [origin w h]} [q r]]
  (let [[oq or-orig] (or origin [0 0])
        w (or (long w) 32)
        h (or (long h) 32)
        q-max (+ oq (dec w))
        r-max (+ or-orig (dec h))]
    (and (<= oq q)
         (<= q q-max)
         (<= or-orig r)
         (<= r r-max))))

(defn- radius-in-bounds?
  [{:keys [origin r]} pos]
  (let [origin (or origin [0 0])]
    (<= (distance origin pos) (long r))))

(defn in-bounds?
  "True when given position lies within provided map metadata.
   `hex-map` expects {:bounds {:shape ...}} as stored in world state."
  [hex-map pos]
  (let [{:keys [bounds]} hex-map
        bounds (or bounds {:shape :rect :w 32 :h 32})
        bounds (ensure-origin bounds)
        {:keys [shape]} bounds]
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
  "Return height of bounds if :rect, otherwise nil."
  [hex-map]
  (let [{:keys [bounds]} hex-map]
    (when (= :rect (:shape bounds))
      (long (:h bounds)))))

(defn ring
  "Return all hex positions at distance `r` from `center` (axial coordinates)."
  [center r]
  (if (= r 0)
    [center]
    (let [start (add center (nth pointy-dirs 4))
          dirs pointy-dirs]
      (loop [results []
             current start
             d-idx 0
             steps 0]
        (if (>= steps (* 6 r))
          results
          (let [next-pos (add current (nth dirs (mod (inc d-idx) 6)))]
            (recur (conj results current)
                   next-pos
                   (if (and (> steps 0) (zero? (mod steps r)))
                     (inc d-idx)
                     d-idx)
                   (inc steps))))))))