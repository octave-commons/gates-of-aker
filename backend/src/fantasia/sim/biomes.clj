(ns fantasia.sim.biomes
  (:require [fantasia.sim.hex :as hex]))

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

(defn- assign-biome-to-region
  "Assign a biome type to a rectangular region of map."
  [world q-start q-end r-start r-end biome-type]
  (let [hex-map (:map world)
        tiles (:tiles world)
        coords (for [q (range q-start (inc q-end))
                      r (range r-start (inc r-end))]
                  [q r])]
    (assoc world :tiles
           (reduce (fn [acc [q r]]
                    (if (hex/in-bounds? hex-map [q r])
                      (let [tile-key (str q "," r)
                            base-tile (get acc tile-key {:terrain :ground})]
                        (assoc acc tile-key
                               (assoc base-tile :biome biome-type)))
                      acc))
                  tiles
                  coords))))

(defn generate-biomes!
  "Generate biomes across the map by partitioning into quadrants."
  [world]
  (let [{:keys [w h origin]} (get-in world [:map :bounds])
        [oq or] (or origin [0 0])
        q-mid (+ oq (quot w 2))
        r-mid (+ or (quot h 2))
        quadrant-w (quot w 2)
        quadrant-h (quot h 2)]
    (-> world
        (assign-biome-to-region oq (+ oq quadrant-w)
                                or (+ or quadrant-h)
                                :village)
        (assign-biome-to-region (+ oq quadrant-w) (+ oq (dec w))
                                or (+ or quadrant-h)
                                :forest)
        (assign-biome-to-region oq (+ oq quadrant-w)
                                (+ or quadrant-h) (+ or (dec h))
                                :field)
        (assign-biome-to-region (+ oq quadrant-w) (+ oq (dec w))
                                (+ or quadrant-h) (+ or (dec h))
                                :rocky))))

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
                                       (assoc tile :resource resource-type))
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
