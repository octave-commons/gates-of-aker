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
                 [fantasia.sim.tick.witness :as witness]
                 [fantasia.sim.tick.social :as tick-social]
                 [fantasia.sim.tick.reproduction :as tick-reproduction]
                 [fantasia.sim.tick.housing :as tick-housing]
                 [fantasia.sim.reproduction :as reproduction]
                 [fantasia.sim.houses :as houses]
                 [fantasia.sim.los :as los]
                 [fantasia.sim.memories :as mem]
                 [fantasia.sim.constants :as const]))

(def ^:dynamic *state (atom (initial/initial-world 1)))
(def ^:dynamic *previous-state (atom nil))

(defn get-state [] @*state)
(defn get-previous-state [] @*previous-state)

(defn update-state! [new-world]
  (clojure.core/reset! *previous-state @*state)
  (clojure.core/reset! *state new-world))

(defn reset-world!
  ([] (reset-world! {}))
  ([opts]
   (clojure.core/reset! *state (initial/initial-world opts))))

(defn set-levers! [levers]
  (swap! *state update :levers merge levers))

(defn set-facet-limit! [limit]
  (swap! *state assoc-in [:levers :facet-limit] limit))

(defn set-vision-radius! [radius]
  (swap! *state assoc-in [:levers :vision-radius] radius))

(defn place-shrine! [pos]
  (swap! *state assoc :shrine pos))

(defn appoint-mouthpiece! [agent-id]
   (swap! *state assoc-in [:levers :mouthpiece-agent-id] agent-id))

(defn process-memory-lifecycle!
  "Process memory decay and cleanup each tick."
  [world]
  (-> world
      (mem/decay-memories!)
      (mem/clean-expired-memories! 0.05)))

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
               (jobs/generate-missing-structures-jobs!)
               (jobs/auto-assign-jobs!)
               (trees/drop-tree-fruits!)
               (food-decay/decay-food!))
         [w3 agents1] (loop [w w2
                             agents (:agents w2)
                             acc-w w2
                             acc-a []]
                        (if (empty? agents)
                          [(assoc-in acc-w [:agents] (vec acc-a)) (vec acc-a)]
                          (let [[w' a'] (movement/move-agent-with-job acc-w (first agents))
                                a'' (agents/update-needs w' a')
                                w'' (assoc-in w' [:agents (:id a'')] a'')]
                            (recur w'' (rest agents) w'' (conj acc-a a'')))))
        [w4 combat-events] (combat/process-combat! w3)
        w4 (-> w4
               (jobs/cleanup-hunt-jobs!)
               (mortality/process-mortality!)
               (process-memory-lifecycle!))
        agents2 (:agents w4)
        ev (runtime/generate w4 agents2)
        ev-step (witness/process-witness-step! w4 agents2 ev)
        agents3 (:agents ev-step)
        player-agents (filter #(= (:faction %) :player) agents3)
        pairs (agents/interactions player-agents)
        talk-step (tick-social/process-social-step! w4 agents3 (:mentions ev-step) (:traces ev-step) pairs)
        agents4 (:agents talk-step)
        w5 (assoc-in (:world talk-step) [:agents] agents4)
        reproduction-step (tick-reproduction/process-reproduction-step! w5 agents4 (or (:next-agent-id w4) 0) pairs t)
        agents5 (:agents reproduction-step)
        growth-step (tick-reproduction/process-growth-step! agents5 t)
        agents6 growth-step
        housing-step (tick-housing/process-housing-step! w5 agents6)
        agents7 (:agents housing-step)
        bcasts (institutions/broadcasts w5)
        inst-step (institutions/process-broadcasts! w5 agents7 (:mentions talk-step) (:traces talk-step) bcasts)
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
                      (take-last (or (:trace-max w5) 200))
                      vec)
        world' (-> w5
                   (assoc :agents agents8)
                   (assoc :next-agent-id (:next-agent-id reproduction-step))
                   (assoc :ledger ledger2)
                   (assoc :recent-events recent')
                   (assoc :traces traces'))
         visibility-update (los/update-tile-visibility! world')
         world'' (-> world'
                     (update :tile-visibility merge (:tile-visibility visibility-update))
                     (update :revealed-tiles-snapshot merge (:revealed-tiles-snapshot visibility-update)))]
    {:world world''
     :out {:tick t
           :event ev
           :combat-events combat-events
           :mentions (:mentions inst-step)
           :traces (:traces inst-step)
           :attribution attr
           :social-interactions (:social-interactions talk-step)
           :books (:books w5)
           :snapshot (world/snapshot world'' attr)
           :delta-snapshot (let [old-world (get-state)]
                             (update-state! world'')
                             (world/delta-snapshot old-world world'' attr))}}))

(defn tick! [n]
    (loop [i 0
            outs []]
      (if (>= i n)
        outs
        (let [out (tick-once (get-state))]
          (recur (inc i) (conj outs (:out out)))))))

(defn next-agent-id
  ([world]
   (if-let [agents (:agents world)]
     (if (empty? agents)
       0
       (inc (apply max (map :id agents))))
     0))
  ([]
   (if-let [agents (:agents (get-state))]
     (if (empty? agents)
       0
       (inc (apply max (map :id agents))))
     0)))

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
