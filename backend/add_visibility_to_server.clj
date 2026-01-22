(defn get-visible-tiles
  "Return only visible or revealed tiles from state."
  [state]
  (let [tile-visibility (:tile-visibility state {})]
    (if (empty? tile-visibility)
      (:tiles state)
      (into {}
            (filter (fn [[tile-key]]
                      (let [vis (get tile-visibility tile-key :hidden)]
                        (or (= vis :visible) (= vis :revealed))))
                  (:tiles state)))))

(defn handle-ws [req]
  (http/with-channel req ch
    (swap! *clients conj ch)
    (ws-send! ch {:op "hello"
                  :state (merge (select-keys (sim/get-state) [:tick :shrine :levers :map :agents :tile-visibility :revealed-tiles-snapshot])
                                {:tiles (get-visible-tiles (sim/get-state))})})
    (http/on-close ch (fn [_] (swap! *clients disj ch))))
