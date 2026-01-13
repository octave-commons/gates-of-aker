(ns fantasia.sim.facets)

(defn clamp01 ^double [^double x]
  (cond
    (neg? x) 0.0
    (> x 1.0) 1.0
    :else x))

(defn bump-facet
  "Add delta to facet activation in an agent frontier."
  [frontier facet delta]
  (let [{:keys [a strength valence]} (get frontier facet {:a 0.0 :strength 0.2 :valence 0.0})
        a' (clamp01 (+ (double a) (double delta)))]
    (assoc frontier facet {:a a' :strength strength :valence valence})))

(defn decay-frontier
  "Decay activations each tick. Keep sparse frontier by dropping tiny activations."
  [frontier {:keys [decay drop-threshold] :or {decay 0.92 drop-threshold 0.02}}]
  (->> frontier
       (map (fn [[k {:keys [a strength valence]}]]
              (let [a' (* (double a) (double decay))]
                [k {:a a' :strength strength :valence valence}])))
       (remove (fn [[_ {:keys [a]}]] (< (double a) (double drop-threshold))))
       (into {})))

(defn seed
  "Seed facets directly from a packet bundle."
  [frontier facets {:keys [seed-strength] :or {seed-strength 0.28}}]
  (reduce (fn [fr f] (bump-facet fr f seed-strength))
          frontier
          facets))

(defn spread-step
  "One hop of spreading along weighted edges.
  edges is a map {[from to] w} with w in [0..1]."
  [frontier edges {:keys [spread-gain max-hops] :or {spread-gain 0.55 max-hops 2}}]
  (loop [hop 0
         fr frontier
         deltas []]
    (if (>= hop max-hops)
      {:frontier fr :deltas deltas}
      (let [active (sort-by (fn [[_ {:keys [a]}]] (- (double a))) fr)
            ;; small frontier = safe; take top 24 active facets
            active (take 24 active)
            step (reduce
                   (fn [{:keys [fr deltas]} [from {:keys [a]}]]
                     (reduce
                       (fn [{:keys [fr deltas]} [[f t] w]]
                         (if (= f from)
                           (let [delta (* (double a) (double w) (double spread-gain))
                                 fr' (bump-facet fr t delta)]
                             {:fr fr'
                              :deltas (conj deltas {:from f :to t :w w :delta delta})})
                           {:fr fr :deltas deltas}))
                       {:fr fr :deltas deltas}
                       edges))
                   {:fr fr :deltas deltas}
                   active)]
        (recur (inc hop) (:fr step) (:deltas step))))))

(defn event-recall
  "Compute recall activation for an event from a facet signature.
  signature: {facet weight}. Returns a normalized score in [0,1]."
  [frontier signature {:keys [threshold] :or {threshold 0.70}}]
  (let [raw (reduce
              (fn [acc [facet w]]
                (let [a (double (get-in frontier [facet :a] 0.0))]
                  (+ acc (* a (double w)))))
              0.0
              signature)
        total (max 1.0e-9 (reduce + 0.0 (map (comp double val) signature)))
        score (clamp01 (/ raw total))]
    {:score score
     :recalled? (>= score (double threshold))}))
