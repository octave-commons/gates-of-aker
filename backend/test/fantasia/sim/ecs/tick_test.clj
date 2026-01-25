(ns fantasia.sim.ecs.tick-test
  (:require [clojure.test :refer [deftest testing is]]
            [fantasia.sim.ecs.tick :as tick]))

(deftest test-ecs-world-management
  (testing "Get ECS world"
    (let [world (tick/get-ecs-world)]
      (is (some? world))))
  
  (testing "Reset ECS world"
    (let [original-world (tick/get-ecs-world)]
      (tick/reset-ecs-world!)
      (let [new-world (tick/get-ecs-world)]
        (is (some? new-world))))))

(deftest test-run-systems
  (testing "Run systems with basic global state"
    (let [ecs-world (tick/get-ecs-world)
          global-state {:tick 5 :seed 42}
          result-world (tick/run-systems ecs-world global-state)]
      (is (some? result-world))))
  
  (testing "Run systems with levers"
    (let [ecs-world (tick/get-ecs-world)
          global-state {:tick 5 :seed 42 :levers {:cold-snap 0.8}}
          result-world (tick/run-systems ecs-world global-state)]
      (is (some? result-world)))))

(deftest test-spawn-initial-agents
  (testing "Spawn agents in rectangular bounds"
    (let [ecs-world (tick/get-ecs-world)
          bounds {:shape :rect :w 100 :h 100 :origin-q 0 :origin-r 0}
          result-world (tick/spawn-initial-agents! ecs-world bounds)]
      (is (some? result-world))))
  
  (testing "Spawn agents in radius bounds"
    (let [ecs-world (tick/get-ecs-world)
          bounds {:shape :radius :r 50 :origin-q 0 :origin-r 0}
          result-world (tick/spawn-initial-agents! ecs-world bounds)]
      (is (some? result-world)))))

(deftest test-create-ecs-initial-world
  (testing "Create initial world with defaults"
    (let [global-state (tick/create-ecs-initial-world {})]
      (is (some? global-state))
      (is (contains? global-state :bounds))
      (is (contains? global-state :seed))
      (is (contains? global-state :calendar))
      (is (contains? global-state :institutions))
      (is (contains? global-state :levers))))
  
  (testing "Create initial world with custom options"
    (let [global-state (tick/create-ecs-initial-world {:seed 999 :tree-density 0.1 :bounds {:shape :rect :w 50 :h 50}})]
      (is (= 999 (:seed global-state)))
      (is (= 0.1 (:tree-density global-state)))
      (is (= {:shape :rect :w 50 :h 50} (:bounds global-state)))))

(deftest test-legacy-compatibility
  (testing "Get state function"
    (tick/reset-ecs-world!)
    (let [global-state (tick/get-state)]
      (is (some? global-state))))
  
  (testing "Tick alias function exists"
    (is (fn? tick/tick!))))

(deftest test-stub-functions
  (testing "Set levers function exists"
    (is (fn? tick/set-levers!)))
  
  (testing "Set facet limit function exists"
    (is (fn? tick/set-facet-limit!)))
  
  (testing "Set vision radius function exists"
    (is (fn? tick/set-vision-radius!)))
  
  (testing "Unimplemented stub functions exist"
    (is (fn? tick/place-shrine!))
    (is (fn? tick/appoint-mouthpiece!))
    (is (fn? tick/place-wall-ghost!))
    (is (fn? tick/place-stockpile!))
    (is (fn? tick/place-warehouse!))
    (is (fn? tick/place-campfire!))
    (is (fn? tick/place-statue-dog!))
    (is (fn? tick/place-tree!))
    (is (fn? tick/place-deer!))
    (is (fn? tick/place-wolf!))
    (is (fn? tick/place-bear!))
    (is (fn? tick/queue-build-job!))
     (is (fn? tick/get-agent-path!)))))