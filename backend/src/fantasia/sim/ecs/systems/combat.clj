(ns fantasia.sim.ecs.systems.combat
  "ECS Combat system - processes combat for all agents."
  (:require [brute.entity :as be]
            [fantasia.sim.ecs.components :as c]
            [fantasia.sim.ecs.core :as ecs-core]
            [fantasia.sim.hex :as hex]
            [fantasia.sim.constants :as const]))

(def ^:private attack-range 1)
(def ^:private deer-sight-range 6)
(def ^:private player-sight-range 4)
(def ^:private wolf-sight-range const/wolf-sight-range)

(def ^:private role-damage
  {:wolf 0.16
   :bear 0.22
   :deer 0.0
   :default 0.12})

(defn- get-component-type-instance
  "Get component type from ECS core."
  [component-instance]
  (ecs-core/component-class component-instance))

(defn- defense-reduction
  "Calculate defense reduction from agent's fortitude stat."
  [ecs-world entity-id]
  (let [stats-instance (c/->Stats 0.0 0.0 0.0 0.0)
        stats-type (get-component-type-instance stats-instance)
        stats (be/get-component ecs-world entity-id stats-type)
        fortitude (or (:fortitude stats) 0.0)
        multiplier (min 1.0 (max 0.0 fortitude))]
    (* multiplier const/max-defense-multiplier)))

(defn- alive-entity?
  "Check if entity is alive."
  [ecs-world entity-id]
  (let [death-instance (c/->DeathState true nil nil)
        death-type (get-component-type-instance death-instance)
        death-state (be/get-component ecs-world entity-id death-type)]
    (or (nil? death-state) (:alive? death-state true))))

(defn- in-range?
  "Check if target entity is within range."
  [ecs-world attacker-id target-id range]
  (let [pos-instance (c/->Position 0 0)
        pos-type (get-component-type-instance pos-instance)
        attacker-pos (be/get-component ecs-world attacker-id pos-type)
        target-pos (be/get-component ecs-world target-id pos-type)]
    (when (and attacker-pos target-pos)
      (<= (hex/distance [(:q attacker-pos) (:r attacker-pos)]
                        [(:q target-pos) (:r target-pos)]) range))))

(defn- find-nearest-agent
  "Find nearest agent matching predicate within range."
  [ecs-world attacker-id predicate range]
  (let [pos-instance (c/->Position 0 0)
        pos-type (get-component-type-instance pos-instance)
        role-instance (c/->Role :priest)
        role-type (get-component-type-instance role-instance)
        attacker-pos (be/get-component ecs-world attacker-id pos-type)
        all-agents (be/get-all-entities-with-component ecs-world role-type)
        living-agents (filter #(alive-entity? ecs-world %) all-agents)
        matching-agents (filter predicate living-agents)]
    (->> matching-agents
         (filter #(in-range? ecs-world attacker-id % range))
         (sort-by (fn [target-id]
                    (let [target-pos (be/get-component ecs-world target-id pos-type)]
                      (hex/distance [(:q attacker-pos) (:r attacker-pos)]
                                    [(:q target-pos) (:r target-pos)]))))
         first)))

(defn- wolf-target-predicate
  "Predicate for wolf targets (deer first, then players)."
  [ecs-world target-id]
  (let [role-instance (c/->Role :priest)
        role-type (get-component-type-instance role-instance)
        target-role (be/get-component ecs-world target-id role-type)]
    (or (= (:type target-role) :deer)
        (= (:type target-role) :knight)
        (= (:type target-role) :peasant))))

(defn- player-target-predicate
  "Predicate for player targets (wolves)."
  [ecs-world target-id]
  (let [role-instance (c/->Role :priest)
        role-type (get-component-type-instance role-instance)
        target-role (be/get-component ecs-world target-id role-type)]
    (= (:type target-role) :wolf)))

(defn- select-combat-target
  "Select combat target for entity based on role."
  [ecs-world entity-id]
  (let [role-instance (c/->Role :priest)
        role-type (get-component-type-instance role-instance)
        role (be/get-component ecs-world entity-id role-type)]
    (case (:type role)
      :wolf (or (find-nearest-agent ecs-world entity-id 
                                   #(and (alive-entity? ecs-world %)
                                         (let [r-instance (c/->Role :priest)
                                               r-type (get-component-type-instance r-instance)
                                               role-comp (be/get-component ecs-world % r-type)]
                                           (= (:type role-comp) :deer)))
                                   wolf-sight-range)
                (find-nearest-agent ecs-world entity-id 
                                   #(and (alive-entity? ecs-world %)
                                         (let [r-instance (c/->Role :priest)
                                               r-type (get-component-type-instance r-instance)
                                               role-comp (be/get-component ecs-world % r-type)]
                                           (or (= (:type role-comp) :knight)
                                               (= (:type role-comp) :peasant))))
                                   player-sight-range))
      :deer nil
      (when (#{:knight :peasant} (:type role))
        (find-nearest-agent ecs-world entity-id 
                           #(and (alive-entity? ecs-world %)
                                 (let [r-instance (c/->Role :priest)
                                       r-type (get-component-type-instance r-instance)
                                       role-comp (be/get-component ecs-world % r-type)]
                                   (= (:type role-comp) :wolf)))
                           attack-range)))))

