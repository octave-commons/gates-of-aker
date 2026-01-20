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
         [w3 agents1] (loop [w w2
                               agents (:agents w2)
                               acc-w w2
                               acc-a []]
                          (if (empty? agents)
                            [acc-w acc-a]
                            (let [[w' a'] (movement/move-agent-with-job w (first agents))
                                  a'' (agents/update-needs w' a')]
                              (recur w' (rest agents) w' (conj acc-a a'')))))
        ev (runtime/generate w3 agents1)
       ev-step (if ev
                 (reduce
                   (fn [{:keys [agents mentions traces]} a]
                     (if (contains? (set (:witnesses ev)) (:id a))
                       (let [res (runtime/apply-to-witness w3 a ev)]
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
                    (let [packet (agents/choose-packet w3 speaker)
                          res (agents/apply-packet-to-listener w3 listener speaker packet)
                          agents' (assoc agents (:id listener) (:listener res))]
                      {:agents agents'
                       :mentions (into mentions (:mentions res))
                       :traces (into traces (:traces res))}))
                 {:agents (vec agents2)
                  :mentions (:mentions ev-step)
                  :traces (:traces ev-step)}
                 pairs)
       agents3 (:agents talk-step)
       bcasts (institutions/broadcasts w3)
       inst-step (reduce
                  (fn [{:keys [agents mentions traces]} b]
                    (let [res (institutions/apply-broadcast w3 agents b)]
                      {:agents (:agents res)
                       :mentions (into mentions (:mentions res))
                       :traces (into traces (:traces res))}))
                 {:agents agents3
                  :mentions (:mentions talk-step)
                  :traces (:traces talk-step)}
                 bcasts)
       agents4 (:agents inst-step)
       ledger-info (world/update-ledger w3 (:mentions inst-step))
       ledger2 (:ledger ledger-info)
       attr (:attribution ledger-info)
       recent' (if ev
                 (->> (concat (:recent-events w3)
                              [(select-keys ev [:id :type :tick :pos :impact :witness-score :witnesses])])
                      (take-last (:recent-max w3))
                      vec)
                 (:recent-events w3))
       traces' (->> (concat (:traces w3) (:traces inst-step))
                    (take-last (:trace-max w3))
                    vec)
       world' (-> w3
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

(defn next-agent-id []
  (if-let [agents (:agents (get-state))]
    (if (empty? agents)
      0
      (inc (apply max (map :id agents))))
    0))

(defn spawn-wolf! [world pos]
  (let [agent-id (next-agent-id)
        [q r] pos]
    (update world :agents conj 
            {:id agent-id
             :pos [q r]
             :role :wolf
             :stats initial/default-agent-stats
             :needs initial/default-agent-needs
             :need-thresholds initial/default-need-thresholds
             :inventories {:personal {:wood 0 :food 0}
                           :hauling {}
                           :equipment {}}
             :status {:alive? true :asleep? false :idle? false}
             :inventory {:wood 0 :food 0}
             :frontier {}
             :recall {}
             :events []})))

(defn spawn-bear! [world pos]
  (let [agent-id (next-agent-id)
        [q r] pos]
    (update world :agents conj 
            {:id agent-id
             :pos [q r]
             :role :bear
             :stats initial/default-agent-stats
             :needs initial/default-agent-needs
             :need-thresholds initial/default-need-thresholds
             :inventories {:personal {:wood 0 :food 0}
                           :hauling {}
                           :equipment {}}
             :status {:alive? true :asleep? false :idle? false}
             :inventory {:wood 0 :food 0}
             :frontier {}
             :recall {}
             :events []})))

(defn get-agent-path! [agent-id]
  (let [world (get-state)
        agent (some #(when (= (:id %) agent-id) %) (:agents world))]
    (when agent
      (:path agent))))
