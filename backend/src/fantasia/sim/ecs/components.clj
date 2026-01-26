(ns fantasia.sim.ecs.components
  (:import [java.util UUID]))

(defrecord Position [q r])

(defrecord TileIndex [q r])

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

(defrecord Inventory [wood food])

(defrecord AgentInfo [id name])

(defrecord AgentStatus [alive? asleep? idle? cause-of-death])

(defrecord PersonalInventory [wood food items])

(defrecord TileResources [items last-fruit-drop next-fruit-drop-tick])

(defrecord StructureState [level health max-health owner-id])

(defrecord CampfireState [warmth-range active? last-fueled-tick])

(defrecord ShrineState [mouthpiece-agent-id])

(defrecord JobQueue [jobs pending-jobs assigned-jobs])

(defrecord WorldItem [resource qty pos created-at])

;; Combat components
(defrecord CombatStats [damage reduction range sight-range])
(defrecord CombatTarget [target-id target-role])
(defrecord CombatState [in-combat? last-attack-tick attack-cooldown])

;; Social interaction components  
(defrecord SocialStats [charisma influence])
(defrecord SocialState [last-interaction-tick interaction-cooldown current-partner-id])
(defrecord Relationships [relations]) ;; Map of agent-id -> {:affinity 0.5 :last-interaction 123}

;; Mortality components
(defrecord DeathState [alive? cause-of-death death-tick])
(defrecord Stats [strength fortitude charisma intelligence]) ;; Agent base stats

;; Reproduction components
(defrecord ReproductionStats [fertility libido pregnancy-duration])
(defrecord PregnancyState [pregnant? partner-id due-tick conception-tick])
(defrecord GrowthState [age-stage growth-progress])

;; Memory and event components
(defrecord Memory [id type location created-at strength entity-id facets])
(defrecord MemoryState [memory-capacity decay-rate last-memory-tick])

;; Event components
(defrecord EventWitness [event-id witness-score witness-tick])
(defrecord EventTrace [trace-id tick listener speaker packet seeded spread event-recall claim-activation mention])

;; Faction and institution components
(defrecord Faction [name relations])
(defrecord Institution [type influence-members broadcasting-range])

;; Vision and awareness components
(defrecord Vision [radius visibility-map last-visibility-update])
(defrecord Awareness [known-agents known-structures last-awareness-update])