(ns fantasia.sim.reproduction
   (:require [fantasia.sim.houses :as houses]
             [fantasia.sim.hex :as hex]
             [fantasia.sim.tick.initial :as initial]
             [fantasia.sim.constants :as const]))

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

(defn determine-child-role
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
                       :house-id nil
                       :voice (initial/generate-voice next-id)}]
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

(defn can-wildlife-reproduce?
   "Check if wildlife agent can reproduce based on food/mood."
   [world agent]
   (let [role (:role agent)
        food (get-in agent [:needs :food] 0.7)
         mood (get-in agent [:needs :mood] 0.5)
         parent (get-in agent [:carrying-child] nil)
         alive? (get-in agent [:status :alive?] true)]
     (and alive?
          (not parent)
          (cond
            (= role :deer) (and (> food 0.6) (> mood 0.7))
            (= role :wolf) (and (> food 0.7) (> mood 0.75))))))

(defn create-fawn!
   "Create a baby deer (fawn) with reduced stats."
   [world parent-agent tick]
   (let [parent-id (:id parent-agent)
         next-id (or (:next-agent-id world) 0)
         pos (:pos parent-agent)
         fawn {:id next-id
                :name (initial/agent-name next-id :fawn)
                :pos pos
                :role :deer
                :faction :wilderness
                :stats {:strength (* 0.3 const/baby-stat-multiplier)
                         :dexterity (* 0.3 const/baby-stat-multiplier)
                         :fortitude (* 0.3 const/baby-stat-multiplier)
                         :charisma (* 0.3 const/baby-stat-multiplier)}
                :needs {:mood 0.6 :social 0.5 :food 0.8 :water 0.7 :rest 0.7 :health 0.6 :security 0.3 :warmth 0.6}
                :need-thresholds {:food-starve 0.0 :food-hungry 0.2 :food-satisfied 0.8}
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
                :parent-ids [parent-id]
                :children-ids []
                :carrying-child nil
                :baby? true
                :ticks-until-growth (+ tick const/fawn-growth-ticks)}]
     {:fawn fawn
      :next-agent-id (inc next-id)}))

(defn create-pup!
   "Create a baby wolf (pup) with reduced stats."
   [world parent-agent tick]
   (let [parent-id (:id parent-agent)
         next-id (or (:next-agent-id world) 0)
         pos (:pos parent-agent)
         pup {:id next-id
               :name (initial/agent-name next-id :pup)
               :pos pos
               :role :wolf
               :faction :wilderness
               :stats {:strength (* 0.4 const/baby-stat-multiplier)
                        :dexterity (* 0.4 const/baby-stat-multiplier)
                        :fortitude (* 0.4 const/baby-stat-multiplier)
                        :charisma (* 0.4 const/baby-stat-multiplier)}
               :needs {:mood 0.5 :social 0.4 :food 0.8 :water 0.7 :rest 0.7 :health 0.7 :security 0.4 :warmth 0.6}
               :need-thresholds {:food-starve 0.0 :food-hungry 0.25 :food-satisfied 0.8}
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
               :parent-ids [parent-id]
               :children-ids []
               :carrying-child nil
               :baby? true
               :ticks-until-growth (+ tick const/pup-growth-ticks)}]
     {:pup pup
      :next-agent-id (inc next-id)}))

(defn advance-wildlife-child-growth
   "Advance wildlife child growth and transition to adult."
   [agent tick]
   (let [role (:role agent)
         ticks-since-birth (- tick (:ticks-as-child agent))
         ticks-until-growth (:ticks-until-growth agent const/fawn-growth-ticks)
         current-stage (:child-stage agent)]
     (cond
       (< ticks-since-birth ticks-until-growth) [agent current-stage false]
       (>= ticks-since-birth ticks-until-growth)
          [(-> agent
               (assoc :child-stage :adult)
               (dissoc :baby?)
               (dissoc :ticks-until-growth)
               (assoc-in [:stats :strength] (get initial/default-agent-stats :strength))
               (assoc-in [:stats :dexterity] (get initial/default-agent-stats :dexterity))
               (assoc-in [:stats :fortitude] (get initial/default-agent-stats :fortitude))
               (assoc-in [:stats :charisma] (get initial/default-agent-stats :charisma)))
           :adult true])))
