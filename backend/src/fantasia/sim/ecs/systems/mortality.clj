(ns fantasia.sim.ecs.systems.mortality
  "ECS Mortality system - processes agent deaths and creates memories."
  (:require [brute.entity :as be]
            [fantasia.dev.logging :as log]
            [fantasia.sim.ecs.components :as c]
            [fantasia.sim.ecs.core :as ecs-core]))

(defn- get-component-type-instance
  "Get component type from ECS core."
  [component-instance]
  (ecs-core/component-class component-instance))

(defn- check-entity-mortality
  "Check if an entity should die based on critical needs.
   Returns cause of death if entity dies, nil otherwise."
  [ecs-world entity-id]
  (let [needs-instance (c/->Needs 0.6 0.7 0.7 1.0 0.8 0.6 0.5 0.5 0.5 0.6 0.5 0.5 0.5)
        needs-type (get-component-type-instance needs-instance)
        needs (be/get-component ecs-world entity-id needs-type)]
    (when needs
      (let [food (or (:food needs) 1.0)
            health (or (:health needs) 1.0)]
        (cond
          (<= food 0.15) :starvation
          (<= health 0.0) :health-critical
          :else nil)))))

(defn- create-death-memory
    "Create a memory entity for agent death."
    [ecs-world tick agent-pos cause killer-role agent-id memory-strength]
    (let [memory-id (java.util.UUID/randomUUID)

          ;; Build facet list for memory
          base-facets ["death" "tragedy" "loss" "warning" "fear" "blood" "corpse"]

          ;; Killer facets based on cause
          killer-facets (cond
                          killer-role [ (name killer-role)]
                          (= cause :starvation) ["wolf" "hunger"]
                          (= cause :health-critical) ["bear" "danger"]
                          :else [])

          ;; Agent-specific facets
          agent-facets ["agent" "person"]

           memory-facets (distinct (concat base-facets agent-facets killer-facets))

          memory-instance (c/->Memory memory-id :memory/danger agent-pos tick memory-strength agent-id memory-facets)]

      (log/log-info "[MORTALITY:DEATH]"
                    {:agent-id agent-id
                     :pos agent-pos
                     :cause cause
                     :killer-role killer-role
                     :strength memory-strength})

      (-> ecs-world
          (be/add-entity {memory-id memory-instance}))))

(defn- handle-entity-death
  "Handle entity death by creating memory and marking as dead."
  [ecs-world tick entity-id cause killer-role]
  (let [pos-instance (c/->Position 0 0)
        pos-type (get-component-type-instance pos-instance)
        stats-instance (c/->Stats 0.0 0.0 0.0 0.0)
        stats-type (get-component-type-instance stats-instance)
        status-instance (c/->AgentStatus true false true nil)
        status-type (get-component-type-instance status-instance)

        entity-pos (be/get-component ecs-world entity-id pos-type)
        agent-stats (be/get-component ecs-world entity-id stats-type)
        status (or (be/get-component ecs-world entity-id status-type)
                   status-instance)

        ;; Calculate memory strength based on agent stats
        strength (min 2.0 (+ 0.5 (* 0.01 (or (:strength agent-stats) 0.4))))

        ;; Mark as dead
        new-death-state (c/->DeathState false cause tick)

        ;; Create memory at death location
        world-with-memory (create-death-memory ecs-world tick entity-pos cause killer-role entity-id strength)]

    (-> world-with-memory
        (be/add-component entity-id new-death-state)
        (be/add-component entity-id (assoc status :alive? false :cause-of-death cause)))))

(defn- cleanup-jobs-for-dead-entity
  "Remove entity from any assigned jobs and mark jobs as pending."
  [ecs-world entity-id]
  (let [job-instance (c/->JobAssignment nil 0.0)
         job-type (get-component-type-instance job-instance)
         path-instance (c/->Path [] 0)
         queue-instance (c/->JobQueue {} [] {})
         queue-type (get-component-type-instance queue-instance)
         current-job (be/get-component ecs-world entity-id job-type)
         job-id (:job-id current-job)
         world-without-assignment (-> ecs-world
                                      (be/remove-component entity-id job-instance)
                                      (be/remove-component entity-id path-instance))]
    (reduce (fn [world building-id]
              (let [queue (be/get-component world building-id queue-type)
                    jobs (:jobs queue {})
                    requeue-ids (->> jobs
                                     (keep (fn [[id job]]
                                             (when (and (= entity-id (:worker-id job))
                                                        (or (nil? job-id) (= id job-id)))
                                               id)))
                                     vec)]
                (if (or (nil? queue) (empty? requeue-ids))
                  world
                  (let [updated-jobs (reduce (fn [acc id]
                                               (if-let [job (get acc id)]
                                                 (assoc acc id (assoc job :worker-id nil :state :pending))
                                                 acc))
                                             jobs
                                             requeue-ids)
                        pending-jobs (reduce (fn [acc id]
                                               (conj (vec (remove #(= % id) acc)) id))
                                             (vec (:pending-jobs queue []))
                                             requeue-ids)
                        assigned-jobs (reduce (fn [acc id] (dissoc acc id))
                                              (:assigned-jobs queue {})
                                              requeue-ids)
                        updated-queue (c/->JobQueue updated-jobs pending-jobs assigned-jobs)]
                    (be/add-component world building-id updated-queue)))))
            world-without-assignment
            (be/get-all-entities-with-component world-without-assignment queue-type))))

(defn process
  "Process mortality for all entities, handling deaths and creating memories.
   Returns updated ECS world."
  ([ecs-world]
   (process ecs-world nil))
  ([ecs-world tick]
   (let [needs-instance (c/->Needs 0.6 0.7 0.7 1.0 0.8 0.6 0.5 0.5 0.5 0.6 0.5 0.5 0.5)
         needs-type (get-component-type-instance needs-instance)
         death-instance (c/->DeathState true nil nil)
         death-type (get-component-type-instance death-instance)
         status-instance (c/->AgentStatus true false true nil)
         status-type (get-component-type-instance status-instance)
         all-entities (be/get-all-entities-with-component ecs-world needs-type)]

     (reduce
      (fn [world entity-id]
        (let [death-state (be/get-component world entity-id death-type)
              status (be/get-component world entity-id status-type)
              alive? (and (not= false (:alive? death-state true))
                          (not= false (:alive? status true)))
              cause-of-death (when alive? (check-entity-mortality world entity-id))]
          (cond
            cause-of-death
            (-> world
                (handle-entity-death tick entity-id cause-of-death nil)
                (cleanup-jobs-for-dead-entity entity-id))

            (not alive?)
            (cleanup-jobs-for-dead-entity world entity-id)

            :else
            world)))
      ecs-world
      all-entities))))
