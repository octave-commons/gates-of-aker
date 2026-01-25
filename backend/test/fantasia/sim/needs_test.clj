(ns fantasia.sim.needs-test
  (:require [clojure.test :refer [deftest is testing]]
            [fantasia.sim.needs :as needs]
            [fantasia.sim.ecs.facets :as f]))

(deftest query-need-axis-empty-concepts
  (testing "returns neutral score for empty concepts"
    (let [world {}
          agent {:id 1 :frontier {} :recall {}}]
      (is (= {:score 0.0 :concepts {} :axis-activation 0.0 :axis :warmth}
             (needs/query-need-axis! world agent :warmth []))))))

(deftest query-need-axis-fire-concepts
  (testing "calculates positive score for fire-related concepts"
    (let [world {}
          agent {:id 1
                 :frontier {:fire {:a 0.9}
                           :campfire {:a 0.8}
                           :warmth {:a 0.85}}
                 :recall {}}]
      (let [result (needs/query-need-axis! world agent
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
    (let [world {}
          agent {:id 1
                 :frontier {:fire {:a 0.9}}
                 :recall {}}]
      (is (true? (needs/axis-activated? world agent
                                         :warmth
                                         [:fire :campfire])))
      (is (false? (needs/axis-activated? world agent
                                          :food
                                          [:hunger]))))))