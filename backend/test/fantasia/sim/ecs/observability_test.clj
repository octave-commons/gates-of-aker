(ns fantasia.sim.ecs.observability-test
  (:require [brute.entity :as be]
            [clojure.test :refer [deftest is testing]]
            [fantasia.dev.logging :as log]
            [fantasia.sim.ecs.components :as c]
            [fantasia.sim.ecs.core :as ecs-core]
            [fantasia.sim.ecs.systems.job-assignment :as job-assignment]
            [fantasia.sim.ecs.systems.job-creation :as job-creation]
            [fantasia.sim.ecs.systems.job-processing :as job-processing]
            [fantasia.sim.ecs.systems.movement :as movement]))

(defn- seed-observability-world
  []
  (let [world (ecs-core/create-ecs-world)
        [agent-id world1] (ecs-core/create-agent world nil 0 0 :peasant)
        [_ world2] (ecs-core/create-building world1 [0 0] :house)
        idle-status (c/->AgentStatus true false true nil)
        world3 (be/add-component world2 agent-id idle-status)]
    {:world world3
     :agent-id agent-id
     :global-state {:tick 10}}))

(deftest test-observability-log-sequence
  (testing "ECS emits lifecycle/pathing observability log sequence"
    (let [{:keys [world global-state]} (seed-observability-world)]
      (with-redefs [log/current-level 3]
        (let [output (with-out-str
                       (let [world1 (job-creation/process world global-state)
                             world2 (job-assignment/process world1 global-state)
                             world3 (movement/process world2)
                             _ (nth (iterate #(job-processing/process % global-state) world3) 6)]
                         nil))]
          (is (re-find #"\[JOB:CREATE\]" output))
          (is (re-find #"\[JOB:ASSIGN\]" output))
          (is (re-find #"\[PATH:REQUEST\]" output))
          (is (re-find #"\[PATH:RESULT\]" output))
          (is (re-find #"\[MOVE:AGENT\]" output))
          (is (re-find #"\[JOB:PROGRESS\]" output))
          (is (re-find #"\[JOB:COMPLETE\]" output)))))))
