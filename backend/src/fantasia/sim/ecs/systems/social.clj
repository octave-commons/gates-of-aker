(ns fantasia.sim.ecs.systems.social
  "ECS Social interaction system - processes conversations and relationships."
  (:require [brute.entity :as be]
            [fantasia.sim.ecs.components :as c]
            [fantasia.sim.ecs.core :as ecs-core]
            [fantasia.sim.hex :as hex]))

(defn- get-component-type-instance
  "Get component type from ECS core."
  [component-instance]
  (ecs-core/component-class component-instance))

(defn- find-adjacent-pairs
  "Find all pairs of entities within hex distance 1."
  [ecs-world]
  (let [pos-instance (c/->Position 0 0)
        pos-type (get-component-type-instance pos-instance)
        all-entities (be/get-all-entities-with-component ecs-world pos-type)]
    (for [a all-entities
          b all-entities
          :when (and (not= a b)
                     (let [pos-a (be/get-component ecs-world a pos-type)
                           pos-b (be/get-component ecs-world b pos-type)]
                       (when (and pos-a pos-b)
                         (<= (hex/distance [(:q pos-a) (:r pos-a)]
                                          [(:q pos-b) (:r pos-b)]) 1))))]
      [a b])))

(defn- create-conversation-packet
  "Create conversation packet from speaker entity."
  [ecs-world speaker-id]
  (let [role-instance (c/->Role :priest)
        role-type (get-component-type-instance role-instance)
        needs-instance (c/->Needs 0.6 0.7 0.7 1.0 0.8 0.6 0.5 0.5 0.5 0.6 0.5 0.5 0.5)
        needs-type (get-component-type-instance needs-instance)
        pos-instance (c/->Position 0 0)
        pos-type (get-component-type-instance pos-instance)
        
        speaker-role (be/get-component ecs-world speaker-id role-type)
        speaker-needs (be/get-component ecs-world speaker-id needs-type)
        speaker-pos (be/get-component ecs-world speaker-id pos-type)
        
        warmth (or (:warmth speaker-needs) 0.5)
        
        ;; Simple packet creation based on agent state
        base-facets (cond-> [:social]
                      (< warmth 0.25) (conj :fear)
                      (= (:type speaker-role) :priest) (conj :wisdom))
        
        packet {:intent (cond
                         (< warmth 0.25) :warn
                         (= (:type speaker-role) :priest) :teach
                         :else :chatter)
                :facets base-facets
                :tone {:charisma 0.2
                       :urgency (if (< warmth 0.25) 0.6 0.2)}}]
    packet))

(defn- update-relationship
  "Update relationship between two entities after interaction."
  [ecs-world entity-id-1 entity-id-2 interaction-type]
  (let [rel-instance (c/->Relationships {})
        rel-type (get-component-type-instance rel-instance)
        
        ;; Get or create relationships for both entities
        rel-1 (or (be/get-component ecs-world entity-id-1 rel-type) {})
        rel-2 (or (be/get-component ecs-world entity-id-2 rel-type) {})
        
        ;; Update affinity based on interaction type
        affinity-change (case interaction-type
                          :positive 0.05
                          :negative -0.03
                          :neutral 0.01)
        
        new-rel-1 (update rel-1 entity-id-2 
                          (fn [rel] 
                            (let [current (or (:affinity rel) 0.5)
                                  new-affinity (max 0.0 (min 1.0 (+ current affinity-change)))]
                              (assoc rel :affinity new-affinity :last-interaction (:tick ecs-world)))))
        
        new-rel-2 (update rel-2 entity-id-1
                          (fn [rel]
                            (let [current (or (:affinity rel) 0.5)
                                  new-affinity (max 0.0 (min 1.0 (+ current affinity-change)))]
                              (assoc rel :affinity new-affinity :last-interaction (:tick ecs-world)))))]
    
    (-> ecs-world
        (be/add-component entity-id-1 new-rel-1)
        (be/add-component entity-id-2 new-rel-2))))

(defn- process-single-interaction
  "Process one social interaction between two entities."
  [ecs-world speaker-id listener-id tick]
  (let [packet (create-conversation-packet ecs-world speaker-id)
        
        ;; Update listener's frontier (simple version)
        frontier-instance (c/->Frontier {})
        frontier-type (get-component-type-instance frontier-instance)
        listener-frontier (or (be/get-component ecs-world listener-id frontier-type) {})
        
        ;; Apply packet facets to listener frontier
        current-facets (:facets listener-frontier)
        new-facets (reduce (fn [acc facet]
                             (let [current-val (get acc facet 0.0)
                                   new-val (+ current-val 0.1)]
                                   (assoc acc facet (cond
                                                   (< new-val 0.0) 0.0
                                                   (> new-val 1.0) 1.0
                                                   :else new-val))))
                           current-facets
                           (:facets packet))
        
        updated-frontier (assoc listener-frontier :facets new-facets)
        
        ;; Update social state cooldowns
        social-instance (c/->SocialState nil 0 0)
        social-type (get-component-type-instance social-instance)
        speaker-social (c/->SocialState nil tick (+ tick 5))
        listener-social (c/->SocialState speaker-id tick (+ tick 3))
        
        ;; Determine interaction type based on roles
        role-instance (c/->Role :priest)
        role-type (get-component-type-instance role-instance)
        speaker-role (be/get-component ecs-world speaker-id role-type)
        interaction-type (if (= (:type speaker-role) :priest) :positive :neutral)]
    
    (-> ecs-world
        (be/add-component listener-id updated-frontier)
        (be/add-component speaker-id speaker-social)
        (be/add-component listener-id listener-social)
        (update-relationship speaker-id listener-id interaction-type))))

(defn process
  "Process social interactions for all adjacent entity pairs.
   Returns [updated-world social-interaction-events] tuple."
  [ecs-world tick]
    (let [pairs (find-adjacent-pairs ecs-world)
        social-instance (c/->SocialState nil 0 0)
        social-type (get-component-type-instance social-instance)
        current-tick tick]
    (reduce
     (fn [{:keys [world events]} [speaker-id listener-id]]
       ;; Check if both entities are ready for social interaction
       (let [speaker-social (be/get-component world speaker-id social-type)
             listener-social (be/get-component world listener-id social-type)
             speaker-cooldown (or (:interaction-cooldown speaker-social) 0)
             listener-cooldown (or (:interaction-cooldown listener-social) 0)]
         (if (and (>= current-tick speaker-cooldown) (>= current-tick listener-cooldown))
           (let [updated-world (process-single-interaction world speaker-id listener-id tick)]
             {:world updated-world
              :events (conj events {:type :social-interaction
                                   :speaker-id speaker-id
                                   :listener-id listener-id
                                   :tick tick})})
           {:world world
            :events events})))
     {:world ecs-world :events []}
     pairs)))