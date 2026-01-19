(ns fantasia.sim.ecs.components
  (:import [java.util UUID]))

(defrecord Position [q r])

(defrecord Needs
  [warmth food sleep
   water health security mood
   hunger-axis    ;; Maps to :food for backward compat
   security-axis  ;; New axis for danger/safety queries
   rest-axis      ;; Maps to :sleep for backward compat
   warmth-axis   ;; Maps to :warmth for backward compat
   health-axis    ;; New axis
   mood-axis])  ;; New axis

(defrecord Role [type])

(defrecord Frontier [facets])

(defrecord Recall [events])

(defrecord JobAssignment [job-id progress])

(defrecord Path [waypoints current-index])

(defrecord Tile [terrain biome structure resource])

(defrecord Stockpile [contents])

(defrecord WallGhost [owner-id])

(defrecord Agent [name])

(defrecord TileIndex [entity-id])

(defrecord Inventory [wood food])