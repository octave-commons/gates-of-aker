(ns fantasia.sim.ecs.systems.reproduction
  "ECS Reproduction system - processes entity reproduction and growth."
  (:require [brute.entity :as be]
            [fantasia.sim.ecs.components :as c]
            [fantasia.sim.ecs.core :as ecs-core]
            [fantasia.sim.hex :as hex]))

(defn- get-component-type-instance
  "Get component type from ECS core."
  [component-instance]
  (ecs-core/component-class component-instance))

(defn- can-reproduce?
  "Check if entity can reproduce based on needs and state."
  [ecs-world entity-id]
  (let [needs-instance (c/->Needs 0.6 0.7 0.7 1.0 0.8 0.6 0.5 0.5 0.5 0.6 0.5 0.5 0.5)
        needs-type (get-component-type-instance needs-instance)
        preg-instance (c/->PregnancyState false nil nil nil)
        preg-type (get-component-type-instance preg-instance)
        death-instance (c/->DeathState true nil nil)
        death-type (get-component-type-instance death-instance)
        stats-instance (c/->Stats 0.0 0.0 0.0 0.0)
        stats-type (get-component-type-instance stats-instance)
        
        needs (be/get-component ecs-world entity-id needs-type)
        pregnancy (be/get-component ecs-world entity-id preg-type)
        death-state (be/get-component ecs-world entity-id death-type)
        stats (be/get-component ecs-world entity-id stats-type)]
    
    (and needs
         (or (nil? death-state) (:alive? death-state true))
         (not (:pregnant? pregnancy false))
         (> (:food needs 0.7) 0.6)
         (> (:health needs 1.0) 0.8)
         (> (:mood needs 0.5) 0.6))))

(defn- find-nearby-partner
  "Find nearby entity for reproduction."
  [ecs-world entity-id]
  (let [pos-instance (c/->Position 0 0)
        pos-type (get-component-type-instance pos-instance)
        entity-pos (be/get-component ecs-world entity-id pos-type)
        all-entities (be/get-all-entities-with-component ecs-world pos-type)]
    (->> all-entities
         (filter #(and (not= % entity-id)
                       (can-reproduce? ecs-world %)))
         (filter #(let [partner-pos (be/get-component ecs-world % pos-type)]
                    (when (and entity-pos partner-pos)
                      (<= (hex/distance [(:q entity-pos) (:r entity-pos)]
                                       [(:q partner-pos) (:r partner-pos)]) 2))))
         first)))

(defn- start-pregnancy
  "Start pregnancy for female entity with male partner."
  [ecs-world female-id male-id tick]
  (let [preg-instance (c/->PregnancyState false nil nil nil)
        preg-type (get-component-type-instance preg-instance)
        pregnancy-duration 50 ; ticks until birth
        
        new-pregnancy (c/->PregnancyState true male-id (+ tick pregnancy-duration) tick)]
    
    (be/add-component ecs-world female-id new-pregnancy)))

(defn- give-birth
  "Give birth to new entity."
  [ecs-world mother-id father-id tick]
  (let [pos-instance (c/->Position 0 0)
        pos-type (get-component-type-instance pos-instance)
        role-instance (c/->Role :priest)
        role-type (get-component-type-instance role-instance)
        stats-instance (c/->Stats 0.0 0.0 0.0 0.0)
        stats-type (get-component-type-instance stats-instance)
        growth-instance (c/->GrowthState :infant 0.0)
        growth-type (get-component-type-instance growth-instance)
        
        mother-pos (be/get-component ecs-world mother-id pos-type)
        mother-role (be/get-component ecs-world mother-id role-type)
        father-stats (be/get-component ecs-world father-id stats-type)
        mother-stats (be/get-component ecs-world mother-id stats-type)
        
        ;; Inherit some stats from parents
        inherited-strength (/ (+ (or (:strength mother-stats) 0.5) (or (:strength father-stats) 0.5)) 2)
        inherited-fortitude (/ (+ (or (:fortitude mother-stats) 0.5) (or (:fortitude father-stats) 0.5)) 2)
        
        child-stats (c/->Stats inherited-strength inherited-fortitude 0.3 0.3)
        child-role (c/->Role (:type mother-role)) ; Inherit mother's role
        
        child-id (java.util.UUID/randomUUID)]
    
    (when mother-pos
      (let [ [_ initial-system] (ecs-core/create-agent ecs-world child-id (:q mother-pos) (:r mother-pos) (:type mother-role) {})
            world-with-stats (be/add-component initial-system child-id child-stats)
            world-with-growth (be/add-component world-with-stats child-id (c/->GrowthState :infant 0.0))
            final-world (be/add-component world-with-growth child-id child-role)]
        final-world))))

