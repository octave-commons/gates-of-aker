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
          death-tick 42
          world2 (mortality/process world1 death-tick)
          death-state (be/get-component world2 agent-id (death-state-type))
          status (be/get-component world2 agent-id (status-type))]
      (is (false? (:alive? death-state)))
      (is (= :starvation (:cause-of-death death-state)))
      (is (= death-tick (:death-tick death-state)))
      (is (false? (:alive? status)))
      (is (= :starvation (:cause-of-death status))))))

(deftest starvation-without-job-assignment-keeps-world-intact
  (testing "mortality cleanup returns world even when no job assignment exists"
    (let [world (ecs/create-ecs-world)
          [agent-id world1] (ecs/create-agent world nil 1 1 :peasant {:needs (needs 0.1)})
          world2 (mortality/process world1 99)]
      (is (some? world2))
      (is (some? (be/get-component world2 agent-id (death-state-type)))))))

(deftest death-requeues-claimed-job
  (testing "when a worker dies their claimed job is returned to pending"
    (let [world0 (ecs/create-ecs-world)
          [agent-id world1] (ecs/create-agent world0 nil 0 0 :peasant {:needs (needs 0.1)})
          [building-id world2] (ecs/create-building world1 [1 0] :house)
          queue-type (be/get-component-type (c/->JobQueue {} [] {}))
          job-id "queued-job"
          queue (c/->JobQueue {job-id {:id job-id
                                       :type :job/improve
                                       :priority 50
                                       :state :claimed
                                       :worker-id agent-id
                                       :target-pos [1 0]
                                       :target [1 0]}}
                              []
                              {job-id agent-id})
          world3 (-> world2
                     (be/add-component building-id queue)
                     (be/add-component agent-id (c/->JobAssignment job-id 0.2)))
          world4 (mortality/process world3 100)
          queue-after (be/get-component world4 building-id queue-type)
          requeued-job (get (:jobs queue-after) job-id)]
      (is (= :pending (:state requeued-job)))
      (is (nil? (:worker-id requeued-job)))
      (is (= [job-id] (:pending-jobs queue-after)))
      (is (empty? (:assigned-jobs queue-after))))))
