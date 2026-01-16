(ns fantasia.sim.tick-test
  (:require [clojure.test :refer [deftest is testing]]
            [fantasia.sim.tick :as tick]))

(deftest state-mutators-update-levers-and-positions
  (let [world (-> (tick/initial-world 11)
                  (assoc :shrine nil)
                  (assoc-in [:levers :mouthpiece-agent-id] nil))]
    (with-redefs [tick/*state (atom world)]
      (tick/set-levers! {:iconography {:fire->patron 0.33}
                         :new-lever {:intensity 0.5}})
      (tick/place-shrine! [5 6])
      (tick/appoint-mouthpiece! 99)
      (let [state (tick/get-state)]
        (is (= 0.33 (get-in state [:levers :iconography :fire->patron])))
        (is (= {:intensity 0.5} (get-in state [:levers :new-lever])))
        (is (= [5 6] (:shrine state)))
        (is (= 99 (get-in state [:levers :mouthpiece-agent-id])))))))

(deftest tick!-uses-tick-once-results
  (let [initial {:tick 0 :foo 1}
        seen (atom [])]
    (with-redefs [tick/*state (atom initial)
                  tick/tick-once (fn [world]
                                   (let [t (inc (:tick world))]
                                     (swap! seen conj t)
                                     {:world (assoc world :tick t :foo (+ (:foo world) t))
                                      :out {:tick t :value (:foo world)}}))]
      (let [outs (tick/tick! 3)
            final (tick/get-state)]
        (is (= [1 2 3] @seen))
        (is (= [{:tick 1 :value 1}
                {:tick 2 :value 2}
                {:tick 3 :value 4}] outs))
        (is (= 3 (:tick final)))
        (is (= 7 (:foo final)))))))
