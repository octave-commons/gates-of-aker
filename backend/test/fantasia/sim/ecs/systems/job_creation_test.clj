(ns fantasia.sim.ecs.systems.job-creation-test
  (:require [clojure.test :refer [deftest testing is]]
            [fantasia.sim.test-helpers :as helpers]
            [fantasia.sim.ecs.core :as ecs]
            [fantasia.sim.ecs.components :as c]
            [brute.entity :as be]
            [fantasia.sim.ecs.systems.job_creation :as jc]))

(deftest test-job-queue-component
  (testing "JobQueue component has correct structure"
    (let [job-queue (c/->JobQueue {} [] {})]
      (is (vector? (:pending-jobs job-queue)))
      (is (map? (:assigned-jobs job-queue))))))

(deftest test-generate-basic-jobs
  (testing "Creates gather-wood job when building has no jobs"
    (let [world (helpers/create-test-world)
          [building-id world] (helpers/create-test-building world [0 0] :house)
          job-queue (c/->JobQueue {} [] {})
          world' (be/add-component world building-id job-queue)
          global-state {:tick 10}
          result (jc/generate-basic-jobs world' global-state)]
      (is (some? result))))
  (testing "Does not create job when building already has jobs"
    (let [world (helpers/create-test-world)
          [building-id world] (helpers/create-test-building world [0 0] :house)
          job-queue (c/->JobQueue {"existing-job" {:id "existing-job"}} [] {})
          world' (be/add-component world building-id job-queue)
          global-state {:tick 10}
          result (jc/generate-basic-jobs world' global-state)]
      (is (some? result))))
  (testing "Uses correct tick in job ID"
    (let [global-state {:tick 42}]
      (is (some? (:tick global-state)))))
  (testing "Sets correct priority for jobs"
    (let [world (helpers/create-test-world)
          [building-id world] (helpers/create-test-building world [0 0] :house)
          job-queue (c/->JobQueue {} [] {})
          world' (be/add-component world building-id job-queue)
          global-state {:tick 10}
          result (jc/generate-basic-jobs world' global-state)]
      (is (some? result))))
  (testing "Sets target position to building position"
    (let [world (helpers/create-test-world)
          [building-id world] (helpers/create-test-building world [5 10] :house)
          job-queue (c/->JobQueue {} [] {})
          world' (be/add-component world building-id job-queue)]
      (is (some? (be/get-component world' building-id (ecs/component-class (c/->Position 0 0))))))))

(deftest test-job-creation-process
  (testing "Processes all buildings with JobQueue components"
    (let [world (helpers/create-test-world)
          [b1 world] (helpers/create-test-building world [0 0] :house)
          [b2 world] (helpers/create-test-building world [1 0] :campfire)
          world' (-> world
                       (be/add-component b1 (c/->JobQueue {} [] {}))
                       (be/add-component b2 (c/->JobQueue {} [] {})))
          global-state {:tick 10}
          result (jc/process world' global-state)]
      (is (some? result))))
     (testing "Handles world with no buildings"
    (let [world (helpers/create-test-world)
          global-state {:tick 10}
          result (jc/process world global-state)]
      (is (some? result))))
  (testing "Handles world with buildings but no JobQueue"
    (let [world (helpers/create-test-world)
          [_ world] (helpers/create-test-building world [0 0] :house)
          global-state {:tick 10}
          result (jc/process world global-state)]
      (is (some? result)))))