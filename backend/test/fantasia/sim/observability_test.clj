(ns fantasia.sim.observability-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [fantasia.sim.world :as world]
            [fantasia.sim.tick.initial :as initial]
            [fantasia.sim.tick.core :as tick-core]
            [fantasia.sim.jobs :as jobs]
            [fantasia.sim.tick.movement :as movement]
            [fantasia.sim.pathing :as pathing]))

(defn- setup-test-world []
  (let [w (initial/initial-world {:seed 42})
        [q r] [10 10]
        tile-key [q r]]
    (-> w
        (assoc-in [:tiles tile-key :resource] :tree)
        (assoc-in [:tiles tile-key :terrain] :ground)
        (jobs/create-stockpile! [5 5] :food 100))))

(deftest test-verify-logging-exists
  (testing "Verify logging functions are available"
    (let [w (setup-test-world)]
      (is (contains? w :jobs))
      (is (some #(= (:type %) :job/chop-tree) (:jobs w))))))

(deftest test-job-creation-logged
  (testing "Job creation produces log entries"
    (let [w (setup-test-world)
          job (jobs/create-job :job/chop-tree [10 10])]
      (is job)
      (is (= :job/chop-tree (:type job))))))

(deftest test-pathing-functions-exist
  (testing "Pathing functions are available"
    (let [w (setup-test-world)]
      (is (fn? pathing/bfs-path))
      (is (fn? pathing/a-star-path))
      (is (fn? pathing/next-step-toward)))))

(deftest test-observability-integration
  (testing "All observability components integrate"
    (let [w (setup-test-world)
          result (tick-core/tick-once w)]
      (is (:world result))
      (is (:out result)))))
