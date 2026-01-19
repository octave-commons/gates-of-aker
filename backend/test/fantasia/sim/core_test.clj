(ns fantasia.sim.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [fantasia.sim.core :as core]
            [fantasia.sim.spatial :as spatial]
            [fantasia.sim.institutions :as inst]
            [fantasia.sim.world :as world]))

(defn approx= [expected actual]
  (<= (Math/abs (- (double expected) (double actual))) 1.0e-6))

(defn synthetic-world []
  (let [inst {:id :temple
              :name "Temple"
              :entropy 0.1
              :broadcast-every 2
              :canonical {:facets [:fire :judgment]
                          :claim-hint :claim/winter-judgment-flame}}]
    {:seed 42
     :tick 0
     :map {:kind :hex :layout :pointy :bounds {:shape :rect :w 6 :h 6}}
     :tiles {"1,1" {:terrain :ground :resource :tree}}
     :shrine [2 2]
     :cold-snap 0.4
     :levers {:iconography {:fire->patron 0.8
                             :lightning->storm 0.7
                             :storm->deity 0.9}
               :mouthpiece-agent-id nil}
     :institutions {:temple inst}
     :agents [(core/->agent 0 1 1 :priest)
              (core/->agent 1 3 3 :peasant)]
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
     :recent-max 10
     :traces []
     :trace-max 20}))

(deftest initial-world-structure
  (let [world (core/initial-world {:seed 99})
        ids (map :id (:agents world))]
    (is (= 99 (:seed world)))
    (is (= :hex (get-in world [:map :kind])))
    (is (= :pointy (get-in world [:map :layout])))
    (is (map? (:map world)))
    (is (map? (:tiles world)))
    (is (= 12 (count ids)))
    (is (= (set (range 12)) (set ids)))
    (is (every? #(spatial/in-bounds? world (:pos %)) (:agents world)))))



(deftest apply-institution-broadcast-respects-mouthpiece
  (let [world (-> (core/initial-world {:seed 9})
                  (assoc-in [:levers :mouthpiece-agent-id] 1))
        agents (:agents world)
        broadcast (first (inst/broadcasts (assoc world :tick 6)))
        res (inst/apply-broadcast world agents broadcast)]
    (is (= (count agents) (count (:agents res))))
    (is (vector? (:mentions res)))
    (is (vector? (:traces res)))))

(deftest snapshot-summarizes-world
  (let [world (core/initial-world {:seed 10})
        snap (world/snapshot world {:winter 1.0})
        agent (first (:agents world))
        agent-snap (first (:agents snap))]
    (is (= (:tick world) (:tick snap)))
    (is (= (:levers world) (:levers snap)))
    (is (vector? (:agents snap)))
    (is (= (:current-job agent) (:current-job agent-snap)))
    (is (= (:idle? agent) (:idle? agent-snap)))
    (is (map? (:ledger snap)))))

(deftest tick-once-advances-synthetic-world
  (let [world0 (synthetic-world)
        {:keys [world out]} (core/tick-once world0)]
    (is (= 1 (:tick world)))
    (is (= 1 (:tick out)))
    (is (= 1 (get-in out [:snapshot :tick])))
    (is (<= (count (:recent-events world)) (:recent-max world)))
    (is (<= (count (:traces world)) (:trace-max world)))))

(deftest tick-batch-produces-events
  (core/reset-world! {:seed 1})
  (try
    (let [outs (core/tick! 40)
          final (core/get-state)]
      (is (= 40 (count outs)))
      (is (= 40 (:tick final)))
      (is (some :event outs)))
    (finally
      (core/reset-world! {:seed 1}))))

(deftest institution-broadcasts-trigger-on-schedule
  (let [world (core/initial-world 7)
        due (inst/broadcasts (assoc world :tick 6))
        not-due (inst/broadcasts (assoc world :tick 5))
        broadcast (first due)]
    (is (= 1 (count due)))
    (is (= :temple (:institution broadcast)))
    (is (= [:fire :judgment :winter] (get-in broadcast [:packet :facets])))
    (is (empty? not-due))))

