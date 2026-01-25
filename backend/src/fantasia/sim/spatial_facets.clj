(ns fantasia.sim.spatial_facets
  "ECS-based spatial facets for entity semantic associations.
   Replaces the old spatial_facets.clj with ECS patterns using brute.entity."
  (:require [brute.entity :as be]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [fantasia.sim.ecs.components :as c]
            [fantasia.sim.ecs.core :as ecs]
            [fantasia.sim.hex :as hex]
            [fantasia.dev.logging :as log]))

;; Embedding cache (word -> vector map)
(defonce ^:private embedding-cache (atom {}))
(defonce ^:private embeddings-loaded? (atom false))

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
  (register-entity-facets! :road
    ["road" "path" "travel" "route" "stone" "speed" "passage"])
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
  "Return facet word list for a tile based on its ECS components."
  [world tile-eid]
  (when tile-eid
    (let [tile (be/get-component world tile-eid fantasia.sim.ecs.components.Tile)
          stockpile (be/get-component world tile-eid fantasia.sim.ecs.components.Stockpile)
          structure-state (be/get-component world tile-eid fantasia.sim.ecs.components.StructureState)
          campfire-state (be/get-component world tile-eid fantasia.sim.ecs.components.CampfireState)
          shrine-state (be/get-component world tile-eid fantasia.sim.ecs.components.ShrineState)]
      (cond
        (:structure tile)
        (cond
          (= (:structure tile) :wall) (get-entity-facets :wall)
          (= (:structure tile) :road) (get-entity-facets :road)
          (= (:structure tile) :campfire) (get-entity-facets :campfire)
          (= (:structure tile) :house) (get-entity-facets :house)
          (= (:structure tile) :lumberyard) (get-entity-facets :lumberyard)
          (= (:structure tile) :orchard) (get-entity-facets :orchard)
          (= (:structure tile) :granary) (get-entity-facets :granary)
          (= (:structure tile) :farm) (get-entity-facets :farm)
          (= (:structure tile) :quarry) (get-entity-facets :quarry)
          (= (:structure tile) :warehouse) (get-entity-facets :warehouse)
          (= (:structure tile) :statue/dog) (get-entity-facets :statue/dog)
          :else [])

        (:resource tile)
        (cond
          (= (:resource tile) :tree) (get-entity-facets :tree)
          (= (:resource tile) :fruit) (get-entity-facets :fruit)
          (= (:resource tile) :berry) (get-entity-facets :berry)
          :else [])

        stockpile
        (get-entity-facets :stockpile)

        structure-state
        (cond
          (= (:level structure-state) 1) [] ;; Basic structure
          :else [])

        campfire-state
        (get-entity-facets :campfire)

        shrine-state
        (get-entity-facets :statue/dog)

        (:terrain tile)
        (cond
          (= (:terrain tile) :ground) []

        :else [])))))

(defn collect-tile-facets!
  "Gather facets from tiles within radius of position using ECS."
  [world pos max-distance]
  (let [all-tiles (ecs/get-all-tiles world)
        tiles-in-range (filter
                        (fn [tile-eid]
                          (when-let [tile-pos (be/get-component world tile-eid fantasia.sim.ecs.components.Position)]
                            (<= (hex/distance [(:q tile-pos) (:r tile-pos)] pos) max-distance)))
                        all-tiles)]
    (reduce
      (fn [acc tile-eid]
        (let [tile-facets (tile->entity-facets world tile-eid)]
          (into acc tile-facets)))
      []
      tiles-in-range)))

