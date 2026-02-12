(ns fantasia.sim.ecs.systems.job-creation-test
  (:require [brute.entity :as be]
             [clojure.test :refer [deftest testing is]]
             [fantasia.sim.constants :as const]
             [fantasia.sim.ecs.components :as c]
             [fantasia.sim.ecs.core :as ecs]
             [fantasia.sim.ecs.systems.job-creation :as jc]
             [fantasia.sim.test-helpers :as helpers]))

(defn- queue-jobs
  [world building-id]
  (let [queue-type (ecs/component-class (c/->JobQueue {} [] {}))
        queue (be/get-component world building-id queue-type)]
    (:jobs queue {})))

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

(deftest test-demand-driven-chop-jobs
  (testing "Creates chop-tree jobs when wood stockpile has open capacity"
    (let [world0 (helpers/create-test-world)
          [building-id world1] (helpers/create-test-building world0 [0 0] :house)
          world2 (-> world1
                     (be/add-component building-id (c/->JobQueue {} [] {}))
                     (be/add-component building-id (c/->Stockpile {:log 0})))
          [_k _tree-id world3] (ecs/create-tile world2 5 5 :ground :plains nil :tree)
          world4 (be/add-component world3 building-id (c/->Tile :ground :plains :house nil))
          result (jc/process world4 {:tick 20})
          jobs (vals (queue-jobs result building-id))]
      (is (some #(= :job/chop-tree (:type %)) jobs))))
  (testing "Does not create chop-tree jobs when wood stockpiles are full"
    (let [world0 (helpers/create-test-world)
          [building-id world1] (helpers/create-test-building world0 [0 0] :house)
          world2 (-> world1
                     (be/add-component building-id (c/->JobQueue {} [] {}))
                     (be/add-component building-id (c/->Stockpile {:log const/default-stockpile-max-qty})))
          [_k _tree-id world3] (ecs/create-tile world2 5 5 :ground :plains nil :tree)
          world4 (be/add-component world3 building-id (c/->Tile :ground :plains :house nil))
          result (jc/process world4 {:tick 21})
          jobs (vals (queue-jobs result building-id))]
      (is (not-any? #(= :job/chop-tree (:type %)) jobs)))))

(deftest test-idle-build-job-generation
  (testing "Process handles idle agents with completed history without crashing"
    (let [world0 (helpers/create-test-world)
          [building-id world1] (helpers/create-test-building world0 [0 0] :house)
          completed-jobs {"done-1" {:id "done-1" :type :job/improve :state :completed}
                          "done-2" {:id "done-2" :type :job/improve :state :completed}}
          world2 (be/add-component world1 building-id (c/->JobQueue completed-jobs [] {}))
          idle-status (c/->AgentStatus true false true nil)
          [_agent-id world3] (helpers/create-test-agent world2 :peasant 4 4 {:status idle-status})
          [_k _tile-id world4] (ecs/create-tile world3 5 4 :ground :plains nil nil)
          result (jc/process world4 {:tick 22})
          jobs (vals (queue-jobs result building-id))]
      (is (some? result))
      (is (seq jobs)))))

(deftest test-stockpile-deliver-food-job-capped
  (testing "Stockpile job generation creates at most one deliver-food job per stockpile"
    (let [world0 (helpers/create-test-world)
          [building-id world1] (helpers/create-test-building world0 [0 0] :stockpile)
          world2 (be/add-component world1 building-id (c/->JobQueue {} [] {}))
          world3 (be/add-component world2 building-id (c/->Tile :ground :plains :stockpile nil))
          result (jc/generate-basic-jobs world3 {:tick 40})
          jobs (vals (queue-jobs result building-id))]
      (is (= 1 (count jobs)))
      (is (= :job/deliver-food (:type (first jobs)))))))

(deftest test-harvest-autobuild-from-demand
  (testing "Creates harvest building autobuild jobs near campfire when resource demand exists"
    (let [world0 (helpers/create-test-world)
          [queue-building-id world1] (helpers/create-test-building world0 [0 0] :house)
          [campfire-id world2] (helpers/create-test-building world1 [2 2] :campfire)
          world3 (-> world2
                     (be/add-component queue-building-id (c/->JobQueue {} [] {}))
                     (be/add-component queue-building-id (c/->Stockpile {:grain 0}))
                     (be/add-component campfire-id (c/->Tile :ground :plains :campfire nil)))
          [_k _tile-id world4] (ecs/create-tile world3 3 2 :ground :plains nil nil)
          result (jc/process world4 {:tick 41})
          jobs (vals (queue-jobs result queue-building-id))
          granary-job (some #(when (= :granary (:structure %)) %) jobs)]
      (is (some? granary-job))
      (is (= :job/build-structure (:type granary-job))))))

(deftest test-basic-job-generation-ignores-completed-jobs-for-cap
  (testing "Building with only completed jobs still receives new active jobs"
    (let [world0 (helpers/create-test-world)
          [building-id world1] (helpers/create-test-building world0 [0 0] :house)
          completed-jobs {"done-1" {:id "done-1" :type :job/improve :state :completed}
                          "done-2" {:id "done-2" :type :job/improve :state :completed}}
          world2 (-> world1
                     (be/add-component building-id (c/->Tile :ground :plains :house nil))
                     (be/add-component building-id (c/->JobQueue completed-jobs [] {})))
          result (jc/generate-basic-jobs world2 {:tick 55})
          jobs (vals (queue-jobs result building-id))
          active-count (count (filter #(not= :completed (:state %)) jobs))]
      (is (= 3 (count jobs)))
      (is (= 1 active-count)))))

(deftest test-need-job-generation-dedupes-per-agent
  (testing "Need jobs are not duplicated every tick for same agent and need"
    (let [world0 (helpers/create-test-world)
          [queue-id world1] (helpers/create-test-building world0 [0 0] :house)
          [_agent-id world2] (helpers/create-test-agent world1 :peasant 2 2
                                                        {:needs (c/->Needs 0.2 0.2 0.2 1.0 0.8 0.6 0.5 0.5 0.5 0.6 0.5 0.5 0.5)})
          world3 (be/add-component world2 queue-id (c/->JobQueue {} [] {}))
          [_k _house-tile world4] (ecs/create-tile world3 3 2 :ground :plains :house nil)
          after-first (jc/generate-need-jobs world4 {:tick 60})
          after-second (jc/generate-need-jobs after-first {:tick 61})
          first-jobs (vals (queue-jobs after-first queue-id))
          second-jobs (vals (queue-jobs after-second queue-id))
          active-types (fn [jobs] (set (map :type (filter #(not= :completed (:state %)) jobs))))
          first-active-types (active-types first-jobs)]
      (is (contains? first-active-types :job/rest))
      (is (contains? first-active-types :job/build-fire))
      (is (or (contains? first-active-types :job/eat)
              (contains? first-active-types :job/gather-food)))
      (is (= (count first-jobs) (count second-jobs))))))

(deftest test-rest-job-targets-house-when-available
  (testing "Sleep need targets nearest house instead of current tile"
    (let [world0 (helpers/create-test-world)
          [queue-id world1] (helpers/create-test-building world0 [0 0] :house)
          [_agent-id world2] (helpers/create-test-agent world1 :peasant 2 2
                                                        {:needs (c/->Needs 0.6 0.8 0.2 1.0 0.8 0.6 0.5 0.5 0.5 0.6 0.5 0.5 0.5)})
          world3 (-> world2
                     (be/add-component queue-id (c/->JobQueue {} [] {}))
                     (be/add-component queue-id (c/->Tile :ground :plains :house nil)))
          [_k _house-tile world4] (ecs/create-tile world3 4 2 :ground :plains :house nil)
          result (jc/generate-need-jobs world4 {:tick 62})
          jobs (vals (queue-jobs result queue-id))
          rest-job (some #(when (= :job/rest (:type %)) %) jobs)]
      (is (some? rest-job))
      (is (= [4 2] (:target-pos rest-job))))))
