(ns fantasia.server.gates-shadow-mode-test
  (:require [cheshire.core :as json]
            [clojure.test :refer [deftest is use-fixtures]]
            [fantasia.server :as server]
            [fantasia.sim.ecs.tick :as sim]
            [fantasia.sim.gates.runtime :as gates-runtime]))

(defn- parse-body
  [resp]
  (json/parse-string (:body resp) true))

(defn- request
  [method uri body]
  (server/app {:request-method method
               :uri uri
               :headers {"content-type" "application/json"}
               :body body}))

(defn- with-clean-gates
  [f]
  (let [initial-mode (gates-runtime/get-mode)]
    (try
      (gates-runtime/reset-registry!)
      (gates-runtime/clear-decision-records!)
      (gates-runtime/set-mode! :shadow)
      (sim/reset-world! {:seed 1 :tree_density 0.05})
      (f)
      (finally
        (gates-runtime/reset-registry!)
        (gates-runtime/clear-decision-records!)
        (gates-runtime/set-mode! initial-mode)))))

(use-fixtures :each with-clean-gates)

(deftest shadow-mode-emits-decision-records-at-request-and-runtime-boundaries
  (let [resp (request :post "/sim/tick" "{\"n\":1}")
        body (parse-body resp)
        records (gates-runtime/decision-records)
        triggered-ids (set (map :id (mapcat :triggered-gates records)))]
    (is (= 200 (:status resp)))
    (is (= #{:ok :last} (set (keys body))))
    (is (contains? (set (map (juxt :boundary :op) records)) [:http :sim/tick]))
    (is (contains? (set (map (juxt :boundary :op) records)) [:runtime :tick]))
    (is (contains? triggered-ids :g-008/no-async-promises))
    (is (contains? triggered-ids :g-009/timezone-dates))
    (is (contains? triggered-ids :g-010/filesystem-workflow))
    (is (contains? triggered-ids :g-011/tool-minimality))))

(deftest shadow-mode-does-not-change-sim-tick-response-shape
  (let [tick-request "{\"n\":1}"
        _ (gates-runtime/set-mode! :off)
        off-resp (request :post "/sim/tick" tick-request)
        off-body (parse-body off-resp)
        _ (sim/reset-world! {:seed 1 :tree_density 0.05})
        _ (gates-runtime/clear-decision-records!)
        _ (gates-runtime/set-mode! :shadow)
        shadow-resp (request :post "/sim/tick" tick-request)
        shadow-body (parse-body shadow-resp)
        off-last-keys (set (keys (:last off-body)))
        shadow-last-keys (set (keys (:last shadow-body)))]
    (is (= 200 (:status off-resp)))
    (is (= 200 (:status shadow-resp)))
    (is (= (set (keys off-body)) (set (keys shadow-body))))
    (is (= off-last-keys shadow-last-keys))
    (is (not (contains? shadow-last-keys :gate-decision)))
    (is (not (contains? shadow-last-keys :gate-decisions)))))

(deftest gate-runtime-mode-toggle-supports-shadow-off-and-enforce
  (gates-runtime/set-mode! :off)
  (is (= :off (gates-runtime/get-mode)))
  (gates-runtime/set-mode! :shadow)
  (is (= :shadow (gates-runtime/get-mode)))
  (gates-runtime/set-mode! :enforce)
  (is (= :enforce (gates-runtime/get-mode))))
