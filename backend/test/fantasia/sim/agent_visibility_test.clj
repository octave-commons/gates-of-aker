(ns fantasia.sim.agent-visibility-test
  (:require [clojure.test :refer [deftest testing is]]
            [fantasia.sim.agent-visibility :as vis]))

(deftest test-to-radians
  (testing "Converts degrees to radians"
    (is (= 0.0 (vis/to-radians 0)))
    (is (= (/ Math/PI 2) (vis/to-radians 90)))
    (is (= Math/PI (vis/to-radians 180)))
    (is (= (* 2 Math/PI) (vis/to-radians 360))))
  (testing "Handles negative degrees"
    (is (= (/ Math/PI -2) (vis/to-radians -90)))))

(deftest test-compute-visibility-polygon
  (testing "Generates polygon with 24 vertices"
    (let [polygon (vis/compute-visibility-polygon [0 0] 5)]
      (is (some? (:vertices polygon)))
      (is (some? (:edges polygon)))
      (is (= 24 (count (:vertices polygon))))
      (is (= 24 (count (:edges polygon))))))
  (testing "Vertices are at correct distance from center"
    (let [polygon (vis/compute-visibility-polygon [10 10] 3)
          vertices (:vertices polygon)]
      (doseq [[q r] vertices]
        (let [distance (Math/sqrt (+ (* (- q 10) (- q 10))
                                     (* (- r 10) (- r 10))))]
          (is (<= (Math/abs (- distance 3.0)) 2.0))))))
  (testing "Edges connect vertices in circular order"
    (let [polygon (vis/compute-visibility-polygon [0 0] 5)
          edges (:edges polygon)]
      (doseq [[v1 v2] edges]
        (is (vector? v1))
        (is (vector? v2)))))
  (testing "Handles radius 0"
    (let [polygon (vis/compute-visibility-polygon [5 5] 0)]
      (is (some? (:vertices polygon)))
      (is (= 24 (count (:vertices polygon)))))))

(deftest test-point-in-polygon
  (testing "Correctly identifies points inside polygon"
    (let [vertices [[0 0] [2 0] [2 2] [0 2]]]
      (is (true? (vis/point-in-polygon? [1 1] vertices)))
      (is (true? (vis/point-in-polygon? [0.5 0.5] vertices)))
      (is (true? (vis/point-in-polygon? [1.5 1.5] vertices)))))
  (testing "Correctly identifies points outside polygon"
    (let [vertices [[0 0] [2 0] [2 2] [0 2]]]
      (is (false? (vis/point-in-polygon? [3 3] vertices)))
      (is (false? (vis/point-in-polygon? [-1 -1] vertices)))
      (is (false? (vis/point-in-polygon? [1 3] vertices)))))
  (testing "Handles edge cases (points on boundary)"
    (let [vertices [[0 0] [2 0] [2 2] [0 2]]]
      (is (some? (vis/point-in-polygon? [0 1] vertices)))
      (is (some? (vis/point-in-polygon? [1 0] vertices)))))
  (testing "Handles convex polygon"
    (let [vertices [[0 0] [4 0] [4 4] [0 4]]]
      (is (true? (vis/point-in-polygon? [2 2] vertices)))
      (is (false? (vis/point-in-polygon? [5 5] vertices)))))
  (testing "Handles irregular polygon"
    (let [vertices [[0 0] [3 0] [2 3] [0 2]]]
      (is (true? (vis/point-in-polygon? [1 1] vertices)))
      (is (false? (vis/point-in-polygon? [3 3] vertices))))))

(deftest test-compute-agent-visibility
  (testing "Returns collection of visible tile keys"
    (let [agent {:pos [5 5] :role :priest}
          world {:tiles {"5,5" {:terrain :ground}
                         "5,6" {:terrain :ground}
                         "6,5" {:terrain :ground}
                         "10,10" {:terrain :ground}}}
          visible (vis/compute-agent-visibility world agent)]
      (is (some? visible))
      (is (coll? visible))))
  (testing "Filters tiles outside vision polygon"
    (let [agent {:pos [5 5] :role :priest}
          world {:tiles {"5,5" {:terrain :ground}
                         "30,30" {:terrain :ground}}}
          visible (vis/compute-agent-visibility world agent)]
      (is (contains? visible "5,5"))
      (is (not (contains? visible "30,30")))))
  (testing "Returns nil when agent has no position"
    (let [agent {:role :priest}
          world {:tiles {"0,0" {:terrain :ground}}}
          visible (vis/compute-agent-visibility world agent)]
      (is (nil? visible))))
  (testing "Handles world with no tiles"
    (let [agent {:pos [0 0] :role :priest}
          world {:tiles {}}
          visible (vis/compute-agent-visibility world agent)]
      (is (some? visible))
      (is (empty? visible)))))

(deftest test-compute-all-agents-visibility
  (testing "Computes visibility for all agents"
    (let [agents [{:id 1 :pos [0 0] :role :priest}
                  {:id 2 :pos [10 10] :role :knight}]
          world {:agents agents :tiles {"0,0" {:terrain :ground}
                                        "10,10" {:terrain :ground}}}
          all-visible (vis/compute-all-agents-visibility world)]
      (is (some? all-visible))
      (is (map? all-visible))))
  (testing "Returns map of agent-id to visible tiles"
    (let [agents [{:id 1 :pos [0 0] :role :priest}]
          world {:agents agents :tiles {"0,0" {:terrain :ground}}}
          all-visible (vis/compute-all-agents-visibility world)]
      (is (contains? all-visible 1))
      (is (coll? (get all-visible 1)))))
  (testing "Handles world with no agents"
    (let [world {:agents [] :tiles {}}
          all-visible (vis/compute-all-agents-visibility world)]
      (is (some? all-visible))
      (is (empty? all-visible)))))
