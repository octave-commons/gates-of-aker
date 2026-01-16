(ns fantasia.server-test
  (:require [clojure.test :refer [deftest is testing]]
            [cheshire.core :as json]
            [ring.mock.request :as mock]
            [fantasia.server :as server]
            [fantasia.sim.tick :as sim])
  (:import [java.io ByteArrayInputStream]))

(defn parse-body [resp]
  (json/parse-string (:body resp) true))

(defn byte-stream [s]
  (ByteArrayInputStream. (.getBytes (or s "") "UTF-8")))

(deftest json-resp-formats-body-and-status
  (let [resp (server/json-resp {:foo "bar"})
        custom (server/json-resp 202 {:ok true})]
    (is (= 200 (:status resp)))
    (is (= {:foo "bar"} (parse-body resp)))
    (is (= 202 (:status custom)))
    (is (= {:ok true} (parse-body custom)))
    (is (= "application/json" (get-in resp [:headers "content-type"])))
    (is (= "GET,POST,OPTIONS" (get-in resp [:headers "access-control-allow-methods"]))))

(deftest read-json-body-keywordizes-deep-maps
  (let [req {:body (byte-stream "{\"foo\": {\"bar\": 2}}")}
        res (server/read-json-body req)]
    (is (= {:foo {:bar 2}} res))))

(deftest read-json-body-blanks-and-errors-return-nil
  (is (nil? (server/read-json-body {:body (byte-stream "")})))
  (is (nil? (server/read-json-body {:body nil})))
  (is (nil? (server/read-json-body {:body (byte-stream "{")}))))

(deftest healthz-endpoint-returns-ok
  (let [resp (server/app (mock/request :get "/healthz"))
        body (parse-body resp)]
    (is (= 200 (:status resp)))
    (is (= {:ok true} body))
    (doseq [h ["content-type" "access-control-allow-origin" "access-control-allow-headers" "access-control-allow-methods"]]
      (is (contains? (:headers resp) h)))))

(deftest sim-state-proxies-world
  (let [state {:tick 99 :shrine [1 2]}
        called (atom 0)
        resp (with-redefs [sim/get-state (fn []
                                           (swap! called inc)
                                           state)]
               (server/app (mock/request :get "/sim/state")))
        body (parse-body resp)]
    (is (= 1 @called))
    (is (= 200 (:status resp)))
    (is (= state body))))

(deftest sim-reset-parses-body-and-responds
  (let [captured (atom nil)
        req (mock/json-body (mock/request :post "/sim/reset") {:seed 77})
        resp (with-redefs [sim/reset-world! (fn [opts]
                                              (reset! captured opts))]
               (server/app req))
        body (parse-body resp)]
    (is (= {:seed 77} @captured))
    (is (= {:ok true :seed 77} body))
    (is (= 200 (:status resp)))))

(deftest sim-tick-forwards-n-and-wraps-last-out
  (let [captured (atom nil)
        outs [{:tick 1} {:tick 2 :snapshot {:tick 2}}]
        req (mock/json-body (mock/request :post "/sim/tick") {:n 2})
        resp (with-redefs [sim/tick! (fn [n]
                                       (reset! captured n)
                                       outs)]
               (server/app req))
        body (parse-body resp)]
    (is (= 2 @captured))
    (is (= {:ok true :last (last outs)} body))
    (is (= 200 (:status resp)))))

(deftest runner-routes-toggle-running-state
  (let [run-called (atom 0)
        pause-called (atom 0)
        run-resp (with-redefs [server/start-runner! (fn [] (swap! run-called inc))]
                   (server/app (mock/request :post "/sim/run")))
        pause-resp (with-redefs [server/stop-runner! (fn [] (swap! pause-called inc) true)]
                     (server/app (mock/request :post "/sim/pause")))
        run-body (parse-body run-resp)
        pause-body (parse-body pause-resp)]
    (is (= 1 @run-called))
    (is (= {:ok true :running true} run-body))
    (is (= 200 (:status run-resp)))
    (is (= 1 @pause-called))
    (is (= {:ok true :running false} pause-body))
    (is (= 200 (:status pause-resp)))))
)
