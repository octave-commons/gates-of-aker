(ns fantasia.sim.tick.mortality
  (:require [fantasia.sim.hex :as hex]
            [fantasia.sim.memories :as mem]
            [fantasia.sim.spatial_facets :as sf]
            [fantasia.dev.logging :as log]))

(defn check-agent-mortality
  "Check if an agent should die based on critical needs.
   Returns cause of death if agent dies, nil otherwise."
  [agent]
  (let [needs (:needs agent)
        thresholds (:need-thresholds agent)
        food (:food needs 1.0)
        health (:health needs 1.0)]
    (cond
      (<= food (get thresholds :food-starve 0.0))
      :starvation
      (<= health (get thresholds :health-critical 0.0))
      :health-critical
      :else
      nil)))

(defn agent-died!
  "Create a death memory facet when an agent dies.
   Memory includes:
   - 'death' facet (always)
   - Agent entity type facets (e.g., strong, warrior, peasant)
   - Killer entity facets (e.g., wolf, bear)
   - Agent-specific attributes"
  ([world agent-id cause]
   (agent-died! world agent-id cause nil))
  ([world agent-id cause killer-role]
   (let [agent (get-in world [:agents agent-id])
         agent-pos (:pos agent)
         agent-type (:role agent)
         current-job-id (:current-job agent)
         
         ;; Build facet list for memory
         base-facets ["death" "tragedy" "loss" "warning" "fear" "blood" "corpse"]
         agent-facets (sf/get-entity-facets (keyword (str "agent/" (name agent-type))))
         
         ;; Killer facets based on cause
         killer-facets (cond
                        killer-role (sf/get-entity-facets killer-role)
                        (= cause :starvation) (sf/get-entity-facets :wolf)
                        (= cause :health-critical) (sf/get-entity-facets :bear)
                        :else nil)
         
         ;; Strength based on agent stats (stronger agents = stronger memories)
         agent-strength (:strength (:stats agent) 0.4)
         memory-strength (min 2.0 (+ 0.5 (* 0.01 agent-strength)))
         
         ;; Create memory
         memory-facets (distinct (concat base-facets agent-facets killer-facets))]
     
     (log/log-info "[MORTALITY:DEATH]"
                   {:agent-id agent-id
                    :agent-type agent-type
                    :pos agent-pos
                    :cause cause
                    :killer-role killer-role
                    :strength memory-strength})
     
     ;; Create memory and mark agent as dead
     (-> (mem/create-memory! world
                             :memory/danger
                             agent-pos
                             memory-strength
                             agent-id
                             memory-facets)
         (cond-> current-job-id
           (update :jobs (fn [jobs]
                           (mapv (fn [job]
                                   (if (= (:id job) current-job-id)
                                     (assoc job :worker-id nil :state :pending)
                                     job))
                                 jobs))))
         (cond-> current-job-id
           (update-in [:agents agent-id] dissoc :current-job))
         (assoc-in [:agents agent-id :status :alive?] false)
         (assoc-in [:agents agent-id :status :cause-of-death] cause)))))

(defn process-mortality!
  "Process all agents and handle deaths by creating memories."
  [world]
  (reduce
    (fn [w' agent-index]
      (let [agent (get-in w' [:agents agent-index])
            agent-id (:id agent)
            alive? (get-in agent [:status :alive?] true)
            cause-of-death (when alive? (check-agent-mortality agent))]
        (if cause-of-death
          (agent-died! w' agent-id cause-of-death)
          w')))
    world
    (range (count (:agents world)))))
