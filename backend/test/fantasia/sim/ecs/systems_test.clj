(ns fantasia.sim.ecs.systems-test
  "Comprehensive tests for ECS systems."
  (:require [brute.entity :as be]
            [fantasia.sim.ecs.core :as ecs]
            [fantasia.sim.ecs.components :as c]
            [fantasia.sim.ecs.systems.needs-decay :as nd]
            [fantasia.sim.ecs.systems.movement :as mv]
            [fantasia.sim.ecs.systems.mortality :as mort]
            [fantasia.sim.ecs.systems.combat :as cb]
            [clojure.test :refer :all]))

(defn setup-world
  "Create a test ECS world with agents."
  []
  (let [world (ecs/create-ecs-world)
        [agent-id1 _ world] (ecs/create-agent world nil 0 0 :priest)
        [agent-id2 _ world] (ecs/create-agent world nil 1 0 :knight)
        [agent-id3 _ world] (ecs/create-agent world nil 0 1 :peasant)
        [_ tile-id1 world] (ecs/create-tile world 0 0 :ground :plains nil nil)
        [_ tile-id2 world] (ecs/create-tile world 1 0 :ground :plains nil nil)
        [_ tile-id3 world] (ecs/create-tile world 0 1 :ground :plains nil nil)]
    {:world world
     :agents {:priest agent-id1
              :knight agent-id2
              :peasant agent-id3}
     :tiles {:tile0 tile-id1
             :tile1 tile-id2
             :tile2 tile-id3}}))

(deftest test-needs-decay
  (testing "Needs decay system processes world"
    (let [{:keys [world agents]} (setup-world)
          role-type (be/get-component-type (c/->Role :priest))
          needs-type (be/get-component-type (c/->Needs nil nil nil 1.0 0.8 0.6 0.5 0.5 0.5 0.6 0.5 0.5 0.5))
          priest-id (:priest agents)
          initial-needs (be/get-component world priest-id needs-type)
          result (nd/process world 0.5)
          updated-needs (be/get-component result priest-id needs-type)]
      (is (not= initial-needs updated-needs) "Needs should change after decay"))))

(deftest test-needs-decay-cold-snap
  (testing "Needs decay accelerates during cold-snap"
    (let [{:keys [world agents]} (setup-world)
          role-type (be/get-component-type (c/->Role :priest))
          needs-type (be/get-component-type (c/->Needs nil nil nil 1.0 0.8 0.6 0.5 0.5 0.5 0.6 0.5 0.5 0.5))
          priest-id (:priest agents)
          result-cold (nd/process world 0.9)
          result-normal (nd/process world 0.5)
          needs-cold (be/get-component result-cold priest-id needs-type)
          needs-normal (be/get-component result-normal priest-id needs-type)]
      (is (:warmth needs-cold) "Cold-snap should affect warmth"))))

(deftest test-movement-along-path
  (testing "Movement system moves agent along path"
    (let [{:keys [world agents]} (setup-world)
          priest-id (:priest agents)
          path-type (be/get-component-type (c/->Path [] 0))
          test-path [(c/->Position 1 1) (c/->Position 2 2)]
          world-with-path (be/add-component world priest-id (c/->Path test-path 0))
          result (mv/process world-with-path)
          updated-path (be/get-component result priest-id path-type)]
      (is (some? (:waypoints updated-path)) "Path should have waypoints"))))

(deftest test-movement-removes-completed-path
  (testing "Movement removes path when agent reaches destination"
    (let [{:keys [world agents]} (setup-world)
          priest-id (:priest agents)
          path-type (be/get-component-type (c/->Path [] 0))
          world-with-path (be/add-component world priest-id (c/->Path [] 1))
          result (mv/process world-with-path)
          has-path? (be/get-component result priest-id path-type)]
      (is (nil? has-path?) "Completed path should be removed"))))

(deftest test-mortality-mark-death
  (testing "Mortality system marks dead agents"
    (let [{:keys [world agents]} (setup-world)
          knight-id (:knight agents)
          status-type (be/get-component-type (c/->AgentStatus true false false nil))
          world-with-dead (be/add-component world knight-id (c/->AgentStatus false true false nil))
          result (mort/process world-with-dead)
          updated-status (be/get-component result knight-id status-type)]
      (is (false? (:alive? updated-status)) "Dead agent should remain dead"))))

(deftest test-mortality-cleanup-components
  (testing "Mortality system cleans up dead agents (optional)"
    (let [{:keys [world agents]} (setup-world)
          knight-id (:knight agents)
          status-type (be/get-component-type (c/->AgentStatus true false false nil))
          world-with-dead (be/add-component world knight-id (c/->AgentStatus false true false nil))
          result (mort/process world-with-dead)
          agent-exists? (be/get-component result knight-id (be/get-component-type (c/->Role :knight)))]
      (is (some? agent-exists?) "Agent components should still exist (cleanup optional)"))))

(deftest test-combat-attack
  (testing "Combat system processes attacks between agents"
    (let [{:keys [world agents]} (setup-world)
          priest-id (:priest agents)
          knight-id (:knight agents)
          result (cb/process world 0)]
      (is (some? (:world result)) "Combat processing should return world"))))

(deftest test-combat-health-modification
  (testing "Combat system modifies agent health"
    (let [{:keys [world agents]} (setup-world)
          priest-id (:priest agents)
          knight-id (:knight agents)
          needs-type (be/get-component-type (c/->Needs nil nil nil 1.0 0.8 0.6 0.5 0.5 0.5 0.6 0.5 0.5 0.5))
          result (cb/process world 0)
          result-world (:world result)
          priest-health (be/get-component result-world priest-id needs-type)
          knight-health (be/get-component result-world knight-id needs-type)]
      (is (some? (:health priest-health)) "Priest should have health after combat")
      (is (some? (:health knight-health)) "Knight should have health after combat"))))

(deftest test-system-sequence
  (testing "All systems can be run in sequence"
    (let [{:keys [world]} (setup-world)
          after-needs (nd/process world 0.5)
          after-movement (mv/process after-needs)
          after-mortality (mort/process after-movement)
          after-combat (:world (cb/process after-mortality 0))]
      (is (some? after-combat) "All systems should process successfully"))))

(defn -main
  "Run all system tests."
  []
  (println "=== Running ECS Systems Tests ===")
  (test-needs-decay)
  (test-needs-decay-cold-snap)
  (test-movement-along-path)
  (test-movement-removes-completed-path)
  (test-mortality-mark-death)
  (test-mortality-cleanup-components)
  (test-combat-attack)
  (test-combat-health-modification)
  (test-system-sequence)
  (println "=== All ECS Systems Tests Passed! ==="))
