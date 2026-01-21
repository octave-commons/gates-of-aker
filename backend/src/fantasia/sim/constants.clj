(ns fantasia.sim.constants)

 ;; Agent needs constants
 (def ^:const campfire-radius 2)
 (def ^:const warmth-bonus-campfire 0.04)
 (def ^:const warmth-bonus-house 0.02)
 (def ^:const mood-env-radius 2)
 (def ^:const mood-bonus-house 0.003)
 (def ^:const mood-bonus-temple 0.006)
 (def ^:const mood-bonus-school 0.004)
 (def ^:const mood-bonus-library 0.005)
 (def ^:const mood-bonus-trees 0.002)
(def ^:const base-warmth-decay 0.004)
(def ^:const cold-warmth-decay-factor 0.012)
(def ^:const heat-damage-threshold 0.85)
(def ^:const heat-damage-per-tick 0.05)
(def ^:const base-food-decay-awake 0.0004)
(def ^:const base-food-decay-asleep 0.0001)
(def ^:const base-sleep-decay 0.0032)
(def ^:const base-rest-decay 0.0024)
(def ^:const base-rest-recovery 0.014)
(def ^:const house-rest-bonus 0.006)
(def ^:const base-social-decay 0.0024)

;; Wildlife needs constants
(def ^:const deer-food-decay 0.003)
(def ^:const wolf-food-decay 0.0006)
(def ^:const deer-forage-range 6)
(def ^:const wolf-hunt-range 8)
(def ^:const wildlife-starvation-health-decay 0.05)
(def ^:const fawn-growth-ticks 300)
(def ^:const pup-growth-ticks 350)
(def ^:const baby-stat-multiplier 0.4)
(def ^:const baby-vulnerability-multiplier 2.0)

;; Reproduction constants
(def ^:const reproduction-affinity-threshold 0.75)

;; Facet constants
(def ^:const max-active-facets 24)
(def ^:const default-decay-rate 0.92)
(def ^:const default-drop-threshold 0.02)
(def ^:const default-spread-gain 0.50)
(def ^:const default-max-hops 2)

;; Pathfinding constants
(def ^:const pathfinding-max-steps 1000)
(def ^:const road-move-cost 0.35)

;; Movement constants
(def ^:const base-move-steps 1)
(def ^:const dex-move-step-multiplier 2.0)
(def ^:const road-dex-move-step-multiplier 3.0)
(def ^:const road-move-step-bonus 1)

;; Job constants
(def ^:const job-progress-required 1.0)
(def ^:const log-drop-count 3)
(def ^:const house-build-requirement 3)
(def ^:const wall-build-requirement 1)

;; Structure constants
(def ^:const max-structure-level 3)
(def ^:const default-stockpile-max-qty 120)

;; Tick constants
(def ^:const default-recent-max 50)
(def ^:const default-trace-max 200)
