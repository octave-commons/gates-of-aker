(ns fantasia.sim.ecs.systems.job-assignment-test
  (:require [clojure.test :refer [deftest testing is]]
            [fantasia.sim.ecs.components :as c]
            [fantasia.sim.ecs.systems.job_assignment :as ja]))

(deftest test-player-agent
  (testing "Returns true for priest role"
    (let [role (c/->Role :priest)]
      (is (true? (ja/player-agent? role)))))

(deftest test-alive-agent
  (testing "Returns true when alive is true"
    (let [status (c/->AgentStatus true false false nil)]
      (is (true? (ja/alive-agent? status)))))
  (testing "Returns true when alive is nil (defaults to true)"
    (let [status (c/->AgentStatus nil false false nil)]
      (is (true? (ja/alive-agent? status)))))
  (testing "Returns false when alive is false"
    (let [status (c/->AgentStatus false true false nil)]
      (is (false? (ja/alive-agent? status)))))))
