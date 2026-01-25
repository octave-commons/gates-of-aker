(ns fantasia.sim.ecs.systems.agent-interaction-test
  (:require [clojure.test :refer [deftest testing is]]
            [fantasia.sim.ecs.core :as ecs]
            [fantasia.sim.ecs.components :as c]
            [brute.entity :as be]
            [fantasia.sim.hex :as hex]
            [fantasia.sim.ecs.systems.agent-interaction :as ai]))

(deftest test-nearby-agents
  (testing "Returns agents within vision radius"
    (let [world (ecs/create-ecs-world)
          agent1 (be/create-entity)
          world (be/add-component world agent1 (c/->Position 0 0))
          world (be/add-component world agent1 (c/->Role :priest))
          agent2 (be/create-entity)
          world (be/add-component world agent2 (c/->Position 0 1))
          world (be/add-component world agent2 (c/->Role :knight))
          agent3 (be/create-entity)
          world (be/add-component world agent3 (c/->Position 1 0))
          world (be/add-component world agent3 (c/->Role :peasant))]
      (let [nearby (ai/nearby-agents world [0 0] 2)]
        (is (= 3 (count nearby)))))))

(deftest test-speech-packet
  (testing "Generates packet with correct intent based on facets"
    (let [frontier {:facets {:warn 0.9}}
          packet (ai/choose-packet-for-speech frontier)]
      (is (= :warning (:intent packet))))))