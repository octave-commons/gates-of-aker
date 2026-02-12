(ns fantasia.sim.ecs.systems.needs
   (:require [brute.entity :as be]
             [fantasia.sim.ecs.components :as c]
             [fantasia.sim.ecs.facets :as f]
             [fantasia.sim.ecs.spatial :as sf]
             [fantasia.dev.logging :as log]))

;; =============================================================================
;; Axis Weights
;; =============================================================================

(defn- get-axis-weights [axis]
  "Get concept weights for specific need axis."
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
;; ECS-based Concept Scoring
;; =============================================================================

(defn- get-agent-facets [ecs-world agent-id]
  "Get agent's facet components from ECS world."
  (let [frontier-type (be/get-component-type (c/->Frontier {}))
        recall-type (be/get-component-type (c/->Recall {}))
        frontier (or (be/get-component ecs-world agent-id frontier-type) {})
        recall (or (be/get-component ecs-world agent-id recall-type) {})]
    {:frontier frontier
     :recall recall}))

(defn- query-concept-score [frontier recall concept]
  "Score a single concept based on frontier and recall."
  (let [facet-activation (or (get-in frontier [:facets concept :a]) 0.0)
        recall-events (:events recall {})
        recall-strength (get recall-events concept 0.0)
        recall-score (* recall-strength 0.5)]
    (f/clamp01 (+ facet-activation recall-score))))

(defn- calculate-axis-score [frontier recall axis concepts]
  "Calculate overall axis activation from multiple concepts."
  (let [weights (get-axis-weights axis)
        weighted-concepts (filter weights concepts)]
    (if (empty? weighted-concepts)
      0.0
      (let [concept-scores (map (fn [concept]
                                   (let [score (query-concept-score
                                                   frontier
                                                   recall
                                                   concept)
                                         weight (get weights concept 1.0)]
                                     (* score weight)))
                                 weighted-concepts)
            total-weights (reduce + (map (fn [c] (get weights c 1.0)) weighted-concepts))]
        (f/clamp01 (/ (reduce + concept-scores) total-weights))))))

;; =============================================================================
;; ECS Public API
;; =============================================================================

(defn query-need-axis!
  "Query facet axis for need-based behavior using ECS.

  Arguments:
  ecs-world: ECS world state
  agent-id: Agent entity ID
  axis: Need axis keyword (e.g., :food, :warmth, :social)
  concept-words: Collection of concepts to query

  Returns:
  Map with :score, :concepts, :axis-activation, :axis"
  [ecs-world agent-id axis concept-words]
  (let [{:keys [frontier recall]} (get-agent-facets ecs-world agent-id)]
    (if (or (empty? concept-words)
              (not (sequential? concept-words)))
      {:score 0.0
       :concepts {}
       :axis-activation 0.0
       :axis axis}
      (let [concept-scores (into {}
                                  (map (fn [concept]
                                         [concept
                                          (query-concept-score
                                           frontier
                                           recall
                                           concept)])
                                       concept-words))
            axis-score (calculate-axis-score
                            frontier
                            recall
                            axis
                            concept-words)
            axis-activation (f/clamp01 (/ (+ axis-score 1.0) 2.0))]
        {:score axis-score
         :concepts concept-scores
         :axis-activation axis-activation
         :axis axis}))))

(defn get-axis-activation-threshold
  "Get threshold for considering an axis 'activated'."
  [axis]
  (case axis
    :warmth 0.7
    :food 0.6
    :social 0.5
    :sleep 0.65
    :mood 0.6
    0.6))

(defn axis-activated?
  "Check if a need axis is sufficiently activated to drive behavior."
  ([ecs-world agent-id axis concept-words]
   (axis-activated? ecs-world agent-id axis concept-words
                      (get-axis-activation-threshold axis)))
  ([ecs-world agent-id axis concept-words threshold]
   (let [result (query-need-axis! ecs-world agent-id axis concept-words)]
     (>= (:axis-activation result) threshold))))

(defn process
  "Process needs system - facet-based queries for job decisions."
  [ecs-world global-state]
  (let [needs-type (be/get-component-type (c/->Needs 0.6 0.7 0.7 1.0 0.8 0.6 0.5 0.5 0.5 0.6 0.5 0.5 0.5))
        agent-ids (be/get-all-entities-with-component ecs-world needs-type)]
    (reduce (fn [acc agent-id]
              ;; For now, just log when agents have strong need activations
              ;; Integration with job system comes later
              (let [food-activation (query-need-axis! acc agent-id :food [:hunger :starving])
                    warmth-activation (query-need-axis! acc agent-id :warmth [:cold :freezing])]
                (when (> (:axis-activation food-activation) 0.7)
                  (log/log-info "[NEED:AXIS-ACTIVATED]" 
                               {:agent-id agent-id 
                                :axis :food 
                                :activation (:axis-activation food-activation)}))
                (when (> (:axis-activation warmth-activation) 0.7)
                  (log/log-info "[NEED:AXIS-ACTIVATED]" 
                               {:agent-id agent-id 
                                :axis :warmth 
                                :activation (:axis-activation warmth-activation)}))
                acc))
            ecs-world
            agent-ids)))