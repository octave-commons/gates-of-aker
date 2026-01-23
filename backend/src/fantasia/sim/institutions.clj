(ns fantasia.sim.institutions
  (:require [fantasia.sim.agents :as agents]))

(defn broadcasts
   "Return institution packets that should fire on this tick."
   [world]
   (let [t (or (:tick world) 0)]
    (for [inst (vals (:institutions world))
          :let [every (:broadcast-every inst)]
          :when (and every (pos? every) (zero? (mod t every)))]
      {:institution (:id inst)
       :packet {:intent :convert
                :facets (get-in inst [:canonical :facets] [])
                :tone {:awe 0.55 :urgency 0.25}
                :claim-hint (get-in inst [:canonical :claim-hint])}})))

(defn apply-broadcast
   "Apply an institution broadcast packet to all agents.
    Returns {:agents [...] :mentions [...] :traces [...]}"
   [world agents {:keys [institution packet]}]
   (let [mouth-id (get-in world [:levers :mouthpiece-agent-id])
         mouth (when (some? mouth-id)
                 (first (filter #(= (:id %) mouth-id) agents)))
         speaker (or mouth {:id (keyword (name institution))
                            :role :institution})]
     (reduce
       (fn [{:keys [agents mentions traces]} a]
         (let [res (agents/apply-packet-to-listener world a speaker packet)]
           {:agents (conj agents (:listener res))
            :mentions (into mentions (:mentions res))
            :traces (into traces (:traces res))}))
       {:agents [] :mentions [] :traces []}
       agents)))

(defn process-broadcasts!
  "Process all institution broadcasts. Returns map with :agents, :mentions, :traces."
  [world agents previous-mentions previous-traces broadcasts]
  (reduce
    (fn [{:keys [agents mentions traces]} b]
      (let [res (apply-broadcast world agents b)]
        {:agents (:agents res)
         :mentions (into mentions (:mentions res))
         :traces (into traces (:traces res))}))
    {:agents agents
     :mentions previous-mentions
     :traces previous-traces}
    broadcasts))
