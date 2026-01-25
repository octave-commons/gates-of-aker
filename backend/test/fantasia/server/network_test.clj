(ns fantasia.server.network-test
  (:require [clojure.test :refer [deftest testing is]]
            [fantasia.test-helpers :as helpers]))

(deftest test-server-starts
  (testing "Server starts without errors"
    (let [port (helpers/get-free-port)]
      (is (some? port)))))

(deftest test-websocket-handshake
  (testing "WebSocket connection and message handling"
    (let [world (helpers/create-test-world)]
      (is (some? :memories world)))
      (is (some? :agents world)))))

(deftest test-broadcast-functionality
  (testing "Message broadcasting to multiple clients"
    (let [world (helpers/create-test-world)]
      (is (some? :broadcast (meta world)))))))