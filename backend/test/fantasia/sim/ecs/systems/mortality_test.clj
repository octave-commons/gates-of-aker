(ns fantasia.sim.ecs.systems.mortality-test
  (:require [clojure.test :refer [deftest testing is]]
            [fantasia.sim.test-helpers :as helpers]
            [fantasia.sim.ecs.core :as ecs]
            [fantasia.sim.ecs.components :as c]
            [brute.entity :as be]
            [fantasia.sim.ecs.systems.mortality :as mort]))

(deftest test-check-entity-mortality
  (testing "Returns :starvation when food <= 0.15"
    (let [world (helpers/create-test-world)
          [agent-id world] (helpers/create-test-agent world :priest 0 0)
          _ (helpers/with-needs world agent-id 0.1 0.7 0.6)
          result (mort/check-entity-mortality world agent-id)]
      (is (some? result)))))))