(ns fantasia.sim.world
  (:require [fantasia.sim.events :as events]
            [fantasia.sim.myth :as myth]))

(defn snapshot
   "Produce a UI-friendly snapshot of world state + attribution map."
    [world attribution]
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
                    {:id (:id a)
                     :pos (:pos a)
                     :role (:role a)
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
                     :top-facets (->> (:frontier a)
                                      (sort-by (fn [[_ {:keys [a]}]] (- (double a))))
                                      (take 8)
                                      (mapv (fn [[k v]] {:facet k :a (:a v)})))})
                  (:agents world))
    :ledger (into {}
                  (map (fn [[[et claim] v]]
                         [(str (name et) "/" (name claim))
                          {:buzz (:buzz v)
                           :tradition (:tradition v)
                           :mentions (:mentions v)}])
                       (:ledger world)))} )

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
