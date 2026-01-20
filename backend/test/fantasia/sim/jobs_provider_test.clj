(ns fantasia.sim.jobs-provider-test
  (:require [clojure.test :refer [deftest is testing]]
            [fantasia.sim.jobs.providers :as job-providers]
            [fantasia.sim.tick.initial :as initial]))

(deftest initial-world-seeds-provider-jobs
  (let [world (initial/initial-world {:seed 3})
        job-types (set (map :type (:jobs world)))]
    (is (= 16 (count (:jobs world))))
    (is (contains? job-types :job/builder))
    (is (contains? job-types :job/improve))
    (is (contains? job-types :job/mine))))

(deftest workshop-provider-respects-cap
  (let [world (assoc (initial/initial-world {:seed 4}) :jobs [])
        generated (job-providers/generate-provider-jobs! world)
        builder-jobs (filter #(= (:type %) :job/builder) (:jobs generated))]
    (is (<= (count builder-jobs) 2))))
