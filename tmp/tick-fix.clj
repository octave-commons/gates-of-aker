(defn initial-world [opts]
  (let [{:keys [seed bounds]} opts
        actual-seed (or seed 1)
        r (rng actual-seed)
        hex-bounds (hex/normalize-bounds bounds {:shape :rect :w 40 :h 40})
        hex-map {:kind :hex
                  :layout :pointy
                  :bounds hex-bounds}]
    {:seed actual-seed
      :tick 0
      :map hex-map
      :tiles (reduce (fn [m k]
                     (if (= k "0,0")
                       m
                       (assoc-in m [k :resource] :food)))
                   {}
                   (for [q (range (:w (:bounds hex-bounds)))
                         r (range (:h (:bounds hex-bounds)))]
                       :let [k (str q "," r)]))
      :items {}
      :shrine nil
      :cold-snap 0.85
      :jobs []
      :stockpiles {}
      :levers {:iconography {:fire->patron 0.80
                              :lightning->storm 0.75
                              :storm->deity 0.85}
                 :mouthpiece-agent-id nil}
      :institutions
      {:temple {:id :temple
                 :name "Temple of Embers"
                 :entropy 0.2
                 :broadcast-every 6
                 :canonical {:facets [:fire :judgment :winter]
                             :claim-hint :claim/winter-judgment-flame}}}
      :agents (vec (for [i (range 12)]
                      (let [[q r] (hex/rand-pos r hex-map)]
                        (->agent i q r (cond
                                            (= i 0) :priest
                                            (= i 1) :knight
                                            :else :peasant)))))
      :edges {[:cold :fire] 0.60
              [:trees :fire] 0.45
              [:lightning :storm] 0.70
              [:storm :deity/storm] 0.80
              [:fire :patron/fire] 0.80
              [:patron/fire :judgment] 0.35
              [:deity/storm :awe] 0.25
              [:judgment :awe] 0.25}
      :ledger {}
      :recent-events []
      :recent-max 30
      :traces []
      :trace-max 250}})
