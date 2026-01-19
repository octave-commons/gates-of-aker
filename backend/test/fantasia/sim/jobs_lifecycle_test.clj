(ns fantasia.sim.jobs-lifecycle-test
  (:require [clojure.test :refer [deftest is testing]]
            [fantasia.sim.jobs :as jobs]
            [fantasia.sim.tick.initial :as initial]))

(defn base-world []
  (-> (initial/initial-world {:seed 1})
      (assoc :jobs [])
      (assoc :items {"2,0" {:fruit 5 :wood 3}})
      (assoc :stockpiles {"0,0" {:resource :fruit :max-qty 10 :current-qty 0}})))

(defn advance-to-adjacent [world agent-id target]
  (assoc-in world [:agents agent-id :pos] target))

(deftest hauling-job-picks-up-items-into-hauling-inventory
  (let [world (base-world)
        target [0 0]
        job (assoc (jobs/create-job :job/haul target) :from-pos [2 0] :resource :fruit :qty 5 :to-pos target)
        world (update world :jobs conj job)
        agent-id 0
        world (jobs/assign-job! world job agent-id)
        world (jobs/pickup-items! world agent-id (:from-pos job) :fruit 5)
        agent (get-in world [:agents agent-id])]
    (is (= 5 (get-in agent [:inventories :hauling :fruit])))
    (is (= 5 (get-in agent [:inventory :fruit])))
    (is (zero? (get-in world [:items "2,0" :fruit] 0)))))

(deftest hauling-job-deposits-items-and-clears-hauling
  (let [target [0 0]
        job (assoc (jobs/create-job :job/haul target)
                   :from-pos [2 0]
                   :resource :fruit
                   :qty 5
                   :to-pos target)
        agent-id 0
        world0 (update (base-world) :jobs conj job)
        world1 (jobs/assign-job! world0 job agent-id)
        world2 (jobs/pickup-items! world1 agent-id (:from-pos job) :fruit 5)
        world3 (advance-to-adjacent world2 agent-id target)
        world4 (jobs/complete-haul! world3 job agent-id)
        stockpile (get-in world4 [:stockpiles "0,0"])
        agent (get-in world4 [:agents agent-id])]
    (is (= {} (get-in agent [:inventories :hauling])))
    (is (nil? (:inventory agent)))
    (is (= 5 (:current-qty stockpile)))))

(deftest hunger-generates-eat-job
  (let [world (-> (base-world)
                  (assoc-in [:agents 0 :needs :food] 0.1)
                  (jobs/generate-need-jobs!))
        eat-job (some #(when (= (:type %) :job/eat) %) (:jobs world))]
    (is eat-job)))
