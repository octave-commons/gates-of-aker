(ns fantasia.sim.tick.combat
  "Combat selection + resolution for agents."
  (:require [fantasia.sim.hex :as hex]
            [fantasia.sim.jobs :as jobs]
            [fantasia.sim.tick.mortality :as mortality]
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

(defn- defense-reduction
  [agent]
  (let [fortitude (get-in agent [:stats :fortitude] 0.0)
        multiplier (min 1.0 (max 0.0 fortitude))]
    (* multiplier const/max-defense-multiplier)))

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
  (or (nearest-agent world (:pos agent) #(= (:role %) :deer) wolf-sight-range)
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
  (let [world' (mortality/agent-died! world (:id target) :combat (:role attacker))
        world' (cond-> world'
                 (= (:role target) :deer)
                 (jobs/add-item! (:pos target) :raw-meat 1)

                 (= (:role attacker) :wolf)
                 (assoc-in [:agents (:id attacker) :needs :food] const/wolf-kill-food-gain))]
    [world' {:type :hunt-kill
             :attacker-id (:id attacker)
             :attacker-role (:role attacker)
             :target-id (:id target)
             :target-role (:role target)
             :pos (:pos target)
             :tick (:tick world)}]))

(defn- apply-attack
   [world attacker target]
   (let [damage (damage-for (:role attacker))
         target-id (:id target)
         reduction (if (= (:faction target) :player)
                     (defense-reduction target)
                     0.0)
         reduced-damage (* damage (- 1.0 reduction))
         health (double (get-in world [:agents target-id :needs :health] 1.0))
         health' (max 0.0 (- health reduced-damage))
         world' (assoc-in world [:agents target-id :needs :health] health')
         world' (if (and (= (:role attacker) :wolf))
                  (update-in world' [:agents (:id attacker) :needs :food] + const/wolf-attack-food-gain)
                  world')]
     (if (<= health' 0.0)
       (apply-kill-effects world' attacker target)
       [world' {:type :hunt-attack
                :attacker-id (:id attacker)
                :attacker-role (:role attacker)
                :target-id (:id target)
                :target-role (:role target)
                :pos (:pos target)
                :damage reduced-damage
                :original-damage damage
                :defense-reduction reduction
                :tick (:tick world)}])))

(defn process-combat!
  "Apply one combat step for all agents. Returns [world events] tuple."
  [world]
  (reduce
   (fn [[w acc-events] attacker]
     (if (and (alive-agent? attacker)
              (not= (:role attacker) :deer))
       (if-let [target (select-target w attacker)]
         (if (in-range? (:pos attacker) target attack-range)
           (let [result (apply-attack w attacker target)]
             (if (vector? result)
               [(first result) (conj acc-events (second result))]
               [result acc-events]))
           [w (conj acc-events {:type :hunt-start
                               :attacker-id (:id attacker)
                               :attacker-role (:role attacker)
                               :target-id (:id target)
                               :target-role (:role target)
                               :pos (:pos target)
                               :tick (:tick w)})])
         [w acc-events])
       [w acc-events]))
   [world []]
   (:agents world)))
