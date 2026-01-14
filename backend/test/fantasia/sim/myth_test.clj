(ns fantasia.sim.myth-test
  (:require [clojure.test :refer [deftest is testing]]
            [fantasia.sim.myth :as myth]))

(defn approx= [expected actual]
  (<= (Math/abs (- (double expected) (double actual))) 1.0e-9))

(deftest decay-ledger-dials-down-values
  (let [ledger {[:winter :claim] {:buzz 10.0 :tradition 5.0}}
        decayed (myth/decay-ledger ledger)]
    (is (approx= (* 10.0 0.90) (get-in decayed [[:winter :claim] :buzz])))
    (is (approx= (* 5.0 0.995) (get-in decayed [[:winter :claim] :tradition])))))

(deftest add-mention-tracks-buzz-tradition-and-instances
  (let [mention {:event-type :winter-pyre
                 :claim :claim/winter-judgment-flame
                 :weight 0.5
                 :event-instance :e-1}
        ledger (myth/add-mention {} mention)
        k [:winter-pyre :claim/winter-judgment-flame]]
    (is (approx= 0.5 (get-in ledger [k :buzz])))
    (is (= 1 (get-in ledger [k :mentions])))
    (is (= #{:e-1} (get-in ledger [k :event-instances])))))

(deftest attribution-normalizes-tradition
  (let [ledger (-> {}
                   (myth/add-mention {:event-type :winter-pyre
                                       :claim :claim/one
                                       :weight 1.0})
                   (myth/add-mention {:event-type :winter-pyre
                                       :claim :claim/two
                                       :weight 3.0}))
        probs (myth/attribution ledger :winter-pyre)
        expected-one (/ (Math/log 2.0)
                        (+ (Math/log 2.0) (Math/log 4.0)))
        expected-two (/ (Math/log 4.0)
                        (+ (Math/log 2.0) (Math/log 4.0)))]
    (is (approx= expected-one (get probs :claim/one)))
    (is (approx= expected-two (get probs :claim/two)))
    (is (approx= 1.0 (+ (get probs :claim/one) (get probs :claim/two))))))
