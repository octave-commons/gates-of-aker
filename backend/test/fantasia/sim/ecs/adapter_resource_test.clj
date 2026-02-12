(ns fantasia.sim.ecs.adapter-resource-test
  (:require [clojure.test :refer [deftest is testing]]
            [fantasia.sim.ecs.adapter :as adapter]
            [fantasia.sim.ecs.components :as c]
            [fantasia.sim.ecs.core :as ecs]
            [fantasia.sim.ecs.systems.mortality :as mortality]))

(defn- low-food-needs []
  (c/->Needs 0.8 0.1 0.8 1.0 0.8 0.6 0.5 0.5 0.5 0.6 0.5 0.5 0.5))

(deftest tile-resource-normalization
  (testing "keyword and colon-prefixed string resources normalize to plain strings"
    (let [world (ecs/create-ecs-world)
          [_ tile-id-a world-a] (ecs/create-tile world 0 0 :ground :forest nil :tree)
          [_ tile-id-b world-b] (ecs/create-tile world-a 1 0 :ground :plains nil ":grain")
          tile-a (adapter/ecs->tile-map world-b tile-id-a)
          tile-b (adapter/ecs->tile-map world-b tile-id-b)]
      (is (= "tree" (:resource tile-a)))
      (is (= "grain" (:resource tile-b))))))

(deftest agent-status-included-in-ecs-agent-map
  (testing "adapter includes alive/death status for UI rendering"
    (let [world (ecs/create-ecs-world)
          [agent-id world1] (ecs/create-agent world nil 0 0 :peasant {:needs (low-food-needs)})
          dead-world (mortality/process world1 7)
          agent-map (adapter/ecs->agent-map dead-world agent-id)]
      (is (= false (get-in agent-map [:status :alive?])))
      (is (= :starvation (get-in agent-map [:status :cause-of-death]))))))
