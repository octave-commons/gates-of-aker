(ns fantasia.sim.tick.reproduction
  "Reproduction step: agent pairing, pregnancy, and child growth."
  (:require [fantasia.sim.reproduction :as reproduction]))

(defn process-reproduction-step!
  "Process reproduction for all agent pairs. Returns map with :agents and :next-agent-id."
  [world agents next-agent-id pairs tick]
  (reduce
    (fn [{:keys [agents next-agent-id] :as acc} [parent1 parent2]]
      (let [world-with-next-id (assoc world :next-agent-id next-agent-id)
            can-reproduce? (reproduction/can-reproduce? world-with-next-id parent1 parent2)]
        (if can-reproduce?
          (let [{:keys [child-agent next-agent-id]} (reproduction/create-child-agent world-with-next-id parent1 parent2 tick)
                agents' (conj agents child-agent)
                parents (vec agents')
                parent1-updated (assoc parents (:id parent1) (assoc parent1 :carrying-child (:id child-agent)))]
            {:agents parent1-updated :next-agent-id next-agent-id})
          acc)))
    {:agents agents :next-agent-id next-agent-id}
    pairs))

(defn process-growth-step!
  "Process child growth for all agents. Returns updated agents map."
  [agents tick]
  (reduce
    (fn [agents agent]
      (if (:child-stage agent)
        (let [[updated-agent new-stage released?] (reproduction/advance-child-growth agent tick)
              id (:id agent)
              agents' (assoc agents id updated-agent)]
          (if (and released? (not= :infant new-stage))
            (let [parent-id (get-in agent [:parent-ids 0])]
              (if parent-id
                (update-in agents' [parent-id :carrying-child] (fn [x] (when (= x id) nil)))
                agents'))
            agents'))
        agents))
    agents
    agents))
