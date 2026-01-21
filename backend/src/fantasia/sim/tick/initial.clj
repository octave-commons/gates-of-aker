(ns fantasia.sim.tick.initial
    (:require [clojure.set :as set]
              [clojure.string :as str]
              [fantasia.sim.hex :as hex]
              [fantasia.sim.biomes :as biomes]
              [fantasia.sim.time :as sim-time]
              [fantasia.sim.tick.trees :as trees]
              [fantasia.sim.jobs :as jobs]
              [fantasia.sim.jobs.providers :as job-providers]
              [fantasia.sim.constants :as const]
              [fantasia.sim.traces :as traces]))

(defn tile-key [q r] [q r])
(defn parse-tile-key [[q r]] [q r])

(defn rng [seed] (java.util.Random. (long seed)))
(defn rand-int* [^java.util.Random r n] (.nextInt r (int n)))

(def default-agent-stats
  {:strength 0.4
   :dexterity 0.4
   :fortitude 0.4
   :charisma 0.4})

(def base-names
  ["Aren" "Bela" "Cato" "Dara" "Elda" "Fenn" "Galen" "Hara"
   "Ilya" "Jori" "Kara" "Leto" "Mira" "Nico" "Orin" "Pela"
   "Quin" "Rhea" "Soren" "Tala" "Ulric" "Vera" "Wren" "Xara"
   "Yuna" "Zane"])

