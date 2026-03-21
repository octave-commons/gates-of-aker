(ns fantasia.sim.ecs-tick-test
  (:require [clojure.test :refer [deftest is]]
            [fantasia.sim.ecs.adapter :as adapter]
            [fantasia.sim.ecs.tick :as sim]))

(deftest reset-world-seeds-fork-tales-roster-names
  (sim/reset-world! {:seed 1 :tree_density 0.05})
  (let [snapshot (adapter/ecs->snapshot (sim/get-ecs-world) (sim/get-state))
        names (set (keep :name (:agents snapshot)))]
    (is (contains? names "Duct"))
    (is (contains? names "Null"))
    (is (contains? names "Patch"))
    (is (contains? names "Sei"))
    (is (contains? names "莉津律宗利都"))))
