(ns fantasia.sim.gates.kernel)

(def gate-required-keys
  #{:id :priority :trigger :enforce})

(defn gate?
  [gate]
  (and (map? gate)
       (every? #(contains? gate %) gate-required-keys)
       (keyword? (:id gate))
       (number? (:priority gate))
       (fn? (:trigger gate))
       (fn? (:enforce gate))))

(defn register-gate
  [registry gate]
  (when-not (gate? gate)
    (throw (ex-info "Invalid gate contract" {:gate gate})))
  (assoc registry (:id gate) gate))

(defn build-registry
  [gates]
  (reduce register-gate {} gates))

(defn has-signal?
  [signals signal]
  (contains? signals signal))

(defn- gate-order-key
  [gate]
  [(- (:priority gate)) (str (:id gate))])

(defn- ordered-gates
  [registry]
  (->> (vals registry)
       (sort-by gate-order-key)
       vec))

(defn- empty-decision-record
  [signals]
  {:signals signals
   :triggered-gates []
   :obligations []
   :prohibitions []
   :formatting-constraints []
   :conflicts []})

(defn- constraint-order-key
  [constraint]
  [(str (:source-gate-id constraint)) (str (:id constraint))])

(defn- normalize-constraint
  [gate kind constraint]
  (assoc constraint
         :kind kind
         :source-gate-id (:id gate)
         :source-priority (:priority gate)))

(defn- conflict-record
  [kind current challenger]
  {:kind kind
   :constraint-id (:id current)
   :winner-gate-id (:source-gate-id current)
   :winner-priority (:source-priority current)
   :loser-gate-id (:source-gate-id challenger)
   :loser-priority (:source-priority challenger)
   :winner-value (:value current)
   :loser-value (:value challenger)})

(defn- challenger-wins?
  [current challenger]
  (let [c-priority (:source-priority challenger)
        w-priority (:source-priority current)]
    (or (> c-priority w-priority)
        (and (= c-priority w-priority)
             (neg? (compare (str (:source-gate-id challenger))
                            (str (:source-gate-id current))))))))

(defn- merge-constraint
  [constraints conflicts kind gate raw-constraint]
  (let [constraint (normalize-constraint gate kind raw-constraint)
        key (:id constraint)]
    (if-let [current (get constraints key)]
      (if (= current constraint)
        [constraints conflicts]
        (if (challenger-wins? current constraint)
          [(assoc constraints key constraint)
           (conj conflicts (conflict-record kind constraint current))]
          [constraints
           (conj conflicts (conflict-record kind current constraint))]))
      [(assoc constraints key constraint) conflicts])))

(defn- merge-constraints
  [decision gate kind constraints]
  (let [{existing kind
         :keys [conflicts]} decision
        by-id (into {} (map (juxt :id identity) existing))
        [next-by-id next-conflicts]
        (reduce (fn [[acc-constraints acc-conflicts] constraint]
                  (merge-constraint acc-constraints acc-conflicts kind gate constraint))
                [by-id conflicts]
                constraints)]
    (assoc decision
           kind (->> (vals next-by-id)
                     (sort-by constraint-order-key)
                     vec)
           :conflicts next-conflicts)))

(defn- enforce-gate
  [decision gate]
  (let [result ((:enforce gate) {:signals (:signals decision)
                                 :decision decision})
        obligations (or (:obligations result) [])
        prohibitions (or (:prohibitions result) [])
        formatting-constraints (or (:formatting-constraints result) [])]
    (-> decision
        (update :triggered-gates conj {:id (:id gate)
                                       :priority (:priority gate)})
        (merge-constraints gate :obligations obligations)
        (merge-constraints gate :prohibitions prohibitions)
        (merge-constraints gate :formatting-constraints formatting-constraints))))

(defn evaluate
  [registry signals]
  (let [triggered (->> (ordered-gates registry)
                       (filter #((:trigger %) signals))
                       vec)]
    (reduce enforce-gate
            (empty-decision-record signals)
            triggered)))

(defn explain-conflict
  [conflict]
  (str "Constraint " (pr-str (:constraint-id conflict))
       " kept gate " (pr-str (:winner-gate-id conflict))
       " over " (pr-str (:loser-gate-id conflict))
       " for " (name (:kind conflict))))
