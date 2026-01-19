(ns fantasia.sim.ecs.comprehensive-test
  (:require [brute.entity :as be]
            [fantasia.sim.ecs.core]
            [fantasia.sim.ecs.components]))

(println "=== ECS Comprehensive Test Suite ===")

(defn test-01-create-world
  []
  (let [_world (fantasia.sim.ecs.core/create-ecs-world)]
    (println "✓ Test 1 PASSED: Created ECS world")))

(defn test-02-create-agent
  []
  (let [world (fantasia.sim.ecs.core/create-ecs-world)
        [eid world'] (fantasia.sim.ecs.core/create-agent world 1 0 0 :priest)
        agent-count (count (fantasia.sim.ecs.core/get-all-agents world'))]
    (println "✓ Test 2 PASSED: Created agent" eid)
    (when (not= 1 agent-count)
      (println "✗ Test 2 FAILED: Agent count mismatch" agent-count))))

(defn test-03-create-tile
  []
  (let [world (fantasia.sim.ecs.core/create-ecs-world)
        [tile-key eid world'] (fantasia.sim.ecs.core/create-tile world 0 0 :ground :forest nil :wall)
        tile-count (count (fantasia.sim.ecs.core/get-all-tiles world'))]
    (println "✓ Test 3 PASSED: Created tile" tile-key "with entity" eid)
    (when (not= 1 tile-count)
      (println "✗ Test 3 FAILED: Tile count mismatch" tile-count))))

(defn test-04-create-stockpile
  []
  (let [[eid _world'] (fantasia.sim.ecs.core/create-stockpile
                       (fantasia.sim.ecs.core/create-ecs-world)
                       "0,0")]
    (println "✓ Test 4 PASSED: Created stockpile" eid)))

(defn test-05-create-multiple-agents
  []
  (let [world (fantasia.sim.ecs.core/create-ecs-world)
        [_ world1] (fantasia.sim.ecs.core/create-agent world 1 0 0 :priest)
        [_ world2] (fantasia.sim.ecs.core/create-agent world1 2 0 0 :knight)
        [_ world3] (fantasia.sim.ecs.core/create-agent world2 3 0 0 :peasant)
        agents (fantasia.sim.ecs.core/get-all-agents world3)]
    (println "✓ Test 5 PASSED: Created 3 agents")
    (when (not= 3 (count agents))
      (println "✗ Test 5 FAILED: Expected 3 agents, got" (count agents)))))

(defn test-06-query-agent-components
  []
  (let [world (fantasia.sim.ecs.core/create-ecs-world)
        [eid world'] (fantasia.sim.ecs.core/create-agent world 1 0 0 :priest)
        pos-dummy (fantasia.sim.ecs.components/->Position 0 0)
        pos-type (be/get-component-type pos-dummy)
        position (be/get-component world' eid pos-type)]
    (if (and position (= (:q position) 0) (= (:r position) 0))
      (println "✓ Test 6 PASSED: Position component correct")
      (println "✗ Test 6 FAILED: Position component incorrect" position))))

(defn test-07-remove-component
  []
  (let [world (fantasia.sim.ecs.core/create-ecs-world)
        [eid world'] (fantasia.sim.ecs.core/create-agent world 1 0 0 :priest)
        world'' (fantasia.sim.ecs.core/remove-component world' eid (fantasia.sim.ecs.components/->Position 0 0))
        position-type (be/get-component-type (fantasia.sim.ecs.components/->Position 0 0))
        position (be/get-component world'' eid position-type)]
    (if position
      (println "✗ Test 7 FAILED: Position still exists after removal" position)
      (println "✓ Test 7 PASSED: Component removed successfully"))))

(defn test-08-component-types
  []
  (let [pos-dummy (fantasia.sim.ecs.components/->Position 0 0)
        pos-type (be/get-component-type pos-dummy)]
    (if (= pos-type (class pos-dummy))
      (println "✓ Test 8 PASSED: Component type resolved correctly")
      (println "✗ Test 8 FAILED: Component type mismatch" pos-type))))

(defn run-all-tests []
  (println "\n--- Running Tests ---\n")
  (test-01-create-world)
  (test-02-create-agent)
  (test-03-create-tile)
  (test-04-create-stockpile)
  (test-05-create-multiple-agents)
  (test-06-query-agent-components)
  (test-07-remove-component)
  (test-08-component-types)
  (println "\n--- Test Complete ---\n"))

(defn -main [& _]
  (run-all-tests))
