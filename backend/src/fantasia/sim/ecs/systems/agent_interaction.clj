(ns fantasia.sim.ecs.systems.agent-interaction
  (:require [brute.entity :as be]
            [fantasia.sim.ecs.core]
            [fantasia.sim.ecs.components :as c]
            [fantasia.sim.hex :as hex]))

(defn nearby-agents
  "Get agents within vision radius of a position."
  [ecs-world pos radius]
  (let [position-type (be/get-component-type (c/->Position 0 0))
        all-agents (be/get-all-entities-with-component ecs-world position-type)
        filter-fn (fn [agent-id]
                      (let [position (be/get-component ecs-world agent-id position-type)
                            q (:q position)
                            r (:r position)]
                        (when (and q r)
                          (<= (hex/distance [q r] pos) radius))))]
    (filter filter-fn all-agents)))

(defn choose-packet-for-speech
  "Generate a communication packet for an agent based on frontier state."
  [frontier]
  (let [facets (:facets frontier)
        top-facets (take 10 (sort-by val > facets))]
        {:packet-id (java.util.UUID/randomUUID)
         :speaker-role nil
         :intent (cond
                     (some #(#{:warn :boast :recruit} %) (keys facets)) :warning
                     (some #(#{:report :ask-question} %) (keys facets)) :inquiry
                     :else :gossip)
         :topic-vec top-facets
         :tone {:arousal 0.0 :valence 0.0 :fear 0.0}
         :salience 0.8
         :anchors #{}
         :credibility {:speaker-reputation 0.5 :channel-mod 1.0}
         :spread {:base-radius 3 :entropy 0.1 :delay 1}}))

;; (defn receive-packet
;;   "Process a received packet for a listener agent."
;;   [ecs-world agent-id packet]
;;   (let [frontier-type (be/get-component-type (c/->Frontier {}))
;;         frontier (be/get-component ecs-world agent-id frontier-type)
;;         recall-type (be/get-component-type (c/->Recall {}))
;;         recall (be/get-component ecs-world agent-id recall-type)
;;         topic-vec (:topic-vec packet)
;;         facets (:facets frontier)
;;         salience (:salience packet)
;;         anchors (:anchors packet)]
;;     (when (> salience 0.5)
;;       (let [updated-frontier (reduce (fn [acc facet]
;;                                          (let [facet-key (:facet acc)
;;                                                 facet-val (get (:topic-vec packet) facet-key)
;;                                                 delta (min 0.1 (* facet-val salience))]
;;                                            (assoc acc facet-key (+ (or (get acc facet-key) 0.0) delta)))
;;                                        facets)]
;;             (be/add-component ecs-world agent-id frontier-type updated-frontier)))))

;; (defn process-conversations
;;   "Process conversations between nearby agents."
;;   [ecs-world]
;;   (let [role-type (be/get-component-type (c/->Role :priest))
;;         all-agents (be/get-all-entities-with-component ecs-world role-type)
;;         status-type (be/get-component-type (c/->AgentStatus true false false nil))]
;;     (doseq [agent-id all-agents]
;;       (let [position (be/get-component ecs-world agent-id (c/->Position 0 0))
;;             nearby-agents (nearby-agents ecs-world position 3)
;;             status (be/get-component ecs-world agent-id status-type)]
;;         (when (and (:alive? status) (not (:asleep? status)))
;;           (when (> (count nearby-agents) 1)
;;             ;; Generate packet from speaker to listener
;;             (let [speaker-frontier (be/get-component ecs-world agent-id (c/->Frontier {}))
;;                   listener-frontier (be/get-component ecs-world agent-id (c/->Frontier {}))
;;                   packet (choose-packet-for-speech speaker-frontier)]
;;  ;;                   new-listener-frontier (receive-packet ecs-world agent-id packet)]
;;             (println "[AGENTS] Conversation:" agent-id "->" (second nearby-agents)))))))))


;; (defn update-agent-mood
;;   "Update agent mood based on social interactions."
;;   [ecs-world agent-id]
;;   (let [status-type (be/get-component-type (c/->AgentStatus true false false nil))
;;         position-type (be/get-component-type (c/->Position 0 0))
;;         status (be/get-component ecs-world agent-id status-type)
;;         mood-type (be/get-component-type (c/->Needs (:mood 0.0 0.5 0.5 0.5))]
;;         mood (be/get-component ecs-world agent-id mood-type)
;;         current-mood (:mood mood)]
;;     (let [env-mood-bonus (cond
;;                            ;; Check environment modifiers
;;                            (let [temperature (double (or (:temperature world) 0.6)]
;;                                  campfire-near? false
;;                                  house-near? false
;;                                  temple-near? false)]
;;                             (cond
;;                               ;; House provides warmth bonus
;;                               (and house-near?
;;                                    (if (> current-mood 0.3)
;;                                      (+ 0.05 0.01))) ;; Slight boost
;;                                (if (< current-mood 0.7)
;;                                      (- 0.05 0.01))) ;; Slight penalty
;;                               :else 0.0)
;;
;;                                ;; Campfire provides warmth bonus
;;                               (and campfire-near?
;;                                    (if (> current-mood 0.3)
;;                                      (+ 0.03 0.01)))
;;                                    (if (< current-mood 0.7)
;;                                      (- 0.03 0.01))) ;; Slight boost
;;                                    :else 0.0)
;;
;;                                ;; Temple provides comfort bonus
;;                               (and temple-near?
;;                                    (if (> current-m0.3)
;;                                      (+ 0.02 0.01))) ;; Slight boost
;;                                    (if (< current-mood 0.7)
;;                                      (- 0.02 0.01))) ;; Slight penalty
;;                                    :else 0.0)
;;
;;                               :else 0.0))]
;;           (be/add-component ecs-world agent-id mood-type new-mood)))))

;; (defn process
;;   "Process agent interactions (conversations, mood effects)."
;;   [ecs-world]
;;   (doseq [agent-id (be/get-all-entities-with-component ecs-world (c/->Role :priest))]
;;         (process-conversations ecs-world agent-id)
;;         (when (> (rand-int 100) 0.15)
;;           ;; 15% chance to initiate conversation
;;           (update-agent-mood ecs-world agent-id)))))

;; (defn update-tick
;;   "Add agent interaction system to tick loop."
;;   [ecs-world global-state]
;;   (doseq [agent-id (be/get-all-entities-with-component ecs-world (c/->Role :priest))]
;;         (let [status-type (be/get-component-type (c/->AgentStatus true false false nil))
;;               position-type (be-get-component-type (c->Position 0 0))]
;;           status (be/get-component ecs-world agent-id status-type)
;;         ;; Only process if agent is alive and not asleep
;;         (when (and (:alive? status) (not (:asleep? status)))
;;           (process ecs-world agent-id)))))

;; (defn -main
;;   []
;;   (let [world (fantasia.sim.ecs.core/create-ecs-world)
;;         agents (be/get-all-entities-with-component world (c/->Role :priest))
;;         initial-count (count agents)]
;;     (println "[AGENT-INTERACTION] Created" initial-count " "agents")
;;     (doseq [agent-id agents]
;;       (when (zero? (mod agent-id 6) ;; Process only every 6 ticks to avoid excessive computation
;;         (process (fantasia.sim.ecs.tick/update-tick (fantasia.sim.ecs.core/reset-ecs-world! world) agent-id))))
;;     (println "[AGENT-INTERACTION] Agent interactions processed")
;;     world))
