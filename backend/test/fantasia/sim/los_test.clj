(ns fantasia.sim.los-test
  (:require [clojure.test :refer [deftest testing is]]
            [fantasia.sim.los :as los]
            [fantasia.sim.constants :as const]))

(deftest test-get-vision-radius
  (testing "Returns correct radius for wolf"
    (is (= const/wolf-vision-radius
           (los/get-vision-radius {:role :wolf}))))
  (testing "Returns correct radius for bear"
    (is (= const/bear-vision-radius
           (los/get-vision-radius {:role :bear}))))
  (testing "Returns correct radius for deer"
    (is (= const/deer-vision-radius
           (los/get-vision-radius {:role :deer}))))
  (testing "Returns default player radius for other roles"
    (is (= const/player-vision-radius
           (los/get-vision-radius {:role :priest})))
    (is (= const/player-vision-radius
           (los/get-vision-radius {:role :knight})))
    (is (= const/player-vision-radius
           (los/get-vision-radius {:role :peasant}))))
  (testing "Returns default for unknown role"
    (is (= const/player-vision-radius
           (los/get-vision-radius {:role :unknown})))))

(deftest test-positions-in-vision
  (testing "Returns center position for radius 0"
    (let [positions (los/positions-in-vision [0 0] 0)]
      (is (= [[0 0]] positions))))
  (testing "Returns center position for radius 1"
    (let [positions (los/positions-in-vision [5 5] 1)]
      (is (contains? (set positions) [5 5]))
      (is (<= 7 (count positions) 8))))
  (testing "Returns positions within hex distance"
    (let [positions (los/positions-in-vision [0 0] 2)]
      (is (contains? (set positions) [0 0]))
      (is (contains? (set positions) [0 1]))
      (is (contains? (set positions) [1 0]))
      (is (contains? (set positions) [-1 0]))
      (is (contains? (set positions) [0 -1]))
      (is (contains? (set positions) [1 -1]))
      (is (not (contains? (set positions) [2 2])))))
  (testing "Returns positions for larger radius"
    (let [positions (los/positions-in-vision [10 10] 5)]
      (is (> (count positions) 10))
      (is (contains? (set positions) [10 10]))))
  (testing "Radius 0 returns center position"
    (let [positions (los/positions-in-vision [0 0] 0)]
      (is (seq positions)))))

(deftest test-normalize-tile-key
  (testing "Converts vector [q r] to string 'q,r'"
    (is (= "0,0" (los/normalize-tile-key [0 0])))
    (is (= "5,10" (los/normalize-tile-key [5 10])))
    (is (= "-3,7" (los/normalize-tile-key [-3 7]))))
  (testing "Returns string unchanged if already string"
    (is (= "0,0" (los/normalize-tile-key "0,0")))
    (is (= "5,10" (los/normalize-tile-key "5,10"))))
  (testing "Handles malformed keys gracefully"
    (is (some? (los/normalize-tile-key "invalid")))
    (is (some? (los/normalize-tile-key :keyword)))))

(deftest test-string-to-vector-key
  (testing "Converts 'q,r' string to vector [q r]"
    (is (= [0 0] (los/string-to-vector-key "0,0")))
    (is (= [5 10] (los/string-to-vector-key "5,10")))
    (is (= [-3 7] (los/string-to-vector-key "-3,7"))))
  (testing "Handles negative coordinates"
    (is (= [-5 -10] (los/string-to-vector-key "-5,-10"))))
  (testing "Returns nil for invalid format"
    (is (nil? (los/string-to-vector-key "invalid")))
    (is (nil? (los/string-to-vector-key "0")))
    (is (nil? (los/string-to-vector-key "0,0,0")))
    (is (nil? (los/string-to-vector-key "")))))

(deftest test-agent-can-see
  (testing "Returns true when target is within vision radius"
    (is (true? (los/agent-can-see? [0 0] [0 1] 2)))
    (is (true? (los/agent-can-see? [5 5] [7 5] 3)))
    (is (true? (los/agent-can-see? [0 0] [1 -1] 2))))
  (testing "Returns true when target is at edge of vision radius"
    (is (true? (los/agent-can-see? [0 0] [0 2] 2)))
    (is (true? (los/agent-can-see? [0 0] [2 0] 2))))
  (testing "Returns false when target is outside vision radius"
    (is (false? (los/agent-can-see? [0 0] [0 3] 2)))
    (is (false? (los/agent-can-see? [0 0] [3 0] 2)))
    (is (false? (los/agent-can-see? [0 0] [10 10] 5))))
  (testing "Returns true for same position"
    (is (true? (los/agent-can-see? [0 0] [0 0] 0)))))

(deftest test-filter-visible-agents
  (testing "Returns agents within vision radius"
    (let [agents [{:id 1 :pos [0 0] :status {:alive? true}}
                 {:id 2 :pos [0 1] :status {:alive? true}}
                 {:id 3 :pos [10 10] :status {:alive? true}}]
          visible (los/filter-visible-agents {:agents agents} [0 0] 2)]
      (is (= 2 (count visible)))
      (is (some #(= (:id %) 1) visible))
      (is (some #(= (:id %) 2) visible))
      (is (not (some #(= (:id %) 3) visible)))))
  (testing "Filters out dead agents"
    (let [agents [{:id 1 :pos [0 0] :status {:alive? true}}
                 {:id 2 :pos [0 1] :status {:alive? false}}
                 {:id 3 :pos [1 0] :status {:alive? true}}]
          visible (los/filter-visible-agents {:agents agents} [0 0] 2)]
      (is (= 2 (count visible)))
      (is (some #(= (:id %) 1) visible))
      (is (not (some #(= (:id %) 2) visible)))))
  (testing "Returns empty list when no agents are visible"
    (let [agents [{:id 1 :pos [10 10] :status {:alive? true}}]
          visible (los/filter-visible-agents {:agents agents} [0 0] 2)]
      (is (empty? visible)))))

(deftest test-visible-agent-ids
  (testing "Returns set of visible agent IDs"
    (let [agents [{:id 1 :pos [0 0] :status {:alive? true}}
                 {:id 2 :pos [0 1] :status {:alive? true}}
                 {:id 3 :pos [10 10] :status {:alive? true}}]
          visible-ids (los/visible-agent-ids {:agents agents} [0 0] 2)]
      (is (= #{1 2} visible-ids))))
  (testing "Handles agents without status (assumes alive)"
    (let [agents [{:id 1 :pos [0 0]}
                 {:id 2 :pos [0 1]}]
          visible-ids (los/visible-agent-ids {:agents agents} [0 0] 2)]
      (is (= #{1 2} visible-ids))))
  (testing "Returns empty set when no agents visible"
    (let [agents [{:id 1 :pos [10 10] :status {:alive? true}}]
          visible-ids (los/visible-agent-ids {:agents agents} [0 0] 2)]
      (is (empty? visible-ids)))))

(deftest test-visible-tiles
  (testing "Returns tile keys visible from position"
    (let [tiles {"[0 0]" {:terrain :ground}
                "[0 1]" {:terrain :ground}
                "[1 0]" {:terrain :ground}
                "[10 10]" {:terrain :ground}}
          world {:tiles tiles}
          visible (los/visible-tiles world [0 0] 2)]
      (is (set? visible))))
  (testing "Handles world with no tiles"
    (let [world {:tiles {}}
          visible (los/visible-tiles world [0 0] 2)]
      (is (empty? visible)))))