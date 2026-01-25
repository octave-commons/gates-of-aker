(ns fantasia.sim.institutions-test
  (:require [clojure.test :refer [deftest is testing]]
            [fantasia.sim.institutions :as inst]
            [fantasia.sim.core :as core]))

(deftest broadcasts-fire-on-cadence
  (let [world (core/initial-world 1)
        world-with-tick (assoc world :tick 6)
        due (inst/broadcasts world-with-tick)
        not-due (inst/broadcasts (assoc world :tick 5))
        broadcast (first due)]
    (is (= 1 (count due)))
    (is (= :temple (:institution broadcast)))
    (is (= [:fire :judgment :winter]
           (get-in broadcast [:packet :facets])))
    (is (empty? not-due))))

(deftest broadcasts-respect-empty-canonical-data
  (let [world (assoc (core/initial-world 3)
                     :institutions {:oracle {:id :oracle
                                             :broadcast-every 1
                                             :canonical {}}})
        due (inst/broadcasts (assoc world :tick 1))]
    (is (= [:oracle] (map :institution due)))
    (is (= [] (get-in (first due) [:packet :facets])))))

(deftest apply-broadcast-uses-mouthpiece
  (let [world (-> (core/initial-world 2)
                  (assoc-in [:levers :mouthpiece-agent-id] 1))
        agents (:agents world)
        broadcast (first (inst/broadcasts (assoc world :tick 6)))
        res (inst/apply-broadcast world agents broadcast)]
    (is (= (count agents) (count (:agents res))))
    (is (vector? (:mentions res)))
    (is (vector? (:traces res)))))

(deftest apply-broadcast-defaults-to-institution-speaker
  (let [world (core/initial-world 4)
        agents (:agents world)
        broadcast {:institution :oracle
                   :packet {:intent :convert :facets [:fate] :tone {} :claim-hint nil}}
        res (inst/apply-broadcast world agents broadcast)]
    (is (= (count agents) (count (:agents res))))
    (is (every? #(= :oracle (:speaker (:mention %))) (:traces res)))))
