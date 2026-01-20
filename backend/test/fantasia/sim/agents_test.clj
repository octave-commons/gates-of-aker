(ns fantasia.sim.agents-test
  (:require [clojure.test :refer [deftest is testing]]
            [fantasia.sim.agents :as agents]
            [fantasia.sim.core :as core]
            [fantasia.sim.world :as world]))

(defn approx= [expected actual]
  (<= (Math/abs (- (double expected) (double actual))) 1.0e-6))

(deftest scaled-edges-respect-levers
  (let [world (-> (core/initial-world 1)
                  (assoc-in [:levers :iconography]
                            {:fire->patron 0.91
                             :lightning->storm 0.33}))
        edges (agents/scaled-edges world)]
    (is (= 0.91 (get edges [:fire :patron/fire])))
    (is (= 0.33 (get edges [:lightning :storm])))
    (is (= 0.85 (get edges [:storm :deity/storm])))))

(deftest scaled-edges-defaults-when-levers-missing
  (let [world (-> (core/initial-world 1)
                  (assoc :edges {[:fire :patron/fire] 0.75}))
        edges (agents/scaled-edges (update world :levers dissoc :iconography))]
    (is (= 0.80 (get edges [:fire :patron/fire])))
    (is (= 0.75 (get edges [:lightning :storm])))))


(deftest update-needs-responds-to-cold
  (let [world (assoc (core/initial-world 5) :cold-snap 0.9)
        agent {:needs {:warmth 0.7 :food 0.7 :sleep 0.7}}
        updated (agents/update-needs world agent)]
    (is (< (get-in updated [:needs :warmth]) 0.7))
    (is (< (get-in updated [:needs :food]) 0.7))
    (is (< (get-in updated [:needs :sleep]) 0.7))))

(deftest update-needs-decay-without-cold
  (let [world (assoc (core/initial-world 5) :cold-snap 0.0)
        agent {:needs {:warmth 0.6 :food 0.6 :sleep 0.6}}
        updated (agents/update-needs world agent)]
    (is (< (get-in updated [:needs :warmth]) 0.6))
    (is (< (get-in updated [:needs :food]) 0.6))
    (is (< (get-in updated [:needs :sleep]) 0.6))))

(deftest update-needs-slow-food-decay-while-asleep
  (let [world (core/initial-world 5)
        base-agent {:pos [0 0]
                    :needs {:warmth 0.6 :food 0.6 :sleep 0.6}}
        awake (agents/update-needs world (assoc base-agent :status {:alive? true :asleep? false}))
        asleep (agents/update-needs world (assoc base-agent :status {:alive? true :asleep? true}))]
    (is (< (get-in awake [:needs :food]) 0.6))
    (is (> (get-in asleep [:needs :food]) (get-in awake [:needs :food])))
    (is (< (get-in awake [:needs :sleep]) 0.6))
    (is (approx= 0.6 (get-in asleep [:needs :sleep])))))

(deftest choose-packet-reflects-context
  (let [world (-> (core/initial-world 2)
                  (assoc :trees #{[2 2]})
                  (assoc :shrine [3 2]))
        priest {:id 10 :pos [2 2] :role :priest :needs {:warmth 0.8 :food 0.6 :sleep 0.6}}
        cold-peasant {:id 11 :pos [5 5] :role :peasant :needs {:warmth 0.22 :food 0.5 :sleep 0.5}}
        priest-packet (agents/choose-packet world priest)
        cold-packet (agents/choose-packet world cold-peasant)]
    (testing "priests convert with judgment + shrine fire"
      (is (= :convert (:intent priest-packet)))
      (is (some #{:judgment} (:facets priest-packet)))
      (is (some #{:fire} (:facets priest-packet)))
      (is (= 0.3 (get-in priest-packet [:tone :awe]))))
    (testing "cold agents warn and inject fear"
      (is (= :warn (:intent cold-packet)))
      (is (some #{:fear} (:facets cold-packet)))
      (is (= 0.6 (get-in cold-packet [:tone :urgency]))))))

(deftest choose-packet-neutral-context
  (let [world (core/initial-world 1)
        agent {:id 5 :pos [0 0] :role :peasant :needs {:warmth 0.5 :food 0.5 :sleep 0.5}}
        packet (agents/choose-packet world agent)]
    (is (= :chatter (:intent packet)))
    (is (= [:cold] (:facets packet)))))

(deftest interactions-produce-adjacent-pairs
  (let [agents [{:id 1 :pos [0 0]}
                {:id 2 :pos [0 1]}
                {:id 3 :pos [5 5]}]
        pairs (agents/interactions agents)]
    (is (= [[{:id 1 :pos [0 0]} {:id 2 :pos [0 1]}]
            [{:id 2 :pos [0 1]} {:id 1 :pos [0 0]}]]
           pairs))))

(deftest apply-packet-to-listener-updates-frontier
  (let [world (core/initial-world 8)
        speaker (first (:agents world))
        listener (second (:agents world))
        packet (assoc (agents/choose-packet world speaker)
                      :event-token {:type :winter-pyre :instance-id :i})
        res (agents/apply-packet-to-listener world listener speaker packet)]
    (is (map? (:frontier (:listener res))))
    (is (vector? (:mentions res)))
    (is (vector? (:traces res)))))

(deftest recall-and-mentions-updates-recall-map
  (let [frontier {:winter {:a 0.3}
                  :fire {:a 0.1}}
        packet {:facets [:winter]
                :tone {:awe 0.9 :urgency 0.5}
                :claim-hint :claim/winter-judgment-flame
                :event-token {:type :winter-pyre :instance-id :evt1}}
        listener {:id 42 :frontier frontier :recall {}}
        speaker {:id 99 :role :priest}
        res (agents/recall-and-mentions (:recall listener)
                                        frontier
                                        (assoc packet :listener-id 42 :speaker-id 99 :tick 0)
                                        speaker)]
    (is (> (get-in res [:new-recall :winter-pyre] 0.0) 0.0))
    (is (vector? (:mentions res)))
    (is (vector? (:traces res)))))
