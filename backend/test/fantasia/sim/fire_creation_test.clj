(ns fantasia.sim.fire-creation-test
  (:require [clojure.test :refer [deftest is testing]]
            [fantasia.sim.tick.initial :as initial]
            [fantasia.sim.jobs :as jobs]
            [fantasia.sim.hex :as hex]
            [fantasia.sim.agents :as agents]))

(deftest build-fire-job-creates-campfire
  (testing "Complete build-fire job creates campfire at target location"
    (let [world (initial/initial-world {})
          agent-id (-> world :agents first :id)
          target-pos [5 5]
          job (jobs/create-job :job/build-fire target-pos)
          world-with-job (-> world
                              (jobs/assign-job! job agent-id)
                              (update-in [:agents agent-id :inventory] assoc :wood 1))]
      (is (= :job/build-fire (:type job)))
      (is (= target-pos (:target job)))
      (let [world' (jobs/complete-job! world-with-job agent-id)]
        (is (= target-pos (:campfire world')))
        (is (= :campfire (get-in world' [:tiles (apply vector target-pos) :structure])))))))

(deftest fire-job-generated-for-cold-agent
  (testing "Build-fire job generated for cold agent with wood"
    (let [world (initial/initial-world {})
          agent-id (-> world :agents first :id)
          campfire-pos (:campfire world)
          world' (-> world
                     (assoc :campfire nil :shrine nil)
                     (cond-> campfire-pos
                       (update-in [:tiles (vec campfire-pos)] dissoc :structure))
                     (assoc-in [:agents agent-id :needs :warmth] 0.2)
                     (assoc-in [:agents agent-id :inventory :wood] 1))]
      (let [world'' (with-redefs [rand (fn [] 0.0)
                                  jobs/find-build-fire-site (fn [_ _] [5 5])]
                      (jobs/generate-need-jobs! world'))]
        (is (some #(= (:type %) :job/build-fire) (:jobs world'')))))))

(deftest fire-job-not-generated-when-campfire-nearby
  (testing "Build-fire job not generated when campfire nearby"
    (let [world (initial/initial-world {})
          agent-id (-> world :agents first :id)
          agent-pos (get-in world [:agents agent-id :pos])
          world' (-> world
                     (assoc :campfire agent-pos)
                     (assoc-in [:agents agent-id :needs :warmth] 0.2)
                     (assoc-in [:agents agent-id :inventory :wood] 1))]
      (let [world'' (jobs/generate-need-jobs! world')]
        (is (not (some #(= (:type %) :job/build-fire) (:jobs world''))))))))
