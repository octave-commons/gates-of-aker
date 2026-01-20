(ns fantasia.sim.eat-job-test
  (:require [clojure.test :refer [deftest is testing]]
            [fantasia.sim.core :as core]
            [fantasia.sim.jobs :as j]))

(deftest eat-job-targets-stockpile-when-no-fruit
  (testing "Eat jobs should target fruit stockpiles when no fruit exists"
    (let [world (-> (core/initial-world 20)
                    (assoc :jobs [])
                    (assoc :items {})
                    (assoc :stockpiles {})
                    (assoc-in [:stockpiles "0,0"] {:resource :fruit :max-qty 200 :current-qty 100})
                    (assoc-in [:stockpiles "8,8"] {:resource :fruit :max-qty 200 :current-qty 100})
                    (assoc-in [:agents 0 :needs :food] 0.2))
          world-after (j/generate-need-jobs! world)
          jobs (:jobs world-after)
          stockpile-positions (set (map (fn [k]
                                          (let [[q r] (clojure.string/split k #",")]
                                            [(Integer/parseInt q) (Integer/parseInt r)]))
                                        (keys (:stockpiles world-after))))]
      (is (seq jobs) "Should have generated jobs")
      (let [eat-job (first (filter #(= (:type %) :job/eat) jobs))]
        (is eat-job "Should have generated an eat job")
        (is (contains? stockpile-positions (:target eat-job))
            "Eat job should target a fruit stockpile")))))

(deftest eat-job-targets-fruit-when-available
  (testing "Eat jobs should prefer fruit items over stockpiles"
    (let [world (-> (core/initial-world 20)
                    (assoc :jobs [])
                    (assoc :items {})
                    (assoc :stockpiles {})
                    (assoc-in [:items "7,7"] {:fruit 5})
                    (assoc-in [:stockpiles "8,8"] {:resource :fruit :max-qty 200 :current-qty 100})
                    (assoc-in [:agents 0 :needs :food] 0.2))
          world-after (j/generate-need-jobs! world)
          jobs (:jobs world-after)]
      (is (seq jobs) "Should have generated jobs")
      (let [eat-job (first (filter #(= (:type %) :job/eat) jobs))]
        (is eat-job "Should have generated an eat job")
        (is (= (:target eat-job) [7 7]) "Eat job should prefer fruit at [7 7]")))))

(deftest complete-eat-consumes-from-stockpile
  (testing "Complete-eat! should consume from stockpile"
    (let [world (-> (core/initial-world 20)
                    (assoc :stockpiles {})
                    (assoc-in [:stockpiles "5,5"] {:resource :fruit :max-qty 200 :current-qty 100}))
          job {:type :job/eat :target [5 5]}
          world-after (j/complete-eat! world job 0)
          stockpile-key "5,5"
          stockpile (get-in world-after [:stockpiles stockpile-key])]
      (is (some? stockpile) "Stockpile should exist")
      (is (= (:current-qty stockpile) 99) "Stockpile should have 99 fruit remaining"))))
