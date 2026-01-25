(ns fantasia.sim.ecs.systems.job-assignment-test
  (:require [clojure.test :refer [deftest testing is]]
            [fantasia.sim.ecs.components :as c]
            [fantasia.sim.ecs.systems.job-assignment :as ja]))

(deftest test-player-agent?
  (testing "Returns true for priest role"
    (let [role (c/->Role :priest)]
      (is (true? (ja/player-agent? role)))))
  (testing "Returns true for knight role"
    (let [role (c/->Role :knight)]
      (is (true? (ja/player-agent? role)))))
  (testing "Returns true for peasant role"
    (let [role (c/->Role :peasant)]
      (is (true? (ja/player-agent? role)))))
  (testing "Returns false for animal roles"
    (let [role (c/->Role :wolf)]
      (is (false? (ja/player-agent? role)))))))

(deftest test-alive-agent?
  (testing "Returns true when alive? is true"
    (let [status (c/->AgentStatus true false false nil)]
      (is (true? (ja/alive-agent? status)))))
  (testing "Returns true when alive? is nil (defaults to true)"
    (let [status (c/->AgentStatus nil false false nil)]
      (is (true? (ja/alive-agent? status)))))
  (testing "Returns false when alive? is false"
    (let [status (c/->AgentStatus false true false nil)]
      (is (false? (ja/alive-agent? status)))))))

(deftest test-get-job-priority
  (testing "Returns priority values for known job types"
    (is (some? (ja/get-job-priority :job/eat))))
    (is (some? (ja/get-job-priority :job/warm-up))))
    (is (some? (ja/get-job-priority :job/build-house)))))
  (testing "Returns default priority for unknown job types"
    (is (some? (ja/get-job-priority :job/unknown))))))

(deftest test-sort-jobs-by-priority
  (testing "Sorts jobs by priority (higher first)"
    (let [jobs [{:id 1 :type :job/hunt :priority 100}
                 {:id 2 :type :job/eat :priority 50}]
          agent-pos [0 0]]
      (let [sorted (ja/sort-jobs-by-priority jobs agent-pos)]
        (is (= 1 (:id (first sorted)))))))
  (testing "Handles empty job list"
    (let [jobs []
          agent-pos [0 0]]
          sorted (ja/sort-jobs-by-priority jobs agent-pos)]
      (is (empty? sorted))))))
