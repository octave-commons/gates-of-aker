(ns fantasia.sim.jobs_logging_test
  (:require [clojure.test :refer [deftest is testing]]
            [fantasia.sim.jobs :as jobs]))

(deftest log-job-complete-prints-correct-format
  (testing "log-job-complete! prints job completion in standard format"
    (is (= "[JOB:COMPLETE]"
           (with-out-str
             (jobs/log-job-complete! :job/chop-tree [5 10] "Tree chopped at [5 10]"))))))

(deftest log-job-complete-handles-different-job-types
  (testing "log-job-complete! works with different job types"
    (let [output (with-out-str
                    (jobs/log-job-complete! :job/build-house [3 7] "House built"))]
      (is (clojure.string/includes? output ":job/build-house"))
      (is (clojure.string/includes? output "[JOB:COMPLETE]")))))
