(ns fantasia.test-helpers
  (:require [brute.entity :as be]
            [fantasia.sim.ecs.core :as ecs]
            [fantasia.sim.ecs.components :as c]))

(defn get-free-port
  "Get a free port for testing."
  []
  (let [server (java.net.ServerSocket.)]
    (.bind server nil 0)
    (let [port (.getLocalPort server)]
      (.close server)
      port)))

(defn create-test-world
  "Create a test ECS world."
  []
  (let [ecs-world (ecs/create-ecs-world)]
    ;; Add some basic components for testing
    (let [[agent-id world1] (ecs/create-agent ecs-world nil [0 0] :priest)]
          [agent-id2 world2] (ecs/create-agent ecs-world1 nil [1 0] :knight)]]
      (assoc ecs-world :agents {agent-id1 agent-id2}))))