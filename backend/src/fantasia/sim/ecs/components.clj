(ns fantasia.sim.ecs.components
  (:import [java.util UUID]))

(defrecord Position [q r])

(defrecord Needs [warmth food sleep])

(defrecord Inventory [wood food])

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