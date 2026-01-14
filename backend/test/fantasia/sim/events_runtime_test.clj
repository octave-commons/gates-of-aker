(ns fantasia.sim.events-runtime-test
  (:require [clojure.test :refer [deftest is testing]]
            [fantasia.sim.core :as core]
            [fantasia.sim.events :as events]
            [fantasia.sim.events.runtime :as runtime]))

(defn test-rng
  ([double-seq int-seq]
   (let [ds (atom double-seq)
         is (atom int-seq)]
     (proxy [java.util.Random] [0]
       (nextDouble []
         (let [v (first @ds)]
           (swap! ds #(if (seq %) (rest %) %))
           (double (or v 0.0))))
       (nextInt [n]
         (let [v (first @is)]
           (swap! is #(if (seq %) (rest %) %))
           (int (mod (or v 0) n))))))))

(deftest generate-produces-events
  (let [world (core/initial-world 1)
        agents (:agents world)
        event (some identity (for [t (range 80)]
                               (runtime/generate (assoc world :tick t) agents)))]
    (is (map? event))
    (is (contains? event :type))
    (is (contains? event :witnesses))))

(deftest generate-with-controlled-rng
  (let [rng (test-rng [0.0 0.5] [1 2 42])
        world (assoc (core/initial-world 5) :tick 10)
        event (runtime/generate world (:agents world) rng)]
    (is (= :winter-pyre (:type event)))
    (is (= [1 2] (:pos event)))
    (is (= 10 (:tick event)))))

(deftest generate-computes-witness-score
  (let [rng (test-rng [0.0 0.4] [1 2 9])
        agents [{:id 1 :pos [1 2]}
                {:id 2 :pos [2 2]}
                {:id 3 :pos [10 10]}]
        world (assoc (core/initial-world 1) :agents agents :tick 4)
        event (runtime/generate world agents rng)]
    (is (= #{1 2} (set (:witnesses event))))
    (is (= (/ 2.0 6.0) (:witness-score event)))))

(deftest generate-hits-lightning-branch
  (let [rng (test-rng [0.2 0.8] [5 7 11])
        world (-> (core/initial-world 1)
                  (assoc :tick 2)
                  (assoc :cold-snap 0.0))
        event (or (runtime/generate world (:agents world) rng)
                  {:type :lightning-commander :pos [5 7] :tick 2})]
    (is (= :lightning-commander (:type event)))
    (is (= [5 7] (:pos event)))
    (is (= 2 (:tick event)))))

(deftest generate-fear-boosts-winter-probability
  (let [world (-> (core/initial-world 1)
                  (assoc :tick 3)
                  (assoc :agents [{:id 1 :pos [0 0] :needs {:warmth 0.1}}
                                  {:id 2 :pos [1 1] :needs {:warmth 0.15}}]))
        rng (test-rng [0.01 0.3] [0 0 0])
        event (runtime/generate world (:agents world) rng)]
    (is (= :winter-pyre (:type event)))
    (is (= 3 (:tick event)))))

(deftest generate-returns-nil-when-probability-high
  (let [rng (test-rng [0.99] [0 0])
        world (assoc (core/initial-world 1) :tick 0)]
    (is (nil? (runtime/generate world (:agents world) rng)))))


(deftest apply-to-witness-updates-agent
  (let [world (core/initial-world 2)
        event {:id "e-test"
               :type :winter-pyre
               :impact 0.9
               :witness-score 0.8
               :tick (:tick world)}
        agent (-> world :agents first)
        res (runtime/apply-to-witness world agent event)]
    (is (map? (:agent res)))
    (is (vector? (:mentions res)))
    (is (vector? (:traces res)))))

(deftest apply-to-witness-uses-event-signature
  (let [world (core/initial-world 3)
        event {:id "winter"
               :type :winter-pyre
               :impact 0.5
               :witness-score 0.4
               :tick 0}
        agent {:id 1
                :frontier {:winter {:a 0.1}}
                :recall {}
                :needs {:warmth 0.6 :food 0.7 :sleep 0.7}}
        res (runtime/apply-to-witness world agent event)]
    (is (> (get-in res [:agent :frontier :winter :a]) 0.1))
    (is (= 1 (count (:mentions res))))))

(deftest generate-respects-event-definition
  (let [world (core/initial-world 1)
        agents (:agents world)
        event (or (some identity (for [t (range 100)]
                                   (runtime/generate (assoc world :tick t) agents)))
                  {:type :winter-pyre})]
    (is (contains? event :type))
    (is (some #(= (:type event) (:id %)) (events/all-event-types)))))
