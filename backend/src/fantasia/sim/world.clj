(ns fantasia.sim.world
  (:require [fantasia.sim.events :as events]
            [fantasia.sim.myth :as myth]))

(defn snapshot
  "Produce a UI-friendly snapshot of world state + attribution map."
  [world attribution]
  (let [agent-name-by-id (->> (:agents world)
                              (map (fn [agent]
                                     [(:id agent) (:name agent)]))
                              (into {}))]
    {:tick (:tick world)
      :shrine (:shrine world)
      :temperature (:temperature world)
      :cold-snap (:cold-snap world)
      :daylight (:daylight world)
      :calendar (:calendar world)
      :levers (:levers world)
      :map (:map world)
      :tiles (:tiles world)
      :recent-events (:recent-events world)
      :attribution attribution
      :jobs (:jobs world)
      :items (:items world)
      :stockpiles (:stockpiles world)
     :agents (mapv (fn [a]
                     (let [relationships (:relationships a)
                           rel-summary (->> relationships
                                           (map (fn [[agent-id rel]]
                                                  {:agent-id agent-id
                                                   :name (get agent-name-by-id agent-id)
                                                   :affinity (:affinity rel 0.5)
                                                   :last-interaction (:last-interaction rel)}))
                                           (sort-by (fn [rel] (- (double (:affinity rel 0.0)))))
                                           (take 3)
                                           vec)]
                       {:id (:id a)
                        :name (:name a)
                        :pos (:pos a)
                        :role (:role a)
                        :faction (:faction a)
                        :stats (:stats a)
                        :needs (:needs a)
                        :need-thresholds (:need-thresholds a)
                        :inventories (:inventories a)
                        :inventory (:inventory a)
                        :status (:status a)
                        :events (:events a)
                        :recall (:recall a)
                        :idle? (:idle? a)
                        :current-job (:current-job a)
                        :current-path (:current-path a)
                        :relationships rel-summary
                        :last-social-tick (:last-social-tick a)
                        :last-social-thought (:last-social-thought a)
                        :top-facets (->> (:frontier a)
                                         (sort-by (fn [[_ {:keys [a]}]] (- (double a))))
                                         (take 8)
                                         (mapv (fn [[k v]] {:facet k :a (:a v)})))}))
                  (:agents world))
     :ledger (into {}
                   (map (fn [[[et claim] v]]
                          [(str (name et) "/" (name claim))
                           {:buzz (:buzz v)
                            :tradition (:tradition v)
                            :mentions (:mentions v)}])
                        (:ledger world)))}))

(defn update-ledger
  "Apply decay + mentions to the ledger and return {:ledger ledger2 :attr {...}}"
  [world mentions]
  (let [ledger1 (myth/decay-ledger (:ledger world))
        ledger2 (reduce myth/add-mention ledger1 mentions)
        attr (into {}
                   (map (fn [et]
                          [(:id et) (myth/attribution ledger2 (:id et))])
                        (events/all-event-types)))]
    {:ledger ledger2
     :attribution attr}))
