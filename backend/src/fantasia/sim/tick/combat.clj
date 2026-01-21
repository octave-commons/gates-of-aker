(ns fantasia.sim.tick.combat
  "Combat selection + resolution for agents."
  (:require [fantasia.sim.hex :as hex]
            [fantasia.sim.jobs :as jobs]
            [fantasia.sim.tick.mortality :as mortality]))

(def ^:private attack-range 1)
(def ^:private deer-sight-range 6)
(def ^:private player-sight-range 4)

(def ^:private role-damage
  {:wolf 0.16
   :bear 0.22
   :deer 0.0
   :default 0.12})

(defn- alive-agent?
  [agent]
  (get-in agent [:status :alive?] true))

(defn- in-range?
  [pos target range]
  (<= (hex/distance pos (:pos target)) range))

(defn- nearest-agent
  [world pos predicate range]
  (->> (:agents world)
       (filter alive-agent?)
       (filter predicate)
       (filter #(in-range? pos % range))
       (sort-by (fn [agent] (hex/distance pos (:pos agent))))
       first))

(defn- wolf-target
  [world agent]
  (or (nearest-agent world (:pos agent) #(= (:role %) :deer) deer-sight-range)
      (nearest-agent world (:pos agent) #(= (:faction %) :player) player-sight-range)))

(defn- hunt-job-target
  [world agent]
  (when-let [job (jobs/get-agent-job world (:id agent))]
    (when (= (:type job) :job/hunt)
      (let [target (get-in world [:agents (:target-agent-id job)])]
        (when (alive-agent? target)
          target)))))

(defn- player-target
  [world agent]
  (let [hunt-target (hunt-job-target world agent)]
    (or hunt-target
        (nearest-agent world (:pos agent) #(= (:role %) :wolf) attack-range))))

(defn- select-target
  [world agent]
  (case (:role agent)
    :wolf (wolf-target world agent)
    :deer nil
    (when (= (:faction agent) :player)
      (player-target world agent))))

(defn- damage-for
  [role]
  (double (get role-damage role (:default role-damage))))

(defn- apply-kill-effects
  [world attacker target]
  (let [world' (mortality/agent-died! world (:id target) :combat (:role attacker))]
    (cond-> world'
      (= (:role target) :deer)
      (jobs/add-item! (:pos target) :raw-meat 1)

      (= (:role attacker) :wolf)
      (assoc-in [:agents (:id attacker) :needs :food] 1.0))))

(defn- apply-attack
  [world attacker target]
  (let [damage (damage-for (:role attacker))
        target-id (:id target)
        health (double (get-in world [:agents target-id :needs :health] 1.0))
        health' (max 0.0 (- health damage))
        world' (assoc-in world [:agents target-id :needs :health] health')]
    (if (<= health' 0.0)
      (apply-kill-effects world' attacker target)
      world')))

(defn process-combat!
  "Apply one combat step for all agents." 
  [world]
  (reduce
   (fn [w attacker]
     (if (and (alive-agent? attacker)
              (not= (:role attacker) :deer))
       (if-let [target (select-target w attacker)]
         (if (in-range? (:pos attacker) target attack-range)
           (apply-attack w attacker target)
           w)
         w)
       w))
   world
   (:agents world)))
