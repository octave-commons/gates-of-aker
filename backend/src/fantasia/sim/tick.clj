(ns fantasia.sim.tick
  (:require [fantasia.sim.agents :as agents]
            [fantasia.sim.events.runtime :as runtime]
            [fantasia.sim.institutions :as institutions]
            [fantasia.sim.spatial :as spatial]
            [fantasia.sim.world :as world]))

(defn rng [seed] (java.util.Random. (long seed)))
(defn rand-int* [^java.util.Random r n] (.nextInt r (int n)))

(defn ->agent [id x y role]
  {:id id
   :pos [x y]
   :role role
   :needs {:warmth 0.6 :food 0.7 :sleep 0.7}
   :frontier {}
   :recall {}})

(defn initial-world [seed]
  (let [r (rng seed)]
    {:seed seed
     :tick 0
     :size [20 20]
     :trees (set (for [x (range 10 18)
                       y (range 2 10)]
                   [x y]))
     :shrine nil
     :cold-snap 0.85
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
                    (->agent i (rand-int* r 20) (rand-int* r 20)
                            (cond
                              (= i 0) :priest
                              (= i 1) :knight
                              :else :peasant))))
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
     :trace-max 250}))

(defn tick-once [world]
  (let [t (inc (:tick world))
        world (assoc world :tick t)
        agents1 (->> (:agents world)
                     (map (fn [a]
                            (agents/update-needs world
                                                 (spatial/move-agent world a))))
                     vec)
        ev (runtime/generate world agents1)
        ev-step (if ev
                  (reduce
                    (fn [{:keys [agents mentions traces]} a]
                      (if (contains? (set (:witnesses ev)) (:id a))
                        (let [res (runtime/apply-to-witness world a ev)]
                          {:agents (conj agents (:agent res))
                           :mentions (into mentions (:mentions res))
                           :traces (into traces (:traces res))})
                        {:agents (conj agents a)
                         :mentions mentions
                         :traces traces}))
                    {:agents [] :mentions [] :traces []}
                    agents1)
                  {:agents agents1 :mentions [] :traces []})
        agents2 (:agents ev-step)
        pairs (agents/interactions agents2)
        talk-step (reduce
                   (fn [{:keys [agents mentions traces]} [speaker listener]]
                     (let [packet (agents/choose-packet world speaker)
                           res (agents/apply-packet-to-listener world listener speaker packet)
                           agents' (assoc agents (:id listener) (:listener res))]
                       {:agents agents'
                        :mentions (into mentions (:mentions res))
                        :traces (into traces (:traces res))}))
                   {:agents (vec agents2)
                    :mentions (:mentions ev-step)
                    :traces (:traces ev-step)}
                   pairs)
        agents3 (:agents talk-step)
        bcasts (institutions/broadcasts world)
        inst-step (reduce
                   (fn [{:keys [agents mentions traces]} b]
                     (let [res (institutions/apply-broadcast world agents b)]
                       {:agents (:agents res)
                        :mentions (into mentions (:mentions res))
                        :traces (into traces (:traces res))}))
                   {:agents agents3
                    :mentions (:mentions talk-step)
                    :traces (:traces talk-step)}
                   bcasts)
        agents4 (:agents inst-step)
        ledger-info (world/update-ledger world (:mentions inst-step))
        ledger2 (:ledger ledger-info)
        attr (:attribution ledger-info)
        recent' (if ev
                  (->> (concat (:recent-events world)
                               [(select-keys ev [:id :type :tick :pos :impact :witness-score :witnesses])])
                       (take-last (:recent-max world))
                       vec)
                  (:recent-events world))
        traces' (->> (concat (:traces world) (:traces inst-step))
                     (take-last (:trace-max world))
                     vec)
        world' (-> world
                   (assoc :agents agents4)
                   (assoc :ledger ledger2)
                   (assoc :recent-events recent')
                   (assoc :traces traces'))]
    {:world world'
     :out {:tick t
           :event ev
           :mentions (:mentions inst-step)
           :traces (:traces inst-step)
           :attribution attr
           :snapshot (world/snapshot world' attr)}}))

(defonce *state (atom (initial-world 1)))

(defn get-state [] @*state)

(defn reset-world! [{:keys [seed] :or {seed 1}}]
  (clojure.core/reset! *state (initial-world seed)))

(defn set-levers! [levers]
  (swap! *state update :levers merge levers))

(defn place-shrine! [pos]
  (swap! *state assoc :shrine pos))

(defn appoint-mouthpiece! [agent-id]
  (swap! *state assoc-in [:levers :mouthpiece-agent-id] agent-id))

(defn tick! [n]
  (loop [i 0
         outs []]
    (if (>= i n)
      outs
      (let [{:keys [world out]} (tick-once (get-state))]
        (clojure.core/reset! *state world)
        (recur (inc i) (conj outs out))))))
