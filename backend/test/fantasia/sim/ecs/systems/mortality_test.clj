(ns fantasia.sim.ecs.systems.mortality-test
  (:require [brute.entity :as be]
            [clojure.test :refer [deftest is testing]]
            [fantasia.sim.ecs.components :as c]
            [fantasia.sim.ecs.core :as ecs]
            [fantasia.sim.ecs.systems.mortality :as mortality]))

(defn- death-state-type []
  (be/get-component-type (c/->DeathState true nil nil)))

(defn- status-type []
  (be/get-component-type (c/->AgentStatus true false true nil)))

(defn- needs
  [food]
  (c/->Needs 0.8 food 0.8 1.0 0.8 0.6 0.5 0.5 0.5 0.6 0.5 0.5 0.5))

(deftest create-agent-seeds-death-state
  (testing "new agents start with alive DeathState"
    (let [world (ecs/create-ecs-world)
          [agent-id world'] (ecs/create-agent world nil 0 0 :peasant)
          death-state (be/get-component world' agent-id (death-state-type))]
      (is (some? death-state))
      (is (true? (:alive? death-state)))
      (is (nil? (:cause-of-death death-state))))))

(deftest starvation-marks-death-and-status
  (testing "food below starvation threshold marks entity dead"
    (let [world (ecs/create-ecs-world)
          [agent-id world1] (ecs/create-agent world nil 0 0 :peasant {:needs (needs 0.1)})
          world2 (mortality/process world1)
          death-state (be/get-component world2 agent-id (death-state-type))
          status (be/get-component world2 agent-id (status-type))]
      (is (false? (:alive? death-state)))
      (is (= :starvation (:cause-of-death death-state)))
      (is (false? (:alive? status)))
      (is (= :starvation (:cause-of-death status))))))

(deftest starvation-without-job-assignment-keeps-world-intact
  (testing "mortality cleanup returns world even when no job assignment exists"
    (let [world (ecs/create-ecs-world)
          [agent-id world1] (ecs/create-agent world nil 1 1 :peasant {:needs (needs 0.1)})
          world2 (mortality/process world1)]
      (is (some? world2))
      (is (some? (be/get-component world2 agent-id (death-state-type)))))))
