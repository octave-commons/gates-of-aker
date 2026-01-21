(ns fantasia.sim.reproduction
  (:require [fantasia.sim.houses :as houses]
            [fantasia.sim.tick.initial :as initial]))

(defn can-reproduce?
  "Check if two agents can reproduce together."
  [world agent1 agent2]
  (let [mood1 (get-in agent1 [:needs :mood] 0.5)
        mood2 (get-in agent2 [:needs :mood] 0.5)
        pos1 (:pos agent1)
        pos2 (:pos agent2)
        same-pos? (= pos1 pos2)
        both-alive? (and (get-in agent1 [:status :alive?] true)
                       (get-in agent2 [:status :alive?] true))
        both-player? (and (= (:faction agent1) :player)
                       (= (:faction agent2) :player))
        both-happy? (and (> mood1 0.8) (> mood2 0.8))
        carrying-child? (get-in agent1 [:carrying-child] nil)]
    (and both-alive?
         both-player?
         both-happy?
         (not carrying-child?)
         same-pos?)))

(defn- determine-child-role
  "Determine child role based on parents (with bias toward higher status roles)."
  [agent1 agent2]
  (let [role1 (:role agent1)
        role2 (:role agent2)
        roles [role1 role2]
        has-priest? (some #(= % :priest) roles)
        has-knight? (some #(= % :knight) roles)]
    (cond
      (and has-priest? has-knight?) :priest
      has-priest? :knight
      has-knight? :knight
      :else :peasant)))

(defn create-child-agent
  "Create a new child agent with basic attributes and parent relationships."
  [world agent1 agent2 tick]
  (let [child-role (determine-child-role agent1 agent2)
        parent-ids [(:id agent1) (:id agent2)]
        next-id (or (:next-agent-id world) 0)
        pos (:pos agent1)
        child-agent {:id next-id
                     :name (initial/agent-name next-id child-role)
                     :pos pos
                     :role child-role
                     :faction :player
                     :stats {:strength 0.3 :dexterity 0.3 :fortitude 0.3 :charisma 0.3}
                     :needs {:mood 0.7 :social 0.6 :food 0.7 :water 0.7 :rest 0.7 :health 1.0 :security 0.5 :warmth 0.6}
                     :need-thresholds {:food-starve 0.0 :food-hungry 0.3 :food-satisfied 0.8
                                       :water-dehydrate 0.0 :water-thirsty 0.3 :water-satisfied 0.8
                                       :rest-collapse 0.0 :rest-tired 0.3 :rest-rested 0.8
                                       :health-critical 0.0 :health-low 0.4 :health-stable 0.8
                                       :security-panic 0.0 :security-unsettled 0.4 :security-safe 0.9
                                       :mood-depressed 0.0 :mood-low 0.3 :mood-uplifted 0.8
                                       :social-lonely 0.0 :social-low 0.3 :social-sated 0.8
                                       :warmth-freeze 0.0 :warmth-cold 0.3 :warmth-comfort 0.8}
                     :inventories {:personal {:wood 0 :food 0}
                                   :hauling {}
                                   :equipment {}}
                     :status {:alive? true :asleep? false :idle? true}
                     :inventory {:wood 0 :food 0}
                     :relationships {}
                     :last-social-tick nil
                     :last-social-thought nil
                     :frontier {}
                     :recall {}
                     :events []
                     :child-stage :infant
                     :ticks-as-child tick
                     :parent-ids parent-ids
                     :children-ids []
                     :carrying-child nil
                     :house-id nil}]
    {:child-agent child-agent
     :next-agent-id (inc next-id)}))

(defn advance-child-growth
  "Advance child growth stage based on ticks since birth."
  [agent tick]
  (let [ticks-since-birth (- tick (:ticks-as-child agent))
        current-stage (:child-stage agent)]
    (cond
      (< ticks-since-birth 50) [agent current-stage false] 
      (and (<= 50 ticks-since-birth) (< ticks-since-birth 200)) 
        [(-> agent
             (assoc :child-stage :child)
             (assoc-in [:status :idle?] true)) :child true]
      (>= ticks-since-birth 200)
        [(-> agent
             (assoc :child-stage :adult)
             (assoc-in [:status :idle?] false)) :adult true])))
