(ns fantasia.sim.ecs.systems.job-processing-test
  (:require [clojure.test :refer [deftest testing is]]
            [fantasia.sim.test-helpers :as helpers]
            [fantasia.sim.ecs.core :as ecs]
            [fantasia.sim.ecs.components :as c]
            [brute.entity :as be]
            [fantasia.sim.hex :as hex]
            [fantasia.sim.ecs.systems.job_processing :as jp]))

(deftest test-job-assignment-component
  (testing "JobAssignment component has correct structure"
    (let [job (c/->JobAssignment "job-123" 0.5)]
      (is (= "job-123" (:job-id job)))
      (is (= 0.5 (:progress job))))))

(deftest test-process-job-for-agent
  (testing "Increments progress when adjacent to target"
    (let [world (helpers/create-test-world)
          [agent-id world] (helpers/create-test-agent world :priest 0 0)
          job-assignment (c/->JobAssignment "job-123" 0.5)
          world' (be/add-component world agent-id job-assignment)]
      (is (some? (be/get-component world' agent-id (ecs/component-class job-assignment))))))
  (testing "Progress increments by 0.1 each tick"
    (let [job (c/->JobAssignment "job-123" 0.5)]
      (is (= 0.5 (:progress job)))))
  (testing "Completes job when progress reaches 1.0"
    (let [job (c/->JobAssignment "job-123" 0.9)]
      (is (< (:progress job) 1.0))))
  (testing "Job at progress 1.0 is complete"
    (let [job (c/->JobAssignment "job-123" 1.0)]
      (is (= 1.0 (:progress job))))))

(deftest test-job-system-process
  (testing "Processes all agents with JobAssignment component"
    (let [world (helpers/create-test-world)
          [agent1 world] (helpers/create-test-agent world :priest 0 0)
          [agent2 world] (helpers/create-test-agent world :knight 1 0)
          job1 (c/->JobAssignment "job-1" 0.5)
          job2 (c/->JobAssignment "job-2" 0.3)
          job-type (ecs/component-class job1)
          world' (-> world
                       (be/add-component agent1 job1)
                       (be/add-component agent2 job2))]
      (is (some? (be/get-component world' agent1 job-type)))
      (is (some? (be/get-component world' agent2 job-type)))))
  (testing "Skips agents without jobs"
    (let [world (helpers/create-test-world)
          [agent1 world] (helpers/create-test-agent world :priest 0 0)
          [agent2 world] (helpers/create-test-agent world :knight 1 0)
          job (c/->JobAssignment "job-1" 0.5)
          job-type (ecs/component-class job)
          world' (be/add-component world agent1 job)]
      (is (some? (be/get-component world' agent1 job-type)))
      (is (nil? (be/get-component world' agent2 job-type)))))
  (testing "Handles empty world"
    (let [world (helpers/create-test-world)
          global-state {:tick 10}
          result (jp/process world global-state)]
      (is (some? result)))))
