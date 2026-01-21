(ns fantasia.sim.tick.core
  (:require [fantasia.sim.agents :as agents]
            [fantasia.sim.social :as social]
            [fantasia.sim.events.runtime :as runtime]
            [fantasia.sim.institutions :as institutions]
            [fantasia.sim.world :as world]
             [fantasia.sim.jobs :as jobs]
             [fantasia.sim.jobs.providers :as job-providers]
             [fantasia.sim.hex :as hex]
             [fantasia.sim.pathing]
             [fantasia.sim.time :as sim-time]
             [fantasia.sim.tick.initial :as initial]
             [fantasia.sim.tick.combat :as combat]
             [fantasia.sim.tick.trees :as trees]
             [fantasia.sim.tick.movement :as movement]
             [fantasia.sim.tick.mortality :as mortality]
             [fantasia.sim.tick.food-decay :as food-decay]
             [fantasia.sim.reproduction :as reproduction]
             [fantasia.sim.houses :as houses]
             [fantasia.sim.constants :as const]))

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
          temperature (sim-time/temperature-at (:seed world) t)
          daylight (sim-time/daylight-at (:seed world) t)
          cold-snap (sim-time/clamp01 (- 1.0 temperature))
          w1 (-> world
                 (assoc :tick t)
                 (assoc :temperature temperature)
                 (assoc :daylight daylight)
                 (assoc :cold-snap cold-snap))
           w1 (assoc w1 :calendar (sim-time/calendar-info w1))
            w2 (-> w1
                     (trees/spread-trees!)
                     (job-providers/auto-generate-jobs!)
                     (jobs/auto-assign-jobs!)
                     (trees/drop-tree-fruits!)
                     (food-decay/decay-food!))
         [w3 agents1] (loop [w w2
                             agents (:agents w2)
                             acc-w w2
                             acc-a []]
                        (if (empty? agents)
                          [acc-w acc-a]
                          (let [[w' a'] (movement/move-agent-with-job w (first agents))
                                a'' (agents/update-needs w' a')]
                            (recur w' (rest agents) w' (conj acc-a a'')))))
         w3 (assoc w3 :agents agents1)
         [w4 combat-events] (combat/process-combat! w3)
         w4 (-> w4
                (jobs/cleanup-hunt-jobs!)
                (mortality/process-mortality!))
         agents2 (:agents w4)
        ev (runtime/generate w4 agents2)
        ev-step (if ev
                  (reduce
                    (fn [{:keys [agents mentions traces]} a]
                      (if (contains? (set (:witnesses ev)) (:id a))
                        (let [res (runtime/apply-to-witness w4 a ev)]
                          {:agents (conj agents (:agent res))
                           :mentions (into mentions (:mentions res))
                           :traces (into traces (:traces res))})
                        {:agents (conj agents a)
                         :mentions mentions
                         :traces traces}))
                   {:agents [] :mentions [] :traces []}
                   agents2)
                 {:agents agents2 :mentions [] :traces []})
        agents3 (:agents ev-step)
        player-agents (filter #(= (:faction %) :player) agents3)
        pairs (agents/interactions player-agents)
        talk-step (reduce
                   (fn [{:keys [world agents mentions traces social-interactions]} [speaker listener]]
                      (let [speaker' (get agents (:id speaker) speaker)
                            listener' (get agents (:id listener) listener)
                            social-step (social/trigger-social-interaction! world speaker' listener')
                            world' (:world social-step)
                            agent-1 (:agent-1 social-step)
                            agent-2 (:agent-2 social-step)
                            interaction (:interaction social-step)
                            agents' (-> agents
                                        (assoc (:id agent-1) agent-1)
                                        (assoc (:id agent-2) agent-2))
                            packet (agents/choose-packet world' agent-1)
                            res (agents/apply-packet-to-listener world' agent-2 agent-1 packet)
                            agents'' (assoc agents' (:id agent-2) (:listener res))]
                        {:world world'
                         :agents agents''
                         :mentions (into mentions (:mentions res))
                         :traces (into traces (:traces res))
                         :social-interactions (into social-interactions [interaction])}))
                  {:world w4
                   :agents (vec agents3)
                   :mentions (:mentions ev-step)
                   :traces (:traces ev-step)
                   :social-interactions []}
                  pairs)
        agents4 (:agents talk-step)
        w5 (:world talk-step)
        reproduction-step (reduce
                           (fn [{:keys [agents next-agent-id] :as acc} [parent1 parent2]]
                             (let [world-with-next-id (assoc w5 :next-agent-id next-agent-id)
                                   can-reproduce? (reproduction/can-reproduce? world-with-next-id parent1 parent2)]
                              (if can-reproduce?
                                (let [{:keys [child-agent next-agent-id]} (reproduction/create-child-agent world-with-next-id parent1 parent2 t)
                                      agents' (conj agents child-agent)
                                      parents (vec agents')
                                      parent1-updated (assoc parents (:id parent1) (assoc parent1 :carrying-child (:id child-agent)))]
                                  {:agents parent1-updated :next-agent-id next-agent-id})
                                acc)))
                          {:agents agents4 :next-agent-id (or (:next-agent-id w4) 0)}
                          pairs)
       agents5 (:agents reproduction-step)
       growth-step (reduce
                    (fn [agents agent]
                      (if (:child-stage agent)
                        (let [[updated-agent new-stage released?] (reproduction/advance-child-growth agent t)
                              id (:id agent)
                              agents' (assoc agents id updated-agent)]
                          (if (and released? (not= :infant new-stage))
                            (let [parent-id (get-in agent [:parent-ids 0])]
                              (if parent-id
                                (update-in agents' [parent-id :carrying-child] (fn [x] (when (= x id) nil)))
                                agents'))
                            agents'))
                        agents))
                    agents5
                    (:agents reproduction-step))
       agents6 growth-step
       housing-step (reduce
                       (fn [{:keys [agents world] :as acc} agent]
                         (let [rest-need (get-in agent [:needs :rest] 0.5)
                               is-tired? (< rest-need 0.3)
                               has-house? (get agent :house-id nil)]
                           (if (and is-tired? (not has-house?))
                             (let [nearby-house (houses/find-nearby-house-with-empty-bed world (:pos agent) 10)]
                               (if nearby-house
                                 (let [world' (houses/assign-agent-to-house world (:id agent) nearby-house)
                                       agent' (assoc agent :house-id nearby-house)]
                                   {:agents (assoc agents (:id agent) agent')
                                    :world world'})
                                 acc))
                             acc)))
                        {:agents agents6 :world w5}
                        agents6)
        agents7 (:agents housing-step)
        bcasts (institutions/broadcasts w5)
          inst-step (reduce
                     (fn [{:keys [agents mentions traces]} b]
                       (let [res (institutions/apply-broadcast w5 agents b)]
                         {:agents (:agents res)
                          :mentions (into mentions (:mentions res))
                          :traces (into traces (:traces res))}))
                   {:agents agents7
                     :mentions (:mentions talk-step)
                     :traces (:traces talk-step)}
                   bcasts)
        agents8 (:agents inst-step)
        ledger-info (world/update-ledger w5 (:mentions inst-step))
         ledger2 (:ledger ledger-info)
         attr (:attribution ledger-info)
         recent' (if ev
                   (->> (concat (:recent-events w5)
                                [(select-keys ev [:id :type :tick :pos :impact :witness-score :witnesses])])
                          (take-last (:recent-max w5))
                         vec)
                   (:recent-events w5))
         traces' (->> (concat (:traces w5) (:traces inst-step))
                       (take-last (:trace-max w5))
                       vec)
         world' (-> w5
                    (assoc :agents agents8)
                    (assoc :next-agent-id (:next-agent-id reproduction-step))
                   (assoc :ledger ledger2)
                   (assoc :recent-events recent')
                   (assoc :traces traces'))]
      {:world world'
      :out {:tick t
            :event ev
            :combat-events combat-events
            :mentions (:mentions inst-step)
            :traces (:traces inst-step)
            :attribution attr
            :social-interactions (:social-interactions talk-step)
            :books (:books w5)
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
             :name (initial/agent-name agent-id :wolf)
             :pos [q r]
             :role :wolf
             :faction :wilderness
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
              :events []
              :voice (initial/generate-voice agent-id)})))

(defn spawn-deer! [world pos]
  (let [agent-id (next-agent-id)
        [q r] pos]
    (update world :agents conj
            {:id agent-id
             :name (initial/agent-name agent-id :deer)
             :pos [q r]
             :role :deer
             :faction :wilderness
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
              :events []
              :voice (initial/generate-voice agent-id)})))

(defn spawn-bear! [world pos]
  (let [agent-id (next-agent-id)
        [q r] pos]
    (update world :agents conj 
            {:id agent-id
             :name (initial/agent-name agent-id :bear)
             :pos [q r]
             :role :bear
             :faction :wilderness
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
              :events []
              :voice (initial/generate-voice agent-id)})))

(defn get-agent-path! [agent-id]
  (let [world (get-state)
        agent (some #(when (= (:id %) agent-id) %) (:agents world))]
    (when agent
      (:path agent))))
