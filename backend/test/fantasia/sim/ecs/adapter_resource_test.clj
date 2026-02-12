(ns fantasia.sim.ecs.adapter-resource-test
  (:require [clojure.test :refer [deftest is testing]]
            [fantasia.sim.ecs.adapter :as adapter]
            [fantasia.sim.ecs.core :as ecs]))

(deftest tile-resource-normalization
  (testing "keyword and colon-prefixed string resources normalize to plain strings"
    (let [world (ecs/create-ecs-world)
          [_ tile-id-a world-a] (ecs/create-tile world 0 0 :ground :forest nil :tree)
          [_ tile-id-b world-b] (ecs/create-tile world-a 1 0 :ground :plains nil ":grain")
          tile-a (adapter/ecs->tile-map world-b tile-id-a)
          tile-b (adapter/ecs->tile-map world-b tile-id-b)]
      (is (= "tree" (:resource tile-a)))
      (is (= "grain" (:resource tile-b))))))
