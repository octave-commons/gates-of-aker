(ns fantasia.server.network-test
  (:require [clojure.test :refer [deftest testing is]]))

(deftest test-network-helpers-exist
  (testing "Network test helpers are available"
    (is (some? 'fantasia.test-helpers))))

(deftest test-port-allocation
  (testing "Can find available ports"
    (let [port1 (java.net.ServerSocket. 0)
          port2 (java.net.ServerSocket. 0)]
      (is (some? port1))
      (is (some? port2))
      (.close port1)
      (.close port2))))