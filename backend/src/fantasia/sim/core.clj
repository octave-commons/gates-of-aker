(ns fantasia.sim.core
  (:require [fantasia.sim.ecs.tick :as ecs]))

(def rng (constantly 0)) ; Legacy compatibility - not used in ECS
(def rand-int* rand-int) ; Legacy compatibility - not used in ECS
(def ->agent identity) ; Legacy compatibility - not used in ECS
(def initial-world ecs/create-ecs-initial-world)
(def tick-once ecs/tick-ecs-once)
(def *state ecs/*state)
(def get-state ecs/get-state)
(def reset-world! ecs/reset-world!)
(def set-levers! ecs/set-levers!)
(def set-facet-limit! ecs/set-facet-limit!)
(def set-vision-radius! ecs/set-vision-radius!)
(def place-shrine! ecs/place-shrine!)
(def appoint-mouthpiece! ecs/appoint-mouthpiece!)
(def place-wall-ghost! ecs/place-wall-ghost!)
(def place-stockpile! ecs/place-stockpile!)
(def place-warehouse! ecs/place-warehouse!)
(def place-campfire! ecs/place-campfire!)
(def place-statue-dog! ecs/place-statue-dog!)
(def place-tree! ecs/place-tree!)
(def place-deer! ecs/place-deer!)
(def place-wolf! ecs/place-wolf!)
(def place-bear! ecs/place-bear!)
(def queue-build-job! ecs/queue-build-job!)
(def get-agent-path! ecs/get-agent-path!)
(def tick! ecs/tick!)
