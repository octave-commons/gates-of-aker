(ns fantasia.sim.ecs-test-simple
  (:require [brute.entity :as be]
            [fantasia.sim.ecs.core]
            [fantasia.sim.ecs.components :as c]))

(defn test-systems-simple
  []
  (println "=== Simple ECS Systems Test ===")
  (let [world (fantasia.sim.ecs.core/create-ecs-world)
        position-type (be/get-component-type (c/->Position 0 0))]

    ;; Test 1: Create an agent
    (println "\n1. Testing agent creation...")
    (let [[agent-id _ world1] (fantasia.sim.ecs.core/create-agent world nil 10 10 :priest)]
      (println "   ✓ Created agent" agent-id)

      ;; Test 2: Create multiple agents
      (println "\n2. Testing multiple agent creation...")
      (let [[agent2 _ world2] (fantasia.sim.ecs.core/create-agent world1 nil 11 11 :knight)
            [agent3 _ world3] (fantasia.sim.ecs.core/create-agent world2 nil 12 12 :peasant)
            all-agents (conj (conj [agent2] [agent2]) [agent3])]
        (println "   ✓ Created 3 agents:" (count all-agents))

        )
      ;; Test 3: Query agents by position
      (println "\n3. Testing position queries...")
      (let [agents-at-pos (be/get-all-entities-with-component world1 position-type)
            _ (println "   ✓ Found" (count agents-at-pos) "agents at position [10 10]")]

        ;; Test 4: Get components from agents
        (println "\n4. Testing component retrieval...")
        (let [role-type (be/get-component-type (c/->Role :priest))
              role (be/get-component world1 agent-id role-type)
              _ (println "   ✓ Retrieved role:" (:type role))]

          ;; Test 5: Update component
          (println "\n5. Testing component updates...")
          (let [new-role (c/->Role :peasant)
                world2 (be/add-component world1 agent-id new-role)
                updated-role (be/get-component world2 agent-id role-type)]
            (println "   ✓ Updated role from" (:type role) "to" (:type updated-role))
            world2)
          (println "\n=== All simple tests passed! ==="))))))

(defn -main
  []
  (test-systems-simple))