(defn- process-pregnancy
  "Process pregnancy and birth for pregnant entities."
  [ecs-world entity-id tick]
  (let [preg-instance (c/->PregnancyState false nil nil nil)
        preg-type (get-component-type-instance preg-instance)
        pregnancy (be/get-component ecs-world entity-id preg-type)]
    (when (:pregnant? pregnancy)
      (if (>= tick (:due-tick pregnancy))
        ;; Give birth
        (let [birth-world (give-birth ecs-world entity-id (:partner-id pregnancy) tick)
              clear-pregnancy (c/->PregnancyState false nil nil nil)]
          (-> birth-world
              (be/add-component entity-id clear-pregnancy)))
        ;; Continue pregnancy
        ecs-world))))

(defn- process-growth
  "Process growth and aging for entities."
  [ecs-world entity-id tick]
  (let [growth-instance (c/->GrowthState :infant 0.0)
        growth-type (get-component-type-instance growth-instance)
        growth (be/get-component ecs-world entity-id growth-type)]
    (when growth
      (let [current-stage (:age-stage growth)
            current-progress (:growth-progress growth)
            growth-rate 0.02 ; Progress per tick
            
            new-progress (min 1.0 (+ current-progress growth-rate))]
        (if (>= new-progress 1.0)
          ;; Advance to next stage
          (let [next-stage (case current-stage
                            :infant :child
                            :child :teenager  
                            :teenager :adult
                            :adult :adult)]
            (be/add-component ecs-world entity-id (c/->GrowthState next-stage 0.0)))
          ;; Continue current stage
          (be/add-component ecs-world entity-id (c/->GrowthState current-stage new-progress)))))))

(defn process-reproduction
  "Process reproduction for all entities.
   Returns [updated-world reproduction-events] tuple."
  [ecs-world tick]
  (let [role-instance (c/->Role :priest)
        role-type (get-component-type-instance role-instance)
        all-entities (be/get-all-entities-with-component ecs-world role-type)]
    
    (reduce
     (fn [{:keys [world events]} entity-id]
       (let [entity-role (be/get-component world entity-id role-type)]
         (if (#{:knight :peasant} (:type entity-role)) ; Only humans reproduce
           (let [entity-stats (be/get-component world entity-id (get-component-type-instance (c/->Stats 0.0 0.0 0.0 0.0)))
                 is-female (even? (hash (str entity-id)))] ; Simple gender determination
             (if (and is-female (can-reproduce? world entity-id))
               (if-let [partner-id (find-nearby-partner world entity-id)]
                 (let [world-with-pregnancy (start-pregnancy world entity-id partner-id tick)]
                   {:world world-with-pregnancy
                    :events (conj events {:type :reproduction-start
                                         :mother-id entity-id
                                         :father-id partner-id
                                         :tick tick})})
                 {:world world
                  :events events})
               ;; Check for existing pregnancies
               (let [world-after-pregnancy (process-pregnancy world entity-id tick)]
                 {:world world-after-pregnancy
                  :events events})))
           ;; Process growth for all entities
           (let [world-after-growth (process-growth world entity-id tick)]
             {:world world-after-growth
              :events events}))))
     {:world ecs-world :events []}
     all-entities)))

(defn process-growth-only
  "Process growth for all entities (separate from reproduction).
   Returns updated ECS world."
  [ecs-world tick]
  (let [growth-instance (c/->GrowthState :infant 0.0)
        growth-type (get-component-type-instance growth-instance)
        all-growing-entities (be/get-all-entities-with-component ecs-world growth-type)]
    (reduce
     (fn [world entity-id]
       (process-growth world entity-id tick))
     ecs-world
     all-growing-entities)))