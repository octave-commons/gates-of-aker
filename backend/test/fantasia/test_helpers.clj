(ns fantasia.test-helpers
  (:require [fantasia.sim.ecs.core :as ecs]))

(defn get-free-port
  "Get a free port for testing."
  []
  (let [server (java.net.ServerSocket. 0)]
    (let [port (.getLocalPort server)]
      (.close server)
      port)))

(defn create-test-world
  "Create a test ECS world."
  []
  (let [ecs-world (ecs/create-ecs-world)
        [_agent-id world1] (ecs/create-agent ecs-world nil 0 0 :priest)
        [_agent-id2 world2] (ecs/create-agent world1 nil 1 0 :knight)]
    world2))
