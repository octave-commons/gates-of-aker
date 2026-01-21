(ns fantasia.sim.social
  (:require [fantasia.sim.facets :as f]
            [fantasia.sim.memories :as memories]
            [fantasia.sim.spatial_facets :as spatial-facets]))

(def social-interaction-types
  {:small-talk {:mood-boost 0.05 :social-boost 0.08 :affinity-delta 0.02 :transmission-strength 0.3 :name "Small talk"}
   :gossip {:mood-boost 0.10 :social-boost 0.10 :affinity-delta 0.03 :transmission-strength 0.5 :name "Gossip"}
   :debate {:mood-boost -0.02 :social-boost 0.02 :affinity-delta -0.05 :transmission-strength 0.8 :name "Debate"}
   :ritual {:mood-boost 0.20 :social-boost 0.18 :affinity-delta 0.06 :transmission-strength 1.0 :divine-favor 1.0 :name "Ritual"}
   :teaching {:mood-boost 0.07 :social-boost 0.10 :affinity-delta 0.04 :transmission-strength 0.7 :name "Teaching"}})

(def interaction-facet-hints
  {:small-talk ["conversation" "neighbor" "comfort"]
   :gossip ["gossip" "rumor" "story"]
   :debate ["argument" "tension" "conflict"]
   :ritual ["ritual" "prayer" "community"]
   :teaching ["teaching" "learning" "guidance"]})

(defn- choose-interaction-type
  "Choose interaction type based on agent moods and randomness."
   [agent1 agent2]
   (let [mood1 (get-in agent1 [:needs :mood] 0.5)
         mood2 (get-in agent2 [:needs :mood] 0.5)
         avg-mood (/ (+ mood1 mood2) 2.0)
         rand (.nextInt (java.util.Random.) 100)]
     (cond
       (< rand 10) :ritual
       (and (< rand 30) (or (= (:role agent1) :priest) (= (:role agent2) :priest))) :debate
       (< rand 50) :teaching
       (< rand 75) :gossip
       :else :small-talk)))

(defn- apply-mood-change
   "Apply mood change to an agent, clamping between 0 and 1."
   [agent delta]
   (let [current-mood (get-in agent [:needs :mood] 0.5)
         new-mood (f/clamp01 (+ current-mood delta))]
     (assoc-in agent [:needs :mood] new-mood)))

(defn- apply-social-change
  "Apply social change to an agent, clamping between 0 and 1."
  [agent delta]
  (let [current-social (get-in agent [:needs :social] 0.55)
        new-social (f/clamp01 (+ current-social delta))]
    (assoc-in agent [:needs :social] new-social)))

(defn- update-relationship
  "Update relationship affinity and last interaction timestamp."
  [agent other-id delta tick]
  (let [relationships (get agent :relationships {})
        current (get relationships other-id {:affinity 0.5 :last-interaction nil})
        affinity (f/clamp01 (+ (double (:affinity current 0.5)) (double delta)))
        updated {:affinity affinity :last-interaction tick}]
    (assoc agent :relationships (assoc relationships other-id updated))))

(defn- social-thought
  "Return a short social thought for an interaction."
  [interaction-type other-id affinity-delta]
  (let [subject (str "#" other-id)
        positive (case interaction-type
                   :ritual [(str "Shared a ritual with " subject)
                            (str "Felt a spark with " subject)]
                   :teaching [(str "Learned from " subject)
                              (str "Guided by " subject)]
                   :gossip [(str "Shared gossip with " subject)
                            (str "Laughed with " subject)]
                   :small-talk [(str "Chatted with " subject)
                                (str "Caught up with " subject)]
                   :debate [(str "Debated with " subject)
                            (str "Argued ideas with " subject)]
                   [(str "Chatted with " subject)])
        negative [(str "Conflict with " subject)
                  (str "Tension with " subject)
                  (str "Words got sharp with " subject)]]
    (rand-nth (if (neg? affinity-delta) negative positive))))

(defn- interaction-memory-facets
  "Return memory facets for an interaction." 
  [interaction-type affinity-delta]
  (let [relationship-facets (if (neg? affinity-delta)
                              (spatial-facets/get-entity-facets :memory/social-conflict)
                              (spatial-facets/get-entity-facets :memory/social-bond))
        hint-facets (get interaction-facet-hints interaction-type [])]
    (vec (distinct (concat relationship-facets hint-facets)))))

(defn trigger-social-interaction!
  "Execute a social interaction between two agents.
   Returns updated world, agents, and interaction record."
  [world agent1 agent2]
  (let [interaction-type (choose-interaction-type agent1 agent2)
        config (get social-interaction-types interaction-type)
        tick (:tick world)
         affinity-delta (:affinity-delta config 0.0)
         social-boost (:social-boost config 0.0)
         mood-boost (+ (:mood-boost config)
                       (if (pos? affinity-delta) (* affinity-delta 0.6) 0.0)
                       (if (> social-boost 0.05) 0.02 0.0))
         agent1' (-> agent1
                     (apply-mood-change mood-boost)
                     (apply-social-change social-boost)
                    (update-relationship (:id agent2) affinity-delta tick)
                    (assoc :last-social-tick tick)
                    (assoc :last-social-thought (social-thought interaction-type (:id agent2) affinity-delta)))
         agent2' (-> agent2
                     (apply-mood-change mood-boost)
                     (apply-social-change social-boost)
                    (update-relationship (:id agent1) affinity-delta tick)
                    (assoc :last-social-tick tick)
                    (assoc :last-social-thought (social-thought interaction-type (:id agent1) affinity-delta)))
        divine-favor-gained (:divine-favor config 0)
        memory-facets (interaction-memory-facets interaction-type affinity-delta)
        world' (if (seq memory-facets)
                 (memories/create-memory! world
                                          :memory/social
                                          (:pos agent1)
                                          0.4
                                          (:id agent1)
                                          memory-facets)
                 world)
         interaction {:interaction-type interaction-type
                      :agent-1-id (:id agent1)
                      :agent-2-id (:id agent2)
                      :mood-change-1 mood-boost
                      :mood-change-2 mood-boost
                     :social-boost social-boost
                     :affinity-delta affinity-delta
                     :divine-favor-gained divine-favor-gained
                     :transmission-strength (:transmission-strength config)
                     :timestamp tick
                     :location (:pos agent1)}]
    {:world world'
     :agent-1 agent1'
     :agent-2 agent2'
     :interaction interaction}))

(defn enhance-packet-with-social-context
   "Enhance a facet packet with social interaction context."
   [packet interaction-strength mood1 mood2]
   (let [strength-multiplier interaction-strength
         mood-bonus (* 0.2 (/ (+ mood1 mood2) 2.0))]
     (-> packet
         (update :spread-gain #(* % strength-multiplier))
         (update :seed-strength #(+ % mood-bonus)))))

(defn apply-social-packet-to-listener
   "Apply a socially-enhanced packet to listener agent."
   [world listener speaker packet interaction-strength]
   (let [mood1 (get-in speaker [:needs :mood] 0.5)
         mood2 (get-in listener [:needs :mood] 0.5)
         enhanced-packet (enhance-packet-with-social-context packet interaction-strength mood1 mood2)]
     enhanced-packet))
