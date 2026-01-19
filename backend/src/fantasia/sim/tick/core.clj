(ns fantasia.sim.tick.core
  (:require [fantasia.sim.agents :as agents]
            [fantasia.sim.events.runtime :as runtime]
            [fantasia.sim.institutions :as institutions]
            [fantasia.sim.world :as world]
            [fantasia.sim.jobs :as jobs]
            [fantasia.sim.hex :as hex]
            [fantasia.sim.pathing]
            [fantasia.sim.tick.initial :as initial]
            [fantasia.sim.tick.trees :as trees]
            [fantasia.sim.tick.movement :as movement]
            [fantasia.sim.tick.mortality :as mortality]))

(def ^:dynamic *state (atom (initial/initial-world 1)))

(defn get-state [] @*state)

(defn reset-world!
  ([] (reset-world! {}))
  ([opts]
   (clojure.core/reset! *state (initial/initial-world opts))))

(defn set-levers! [levers]
  (swap! *state update :levers merge levers))

(defn place-shrine! [pos]
  (swap! *state assoc :shrine pos))

(defn appoint-mouthpiece! [agent-id]
  (swap! *state assoc-in [:levers :mouthpiece-agent-id] agent-id))

(defn ^:dynamic tick-once [world]
    (let [t (inc (:tick world))
           w1 (assoc world :tick t)
           w2 (-> w1
                    (trees/spread-trees!)
                    (mortality/process-mortality!)
                    (jobs/auto-generate-jobs!)
                    (jobs/auto-assign-jobs!)
                    (trees/drop-tree-fruits!))
         agents1 (->> (:agents w2)
                      (map (fn [a]
                             (let [[w2' a'] (movement/move-agent-with-job w2 a)]
                               (agents/update-needs w2' a'))))
                      vec)
       ev (runtime/generate w2 agents1)
      ev-step (if ev
                (reduce
                  (fn [{:keys [agents mentions traces]} a]
                    (if (contains? (set (:witnesses ev)) (:id a))
                      (let [res (runtime/apply-to-witness w2 a ev)]
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
                   (let [packet (agents/choose-packet w2 speaker)
                         res (agents/apply-packet-to-listener w2 listener speaker packet)
                         agents' (assoc agents (:id listener) (:listener res))]
                     {:agents agents'
                      :mentions (into mentions (:mentions res))
                      :traces (into traces (:traces res))}))
                {:agents (vec agents2)
                 :mentions (:mentions ev-step)
                 :traces (:traces ev-step)}
                pairs)
      agents3 (:agents talk-step)
      bcasts (institutions/broadcasts w2)
      inst-step (reduce
                 (fn [{:keys [agents mentions traces]} b]
                   (let [res (institutions/apply-broadcast w2 agents b)]
                     {:agents (:agents res)
                      :mentions (into mentions (:mentions res))
                      :traces (into traces (:traces res))}))
                {:agents agents3
                 :mentions (:mentions talk-step)
                 :traces (:traces talk-step)}
                bcasts)
      agents4 (:agents inst-step)
      ledger-info (world/update-ledger w2 (:mentions inst-step))
      ledger2 (:ledger ledger-info)
      attr (:attribution ledger-info)
      recent' (if ev
                (->> (concat (:recent-events w2)
                             [(select-keys ev [:id :type :tick :pos :impact :witness-score :witnesses])])
                     (take-last (:recent-max w2))
                     vec)
                (:recent-events w2))
      traces' (->> (concat (:traces w2) (:traces inst-step))
                   (take-last (:trace-max w2))
                   vec)
      world' (-> w2
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

(defn tick! [n]
  (loop [i 0
         outs []]
    (if (>= i n)
      outs
      (let [{:keys [world out]} (tick-once (get-state))]
        (clojure.core/reset! *state world)
        (recur (inc i) (conj outs out))))))
