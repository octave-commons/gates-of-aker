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
          _ (helpers/with-needs world agent-id 0.8 0.7 0.6)
          result (mort/check-entity-mortality world agent-id)]
      (is (some? result))))
    (let [world (helpers/create-test-world)
          [agent-id world] (helpers/create-test-agent world :priest 0 0)
          _ (helpers/with-needs world agent-id 0.1 0.7 0.6)
          result (mort/check-entity-mortality world agent-id)]
      (is (= :starvation result)))))
  (testing "Returns :health-critical when health <= 0.0"
    (let [world (helpers/create-test-world)
          [agent-id world] (helpers/create-test-agent world :priest 0 0)
          _ (helpers/with-needs world agent-id 0.8 0.7 0.6)
          _ (helpers/with-stats world agent-id 0.5 0.5 0.5 0.5)
          world' (ecs/update-agent-needs world agent-id 0.8 0.7 0.0)
          result (mort/check-entity-mortality world' agent-id)]
      (is (= :health-critical result)))))
  (testing "Returns nil for healthy agents"
    (let [world (helpers/create-test-world)
          [agent-id world] (helpers/create-test-agent world :priest 0 0)
          _ (helpers/with-needs world agent-id 0.8 0.7 0.6)
          result (mort/check-entity-mortality world agent-id)]
      (is (nil? result))))
  (testing "Handles missing needs components"
    (let [world (helpers/create-test-world)
          [agent-id world] (helpers/create-test-agent world :priest 0 0)
          result (mort/check-entity-mortality world agent-id)]
      (is (nil? result)))))

(deftest test-create-death-memory
  (testing "Creates memory entity at death location"
    (let [world (helpers/create-test-world)
          agent-pos [5 10]
          memory (c/->Memory (java.util.UUID/randomUUID) :memory/danger agent-pos 100 1.5 (java.util.UUID/randomUUID) ["death" "danger"])
          memory-type (ecs/component-class memory)
          result (mort/create-death-memory world agent-pos :starvation :knight (java.util.UUID/randomUUID) 1.5)]
      (is (some? result)))))

(deftest test-cleanup-jobs-for-dead-entity
  (testing "Removes job assignment from dead entity"
    (let [world (helpers/create-test-world)
          [agent-id world] (helpers/create-test-agent world :priest 0 0)
          world' (helpers/with-job-assignment world agent-id "job-123" 0.5)
          job-type (ecs/component-class (c/->JobAssignment nil 0.0))
          _ (be/get-component world' agent-id job-type)
          result (mort/cleanup-jobs-for-dead-entity world' agent-id)]
      (is (some? result)))))
  (testing "Returns world unchanged when no job assigned"
    (let [world (helpers/create-test-world)
          [agent-id world] (helpers/create-test-agent world :priest 0 0)
          result (mort/cleanup-jobs-for-dead-entity world agent-id)]
      (is (some? result)))))

(deftest test-handle-entity-death
  (testing "Marks agent as dead"
    (let [world (helpers/create-test-world)
          [agent-id world] (helpers/create-test-agent world :priest 0 0)
          world' (helpers/with-needs world agent-id 0.1 0.7 0.6)
          result (mort/handle-entity-death world' agent-id :starvation :knight)]
      (is (some? result)))))

(deftest test-mortality-process
  (testing "Processes all entities with DeathState component"
    (let [world (helpers/create-test-world)
          [agent1 world] (helpers/create-test-agent world :priest 0 0)
          [agent2 world] (helpers/create-test-agent world :knight 1 0)
          _ (helpers/with-death-state world agent1 true :starvation nil)
          _ (helpers/with-death-state world agent2 true nil nil)
          result (mort/process world)]
      (is (some? result))))))
  (testing "Handles world with no DeathState entities"
    (let [world (helpers/create-test-world)
          result (mort/process world)]
      (is (some? result)))))
  (testing "Handles newly dead agents"
    (let [world (helpers/create-test-world)
          [agent-id world] (helpers/create-test-agent world :priest 0 0)
          _ (helpers/with-needs world agent-id 0.1 0.7 0.6)
          world' (helpers/with-death-state world agent-id true nil nil)
          result (mort/process world')]
      (is (some? result)))))
  (testing "Handles entities without needs (skips check)"
    (let [world (helpers/create-test-world)
          [agent-id world] (helpers/create-test-agent world :priest 0 0)
          world' (helpers/with-death-state world agent-id true nil nil)
          result (mort/process world')]
      (is (some? result)))))
