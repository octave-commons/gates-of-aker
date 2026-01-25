(ns fantasia.sim.ecs.systems.mortality
  "ECS Mortality system - processes agent deaths and creates memories."
  (:require [brute.entity :as be]
            [fantasia.sim.ecs.components :as c]
            [fantasia.sim.ecs.core :as ecs-core]
            [fantasia.dev.logging :as log]))

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
    [ecs-world agent-pos cause killer-role agent-id memory-strength]
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

          memory-instance (c/->Memory memory-id :memory/danger agent-pos (:tick ecs-world) memory-strength agent-id memory-facets)
          memory-type (get-component-type-instance memory-instance)]

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
  [ecs-world entity-id cause killer-role]
  (let [pos-instance (c/->Position 0 0)
        pos-type (get-component-type-instance pos-instance)
        stats-instance (c/->Stats 0.0 0.0 0.0 0.0)
        stats-type (get-component-type-instance stats-instance)
        needs-instance (c/->Needs 0.6 0.7 0.7 1.0 0.8 0.6 0.5 0.5 0.5 0.6 0.5 0.5 0.5)
        needs-type (get-component-type-instance needs-instance)
        death-instance (c/->DeathState true nil nil)
        death-type (get-component-type-instance death-instance)

        entity-pos (be/get-component ecs-world entity-id pos-type)
        agent-stats (be/get-component ecs-world entity-id stats-type)

        ;; Calculate memory strength based on agent stats
        strength (min 2.0 (+ 0.5 (* 0.01 (or (:strength agent-stats) 0.4))))

        ;; Mark as dead
        new-death-state (c/->DeathState false cause (:tick ecs-world))

        ;; Create memory at death location
        world-with-memory (create-death-memory ecs-world entity-pos cause killer-role entity-id strength)]

    (-> world-with-memory
        (be/add-component entity-id new-death-state))))

(defn- cleanup-jobs-for-dead-entity
  "Remove entity from any assigned jobs and mark jobs as pending."
  [ecs-world entity-id]
  (let [job-instance (c/->JobAssignment nil 0.0)
        job-type (get-component-type-instance job-instance)
        current-job (be/get-component ecs-world entity-id job-type)]
    (when current-job
      ;; Remove job assignment from dead entity
      (be/remove-component ecs-world entity-id job-type))))

(defn process
  "Process mortality for all entities, handling deaths and creating memories.
   Returns updated ECS world."
  [ecs-world]
  (let [death-instance (c/->DeathState true nil nil)
        death-type (get-component-type-instance death-instance)
        all-entities (be/get-all-entities-with-component ecs-world death-type)]

    (reduce
     (fn [world entity-id]
       (let [death-state (be/get-component world entity-id death-type)
             alive? (or (:alive? death-state) true)
             cause-of-death (when alive? (check-entity-mortality world entity-id))]
         (if cause-of-death
           (-> world
               (handle-entity-death entity-id cause-of-death nil)
               (cleanup-jobs-for-dead-entity entity-id))
           world)))
     ecs-world
     all-entities)))
