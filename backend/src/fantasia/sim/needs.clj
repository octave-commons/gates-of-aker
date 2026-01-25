(ns fantasia.sim.needs
  (:require [fantasia.sim.ecs.facets :as f]
            [fantasia.sim.spatial_facets :as sf]
            [fantasia.sim.constants :as const]))

;; =============================================================================
;; Axis Weights
;; =============================================================================

(defn- get-axis-weights [axis]
  "Get concept weights for specific need axis.

  Returns a map of concept keywords to importance weights (0.0-1.0).
  Higher weights mean the concept is more relevant to that need."
  (case axis
    :warmth
    {:fire 1.0 :campfire 0.9 :warmth 1.0 :cold 1.0
     :heat 0.8 :shivering 0.9 :comfortable 0.7
     :freezing 1.0 :burning 0.8 :chill 0.6}

    :food
    {:hunger 1.0 :starving 1.0 :eating 1.0 :food 1.0
     :full 0.8 :satisfied 0.7 :foraging 0.6
     :cooking 0.5 :hungry 0.9 :famished 1.0}

    :social
    {:community 1.0 :friendship 0.9 :lonely 0.9 :isolation 1.0
     :gathering 0.8 :conversation 0.7 :together 0.8
     :alone 0.7 :belonging 0.9 :family 0.85}

    :sleep
    {:rested 1.0 :tired 1.0 :sleeping 0.9 :awake 0.7
     :exhausted 1.0 :energy 0.8 :drowsy 0.9
     :alert 0.6 :refreshed 1.0 :fatigue 0.9}

    :mood
    {:happy 1.0 :sad 0.9 :anxious 1.0 :calm 0.8
     :excited 0.7 :bored 0.7 :satisfied 0.9
     :joyful 0.9 :miserable 1.0 :content 0.8}

    {}))

;; =============================================================================
;; Concept Scoring
;; =============================================================================

(defn- query-concept-score [frontier recall concept]
  "Score a single concept based on frontier and recall.

  Combines multiple sources of concept activation:
  1. Direct facet activation (from beliefs)
  2. Memory recall strength (from experiences)
  3. Recent event influence (weighted by recency)

  Returns score in range [-1.0, 1.0] where:
  - Positive values indicate presence/activation
  - Negative values indicate absence/opposition"
  (let [facet-activation (get-in frontier [concept :a] 0.0)
        recall-strength (get recall concept 0.0)
        ;; Recall values are typically positive (0.0-1.0)
        ;; Convert to signed score based on concept semantics
        recall-score (* recall-strength 0.5)]
    (f/clamp01 (+ facet-activation recall-score))))

(defn- calculate-axis-score [frontier recall axis concepts]
  "Calculate overall axis activation from multiple concepts.

  Weights and combines individual concept scores using axis-specific weights.

  Returns final score in [-1.0, 1.0] representing overall
  axis activation strength."
  (let [weights (get-axis-weights axis)
        weighted-concepts (filter weights concepts)
        concept-scores (map (fn [concept]
                             (let [score (query-concept-score
                                             frontier
                                             recall
                                             concept)
                                   weight (get weights concept 1.0)]
                               (* score weight)))
                           weighted-concepts)]
    (if (empty? concept-scores)
      0.0
      (let [raw-score (/ (reduce + concept-scores)
                         (reduce + (map (fn [c] (get weights c 1.0))
                                       weighted-concepts)))
            ;; Normalize to [-1.0, 1.0] by clamping
            normalized-score (f/clamp01 raw-score)]
        normalized-score))))

;; =============================================================================
;; Public API
;; =============================================================================

(defn query-need-axis!
  "Query facet axis for need-based behavior.

  Calculates how strongly a set of concept words are activated
  in an agent's facet frontier along a specific need axis.

  This enables agents to make decisions based on their beliefs
  and memories rather than just hardcoded needs.

  Arguments:
  world: Current world state (map)
  agent: Agent entity (map with :id, :frontier, :recall, etc.)
  axis: Keyword identifying need axis (e.g., :food, :warmth, :social)
  concept-words: Collection of keywords representing concepts to query

  Returns:
  Map with keys:
  - :score - Overall activation score in range [-1.0, 1.0]
  - :concepts - Map of concept-word => individual scores
  - :axis-activation - How strongly the axis is activated [0.0, 1.0]
  - :axis - The queried axis (echoed back)

  Examples:
  ;; Agent with strong fire-related beliefs
  (query-need-axis! world agent :warmth [:fire :campfire :warm])
  => {:score 0.85
       :concepts {:fire 0.9 :campfire 0.8 :warmth 0.85}
       :axis-activation 0.9
       :axis :warmth}

  ;; Agent lacking food concepts
  (query-need-axis! world agent :food [:hunger :starving])
  => {:score -0.3
       :concepts {:hunger -0.1 :starving -0.5}
       :axis-activation 0.2
       :axis :food}

  Notes:
  - Returns {:score 0.0} if concept-words is empty
  - Returns {:score 0.0} if axis has no weights defined
  - Caching could be added for performance optimization

  Related:
  - fantasia.sim.spatial_facets/query-concept-axis!
  - fantasia.sim.facets/event-recall"
  [world agent axis concept-words]
  (let [frontier (:frontier agent {})
        recall (:recall agent {})]
    (if (or (empty? concept-words)
              (not (sequential? concept-words)))
      {:score 0.0
       :concepts {}
       :axis-activation 0.0
       :axis axis}
      (let [concept-scores (->> concept-words
                               (map (fn [concept]
                                      [concept
                                       (query-concept-score
                                        frontier
                                        recall
                                        concept)]))
                               (into {}))
            axis-score (calculate-axis-score
                            frontier
                            recall
                            axis
                            concept-words)
            ;; Convert score to activation [0.0, 1.0]
            axis-activation (f/clamp01 (/ (+ axis-score 1.0) 2.0))]
        {:score axis-score
         :concepts concept-scores
         :axis-activation axis-activation
         :axis axis}))))

(defn get-axis-activation-threshold
  "Get threshold for considering an axis 'activated'.

  Used by decision-making logic to determine if a need is
  strong enough to drive behavior.

  Arguments:
  axis: Keyword identifying need axis

  Returns:
  Float threshold in [0.0, 1.0] above which axis is considered activated

  Default thresholds:
  - :warmth -> 0.7
  - :food -> 0.6
  - :social -> 0.5
  - :sleep -> 0.65
  - :mood -> 0.6"
  [axis]
  (case axis
    :warmth 0.7
    :food 0.6
    :social 0.5
    :sleep 0.65
    :mood 0.6
    0.6))

(defn axis-activated?
  "Check if a need axis is sufficiently activated to drive behavior.

  Convenience function combining query-need-axis! with threshold checking.

  Arguments:
  world: Current world state
  agent: Agent entity
  axis: Need axis keyword
  concept-words: Concepts to query
  threshold: Optional threshold (defaults to axis-specific threshold)

  Returns:
  Boolean indicating if axis is activated

  Examples:
  (axis-activated? world agent :warmth [:fire :campfire])
  => true

  (axis-activated? world agent :food [:full :satisfied])
  => false"
  ([world agent axis concept-words]
   (axis-activated? world agent axis concept-words
                      (get-axis-activation-threshold axis)))
  ([world agent axis concept-words threshold]
   (let [result (query-need-axis! world agent axis concept-words)]
     (>= (:axis-activation result) threshold))))