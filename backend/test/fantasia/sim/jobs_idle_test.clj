(ns fantasia.sim.jobs-idle-test
  (:require [clojure.test :refer [deftest is testing]]
            [fantasia.sim.jobs :as jobs]
            [fantasia.sim.tick.initial :as initial]))

(defn base-world []
  (let [world (initial/initial-world {:seed 1})]
    (assoc world :jobs [])))

(defn with-job [world job-type]
  (update world :jobs conj (jobs/create-job job-type [0 0])))

(deftest agent-reassigns-immediately-after-completion
  (let [world (-> (base-world)
                  (with-job :job/chop-tree))
        agent-id 0
        job (first (:jobs world))
        world-assigned (jobs/assign-job! world job agent-id)
        world-complete (jobs/advance-job! world-assigned agent-id 1.0)
        agent (get-in world-complete [:agents agent-id])]
    (is (nil? (:current-job (get-in world [:agents agent-id]))))
    (is (= (:current-job agent) (:current-job (get-in world-complete [:agents agent-id]))))
    (is (true? (:idle? agent)))))

(deftest idle-agent-claims-new-job
  (let [world (-> (base-world)
                  (with-job :job/chop-tree))
        agent-id 0
        job (first (:jobs world))
        world-assigned (jobs/assign-job! world job agent-id)
        world-idle (jobs/mark-agent-idle world-assigned agent-id)
        new-world (with-job world-idle :job/haul)
        claimed (jobs/auto-assign-jobs! new-world)
        agent (get-in claimed [:agents agent-id])]
    (is (:current-job agent))
    (is (false? (:idle? agent)))))
