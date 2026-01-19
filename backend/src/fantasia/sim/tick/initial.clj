(ns fantasia.sim.tick.initial
  (:require [fantasia.sim.hex :as hex]
            [fantasia.sim.biomes :as biomes]
            [fantasia.sim.tick.trees :as trees]
            [fantasia.sim.jobs :as jobs]))

(defn rng [seed] (java.util.Random. (long seed)))
(defn rand-int* [^java.util.Random r n] (.nextInt r (int n)))

(defn ->agent [id q r role]
   {:id id
    :pos [q r]
    :role role
    :needs {:warmth 0.6 :food 0.7 :sleep 0.7}
    :need-thresholds {:food-starve 0.0 :food-hungry 0.3 :food-satisfied 0.8
                      :sleep-exhausted 0.0 :sleep-tired 0.3 :sleep-rested 0.8}
    :inventory {:wood 0 :food 0}
    :frontier {}
    :recall {}})

(defn initial-world [opts]
   (let [{:keys [seed bounds tree-density]} opts
         actual-seed (or seed 1)
         tree-density (or tree-density 0.05)
         r (rng actual-seed)
         hex-bounds (hex/normalize-bounds bounds {:shape :rect :w 128 :h 128})
         hex-map {:kind :hex
                   :layout :pointy
                   :bounds hex-bounds}
         base-world
         {:seed actual-seed
          :tick 0
          :map hex-map
          :tiles {}
          :shrine nil
          :cold-snap 0.85
          :jobs []
          :items {}
          :stockpiles {}
          :levers {:iconography {:fire->patron 0.80
                                  :lightning->storm 0.75
                                  :storm->deity 0.85}
                     :mouthpiece-agent-id nil}
          :institutions
          {:temple {:id :temple
                    :name "Temple of Embers"
                    :entropy 0.2
                    :broadcast-every 6
                    :canonical {:facets [:fire :judgment :winter]
                                :claim-hint :claim/winter-judgment-flame}}}
           :edges {[:cold :fire] 0.60
                   [:trees :fire] 0.45
                   [:lightning :storm] 0.70
                   [:storm :deity/storm] 0.80
                   [:fire :patron/fire] 0.80
                   [:patron/fire :judgment] 0.35
                   [:deity/storm :awe] 0.25
                   [:judgment :awe] 0.25}
           :ledger {}
           :recent-events []
           :recent-max 30
           :traces []
           :trace-max 250}
         world-with-biomes (biomes/generate-biomes! base-world)
         world-with-resources (biomes/spawn-biome-resources! world-with-biomes)
         world-with-trees (trees/spawn-initial-trees! world-with-resources tree-density)
         world-with-warehouse (assoc-in world-with-trees [:tiles "0,0"] {:terrain :ground :structure :warehouse :resource nil})
         world-with-stockpile (jobs/create-stockpile! world-with-warehouse [0 0] :food 200)
         world-with-food (jobs/add-item! world-with-stockpile [0 0] :food 10)]
     (println "Warehouse created:" (get-in world-with-food [:tiles "0,0"]))
     (println "Stockpiles:" (:stockpiles world-with-food))
     (println "Items:" (:items world-with-food))
     (assoc world-with-food
            :agents (vec (for [i (range 12)]
                           (let [[q r] (biomes/rand-pos-in-biome r hex-map :village (:tiles world-with-food))]
                             (->agent i q r (cond
                                                 (= i 0) :priest
                                                 (= i 1) :knight
                                                 :else :peasant))))))))
