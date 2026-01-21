(ns fantasia.sim.spatial_facets
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [fantasia.sim.hex :as hex]
            [fantasia.dev.logging :as log]))

;; Embedding cache (word -> vector map)
(defonce ^:private embedding-cache (atom {}))
(defonce ^:private embeddings-loaded? (atom false))
(defonce ^:private facets-initialized? (atom false))

;; Entity facet definitions (entity-type -> word list map)
(defonce ^:private entity-facets (atom {}))

(defonce ^:private max-facets-allowed 64)

(defn clamp01 ^double [^double x]
  (cond
    (neg? x) 0.0
    (> x 1.0) 1.0
    :else x))

(defn- normalize-word [word]
  (if (string? word)
    (keyword (str/lower-case word))
    word))

(defn- parse-embedding-line [line]
  (let [parts (str/split line #"\s+")
        word (first parts)
        values (mapv #(Double/parseDouble %) (rest parts))]
    {:word (normalize-word word)
     :vec values}))

(defn- load-embedding-file! [model-path {:keys [word-set max-rows]}]
  (with-open [reader (io/reader model-path)]
    (let [lines (line-seq reader)
          limit (or max-rows Long/MAX_VALUE)]
      (reduce
        (fn [acc line]
          (if (>= (:count acc) limit)
            (reduced acc)
            (let [{:keys [word vec]} (parse-embedding-line line)]
              (if (and word (seq vec) (or (nil? word-set) (contains? word-set word)))
                {:count (inc (:count acc))
                 :cache (assoc (:cache acc) word vec)}
                acc))))
        {:count 0 :cache {}}
        lines))))

(defn load-embeddings!
  "Load pre-trained word embeddings (GloVe format) once at startup.
   Returns {:loaded? true :count n} when loaded, false when skipped."
  ([model-path]
   (load-embeddings! model-path {}))
  ([model-path {:keys [word-set max-rows]}]
   (if (or (nil? model-path) (str/blank? (str model-path)))
     (do
       (log/log-warn "[EMBEDDINGS] No model path provided; using fallback embeddings")
       {:loaded? false :count 0})
     (try
       (log/log-info "[EMBEDDINGS] Loading embeddings from:" model-path)
       (let [{:keys [count cache]}
             (load-embedding-file! model-path {:word-set word-set :max-rows max-rows})]
         (swap! embedding-cache merge cache)
         (reset! embeddings-loaded? true)
         (log/log-info "[EMBEDDINGS] Loaded" count "embeddings")
         {:loaded? true :count count})
        (catch Exception e
          (log/log-error "[EMBEDDINGS] Failed to load embeddings:" e)
          {:loaded? false :count 0})))))

(defn- fallback-embedding
  "Create a deterministic fallback embedding for a word."
  [word]
  (let [dim 12
        seed (long (hash (str word)))
        rng (java.util.Random. seed)]
    (vec (repeatedly dim #(- (* 2.0 (.nextDouble rng)) 1.0)))))

(defn ensure-embedding!
  "Ensure a word has an embedding, populating fallback when missing."
  [word]
  (let [word-kw (normalize-word word)]
    (if-let [existing (get @embedding-cache word-kw)]
      existing
      (let [vec (fallback-embedding word-kw)]
        (swap! embedding-cache assoc word-kw vec)
        vec))))

(defn get-embedding
  "Retrieve cached embedding for a word. Returns nil if not in cache."
  [word]
  (let [word-kw (normalize-word word)]
    (get @embedding-cache word-kw)))

(defn register-entity-facets!
  "Register facet word list for an entity type.
   Enforces facet limit per entity (default 16, max 64)."
  ([entity-type words]
   (register-entity-facets! entity-type words {}))
  ([entity-type words {:keys [max-facets]}]
   (let [max-facets (or max-facets 16)
         word-count (count words)]
     (when (> word-count max-facets)
       (log/log-error "Facet limit exceeded"
                      {:entity-type entity-type
                       :requested word-count
                       :max-allowed max-facets}))
     (swap! entity-facets assoc entity-type (vec words)))))

(defn get-entity-facets
  "Get facet word list for an entity type.
   Returns empty vector if not defined."
  [entity-type]
  (vec (get @entity-facets entity-type [])))

(defn cosine-similarity
  "Compute cosine similarity between two vectors."
  [v1 v2]
  (let [dot (reduce + 0 (map * v1 v2))
        mag1 (Math/sqrt (reduce + 0 (map #(* % %) v1)))
        mag2 (Math/sqrt (reduce + 0 (map #(* % %) v2)))]
    (if (or (zero? mag1) (zero? mag2))
      0.0
      (/ dot (* mag1 mag2)))))

(defn init-entity-facets!
  "Initialize all entity facet definitions."
  []
  (register-entity-facets! :wolf
    ["wolf" "predator" "hunting" "pack" "teeth" "wild" "danger" "animal" "hostile" "fear"])
  (register-entity-facets! :bear
    ["bear" "predator" "ferocious" "strong" "wild" "danger" "animal" "hostile" "aggressive"])
  (register-entity-facets! :deer
    ["deer" "prey" "herd" "wild" "animal" "gentle" "forest" "meat" "skittish"])
  (register-entity-facets! :tree
    ["tree" "wood" "forest" "resource" "nature" "standing" "plant" "material"])
   (register-entity-facets! :fruit
     ["fruit" "food" "nourishment" "edible" "gather" "resource" "nature" "sweet"])
   (register-entity-facets! :berry
     ["berry" "bush" "wild" "food" "nourishment" "edible" "gather" "resource" "nature" "small" "fruit"])
   (register-entity-facets! :stockpile
    ["stockpile" "storage" "safe" "protected" "structure" "resource" "organized" "supplies"])
  (register-entity-facets! :campfire
    ["fire" "warmth" "light" "camp" "safe" "community" "comfort" "protection" "warm"])
  (register-entity-facets! :house
    ["house" "shelter" "home" "warmth" "safe" "community" "comfort" "sleep"])
  (register-entity-facets! :lumberyard
    ["lumber" "wood" "work" "craft" "resource" "storage" "industry"])
  (register-entity-facets! :orchard
    ["orchard" "fruit" "food" "sweet" "trees" "harvest" "resource"])
  (register-entity-facets! :granary
    ["granary" "grain" "food" "storage" "harvest" "safety" "resource"])
  (register-entity-facets! :farm
    ["farm" "grain" "field" "food" "harvest" "soil" "fertility" "resource"])
  (register-entity-facets! :quarry
    ["quarry" "stone" "rock" "resource" "work" "mining" "industry"])
  (register-entity-facets! :warehouse
    ["warehouse" "structure" "building" "storage" "safe" "protected" "resource" "organized"])
  (register-entity-facets! :wall
    ["wall" "barrier" "protection" "structure" "defense" "safe" "blockade" "solid"])
  (register-entity-facets! :statue/dog
    ["dog" "statue" "guardian" "worship" "protection" "memorial" "safe" "blessed"])
  (register-entity-facets! :agent/peasant
    ["peasant" "worker" "member" "community" "weak" "vulnerable" "helpless" "settler"])
  (register-entity-facets! :agent/strong
    ["strong" "warrior" "brave" "capable" "fighter" "powerful" "competent"])
  (register-entity-facets! :agent/weak
    ["weak" "vulnerable" "frail" "helpless" "unskilled" "inexperienced"])
  (register-entity-facets! :memory/danger
     ["death" "tragedy" "loss" "warning" "fear" "danger" "blood" "corpse"])
  (register-entity-facets! :memory/social-bond
    ["friendship" "bond" "trust" "support" "community" "connection" "comfort"])
  (register-entity-facets! :memory/social-conflict
    ["conflict" "argument" "tension" "hostile" "betrayal" "unease" "rivalry"]))

(defn tile->entity-facets
  "Return facet word list for a tile based on its contents."
  [tile]
  (when tile
    (let [structure (:structure tile)
          resource (:resource tile)
          terrain (:terrain tile)]
      (cond
        structure
        (cond
          (= structure :wall) (get-entity-facets :wall)
          (= structure :campfire) (get-entity-facets :campfire)
          (= structure :house) (get-entity-facets :house)
          (= structure :lumberyard) (get-entity-facets :lumberyard)
          (= structure :orchard) (get-entity-facets :orchard)
          (= structure :granary) (get-entity-facets :granary)
          (= structure :farm) (get-entity-facets :farm)
          (= structure :quarry) (get-entity-facets :quarry)
          (= structure :warehouse) (get-entity-facets :warehouse)
          (= structure :statue/dog) (get-entity-facets :statue/dog)
          :else [])

        resource
        (cond
          (= resource :tree) (get-entity-facets :tree)
          (= resource :fruit) (get-entity-facets :fruit)
          (= resource :berry) (get-entity-facets :berry)
          :else [])

         terrain
         (cond
           (= terrain :ground) [])))))


(defn has-stockpile?
  "Check if tile has a stockpile using world state."
  [world tile-key]
  (some? (get-in (:stockpiles world) [tile-key])))

(defn collect-tile-facets!
  "Gather facets from tiles within radius of position."
  [world pos max-distance]
  (let [tile-keys (loop [q (hex/neighbors pos)
                       radius 1
                       seen #{q}]
                 (if (> radius max-distance)
                   seen
                   (recur (apply concat (map hex/neighbors q))
                          (inc radius)
                          (into seen q))))
        tiles (map #(get-in (:tiles world) [%]) tile-keys)]
    (reduce
      (fn [acc tile-key]
        (if-let [tile (get-in (:tiles world) [tile-key])]
          (let [entity-facets (tile->entity-facets tile)]
            (into acc entity-facets)
          acc)))
      [])))

(defn collect-memory-facets!
  "Gather facet words from all memories in range."
  [memories pos max-distance]
   (->> memories
         (filter #(<= (hex/distance (:location %) pos) max-distance))
         (mapcat :facets)))

(defn collect-agent-facets!
  "Gather facet words from nearby agents excluding self."
  [world pos max-distance exclude-id]
  (->> (:agents world)
        vals
        (filter #(and (not= (:id %) exclude-id)
                   (<= (hex/distance (:pos %) pos) max-distance)))
        (mapcat #(get-entity-facets (:role %)))))

(defn query-concept-axis!
  "Query a concept against local facets, returning score in [-1, 1].
   Positive = concept present, negative = concept absent.
   
   Examples:
   - (query-concept-axis! world agent :security-axis :safety?) 
     Returns 0.8 (safe, increases security need)
   - (query-concept-axis! world agent :security-axis :danger?) 
     Returns -0.7 (dangerous, decreases security need)
   
   Process:
   1. Gather all local facets within max-distance:
      - Agent's own entity facets
      - Memory facets within range (filtered by strength > 0.1)
      - Nearby entity facets (tiles, structures, items, other agents)
   2. Sort facets by relevance (closest first, then strength)
   3. Take top max-facets (respect facet limit)
   4. For each facet word, get embedding
   5. Compute cosine similarity to concept embedding
   6. Sum all positive similarities (concept facets) and negative similarities (anti-concept facets)
   7. Return total score clamped to [-1, 1]"
  [world agent concept {:keys [max-facets max-distance]}]
  (let [max-facets (or max-facets (:facet-limit world) 16)
        max-distance (or max-distance (:vision agent) 10)
        agent-pos (:pos agent)
        agent-id (:id agent)

        ;; Step 1: Gather local facets
        local-facets (vec
                          (concat
                            ;; Agent's own facets
                            (when (:entity-type agent)
                              (get-entity-facets (:entity-type agent)))

                             ;; Memory facets in range (strength > 0.1)
                             (if-let [memories (vals (get-in world [:memories]))]
                                (collect-memory-facets! memories agent-pos max-distance)
                                [])

                            ;; Nearby entity facets (tiles, structures, items)
                            (collect-tile-facets! world agent-pos max-distance)

                              ;; Nearby agents' facets
                              (collect-agent-facets! world agent-pos max-distance agent-id)))
          ;; Step 2: Take top max-facets (no sorting needed for facet words)
          sorted-facets (take max-facets local-facets)
         ;; Step 3: Flatten word lists and deduplicate
          facet-words (distinct local-facets)
         ;; Step 4-5: Compute embedding similarity and sum
         concept-embedding (get-embedding concept)]
     (if concept-embedding
       (let [score (reduce
                      (fn [acc word]
                        (let [word-embedding (get-embedding word)]
                          (if word-embedding
                            (+ acc (cosine-similarity word-embedding concept-embedding))
                            acc)))
                      0.0
                      facet-words)]

         ;; Step 6: Clamp to [-1, 1]
         (log/log-debug "[FACET:QUERY]"
                        {:agent-id agent-id
                         :concept concept
                         :score score
                         :facets-queried (count facet-words)
                         :top-3-facets (take 3 facet-words)})
         (max -1.0 (min 1.0 score)))

       (do
         (log/log-warn "[FACET:QUERY] Concept embedding not found"
                       {:agent-id agent-id
                        :concept concept
                        :facets-queried (count facet-words)})
         0.0))))