(defn agent-name
  [id role]
  (if (#{:wolf :bear :deer} role)
    (str (str/capitalize (name role)) "-" id)
    (let [base (nth base-names (mod id (count base-names)))]
      (str base "-" id))))

(def default-agent-needs
  {:food 0.7
   :water 0.7
   :rest 0.7
   :health 1.0
   :security 0.6
   :mood 0.5
   :social 0.55
   :warmth 0.6})

(def default-need-thresholds
   {:food-starve 0.05 :food-hungry 0.3 :food-satisfied 0.8
    :water-dehydrate 0.0 :water-thirsty 0.3 :water-satisfied 0.8
    :rest-collapse 0.0 :rest-tired 0.3 :rest-rested 0.8
    :health-critical 0.05 :health-low 0.4 :health-stable 0.8
    :security-panic 0.0 :security-unsettled 0.4 :security-safe 0.9
    :mood-depressed 0.0 :mood-low 0.3 :mood-uplifted 0.8
     :social-lonely 0.0 :social-low 0.3 :social-sated 0.8
     :warmth-freeze 0.0 :warmth-cold 0.3 :warmth-comfort 0.8})

(defn generate-voice [id]
   (let [rng (rng id)
         waveform-types [:sine :triangle :square :sawtooth]
         waveform (nth waveform-types (rand-int* rng (count waveform-types)))
         pitch-offset (+ -12 (* 24 (/ (rand-int* rng 100) 100.0)))
         vibrato-depth (/ (rand-int* rng 30) 100.0)
         attack-time (/ (rand-int* rng 50) 1000.0)]
     {:waveform waveform
      :pitch-offset pitch-offset
      :vibrato-depth vibrato-depth
      :attack-time attack-time}))

(defn generate-sex [id]
   (let [rng (rng id)
         sex-choice (rand-int* rng 2)]
     (if (zero? sex-choice) :male :female)))

(defn ->agent [id q r role]
   {:id id
     :name (agent-name id role)
     :pos [q r]
     :role role
     :faction :player
     :sex (generate-sex id)
     :stats default-agent-stats
     :needs default-agent-needs
     :need-thresholds default-need-thresholds
     :inventories {:personal {:wood 0 :food 0}
                   :hauling {}
                   :equipment {}}
     :status {:alive? true :asleep? false :idle? false}
     :inventory {:wood 0 :food 0}
     :relationships {}
     :last-social-tick nil
     :last-social-thought nil
     :frontier {}
     :recall {}
     :events []
     :voice (generate-voice id)})

(defn- next-agent-id [world]
  (if-let [agents (:agents world)]
    (if (empty? agents)
      0
      (inc (apply max (map :id agents))))
     0))

(defn ->wildlife-agent [world pos role]
   (let [[q r] pos
         agent-id (next-agent-id world)]
     {:id agent-id
      :name (agent-name agent-id role)
      :pos [q r]
      :role role
      :faction :wilderness
      :sex (generate-sex agent-id)
      :stats default-agent-stats
      :needs default-agent-needs
      :need-thresholds default-need-thresholds
      :inventories {:personal {:wood 0 :food 0}
                    :hauling {}
                    :equipment {}}
      :status {:alive? true :asleep? false :idle? false}
      :inventory {:wood 0 :food 0}
      :relationships {}
      :last-social-tick nil
      :last-social-thought nil
      :frontier {}
      :recall {}
      :events []
      :voice (generate-voice agent-id)}))

(defn- bounds-tile-count [hex-map]
  (let [{:keys [bounds]} hex-map
        {:keys [shape]} bounds]
    (case shape
      :radius (let [radius (long (:r bounds 1))]
                (+ 1 (* 3 radius (inc radius))))
      (* (long (:w bounds 1)) (long (:h bounds 1))))))

(defn- positions-within-radius [origin radius]
  (loop [frontier #{origin}
         visited #{origin}
         step 0]
    (if (>= step radius)
      visited
      (let [next (->> frontier
                      (mapcat hex/neighbors)
                      set)
            next (set/difference next visited)]
        (recur next (into visited next) (inc step))))))

(defn- nearby-positions [pos radius]
  (->> (positions-within-radius pos radius)
       (remove #(= % pos))
       (sort-by (fn [p] (hex/distance pos p)))))

(defn- eligible-wildlife-positions
  [world biome-types]
  (let [agent-positions (set (map :pos (:agents world)))]
    (->> (:tiles world)
         (keep (fn [[k tile]]
                 (when (and (contains? biome-types (:biome tile))
                            (nil? (:structure tile))
                            (not (contains? agent-positions k)))
                   (parse-tile-key k)))))))

(defn- shuffled-positions
  [rng positions]
  (sort-by (fn [_] (.nextDouble rng)) positions))

(defn- spawn-wildlife
  [world rng {:keys [role density biomes]}]
  (let [positions (eligible-wildlife-positions world biomes)
        desired (long (Math/ceil (* (count positions) density)))
        picks (take desired (shuffled-positions rng positions))]
    (reduce (fn [w pos]
              (update w :agents (fnil conj []) (->wildlife-agent w pos role)))
            world
            picks)))

(defn- quarry-position [world]
  (some (fn [[k tile]]
          (when (= (:structure tile) :quarry)
            k))
        (:tiles world)))

(defn- ensure-quarry-resource [world rng]
  (if-let [quarry-pos (quarry-position world)]
    (let [tile (get-in world [:tiles quarry-pos])]
      (if (and tile (contains? jobs/mineral-types (:resource tile)))
        world
        (if tile
          (let [resource (if (< (.nextDouble rng) 0.35)
                           (nth jobs/ore-types (.nextInt rng (count jobs/ore-types)))
                           :rock)]
            (assoc-in world [:tiles quarry-pos :resource] resource))
          world)))
    world))

(defn- scatter-fruit! [world rng]
  (let [hex-map (:map world)
        total-tiles (bounds-tile-count hex-map)
        desired (max 20 (long (Math/ceil (* total-tiles 0.002))))
        positions (loop [acc #{}
                         attempts 0]
                     (if (or (>= (count acc) desired)
                             (>= attempts (* desired 6)))
                      acc
                      (recur (conj acc (hex/rand-pos rng hex-map)) (inc attempts))))]
    (reduce (fn [w pos] (jobs/add-item! w pos :fruit 1)) world positions)))

(defn initial-world [opts]
   (let [{:keys [seed bounds tree-density]} opts
          actual-seed (or seed 1)
          tree-density (or tree-density 0.08)
          rand-gen (rng actual-seed)
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
              :temperature 0.6
              :cold-snap 0.4
              :daylight 0.7
              :jobs []
              :items {}
              :stockpiles {}
              :levers {:iconography {:fire->patron 0.80
                                     :lightning->storm 0.75
                                     :storm->deity 0.85}
                        :mouthpiece-agent-id nil
                        :ollama-embed-url "http://localhost:11434/api/embed"
                        :ollama-embed-model "nomic-embed-text"
                        :ollama-embed-timeout-ms 10000
                        :ollama-url "http://localhost:11434/api/generate"
                        :ollama-model "qwen3:4b"
                        :ollama-timeout-ms 60000
                        :ollama-retries 1
                        :ollama-retry-delay-ms 2000
                        :ollama-keep-alive-enabled true
                        :ollama-keep-alive-interval-ms 300000}
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
                :memories {}
                 :recent-events []
                :recent-max const/default-recent-max
                :traces []
                :trace-max const/default-trace-max
                :books {}
                :books-list []
                 :favor 0.0
                 :faith 0.0
                 :deities {:patron/fire {:faith 0.0}
                           :deity/storm {:faith 0.0}}
                :champions []
               :cultures {"culture-1" (traces/create-culture "culture-1"
                                                          "The Northern Covenant"
                                                          [:fire :winter :judgment :community :sacrifice]
                                                          ["revelation" "covenant" "judgment" "test" "sacred" "embers" "cold" "judgment"])}
               :jobs-by-id {}}
           base-world (assoc base-world :calendar (sim-time/calendar-info base-world))
          world-with-biomes (biomes/generate-biomes! base-world)
         world-with-resources (biomes/spawn-biome-resources! world-with-biomes)
          world-with-trees (trees/spawn-initial-trees! world-with-resources tree-density)
            world-with-fruit (scatter-fruit! world-with-trees rand-gen)
             campfire-pos (biomes/rand-pos-in-biome rand-gen hex-map :field (:tiles world-with-fruit))
           [q r] campfire-pos
           campfire-key (tile-key q r)
            world-with-campfire (-> world-with-fruit
                                    (assoc :campfire campfire-pos)
                                    (assoc :shrine campfire-pos)
                                    (update-in [:tiles campfire-key] merge {:structure :campfire}))
           agent-count 16
           nearby-tiles (nearby-positions campfire-pos 3)
           house-tiles (take agent-count nearby-tiles)
           building-tiles (take 7 (drop agent-count nearby-tiles))
            world-with-houses (reduce (fn [w pos]
                                        (let [[q r] pos
                                              k (tile-key q r)]
                                          (update-in w [:tiles k] merge {:structure :house})))
                                      world-with-campfire
                                      house-tiles)
           building-types [:lumberyard :orchard :granary :quarry :workshop :improvement-hall :smelter]
           world-with-buildings
            (reduce (fn [w [pos structure]]
                      (let [[q r] pos
                            k (tile-key q r)
                            resource (case structure
                                       :lumberyard :log
                                       :orchard :fruit
                                       :granary :grain
                                       :quarry :rock
                                       nil)]
                        (let [w' (assoc-in w [:tiles k]
                                           (merge {:terrain :ground :structure structure :resource nil :level 1}))]
                          (if resource
                            (jobs/create-stockpile! w' pos resource 120)
                            w'))))
                   world-with-houses
                   (map vector building-tiles building-types))
             world-with-quarry-resource (ensure-quarry-resource world-with-buildings rand-gen)
             world-with-warehouse (update-in world-with-quarry-resource
                                            [:tiles (tile-key 0 0)] merge {:structure :warehouse})
              world-with-stockpile (jobs/create-stockpile! world-with-warehouse [0 0] :fruit 200)
              world-with-food (jobs/add-to-stockpile! world-with-stockpile [0 0] :fruit 200)]
       (println "Warehouse created:" (get-in world-with-food [:tiles (tile-key 0 0)]))
      (println "Stockpiles:" (:stockpiles world-with-food))
      (println "Items:" (:items world-with-food))
       (let [world-with-agents
             (assoc world-with-food
                    :agents (vec (for [i (range agent-count)]
                                   (let [[q r] (nth (concat [campfire-pos] nearby-tiles) (mod i (inc (count nearby-tiles))))]
                                     (->agent i q r (cond
                                                     (= i 0) :priest
                                                     (= i 1) :knight
                                                     :else :peasant))))))
             world-with-wildlife
             (-> world-with-agents
                 (spawn-wildlife rand-gen {:role :deer :density 0.003 :biomes #{:forest :field}})
                 (spawn-wildlife rand-gen {:role :wolf :density 0.0008 :biomes #{:forest}}))]
         (job-providers/seed-initial-jobs world-with-wildlife agent-count))))
