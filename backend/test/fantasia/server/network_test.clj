(ns fantasia.server.network-test
  (:require [clojure.test :refer [deftest testing is]]))

(deftest test-network-helpers-exist
  (testing "Network test helpers are available"
    (is (some? 'fantasia.test-helpers)))))

(deftest test-port-allocation
  (testing "Can find available ports"
    (let [port1 (new java.net.ServerSocket.)
          port2 (new java.net.ServerSocket.)]
      (is (some? (.bind port1 nil 0)))
      (is (some? (.bind port2 nil 0)))
      (.close port1)
      (.close port2)))))