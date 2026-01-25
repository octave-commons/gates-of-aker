(ns fantasia.sim.ecs.systems.needs-test
  (:require [clojure.test :refer [deftest is testing]]
            [brute.entity :as be]
            [fantasia.sim.ecs.systems.needs :as needs]
            [fantasia.sim.ecs.components :as c]
            [fantasia.sim.ecs.facets :as f]
            [fantasia.sim.ecs.core :as ecs]))

(deftest query-need-axis-empty-concepts
  (testing "returns neutral score for empty concepts"
    (let [ecs-world (ecs/create-ecs-world)
          agent-id 1]
      (is (= {:score 0.0 :concepts {} :axis-activation 0.0 :axis :warmth}
             (needs/query-need-axis! ecs-world agent-id :warmth []))))))

(deftest query-need-axis-fire-concepts
  (testing "calculates positive score for fire-related concepts"
    (let [ecs-world (ecs/create-ecs-world)
          agent-id 1
          frontier (c/->Frontier {:fire {:a 0.9}
                          :campfire {:a 0.8}
                          :warmth {:a 0.85}})]
      (be/add-component ecs-world agent-id frontier)
      (let [result (needs/query-need-axis! ecs-world agent-id
                                            :warmth
                                            [:fire :campfire :warm])]
        (is (> (:score result) 0.7))
        (is (= :warmth (:axis result)))))))

(deftest get-axis-activation-threshold
  (testing "returns correct thresholds for axes"
    (is (= 0.7 (needs/get-axis-activation-threshold :warmth)))
    (is (= 0.6 (needs/get-axis-activation-threshold :food)))
    (is (= 0.5 (needs/get-axis-activation-threshold :social)))))

(deftest axis-activated?
  (testing "correctly identifies activated axes"
    (let [ecs-world (ecs/create-ecs-world)
          agent-id 1
          frontier (c/->Frontier {:fire {:a 0.9}})]
      (be/add-component ecs-world agent-id frontier)
      (is (true? (needs/axis-activated? ecs-world agent-id
                                         :warmth
                                         [:fire :campfire])))
      (is (false? (needs/axis-activated? ecs-world agent-id
                                          :food
                                          [:hunger]))))))