(defn- damage-for-role
  "Get damage value for role."
  [role-type]
  (double (get role-damage role-type (:default role-damage))))

(defn- apply-combat-attack
  "Apply attack from attacker to target."
  [ecs-world attacker-id target-id tick]
  (let [role-instance (c/->Role :priest)
        role-type (get-component-type-instance role-instance)
        needs-instance (c/->Needs 0.6 0.7 0.7 1.0 0.8 0.6 0.5 0.5 0.5 0.6 0.5 0.5 0.5)
        needs-type (get-component-type-instance needs-instance)
        
        attacker-role (be/get-component ecs-world attacker-id role-type)
        target-role (be/get-component ecs-world target-id role-type)
        
        damage (damage-for-role (:type attacker-role))
        reduction (if (#{:knight :peasant} (:type target-role))
                   (defense-reduction ecs-world target-id)
                   0.0)
        reduced-damage (* damage (- 1.0 reduction))
        
        target-needs (be/get-component ecs-world target-id needs-type)
        current-health (double (or (:health target-needs) 1.0))
        new-health (max 0.0 (- current-health reduced-damage))
        
        new-needs (assoc target-needs :health new-health)
        
        attacker-needs (be/get-component ecs-world attacker-id needs-type)
        attacker-needs' (if (= (:type attacker-role) :wolf)
                        (update attacker-needs :food + const/wolf-attack-food-gain)
                        attacker-needs)]
    
    (if (<= new-health 0.0)
      ;; Target died
      (let [death-state (c/->DeathState false :combat tick)
            death-type (get-component-type-instance death-state)
            ecs-world' (-> ecs-world
                           (be/add-component target-id death-state)
                           (be/add-component target-id new-needs)
                           (be/add-component attacker-id attacker-needs'))]
        {:world ecs-world'
         :event {:type :hunt-kill
                 :attacker-id attacker-id
                 :attacker-role (:type attacker-role)
                 :target-id target-id
                 :target-role (:type target-role)
                 :tick tick}})
      ;; Target survived
      (let [ecs-world' (-> ecs-world
                           (be/add-component target-id new-needs)
                           (be/add-component attacker-id attacker-needs'))]
        {:world ecs-world'
         :event {:type :hunt-attack
                 :attacker-id attacker-id
                 :attacker-role (:type attacker-role)
                 :target-id target-id
                 :target-role (:type target-role)
                 :damage reduced-damage
                 :original-damage damage
                 :defense-reduction reduction
                 :tick tick}}))))

(defn process
  "Process combat for all combat-capable entities.
   Returns [updated-world combat-events] tuple."
  [ecs-world tick]
  (let [role-instance (c/->Role :priest)
        role-type (get-component-type-instance role-instance)
        combat-capable-roles #{:wolf :bear :knight :peasant}
        all-agents (be/get-all-entities-with-component ecs-world role-type)
        combat-entities (filter #(and (alive-entity? ecs-world %)
                                     (let [role (be/get-component ecs-world % role-type)]
                                       (combat-capable-roles (:type role))))
                                all-agents)]
    (reduce
     (fn [{:keys [world events]} attacker-id]
       (if-let [target-id (select-combat-target world attacker-id)]
         (if (in-range? world attacker-id target-id attack-range)
           ;; In range - attack
           (let [result (apply-combat-attack world attacker-id target-id tick)]
             {:world (:world result)
              :events (conj events (:event result))})
           ;; Not in range - start hunt
           {:world world
            :events (conj events 
                          {:type :hunt-start
                           :attacker-id attacker-id
                           :target-id target-id
                           :tick tick})})
         ;; No target
         {:world world
          :events events}))
     {:world ecs-world :events []}
     combat-entities)))