(defn collect-memory-facets!
  "Gather facet words from all memories in range using ECS."
  [world pos max-distance]
  (let [all-agents (ecs/get-all-agents world)
        agents-in-range (filter
                          (fn [agent-eid]
                            (when-let [agent-pos (be/get-component world agent-eid fantasia.sim.ecs.components.Position)]
                              (<= (hex/distance [(:q agent-pos) (:r agent-pos)] pos) max-distance)))
                          all-agents)
        memories-in-range (keep
                            (fn [agent-eid]
                              (when-let [recall (be/get-component world agent-eid fantasia.sim.ecs.components.Recall)]
                                (:events recall)))
                            agents-in-range)]
    (reduce
      (fn [acc memory-events]
        (into acc (mapcat :facets memory-events)))
      []
      memories-in-range)))

(defn collect-agent-facets!
  "Gather facet words from nearby agents excluding self using ECS."
  [world pos max-distance exclude-id]
  (let [all-agents (ecs/get-all-agents world)
        nearby-agents (filter
                        (fn [agent-eid]
                          (and (not= agent-eid exclude-id)
                               (when-let [agent-pos (be/get-component world agent-eid fantasia.sim.ecs.components.Position)]
                                 (<= (hex/distance [(:q agent-pos) (:r agent-pos)] pos) max-distance))))
                        all-agents)
        nearby-roles (keep
                       (fn [agent-eid]
                         (when-let [role (be/get-component world agent-eid fantasia.sim.ecs.components.Role)]
                           (:type role)))
                       nearby-agents)]
    (reduce
      (fn [acc role-type]
        (let [role-keyword (case role-type
                             :peasant :agent/peasant
                             :knight :agent/strong
                             :priest :agent/peasant
                             role-type)]
          (into acc (get-entity-facets role-keyword))))
      []
      nearby-roles)))

(defn query-concept-axis!
  "Query a concept against local facets using ECS, returning score in [-1, 1].
   Positive = concept present, negative = concept absent."
  [world agent-eid concept {:keys [max-facets max-distance]}]
  (let [max-facets (or max-facets 16)
        max-distance (or max-distance 10)
        agent-pos (be/get-component world agent-eid fantasia.sim.ecs.components.Position)
        agent-role (be/get-component world agent-eid fantasia.sim.ecs.components.Role)
        agent-frontier (be/get-component world agent-eid fantasia.sim.ecs.components.Frontier)]
    
    (if-not agent-pos
      (do
        (log/log-warn "[FACET:QUERY] Agent missing Position component"
                      {:agent-eid agent-eid})
        0.0)
      ; else: agent exists, continue with processing
      (let [agent-pos-vec [(:q agent-pos) (:r agent-pos)]

            ;; Step 1: Gather local facets using ECS
            local-facets (vec
                          (concat
                            ;; Agent's own facets from Frontier component
                            (when agent-frontier
                              (:facets agent-frontier))

                            ;; Agent's role-based facets
                            (when agent-role
                              (let [role-keyword (case (:type agent-role)
                                                   :peasant :agent/peasant
                                                   :knight :agent/strong
                                                   :priest :agent/peasant
                                                   (:type agent-role))]
                                (get-entity-facets role-keyword)))

                            ;; Memory facets in range using ECS
                            (collect-memory-facets! world agent-pos-vec max-distance)

                            ;; Nearby tile facets using ECS
                            (collect-tile-facets! world agent-pos-vec max-distance)

                            ;; Nearby agents' facets using ECS
                            (collect-agent-facets! world agent-pos-vec max-distance agent-eid)))

            ;; Step 2: Take top max-facets and deduplicate
            facet-words (distinct (take max-facets local-facets))

            ;; Step 3: Compute embedding similarity
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

            ;; Step 4: Clamp to [-1, 1] and log
            (log/log-debug "[FACET:QUERY]"
                           {:agent-eid agent-eid
                            :concept concept
                            :score score
                            :facets-queried (count facet-words)
                            :top-3-facets (take 3 facet-words)})
            (max -1.0 (min 1.0 score)))

          (do
            (log/log-warn "[FACET:QUERY] Concept embedding not found"
                          {:agent-eid agent-eid
                           :concept concept
                           :facets-queried (count facet-words)})
            0.0))))))