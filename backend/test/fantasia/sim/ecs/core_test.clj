(ns fantasia.sim.ecs.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [brute.entity :as be]
            [fantasia.sim.ecs.core :as ecs]
            [fantasia.sim.ecs.components :as c]))

(deftest test-component-class
  (testing "Get component class for position"
    (let [position (c/->Position 5 10)
          component-class (ecs/component-class position)]
      (is (= (class position) component-class))))

  (testing "Get component class for role"
    (let [role (c/->Role :priest)
          component-class (ecs/component-class role)]
      (is (= (class role) component-class)))))

(deftest test-create-ecs-world
  (testing "Create new ECS world"
    (let [world (ecs/create-ecs-world)]
      (is (some? world))
      (is (not (empty? world))))))

(deftest test-tile-key-functions
  (testing "Tile key creation"
    (let [key (ecs/tile-key [5 10])]
      (is (= [5 10] key))))

  (testing "Tile key parsing"
    (let [key (ecs/parse-tile-key "5,10")]
      (is (= "5,10" key)))))

(deftest test-create-agent
  (testing "Create agent with minimal parameters"
    (let [world (ecs/create-ecs-world)
          [eid _ world'] (ecs/create-agent world nil 5 10 :priest)]
      (is (some? eid))
      (is (some? world'))))

  (testing "Create agent with custom ID"
    (let [world (ecs/create-ecs-world)
          custom-id (java.util.UUID/randomUUID)
          [eid _ world'] (ecs/create-agent world custom-id 5 10 :knight)]
      (is (= custom-id eid))))

  (testing "Create agent with options"
(let [world (ecs/create-ecs-world)
          options {:warmth 0.9 :food 0.8 :sleep 0.7}
          [eid _ world'] (ecs/create-agent world nil 5 10 :peasant options)]
      (is (some? eid)))))

(deftest test-create-tile
  (testing "Create tile with minimal parameters"
    (let [world (ecs/create-ecs-world)
          [_ _ world'] (ecs/create-tile world 5 10 :ground :forest :house nil)]
      (is (some? world'))))

  (testing "Create tile with options"
    (let [world (ecs/create-ecs-world)
          [_ _ world'] (ecs/create-tile world 5 10 :ground :plains :campfire nil {:tick 100})]
      (is (some? world'))))

  (testing "Create tile with shrine"
    (let [world (ecs/create-ecs-world)
          [_ _ world'] (ecs/create-tile world 5 10 :ground :plains :shrine nil {})]
      (is (some? world')))))

(deftest test-create-stockpile
  (testing "Create stockpile at position"
    (let [world (ecs/create-ecs-world)
          [_ _ world'] (ecs/create-stockpile world 5 10)]
      (is (some? world')))))

(deftest test-create-world-item
  (testing "Create world item"
    (let [world (ecs/create-ecs-world)
          [_ _ world'] (ecs/create-world-item world 5 10 :wood 5 100)]
      (is (some? world')))))

(deftest test-get-all-agents
  (testing "Get all agents from empty world"
    (let [world (ecs/create-ecs-world)
          agents (ecs/get-all-agents world)]
      (is (empty? agents))))

  (testing "Get all agents from world with agents"
    (let [world (ecs/create-ecs-world)
          [_ _ world1] (ecs/create-agent world nil 5 10 :priest)
          [_ _ world2] (ecs/create-agent world1 nil 6 11 :knight)
          agents (ecs/get-all-agents world2)]
      (is (= 2 (count agents))))))

(deftest test-get-all-tiles
  (testing "Get all tiles from empty world"
    (let [world (ecs/create-ecs-world)
          tiles (ecs/get-all-tiles world)]
      (is (empty? tiles))))

  (testing "Get all tiles from world with tiles"
    (let [world (ecs/create-ecs-world)
          [_ _ world1] (ecs/create-tile world 5 10 :ground :forest nil nil)
          tiles (ecs/get-all-tiles world1)]
      (is (= 1 (count tiles))))))

(deftest test-get-tile-at-pos
  (testing "Get tile at existing position"
    (let [world (ecs/create-ecs-world)
          [_ _ world1] (ecs/create-tile world 5 10 :ground :forest nil nil)
          tile-id (ecs/get-tile-at-pos world1 [5 10])]
      (is (some? tile-id))))

  (testing "Get tile at non-existent position"
    (let [world (ecs/create-ecs-world)
          tile-id (ecs/get-tile-at-pos world [99 99])]
      (is (nil? tile-id)))))

(deftest test-get-buildings-with-job-queue
  (testing "Get buildings with job queues"
    (let [world (ecs/create-ecs-world)
          buildings (ecs/get-buildings-with-job-queue world)]
      (is (empty? buildings)))))

(deftest test-get-all-world-items
  (testing "Get all world items"
    (let [world (ecs/create-ecs-world)
          items (ecs/get-all-world-items world)]
      (is (empty? items)))))

(deftest test-assign-job-to-agent
  (testing "Assign job to agent"
    (let [world (ecs/create-ecs-world)
          [eid _ world1] (ecs/create-agent world nil 5 10 :peasant)
          world2 (ecs/assign-job-to-agent world1 eid "job-123")]
      (is (some? world2)))))

(deftest test-set-agent-path
  (testing "Set agent path"
    (let [world (ecs/create-ecs-world)
          [eid _ world1] (ecs/create-agent world nil 5 10 :peasant)
          waypoints [[5 10] [6 11] [7 12]]
          world2 (ecs/set-agent-path world1 eid waypoints)]
      (is (some? world2)))))

(deftest test-update-agent-needs
  (testing "Update agent needs"
    (let [world (ecs/create-ecs-world)
          [eid _ world1] (ecs/create-agent world nil 5 10 :peasant)
          world2 (ecs/update-agent-needs world1 eid 0.5 0.8 0.9)]
      (is (some? world2)))))

(deftest test-update-agent-inventory
  (testing "Update agent inventory"
    (let [world (ecs/create-ecs-world)
          [eid _ world1] (ecs/create-agent world nil 5 10 :peasant)
          world2 (ecs/update-agent-inventory world1 eid 15 25)]
      (is (some? world2)))))

(deftest test-remove-component
  (testing "Remove component from entity"
    (let [world (ecs/create-ecs-world)
          [eid _ world1] (ecs/create-agent world nil 5 10 :peasant)
          position-type (ecs/component-class (c/->Position 5 10))
          world2 (ecs/remove-component world1 eid position-type)]
      (is (some? world2)))))

(deftest test-has-component
  (testing "Check if entity has component"
    (let [world (ecs/create-ecs-world)
          [eid _ world1] (ecs/create-agent world nil 5 10 :peasant)
          position-type (ecs/component-class (c/->Position 5 10))
          tile-type (ecs/component-class (c/->Tile :ground :plains nil nil))]
      (is (true? (ecs/has-component? world1 eid position-type)))
      (is (false? (ecs/has-component? world1 eid tile-type)))))
  )
