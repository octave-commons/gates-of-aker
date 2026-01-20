(ns fantasia.sim.world-test
  (:require [clojure.test :refer [deftest is testing]]
            [fantasia.sim.core :as core]
            [fantasia.sim.world :as world]))

(defn approx= [expected actual]
  (<= (Math/abs (- (double expected) (double actual))) 1.0e-6))

(deftest snapshot-structure
  (let [base (core/initial-world 10)
        attr {:winter-pyre {:claim/storm 0.7 :claim/patron 0.3}}
        snap (world/snapshot base attr)]
    (is (= (:tick base) (:tick snap)))
    (is (= (:levers base) (:levers snap)))
    (is (map? (:calendar snap)))
    (is (vector? (:agents snap)))
    (is (= attr (:attribution snap)))
    (is (map? (:ledger snap)))))

(deftest snapshot-trims-top-facets-and-formats-ledger
  (let [agent {:id 7
               :pos [1 2]
               :role :scribe
               :needs {:warmth 0.5 :food 0.6 :sleep 0.7}
               :recall {:winter 0.1}
               :frontier (into {}
                                (map (fn [idx]
                                       [(keyword (str "facet" idx)) {:a (double (/ idx 10.0))}])
                                     (range 12))) }
        ledger {[:winter-pyre :claim/a] {:buzz 2.0 :tradition 1.0 :mentions 3}}
        base {:tick 5
              :shrine [3 3]
              :levers {:foo true}
              :recent-events []
              :agents [agent]
              :ledger ledger}
        snap (world/snapshot base {:winter-pyre {:claim/a 1.0}})
        top-facets (get-in snap [:agents 0 :top-facets])]
    (is (= 8 (count top-facets)))
    (is (= :facet11 (:facet (first top-facets))))
    (is (= {"winter-pyre/a" {:buzz 2.0 :tradition 1.0 :mentions 3}}
           (:ledger snap)))))

(deftest update-ledger-applies-decay-and-mentions
  (let [base {:ledger {[:winter-pyre :claim/old]
                       {:buzz 10.0 :tradition 5.0 :mentions 2}}}
        mentions [{:event-type :winter-pyre
                   :claim :claim/old
                   :weight 1.0
                   :event-instance :e1}
                  {:event-type :winter-pyre
                   :claim :claim/new
                   :weight 0.5}]
        {:keys [ledger attribution]} (world/update-ledger base mentions)
        old (get ledger [:winter-pyre :claim/old])
        new (get ledger [:winter-pyre :claim/new])]
    (testing "decay then mention update"
      (is (approx= (+ (* 10.0 0.90) 1.0) (:buzz old)))
      (is (approx= (+ (* 5.0 0.995) (* 0.12 (Math/log 2.0))) (:tradition old)))
      (is (= 3 (:mentions old)))
      (is (= #{:e1} (:event-instances old))))
    (testing "new mention entry created"
      (is (approx= 0.5 (:buzz new)))
      (is (= 1 (:mentions new))))
    (is (contains? attribution :winter-pyre))))
