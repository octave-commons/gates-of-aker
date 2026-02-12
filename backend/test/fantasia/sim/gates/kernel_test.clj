(ns fantasia.sim.gates.kernel-test
  (:require [clojure.test :refer [deftest is testing]]
            [fantasia.sim.gates.kernel :as kernel]))

(defn gate
  [id priority trigger enforce]
  {:id id
   :priority priority
   :trigger trigger
   :enforce enforce})

(deftest evaluate-empty-signals-produces-empty-decision
  (let [registry (kernel/build-registry
                  [(gate :g/needs-web
                         20
                         #(kernel/has-signal? % :needs-web)
                         (fn [_]
                           {:obligations [{:id :must-web-run
                                           :value :required}]}))])
        decision (kernel/evaluate registry #{})]
    (is (= [] (:triggered-gates decision)))
    (is (= [] (:obligations decision)))
    (is (= [] (:prohibitions decision)))
    (is (= [] (:formatting-constraints decision)))
    (is (= [] (:conflicts decision)))))

(deftest evaluate-assembles-decision-record-fields
  (let [registry (kernel/build-registry
                  [(gate :g/tooling
                         12
                         #(kernel/has-signal? % :tooling)
                         (fn [_]
                           {:obligations [{:id :must-web-run
                                           :value :required}]
                            :prohibitions [{:id :no-deferral
                                            :value :forbidden}]
                            :formatting-constraints [{:id :citation-style
                                                      :value :inline-brackets}]}))])
        decision (kernel/evaluate registry #{:tooling})]
    (is (= [{:id :g/tooling :priority 12}] (:triggered-gates decision)))
    (is (= [{:id :must-web-run
             :kind :obligations
             :source-gate-id :g/tooling
             :source-priority 12
             :value :required}]
           (:obligations decision)))
    (is (= [{:id :no-deferral
             :kind :prohibitions
             :source-gate-id :g/tooling
             :source-priority 12
             :value :forbidden}]
           (:prohibitions decision)))
    (is (= [{:id :citation-style
             :kind :formatting-constraints
             :source-gate-id :g/tooling
             :source-priority 12
             :value :inline-brackets}]
           (:formatting-constraints decision)))
    (is (= [] (:conflicts decision)))))

(deftest evaluate-priority-conflicts-pick-higher-priority-winner
  (let [high (gate :g/high
                   90
                   (constantly true)
                   (fn [_]
                     {:obligations [{:id :must-web-run
                                     :value :required}]}))
        low (gate :g/low
                  10
                  (constantly true)
                  (fn [_]
                    {:obligations [{:id :must-web-run
                                    :value :not-required}]}))
        decision (kernel/evaluate (kernel/build-registry [low high]) #{})]
    (is (= [{:id :must-web-run
             :kind :obligations
             :source-gate-id :g/high
             :source-priority 90
             :value :required}]
           (:obligations decision)))
    (is (= [{:constraint-id :must-web-run
             :kind :obligations
             :loser-gate-id :g/low
             :loser-priority 10
             :loser-value :not-required
             :winner-gate-id :g/high
             :winner-priority 90
             :winner-value :required}]
           (:conflicts decision)))))

(deftest evaluate-conflicts-are-deterministic-across-registry-order
  (let [high (gate :g/high
                   90
                   (constantly true)
                   (fn [_]
                     {:prohibitions [{:id :no-future-promises
                                      :value :strict}]}))
        low (gate :g/low
                  10
                  (constantly true)
                  (fn [_]
                    {:prohibitions [{:id :no-future-promises
                                     :value :soft}]}))
        registry-a (kernel/build-registry [low high])
        registry-b (kernel/build-registry [high low])
        decision-a (kernel/evaluate registry-a #{:any-signal})
        decision-b (kernel/evaluate registry-b #{:any-signal})]
    (testing "same winner and conflict metadata regardless of registration order"
      (is (= (:prohibitions decision-a) (:prohibitions decision-b)))
      (is (= (:conflicts decision-a) (:conflicts decision-b))))))
