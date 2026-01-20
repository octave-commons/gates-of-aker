(ns fantasia.sim.biomes
  (:require [fantasia.sim.hex :as hex]))

(def ^:private simplex-gradients
  [[1 1] [-1 1] [1 -1] [-1 -1]
   [1 0] [-1 0] [0 1] [0 -1]])

(defn- build-perm [seed]
  (let [r (java.util.Random. (long seed))
        base (int-array 256)]
    (dotimes [i 256]
      (aset base i i))
    (dotimes [i 256]
      (let [j (.nextInt r 256)
            tmp (aget base i)]
        (aset base i (aget base j))
        (aset base j tmp)))
    (let [perm (int-array 512)]
      (dotimes [i 512]
        (aset perm i (aget base (bit-and i 255))))
      perm)))

(defn- dot2 [[gx gy] x y]
  (+ (* gx x) (* gy y)))

(defn- simplex2d [perm x y]
  (let [f2 (* 0.5 (- (Math/sqrt 3.0) 1.0))
        g2 (/ (- 3.0 (Math/sqrt 3.0)) 6.0)
        s (* (+ x y) f2)
        i (long (Math/floor (+ x s)))
        j (long (Math/floor (+ y s)))
        t (* (+ i j) g2)
        x0 (- x (- i t))
        y0 (- y (- j t))
        [i1 j1] (if (> x0 y0) [1 0] [0 1])
        x1 (+ (- x0 i1) g2)
        y1 (+ (- y0 j1) g2)
        x2 (+ (- x0 1.0) (* 2.0 g2))
        y2 (+ (- y0 1.0) (* 2.0 g2))
        ii (bit-and i 255)
        jj (bit-and j 255)
        gi0 (mod (aget perm (+ ii (aget perm jj))) 8)
        gi1 (mod (aget perm (+ ii i1 (aget perm (+ jj j1)))) 8)
        gi2 (mod (aget perm (+ ii 1 (aget perm (+ jj 1)))) 8)
        t0 (- 0.5 (+ (* x0 x0) (* y0 y0)))
        t1 (- 0.5 (+ (* x1 x1) (* y1 y1)))
        t2 (- 0.5 (+ (* x2 x2) (* y2 y2)))
        n0 (if (neg? t0) 0.0 (let [t0 (* t0 t0)] (* t0 t0 (dot2 (nth simplex-gradients gi0) x0 y0))))
        n1 (if (neg? t1) 0.0 (let [t1 (* t1 t1)] (* t1 t1 (dot2 (nth simplex-gradients gi1) x1 y1))))
        n2 (if (neg? t2) 0.0 (let [t2 (* t2 t2)] (* t2 t2 (dot2 (nth simplex-gradients gi2) x2 y2))))]
    (* 70.0 (+ n0 n1 n2))))

(def biome-definitions
  "Map of biome type keyword to biome metadata."
  {:forest {:base-color "#2e7d32"
            :resource :tree
            :spawn-prob 0.3
            :description "Dense forest with abundant trees"}
   :village {:base-color "#8d6e63"
             :resource nil
             :spawn-prob 0.0
             :description "Settlement area"}
   :field {:base-color "#9e9e24"
           :resource :grain
           :spawn-prob 0.25
           :description "Open fields with grain crops"}
   :rocky {:base-color "#616161"
           :resource :rock
           :spawn-prob 0.2
           :description "Rocky terrain with stone resources"}})

(def ore-types
  [:ore-iron :ore-copper :ore-tin :ore-gold :ore-silver :ore-aluminum :ore-lead])

(defn- pick-rock-resource [^java.util.Random r]
  (let [roll (.nextDouble r)]
    (if (< roll 0.35)
      (nth ore-types (.nextInt r (count ore-types)))
      :rock)))

(defn generate-biomes!
  "Generate biomes across the map using simplex noise." 
  [world]
  (let [hex-map (:map world)
        tiles (:tiles world)
        {:keys [bounds]} hex-map
        {:keys [w h origin]} bounds
        [oq or] (or origin [0 0])
        perm (build-perm (:seed world))
        scale 0.08
        village-scale 0.14
        center [(+ oq (quot w 2)) (+ or (quot h 2))]
        max-dist (max 1 (long (/ (max w h) 2)))
        coords (for [q (range oq (+ oq w))
                     r (range or (+ or h))]
                 [q r])]
    (assoc world :tiles
           (reduce
             (fn [acc [q r]]
               (if (hex/in-bounds? hex-map [q r])
                 (let [tile-key (str q "," r)
                       base-tile (get acc tile-key {:terrain :ground})
                       noise (simplex2d perm (* q scale) (* r scale))
                       bias-dist (min 1.0 (/ (double (hex/distance center [q r])) max-dist))
                       field-bias (* 0.18 (- 1.0 bias-dist))
                       value (+ noise field-bias)
                       village-noise (simplex2d perm (* q village-scale) (* r village-scale))
                       village? (and (> village-noise 0.55) (< bias-dist 0.85))
                       biome (cond
                               village? :village
                               (< value -0.25) :rocky
                               (< value 0.05) :forest
                               (< value 0.45) :field
                               :else :forest)]
                   (assoc acc tile-key (assoc base-tile :biome biome)))
                 acc))
             tiles
             coords))))

(defn spawn-biome-resources!
  "Spawn resources on tiles based on their biome type."
  [world]
  (let [r (java.util.Random. (:seed world))
        tiles (:tiles world)
        updated-tiles (reduce-kv
                        (fn [acc tile-key tile]
                          (if-let [biome-type (:biome tile)]
                            (let [biome-def (get biome-definitions biome-type)
                                   resource-type (:resource biome-def)
                                   spawn-prob (:spawn-prob biome-def)]
                               (if (and resource-type
                                        (< (.nextDouble r) spawn-prob))
                                 (assoc acc tile-key
                                        (assoc tile :resource (if (= resource-type :rock)
                                                                (pick-rock-resource r)
                                                                resource-type)))
                                 acc))
                            acc))
                        tiles
                        tiles)]
    (assoc world :tiles updated-tiles)))

(defn biome-color
  "Get base color for a biome type."
  [biome-type]
  (get-in biome-definitions [biome-type :base-color] "#777"))

(defn rand-pos-in-biome
  "Sample a random position within a specific biome."
  [^java.util.Random rng hex-map biome-type tiles]
  (let [biome-positions (reduce-kv
                          (fn [acc tile-key tile]
                            (if (= (:biome tile) biome-type)
                              (let [[q r] (map #(Long/parseLong %) (clojure.string/split tile-key #","))]
                                (conj acc [q r]))
                              acc))
                          []
                          tiles)]
    (if (empty? biome-positions)
      (hex/rand-pos rng hex-map)
      (let [idx (.nextInt rng (count biome-positions))]
        (nth biome-positions idx)))))
