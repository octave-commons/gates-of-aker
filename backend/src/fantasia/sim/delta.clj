(ns fantasia.sim.delta
  "Delta tracking for efficient state updates."
  (:require [clojure.set :as set]))

(defn- deep-equal?
   "Deep equality check for maps/vectors, ignoring nested nil differences."
   [a b]
   (= a b))
 
(defn map-delta
   "Return delta between two maps. Keys with unchanged values are omitted."
   [old-map new-map]
   (if (and (map? old-map) (map? new-map))
     (reduce-kv (fn [delta k new-val]
                   (let [old-val (get old-map k)]
                     (if (deep-equal? old-val new-val)
                       delta
                       (assoc delta k new-val))))
                 {}
               new-map)
     new-map))
 
(defn agent-delta
   "Return delta for an agent (changed fields only)."
   [old-agent new-agent]
   (if (nil? old-agent)
     new-agent
     (map-delta old-agent new-agent)))
 
(defn world-delta
    "Compute delta between old and new world states."
    [old-world new-world]
     (println "[DELTA] Computing delta, old-world nil?" (nil? old-world) "old-world agents count:" (count (:agents old-world)) "new-world agents count:" (count (:agents new-world)))
     (if (nil? old-world)
       (do
         (println "[DELTA] Returning initial delta with all agents")
         {:tick (:tick new-world)
          :global-updates {:tick (:tick new-world)
                            :temperature (:temperature new-world)
                            :daylight (:daylight new-world)
                            :calendar (:calendar new-world)
                            :levers (:levers new-world)
                            :map (:map new-world)}
          :changed-agents (->> (:agents new-world)
                               (map (fn [new-agent]
                                      [(str (:id new-agent)) new-agent]))
                               (into {}))
          :changed-tiles (:tiles new-world)
          :changed-items (:items new-world)
          :changed-stockpiles (:stockpiles new-world)
          :changed-jobs (:jobs new-world)
          :changed-tile-visibility (:tile-visibility new-world)
          :changed-agent-visibility (map-delta (:agent-visibility old-world {}) (:agent-visibility new-world))
          :changed-revealed-tiles-snapshot (:revealed-tiles-snapshot old-world {}) (:revealed-tiles-snapshot new-world))})
     (let [changed-agents-result (->> (:agents new-world)
                            (map (fn [new-agent]
                                    (let [old-agent (first (filter #(= (:id %) (:id new-agent)) (:agents old-world)))
                                          equal? (deep-equal? old-agent new-agent)]
                                        (when-not equal?
                                          (println "[DELTA] Agent" (:id new-agent) "changed, equal?" equal?)
                                          [(str (:id new-agent)) (agent-delta old-agent new-agent)]))))
                            (filter identity)
                            (into {}))]
       (println "[DELTA] Returning delta with" (count changed-agents-result) "changed agents")
       {:tick (:tick new-world)
        :global-updates {:tick (:tick new-world)
                          :temperature (:temperature new-world)
                          :daylight (:daylight new-world)
                          :calendar (:calendar new-world)
                          :levers (:levers new-world)
                          :map (:map new-world)}
        :changed-agents changed-agents-result
        :changed-tiles (map-delta (:tiles old-world) (:tiles new-world))
        :changed-items (map-delta (:items old-world) (:items new-world))
        :changed-stockpiles (map-delta (:stockpiles old-world) (:stockpiles new-world))
        :changed-jobs (map-delta (:jobs old-world) (:jobs new-world))
        :changed-tile-visibility (map-delta (:tile-visibility old-world) (:tile-visibility new-world))
        :changed-agent-visibility (map-delta (:agent-visibility old-world {}) (:agent-visibility new-world))
        :changed-revealed-tiles-snapshot (map-delta (:revealed-tiles-snapshot old-world {}) (:revealed-tiles-snapshot new-world))})))

(defn apply-delta-to-agent
  "Apply delta to a single agent, handling removal."
  [agent delta]
  (cond
    (:removed delta) nil
    (nil? delta) agent
    :else (merge agent delta)))

(defn apply-agent-deltas
  "Apply agent deltas to agent list."
  [agents agent-deltas]
  (let [delta-map (->> agent-deltas
                       (map (fn [[id delta]] [id delta]))
                       (into {}))
        delta-ids (set (keys delta-map))
        removed-ids (->> (vals delta-map)
                         (filter :removed)
                         (map :id)
                         set)
        filter-agent (fn [agent]
                       (or (not (contains? removed-ids (:id agent)))
                           (contains? delta-ids (str (:id agent)))))
        updated-agents (->> agents
                             (map (fn [agent]
                                    (let [delta (get delta-map (str (:id agent)))]
                                      (apply-delta-to-agent agent delta))))
                             (filter filter-agent)
                             vec)
        updated-ids (set (map :id updated-agents))
        to-add (->> (vals delta-map)
                     (filter #(and (not (contains? updated-ids (:id %)))
                                   (not (:removed %))))
                     vec)]
    (vec (concat updated-agents to-add))))

(defn merge-world-delta
  "Apply delta to world state, returning updated world."
  [world delta]
  (-> world
      (update :agents #(apply-agent-deltas % (:changed-agents delta)))
      (update :tiles merge (:changed-tiles delta))
      (update :items merge (:changed-items delta))
      (update :stockpiles merge (:changed-stockpiles delta))
      (assoc :tick (:tick delta))
      (assoc :temperature (get-in delta [:global-updates :temperature]))
      (assoc :daylight (get-in delta [:global-updates :daylight]))
      (assoc :calendar (get-in delta [:global-updates :calendar]))
      (assoc :levers (get-in delta [:global-updates :levers]))
      (assoc :map (get-in delta [:global-updates :map]))
      (update :tile-visibility merge (:changed-tile-visibility delta))
      (update :agent-visibility merge (:changed-agent-visibility delta))
      (update :revealed-tiles-snapshot merge (:changed-revealed-tiles-snapshot delta))))
