(ns fantasia.sim.constants)

;; Agent needs constants
(def ^:const campfire-radius 2)
(def ^:const warmth-bonus-campfire 0.04)
(def ^:const warmth-bonus-house 0.02)
(def ^:const base-warmth-decay 0.004)
(def ^:const cold-warmth-decay-factor 0.012)
(def ^:const base-food-decay-awake 0.0008)
(def ^:const base-food-decay-asleep 0.0002)
(def ^:const base-sleep-decay 0.0032)

;; Facet constants
(def ^:const max-active-facets 24)
(def ^:const default-decay-rate 0.92)
(def ^:const default-drop-threshold 0.02)
(def ^:const default-spread-gain 0.50)
(def ^:const default-max-hops 2)

;; Pathfinding constants
(def ^:const pathfinding-max-steps 1000)

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
