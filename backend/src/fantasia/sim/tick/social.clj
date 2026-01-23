(ns fantasia.sim.tick.social
  "Social step: agent conversations and interactions."
  (:require [fantasia.sim.social :as social]
            [fantasia.sim.agents :as agents]))

(defn process-social-step!
  "Process all social interactions between agents. Returns map with :world, :agents, :mentions, :traces, :social-interactions."
  [world agents previous-mentions previous-traces pairs]
  (reduce
    (fn [{:keys [world agents mentions traces social-interactions]} [speaker listener]]
      (let [speaker' (get agents (:id speaker) speaker)
            listener' (get agents (:id listener) listener)
            social-step (social/trigger-social-interaction! world speaker' listener')
            world' (:world social-step)
            agent-1 (:agent-1 social-step)
            agent-2 (:agent-2 social-step)
            interaction (:interaction social-step)
            agents' (-> agents
                        (assoc (:id agent-1) agent-1)
                        (assoc (:id agent-2) agent-2))
            packet (agents/choose-packet world' agent-1)
            res (agents/apply-packet-to-listener world' agent-2 agent-1 packet)
            agents'' (assoc agents' (:id agent-2) (:listener res))]
        {:world world'
         :agents agents''
         :mentions (into mentions (:mentions res))
         :traces (into traces (:traces res))
         :social-interactions (into social-interactions [interaction])}))
    {:world world
     :agents (vec agents)
     :mentions previous-mentions
     :traces previous-traces
     :social-interactions []}
    pairs))
