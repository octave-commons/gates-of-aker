(ns fantasia.sim.tick.witness
  "Witness step: applies runtime events to witness agents."
  (:require [fantasia.sim.events.runtime :as runtime]))

(defn process-witness-step!
  "Apply event to all witness agents. Returns map with :agents, :mentions, :traces.
   If ev is nil, returns initial values with original agents."
  [world agents ev]
  (if ev
    (reduce
      (fn [{:keys [agents mentions traces]} a]
        (if (contains? (set (:witnesses ev)) (:id a))
          (let [res (runtime/apply-to-witness world a ev)]
            {:agents (conj agents (:agent res))
             :mentions (into mentions (:mentions res))
             :traces (into traces (:traces res))})
          {:agents (conj agents a)
           :mentions mentions
           :traces traces}))
      {:agents [] :mentions [] :traces []}
      agents)
    {:agents agents :mentions [] :traces []}))
