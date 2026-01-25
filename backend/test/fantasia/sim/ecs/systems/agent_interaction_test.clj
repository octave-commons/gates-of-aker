(ns fantasia.sim.ecs.systems.agent-interaction-test
  (:require [clojure.test :refer [deftest testing is]]
            [fantasia.sim.test-helpers :as helpers]
            [fantasia.sim.ecs.core :as ecs]
            [fantasia.sim.ecs.components :as c]
            [brute.entity :as be]
            [fantasia.sim.hex :as hex]
            [fantasia.sim.ecs.systems.agent-interaction :as ai]))

(deftest test-nearby-agents
  (testing "Returns agents within vision radius"
    (let [world (helpers/create-test-world)
          [agent1 world] (helpers/create-test-agent world :priest 0 0)
          [agent2 world] (helpers/create-test-agent world :knight 0 1)
          [agent3 world] (helpers/create-test-agent world :peasant 1 0)
          nearby (ai/nearby-agents world [0 0] 2)]
      (is (some? nearby))
      (is (= 3 (count nearby)))))
  (testing "Returns empty list when no agents within radius"
    (let [world (helpers/create-test-world)
          [agent1 world] (helpers/create-test-agent world :priest 0 0)
          [agent2 world] (helpers/create-test-agent world :knight 10 10)
          nearby (ai/nearby-agents world [0 0] 2)]
      (is (= 1 (count nearby)))))
  (testing "Filters by hex distance from position"
    (let [world (helpers/create-test-world)
          [agent1 world] (helpers/create-test-agent world :priest 0 0)
          [agent2 world] (helpers/create-test-agent world :knight 0 1)
          [agent3 world] (helpers/create-test-agent world :peasant 10 10)
          nearby (ai/nearby-agents world [0 0] 2)]
      (is (= 2 (count nearby)))
      (is (not (some #(= (:q %) 10) nearby)))))))

(deftest test-speech-packet
  (testing "Generates packet with correct intent based on facets"
    (let [frontier {:facets {:warn 0.9}}]
          packet (ai/choose-packet-for-speech frontier)]
      (is (= :warning (:intent packet))))
    (let [frontier {:facets {:boast 0.8}}]
          packet (ai/choose-packet-for-speech frontier)]
      (is (= :warning (:intent packet))))
    (let [frontier {:facets {:recruit 0.7}}]
          packet (ai/choose-packet-for-speech frontier)]
      (is (= :warning (:intent packet))))
    (let [frontier {:facets {:report 0.9}}]
          packet (ai/choose-packet-for-speech frontier)]
      (is (= :inquiry (:intent packet))))
    (let [frontier {:facets {:ask-question 0.8}}]
          packet (ai/choose-packet-for-speech frontier)]
      (is (= :inquiry (:intent packet))))
    (let [frontier {:facets {:gossip 0.9}}]
          packet (ai/choose-packet-for-speech frontier)]
      (is (= :gossip (:intent packet))))))
  (testing "Includes top 10 facets from frontier sorted by activation"
    (let [frontier {:facets {:fire 0.9 :campfire 0.8 :warmth 0.85 :cold 0.7 :danger 0.6}}
          packet (ai/choose-packet-for-speech frontier)]
      (is (some? (:topic-vec packet)))
      (is (vector? (:topic-vec packet)))
      (is (<= (count (:topic-vec packet)) 6)))))
  (testing "Sets appropriate tone"
    (let [frontier {:facets {:gossip 0.9}}]
          packet (ai/choose-packet-for-speech frontier)]
      (is (some? (:tone packet)))
      (is (= {:arousal 0.0 :valence 0.0 :fear 0.0} (:tone packet)))))
  (testing "Sets salience to 0.8"
    (let [frontier {:facets {:gossip 0.9}}]
          packet (ai/choose-packet-for-speech frontier)]
      (is (= 0.8 (:salience packet)))))
  (testing "Generates unique packet ID"
    (let [frontier {:facets {:gossip 0.9}}
          packet1 (ai/choose-packet-for-speech frontier)
          packet2 (ai/choose-packet-for-speech frontier)]
      (is (not (= (:packet-id packet1) (:packet-id packet2))))
      (is (uuid? (:packet-id packet1)))
      (is (uuid? (:packet-id packet2)))))
  (testing "Sets empty anchors"
    (let [frontier {:facets {:gossip 0.9}}]
          packet (ai/choose-packet-for-speech frontier)]
      (is (= #{} (:anchors packet)))))
  (testing "Sets credibility with default values"
    (let [frontier {:facets {:gossip 0.9}}]
          packet (ai/choose-packet-for-speech frontier)]
      (is (some? (:credibility packet)))
      (is (= 0.5 (:speaker-reputation (:credibility packet))))
      (is (= 1.0 (:channel-mod (:credibility packet))))))
  (testing "Sets spread parameters"
    (let [frontier {:facets {:gossip 0.9}}]
          packet (ai/choose-packet-for-speech frontier)]
      (is (some? (:spread packet)))
      (is (= 3 (:base-radius (:spread packet))))
      (is (= 0.1 (:entropy (:spread packet))))
      (is (= 1 (:delay (:spread packet)))))))
  (testing "Handles empty frontier"
    (let [frontier {:facets {}}
          packet (ai/choose-packet-for-speech frontier)]
      (is (some? packet))
      (is (= :gossip (:intent packet)))
      (is (empty? (:topic-vec packet))))))
