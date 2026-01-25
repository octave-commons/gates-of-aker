(ns fantasia.sim.delta-test
  (:require [clojure.test :refer [deftest testing is]]
            [fantasia.sim.delta :as delta]))

(deftest test-map-delta
  (testing "Returns only changed keys in new map"
    (let [old {:a 1 :b 2 :c 3}
          new {:a 1 :b 5 :c 3}
          result (delta/map-delta old new)]
      (is (not (contains? result :a)))
      (is (contains? result :b))
      (is (= 5 (:b result)))
      (is (not (contains? result :c)))))
  (testing "Returns empty map when no changes"
    (let [old {:a 1 :b 2}
          new {:a 1 :b 2}
          result (delta/map-delta old new)]
      (is (empty? result))))
  (testing "Handles nil old map (initial state)"
    (let [new {:a 1 :b 2}
          result (delta/map-delta nil new)]
      (is (= new result))))
  (testing "Handles new keys added"
    (let [old {:a 1}
          new {:a 1 :b 2}
          result (delta/map-delta old new)]
      (is (not (contains? result :a)))
      (is (contains? result :b))
      (is (= 2 (:b result)))))
  (testing "Handles keys removed"
    (let [old {:a 1 :b 2}
          new {:a 1}
          result (delta/map-delta old new)]
      (is (empty? result))))
  (testing "Preserves nested structure"
    (let [old {:a {:b 1}}
          new {:a {:b 2}}
          result (delta/map-delta old new)]
      (is (contains? result :a))
      (is (= {:b 2} (:a result))))))

(deftest test-agent-delta
  (testing "Returns delta for changed agent fields"
    (let [old {:id 1 :pos [0 0] :role :priest :needs {:food 0.8}}
          new {:id 1 :pos [0 1] :role :priest :needs {:food 0.6}}
          result (delta/agent-delta old new)]
      (is (not (contains? result :id)))
      (is (contains? result :pos))
      (is (= [0 1] (:pos result)))
      (is (not (contains? result :role)))
      (is (contains? result :needs))))
  (testing "Returns empty map when agent unchanged"
    (let [agent {:id 1 :pos [0 0] :role :priest}
          result (delta/agent-delta agent agent)]
      (is (empty? result))))
  (testing "Handles nil old agent (new agent)"
    (let [new {:id 1 :pos [0 0] :role :priest}
          result (delta/agent-delta nil new)]
      (is (= new result))))
  (testing "Handles complex agent structures"
    (let [old {:id 1 :inventory {:wood 5 :food 3} :needs {:food 0.8 :warmth 0.9}}
          new {:id 1 :inventory {:wood 5 :food 2} :needs {:food 0.6 :warmth 0.9}}
          result (delta/agent-delta old new)]
      (is (not (contains? result :id)))
      (is (contains? result :inventory))
      (is (contains? result :needs)))))

(deftest test-world-delta-initial
  (testing "Returns initial delta with all agents when old-world is nil"
    (let [new-world {:tick 10
                     :agents [{:id 1 :pos [0 0] :role :priest}
                              {:id 2 :pos [1 0] :role :knight}]
                     :tiles {"0,0" {:terrain :ground}
                             "1,0" {:terrain :ground}}
                     :items [{:pos [0 0] :resource :wood}]
                     :temperature 0.6
                     :daylight 0.8
                     :calendar {:day 1}
                     :levers {:test true}}
          result (delta/world-delta nil new-world)]
      (is (some? result))
      (is (= 10 (:tick result)))
      (is (some? (:global-updates result)))
      (is (= 2 (count (:changed-agents result))))
      (is (= 2 (count (:changed-tiles result))))
      (is (= 1 (count (:changed-items result)))))))

(deftest test-world-delta-changes
  (testing "Computes delta with only changed agents"
    (let [old-world {:tick 10
                     :agents [{:id 1 :pos [0 0] :role :priest}
                              {:id 2 :pos [1 0] :role :knight}]
                     :tiles {"0,0" {:terrain :ground}
                             "1,0" {:terrain :ground}}}
          new-world {:tick 11
                     :agents [{:id 1 :pos [0 1] :role :priest}
                              {:id 2 :pos [1 0] :role :knight}]
                     :tiles {"0,0" {:terrain :ground}
                             "1,0" {:terrain :forest}}}
          result (delta/world-delta old-world new-world)]
      (is (= 11 (:tick result)))
      (is (= 1 (count (:changed-agents result))))
      (is (contains? (:changed-agents result) "1"))
      (is (some? (:changed-tiles result)))))
  (testing "Computes delta with only changed tiles"
    (let [old-world {:tick 10
                     :agents [{:id 1 :pos [0 0]}]
                     :tiles {"0,0" {:terrain :ground}
                             "1,0" {:terrain :ground}}}
          new-world {:tick 11
                     :agents [{:id 1 :pos [0 0]}]
                     :tiles {"0,0" {:terrain :ground}
                             "1,0" {:terrain :forest}}}
          result (delta/world-delta old-world new-world)]
      (is (= 11 (:tick result)))
      (is (empty? (:changed-agents result)))
      (is (some? (:changed-tiles result)))))
  (testing "Includes global state updates"
    (let [old-world {:tick 10 :temperature 0.6 :daylight 0.8}
          new-world {:tick 11 :temperature 0.7 :daylight 0.9}
          result (delta/world-delta old-world new-world)]
      (is (some? (:global-updates result)))
      (is (= 11 (get-in result [:global-updates :tick])))
      (is (= 0.7 (get-in result [:global-updates :temperature])))
      (is (= 0.9 (get-in result [:global-updates :daylight])))))
  (testing "Handles empty old-world"
    (let [new-world {:tick 10 :agents [] :tiles {}}
          result (delta/world-delta {} new-world)]
      (is (some? result))))
  (testing "Handles items and stockpiles"
    (let [old-world {:tick 10
                     :items [{:pos [0 0] :resource :wood}]
                     :stockpiles {"0,0" {:log 5}}}
          new-world {:tick 11
                     :items [{:pos [0 0] :resource :wood}
                              {:pos [1 0] :resource :food}]
                     :stockpiles {"0,0" {:log 10}}}
          result (delta/world-delta old-world new-world)]
      (is (some? (:changed-items result)))
      (is (some? (:changed-stockpiles result))))))
