(ns fantasia.sim.world
   (:require [fantasia.sim.events :as events]
             [fantasia.sim.myth :as myth]
             [fantasia.sim.los :as los]
             [fantasia.sim.delta :as delta]
             [fantasia.sim.constants :as const]))

(defn snapshot
    "Produce a UI-friendly snapshot of world state + attribution map."
    [world attribution]
    (let [agent-name-by-id (->> (:agents world)
                                (map (fn [agent]
                                       [(:id agent) (:name agent)]))
                                (into {}))
          tile-visibility (:tile-visibility world {})
          visible-tiles (if (empty? tile-visibility)
                          (:tiles world)
                          (into {}
                                (filter (fn [[tile-key]]
                                          (let [vis (get tile-visibility tile-key :hidden)]
                                            (or (= vis :visible) (= vis :revealed))))
                                        (:tiles world))))]
      {:tick (:tick world)
        :shrine (:shrine world)
        :temperature (:temperature world)
        :cold-snap (:cold-snap world)
        :daylight (:daylight world)
        :calendar (:calendar world)
        :levers (:levers world)
        :map (:map world)
        :tiles visible-tiles
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
                           (:ledger world)))
        :favor (:favor world)
        :faith (:faith world)
        :deities (:deities world)
        :memories (mapv (fn [m]
                          {:id (:id m)
                           :type (:type m)
                           :location (:location m)
                           :created-at (:created-at m)
                           :strength (:strength m)
                           :decay-rate (:decay-rate m)
                           :entity-id (:entity-id m)
                           :facets (:facets m)})
                        (vals (:memories world)))}))

(defn update-ledger
   "Apply decay + mentions to ledger and return {:ledger ledger2 :attr {...}}"
   [world mentions]
   (let [ledger1 (myth/decay-ledger (:ledger world))
         ledger2 (reduce myth/add-mention ledger1 mentions)
         attr (into {}
                    (map (fn [et]
                           [(:id et) (myth/attribution ledger2 (:id et))])
                         (events/all-event-types)))]
     {:ledger ledger2
      :attribution attr}))

(defn delta-snapshot
     "Compute delta snapshot with LOS filtering. Returns map with :delta true marker and per-visibility data."
      [old-world new-world attribution]
    (let [d (delta/world-delta old-world new-world)
           player-agents (filter #(= (:faction %) :player) (:agents new-world))
           player-positions (map :pos player-agents)
            visibility-map (reduce (fn [m pos]
                                     (assoc m (str pos) (los/compute-visibility new-world pos const/player-vision-radius)))
                                   {}
                                   player-positions)]
     {:delta true
      :tick (:tick new-world)
      :global-updates (:global-updates d)
      :changed_agents (:changed-agents d)
      :changed-tiles (:changed-tiles d)
      :changed-items (:changed-items d)
      :changed-stockpiles (:changed-stockpiles d)
      :changed-jobs (:changed-jobs d)
      :changed-tile-visibility (:changed-tile-visibility d)
      :tile-visibility (:tile-visibility new-world)
      :changed-revealed-tiles-snapshot (:changed-revealed-tiles-snapshot d)
      :revealed-tiles-snapshot (:revealed-tiles-snapshot new-world)
      :visibility visibility-map
      :attribution attribution}))
