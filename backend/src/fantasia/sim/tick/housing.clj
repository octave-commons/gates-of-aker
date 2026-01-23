(ns fantasia.sim.tick.housing
  "Housing step: assign tired agents to nearby houses."
  (:require [fantasia.sim.houses :as houses]))

(defn process-housing-step!
  "Process housing assignment for all agents. Returns map with :agents and :world."
  [world agents]
  (reduce
    (fn [{:keys [agents world] :as acc} agent]
      (let [rest-need (get-in agent [:needs :rest] 0.5)
            is-tired? (< rest-need 0.3)
            has-house? (get agent :house-id nil)]
        (if (and is-tired? (not has-house?))
          (let [nearby-house (houses/find-nearby-house-with-empty-bed world (:pos agent) 10)]
            (if nearby-house
              (let [world' (houses/assign-agent-to-house world (:id agent) nearby-house)
                    agent' (assoc agent :house-id nearby-house)]
                {:agents (assoc agents (:id agent) agent')
                 :world world'})
              acc))
          acc)))
    {:agents agents :world world}
    agents))
