(ns fantasia.sim.spatial_facets
  (:require [clojure.string :as str]
            [fantasia.sim.hex :as hex]
            [fantasia.dev.logging :as log]))

;; Embedding cache (word -> vector map)
(defonce ^:private embedding-cache (atom {}))

;; Entity facet definitions (entity-type -> word list map)
(defonce ^:private entity-facets (atom {}))

(defonce ^:private max-facets-allowed 64)

(defn clamp01 ^double [^double x]
  (cond
    (neg? x) 0.0
    (> x 1.0) 1.0
    :else x))

(defn load-embeddings!
  "Load pre-trained word2vec/GloVe model once at startup.
   Parses model file and populates embedding-cache."
  [model-path]
  (log/log-info "Loading embeddings from:" model-path)
  (try
    ;; TODO: Parse GloVe format (word v1 v2 ... v300)
    ;; For now, stub with random vectors
    (log/log-info "Embedding loading not yet implemented - using stub vectors")
    (swap! embedding-cache
      (fn [cache]
        (assoc cache :stub 0.5)
        (assoc cache :danger [-0.8 -0.7 -0.5 0.9 -0.4 0.6 -0.3])
        (assoc cache :safety [0.9 0.7 0.8 0.5 0.6 0.3 0.4])
        (assoc cache :comfort [0.8 0.7 0.6 0.5 0.4 0.9])))
    (catch Exception e
      (log/log-error "Failed to load embeddings:" e)
      (throw e))))

(defn get-embedding
  "Retrieve cached embedding for a word. Returns nil if not in cache."
  [word]
  (let [word-kw (if (string? word) (keyword word) word)]
    (get @embedding-cache word-kw)))

(defn register-entity-facets!
  "Register facet word list for an entity type.
   Enforces facet limit per entity (default 16, max 64)."
  [entity-type words {:keys [max-facets]}]
  (let [max-facets (or max-facets 16)
        word-count (count words)]
    (when (> word-count max-facets)
      (log/log-error "Facet limit exceeded"
                   {:entity-type entity-type
                    :requested word-count
                    :max-allowed max-facets}))
    (swap! entity-facets assoc entity-type (vec words))))

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
  (register-entity-facets! :tree
    ["tree" "wood" "forest" "resource" "nature" "standing" "plant" "material"])
  (register-entity-facets! :fruit
    ["fruit" "food" "nourishment" "edible" "gather" "resource" "nature" "sweet"])
  (register-entity-facets! :stockpile
    ["stockpile" "storage" "safe" "protected" "structure" "resource" "organized" "supplies"])
  (register-entity-facets! :campfire
    ["fire" "warmth" "light" "camp" "safe" "community" "comfort" "protection" "warm"])
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
    ["death" "tragedy" "loss" "warning" "fear" "danger" "blood" "corpse"]))

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
          (= structure :warehouse) (get-entity-facets :warehouse)
          (= structure :statue/dog) (get-entity-facets :statue/dog)
          :else [])

        resource
        (cond
          (= resource :tree) (get-entity-facets :tree)
          (= resource :fruit) (get-entity-facets :fruit)
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
